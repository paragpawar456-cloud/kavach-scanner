package com.kavach.scanner.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kavach.scanner.data.SettingsStore
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.theme.K
import com.kavach.scanner.ui.theme.ThemeMode

@Composable
fun SettingsScreen(settings: SettingsStore, theme: ThemeMode, onTheme: (ThemeMode) -> Unit, onClearHistory: () -> Unit) {
    var online by remember { mutableStateOf(settings.onlineChecks) }
    var redirects by remember { mutableStateOf(settings.followRedirects) }
    var age by remember { mutableStateOf(settings.domainAge) }
    var history by remember { mutableStateOf(settings.saveHistory) }

    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Settings & privacy", null)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
            SectionTitle("Appearance")
            KCard(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "Auto").forEach { (m, l) ->
                        FilterChip(
                            selected = theme == m, onClick = { onTheme(m) }, label = { Text(l) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = K.orange, selectedLabelColor = Color.White, labelColor = K.text),
                        )
                    }
                }
            }

            SectionTitle("Online checks")
            KCard(Modifier.fillMaxWidth()) {
                Toggle("Use online checks", "Master switch. Off = 100% offline analysis, nothing leaves your phone.", online) { online = it; settings.onlineChecks = it }
                Toggle("Reveal hidden destination", "Follows short links and redirects to show where they really go. Fetches page text only: no JavaScript, no cookies.", redirects, online) { redirects = it; settings.followRedirects = it }
                Toggle("Check website age", "Looks up when the domain was registered (public RDAP/WHOIS). Only the domain name is sent.", age, online) { age = it; settings.domainAge = it }
            }

            SectionTitle("History")
            KCard(Modifier.fillMaxWidth()) {
                Toggle("Save scan history", "Keeps your last 50 scans, encrypted, on this phone only.", history) { history = it; settings.saveHistory = it }
                TextButton(onClick = onClearHistory) { Text("Clear history", color = K.red) }
            }

            SectionTitle("Threat-intelligence keys (optional, free)")
            Text(
                "Add your own free keys for a stronger check against global databases of phishing and malware links. Keys are encrypted with the Android Keystore and only sent to that provider, along with the link being checked.",
                color = K.textDim, style = MaterialTheme.typography.bodySmall,
            )
            KeyField("Google Safe Browsing API key", settings.safeBrowsingKey, "https://developers.google.com/safe-browsing/v4/get-started") { settings.safeBrowsingKey = it }
            KeyField("VirusTotal API key", settings.virusTotalKey, "https://www.virustotal.com/gui/join-us") { settings.virusTotalKey = it }
            KeyField("URLhaus (abuse.ch) Auth-Key", settings.urlhausKey, "https://auth.abuse.ch/") { settings.urlhausKey = it }

            SectionTitle("Your privacy")
            KCard(Modifier.fillMaxWidth(), accent = K.green) {
                listOf(
                    "No account, no ads, no analytics, no tracking.",
                    "Screenshots and passwords are checked on the phone and never uploaded.",
                    "Permissions: Internet (optional checks), Camera (QR screen only), app list (Phone check, analysed on the phone).",
                    "Built around India's DPDP Act 2023: purpose limitation, data minimisation and consent.",
                ).forEach { Text("• $it", color = K.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
            }
            Text("Kavach v4.0", color = K.textDim, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 20.dp))
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = if (enabled) K.text else K.textDim, fontWeight = FontWeight.SemiBold)
            Text(sub, color = K.textDim, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked && enabled, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = K.orange, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun KeyField(label: String, initial: String, getKeyUrl: String, onSave: (String) -> Unit) {
    val context = LocalContext.current
    var value by remember { mutableStateOf(initial) }
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it.trim(); onSave(value) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = K.orange, unfocusedBorderColor = K.outline, focusedLabelColor = K.orange, focusedTextColor = K.text, unfocusedTextColor = K.text, cursorColor = K.orange),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Show", tint = K.textDim) }
        },
    )
    Row {
        Spacer(Modifier.width(4.dp))
        TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getKeyUrl))) } }) {
            Text("Get a free key →", color = K.orange)
        }
    }
}
