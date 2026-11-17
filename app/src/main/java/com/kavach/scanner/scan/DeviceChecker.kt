package com.kavach.scanner.scan

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CheckLevel { GOOD, WARN, BAD }

data class DeviceCheck(
    val id: String,
    val title: String,
    val detail: String,
    val level: CheckLevel,
    val penalty: Int,
    val fixLabel: String? = null,
    val fixIntent: Intent? = null,
)

data class RiskyApp(val name: String, val pkg: String, val reasons: List<String>, val high: Boolean)

data class DeviceReport(val score: Int, val checks: List<DeviceCheck>, val riskyApps: List<RiskyApp>)

/**
 * On-device security posture check (no data leaves the phone). Covers the settings that matter most against
 * Indian banking-malware campaigns: screen lock, security updates, accessibility-service abuse, device-admin
 * abuse, sideloaded apps that read SMS/OTP, root and USB debugging. Inspired by CIS mobile benchmarks,
 * OWASP MASVS-RESILIENCE and CERT-In mobile security advisories.
 */
object DeviceChecker {

    private val trustedStores = setOf(
        "com.android.vending", "com.google.android.feedback", "com.sec.android.app.samsungapps", "com.huawei.appmarket",
        "com.xiaomi.market", "com.xiaomi.mipicks", "com.heytap.market", "com.oppo.market", "com.bbk.appstore", "com.vivo.appstore",
        "com.amazon.venezia", "com.oneplus.market", "com.realme.appstore", "com.miui.packageinstaller.market",
    )

