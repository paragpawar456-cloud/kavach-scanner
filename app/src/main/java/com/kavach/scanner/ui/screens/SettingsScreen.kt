package com.kavach.scanner.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kavach.scanner.data.SettingsStore
import com.kavach.scanner.ui.components.CyberBackground
import com.kavach.scanner.ui.components.CyberCard
import com.kavach.scanner.ui.components.SectionLabel
import com.kavach.scanner.ui.theme.KColors

@Composable
fun SettingsScreen(settings: SettingsStore, onBack: () -> Unit) {
    var online by remember { mutableStateOf(settings.onlineChecks) }
    var redirects by remember { mutableStateOf(settings.followRedirects) }
    var age by remember { mutableStateOf(settings.domainAge) }

    CyberBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Settings & privacy", style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                SectionLabel("Online checks")
                CyberCard(Modifier.fillMaxWidth()) {
                    Toggle("Use online checks", "Master switch. Off = 100% offline analysis, nothing leaves your phone.", online) { online = it; settings.onlineChecks = it }
                    Toggle("Reveal hidden destination", "Follows short links and redirects to show where they really go. Kavach fetches the page text only: no JavaScript, no cookies, nothing is installed.", redirects, online) { redirects = it; settings.followRedirects = it }
                    Toggle("Check website age", "Looks up when the domain was registered (public RDAP/WHOIS data). Only the domain name is sent.", age, online) { age = it; settings.domainAge = it }
                }

                SectionLabel("Threat-intelligence API keys (optional, free)")
                Text(
                    "Add your own free keys for a much stronger check against global databases of known phishing and malware links. " +
                        "Keys are stored encrypted on this phone (Android Keystore) and only sent to that provider. The full link you scan is shared with the provider you enable.",
                    color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium,
                )
                KeyField("Google Safe Browsing API key", settings.safeBrowsingKey, "https://developers.google.com/safe-browsing/v4/get-started") { settings.safeBrowsingKey = it }
                KeyField("VirusTotal API key", settings.virusTotalKey, "https://www.virustotal.com/gui/join-us") { settings.virusTotalKey = it }
                KeyField("URLhaus (abuse.ch) Auth-Key", settings.urlhausKey, "https://auth.abuse.ch/") { settings.urlhausKey = it }

                SectionLabel("Your privacy")
                CyberCard(Modifier.fillMaxWidth()) {
                    listOf(
                        "No account, no ads, no analytics, no tracking.",
                        "Scan history is not stored. Screenshots are read on the phone and never uploaded.",
                        "Only two permissions: Internet (optional checks) and Camera (QR scanner only).",
                        "Designed around India's DPDP Act 2023 principles: purpose limitation, data minimisation and consent.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = if (enabled) KColors.Text else KColors.TextDim)
            Text(sub, color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked && enabled, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = KColors.Cyan),
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
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Show") }
        },
    )
    TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getKeyUrl))) } }) {
        Text("Get a free key →", color = KColors.Cyan)
    }
}
