package com.kavach.scanner.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavach.scanner.ui.components.CyberBackground
import com.kavach.scanner.ui.components.CyberCard
import com.kavach.scanner.ui.components.SectionLabel
import com.kavach.scanner.ui.theme.KColors

private data class Tip(val title: String, val body: String)

private val goldenRules = listOf(
    "Never share OTP, UPI PIN, CVV or passwords, with anyone, for any reason.",
    "You never scan a QR code or enter your PIN to RECEIVE money.",
    "Never install apps (.apk) sent on WhatsApp/SMS. Use Play Store only.",
    "Never install AnyDesk/TeamViewer because a \"bank officer\" asked.",
    "Banks, RBI, police, CBI or TRAI never threaten you over phone or video call. \"Digital arrest\" does not exist.",
    "Find customer-care numbers on the official website, not on Google ads or in messages.",
    "Act fast if cheated: call 1930 within the first hour (the \"golden hour\") so money can be frozen.",
)

private val scams = listOf(
    Tip("KYC / account-block SMS", "\"Your SBI/YONO account will be blocked, update PAN/KYC\" with a link. Fake page steals login + OTP."),
    Tip("Electricity disconnection", "\"Power will be cut tonight, call officer\". Leads to a remote-access app or payment link."),
    Tip("Digital arrest / parcel", "Caller poses as FedEx, customs, CBI or police, says your parcel had drugs, keeps you on video and demands money."),
    Tip("Part-time job / task", "Earn by liking videos or reviews. Small payouts first, then \"prepaid tasks\" take large deposits."),
    Tip("Investment & trading groups", "WhatsApp/Telegram \"VIP stock tips\" and fake trading apps showing fake profits you can't withdraw."),
    Tip("QR code / UPI scams", "\"Scan to receive refund/payment\". Scanning and entering PIN sends YOUR money. Fake QR stickers on shop counters."),
    Tip("Fake APK files", "\"RTO challan.apk\", \"PM Kisan.apk\", \"Wedding invitation.apk\" read your SMS/OTP and take over WhatsApp."),
    Tip("Lottery / KBC / gifts", "You \"won\" a prize but must pay tax or fees first."),
    Tip("Call-forwarding trick", "Being asked to dial *401* or **21* followed by a number forwards your calls and OTPs to the fraudster."),
    Tip("Loan apps", "Instant loans without documents; then harassment using your contacts and photos. Check RBI-registered lenders."),
)

private val help = listOf(
    Triple("1930 – National Cyber Crime Helpline", "Call immediately if money was lost", "tel:1930"),
    Triple("cybercrime.gov.in", "File an online complaint (I4C, MHA)", "https://cybercrime.gov.in/"),
    Triple("Sanchar Saathi – Chakshu", "Report fraud calls, SMS & WhatsApp numbers (DoT)", "https://sancharsaathi.gov.in/sfc/"),
    Triple("RBI Sachet / CMS", "Complaints about banks and unauthorised lenders", "https://cms.rbi.org.in/"),
    Triple("CERT-In", "Report cyber security incidents: incident@cert-in.org.in", "https://www.cert-in.org.in/"),
)

private val standards = listOf(
    "CERT-In advisories (smishing, malicious APKs, phishing) – threat patterns used by the scanner.",
    "IT Act 2000 (Sec 43, 66C identity theft, 66D cheating by impersonation) – the offences these scams fall under.",
    "Digital Personal Data Protection Act 2023 – data minimisation, purpose limitation, no data collection.",
    "RBI BE(A)WARE booklet & digital payment security directions; NPCI UPI safety guidance.",
    "OWASP MASVS v2 & Mobile Top 10 – secure storage (Keystore), minimal permissions, no WebView, no backups of secrets.",
    "NIST SP 800-63 / 800-52 & Cybersecurity Framework 2.0 – defence in depth, TLS checks, layered detection.",
    "ISO/IEC 27001:2022 principles – least privilege, secure-by-default configuration, confidentiality of keys.",
    "Unicode TR39 – detection of look-alike (homoglyph) characters.",
)

@Composable
fun GuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    CyberBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Safety guide", style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                SectionLabel("Get help now")
                help.forEach { (title, sub, link) ->
                    CyberCard(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            val action = if (link.startsWith("tel:")) Intent.ACTION_DIAL else Intent.ACTION_VIEW
                            runCatching { context.startActivity(Intent(action, Uri.parse(link))) }
                        },
                        accent = if (link.startsWith("tel:")) KColors.Red else KColors.Cyan,
                    ) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Text(sub, color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SectionLabel("Golden rules", Modifier.padding(top = 12.dp))
                CyberCard(Modifier.fillMaxWidth(), accent = KColors.Green) {
                    goldenRules.forEachIndexed { i, r -> Text("${i + 1}. $r", modifier = Modifier.padding(vertical = 3.dp)) }
                }

                SectionLabel("Common scams in India", Modifier.padding(top = 12.dp))
                scams.forEach { s ->
                    CyberCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), accent = KColors.Amber) {
                        Text(s.title, fontWeight = FontWeight.SemiBold)
                        Text(s.body, color = KColors.TextDim, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SectionLabel("Standards & laws Kavach follows", Modifier.padding(top = 12.dp))
                CyberCard(Modifier.fillMaxWidth()) {
                    standards.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
                }
                Text(
                    "Kavach is an awareness and risk-assessment tool. It is not affiliated with any government agency or bank.",
                    color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}
