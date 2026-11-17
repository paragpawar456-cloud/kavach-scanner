package com.kavach.core.analyzers

import com.kavach.core.model.Category
import com.kavach.core.model.Finding
import com.kavach.core.model.Severity
import com.kavach.core.util.DomainUtils
import com.kavach.core.util.KnownData
import com.kavach.core.util.ParsedUrl

/**
 * Offline checks on a single web address. Nothing here touches the network, so it works without internet
 * and never leaks what the user is checking.
 */
object UrlHeuristics {

    private const val REF_CERTIN = "CERT-In advisories on phishing & smishing"
    private const val REF_OWASP = "OWASP MASVS-PLATFORM / Mobile Top 10"

    fun analyze(url: ParsedUrl): List<Finding> {
        val f = mutableListOf<Finding>()
        val host = url.host
        val reg = url.registrableDomain
        val label = DomainUtils.domainLabel(reg)
        val official = officialMatch(host)
        val onFreeHost = KnownData.freeHosting.any { DomainUtils.isSameOrSubdomain(host, it) }

        // --- Connection basics -------------------------------------------------------------------------
        if (url.scheme == "http") {
            f += Finding(
                "http", "Not a secure (https) link",
                "Anything you type on this page can be read by others on the network. Banks and government sites always use https.",
                Severity.MEDIUM, Category.CONNECTION, 15, "NIST SP 800-52 (TLS) / RBI digital payment security controls",
            )
        }
        if (DomainUtils.isIp(host)) {
            f += Finding(
                "ip_host", "Uses a raw IP address instead of a name",
                "The link points to $host. Genuine companies use a domain name; IP links are typical of phishing kits and malware servers.",
                Severity.HIGH, Category.DOMAIN, 45, REF_CERTIN,
            )
        }
        if (!url.userInfo.isNullOrEmpty()) {
            f += Finding(
                "userinfo", "Hidden destination using the '@' trick",
                "Everything before '@' is ignored by the browser. The link really opens $host, not \"${url.userInfo}\".",
                Severity.HIGH, Category.DOMAIN, 55, REF_CERTIN,
            )
        }
        if (url.port != -1 && url.port != 80 && url.port != 443) {
            f += Finding(
                "port", "Unusual network port (${url.port})",
                "Normal websites do not need a port number in the link. This is common on hacked or temporary servers.",
                Severity.MEDIUM, Category.CONNECTION, 20,
            )
        }

        // --- Look-alike / impersonation ---------------------------------------------------------------
        val idn = host.split('.').any { it.startsWith("xn--") }
        if (idn) {
            val scripts = DomainUtils.scripts(url.unicodeHost.substringBeforeLast('.'))
            val skel = DomainUtils.skeleton(url.unicodeHost)
            val mimics = KnownData.officialDomains.firstOrNull { DomainUtils.skeleton(it) == skel || skel.contains(DomainUtils.skeleton(it.substringBefore('.'))) && it.substringBefore('.').length >= 4 }
            when {
                mimics != null -> f += Finding(
                    "homoglyph_brand", "Fake letters imitating $mimics",
                    "The address is \"${url.unicodeHost}\" but uses look-alike characters from other alphabets (${scripts.joinToString()}). It is NOT $mimics.",
                    Severity.CRITICAL, Category.DOMAIN, 85, "Unicode TR39 confusables / $REF_CERTIN",
                )
                scripts.size > 1 -> f += Finding(
                    "mixed_script", "Mixes letters from different alphabets",
                    "The name \"${url.unicodeHost}\" combines ${scripts.joinToString()} letters, a classic trick to fake a familiar name.",
                    Severity.HIGH, Category.DOMAIN, 60, "Unicode TR39 confusables",
                )
                else -> f += Finding(
                    "idn", "Uses an international (non-English) domain name",
                    "Shown as \"${url.unicodeHost}\" (real form: $host). Often legitimate, but check it is the site you expect.",
                    Severity.LOW, Category.DOMAIN, 10,
                )
            }
        }

        if (official == null && !DomainUtils.isIp(host)) {
            // Official domain buried inside a different domain, e.g. sbi.co.in.kyc-update.xyz
            val buried = KnownData.officialDomains.firstOrNull { od ->
                host.contains("$od.") || host.contains("$od-") || host.contains("-$od")
            }
            if (buried != null) {
                f += Finding(
                    "buried_official", "Pretends to be $buried",
                    "\"$buried\" appears inside the address, but the real website is $reg. This is a well-known phishing trick.",
                    Severity.CRITICAL, Category.DOMAIN, 80, REF_CERTIN,
                )
            }

            val brandHit = brandTokens(host).firstOrNull()
            if (brandHit != null && buried == null) {
                val officials = KnownData.brands[brandHit]!!.take(2).joinToString(" or ")
                f += Finding(
                    "brand_misuse", "Uses the name \"${brandHit.uppercase()}\" but is not its official site",
                    "Official site: $officials. This link goes to $reg instead. Fraudsters put bank or company names in fake links.",
                    Severity.HIGH, Category.DOMAIN, 55, "RBI fraud awareness (BE(A)WARE) / $REF_CERTIN",
                )
            }

            val typo = typoTarget(label)
            if (typo != null && brandHit == null && buried == null) {
                f += Finding(
                    "typosquat", "Looks like a misspelling of $typo",
                    "\"$reg\" is only a letter or two away from the real \"$typo\". Typo-domains are registered to catch people who don't look closely.",
                    Severity.HIGH, Category.DOMAIN, 65, "Typosquatting (MITRE ATT&CK T1583.001)",
                )
            }

            val govTokens = tokens(host)
            val looksGov = govTokens.any { it == "gov" || it == "govt" || it == "government" || it == "sarkar" || it == "yojana" }
            val isGov = DomainUtils.isSameOrSubdomain(host, "gov.in") || DomainUtils.isSameOrSubdomain(host, "nic.in") || url.tld == "gov"
            if (looksGov && !isGov) {
                f += Finding(
                    "fake_gov", "Pretends to be a government site",
                    "Real Indian government sites end in .gov.in or .nic.in. This one is $reg.",
                    Severity.HIGH, Category.DOMAIN, 55, "CERT-In / MeitY guidelines for government websites (GIGW)",
                )
            }
        }

        // --- Hiding & cheap infrastructure -------------------------------------------------------------
        if (KnownData.urlShorteners.any { DomainUtils.isSameOrSubdomain(host, it) }) {
            f += Finding(
                "shortener", "Short link hides the real website",
                "$host is a link-shortening service. The real destination is hidden until you open it. Turn on online checks to reveal it.",
                Severity.MEDIUM, Category.DOMAIN, 20,
            )
        }
        if (onFreeHost) {
            f += Finding(
                "free_hosting", "Hosted on a free website builder",
                "Anyone can create a page on ${KnownData.freeHosting.first { DomainUtils.isSameOrSubdomain(host, it) }} in minutes. Banks never ask you to log in on such pages.",
                Severity.MEDIUM, Category.DOMAIN, 25, REF_CERTIN,
            )
        }
        if (url.tld in KnownData.riskyTlds) {
            f += Finding(
                "risky_tld", "Ending \".${url.tld}\" is often used by scammers",
                "Domains ending in .${url.tld} are cheap and frequently abused for phishing. Indian banks use .in, .co.in, .bank.in or .com.",
                Severity.MEDIUM, Category.DOMAIN, 18, "Spamhaus / Interisle phishing TLD statistics",
            )
        }
        val subLevels = if (url.subdomain.isEmpty()) 0 else url.subdomain.count { it == '.' } + 1
        if (subLevels >= 3) {
            f += Finding(
                "deep_subdomains", "Unusually long chain of sub-names",
                "\"$host\" has $subLevels sub-levels. This pushes the real domain ($reg) out of view on small phone screens.",
                Severity.LOW, Category.DOMAIN, 12,
            )
        }
        val hyphens = label.count { it == '-' }
        if (hyphens >= 2) {
            f += Finding(
                "hyphens", "Many hyphens in the name",
                "Names like \"$label\" are typical of throw-away scam domains.",
                Severity.LOW, Category.DOMAIN, 10,
            )
        }
        if (label.count { it.isDigit() } >= 4 && !DomainUtils.isIp(host)) {
            f += Finding("digits", "Random-looking numbers in the name", "Machine-generated names are common for scam sites.", Severity.LOW, Category.DOMAIN, 8)
        }

        val hostWords = tokens(host).filter { it in KnownData.phishWords }.distinct()
        if (hostWords.isNotEmpty() && official == null) {
            f += Finding(
                "phish_words_host", "Scam keywords in the website name",
                "Contains: ${hostWords.joinToString()}. Real banks don't put words like these in their domain name.",
                Severity.MEDIUM, Category.DOMAIN, minOf(15 + 7 * hostWords.size, 35), REF_CERTIN,
            )
        }
        val decodedPath = DomainUtils.percentDecode(url.path + " " + url.query).lowercase()
        val pathWords = KnownData.phishWords.filter { Regex("(^|[^a-z])${Regex.escape(it)}([^a-z]|$)").containsMatchIn(decodedPath) }
        if (pathWords.size >= 2 && official == null) {
            f += Finding(
                "phish_words_path", "Login / KYC style page",
                "The link mentions: ${pathWords.take(5).joinToString()}. Be careful entering passwords, OTP, card or Aadhaar details.",
                Severity.LOW, Category.CONTENT, 10,
            )
        }

        // --- Downloads -----------------------------------------------------------------------------------
        val fileExt = url.path.substringAfterLast('/').substringAfterLast('.', "").lowercase().takeIf { url.path.substringAfterLast('/').contains('.') }
        if (fileExt != null && fileExt in KnownData.dangerousExtensions) {
            val apk = fileExt.startsWith("apk") || fileExt == "xapk"
            f += Finding(
                "dangerous_download",
                if (apk) "Downloads an Android app (.apk) from outside Play Store" else "Downloads a program file (.$fileExt)",
                if (apk) "Fake bank, electricity, RTO challan and \"customer support\" APKs steal OTPs and empty bank accounts. Never install apps from links."
                else "This file can run code on a device. Do not open it unless you know exactly who sent it and why.",
                Severity.CRITICAL, Category.DOWNLOAD, if (apk) 90 else 70,
                "CERT-In advisory on malicious APKs / $REF_OWASP",
            )
        } else if (fileExt != null && fileExt in KnownData.archiveExtensions) {
            f += Finding("archive", "Downloads a compressed file (.$fileExt)", "Archives are often used to hide malware.", Severity.LOW, Category.DOWNLOAD, 15)
        }

        if (Regex("""(^|&)(url|redirect|next|goto|target|dest|continue|return|r|u)=(https?|%68%74%74%70)""", RegexOption.IGNORE_CASE).containsMatchIn(url.query)) {
            f += Finding(
                "open_redirect", "Contains another link inside it",
                "This link forwards you to a second website, a trick used to borrow the trust of a known domain.",
                Severity.MEDIUM, Category.DOMAIN, 18, "OWASP Unvalidated Redirects",
            )
        }
        if (url.normalized.length > 200) {
            f += Finding("long_url", "Extremely long link", "Very long links are used to hide the important part from view.", Severity.LOW, Category.DOMAIN, 6)
        }

        // --- Trust signals ------------------------------------------------------------------------------
        if (official != null && !onFreeHost && url.userInfo.isNullOrEmpty()) {
            val (brand, domain) = official
            f += Finding(
                "official", "Official domain of ${brand.uppercase()}",
                "$reg is a known official domain ($domain). Still never share OTP, PIN or passwords on calls or chats.",
                Severity.INFO, Category.TRUST, if (url.scheme == "https") -35 else -15,
            )
        } else if (reg in KnownData.wellKnownDomains && !onFreeHost) {
            f += Finding("well_known", "Well-known website", "$reg is a widely used, established website.", Severity.INFO, Category.TRUST, -15)
        }
        return f
    }

