package com.kavach.scanner.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavach.scanner.data.Tips
import com.kavach.scanner.ui.components.Chip
import com.kavach.scanner.ui.components.IconBadge
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KavachLogo
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K

data class HomeStats(val scans: Int, val dangerous: Int, val suspicious: Int, val deviceScore: Int?)

@Composable
fun HomeScreen(
    stats: HomeStats,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onScanText: (String) -> Unit,
    onQr: () -> Unit,
    onScreenshot: () -> Unit,
    onDevice: () -> Unit,
    onPassword: () -> Unit,
    onReport: () -> Unit,
    onGuide: () -> Unit,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(K.bg).verticalScroll(rememberScrollState())) {
        // ---------- Hero ----------
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF7A3D), K.orange, K.orangeDeep)))
                .statusBarsPadding()
                .padding(bottom = 48.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text("KAVACH", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 4.sp)
                        Text("Cyber Shield for India", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Theme", tint = Color.White)
                    }
                }
                KavachLogo(LogoState.IDLE, size = 150.dp, onDark = true, modifier = Modifier.padding(top = 4.dp))
                Text("Scan before you click", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip(Icons.Filled.GppGood, "Protection on")
                    HeroChip(Icons.Filled.PhonelinkLock, stats.deviceScore?.let { "Phone score $it" } ?: "Check phone", onDevice)
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp).offset(y = (-32).dp)) {
            // ---------- Scan box ----------
            KCard(Modifier.fillMaxWidth().enter(0)) {
                Text("Check a link or message", style = MaterialTheme.typography.titleMedium, color = K.text)
                Text("Paste a URL, SMS, e-mail or WhatsApp message", color = K.textDim, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("e.g. sbi-kyc-update.xyz or \"Your account will be blocked today…\"", color = K.textDim.copy(alpha = 0.7f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = K.orange, unfocusedBorderColor = K.outline, cursorColor = K.orange,
                        focusedContainerColor = K.bg, unfocusedContainerColor = K.bg,
                        focusedTextColor = K.text, unfocusedTextColor = K.text,
                    ),
                    trailingIcon = {
                        if (input.isEmpty()) IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { input = it }
                        }) { Icon(Icons.Filled.ContentPaste, "Paste", tint = K.orange) }
                        else IconButton(onClick = { input = "" }) { Icon(Icons.Filled.Close, "Clear", tint = K.textDim) }
                    },
                )
                Spacer(Modifier.height(12.dp))
                ShineButton("SCAN NOW", Icons.Filled.Shield, Modifier.fillMaxWidth(), enabled = input.isNotBlank()) { onScanText(input) }
            }

            // ---------- Tools ----------
            SectionTitle("Security tools")
            val tools = listOf(
                Tool(Icons.Filled.QrCodeScanner, "Scan QR", "Camera or gallery", K.green, onQr),
                Tool(Icons.Filled.Screenshot, "Screenshot", "SMS & WhatsApp", K.purple, onScreenshot),
                Tool(Icons.Filled.PhonelinkLock, "Phone check", "Find risky apps", K.blue, onDevice),
                Tool(Icons.Filled.Key, "Password", "Strength & leaks", K.amber, onPassword),
                Tool(Icons.Filled.ReportProblem, "Report fraud", "1930 & cybercrime", K.red, onReport),
                Tool(Icons.AutoMirrored.Filled.MenuBook, "Learn", "Scams in India", K.orange, onGuide),
            )
            tools.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { i, t -> ToolTile(t, Modifier.weight(1f).enter(1 + row * 2 + i)) }
                }
            }

            // ---------- Stats ----------
            SectionTitle("Your activity", action = "History", onAction = onHistory)
            KCard(Modifier.fillMaxWidth().enter(7)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("${stats.scans}", "Scans", K.orange)
                    Stat("${stats.dangerous}", "Threats blocked", K.red)
                    Stat("${stats.suspicious}", "Suspicious", K.amber)
                }
            }

            // ---------- Tip ----------
            SectionTitle("Tip of the day")
            KCard(Modifier.fillMaxWidth().enter(8), accent = K.amber) {
                Row(verticalAlignment = Alignment.Top) {
                    IconBadge(Icons.Filled.Lightbulb, K.amber, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(Tips.today(), color = K.text, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ---------- Emergency ----------
            Spacer(Modifier.height(16.dp))
            EmergencyBanner { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) }

            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GppGood, null, tint = K.green, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Private by design: checks run on your phone and links are never opened.", color = K.textDim, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private data class Tool(val icon: ImageVector, val title: String, val sub: String, val color: Color, val onClick: () -> Unit)

@Composable
private fun ToolTile(t: Tool, modifier: Modifier) {
    KCard(modifier.clip(RoundedCornerShape(20.dp)).pressable(t.onClick), padding = 14.dp) {
        IconBadge(t.icon, t.color, 46.dp)
        Spacer(Modifier.height(12.dp))
        Text(t.title, fontWeight = FontWeight.Bold, color = K.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(t.sub, color = K.textDim, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HeroChip(icon: ImageVector, text: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f))
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
            .pressable(onCall).padding(16.dp),
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
        Chip("CALL", Color.White)
    }
}
