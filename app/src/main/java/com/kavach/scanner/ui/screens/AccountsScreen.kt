package com.kavach.scanner.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kavach.scanner.data.AccountItems
import com.kavach.scanner.data.SettingsStore
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.ScoreRing
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.theme.K

@Composable
fun AccountsScreen(settings: SettingsStore, onPassword: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var done by remember { mutableStateOf(settings.accountChecklist) }
    val pct = done.count { id -> AccountItems.all.any { it.id == id } } * 100 / AccountItems.all.size

    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Secure my accounts", onBack)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                KCard(Modifier.fillMaxWidth().padding(vertical = 8.dp), accent = K.orange) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScoreRing(pct, if (pct >= 80) K.green else K.orange, "% DONE", size = 110.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Lock the doors hackers use", style = MaterialTheme.typography.titleMedium, color = K.text)
                            Text("Tick each step after you finish it. Most take under 2 minutes.", color = K.textDim, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            itemsIndexed(AccountItems.all, key = { _, it -> it.id }) { i, item ->
                val checked = item.id in done
                KCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).enter(minOf(i, 8)), padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, {
                            done = if (it) done + item.id else done - item.id
                            settings.accountChecklist = done
                        }, colors = CheckboxDefaults.colors(checkedColor = K.green))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, color = if (checked) K.textDim else K.text, textDecoration = if (checked) TextDecoration.LineThrough else null)
                            Text(item.why, color = K.textDim, style = MaterialTheme.typography.bodySmall)
                        }
                        if (item.link != null) IconButton(onClick = {
                            when {
                                item.link == "kavach:password" -> onPassword()
                                item.link == "settings:unknown" -> runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)) }
                                else -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link))) }
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open", tint = K.orange) }
                    }
                }
            }
        }
    }
}
