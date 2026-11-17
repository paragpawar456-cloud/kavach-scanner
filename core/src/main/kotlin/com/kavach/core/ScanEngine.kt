package com.kavach.core

import com.kavach.core.analyzers.MessageAnalyzer
import com.kavach.core.analyzers.QrContentAnalyzer
import com.kavach.core.analyzers.UpiAnalyzer
import com.kavach.core.analyzers.UrlHeuristics
import com.kavach.core.model.Category
import com.kavach.core.model.CheckStatus
import com.kavach.core.model.Finding
import com.kavach.core.model.InputType
import com.kavach.core.model.ScanOptions
import com.kavach.core.model.ScanReport
import com.kavach.core.model.Severity
import com.kavach.core.model.Verdict
import com.kavach.core.online.HttpOnlineChecker
import com.kavach.core.online.OnlineChecker
import com.kavach.core.util.DomainUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Runs every independent check and combines them into one risk score and verdict.
 *
 * Layers (defence in depth, as recommended by NIST SP 800-53 and OWASP):
 *  1. Offline heuristics: address tricks, look-alikes, payment & message patterns (always on, private).
 *  2. Live inspection: redirect expansion, certificate, what the page serves (optional).
 *  3. Domain age via RDAP/WHOIS (optional).
 *  4. Threat-intelligence reputation: Google Safe Browsing, VirusTotal, URLhaus (only with the user's own keys).
 */
class ScanEngine(private val online: OnlineChecker = HttpOnlineChecker()) {

    suspend fun scan(input: String, type: InputType, options: ScanOptions = ScanOptions()): ScanReport = withContext(Dispatchers.IO) {
        val text = input.trim()
        when (type) {
            InputType.QR -> scanQr(text, options)
            InputType.MESSAGE, InputType.SCREENSHOT -> scanMessage(text, type, options)
            InputType.URL -> {
                // People paste whole messages into the link box too; route them sensibly.
                when {
                    UpiAnalyzer.isUpi(text) -> scanQr(text, options).copy(inputType = InputType.URL)
                    DomainUtils.parse(text) != null && !text.contains(' ') -> scanUrl(text, InputType.URL, options)
                    else -> scanMessage(text, InputType.MESSAGE, options)
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------

    private class UrlResult(val findings: List<Finding>, val checks: List<CheckStatus>, val chain: List<String>, val url: String)

    private suspend fun checkUrl(raw: String, options: ScanOptions): UrlResult = coroutineScope {
        val checks = mutableListOf<CheckStatus>()
        val parsed = DomainUtils.parse(raw)
            ?: return@coroutineScope UrlResult(
                listOf(Finding("unparseable", "Not a valid web address", "\"${raw.take(80)}\" could not be read as a link.", Severity.LOW, Category.DOMAIN, 10)),
                listOf(CheckStatus("Address analysis", true, "Could not parse")), emptyList(), raw,
            )
        val findings = UrlHeuristics.analyze(parsed).toMutableList()
        checks += CheckStatus("Address & look-alike analysis", true, "${findings.size} signal(s), offline")

        var chain = listOf(parsed.normalized)
        var finalUrl = parsed.normalized
        if (options.onlineChecks && options.followRedirects) {
            val r = runCatching { online.followRedirects(parsed.normalized) }.getOrNull()
            if (r != null) {
                chain = r.chain
                finalUrl = r.finalUrl
                findings += r.findings
                checks += CheckStatus("Live link inspection", r.reached, if (r.reached) "Followed ${r.chain.size - 1} redirect(s), checked certificate & page" else "Site not reachable")
                val finalParsed = DomainUtils.parse(finalUrl)
                if (finalParsed != null && finalParsed.scheme == "https" && parsed.scheme == "http" && finalParsed.registrableDomain == parsed.registrableDomain) {
                    // The site upgrades itself to https straight away: not a real weakness.
                    findings.removeAll { it.id == "http" }
                }
                if (finalParsed != null && finalParsed.registrableDomain != parsed.registrableDomain) {
                    // Destination is a different site: analyse it too, but don't double-count trust from the shortener.
                    val dest = UrlHeuristics.analyze(finalParsed).map {
                        it.copy(id = "dest_" + it.id, title = "Destination: " + it.title)
                    }
                    findings.removeAll { it.id == "official" || it.id == "well_known" }
                    findings += dest
                }
            } else checks += CheckStatus("Live link inspection", false, "Failed")
        } else {
            checks += CheckStatus("Live link inspection", false, "Off in settings")
        }

        val targets = listOf(parsed.normalized, finalUrl).distinct()
        val finalReg = DomainUtils.parse(finalUrl)?.registrableDomain ?: parsed.registrableDomain
        val jobs = buildList {
            if (options.onlineChecks && options.domainAgeLookup && !DomainUtils.isIp(finalReg)) {
                add(async { "Domain age (RDAP/WHOIS)" to runCatching { listOfNotNull(online.domainAge(finalReg)) } })
            }
            options.googleSafeBrowsingKey?.takeIf { options.onlineChecks && it.isNotBlank() }?.let { key ->
                add(async { "Google Safe Browsing" to runCatching { targets.mapNotNull { online.googleSafeBrowsing(it, key) } } })
            }
            options.virusTotalKey?.takeIf { options.onlineChecks && it.isNotBlank() }?.let { key ->
                add(async { "VirusTotal" to runCatching { targets.mapNotNull { online.virusTotal(it, key) } } })
            }
            options.urlhausKey?.takeIf { options.onlineChecks && it.isNotBlank() }?.let { key ->
                add(async { "URLhaus (abuse.ch)" to runCatching { targets.mapNotNull { online.urlhaus(it, key) } } })
            }
        }.awaitAll()
        for ((name, res) in jobs) {
            res.onSuccess { list ->
                // When both the short link and destination were checked, keep the worst result per source.
                val worst = list.groupBy { it.id.substringBefore('_') }.values.map { g -> g.maxBy { it.risk } }
                findings += worst
                checks += CheckStatus(name, worst.isNotEmpty(), worst.joinToString { it.title.substringAfter(": ", it.title) }.ifBlank { "No public data for this domain" })
            }.onFailure { checks += CheckStatus(name, false, "Error: ${it.message ?: it.javaClass.simpleName}") }
        }
        if (options.googleSafeBrowsingKey.isNullOrBlank()) checks += CheckStatus("Google Safe Browsing", false, "Add an API key in Settings")
        if (options.virusTotalKey.isNullOrBlank()) checks += CheckStatus("VirusTotal", false, "Add an API key in Settings")
        if (options.urlhausKey.isNullOrBlank()) checks += CheckStatus("URLhaus (abuse.ch)", false, "Add an API key in Settings")

        UrlResult(findings.distinctBy { it.id }, checks, chain, parsed.normalized)
    }

    private suspend fun scanUrl(raw: String, type: InputType, options: ScanOptions): ScanReport {
        val r = checkUrl(raw, options)
        return RiskScorer.report(raw, type, r.findings, listOf(r.url), r.checks, r.chain)
    }

    private suspend fun scanQr(raw: String, options: ScanOptions): ScanReport {
        val kind = QrContentAnalyzer.classify(raw)
        val qrCheck = CheckStatus("QR content type", true, kind.label)
        return when (kind) {
            QrContentAnalyzer.Kind.WEB -> {
                val r = checkUrl(raw, options)
                RiskScorer.report(raw, InputType.QR, r.findings, listOf(r.url), listOf(qrCheck) + r.checks, r.chain)
            }
            QrContentAnalyzer.Kind.UPI -> RiskScorer.report(
                raw, InputType.QR, UpiAnalyzer.analyze(raw), emptyList(),
                listOf(qrCheck, CheckStatus("UPI payment analysis", true, "NPCI / RBI rules applied")),
            )
            QrContentAnalyzer.Kind.TEXT -> scanMessage(raw, InputType.QR, options).let { it.copy(checks = listOf(qrCheck) + it.checks) }
            else -> RiskScorer.report(raw, InputType.QR, QrContentAnalyzer.analyzeSpecial(raw, kind), emptyList(), listOf(qrCheck))
        }
    }

    private suspend fun scanMessage(text: String, type: InputType, options: ScanOptions): ScanReport = coroutineScope {
        val findings = MessageAnalyzer.analyze(text).toMutableList()
        val checks = mutableListOf(CheckStatus("Message wording analysis", true, "${findings.size} scam pattern(s) found"))
        val urls = MessageAnalyzer.extractUrls(text).take(5)
        val chains = mutableListOf<String>()
        val results = urls.map { u -> async { u to checkUrl(u, options) } }.awaitAll()
        for ((u, r) in results) {
            val host = DomainUtils.parse(u)?.host ?: u
            findings += r.findings.map { it.copy(id = "$host:${it.id}", title = "$host · ${it.title}") }
            if (r.chain.size > 1) chains += r.chain
        }
        if (urls.isNotEmpty()) {
            // Merge per-check status across links.
            results.flatMap { it.second.checks }.groupBy { it.name }.forEach { (name, list) ->
                checks += CheckStatus(name, list.any { it.ran }, if (list.size == 1) list.first().note else "${list.count { it.ran }}/${list.size} links checked")
            }
        } else {
            checks += CheckStatus("Links in message", true, "No links found")
        }
        val upi = MessageAnalyzer.extractUpiIds(text)
        upi.filter { it.startsWith("upi://", true) }.forEach { findings += UpiAnalyzer.analyze(it).map { f -> f.copy(id = "upi:" + f.id) } }
        if (upi.isNotEmpty() && findings.any { it.severity >= Severity.MEDIUM && it.category == Category.CONTENT }) {
            findings += Finding(
                "upi_in_scam", "Asks you to pay a UPI ID (${upi.first().take(40)})",
                "Combined with the warning signs above, paying this UPI ID is very risky. Money sent by UPI is very hard to recover.",
                Severity.HIGH, Category.PAYMENT, 40, "NPCI UPI safety tips",
            )
        }
        val phones = MessageAnalyzer.extractPhones(text)
        if (phones.isNotEmpty() && findings.any { it.severity >= Severity.HIGH }) {
            findings += Finding(
                "phone_in_scam", "Contains a phone number to call (${phones.first()})",
                "Don't call it. You can report fraud numbers on Sanchar Saathi (Chakshu) at sancharsaathi.gov.in.",
                Severity.MEDIUM, Category.CONTENT, 15, "DoT Sanchar Saathi – Chakshu",
            )
        }
        RiskScorer.report(text, type, findings, urls, checks, chains)
    }
}

/** Turns findings into a 0-100 risk score and a verdict. */
object RiskScorer {

    /**
     * Independent signals combine like probabilities: risk = 1 − Π(1 − rᵢ). Two medium signals make a high risk,
     * but no amount of small signals reaches 100. Trust signals only reduce the score when nothing serious was found.
     */
    fun score(findings: List<Finding>): Int {
        val positive = findings.filter { it.risk > 0 }
        var p = 1.0
        for (f in positive) p *= (1.0 - f.risk.coerceAtMost(99) / 100.0)
        var score = ((1.0 - p) * 100).toInt()
        val serious = findings.any { it.severity >= Severity.HIGH && it.risk > 0 }
        if (!serious) {
            val trust = findings.filter { it.risk < 0 }.sumOf { it.risk }.coerceAtLeast(-40)
            score = (score + trust).coerceAtLeast(0)
        }
        if (findings.any { it.severity == Severity.CRITICAL && it.risk > 0 }) score = maxOf(score, 80)
        return score.coerceIn(0, 100)
    }

    fun verdict(score: Int, findings: List<Finding>): Verdict = when {
        findings.any { it.severity == Severity.CRITICAL && it.risk > 0 } || score >= 60 -> Verdict.DANGEROUS
        score >= 25 -> Verdict.SUSPICIOUS
        else -> Verdict.SAFE
    }

    fun report(
        input: String, type: InputType, findings: List<Finding>, urls: List<String>,
        checks: List<CheckStatus>, chain: List<String> = emptyList(),
    ): ScanReport {
        val sorted = findings.sortedWith(compareByDescending<Finding> { it.severity }.thenByDescending { it.risk })
        val s = score(sorted)
        val v = verdict(s, sorted)
        val onlineRan = checks.any { it.ran && (it.name.contains("Safe Browsing") || it.name.contains("VirusTotal") || it.name.contains("URLhaus")) }
        val summary = when (v) {
            Verdict.DANGEROUS -> "High risk of fraud. Do not open, pay, install or reply."
            Verdict.SUSPICIOUS -> "Warning signs found. Verify through an official channel before you act."
            Verdict.SAFE -> if (onlineRan || type == InputType.MESSAGE || type == InputType.SCREENSHOT)
                "No warning signs found. Still never share OTP, PIN or passwords."
            else "No warning signs in offline checks. Add free API keys in Settings for a deeper check."
        }
        return ScanReport(input, type, s, v, summary, sorted, urls, checks, chain, advice(v, sorted))
    }

    private fun advice(v: Verdict, findings: List<Finding>): List<String> {
        val a = mutableListOf<String>()
        if (v != Verdict.SAFE) {
            a += "Do not click the link, scan the code, install any app or share OTP / PIN / card details."
            a += "Verify by calling the official number on the company's website or the back of your card, not one from the message."
        }
        if (findings.any { it.category == Category.PAYMENT && it.risk >= 40 }) {
            a += "Remember: you never enter your UPI PIN or scan a QR code to RECEIVE money."
        }
        if (findings.any { it.category == Category.DOWNLOAD && it.risk >= 60 }) {
            a += "If you already installed an app from a link: switch on Airplane mode, uninstall it, and call your bank."
        }
        if (v == Verdict.DANGEROUS) {
            a += "Lost money? Call 1930 (National Cyber Crime Helpline) immediately and report at cybercrime.gov.in. Speed matters for freezing funds."
            a += "Report the fraud SMS/call/WhatsApp number on Sanchar Saathi → Chakshu (sancharsaathi.gov.in)."
        }
        if (v == Verdict.SAFE) a += "Stay alert: scam sites appear daily and new ones may not be known to any database yet."
        return a
    }
}
