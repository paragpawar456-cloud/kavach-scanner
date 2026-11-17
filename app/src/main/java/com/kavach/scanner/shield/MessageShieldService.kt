package com.kavach.scanner.shield

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kavach.core.ScanEngine
import com.kavach.core.model.InputType
import com.kavach.core.model.ScanOptions
import com.kavach.core.model.Verdict
import com.kavach.scanner.MainActivity
import com.kavach.scanner.R
import com.kavach.scanner.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Real-time Message Shield. When the user grants "Notification access", Kavach reads the text of new
 * SMS / WhatsApp / Telegram / e-mail notifications and runs the OFFLINE checks on them (no internet,
 * nothing stored). If a message looks like a scam it shows a warning notification before the user taps the link.
 */
class MessageShieldService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine = ScanEngine()
    private val recent = ArrayDeque<Int>()

    private val watched = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging", "com.samsung.android.messaging",
        "com.android.mms", "com.android.messaging", "org.telegram.messenger", "com.google.android.gm", "com.truecaller",
        "com.facebook.orca", "com.instagram.android", "com.microsoft.office.outlook", "com.oneplus.mms", "com.miui.smsextra",
        "com.jio.messages", "com.sonyericsson.conversations", "com.coloros.mms", "com.vivo.message", "in.amazon.mShop.android.shopping",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!SettingsStore(this).messageShield) return
        val pkg = sbn.packageName
        if (pkg == packageName) return
        val defaultSms = runCatching { Telephony.Sms.getDefaultSmsPackage(this) }.getOrNull()
        if (pkg !in watched && pkg != defaultSms) return
        val n = sbn.notification ?: return
        if (n.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) return
        val e = n.extras
        val text = listOfNotNull(
            e.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT) ?: e.getCharSequence(android.app.Notification.EXTRA_TEXT),
            e.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES)?.joinToString("\n"),
        ).joinToString("\n").trim()
        if (text.length < 12) return
        val hash = text.hashCode()
        synchronized(recent) {
            if (hash in recent) return
            recent.addLast(hash); if (recent.size > 50) recent.removeFirst()
        }
        val sender = e.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        scope.launch {
            val r = runCatching { engine.scan(text, InputType.MESSAGE, ScanOptions(onlineChecks = false)) }.getOrNull() ?: return@launch
            val alert = r.verdict == Verdict.DANGEROUS || (r.verdict == Verdict.SUSPICIOUS && r.urls.isNotEmpty())
            if (alert) {
                SettingsStore(this@MessageShieldService).shieldBlocked = SettingsStore(this@MessageShieldService).shieldBlocked + 1
                warn(appName(pkg), sender, text, r.verdict, r.findings.firstOrNull { it.risk > 0 }?.title ?: r.summary)
            }
        }
    }

    private fun appName(pkg: String) = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault("a message")

    private fun warn(app: String, sender: String, text: String, verdict: Verdict, reason: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(this)
        val open = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(this, text.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = if (verdict == Verdict.DANGEROUS) "⚠ Scam alert in $app" else "Suspicious message in $app"
        val notif = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText("${if (sender.isNotBlank()) "$sender: " else ""}$reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${if (sender.isNotBlank()) "From $sender\n" else ""}$reason\n\nDon't tap links, pay or share OTP. Tap here for the full Kavach report."))
            .setColor(0xFFFF5722.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(text.hashCode(), notif) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "kavach_shield"

        fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = ctx.getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel(CHANNEL) == null) {
                    nm.createNotificationChannel(NotificationChannel(CHANNEL, "Scam alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Warnings when a received message looks like a scam"
                    })
                }
            }
        }

        fun hasAccess(ctx: Context): Boolean = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

        fun accessSettingsIntent(ctx: Context): Intent =
            if (Build.VERSION.SDK_INT >= 30) Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(ctx, MessageShieldService::class.java).flattenToString())
            else Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
    }
}
