package com.kavach.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.KavachLogo
import com.kavach.scanner.ui.components.LogoState
import com.kavach.scanner.ui.components.ScoreRing
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.theme.K

@Composable
fun ResultScreen(state: ScanUiState, onBack: () -> Unit, onScanAgain: () -> Unit, onReport: () -> Unit, openUrl: String? = null, onOpenUrl: (String) -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Scan result", onBack)
        when (state) {
            is ScanUiState.Scanning, ScanUiState.Idle -> Scanning((state as? ScanUiState.Scanning)?.label ?: "Preparing…")
            is ScanUiState.Error -> ErrorView(state.message, onScanAgain)
            is ScanUiState.Done -> Report(state, onScanAgain, onReport, openUrl, onOpenUrl)
        }
    }
}

private val layers = listOf(
    "Address & look-alike analysis", "Scam wording patterns", "UPI & QR payment rules",
    "Redirects & certificate", "Domain age (WHOIS)", "Threat databases",
)

@Composable
private fun Scanning(label: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        KavachLogo(LogoState.SCANNING, size = 190.dp)
        Spacer(Modifier.height(24.dp))
        Text(label, style = MaterialTheme.typography.titleLarge, color = K.text)
        Spacer(Modifier.height(16.dp))
        layers.forEachIndexed { i, l ->
            Row(Modifier.enter(i).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TaskAlt, null, tint = K.orange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(l, color = K.textDim, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onScanAgain: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        KavachLogo(LogoState.WARNING, size = 130.dp)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, color = K.text)
        Spacer(Modifier.height(20.dp))
        ShineButton("TRY AGAIN", null, Modifier.fillMaxWidth()) { onScanAgain() }
    }
}

@Composable
private fun Report(done: ScanUiState.Done, onScanAgain: () -> Unit, onReport: () -> Unit, openUrl: String?, onOpenUrl: (String) -> Unit) {
    val r = done.report
    val context = LocalContext.current
    val color = K.verdict(r.verdict)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
        // ---- Verdict banner ----
        Column(
            Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.78f))))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KavachLogo(
                    when (r.verdict) { Verdict.SAFE -> LogoState.SAFE; Verdict.SUSPICIOUS -> LogoState.WARNING; Verdict.DANGEROUS -> LogoState.DANGER },
                    size = 110.dp, onDark = true,
                )
                Spacer(Modifier.width(8.dp))
                ScoreRing(r.riskScore, Color.White, "RISK", size = 110.dp, trackColor = Color.White.copy(alpha = 0.3f), labelColor = Color.White)
            }
            Text(verdictTitle(r.verdict), style = MaterialTheme.typography.headlineLarge, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            Text(r.summary, color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center)
        }

        KCard(Modifier.fillMaxWidth().padding(top = 12.dp).enter(1)) {
            Text("CHECKED", style = MaterialTheme.typography.labelSmall, color = K.orange)
            SelectionContainer {
                Text(r.input.take(400), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, color = K.text, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            if (r.redirectChain.size > 1) {
                Text("WHERE THE LINK REALLY GOES", style = MaterialTheme.typography.labelSmall, color = K.orange, modifier = Modifier.padding(top = 10.dp))
                r.redirectChain.forEachIndexed { i, u ->
                    Text("${if (i == 0) "▶" else "↳"} $u", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = K.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (r.advice.isNotEmpty()) {
            SectionTitle("What you should do")
            KCard(Modifier.fillMaxWidth().enter(2), accent = color) {
                r.advice.forEach {
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Filled.Shield, null, tint = color, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = K.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        val shown = r.findings.filter { it.risk != 0 || it.severity.ordinal > 0 || it.id.endsWith("upi_info") || it.id.contains("redirect") || it.id.contains("tls_ok") }
        SectionTitle("Why · ${shown.size} signals")
        if (shown.isEmpty()) Text("No risk signals found.", color = K.textDim)
        shown.groupBy { it.category }.entries.forEachIndexed { gi, (cat, items) ->
            Text(cat.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = K.textDim, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            items.forEachIndexed { i, f -> FindingCard(f, Modifier.enter(3 + gi + i)) }
        }

        if (done.extra.isNotEmpty()) {
            SectionTitle("Also found in the image")
            done.extra.forEach { e ->
                KCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), accent = K.verdict(e.verdict)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Chip(verdictTitle(e.verdict).uppercase(), K.verdict(e.verdict))
                        Spacer(Modifier.width(8.dp))
                        Text(e.input.take(80), fontFamily = FontFamily.Monospace, color = K.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    e.findings.filter { it.risk > 0 }.take(3).forEach { Text("• ${it.title}", color = K.textDim, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }

        if (done.extractedText != null) {
            var open by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Text read from the screenshot", Modifier.weight(1f))
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = K.orange)
            }
            if (open) KCard(Modifier.fillMaxWidth()) { SelectionContainer { Text(done.extractedText, color = K.text, style = MaterialTheme.typography.bodyMedium) } }
        }

        SectionTitle("Checks performed")
        KCard(Modifier.fillMaxWidth()) {
            r.checks.distinctBy { it.name + it.ran }.forEach { c ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(if (c.ran) Icons.Filled.CheckCircle else Icons.Filled.RemoveCircleOutline, null, tint = if (c.ran) K.green else K.textDim, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(c.name, fontWeight = FontWeight.SemiBold, color = K.text, style = MaterialTheme.typography.bodyMedium)
                        Text(c.note, color = K.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        SectionTitle("Actions")
        if (openUrl != null) {
            // Link Guard: the user tapped this link in another app and sent it here first.
            KCard(Modifier.fillMaxWidth(), accent = K.verdict(r.verdict)) {
                Text(
                    when (r.verdict) {
                        Verdict.SAFE -> "No warning signs found. You can open this link."
                        Verdict.SUSPICIOUS -> "Be careful. Only open it if you trust the sender, and never enter OTP, PIN or card details."
                        Verdict.DANGEROUS -> "Do not open this link. It shows strong signs of a scam."
                    },
                    color = K.text, style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                if (r.verdict == Verdict.SAFE) {
                    ShineButton("OPEN IN BROWSER", Icons.Filled.Shield, Modifier.fillMaxWidth(), color = K.green) { onOpenUrl(openUrl) }
                } else {
                    var confirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { if (confirm) onOpenUrl(openUrl) else confirm = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
                    ) { Text(if (confirm) "Tap again to open at your own risk" else "Open anyway", color = K.red, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (r.verdict != Verdict.SAFE) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShineButton("CALL 1930", Icons.Filled.Phone, Modifier.weight(1f), color = K.red) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930")))
                }
                ShineButton("REPORT", Icons.Filled.Flag, Modifier.weight(1f), color = K.navy) { onReport() }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { share(context, r) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Share, null, tint = K.orange); Text("  Share", color = K.text)
            }
            OutlinedButton(onClick = { copy(context, r.input) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.ContentCopy, null, tint = K.orange); Text("  Copy", color = K.text)
            }
        }
        Spacer(Modifier.height(14.dp))
        ShineButton("SCAN SOMETHING ELSE", Icons.Filled.Shield, Modifier.fillMaxWidth()) { onScanAgain() }
        Text(
            "Kavach gives a risk assessment, not a guarantee. When in doubt, contact the organisation through its official website or app.",
            color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 20.dp),
        )
    }
}

@Composable
private fun FindingCard(f: Finding, modifier: Modifier) {
    val c = if (f.risk < 0) K.green else K.severity(f.severity)
    KCard(modifier.fillMaxWidth().padding(vertical = 5.dp), padding = 0.dp) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(c))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip(if (f.risk < 0) "GOOD SIGN" else f.severity.name, c)
                    Spacer(Modifier.width(8.dp))
                    Text(f.title, fontWeight = FontWeight.SemiBold, color = K.text, modifier = Modifier.weight(1f))
                }
                Text(f.detail, color = K.textDim, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                f.reference?.let { Text("Ref: $it", style = MaterialTheme.typography.labelSmall, color = K.orange, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
    }
}

fun verdictTitle(v: Verdict) = when (v) {
    Verdict.SAFE -> "Looks Safe"
    Verdict.SUSPICIOUS -> "Suspicious"
    Verdict.DANGEROUS -> "Dangerous"
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