    /** (brand, officialDomain) when [host] belongs to a known official domain. */
    fun officialMatch(host: String): Pair<String, String>? {
        for ((brand, domains) in KnownData.brands) {
            val d = domains.firstOrNull { DomainUtils.isSameOrSubdomain(host, it) }
            if (d != null) return brand to d
        }
        return null
    }

    private fun tokens(host: String): List<String> =
        DomainUtils.skeleton(host).split('.', '-', '_').filter { it.isNotEmpty() } +
            host.split('.', '-', '_').filter { it.isNotEmpty() }

    /** Brand keywords that appear in the host as whole words (or as a prefix/suffix of longer words for long brands). */
    fun brandTokens(host: String): List<String> {
        val raw = host.split('.', '-', '_').filter { it.isNotEmpty() }
        val toks = (raw + raw.map { DomainUtils.skeleton(it) } + raw.map { it.replace(Regex("\\d+"), "") }).distinct()
        return KnownData.brands.keys.filter { brand ->
            val bSkel = DomainUtils.skeleton(brand)
            toks.any { t ->
                t == brand || DomainUtils.skeleton(t) == bSkel ||
                    (brand.length >= 5 && (t.startsWith(brand) || t.endsWith(brand)))
            }
        }.sortedByDescending { it.length }
    }

    /** The official domain that [label] is a near-miss of, or null. */
    fun typoTarget(label: String): String? {
        if (label.length < 4) return null
        val skel = DomainUtils.skeleton(label.replace("-", ""))
        for (od in KnownData.officialDomains) {
            val ol = DomainUtils.domainLabel(od)
            if (ol.length < 4 || ol == label) continue
            if (DomainUtils.skeleton(ol) == skel) return od
            if (ol.length < 5) continue
            val d = DomainUtils.levenshtein(label, ol)
            val allowed = if (ol.length >= 7) 2 else 1
            if (d in 1..allowed && kotlin.math.abs(label.length - ol.length) <= allowed) return od
        }
        return null
    }
}
