package com.kavach.scanner.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavach.scanner.ui.components.CyberBackground
import com.kavach.scanner.ui.components.CyberCard
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.ShieldLogo
import com.kavach.scanner.ui.theme.KColors

@Composable
fun HomeScreen(
    onScanText: (String) -> Unit,
    onQr: () -> Unit,
    onScreenshot: () -> Unit,
    onGuide: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    CyberBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings", tint = KColors.TextDim) }
            }
            ShieldLogo(LogoState.IDLE, size = 130.dp)
            Spacer(Modifier.height(12.dp))
            Text("KAVACH", style = MaterialTheme.typography.headlineMedium, color = KColors.Text, letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 4)
            Text("Scan before you click", color = KColors.Cyan, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            CyberCard(Modifier.fillMaxWidth(), accent = KColors.Cyan) {
                Text("Check a link or message", style = MaterialTheme.typography.titleMedium)
                Text("Paste a URL, an SMS or a WhatsApp message.", color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    placeholder = { Text("e.g. http://sbi-kyc-update.xyz or \"Your account will be blocked…\"", color = KColors.TextDim.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KColors.Cyan, unfocusedBorderColor = KColors.Outline,
                        focusedContainerColor = Color(0xFF0A1220), unfocusedContainerColor = Color(0xFF0A1220),
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { input = it }
                        }) { Icon(Icons.Filled.ContentPaste, "Paste", tint = KColors.Cyan) }
                    },
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onScanText(input) },
                    enabled = input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KColors.Cyan, contentColor = Color(0xFF001A20)),
                ) {
                    Icon(Icons.Filled.Security, null)
                    Spacer(Modifier.width(8.dp))
                    Text("SCAN NOW", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(Icons.Filled.QrCodeScanner, "Scan QR", "Camera", KColors.Green, Modifier.weight(1f), onQr)
                ActionTile(Icons.Filled.Image, "Screenshot", "SMS / WhatsApp", Color(0xFFB388FF), Modifier.weight(1f), onScreenshot)
                ActionTile(Icons.Filled.MenuBook, "Safety", "Guide & help", KColors.Amber, Modifier.weight(1f), onGuide)
            }

            Spacer(Modifier.height(16.dp))
            CyberCard(Modifier.fillMaxWidth(), accent = KColors.Red) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, null, tint = KColors.Red)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Lost money to a scam?", fontWeight = FontWeight.SemiBold)
                        Text("Call 1930 right away or report at cybercrime.gov.in", color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) }) {
                        Text("CALL", color = KColors.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                Icon(Icons.Filled.VerifiedUser, null, tint = KColors.Green, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Private by design: checks run on your phone. Links are never opened in a browser.",
                    color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    CyberCard(modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), accent = color) {
        Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(sub, color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}
