package com.kavach.core.online

import com.kavach.core.model.Category
import com.kavach.core.model.Finding
import com.kavach.core.model.Severity
import com.kavach.core.util.DomainUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/** Result of following a link hop by hop without a browser (no JavaScript, no cookies). */
data class RedirectResult(
    val chain: List<String>,
    val finalUrl: String,
    val findings: List<Finding>,
    val reached: Boolean,
)

/** What the engine needs from the network. An interface so it can be faked in tests. */
interface OnlineChecker {
    fun followRedirects(url: String): RedirectResult
    fun domainAge(registrableDomain: String): Finding?
    fun googleSafeBrowsing(url: String, apiKey: String): Finding?
    fun virusTotal(url: String, apiKey: String): Finding?
    fun urlhaus(url: String, authKey: String): Finding?
}

class HttpOnlineChecker(
    private val client: OkHttpClient = defaultClient(),
) : OnlineChecker {

    companion object {
        /** Generic mobile browser UA: some phishing kits hide from obvious bots, and we don't want to reveal the user. */
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
        private const val MAX_HOPS = 10
        private const val PEEK_BYTES = 64L * 1024

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build() // no cookie jar: OkHttp's default is NO_COOKIES
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val apiClient by lazy { client.newBuilder().followRedirects(true).followSslRedirects(true).build() }

    override fun followRedirects(url: String): RedirectResult {
        val chain = mutableListOf<String>()
        val findings = mutableListOf<Finding>()
        var current = url
        var reached = false
        for (hop in 0 until MAX_HOPS) {
            chain += current
            val httpUrl = current.toHttpUrlOrNull() ?: break
            if (isPrivateHost(httpUrl.host)) {
                findings += Finding("private_host", "Points to a private / local network address", "${httpUrl.host} is inside a local network. Links from strangers should never do this.", Severity.HIGH, Category.CONNECTION, 40)
                break
            }
            val req = Request.Builder().url(httpUrl).header("User-Agent", USER_AGENT).header("Accept", "text/html,*/*").get().build()
            try {
                client.newCall(req).execute().use { resp ->
                    reached = true
                    if (hop == 0 || resp.request.url.isHttps) {
                        (resp.handshake?.peerCertificates?.firstOrNull() as? X509Certificate)?.let { cert ->
                            if (hop == chain.lastIndex && !resp.isRedirect) certificateFinding(cert)?.let { findings += it }
                        }
                    }
                    val location = resp.header("Location")
                    if (resp.isRedirect && location != null) {
                        current = resp.request.url.resolve(location)?.toString() ?: return@use
                        return@use
                    }
                    val type = resp.header("Content-Type").orEmpty().lowercase()
                    val disposition = resp.header("Content-Disposition").orEmpty().lowercase()
                    if (type.contains("android.package-archive") || disposition.contains(".apk")) {
                        findings += Finding(
                            "serves_apk", "The website sends an Android app file (APK)",
                            "Opening this link starts downloading an app from outside the Play Store. This is how most banking-trojan infections in India start.",
                            Severity.CRITICAL, Category.DOWNLOAD, 90, "CERT-In advisory on malicious APKs",
                        )
                    } else if (type.contains("application/octet-stream") || type.contains("x-msdownload") || disposition.contains("attachment")) {
                        findings += Finding("serves_file", "The website starts a file download", "A link that downloads a file directly deserves extra caution.", Severity.MEDIUM, Category.DOWNLOAD, 30)
                    } else if (type.contains("html")) {
                        val html = resp.peekBody(PEEK_BYTES).string()
                        findings += pageFindings(html)
                        val meta = Regex("""<meta[^>]+http-equiv=["']?refresh["']?[^>]+content=["']?\s*\d+\s*;\s*url=([^"'>\s]+)""", RegexOption.IGNORE_CASE).find(html)
                            ?: Regex("""(?:window\.)?location(?:\.href)?\s*=\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
                        val next = meta?.groupValues?.get(1)?.let { resp.request.url.resolve(it.replace("&amp;", "&"))?.toString() }
                        if (next != null && next != current) {
                            current = next
                            return@use
                        }
                    }
                    if (resp.code >= 400) {
                        findings += Finding("http_error", "Website answered with error ${resp.code}", "The page may have been taken down (often a sign it was reported) or blocks scanners.", Severity.INFO, Category.CONNECTION, 0)
                    }
                    current = resp.request.url.toString()
                    return RedirectResult(chain.distinct(), current, findings + hopFindings(chain), reached)
                }
            } catch (e: SSLException) {
                findings += Finding(
                    "tls_error", "Security certificate problem",
                    "A secure connection could not be verified (${e.javaClass.simpleName}). The site may be fake or misconfigured; never enter details on it.",
                    Severity.HIGH, Category.CONNECTION, 45, "NIST SP 800-52 / OWASP MASVS-NETWORK",
                )
                break
            } catch (e: Exception) {
                findings += Finding("unreachable", "Could not reach the website", "${e.javaClass.simpleName}: the site may be down, blocked, or only work in some regions. Other checks still ran.", Severity.INFO, Category.CONNECTION, 0)
                break
            }
        }
        return RedirectResult(chain.distinct(), current, findings + hopFindings(chain), reached)
    }

    private fun hopFindings(chain: List<String>): List<Finding> {
        val hosts = chain.mapNotNull { it.toHttpUrlOrNull()?.host }.map { DomainUtils.registrableDomain(it) }.distinct()
        val out = mutableListOf<Finding>()
        if (chain.size > 1) {
            out += Finding(
                "redirects", "Link forwards you ${chain.size - 1} time(s)",
                "Real destination: ${chain.last()}",
                if (chain.size > 4) Severity.MEDIUM else Severity.INFO, Category.CONNECTION,
                if (chain.size > 4) 18 else 0,
            )
        }
        if (hosts.size >= 3) {
            out += Finding("many_domains", "Passes through ${hosts.size} different websites", hosts.joinToString(" → "), Severity.MEDIUM, Category.CONNECTION, 15)
        }
        return out
    }

    private fun certificateFinding(cert: X509Certificate): Finding? {
        val issuedDaysAgo = Duration.between(cert.notBefore.toInstant(), Instant.now()).toDays()
        val issuer = cert.issuerX500Principal.name.substringAfter("O=", "").substringBefore(',').ifBlank { cert.issuerX500Principal.name.take(40) }
        return Finding(
            "tls_ok", "Valid HTTPS certificate",
            "Issued by $issuer, $issuedDaysAgo days ago. This only means the connection is encrypted; scammers can get certificates too.",
            Severity.INFO, Category.CONNECTION, 0,
        )
    }

    private fun pageFindings(html: String): List<Finding> {
        val h = html.lowercase()
        val out = mutableListOf<Finding>()
        if (Regex("""type=["']?password""").containsMatchIn(h)) {
            out += Finding("password_form", "Page asks for a password", "Only type passwords on the official website or app you opened yourself.", Severity.MEDIUM, Category.CONTENT, 15)
        }
        val sensitive = listOf("cvv", "card number", "atm pin", "upi pin", "mpin", "aadhaar", "aadhar", "otp", "expiry").filter { h.contains(it) }
        if (sensitive.size >= 2 && h.contains("<input")) {
            out += Finding("sensitive_form", "Page collects card / PIN / Aadhaar details", "Form fields mention: ${sensitive.joinToString()}.", Severity.HIGH, Category.CONTENT, 35, "RBI BE(A)WARE booklet")
        }
        return out
    }

    private fun isPrivateHost(host: String): Boolean =
        host == "localhost" || host.endsWith(".local") || host.startsWith("127.") || host.startsWith("10.") ||
            host.startsWith("192.168.") || host.startsWith("169.254.") || host == "0.0.0.0" || host == "[::1]" ||
            Regex("""^172\.(1[6-9]|2\d|3[01])\.""").containsMatchIn(host)

    /** Domain age from RDAP (the modern, free replacement for WHOIS). New domains are a strong scam signal. */
    override fun domainAge(registrableDomain: String): Finding? {
        if (DomainUtils.isIp(registrableDomain)) return null
        val req = Request.Builder().url("https://rdap.org/domain/$registrableDomain").header("Accept", "application/rdap+json").build()
        return try {
            apiClient.newCall(req).execute().use { resp ->
                if (resp.code == 404) {
                    return Finding("rdap_missing", "Domain not found in the registry", "$registrableDomain is not (or no longer) registered, or its registry doesn't publish data.", Severity.LOW, Category.DOMAIN, 10)
                }
                if (!resp.isSuccessful) return null
                val obj = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
                val events = obj["events"] as? JsonArray ?: return null
                val reg = events.map { it.jsonObject }.firstOrNull { it["eventAction"]?.jsonPrimitive?.content == "registration" }
                    ?.get("eventDate")?.jsonPrimitive?.content ?: return null
                val days = Duration.between(OffsetDateTime.parse(reg).toInstant(), Instant.now()).toDays()
                ageFinding(registrableDomain, days)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun googleSafeBrowsing(url: String, apiKey: String): Finding? {
        val body = buildJsonObject {
            putJsonObject("client") { put("clientId", "kavach-scanner"); put("clientVersion", "1.0") }
            putJsonObject("threatInfo") {
                putJsonArray("threatTypes") {
                    listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION").forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
                putJsonArray("platformTypes") { add(kotlinx.serialization.json.JsonPrimitive("ANY_PLATFORM")) }
                putJsonArray("threatEntryTypes") { add(kotlinx.serialization.json.JsonPrimitive("URL")) }
                putJsonArray("threatEntries") { add(buildJsonObject { put("url", url) }) }
            }
        }
        val req = Request.Builder()
            .url("https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Safe Browsing HTTP ${resp.code}")
            val obj = json.parseToJsonElement(resp.body?.string().orEmpty().ifBlank { "{}" }).jsonObject
            val matches = obj["matches"]?.jsonArray
            if (matches.isNullOrEmpty()) {
                Finding("gsb_clean", "Google Safe Browsing: not listed", "Not on Google's list of known dangerous sites (new scam sites may not be listed yet).", Severity.INFO, Category.REPUTATION, -5)
            } else {
                val types = matches.mapNotNull { it.jsonObject["threatType"]?.jsonPrimitive?.content }.distinct()
                Finding(
                    "gsb_hit", "Google Safe Browsing: DANGEROUS",
                    "Google lists this link as ${types.joinToString { it.lowercase().replace('_', ' ') }}.",
                    Severity.CRITICAL, Category.REPUTATION, 95, "Google Safe Browsing v4",
                )
            }
        }
    }

    override fun virusTotal(url: String, apiKey: String): Finding? {
        val id = Base64.getUrlEncoder().withoutPadding().encodeToString(url.toByteArray())
        val req = Request.Builder().url("https://www.virustotal.com/api/v3/urls/$id").header("x-apikey", apiKey).build()
        return apiClient.newCall(req).execute().use { resp ->
            if (resp.code == 404) {
                return Finding("vt_unknown", "VirusTotal: never scanned before", "No security vendor has analysed this exact link yet. Brand-new links are common in scams.", Severity.LOW, Category.REPUTATION, 5)
            }
            if (!resp.isSuccessful) throw IllegalStateException("VirusTotal HTTP ${resp.code}")
            val stats = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject["data"]?.jsonObject
                ?.get("attributes")?.jsonObject?.get("last_analysis_stats")?.jsonObject ?: return null
            val mal = stats["malicious"]?.jsonPrimitive?.int ?: 0
            val sus = stats["suspicious"]?.jsonPrimitive?.int ?: 0
            val harmless = stats["harmless"]?.jsonPrimitive?.int ?: 0
            when {
                mal >= 2 -> Finding("vt_malicious", "VirusTotal: $mal security vendors flag it", "Multiple antivirus/security engines report this link as malicious.", Severity.CRITICAL, Category.REPUTATION, 90, "VirusTotal API v3")
                mal == 1 || sus >= 1 -> Finding("vt_suspicious", "VirusTotal: flagged by ${mal + sus} vendor(s)", "At least one security engine considers it malicious or suspicious.", Severity.HIGH, Category.REPUTATION, 45, "VirusTotal API v3")
                else -> Finding("vt_clean", "VirusTotal: no vendor flags it", "$harmless engines rated it harmless.", Severity.INFO, Category.REPUTATION, -8)
            }
        }
    }

    override fun urlhaus(url: String, authKey: String): Finding? {
        val req = Request.Builder().url("https://urlhaus-api.abuse.ch/v1/url/")
            .header("Auth-Key", authKey)
            .post(FormBody.Builder().add("url", url).build()).build()
        return apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("URLhaus HTTP ${resp.code}")
            val obj: JsonObject = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            when (obj["query_status"]?.jsonPrimitive?.content) {
                "ok" -> Finding(
                    "urlhaus_hit", "URLhaus: known MALWARE link",
                    "abuse.ch lists this link as spreading malware (${obj["threat"]?.jsonPrimitive?.content ?: "malware"}, status ${obj["url_status"]?.jsonPrimitive?.content ?: "unknown"}).",
                    Severity.CRITICAL, Category.REPUTATION, 95, "abuse.ch URLhaus",
                )
                "no_results" -> Finding("urlhaus_clean", "URLhaus: not a known malware link", "Not in the abuse.ch malware URL database.", Severity.INFO, Category.REPUTATION, 0)
                else -> null
            }
        }
    }
}

/** Shared so the engine and tests use the same thresholds. */
fun ageFinding(domain: String, days: Long): Finding = when {
    days < 30 -> Finding(
        "domain_new", "Website created only $days day(s) ago",
        "$domain is brand new. Most scam sites are used for a few days and then abandoned. Genuine banks' domains are decades old.",
        Severity.HIGH, Category.DOMAIN, 50, "RDAP (ICANN) registration data",
    )
    days < 180 -> Finding("domain_young", "Website is less than 6 months old", "$domain was registered $days days ago.", Severity.MEDIUM, Category.DOMAIN, 22, "RDAP (ICANN) registration data")
    days > 3 * 365 -> Finding("domain_old", "Website has existed for ${days / 365} years", "Older domains are less likely to be throw-away scam sites (but can still be hacked).", Severity.INFO, Category.TRUST, -10)
    else -> Finding("domain_age", "Website is about ${days / 30} months old", "$domain was registered $days days ago.", Severity.INFO, Category.DOMAIN, 0)
}
