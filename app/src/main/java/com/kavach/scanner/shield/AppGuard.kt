package com.kavach.scanner.shield

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import com.kavach.core.util.KnownData
import com.kavach.scanner.scan.DeviceChecker

/**
 * New App Guard: inspects an app right after it is installed and flags the capability combinations
 * used by Android banking trojans and stalkerware (MITRE ATT&CK for Mobile T1417, T1517, T1626; CERT-In advisories).
 * Runs only on the phone.
 */
object AppGuard {
    data class Verdict(val name: String, val pkg: String, val danger: Boolean, val reasons: List<String>)

    fun inspect(ctx: Context, pkg: String): Verdict? {
        val pm = ctx.packageManager
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES
        val pi: PackageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
            else @Suppress("DEPRECATION") pm.getPackageInfo(pkg, flags)
        }.getOrNull() ?: return null
        val ai = pi.applicationInfo ?: return null
        val name = pm.getApplicationLabel(ai).toString()
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(pkg).installingPackageName
            else @Suppress("DEPRECATION") pm.getInstallerPackageName(pkg)
        }.getOrNull()
        val sideloaded = installer == null || installer !in DeviceChecker.trustedStores
        val req = pi.requestedPermissions?.toSet().orEmpty()
        val services = pi.services.orEmpty()
        fun svc(perm: String) = services.any { it.permission == perm }

        val reasons = mutableListOf<String>()
        if (req.any { it == "android.permission.READ_SMS" || it == "android.permission.RECEIVE_SMS" }) reasons += "Wants to read your SMS (bank OTPs)"
        if (svc("android.permission.BIND_ACCESSIBILITY_SERVICE")) reasons += "Can control your screen (Accessibility service)"
        if (svc("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")) reasons += "Can read all your notifications"
        if (req.contains("android.permission.SYSTEM_ALERT_WINDOW")) reasons += "Can draw over other apps (fake login screens)"
        if (req.contains("android.permission.REQUEST_INSTALL_PACKAGES")) reasons += "Can install other apps"
        if (req.contains("android.permission.BIND_DEVICE_ADMIN") || svc("android.permission.BIND_DEVICE_ADMIN")) reasons += "Asks to become device admin (hard to uninstall)"
        if (req.contains("android.permission.CALL_PHONE") && req.contains("android.permission.READ_CALL_LOG")) reasons += "Can make calls and read call logs (call forwarding fraud)"
        if (req.contains("android.permission.RECORD_AUDIO") && req.contains("android.permission.ACCESS_BACKGROUND_LOCATION")) reasons += "Can record audio and track location in background"
        val launchable = pm.getLaunchIntentForPackage(pkg) != null
        if (!launchable) reasons += "Has no app icon (can hide after install)"
        val remote = KnownData.remoteAccessApps.any { name.lowercase().contains(it) || pkg.lowercase().contains(it.replace(" ", "")) }
        if (remote) reasons.add(0, "Screen-sharing / remote-control app: scammers use these to empty bank accounts")

        val strong = reasons.count { !it.startsWith("Can install") }
        val danger = remote || (sideloaded && strong >= 2) || (sideloaded && reasons.any { it.contains("SMS") || it.contains("Accessibility") })
        val worth = danger || (sideloaded && reasons.isNotEmpty())
        if (!worth) return null
        if (sideloaded) reasons.add(0, "Installed from outside the Play Store" + (installer?.let { " (by $it)" } ?: ""))
        return Verdict(name, pkg, danger, reasons)
    }
}
