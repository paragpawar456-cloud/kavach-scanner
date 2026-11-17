package com.kavach.core.util

import java.net.IDN
import java.net.URI
import java.util.Locale

/** A URL broken into the parts the checks need. */
data class ParsedUrl(
    val original: String,
    val normalized: String,
    val scheme: String,
    val host: String,          // lower-case, punycode (ASCII) form
    val unicodeHost: String,   // what the user would see
    val port: Int,
    val path: String,
    val query: String,
    val userInfo: String?,
    val registrableDomain: String,
    val tld: String,
    val subdomain: String,
)

object DomainUtils {

    private val ipv4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val hexOrOctalHost = Regex("""^(0x[0-9a-f]+|\d{8,10}|0\d+(\.\d+){0,3})$""", RegexOption.IGNORE_CASE)

    /** Parses anything that looks like a link. Returns null when it cannot be understood as a web address. */
    fun parse(raw: String): ParsedUrl? {
        var s = raw.trim().trim('<', '>', '"', '\'', '(', ')', '[', ']').trimEnd('.', ',', ';', '!', '?')
        if (s.isEmpty() || s.any { it.isWhitespace() }) return null
        if (!Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(s)) s = "http://$s"
        val uri = try {
            URI(encodeLoosely(s))
        } catch (e: Exception) {
            return null
        }
        val scheme = (uri.scheme ?: "http").lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        val rawHost = uri.rawAuthority?.substringAfterLast('@')?.let { a ->
            if (a.startsWith("[")) a.substringBefore("]") + "]" else a.substringBefore(':')
        } ?: return null
        if (rawHost.isBlank()) return null
        val decodedHost = percentDecode(rawHost).lowercase(Locale.ROOT).trimEnd('.')
        val asciiHost = try {
            if (decodedHost.startsWith("[")) decodedHost else IDN.toASCII(decodedHost, IDN.ALLOW_UNASSIGNED).lowercase(Locale.ROOT)
        } catch (e: Exception) {
            decodedHost
        }
        val unicodeHost = try {
            IDN.toUnicode(asciiHost, IDN.ALLOW_UNASSIGNED)
        } catch (e: Exception) {
            asciiHost
        }
        if (!isIp(asciiHost) && !asciiHost.contains('.')) return null
        val reg = registrableDomain(asciiHost)
        val tld = if (isIp(asciiHost)) "" else asciiHost.substringAfterLast('.')
        val sub = if (asciiHost.length > reg.length) asciiHost.removeSuffix(reg).trimEnd('.') else ""
        return ParsedUrl(
            original = raw,
            normalized = s,
            scheme = scheme,
            host = asciiHost,
            unicodeHost = unicodeHost,
            port = uri.port,
            path = uri.rawPath ?: "",
            query = uri.rawQuery ?: "",
            userInfo = uri.rawUserInfo,
            registrableDomain = reg,
            tld = tld,
            subdomain = sub,
        )
    }

    /** Makes characters that java.net.URI rejects (spaces, unicode, |) acceptable without changing meaning. */
    private fun encodeLoosely(s: String): String {
        val sb = StringBuilder()
        val schemeEnd = s.indexOf("://") + 3
        val authEnd = s.indexOfAny(charArrayOf('/', '?', '#'), schemeEnd).let { if (it == -1) s.length else it }
        sb.append(s, 0, authEnd) // authority: unicode allowed, IDN handles it
        for (c in s.substring(authEnd)) {
            if (c.code > 126 || c == ' ' || c == '|' || c == '"' || c == '<' || c == '>' || c == '\\' || c == '^' || c == '`' || c == '{' || c == '}') {
                c.toString().toByteArray(Charsets.UTF_8).forEach { b -> sb.append('%').append(String.format("%02X", b)) }
            } else sb.append(c)
        }
        return sb.toString()
    }

    fun percentDecode(s: String): String = try {
        java.net.URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
    } catch (e: Exception) {
        s
    }

    fun isIp(host: String): Boolean =
        ipv4.matches(host) || host.startsWith("[") || hexOrOctalHost.matches(host)

