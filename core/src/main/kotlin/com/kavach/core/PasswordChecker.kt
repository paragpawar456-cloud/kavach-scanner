package com.kavach.core

import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.log2
import kotlin.math.pow

/**
 * Offline password strength estimate (NIST SP 800-63B ideas: length first, block common/breached passwords),
 * plus an optional breach check that uses k-anonymity: only the first 5 characters of the SHA-1 hash are sent,
 * so the password itself never leaves the phone.
 */
object PasswordChecker {

    enum class Strength(val label: String) { VERY_WEAK("Very weak"), WEAK("Weak"), FAIR("Fair"), STRONG("Strong"), VERY_STRONG("Very strong") }

    data class Result(val score: Int, val strength: Strength, val crackTime: String, val problems: List<String>, val tips: List<String>)

    private val common = setOf(
        "123456", "123456789", "12345678", "password", "qwerty", "12345", "1234567", "111111", "123123", "abc123",
        "password1", "1234", "iloveyou", "000000", "1q2w3e4r", "qwerty123", "admin", "welcome", "monkey", "dragon",
        "india123", "india@123", "sachin", "krishna", "sairam", "omsairam", "jaishreeram", "ganesh", "123321",
        "654321", "666666", "7777777", "987654321", "qwertyuiop", "asdfgh", "zxcvbnm", "passw0rd", "p@ssw0rd",
        "letmein", "sunshine", "princess", "football", "cricket", "superman", "batman", "secret", "login", "master",
        "hello123", "pass@123", "admin@123", "test@123", "welcome@123", "india", "bharat", "hindustan",
    )
    private val sequences = listOf("0123456789", "abcdefghijklmnopqrstuvwxyz", "qwertyuiop", "asdfghjkl", "zxcvbnm")

    fun check(pw: String): Result {
        if (pw.isEmpty()) return Result(0, Strength.VERY_WEAK, "instantly", listOf("Empty password"), emptyList())
        val problems = mutableListOf<String>()
        val tips = mutableListOf<String>()
        var pool = 0
        if (pw.any { it.isLowerCase() }) pool += 26
        if (pw.any { it.isUpperCase() }) pool += 26
        if (pw.any { it.isDigit() }) pool += 10
        if (pw.any { !it.isLetterOrDigit() }) pool += 33
        var bits = pw.length * log2(pool.coerceAtLeast(10).toDouble())

        val lower = pw.lowercase()
        val leet = lower.replace('@', 'a').replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('$', 's').replace('5', 's')
        if (lower in common || leet in common || leet.trimEnd { it.isDigit() || !it.isLetterOrDigit() } in common) {
            problems += "This is one of the most commonly used passwords"
            bits = minOf(bits, 10.0)
        }
        if (pw.length < 8) problems += "Shorter than 8 characters"
        else if (pw.length < 12) tips += "Use 12 or more characters"
        if (Regex("""(.)\1{2,}""").containsMatchIn(pw)) { problems += "Repeated characters (like 'aaa')"; bits -= 8 }
        if (sequences.any { s -> (0..s.length - 4).any { i -> lower.contains(s.substring(i, i + 4)) || lower.contains(s.substring(i, i + 4).reversed()) } }) {
            problems += "Contains an easy sequence (like 1234, abcd, qwer)"; bits -= 10
        }
        if (Regex("""(19|20)\d{2}""").containsMatchIn(pw)) { problems += "Contains a year (birth years are guessed first)"; bits -= 6 }
        if (Regex("""^[6-9]\d{9}$""").matches(pw)) { problems += "Looks like a mobile number"; bits = minOf(bits, 15.0) }
        if (pw.all { it.isDigit() }) problems += "Only numbers"
        if (pool <= 26) tips += "Mix UPPER and lower case, numbers and symbols"
        tips += "Use a different password for every account, or a password manager"
        tips += "Turn on two-step verification wherever possible"

        bits = bits.coerceAtLeast(0.0)
        val strength = when {
            bits < 28 -> Strength.VERY_WEAK
            bits < 40 -> Strength.WEAK
            bits < 60 -> Strength.FAIR
            bits < 80 -> Strength.STRONG
            else -> Strength.VERY_STRONG
        }
        val score = (bits / 100.0 * 100).toInt().coerceIn(0, 100)
        return Result(score, strength, crackTime(bits), problems, tips)
    }

    /** Offline attack at 10 billion guesses per second (modern GPU rig). */
    private fun crackTime(bits: Double): String {
        val seconds = 2.0.pow(bits) / 2 / 1e10
        return when {
            seconds < 1 -> "instantly"
            seconds < 60 -> "${seconds.toInt()} seconds"
            seconds < 3600 -> "${(seconds / 60).toInt()} minutes"
            seconds < 86400 -> "${(seconds / 3600).toInt()} hours"
            seconds < 86400 * 365.0 -> "${(seconds / 86400).toInt()} days"
            seconds < 86400 * 365.0 * 1000 -> "${(seconds / 86400 / 365).toInt()} years"
            else -> "centuries"
        }
    }

    fun sha1(s: String): String = MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02X".format(it) }

    /**
     * Have I Been Pwned "Pwned Passwords" range API (free, no key). Returns how many times the password
     * appears in known data breaches. Only the first 5 hex characters of the SHA-1 hash are sent.
     */
    fun breachCount(pw: String, client: OkHttpClient = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()): Int {
        val hash = sha1(pw)
        val req = Request.Builder().url("https://api.pwnedpasswords.com/range/${hash.take(5)}")
            .header("Add-Padding", "true").header("User-Agent", "Kavach-Scanner").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Breach service HTTP ${resp.code}")
            val suffix = hash.drop(5)
            resp.body?.string().orEmpty().lineSequence().forEach { line ->
                val (s, c) = line.trim().split(':').let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "0") }
                if (s.equals(suffix, ignoreCase = true)) return c.trim().toIntOrNull() ?: 0
            }
        }
        return 0
    }
}
