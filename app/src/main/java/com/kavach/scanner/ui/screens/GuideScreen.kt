package com.kavach.scanner.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CallMissedOutgoing
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavach.scanner.ui.components.IconBadge
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KRow
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.components.enter
import com.kavach.scanner.ui.components.pressable
import com.kavach.scanner.ui.theme.K

private data class Scam(val icon: ImageVector, val title: String, val body: String)

private val scams = listOf(
    Scam(Icons.Filled.AccountBalance, "KYC / account-block SMS", "\"Your SBI/YONO account will be blocked, update PAN/KYC\" with a link. The fake page steals your login and OTP."),
    Scam(Icons.Filled.ElectricBolt, "Electricity disconnection", "\"Power will be cut tonight, call this officer.\" Leads to a screen-sharing app or a payment link."),
    Scam(Icons.Filled.Policy, "Digital arrest / parcel", "Caller poses as FedEx, customs, CBI or police, says your parcel had drugs, keeps you on video and demands money."),
    Scam(Icons.Filled.WorkOutline, "Part-time job / task", "Earn by liking videos or writing reviews. Small payouts first, then \"prepaid tasks\" take big deposits."),
    Scam(Icons.Filled.ShowChart, "Investment & trading groups", "WhatsApp/Telegram \"VIP stock tips\" and fake trading apps showing fake profits you can never withdraw."),
    Scam(Icons.Filled.QrCode2, "QR code / UPI scams", "\"Scan to receive refund.\" Scanning and entering your PIN sends YOUR money. Watch for fake QR stickers at shops."),
    Scam(Icons.Filled.Android, "Fake APK files", "\"RTO challan.apk\", \"PM Kisan.apk\", \"Wedding invitation.apk\" read your SMS/OTP and take over WhatsApp."),
    Scam(Icons.Filled.CardGiftcard, "Lottery / KBC / gifts", "You \"won\" a prize but must first pay tax or a processing fee."),
    Scam(Icons.Filled.CallMissedOutgoing, "Call-forwarding trick", "Being asked to dial *401* or **21* plus a number forwards your calls and OTPs to the fraudster."),
    Scam(Icons.Filled.RequestQuote, "Instant loan apps", "Loans without documents, followed by harassment using your contacts and photos. Check the lender is RBI-registered."),
    Scam(Icons.Filled.LocalShipping, "Courier / address update", "\"Parcel on hold, pay ₹25 to update address.\" The link captures your card details."),
    Scam(Icons.Filled.Payments, "\"Sent by mistake\"", "A fake credit SMS, then a request to return the money. Always check your bank app, not the SMS."),
)

private data class Quiz(val msg: String, val scam: Boolean, val why: String)

private val quiz = listOf(
    Quiz("Dear customer, your electricity will be disconnected tonight at 9:30 PM. Call officer 98XXXXXX10 immediately.", true, "Power companies never threaten disconnection from a personal mobile number."),
    Quiz("482913 is your OTP for txn of INR 1,250.00 at AMAZON. Do not share this OTP with anyone. - HDFC Bank", false, "A normal OTP SMS. It tells you NOT to share it and asks for nothing."),
    Quiz("Congratulations! You won ₹25,00,000 in KBC lucky draw. Pay ₹12,500 GST to claim. WhatsApp now.", true, "You can't win a lottery you never entered, and real prizes never ask for fees."),
    Quiz("Scan this QR code to receive your ₹2,000 cashback from PhonePe.", true, "You NEVER scan a QR or enter a PIN to receive money."),
    Quiz("Your Swiggy order #4821 is out for delivery. Track it in the Swiggy app.", false, "No link to an unknown site, no request for money or details."),
    Quiz("This is CBI. A parcel in your name contains drugs. Stay on this video call, you are under digital arrest.", true, "\"Digital arrest\" does not exist. Hang up and call 1930."),
)

private val standards = listOf(
    "CERT-In advisories (smishing, malicious APKs, phishing)",
    "IT Act 2000: Sec 43, 66C (identity theft), 66D (cheating by impersonation)",
    "Digital Personal Data Protection Act 2023: Kavach collects no personal data",
    "RBI BE(A)WARE booklet & digital payment security directions; NPCI UPI safety",
    "OWASP MASVS v2 & Mobile Top 10: encrypted keys, minimal permissions, no backups",
    "NIST CSF 2.0, SP 800-52 & 800-63B: layered defence, TLS and password guidance",
    "ISO/IEC 27001:2022 principles: least privilege, secure by default",
    "Unicode TR39: detection of look-alike (homoglyph) characters",
)

