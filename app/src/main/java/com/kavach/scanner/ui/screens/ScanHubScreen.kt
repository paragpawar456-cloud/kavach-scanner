package com.kavach.scanner.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Screenshot
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavach.scanner.ui.components.GradientIcon
import com.kavach.scanner.ui.components.HexMesh
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KavachLogo
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K

enum class HubMode { TEXT, CALLER }

/** One place to start any scan: paste a link or message, check a caller/UPI ID, or jump to QR and screenshot. */
@Composable
fun ScanHubScreen(
    initialMode: HubMode,
    onScanText: (String) -> Unit,
    onScanCaller: (String) -> Unit,
    onQr: () -> Unit,
    onScreenshot: () -> Unit,
    onBack: (() -> Unit)?,
) {
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var text by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val caller = mode == HubMode.CALLER

    Column(Modifier.fillMaxSize().background(K.bg).verticalScroll(rememberScrollState())) {
        // Header
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF14213D), Color(0xFF1F2E52)))),
        ) {
            HexMesh(Modifier.matchParentSize(), color = Color(0xFFFF8A50))
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    else Spacer(Modifier.width(16.dp))
                    Text("Threat Scanner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                }
                KavachLogo(if (text.isBlank()) LogoState.IDLE else LogoState.SCANNING, size = 110.dp, onDark = true)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (caller) "Is this caller or UPI ID genuine?" else "Is this link or message safe?",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                )
                Text("10+ independent checks · nothing is opened", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(14.dp))
                // Mode switch
                Row(
                    Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).padding(4.dp),
                ) {
                    ModeTab(Icons.Filled.Link, "Link / Message", !caller, Modifier.weight(1f)) { mode = HubMode.TEXT; text = "" }
                    ModeTab(Icons.Filled.PhoneInTalk, "Caller / UPI", caller, Modifier.weight(1f)) { mode = HubMode.CALLER; text = "" }
                }
            }
        }

        Column(Modifier.padding(16.dp).navigationBarsPadding()) {
            KCard(Modifier.fillMaxWidth().enter(0)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(10_000) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = if (caller) 56.dp else 120.dp),
                    placeholder = {
                        Text(
                            if (caller) "Phone number or UPI ID, e.g. +92 300 1234567 or refund@ybl"
                            else "Paste a link, SMS, WhatsApp or email message…",
                            color = K.textDim,
                        )
                    },
                    singleLine = caller,
                    keyboardOptions = KeyboardOptions(keyboardType = if (caller) KeyboardType.Email else KeyboardType.Text),
                    trailingIcon = if (text.isNotEmpty()) ({ IconButton(onClick = { text = "" }) { Icon(Icons.Filled.Clear, "Clear", tint = K.textDim) } }) else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = K.orange, unfocusedBorderColor = K.outline, cursorColor = K.orange,
                        focusedTextColor = K.text, unfocusedTextColor = K.text,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(K.orangeSoft)
                            .pressable(onClick = { clipboard.getText()?.text?.let { text = it.take(10_000) } })
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.ContentPaste, null, tint = K.orange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paste", color = K.orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(if (caller) "Works offline" else "${text.length} chars", color = K.textDim, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                ShineButton(
                    if (caller) "Check caller / UPI" else "Scan now",
                    Icons.Filled.Radar,
                    Modifier.fillMaxWidth(),
                    enabled = text.isNotBlank(),
                ) { if (caller) onScanCaller(text.trim()) else onScanText(text.trim()) }
            }

            SectionTitle("Other ways to scan")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigOption(Icons.Filled.QrCodeScanner, "Scan QR code", "Camera", K.green, Modifier.weight(1f).enter(1), onQr)
                BigOption(Icons.Filled.Screenshot, "Screenshot", "From gallery", K.purple, Modifier.weight(1f).enter(2), onScreenshot)
            }

            SectionTitle("What we check")
            KCard(Modifier.fillMaxWidth().enter(3)) {
                val items = if (caller) listOf(
                    "International codes used in scam calls (+92, +84, +855…)",
                    "TRAI 1600-series numbers used only by banks and regulators",
                    "Telemarketing 140-series and fake helplines",
                    "UPI IDs that impersonate banks, refunds or KYC",
                    "Where to report: Chakshu (Sanchar Saathi) and 1930",
                ) else listOf(
                    "Look-alike bank and brand websites (typosquatting, Unicode tricks)",
                    "Hidden redirects, short links and fake download pages",
                    "Scam wording: KYC, electricity cut-off, digital arrest, prizes",
                    "UPI collect requests and QR codes that take money",
                    "Domain age and threat databases (optional, online)",
                )
                items.forEach { s ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, null, tint = K.green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(s, color = K.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeTab(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Color.White else Color.Transparent, label = "tab")
    Row(
        modifier.clip(RoundedCornerShape(50)).background(bg).pressable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) Color(0xFFFF5722) else Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) Color(0xFF14213D) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun BigOption(icon: ImageVector, title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(K.surface).border(1.dp, K.outline, RoundedCornerShape(22.dp))
            .pressable(onClick = onClick).padding(16.dp),
    ) {
        GradientIcon(icon, color, 48.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = K.text, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(sub, color = K.textDim, style = MaterialTheme.typography.bodySmall)
    }
}
