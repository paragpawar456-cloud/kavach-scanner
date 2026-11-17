package com.kavach.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kavach.core.model.Finding
import com.kavach.core.model.ScanReport
import com.kavach.core.model.Verdict
import com.kavach.scanner.scan.ScanUiState
import com.kavach.scanner.ui.components.Chip
import com.kavach.scanner.ui.components.CyberBackground
import com.kavach.scanner.ui.components.CyberCard
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.RiskGauge
import com.kavach.scanner.ui.components.SectionLabel
import com.kavach.scanner.ui.components.ShieldLogo
import com.kavach.scanner.ui.theme.KColors

@Composable
fun ResultScreen(state: ScanUiState, onBack: () -> Unit, onScanAgain: () -> Unit) {
    CyberBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Scan result", style = MaterialTheme.typography.titleLarge)
            }
            when (state) {
                is ScanUiState.Scanning, ScanUiState.Idle -> Scanning((state as? ScanUiState.Scanning)?.label ?: "Preparing…")
                is ScanUiState.Error -> ErrorView(state.message, onScanAgain)
                is ScanUiState.Done -> Report(state, onScanAgain)
            }
        }
    }
}

@Composable
private fun Scanning(label: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        ShieldLogo(LogoState.SCANNING, size = 170.dp)
        Spacer(Modifier.height(24.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            "Address analysis · look-alike detection · scam wording · redirects · certificate · domain age · threat databases",
            color = KColors.TextDim, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ErrorView(message: String, onScanAgain: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        ShieldLogo(LogoState.WARNING, size = 120.dp)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onScanAgain) { Text("Try again") }
    }
}

@Composable
private fun Report(done: ScanUiState.Done, onScanAgain: () -> Unit) {
    val r = done.report
    val context = LocalContext.current
    val color = KColors.verdict(r.verdict)
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(r) { show = true }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        // ---- Verdict header ----
        CyberCard(Modifier.fillMaxWidth(), accent = color) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldLogo(
                    when (r.verdict) { Verdict.SAFE -> LogoState.SAFE; Verdict.SUSPICIOUS -> LogoState.WARNING; Verdict.DANGEROUS -> LogoState.DANGER },
                    size = 96.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(verdictTitle(r.verdict), style = MaterialTheme.typography.headlineMedium, color = color)
                    Text(r.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                RiskGauge(r.riskScore, color)
            }
            SectionLabel("Checked")
            SelectionContainer {
                Text(
                    r.input.take(400), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium,
                    color = KColors.TextDim, maxLines = 6, overflow = TextOverflow.Ellipsis,
                )
            }
            if (r.redirectChain.size > 1) {
                SectionLabel("Where the link really goes")
                r.redirectChain.forEachIndexed { i, u ->
                    Text("${if (i == 0) "▶" else "↳"} $u", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        AnimatedVisibility(show, enter = fadeIn() + slideInVertically { it / 4 }) {
            Column {
                // ---- What to do ----
                if (r.advice.isNotEmpty()) {
                    SectionLabel("What you should do", Modifier.padding(top = 8.dp))
                    CyberCard(Modifier.fillMaxWidth(), accent = color) {
                        r.advice.forEach { Text("• $it", modifier = Modifier.padding(vertical = 3.dp)) }
                    }
                }

                // ---- Findings ----
                val shown = r.findings.filter { it.risk != 0 || it.severity.ordinal > 0 || it.id.endsWith("upi_info") || it.id.contains("redirect") || it.id.contains("tls_ok") }
                SectionLabel("Why (${shown.size} signals)", Modifier.padding(top = 8.dp))
                if (shown.isEmpty()) Text("No risk signals found.", color = KColors.TextDim)
                shown.groupBy { it.category }.forEach { (cat, items) ->
                    Text(cat.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    items.forEach { FindingCard(it) }
                }

                // ---- Other items found in a screenshot ----
                if (done.extra.isNotEmpty()) {
                    SectionLabel("Also found in the image", Modifier.padding(top = 8.dp))
                    done.extra.forEach { e ->
                        CyberCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), accent = KColors.verdict(e.verdict)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Chip(verdictTitle(e.verdict).uppercase(), KColors.verdict(e.verdict))
                                Spacer(Modifier.width(8.dp))
                                Text(e.input.take(80), fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            e.findings.filter { it.risk > 0 }.take(3).forEach { Text("• ${it.title}", color = KColors.TextDim, modifier = Modifier.padding(top = 4.dp)) }
                        }
                    }
                }

                if (done.extractedText != null) {
                    var open by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Text read from screenshot", Modifier.weight(1f))
                        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = KColors.Cyan)
                    }
                    if (open) CyberCard(Modifier.fillMaxWidth()) { SelectionContainer { Text(done.extractedText, style = MaterialTheme.typography.bodyMedium) } }
                }

                // ---- Checks performed ----
                SectionLabel("Checks performed", Modifier.padding(top = 12.dp))
                CyberCard(Modifier.fillMaxWidth()) {
                    r.checks.distinctBy { it.name + it.ran }.forEach { c ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                if (c.ran) Icons.Filled.CheckCircle else Icons.Filled.RemoveCircleOutline, null,
                                tint = if (c.ran) KColors.Green else KColors.TextDim, modifier = Modifier.size(18.dp).padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(c.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(c.note, color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // ---- Actions ----
                SectionLabel("Actions", Modifier.padding(top = 12.dp))
                if (r.verdict != Verdict.SAFE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) },
                            colors = ButtonDefaults.buttonColors(containerColor = KColors.Red), modifier = Modifier.weight(1f),
                        ) { Icon(Icons.Filled.Phone, null); Text(" Call 1930") }
                        Button(
                            onClick = { open(context, "https://cybercrime.gov.in/") },
                            colors = ButtonDefaults.buttonColors(containerColor = KColors.Amber, contentColor = Color.Black), modifier = Modifier.weight(1f),
                        ) { Icon(Icons.Filled.Flag, null); Text(" Report") }
                    }
                    OutlinedButton(onClick = { open(context, "https://sancharsaathi.gov.in/sfc/") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Report fraud number / SMS on Sanchar Saathi (Chakshu)")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedButton(onClick = { share(context, r) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Share, null); Text(" Share") }
                    OutlinedButton(onClick = { copy(context, r.input) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.ContentCopy, null); Text(" Copy") }
                }
                Button(
                    onClick = onScanAgain, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(50.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = KColors.Cyan, contentColor = Color(0xFF001A20)),
                ) { Text("SCAN SOMETHING ELSE", fontWeight = FontWeight.Bold) }
                Text(
                    "Kavach gives a risk assessment, not a guarantee. When in doubt, contact the organisation through its official website or app.",
                    color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun FindingCard(f: Finding) {
    val c = KColors.severity(f.severity)
    CyberCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), accent = c) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip(if (f.risk < 0) "GOOD SIGN" else f.severity.name, if (f.risk < 0) KColors.Green else c)
            Spacer(Modifier.width(8.dp))
            Text(f.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        Text(f.detail, color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        f.reference?.let { Text("Ref: $it", style = MaterialTheme.typography.labelSmall, color = KColors.Cyan.copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp)) }
    }
}

private fun verdictTitle(v: Verdict) = when (v) {
    Verdict.SAFE -> "Looks Safe"
    Verdict.SUSPICIOUS -> "Suspicious"
    Verdict.DANGEROUS -> "Dangerous"
}

private fun open(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun copy(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Kavach", text))
}

private fun share(context: Context, r: ScanReport) {
    val body = buildString {
        appendLine("Kavach scan: ${verdictTitle(r.verdict).uppercase()} (risk ${r.riskScore}/100)")
        appendLine(r.summary)
        r.findings.filter { it.risk > 0 }.take(6).forEach { appendLine("• ${it.title}") }
        appendLine()
        // Defang links so the shared report can't be tapped by accident.
        appendLine("Checked: " + r.input.take(300).replace("http", "hxxp").replace(".", "[.]"))
        appendLine("Report cyber fraud: call 1930 / cybercrime.gov.in")
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body), "Share report"))
}
