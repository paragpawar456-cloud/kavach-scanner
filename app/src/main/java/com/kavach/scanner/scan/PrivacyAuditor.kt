package com.kavach.scanner.scan

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

data class PermGroup(val id: String, val label: String, val why: String, val perms: Set<String>)
data class AppPerm(val name: String, val pkg: String, val system: Boolean)
data class PrivacyReport(val groups: List<Pair<PermGroup, List<AppPerm>>>)

/** Lists which installed apps currently HOLD (granted) sensitive permissions. Runs on the phone only. */
object PrivacyAuditor {

    val groups = listOf(
        PermGroup("sms", "SMS & OTP", "Can read your messages, including bank OTPs.", setOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS")),
        PermGroup("mic", "Microphone", "Can record audio.", setOf("android.permission.RECORD_AUDIO")),
        PermGroup("camera", "Camera", "Can take photos and video.", setOf("android.permission.CAMERA")),
        PermGroup("location", "Location", "Knows where you are.", setOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")),
        PermGroup("bglocation", "Location all the time", "Tracks you even when the app is closed.", setOf("android.permission.ACCESS_BACKGROUND_LOCATION")),
        PermGroup("contacts", "Contacts", "Can read everyone in your phonebook (loan apps misuse this).", setOf("android.permission.READ_CONTACTS")),
        PermGroup("calls", "Call logs & calls", "Can see who you call or make calls.", setOf("android.permission.READ_CALL_LOG", "android.permission.CALL_PHONE", "android.permission.READ_PHONE_STATE")),
        PermGroup("media", "Photos & files", "Can read your photos, documents and downloads.", setOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_EXTERNAL_STORAGE")),
        PermGroup("nearby", "Nearby devices", "Can find and connect to Bluetooth devices.", setOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT")),
    )

    fun run(ctx: Context, includeSystem: Boolean = false): PrivacyReport {
        val pm = ctx.packageManager
        val pkgs: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= 33) pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            else @Suppress("DEPRECATION") pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            emptyList()
        }
        val granted = pkgs.mapNotNull { pi ->
            val ai = pi.applicationInfo ?: return@mapNotNull null
            if (pi.packageName == ctx.packageName) return@mapNotNull null
            val system = ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (system && !includeSystem) return@mapNotNull null
            val req = pi.requestedPermissions ?: return@mapNotNull null
            val flags = pi.requestedPermissionsFlags ?: return@mapNotNull null
            val set = req.indices.filter { flags[it] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }.map { req[it] }.toSet()
            AppPerm(pm.getApplicationLabel(ai).toString(), pi.packageName, system) to set
        }
        return PrivacyReport(groups.map { g ->
            g to granted.filter { (_, s) -> s.any { it in g.perms } }.map { it.first }.sortedBy { it.name.lowercase() }
        })
    }
}
