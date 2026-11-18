package com.kavach.scanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kavach.scanner.data.SettingsStore
import com.kavach.scanner.shield.MessageShieldService
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.KavachLogo
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.theme.K

@Composable
fun ShieldScreen(settings: SettingsStore, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var access by remember { mutableStateOf(MessageShieldService.hasAccess(ctx)) }
    var notify by remember { mutableStateOf(notifGranted(ctx)) }
    var on by remember { mutableStateOf(settings.messageShield) }
    var blocked by remember { mutableIntStateOf(settings.shieldBlocked) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = MessageShieldService.hasAccess(ctx); notify = notifGranted(ctx); blocked = settings.shieldBlocked
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notify = it }
    val active = access && notify && on

    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Message Shield", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            KavachLogo(if (active) LogoState.SAFE else LogoState.IDLE, size = 150.dp)
            Text(if (active) "Real-time protection is ON" else "Real-time protection is OFF", style = MaterialTheme.typography.titleLarge, color = if (active) K.green else K.text)
            Text(
                "Kavach checks new SMS, WhatsApp, Telegram and e-mail messages the moment they arrive and warns you before you tap a scam link.",
                color = K.textDim, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
            )
            if (blocked > 0) Text("$blocked scam message(s) caught so far", color = K.orange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

            SectionTitle("Set up (one time)")
            KCard(Modifier.fillMaxWidth()) {
                Step(1, "Allow notification access", "Lets Kavach read incoming message previews.", access) {
                    runCatching { ctx.startActivity(MessageShieldService.accessSettingsIntent(ctx)) }
                }
                Step(2, "Allow alerts", "So Kavach can warn you.", notify) {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, null, tint = K.orange)
                    Spacer(Modifier.width(12.dp))
                    Text("Shield switched on", color = K.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Switch(on, { on = it; settings.messageShield = it }, colors = SwitchDefaults.colors(checkedTrackColor = K.orange, checkedThumbColor = Color.White))
                }
            }
            if (!access) {
                Spacer(Modifier.height(14.dp))
                ShineButton("TURN ON PROTECTION", Icons.Filled.NotificationsActive, Modifier.fillMaxWidth()) {
                    runCatching { ctx.startActivity(MessageShieldService.accessSettingsIntent(ctx)) }
                }
                Text("On the next screen, find Kavach and switch it on. Android shows a standard warning for any app with this access.", color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }

            SectionTitle("Your privacy")
            KCard(Modifier.fillMaxWidth(), accent = K.green) {
                listOf(
                    "Also guards new installs: you get a warning if a newly installed app looks like a banking trojan, spyware or a remote-control app.",
                    "Only message apps are checked (SMS, WhatsApp, Telegram, Gmail, Truecaller…).",
                    "Checks run offline on your phone. Messages are never stored, uploaded or shared.",
                    "You can switch it off here or in Android settings at any time.",
                ).forEach { Text("• $it", color = K.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Step(n: Int, title: String, sub: String, done: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (done) K.green else K.textDim)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$n. $title", color = K.text, fontWeight = FontWeight.SemiBold)
            Text(sub, color = K.textDim, style = MaterialTheme.typography.bodySmall)
        }
        if (!done) androidx.compose.material3.TextButton(onClick = onClick) { Text("Allow", color = K.orange, fontWeight = FontWeight.Bold) }
    }
}

private fun notifGranted(ctx: android.content.Context) =
    Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
