package com.kavach.scanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kavach.core.model.InputType
import com.kavach.scanner.data.HistoryItem
import com.kavach.scanner.ui.components.Chip
import com.kavach.scanner.ui.components.IconBadge
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(items: List<HistoryItem>, enabled: Boolean, onRescan: (HistoryItem) -> Unit, onClear: () -> Unit) {
    val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))
    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Scan history", null) {
            if (items.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Filled.DeleteSweep, "Clear", tint = K.orange) }
        }
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Filled.History, K.orange, 72.dp)
                Spacer(Modifier.height(16.dp))
                Text(if (enabled) "No scans yet" else "History is turned off", style = MaterialTheme.typography.titleLarge, color = K.text)
                Text(
                    if (enabled) "Your scans are saved here, encrypted on this phone only." else "Turn it on in Settings to keep your last 50 scans on this phone.",
                    color = K.textDim, textAlign = TextAlign.Center,
                )
            }
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            itemsIndexed(items) { i, it ->
                KCard(Modifier.fillMaxWidth().padding(vertical = 5.dp).enter(minOf(i, 8)).clip(RoundedCornerShape(20.dp)).pressable { onRescan(it) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            when (it.type) { InputType.QR -> Icons.Filled.QrCode2; InputType.SCREENSHOT -> Icons.Filled.Image; InputType.MESSAGE -> Icons.Filled.Message; InputType.URL -> Icons.Filled.Link; InputType.PHONE -> Icons.Filled.Phone },
                            K.verdict(it.verdict), 40.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it.input, fontFamily = FontFamily.Monospace, color = K.text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text(it.topReason ?: "No risk signals", color = K.textDim, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(fmt.format(Date(it.time)), color = K.textDim, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Chip(verdictTitle(it.verdict).uppercase(), K.verdict(it.verdict))
                            Text("${it.score}", fontWeight = FontWeight.Bold, color = K.verdict(it.verdict), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
