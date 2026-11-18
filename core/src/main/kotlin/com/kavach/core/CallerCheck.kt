package com.kavach.core

import com.kavach.core.analyzers.UpiAnalyzer
import com.kavach.core.model.Category
import com.kavach.core.model.CheckStatus
import com.kavach.core.model.Finding
import com.kavach.core.model.InputType
import com.kavach.core.model.ScanReport
import com.kavach.core.model.Severity
import com.kavach.core.util.KnownData

/**
 * Checks a phone number or UPI ID that someone gave you. Uses TRAI numbering rules for India
 * (1600xx = genuine bank/insurance/government service calls, 140xx = telemarketing) and country codes
 * frequently seen in scam calls to India (I4C / DoT advisories).
 */
object CallerCheck {

    private val riskyCountries = mapOf(
        "92" to "Pakistan", "84" to "Vietnam", "62" to "Indonesia", "254" to "Kenya", "855" to "Cambodia",
        "856" to "Laos", "95" to "Myanmar", "880" to "Bangladesh", "63" to "Philippines", "60" to "Malaysia",
        "234" to "Nigeria", "233" to "Ghana", "977" to "Nepal", "94" to "Sri Lanka", "86" to "China",
        "1" to "USA/Canada", "44" to "UK", "971" to "UAE", "7" to "Russia/Kazakhstan",
    )

    private const val TRAI = "TRAI numbering for commercial calls (1600 / 140 series)"
    private const val DOT = "DoT / I4C advisories on international fraud calls"

    fun analyze(raw: String): ScanReport {
        val input = raw.trim()
        if (input.contains('@') && !input.contains(' ') && !input.contains("://")) return upi(input)
        val digits = input.filter { it.isDigit() || it == '+' }
        val f = mutableListOf<Finding>()
        val n = digits.removePrefix("+")
        val local = when {
            digits.startsWith("+91") -> n.removePrefix("91")
            digits.startsWith("0091") -> n.removePrefix("0091")
            digits.startsWith("+") || digits.startsWith("00") -> null // international
            digits.length == 12 && digits.startsWith("91") -> digits.removePrefix("91")
            digits.startsWith("0") && digits.length == 11 -> digits.removePrefix("0")
            else -> digits
        }

        if (local == null) {
            val intl = n.removePrefix("00")
            val code = listOf(3, 2, 1).map { intl.take(it) }.firstOrNull { it in riskyCountries }
            if (code != null) {
                val country = riskyCountries.getValue(code)
                val high = code !in setOf("1", "44", "971")
                f += Finding(
                    "intl_risky", "International number from $country (+$code)",
                    "Indian banks, police, courier companies and government offices never call from foreign numbers. " +
                        if (high) "+$code numbers are widely reported in job, investment and \"digital arrest\" scams targeting India." else "Fake job offers and \"customer care\" calls often use such numbers.",
                    if (high) Severity.HIGH else Severity.MEDIUM, Category.CONTENT, if (high) 55 else 30, DOT,
                )
            } else {
                f += Finding("intl", "International number", "Be careful: no Indian bank or government office will call you from abroad.", Severity.MEDIUM, Category.CONTENT, 25, DOT)
            }
        } else when {
            local.startsWith("1600") -> f += Finding(
                "trai_1600", "1600 series: registered service number",
                "TRAI reserves 1600xx numbers for genuine banks, insurers, RBI/SEBI-regulated firms and government service calls. This is a good sign, but still never share OTP or PIN on any call.",
                Severity.INFO, Category.TRUST, -30, TRAI,
            )
            local.startsWith("140") -> f += Finding(
                "trai_140", "140 series: telemarketing / promotional",
                "This is a registered promotional caller. Usually just marketing, not a bank. You can block such calls through DND (dial 1909).",
                Severity.LOW, Category.CONTENT, 8, TRAI,
            )
            local in setOf("1930", "112", "100", "101", "102", "108", "181", "1098", "14422", "1909", "1800") -> f += Finding(
                "helpline", "Official helpline number", "This is a public government helpline.", Severity.INFO, Category.TRUST, -30,
            )
            local.startsWith("1800") || local.startsWith("1860") -> f += Finding(
                "tollfree", "Toll-free / business number",
                "Real companies use these, but scammers also advertise fake 'customer care' 1800 numbers on Google. Confirm it on the official website.",
                Severity.LOW, Category.CONTENT, 10,
            )
            Regex("^[6-9]\\d{9}$").matches(local) -> {
                f += Finding(
                    "mobile", "Ordinary Indian mobile number",
                    "Banks and government departments make service calls from 1600-series numbers, not personal mobiles. If this caller claims to be a bank, police, courier, TRAI or electricity board, it is very likely a scam.",
                    Severity.MEDIUM, Category.CONTENT, 25, TRAI,
                )
            }
            Regex("^[1-5]\\d{9}$").matches(local) -> f += Finding("landline", "Indian landline number", "Check it against the number on the organisation's official website.", Severity.LOW, Category.CONTENT, 10)
            else -> f += Finding("odd", "Unusual number format", "This doesn't look like a normal Indian number. Be careful.", Severity.MEDIUM, Category.CONTENT, 25)
        }
        f += Finding(
            "report_number", "Report fraud numbers on Sanchar Saathi (Chakshu)",
            "If this number called or messaged you with a scam, report it at sancharsaathi.gov.in so it can be blocked.",
            Severity.INFO, Category.CONTENT, 0, "DoT Sanchar Saathi",
        )
        return RiskScorer.report(input, InputType.PHONE, f, emptyList(), listOf(CheckStatus("Caller number analysis", true, "TRAI numbering rules applied")))
    }

    private fun upi(vpa: String): ScanReport {
        val f = UpiAnalyzer.analyze("upi://pay?pa=$vpa").filter { it.id != "upi_info" && it.id != "upi_amount" }.toMutableList()
        val name = vpa.substringBefore('@').lowercase()
        if (Regex("^[6-9]\\d{9}$").matches(name)) {
            f += Finding("upi_mobile", "Personal UPI ID (mobile number)", "This UPI ID belongs to a person, not a company. Banks, government and police never ask you to pay a personal UPI ID.", Severity.MEDIUM, Category.PAYMENT, 20, "NPCI UPI safety tips")
        }
        val lure = listOf("refund", "cashback", "reward", "lottery", "prize", "kyc", "support", "care", "help", "rbi", "police", "govt", "tax", "customs")
            .filter { name.contains(it) }
        if (lure.isNotEmpty()) {
            f += Finding("upi_lure", "UPI ID contains \"${lure.first()}\"", "Scammers name UPI IDs to look official (e.g. 'sbi.refund@ybl'). Real refunds never need you to pay.", Severity.HIGH, Category.PAYMENT, 50, "NPCI UPI safety tips")
        }
        val handle = vpa.substringAfter('@').lowercase()
        if (handle in KnownData.upiHandles) f += Finding("upi_handle_ok", "Valid UPI bank handle (@$handle)", "The handle belongs to a known bank/app. This doesn't prove who owns the ID: check the name your UPI app shows before paying.", Severity.INFO, Category.PAYMENT, 0)
        f += Finding("upi_rule", "Golden rule", "You never need to pay, scan a QR or enter your UPI PIN to RECEIVE money.", Severity.INFO, Category.PAYMENT, 0, "RBI / NPCI")
        return RiskScorer.report(vpa, InputType.PHONE, f, emptyList(), listOf(CheckStatus("UPI ID analysis", true, "NPCI rules applied")))
    }
}
