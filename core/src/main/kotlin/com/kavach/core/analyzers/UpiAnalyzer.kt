package com.kavach.core.analyzers

import com.kavach.core.model.Category
import com.kavach.core.model.Finding
import com.kavach.core.model.Severity
import com.kavach.core.util.DomainUtils
import com.kavach.core.util.KnownData

/**
 * Checks UPI payment links/QR codes (upi://pay?...). The single most important rule, repeated by NPCI and RBI:
 * you never scan a QR code or enter your UPI PIN to RECEIVE money.
 */
object UpiAnalyzer {

    private const val REF = "NPCI UPI safety tips / RBI BE(A)WARE booklet"

    data class UpiPayment(val payee: String?, val name: String?, val amount: String?, val note: String?, val raw: String)

    fun isUpi(s: String) = s.trim().lowercase().startsWith("upi://")

    fun parse(s: String): UpiPayment {
        val q = s.substringAfter('?', "")
        val params = q.split('&').mapNotNull {
            val k = it.substringBefore('=', "").lowercase()
            if (k.isEmpty()) null else k to DomainUtils.percentDecode(it.substringAfter('=', ""))
        }.toMap()
        return UpiPayment(params["pa"], params["pn"], params["am"], params["tn"], s)
    }

    fun analyze(s: String): List<Finding> {
        val p = parse(s)
        val f = mutableListOf<Finding>()
        val action = s.trim().lowercase().removePrefix("upi://").substringBefore('?')
        f += Finding(
            "upi_info", "This is a UPI payment request",
            "Paying to: ${p.payee ?: "unknown"}${p.name?.let { " ($it)" } ?: ""}${p.amount?.let { ", amount ₹$it" } ?: ""}. " +
                "Scanning it and entering your PIN will SEND money from your account. You never need your PIN to receive money.",
            Severity.INFO, Category.PAYMENT, 5, REF,
        )
        if (action != "pay" && action != "mandate") {
            f += Finding("upi_odd_action", "Unusual UPI request type \"$action\"", "Normal payment QR codes use upi://pay.", Severity.MEDIUM, Category.PAYMENT, 20)
        }
        val vpa = p.payee
        if (vpa == null || !Regex("""^[a-zA-Z0-9._\-]{2,256}@[a-zA-Z0-9]{2,64}$""").matches(vpa)) {
            f += Finding("upi_bad_vpa", "Payee UPI ID is missing or malformed", "A genuine merchant QR always contains a valid UPI ID.", Severity.HIGH, Category.PAYMENT, 40, REF)
        } else {
            val handle = vpa.substringAfter('@').lowercase()
            if (handle !in KnownData.upiHandles) {
                f += Finding("upi_handle", "Uncommon UPI bank handle \"@$handle\"", "Not one of the commonly used bank handles. Check the name shown in your UPI app before paying.", Severity.LOW, Category.PAYMENT, 8)
            }
        }
        val text = listOfNotNull(p.name, p.note).joinToString(" ").lowercase()
        val lureWords = listOf("refund", "prize", "cashback", "lottery", "reward", "receive", "winning", "won", "gift", "bonus", "kyc", "claim", "lucky", "inaam")
        val lure = lureWords.filter { Regex("\\b$it").containsMatchIn(text) }
        if (lure.isNotEmpty()) {
            f += Finding(
                "upi_receive_scam", "\"Scan to receive money\" scam",
                "The payment note mentions ${lure.joinToString()}. Nobody sends you a refund, prize or cashback via a QR you must scan with your PIN. This will TAKE money from you.",
                Severity.CRITICAL, Category.PAYMENT, 90, REF,
            )
        }
        val authority = listOf("rbi", "reserve bank", "police", "cbi", "income tax", "customs", "court", "government", "govt", "customer care", "helpline", "trai", "cyber cell")
            .filter { text.contains(it) }
        if (authority.isNotEmpty()) {
            f += Finding(
                "upi_authority", "Payee claims to be an authority (${authority.first()})",
                "Government agencies, police and RBI never collect fines or \"verification charges\" through personal UPI QR codes.",
                Severity.HIGH, Category.PAYMENT, 50, "I4C / cybercrime.gov.in alerts",
            )
        }
        p.amount?.toDoubleOrNull()?.let { amt ->
            if (amt >= 10000) {
                f += Finding("upi_big_amount", "Large amount pre-filled (₹${"%,.0f".format(amt)})", "Double-check who you are paying before entering your PIN.", Severity.LOW, Category.PAYMENT, 12)
            } else if (amt > 0) {
                f += Finding("upi_amount", "Amount is pre-filled (₹${p.amount})", "Make sure this matches what you agreed to pay.", Severity.INFO, Category.PAYMENT, 3)
            }
        }
        return f
    }
}
