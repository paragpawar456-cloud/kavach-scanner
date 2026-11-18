package com.kavach.scanner.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavach.scanner.data.Attacks
import com.kavach.scanner.data.Tips
import com.kavach.scanner.ui.components.GradientIcon
import com.kavach.scanner.ui.components.HexMesh
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KProgress
import com.kavach.scanner.ui.components.LiveDot
import com.kavach.scanner.ui.components.PulseAction
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.SegmentedGauge
import com.kavach.scanner.ui.components.StatusPill
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K
import java.util.Calendar

data class HomeStats(
    val scans: Int,
    val dangerous: Int,
    val suspicious: Int,
    val deviceScore: Int?,
    val shieldOn: Boolean = false,
    val accountsPct: Int = 0,
    val copiedLink: Boolean = false,
) {
    /** Overall protection: phone posture 40%, real-time shield 30%, account hardening 30%. */
    val securityScore: Int get() = ((deviceScore ?: 50) * 0.4 + (if (shieldOn) 100 else 0) * 0.3 + accountsPct * 0.3).toInt().coerceIn(0, 100)
}

class HomeActions(
    val onToggleTheme: () -> Unit = {},
    val onScanHub: () -> Unit = {},
    val onPasteScan: () -> Unit = {},
    val onLink: () -> Unit = {},
    val onQr: () -> Unit = {},
    val onScreenshot: () -> Unit = {},
    val onCaller: () -> Unit = {},
    val onDevice: () -> Unit = {},
    val onPrivacy: () -> Unit = {},
    val onPassword: () -> Unit = {},
    val onReport: () -> Unit = {},
    val onGuide: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onShield: () -> Unit = {},
    val onAccounts: () -> Unit = {},
    val onAttacks: () -> Unit = {},
)