    /** Simplified public-suffix logic: good enough to compare "sbi.co.in" with "sbi-kyc.co.in". */
    fun registrableDomain(host: String): String {
        if (isIp(host)) return host
        val labels = host.split('.').filter { it.isNotEmpty() }
        if (labels.size <= 2) return labels.joinToString(".")
        // Longest matching multi-part suffix wins.
        for (n in minOf(3, labels.size - 1) downTo 2) {
            val suffix = labels.takeLast(n).joinToString(".")
            if (suffix in KnownData.multiPartSuffixes) return labels.takeLast(n + 1).joinToString(".")
        }
        return labels.takeLast(2).joinToString(".")
    }

    /** True when [host] equals or is a subdomain of [domain]. */
    fun isSameOrSubdomain(host: String, domain: String): Boolean = host == domain || host.endsWith(".$domain")

    /** The domain name without its public suffix, e.g. "sbi-kyc" for "sbi-kyc.co.in". */
    fun domainLabel(registrable: String): String = registrable.substringBefore('.')

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /**
     * Maps characters that look alike to one "skeleton" so "paypa1" and "pаypal" (Cyrillic а) both become "paypal".
     * Based on the idea of Unicode TR39 confusables, trimmed to the characters scammers actually use.
     */
    fun skeleton(s: String): String {
        val sb = StringBuilder()
        var i = 0
        val lower = s.lowercase(Locale.ROOT)
        while (i < lower.length) {
            val c = lower[i]
            // multi-character tricks first
            if (lower.startsWith("rn", i)) { sb.append('m'); i += 2; continue }
            if (lower.startsWith("vv", i)) { sb.append('w'); i += 2; continue }
            sb.append(confusables[c] ?: c)
            i++
        }
        return sb.toString()
    }

    private val confusables: Map<Char, Char> = mapOf(
        '0' to 'o', '1' to 'l', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '8' to 'b', '9' to 'g', '|' to 'l', 'i' to 'l',
        // Cyrillic
        'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c', 'у' to 'y', 'х' to 'x', 'і' to 'l', 'ј' to 'j',
        'ѕ' to 's', 'һ' to 'h', 'ԁ' to 'd', 'ԛ' to 'q', 'ԝ' to 'w', 'к' to 'k', 'м' to 'm', 'н' to 'h', 'т' to 't',
        'в' to 'b', 'ӏ' to 'l', 'ɡ' to 'g',
        // Greek
        'α' to 'a', 'ο' to 'o', 'ρ' to 'p', 'ν' to 'v', 'τ' to 't', 'υ' to 'u', 'ι' to 'l', 'κ' to 'k', 'ε' to 'e',
        'χ' to 'x', 'γ' to 'y', 'η' to 'n',
        // Latin look-alikes
        'ı' to 'l', 'ł' to 'l', 'ś' to 's', 'ç' to 'c', 'à' to 'a', 'á' to 'a', 'â' to 'a', 'ä' to 'a', 'å' to 'a',
        'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e', 'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'ö' to 'o', 'ø' to 'o',
        'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u', 'ñ' to 'n', 'ý' to 'y', 'ɑ' to 'a', 'ɩ' to 'l',
    )

    /** Names of the Unicode scripts used by letters in [s], ignoring digits, '-' and '.'. */
    fun scripts(s: String): Set<String> = s.filter { it.isLetter() }.map { ch ->
        when (Character.UnicodeScript.of(ch.code)) {
            Character.UnicodeScript.LATIN -> "Latin"
            Character.UnicodeScript.CYRILLIC -> "Cyrillic"
            Character.UnicodeScript.GREEK -> "Greek"
            Character.UnicodeScript.DEVANAGARI -> "Devanagari"
            Character.UnicodeScript.ARMENIAN -> "Armenian"
            Character.UnicodeScript.HAN -> "Han"
            Character.UnicodeScript.ARABIC -> "Arabic"
            else -> Character.UnicodeScript.of(ch.code).name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
        }
    }.toSet()
}
