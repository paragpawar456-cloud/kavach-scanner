package com.kavach.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KRow
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val steps = listOf(
    "Call 1930 immediately (National Cyber Crime Helpline). Within the first hour your money can often be frozen.",
    "Call your bank's official helpline and block your card / UPI / net banking.",
    "File a complaint on cybercrime.gov.in → 'Report Financial Fraud'. Note the acknowledgement number.",
    "Report the fraud number or SMS on Sanchar Saathi → Chakshu so it gets blocked.",
    "Keep evidence: screenshots, transaction IDs (UTR), the fraudster's number, UPI ID and links. Don't delete chats.",
    "If an app was installed: switch on Airplane mode, uninstall it, change UPI PIN and passwords from another phone.",
)

@Composable
fun ReportScreen(prefill: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    var what by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var utr by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf(prefill?.take(300) ?: "") }

    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Report fraud", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding().imePadding()) {
            EmergencyBanner { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))) }

            SectionTitle("Do this now")
            KCard(Modifier.fillMaxWidth()) {
                steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 6.dp).enter(i), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(K.orange), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(s, color = K.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionTitle("Report online")
            KCard(Modifier.fillMaxWidth()) {
                KRow(Icons.Filled.Gavel, K.red, "cybercrime.gov.in", "National Cyber Crime Reporting Portal (I4C, MHA)", Modifier.clip(RoundedCornerShape(12.dp)).pressable { open(context, "https://cybercrime.gov.in/") })
                KRow(Icons.Filled.PhoneDisabled, K.blue, "Sanchar Saathi – Chakshu", "Report fraud calls, SMS and WhatsApp numbers", Modifier.clip(RoundedCornerShape(12.dp)).pressable { open(context, "https://sancharsaathi.gov.in/sfc/") })
                KRow(Icons.Filled.AccountBalance, K.green, "RBI complaint (CMS)", "If your bank doesn't help within 30 days", Modifier.clip(RoundedCornerShape(12.dp)).pressable { open(context, "https://cms.rbi.org.in/") })
                KRow(Icons.Filled.Language, K.purple, "CERT-In", "Report hacking / cyber incidents", Modifier.clip(RoundedCornerShape(12.dp)).pressable { open(context, "https://www.cert-in.org.in/") })
            }

            SectionTitle("Prepare your complaint")
            Text("Fill in what you know. Kavach writes a clear summary you can paste into the portal or read out on the 1930 call. Nothing is sent anywhere.", color = K.textDim, style = MaterialTheme.typography.bodySmall)
            Field("What happened?", what, 4) { what = it }
            Field("Amount lost (₹)", amount) { amount = it }
            Field("Transaction ID / UTR number(s)", utr) { utr = it }
            Field("Fraudster's phone / UPI ID / account", contact) { contact = it }
            Field("Link, app or message used", link, 2) { link = it }

            val text = buildString {
                appendLine("CYBER FRAUD COMPLAINT SUMMARY")
                appendLine("Date of report: " + SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("en", "IN")).format(Date()))
                if (what.isNotBlank()) appendLine("What happened: $what")
                if (amount.isNotBlank()) appendLine("Amount lost: ₹$amount")
                if (utr.isNotBlank()) appendLine("Transaction ID / UTR: $utr")
                if (contact.isNotBlank()) appendLine("Fraudster details: $contact")
                if (link.isNotBlank()) appendLine("Link / app / message: ${link.replace("http", "hxxp")}")
                appendLine("Evidence: screenshots and bank statement available.")
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Complaint", text))
                    Toast.makeText(context, "Copied. Paste it on cybercrime.gov.in", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.ContentCopy, null, tint = K.orange); Text("  Copy", color = K.text)
                }
                OutlinedButton(onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share complaint"))
                }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.Share, null, tint = K.orange); Text("  Share", color = K.text)
                }
            }
            Text(
                "Offences: IT Act 2000 Sec 66C (identity theft) and 66D (cheating by impersonation), BNS Sec 318/319 (cheating). Reporting is free.",
                color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun Field(label: String, value: String, lines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, minLines = lines, singleLine = lines == 1,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = K.orange, unfocusedBorderColor = K.outline, focusedLabelColor = K.orange, focusedTextColor = K.text, unfocusedTextColor = K.text, cursorColor = K.orange),
    )
}

private fun open(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