    fun run(ctx: Context): DeviceReport {
        val checks = mutableListOf<DeviceCheck>()
        val pm = ctx.packageManager
        val security = Intent(Settings.ACTION_SECURITY_SETTINGS)

        // 1. Screen lock
        val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        checks += if (km.isDeviceSecure) DeviceCheck("lock", "Screen lock is on", "PIN, pattern or password protects your phone and banking apps.", CheckLevel.GOOD, 0)
        else DeviceCheck("lock", "No screen lock", "Anyone who picks up your phone can open WhatsApp, SMS (OTP) and UPI apps. Set a PIN or password.", CheckLevel.BAD, 25, "Set lock", security)

        // 2. Security patch age
        val patch = runCatching { LocalDate.parse(Build.VERSION.SECURITY_PATCH) }.getOrNull()
        if (patch != null) {
            val months = ChronoUnit.MONTHS.between(patch, LocalDate.now())
            val upd = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
            checks += when {
                months <= 3 -> DeviceCheck("patch", "Security updates are recent", "Security patch: ${Build.VERSION.SECURITY_PATCH}.", CheckLevel.GOOD, 0)
                months <= 12 -> DeviceCheck("patch", "Security update is $months months old", "Patch ${Build.VERSION.SECURITY_PATCH}. Check Settings → System update.", CheckLevel.WARN, 10, "Check updates", upd)
                else -> DeviceCheck("patch", "Security updates are over a year old", "Patch ${Build.VERSION.SECURITY_PATCH}. Old phones have known holes that malware uses. Update, or avoid banking on this phone.", CheckLevel.BAD, 20, "Check updates", upd)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            checks += DeviceCheck("os", "Old Android version (${Build.VERSION.RELEASE})", "Android 10 or newer has much better protection against malicious apps.", CheckLevel.WARN, 10)
        }

        // 3. Accessibility services (No.1 abuse vector of banking trojans)
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val acc = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .mapNotNull { it.resolveInfo?.serviceInfo?.applicationInfo }
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != ctx.packageName }
            .distinctBy { it.packageName }
        checks += if (acc.isEmpty()) DeviceCheck("acc", "No apps can read your screen", "No third-party accessibility services are switched on.", CheckLevel.GOOD, 0)
        else DeviceCheck(
            "acc", "${acc.size} app(s) can read and control your screen",
            acc.joinToString { pm.getApplicationLabel(it).toString() } + ". Banking malware uses Accessibility to read OTPs and tap buttons. Turn it off for any app you don't fully trust.",
            CheckLevel.WARN, 15, "Review", Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        )

        // 4. Device admin apps
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admins = dpm.activeAdmins.orEmpty().filter {
            runCatching { pm.getApplicationInfo(it.packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM == 0 }.getOrDefault(false)
        }
        if (admins.isNotEmpty()) {
            checks += DeviceCheck(
                "admin", "${admins.size} app(s) have device-admin rights",
                admins.joinToString { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it.packageName, 0)).toString() }.getOrDefault(it.packageName) } +
                    ". Malware uses this to stop you uninstalling it. Fine for work/MDM or Find My Device apps.",
                CheckLevel.WARN, 10, "Review", security,
            )
        }

        // 5. Developer options / USB debugging
        val adb = Settings.Global.getInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        val dev = Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        checks += when {
            adb -> DeviceCheck("adb", "USB debugging is ON", "A computer or charging kiosk could install apps or copy data. Turn it off unless you are developing apps.", CheckLevel.BAD, 15, "Turn off", Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            dev -> DeviceCheck("dev", "Developer options are on", "Not dangerous by itself, but turn it off if you don't need it.", CheckLevel.WARN, 3, "Open", Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            else -> DeviceCheck("adb", "USB debugging is off", "Good.", CheckLevel.GOOD, 0)
        }

        // 6. Root
        val rooted = Build.TAGS?.contains("test-keys") == true ||
            listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk", "/data/adb/magisk", "/system/sd/xbin/su").any { File(it).exists() }
        checks += if (rooted) DeviceCheck("root", "Phone appears to be rooted", "Rooting removes Android's security walls. Banking apps and UPI may be unsafe.", CheckLevel.BAD, 20)
        else DeviceCheck("root", "No root detected", "Android's built-in protections are intact.", CheckLevel.GOOD, 0)

        // 7. Encryption
        if (dpm.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE || dpm.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED) {
            checks += DeviceCheck("enc", "Storage is not encrypted", "If the phone is lost, data could be read. Set a screen lock to enable encryption.", CheckLevel.BAD, 10, "Open", security)
        } else checks += DeviceCheck("enc", "Storage is encrypted", "Your data is protected if the phone is lost.", CheckLevel.GOOD, 0)

        // 8. Sideloaded apps with dangerous permissions
        val risky = riskyApps(ctx)
        checks += when {
            risky.any { it.high } -> DeviceCheck(
                "apps", "${risky.count { it.high }} app(s) from outside Play Store can read SMS/OTP or control the screen",
                "See the list below. If you don't recognise an app, uninstall it and call your bank.", CheckLevel.BAD, 25,
            )
            risky.isNotEmpty() -> DeviceCheck("apps", "${risky.size} app(s) installed from outside an app store", "Review the list below and remove apps you don't need.", CheckLevel.WARN, 8)
            else -> DeviceCheck("apps", "All apps come from trusted app stores", "No sideloaded apps with risky permissions found.", CheckLevel.GOOD, 0)
        }

        // 9. Play Protect reminder (status cannot be read by apps)
        checks += DeviceCheck("pp", "Keep Google Play Protect on", "Play Store → profile → Play Protect → Scan. It removes known harmful apps automatically.", CheckLevel.GOOD, 0, "Open Play Store", Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms")))

        val score = (100 - checks.sumOf { it.penalty }).coerceIn(0, 100)
        return DeviceReport(score, checks.sortedBy { it.level != CheckLevel.BAD }.sortedBy { it.level == CheckLevel.GOOD }, risky)
    }

    private fun riskyApps(ctx: Context): List<RiskyApp> {
        val pm = ctx.packageManager
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val accPkgs = am.installedAccessibilityServiceList.orEmpty().mapNotNull { it.resolveInfo?.serviceInfo?.packageName }.toSet()
        val pkgs: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= 33) pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            else @Suppress("DEPRECATION") pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            emptyList()
        }
        return pkgs.mapNotNull { pi ->
            val ai = pi.applicationInfo ?: return@mapNotNull null
            if (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) return@mapNotNull null
            if (pi.packageName == ctx.packageName) return@mapNotNull null
            val installer = runCatching {
                if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(pi.packageName).installingPackageName
                else @Suppress("DEPRECATION") pm.getInstallerPackageName(pi.packageName)
            }.getOrNull()
            if (installer != null && installer in trustedStores) return@mapNotNull null
            val perms = pi.requestedPermissions.orEmpty().toSet()
            val reasons = mutableListOf<String>()
            if ("android.permission.RECEIVE_SMS" in perms || "android.permission.READ_SMS" in perms) reasons += "reads SMS / OTP"
            if (pi.packageName in accPkgs) reasons += "screen control"
            if ("android.permission.SYSTEM_ALERT_WINDOW" in perms) reasons += "draws over other apps"
            if ("android.permission.REQUEST_INSTALL_PACKAGES" in perms) reasons += "installs other apps"
            if ("android.permission.READ_CONTACTS" in perms) reasons += "reads contacts"
            if ("android.permission.READ_CALL_LOG" in perms || "android.permission.CALL_PHONE" in perms) reasons += "calls / call log"
            val high = reasons.any { it.startsWith("reads SMS") || it == "screen control" }
            RiskyApp(pm.getApplicationLabel(ai).toString(), pi.packageName, reasons.ifEmpty { listOf("installed from outside an app store") }, high)
        }.sortedByDescending { it.high }
    }

    fun appDetails(pkg: String) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
}