@Composable
fun HomeScreen(stats: HomeStats, isDark: Boolean, a: HomeActions) {
    val context = LocalContext.current
    val score = stats.securityScore
    val (statusText, statusColor) = when {
        score >= 75 -> "Strongly protected" to K.green
        score >= 45 -> "Partly protected" to K.amber
        else -> "Needs attention" to K.red
    }

    Column(Modifier.fillMaxSize().background(K.bg).verticalScroll(rememberScrollState())) {
        // ================= HERO =================
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF8A50), K.orange, Color(0xFFD84315)))),
        ) {
            HexMesh(Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 26.dp)) {
                // Top bar
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Shield, null, tint = K.orange, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("KAVACH", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 3.sp)
                        Text(greeting() + " · stay cyber-safe", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = a.onToggleTheme) { Icon(if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Theme", tint = Color.White) }
                }
                // Score + status
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SegmentedGauge(score, size = 156.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiveDot(Color.White, 6.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(statusText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        HeroStatus(Icons.Filled.NotificationsActive, "Message Shield", if (stats.shieldOn) "ON" else "OFF", stats.shieldOn, a.onShield)
                        HeroStatus(Icons.Filled.PhonelinkLock, "Phone security", stats.deviceScore?.let { "$it" } ?: "Scan", (stats.deviceScore ?: 0) >= 75, a.onDevice)
                        HeroStatus(Icons.Filled.ManageAccounts, "Accounts", "${stats.accountsPct}%", stats.accountsPct >= 70, a.onAccounts)
                    }
                }
                // Full check button
                Row(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color.White).pressable(onClick = a.onDevice).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, null, tint = K.orange)
                    Spacer(Modifier.width(10.dp))
                    Text("Run full security check", color = Color(0xFF1B1F24), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = K.orange)
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            // ================= COPIED LINK =================
            AnimatedVisibility(stats.copiedLink, enter = fadeIn() + expandVertically()) {
                Row(
                    Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.navy)
                        .pressable(onClick = a.onPasteScan).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.ContentPaste, null, tint = K.orange)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("You copied a link", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Check it before you open it", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                    }
                    StatusPill("SCAN", K.orange, filled = true)
                }
            }

            // ================= QUICK SCAN =================
            SectionTitle("Quick scan")
            KCard(Modifier.fillMaxWidth().enter(0), padding = 12.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PulseAction(Icons.Filled.Link, "Link / SMS", K.orange, delayMs = 0, onClick = a.onLink)
                    PulseAction(Icons.Filled.QrCodeScanner, "QR code", K.green, delayMs = 500, onClick = a.onQr)
                    PulseAction(Icons.Filled.Screenshot, "Screenshot", K.purple, delayMs = 1000, onClick = a.onScreenshot)
                    PulseAction(Icons.Filled.PhoneInTalk, "Caller/UPI", K.blue, delayMs = 1500, onClick = a.onCaller)
                }
            }

            // ================= MODULES =================
            SectionTitle("Protection modules")
            val modules = listOf(
                Module(Icons.Filled.NotificationsActive, "Message Shield", "Real-time scam alerts for SMS & WhatsApp", K.orange, if (stats.shieldOn) "ON" else "OFF", if (stats.shieldOn) K.green else K.red, a.onShield),
                Module(Icons.Filled.PhonelinkLock, "Phone & spyware check", "Risky apps, root, updates, screen lock", K.blue, stats.deviceScore?.let { "$it/100" } ?: "RUN", if ((stats.deviceScore ?: 0) >= 75) K.green else K.amber, a.onDevice),
                Module(Icons.Filled.Wifi, "Network guard", "Fake Wi-Fi, spy certificates, proxy, DNS", Color(0xFF0EA5E9), "CHECK", K.blue, a.onDevice),
                Module(Icons.Filled.PrivacyTip, "Privacy audit", "Who can use your camera, mic, SMS, location", K.purple, "VIEW", K.purple, a.onPrivacy),
                Module(Icons.Filled.ManageAccounts, "Secure my accounts", "WhatsApp, Google, Aadhaar, SIM, UPI", K.green, "${stats.accountsPct}%", if (stats.accountsPct >= 70) K.green else K.amber, a.onAccounts, stats.accountsPct / 100f),
                Module(Icons.Filled.Key, "Password check", "Strength and data-breach leaks", K.amber, "TEST", K.amber, a.onPassword),
            )
            modules.forEachIndexed { i, m -> ModuleRow(m, Modifier.enter(1 + i)) }

            // ================= TRENDING =================
            SectionTitle("Trending scams in India", action = "See all", onAction = a.onAttacks)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                trendingIds.mapNotNull { id -> Attacks.all.firstOrNull { it.id == id } }.forEachIndexed { i, at ->
                    val c = listOf(K.red, K.purple, K.orange, K.blue, K.amber, K.green)[i % 6]
                    Column(
                        Modifier.width(220.dp).height(150.dp).shadow(3.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.75f))))
                            .pressable(onClick = a.onAttacks).padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BugReport, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(at.category.uppercase(), color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(at.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(at.how, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ================= STATS =================
            SectionTitle("Your activity", action = "History", onAction = a.onHistory)
            KCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("${stats.scans}", "Scans", K.orange)
                    Stat("${stats.dangerous}", "Threats", K.red)
                    Stat("${stats.suspicious}", "Suspicious", K.amber)
                }
            }

            // ================= TIP =================
            SectionTitle("Tip of the day")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(K.orangeSoft).padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Lightbulb, null, tint = K.orange)
                Spacer(Modifier.width(10.dp))
                Text(Tips.today(), color = K.text, style = MaterialTheme.typography.bodyMedium)
            }

            // ================= MORE =================
            SectionTitle("More tools")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniTile(Icons.Filled.ReportProblem, "Report fraud", K.red, Modifier.weight(1f), a.onReport)
                MiniTile(Icons.Filled.BugReport, "Attack library", K.purple, Modifier.weight(1f), a.onAttacks)
                MiniTile(Icons.AutoMirrored.Filled.MenuBook, "Learn & quiz", K.orange, Modifier.weight(1f), a.onGuide)
            }

            Spacer(Modifier.height(16.dp))
            EmergencyBanner { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) }
            Text(
                "Private by design: all checks run on your phone. Links are inspected, never opened.",
                color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

private val trendingIds = listOf("digital_arrest", "apk", "investment", "quishing", "callforward", "whatsapp")

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun HeroStatus(icon: ImageVector, label: String, value: String, good: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.16f))
            .pressable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Box(Modifier.clip(RoundedCornerShape(50)).background(if (good) Color.White else Color(0x33000000)).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(value, color = if (good) Color(0xFF16A34A) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private data class Module(
    val icon: ImageVector, val title: String, val sub: String, val color: Color,
    val status: String, val statusColor: Color, val onClick: () -> Unit, val progress: Float? = null,
)

@Composable
private fun ModuleRow(m: Module, modifier: Modifier) {
    KCard(modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(20.dp)).pressable(onClick = m.onClick), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientIcon(m.icon, m.color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, fontWeight = FontWeight.Bold, color = K.text)
                Text(m.sub, color = K.textDim, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (m.progress != null) { Spacer(Modifier.height(6.dp)); KProgress(m.progress, m.statusColor) }
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(m.status, m.statusColor)
        }
    }
}

@Composable
private fun MiniTile(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    KCard(modifier.clip(RoundedCornerShape(20.dp)).pressable(onClick = onClick), padding = 12.dp) {
        GradientIcon(icon, color, 40.dp)
        Spacer(Modifier.height(8.dp))
        Text(label, color = K.text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = color)
        Text(label, color = K.textDim, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EmergencyBanner(onCall: () -> Unit) {
    val t = rememberInfiniteTransition(label = "sos")
    val pulse by t.animateFloat(1f, 1.15f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFB91C1C))))
            .pressable(onClick = onCall).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).scale(pulse).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Phone, null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Lost money to a scam?", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Call 1930 now. The first hour matters most.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
        StatusPill("CALL", Color.White, filled = false)
    }
}
