package com.kavach.scanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kavach.scanner.scan.CheckLevel
import com.kavach.scanner.scan.DeviceChecker
import com.kavach.scanner.scan.DeviceReport
import com.kavach.scanner.ui.components.IconBadge
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.KavachLogo
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.ScoreRing
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.theme.K

@Composable
fun DeviceScreen(report: DeviceReport?, running: Boolean, onRun: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { if (report == null) onRun() }

    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Phone security check", onBack) {
            IconButton(onClick = onRun, enabled = !running) { Icon(Icons.Filled.Refresh, "Re-scan", tint = K.orange) }
        }
        if (running || report == null) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                KavachLogo(LogoState.SCANNING, size = 190.dp)
                Spacer(Modifier.height(20.dp))
                Text("Scanning your phone…", style = MaterialTheme.typography.titleLarge, color = K.text)
                Text("Screen lock · updates · apps · permissions · root", color = K.textDim, textAlign = TextAlign.Center)
                Text("Nothing leaves your phone.", color = K.green, modifier = Modifier.padding(top = 8.dp))
            }
            return@Column
        }
        val color = when {
            report.score >= 80 -> K.green
            report.score >= 55 -> K.amber
            else -> K.red
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            KCard(Modifier.fillMaxWidth().padding(top = 12.dp).enter(0), accent = color) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(report.score, color, "SCORE", size = 130.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when { report.score >= 80 -> "Well protected"; report.score >= 55 -> "Needs attention"; else -> "At risk" },
                            style = MaterialTheme.typography.headlineMedium, color = color,
                        )
                        val bad = report.checks.count { it.level == CheckLevel.BAD }
                        val warn = report.checks.count { it.level == CheckLevel.WARN }
                        Text("$bad problem(s), $warn warning(s)", color = K.textDim)
                    }
                }
            }
            SectionTitle("Checks")
            report.checks.forEachIndexed { i, c ->
                val (icon, col) = when (c.level) {
                    CheckLevel.GOOD -> Icons.Filled.CheckCircle to K.green
                    CheckLevel.WARN -> Icons.Filled.Warning to K.amber
                    CheckLevel.BAD -> Icons.Filled.Error to K.red
                }
                KCard(Modifier.fillMaxWidth().padding(vertical = 5.dp).enter(i + 1), accent = if (c.level == CheckLevel.GOOD) null else col) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(icon, col, 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.title, fontWeight = FontWeight.SemiBold, color = K.text)
                            Text(c.detail, color = K.textDim, style = MaterialTheme.typography.bodyMedium)
                            if (c.fixIntent != null && c.fixLabel != null) {
                                OutlinedButton(
                                    onClick = { runCatching { context.startActivity(c.fixIntent) } },
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 6.dp),
                                ) { Text(c.fixLabel, color = K.orange) }
                            }
                        }
                    }
                }
            }
            if (report.riskyApps.isNotEmpty()) {
                SectionTitle("Apps from outside an app store")
                report.riskyApps.forEach { a ->
                    KCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), accent = if (a.high) K.red else null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.Android, if (a.high) K.red else K.amber, 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.name, fontWeight = FontWeight.SemiBold, color = K.text)
                                Text(a.reasons.joinToString(" · "), color = if (a.high) K.red else K.textDim, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { runCatching { context.startActivity(DeviceChecker.appDetails(a.pkg)) } }, shape = RoundedCornerShape(12.dp)) {
                                Text("Manage", color = K.orange)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            ShineButton("SCAN AGAIN", Icons.Filled.PhonelinkLock, Modifier.fillMaxWidth()) { onRun() }
            Text(
                "Based on CIS mobile benchmarks, OWASP MASVS and CERT-In advisories. Android doesn't let apps read every setting (e.g. Play Protect), so also run Play Protect regularly.",
                color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}
