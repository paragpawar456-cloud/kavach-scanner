package com.kavach.core.model

/** Final verdict shown to the user. */
enum class Verdict { SAFE, SUSPICIOUS, DANGEROUS }

/** How serious a single finding is. CRITICAL alone forces a DANGEROUS verdict. */
enum class Severity { INFO, LOW, MEDIUM, HIGH, CRITICAL }

/** Groups findings in the result screen. */
enum class Category(val label: String) {
    DOMAIN("Website address"),
    CONTENT("Message wording"),
    PAYMENT("Payment / UPI"),
    REPUTATION("Threat databases"),
    CONNECTION("Connection & certificate"),
    DOWNLOAD("Downloads & apps"),
    TRUST("Trust signals"),
}

/** What the user asked us to scan. */
enum class InputType { URL, MESSAGE, QR, SCREENSHOT }

/**
 * One reason behind the verdict.
 *
 * @param risk 0..100 how much this alone suggests a scam. Negative values are trust signals that reduce risk.
 * @param reference optional pointer to the guidance this check follows (CERT-In, RBI, OWASP...).
 */
data class Finding(
    val id: String,
    val title: String,
    val detail: String,
    val severity: Severity,
    val category: Category,
    val risk: Int,
    val reference: String? = null,
)

/** Status of one of the independent checks, so the user can see what ran. */
data class CheckStatus(val name: String, val ran: Boolean, val note: String)

data class ScanReport(
    val input: String,
    val inputType: InputType,
    val riskScore: Int,
    val verdict: Verdict,
    val summary: String,
    val findings: List<Finding>,
    val urls: List<String>,
    val checks: List<CheckStatus>,
    val redirectChain: List<String> = emptyList(),
    val advice: List<String> = emptyList(),
)

/** User-controlled options. Keys are never hardcoded; they come from the app's settings screen. */
data class ScanOptions(
    val onlineChecks: Boolean = true,
    val followRedirects: Boolean = true,
    val domainAgeLookup: Boolean = true,
    val googleSafeBrowsingKey: String? = null,
    val virusTotalKey: String? = null,
    val urlhausKey: String? = null,
)