@Composable
fun GuideScreen(onBack: (() -> Unit)?) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Learn & stay safe", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("Quick quiz: scam or genuine?")
            QuizCard()

            SectionTitle("Golden rules")
            KCard(Modifier.fillMaxWidth(), accent = K.green) {
                listOf(
                    "Never share OTP, UPI PIN, CVV or passwords, with anyone.",
                    "You never scan a QR or enter your PIN to RECEIVE money.",
                    "Install apps only from Play Store, never .apk files from chats.",
                    "Never install AnyDesk/TeamViewer because a caller asked.",
                    "Police, CBI, RBI, TRAI never threaten arrest on a call.",
                    "Get customer-care numbers from the official website only.",
                    "Cheated? Call 1930 within the first (golden) hour.",
                ).forEachIndexed { i, r ->
                    Row(Modifier.padding(vertical = 4.dp).enter(i)) {
                        Icon(Icons.Filled.Verified, null, tint = K.green)
                        Spacer(Modifier.width(10.dp)); Text(r, color = K.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionTitle("Common scams in India")
            scams.forEachIndexed { i, s ->
                KCard(Modifier.fillMaxWidth().padding(vertical = 5.dp).enter(i)) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(s.icon, K.orange, 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(s.title, fontWeight = FontWeight.Bold, color = K.text)
                            Text(s.body, color = K.textDim, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            SectionTitle("Get help")
            KCard(Modifier.fillMaxWidth()) {
                listOf(
                    Triple(Icons.Filled.Phone, "1930: Cyber Crime Helpline" to "Call immediately if money was lost", "tel:1930"),
                    Triple(Icons.Filled.Gavel, "cybercrime.gov.in" to "File an online complaint (I4C, MHA)", "https://cybercrime.gov.in/"),
                    Triple(Icons.Filled.PhoneDisabled, "Sanchar Saathi: Chakshu" to "Report fraud calls, SMS, WhatsApp", "https://sancharsaathi.gov.in/sfc/"),
                    Triple(Icons.Filled.AccountBalance, "RBI complaint (CMS)" to "Complaints about banks & lenders", "https://cms.rbi.org.in/"),
                ).forEach { (icon, t, link) ->
                    KRow(icon, if (link.startsWith("tel")) K.red else K.blue, t.first, t.second, Modifier.clip(RoundedCornerShape(12.dp)).pressable {
                        runCatching { context.startActivity(Intent(if (link.startsWith("tel")) Intent.ACTION_DIAL else Intent.ACTION_VIEW, Uri.parse(link))) }
                    })
                }
            }

            SectionTitle("Standards & laws Kavach follows")
            KCard(Modifier.fillMaxWidth()) {
                standards.forEach {
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = K.orange)
                        Spacer(Modifier.width(8.dp)); Text(it, color = K.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                "Kavach is an awareness and risk-assessment tool. It is not affiliated with any government agency or bank.",
                color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun QuizCard() {
    var idx by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf<Boolean?>(null) }
    var score by remember { mutableIntStateOf(0) }
    val q = quiz[idx % quiz.size]
    KCard(Modifier.fillMaxWidth(), accent = K.orange) {
        Text("Question ${idx % quiz.size + 1} of ${quiz.size}  ·  Score $score", style = MaterialTheme.typography.labelSmall, color = K.orange)
        Spacer(Modifier.height(8.dp))
        Text("\"${q.msg}\"", color = K.text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        if (answer == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShineButton("SCAM", null, Modifier.weight(1f), color = K.red) { answer = true; if (q.scam) score++ }
                ShineButton("GENUINE", null, Modifier.weight(1f), color = K.green) { answer = false; if (!q.scam) score++ }
            }
        }
        AnimatedVisibility(answer != null) {
            Column {
                val right = answer == q.scam
                Text(if (right) "Correct!" else "Not quite.", color = if (right) K.green else K.red, fontWeight = FontWeight.Bold)
                Text("It's ${if (q.scam) "a SCAM" else "GENUINE"}. ${q.why}", color = K.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                ShineButton(if ((idx + 1) % quiz.size == 0) "PLAY AGAIN" else "NEXT", null, Modifier.fillMaxWidth(), color = K.navy) {
                    if ((idx + 1) % quiz.size == 0) score = 0
                    idx++; answer = null
                }
            }
        }
    }
}

@Suppress("unused") private val unusedColor = Color.Unspecified
