package com.kavach.core.analyzers

import com.kavach.core.model.Category
import com.kavach.core.model.Finding
import com.kavach.core.model.Severity

/** Works out what a QR code (or any raw scanned value) actually does before anything is opened. */
object QrContentAnalyzer {

    enum class Kind(val label: String) {
        WEB("Website link"), UPI("UPI payment"), WIFI("Wi-Fi network"), PHONE("Phone call"), SMS("Text message"),
        EMAIL("E-mail"), LOCATION("Map location"), CONTACT("Contact card"), APP_LINK("App link"),
        SCRIPT("Script / data"), TEXT("Plain text"),
    }

    fun classify(raw: String): Kind {
        val s = raw.trim().lowercase()
        return when {
            s.startsWith("upi://") -> Kind.UPI
            s.startsWith("http://") || s.startsWith("https://") || s.startsWith("www.") -> Kind.WEB
            s.startsWith("wifi:") -> Kind.WIFI
            s.startsWith("tel:") -> Kind.PHONE
            s.startsWith("sms:") || s.startsWith("smsto:") || s.startsWith("mms:") -> Kind.SMS
            s.startsWith("mailto:") || s.startsWith("matmsg:") -> Kind.EMAIL
            s.startsWith("geo:") -> Kind.LOCATION
            s.startsWith("begin:vcard") || s.startsWith("mecard:") -> Kind.CONTACT
            s.startsWith("intent:") || s.startsWith("market:") || s.startsWith("android-app:") -> Kind.APP_LINK
            s.startsWith("javascript:") || s.startsWith("data:") || s.startsWith("file:") || s.startsWith("vbscript:") -> Kind.SCRIPT
            !s.contains(' ') && MessageAnalyzer.extractUrls(s).singleOrNull() == s -> Kind.WEB
            else -> Kind.TEXT
        }
    }

    /** Findings for non-web, non-UPI payloads. Web links and UPI are handled by their own analyzers. */
    fun analyzeSpecial(raw: String, kind: Kind): List<Finding> {
        val s = raw.trim()
        val lower = s.lowercase()
        val f = mutableListOf<Finding>()
        when (kind) {
            Kind.PHONE -> {
                val num = s.substringAfter(':')
                // Call-forwarding USSD codes: scammers make victims dial these so OTP calls/SMS go to them.
                if (Regex("""^(\*\*?(21|61|62|67|401|004|002)\*|\*#?#?(21|62)#)""").containsMatchIn(num) || num.contains("*401*")) {
                    f += Finding(
                        "call_forward", "Dials a CALL-FORWARDING code",
                        "\"$num\" would forward your calls (and OTP calls) to someone else. This is a known Indian telecom fraud. Do not dial it.",
                        Severity.CRITICAL, Category.CONTENT, 90, "DoT / Sanchar Saathi advisory on call-forwarding fraud",
                    )
                } else if (num.startsWith("*") || num.startsWith("#")) {
                    f += Finding("ussd", "Dials a special USSD code ($num)", "USSD codes can change phone settings or make payments (*99#). Only dial codes you understand.", Severity.HIGH, Category.CONTENT, 45)
                } else {
                    f += Finding("phone", "Will start a phone call to $num", "Check the number belongs to who you think. Scam QR codes on posters lead to fake 'customer care'.", Severity.LOW, Category.CONTENT, 10)
                }
                if (num.startsWith("+") && !num.startsWith("+91")) {
                    f += Finding("intl_call", "International number", "Calls to international numbers can be expensive and are common in scams targeting India.", Severity.MEDIUM, Category.CONTENT, 20)
                }
            }
            Kind.SMS -> {
                val body = s.substringAfter(':').substringAfter(':', "")
                f += Finding(
                    "sms_send", "Prepares an SMS to send from your phone",
                    "Sending SMS from your SIM can register your number on someone else's UPI/WhatsApp, or subscribe you to paid services. Message: \"${body.take(60)}\"",
                    Severity.MEDIUM, Category.CONTENT, 30, "NPCI SIM-binding / I4C",
                )
            }
            Kind.WIFI -> {
                val ssid = Regex("""S:([^;]*)""").find(s)?.groupValues?.get(1) ?: "unknown"
                val open = lower.contains("t:nopass") || !lower.contains("p:")
                f += Finding(
                    "wifi", "Connects to Wi-Fi network \"$ssid\"",
                    if (open) "This network has no password. Others on it may see your traffic; avoid banking on it." else "Only join networks you trust, such as your home or office.",
                    if (open) Severity.MEDIUM else Severity.LOW, Category.CONNECTION, if (open) 20 else 8, "CERT-In public Wi-Fi advisory",
                )
            }
            Kind.APP_LINK -> f += Finding(
                "app_link", "Opens an app or app store directly",
                "This code launches \"${s.take(80)}\". Make sure it is an app you actually want; fake apps impersonate banks.",
                Severity.MEDIUM, Category.DOWNLOAD, 25, "OWASP MASVS-PLATFORM",
            )
            Kind.SCRIPT -> f += Finding(
                "script", "Contains code, not a normal link",
                "javascript:, data: or file: content can run code or show a fake page. There is no reason for a legitimate QR code to do this.",
                Severity.CRITICAL, Category.CONTENT, 85, "OWASP Mobile Top 10 (M4 insufficient input validation)",
            )
            Kind.EMAIL -> f += Finding("email", "Prepares an e-mail", "Check the address before sending anything personal.", Severity.LOW, Category.CONTENT, 5)
            Kind.CONTACT -> f += Finding("contact", "Adds a contact card", "Contact cards can carry fake 'bank' numbers. Verify before saving.", Severity.LOW, Category.CONTENT, 5)
            Kind.LOCATION -> f += Finding("geo", "Opens a map location", "Low risk.", Severity.INFO, Category.CONTENT, 0)
            else -> Unit
        }
        return f
    }
}
