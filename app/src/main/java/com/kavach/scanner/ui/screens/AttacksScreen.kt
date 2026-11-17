package com.kavach.scanner.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavach.scanner.data.Attacks
import com.kavach.scanner.ui.components.IconBadge
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K

@Composable
fun AttacksScreen(onBack: () -> Unit) {
    val cats = listOf("All") + Attacks.all.map { it.category }.distinct()
    var cat by remember { mutableStateOf("All") }
    var open by remember { mutableStateOf<String?>(null) }
    val list = Attacks.all.filter { cat == "All" || it.category == cat }

    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Attack library", onBack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEach { c ->
                FilterChip(
                    selected = cat == c, onClick = { cat = c }, label = { Text(c) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = K.orange, selectedLabelColor = Color.White, labelColor = K.text),
                )
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text("${Attacks.all.size} ways phones get hacked or people get cheated, and how to stay safe. Based on OWASP Mobile Top 10, MITRE ATT&CK for Mobile and CERT-In advisories.", color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            }
            itemsIndexed(list, key = { _, a -> a.id }) { i, a ->
                val expanded = open == a.id
                KCard(Modifier.fillMaxWidth().padding(vertical = 5.dp).enter(minOf(i, 8)).clip(RoundedCornerShape(20.dp)).pressable { open = if (expanded) null else a.id }.animateContentSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(iconFor(a.category), colorFor(a.category), 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, fontWeight = FontWeight.Bold, color = K.text)
                            Text(a.category, color = K.textDim, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = K.orange)
                    }
                    if (expanded) {
                        Text(a.how, color = K.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                        Text("WARNING SIGNS", style = MaterialTheme.typography.labelSmall, color = K.red, modifier = Modifier.padding(top = 10.dp))
                        a.signs.forEach { Text("• $it", color = K.text, style = MaterialTheme.typography.bodyMedium) }
                        Text("HOW TO PROTECT YOURSELF", style = MaterialTheme.typography.labelSmall, color = K.green, modifier = Modifier.padding(top = 10.dp))
                        a.protect.forEach { Text("• $it", color = K.text, style = MaterialTheme.typography.bodyMedium) }
                        Row(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(K.orangeSoft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.GppGood, null, tint = K.orange)
                            Spacer(Modifier.width(8.dp))
                            Text("Kavach protects: ${a.kavach}", color = K.text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun colorFor(c: String): Color = when (c) {
    "Scam messages" -> K.orange
    "Social engineering" -> K.purple
    "Payment fraud" -> K.amber
    "Malware" -> K.red
    "Account takeover" -> K.blue
    "Network" -> K.green
    else -> K.textDim
}

private fun iconFor(c: String): ImageVector = when (c) {
    "Scam messages" -> Icons.Filled.Sms
    "Social engineering" -> Icons.Filled.Groups
    "Payment fraud" -> Icons.Filled.Payments
    "Malware" -> Icons.Filled.BugReport
    "Account takeover" -> Icons.Filled.ManageAccounts
    "Network" -> Icons.Filled.Wifi
    else -> Icons.Filled.Usb
}
