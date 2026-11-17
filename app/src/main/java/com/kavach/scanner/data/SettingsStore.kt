package com.kavach.scanner.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kavach.core.model.ScanOptions

/**
 * Stores the user's settings and their own API keys, encrypted with a key held in the Android Keystore
 * (OWASP MASVS-STORAGE-1). Keys are never hardcoded in the app and never leave the phone except to the
 * matching API provider over HTTPS. Backups are disabled in the manifest so keys aren't copied to the cloud.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "kavach_secure_settings", master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Very rare Keystore failures on some devices: fall back to app-private storage rather than crash.
        context.getSharedPreferences("kavach_settings", Context.MODE_PRIVATE)
    }

    var onlineChecks: Boolean
        get() = prefs.getBoolean(K_ONLINE, true)
        set(v) = prefs.edit().putBoolean(K_ONLINE, v).apply()
    var followRedirects: Boolean
        get() = prefs.getBoolean(K_REDIRECTS, true)
        set(v) = prefs.edit().putBoolean(K_REDIRECTS, v).apply()
    var domainAge: Boolean
        get() = prefs.getBoolean(K_AGE, true)
        set(v) = prefs.edit().putBoolean(K_AGE, v).apply()
    var safeBrowsingKey: String
        get() = prefs.getString(K_GSB, "").orEmpty()
        set(v) = prefs.edit().putString(K_GSB, v.trim()).apply()
    var virusTotalKey: String
        get() = prefs.getString(K_VT, "").orEmpty()
        set(v) = prefs.edit().putString(K_VT, v.trim()).apply()
    var urlhausKey: String
        get() = prefs.getString(K_UH, "").orEmpty()
        set(v) = prefs.edit().putString(K_UH, v.trim()).apply()

    /** "LIGHT", "DARK" or "SYSTEM". */
    var theme: String
        get() = prefs.getString(K_THEME, "LIGHT") ?: "LIGHT"
        set(v) = prefs.edit().putString(K_THEME, v).apply()
    var saveHistory: Boolean
        get() = prefs.getBoolean(K_HISTORY, true)
        set(v) = prefs.edit().putBoolean(K_HISTORY, v).apply()

    /** Encrypted on-device scan history (JSON). Never synced or uploaded. */
    var historyJson: String
        get() = prefs.getString(K_HISTORY_DATA, "[]") ?: "[]"
        set(v) = prefs.edit().putString(K_HISTORY_DATA, v).apply()

    fun toOptions() = ScanOptions(
        onlineChecks = onlineChecks,
        followRedirects = followRedirects,
        domainAgeLookup = domainAge,
        googleSafeBrowsingKey = safeBrowsingKey.ifBlank { null },
        virusTotalKey = virusTotalKey.ifBlank { null },
        urlhausKey = urlhausKey.ifBlank { null },
    )

    private companion object {
        const val K_ONLINE = "online_checks"
        const val K_REDIRECTS = "follow_redirects"
        const val K_AGE = "domain_age"
        const val K_GSB = "key_safe_browsing"
        const val K_VT = "key_virustotal"
        const val K_UH = "key_urlhaus"
        const val K_THEME = "theme"
        const val K_HISTORY = "save_history"
        const val K_HISTORY_DATA = "history_data"
    }
}
