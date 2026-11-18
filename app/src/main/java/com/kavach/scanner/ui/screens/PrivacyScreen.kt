package com.kavach.scanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavach.scanner.scan.DeviceChecker
import com.kavach.scanner.scan.PermGroup
import com.kavach.scanner.scan.PrivacyReport
import com.kavach.scanner.ui.components.GradientIcon
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.KavachLogo
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.components.StatusPill
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K

/** Shows which installed apps can use the camera, mic, SMS, location and more (OWASP MASVS-PRIVACY, DPDP Act 2023 data minimisation). */
@Composable
fun PrivacyScreen(report: PrivacyReport?, running: Boolean, onRun: () -> Unit, onBack: () -> Unit) {
    LaunchedEffect(Unit) { if (report == null && !running) onRun() }
    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Privacy audit", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            KCard(Modifier.fillMaxWidth(), padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KavachLogo(if (running) LogoState.SCANNING else LogoState.IDLE, size = 84.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (running) "Auditing apps…" else "Who can watch you?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = K.text)
                        Text(
                            "Apps that currently have access to your camera, microphone, messages and location. Remove access you don't need.",
                            color = K.textDim, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ShineButton(if (report == null) "Start audit" else "Audit again", Icons.Filled.PrivacyTip, Modifier.fillMaxWidth(), enabled = !running, onClick = onRun)
            }
            if (report != null && !running) {
                val total = report.groups.sumOf { it.second.size }
                Text(
                    "$total permission grants found across ${report.groups.flatMap { it.second }.map { it.pkg }.distinct().size} apps you installed. Tap a group to see the apps, then tap an app to change its permissions.",
                    color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp),
                )
                report.groups.forEachIndexed { i, (g, apps) -> GroupCard(g, apps.map { it.name to it.pkg }, Modifier.enter(i)) }
            }
            Text(
                "Tip: SMS access lets an app read bank OTPs. Only your messaging app should have it.",
                color = K.textDim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            )
        }
    }
}

private fun iconFor(id: String): ImageVector = when (id) {
    "sms" -> Icons.Filled.Sms
    "mic" -> Icons.Filled.Mic
    "camera" -> Icons.Filled.PhotoCamera
    "location" -> Icons.Filled.LocationOn
    "bglocation" -> Icons.Filled.MyLocation
    "contacts" -> Icons.Filled.Contacts
    "calls" -> Icons.Filled.Call
    "media" -> Icons.Filled.PhotoLibrary
    else -> Icons.Filled.Bluetooth
}

private val sensitive = setOf("sms", "bglocation", "mic", "calls")

@Composable
private fun GroupCard(g: PermGroup, apps: List<Pair<String, String>>, modifier: Modifier) {
    val context = LocalContext.current
    var open by rememberSaveable(g.id) { mutableStateOf(false) }
    val color = when {
        apps.isEmpty() -> K.green
        g.id in sensitive && apps.size > 2 -> K.red
        g.id in sensitive -> K.amber
        else -> K.blue
    }
    KCard(modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(20.dp)).pressable(enabled = apps.isNotEmpty(), onClick = { open = !open }), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientIcon(iconFor(g.id), color, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.label, fontWeight = FontWeight.Bold, color = K.text)
                Text(g.why, color = K.textDim, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(if (apps.isEmpty()) "NONE" else if (apps.size == 1) "1 APP" else "${apps.size} APPS", color)
            if (apps.isNotEmpty()) Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = K.textDim)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(top = 8.dp)) {
                apps.forEach { (name, pkg) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .pressable(onClick = { runCatching { context.startActivity(DeviceChecker.appDetails(pkg)) } })
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text(name.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(name, color = K.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Manage", color = K.orange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
