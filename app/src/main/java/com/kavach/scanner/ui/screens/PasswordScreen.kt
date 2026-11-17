package com.kavach.scanner.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kavach.core.PasswordChecker
import com.kavach.scanner.ui.components.IconBadge
import com.kavach.scanner.ui.components.KCard
import com.kavach.scanner.ui.components.KTopBar
import com.kavach.scanner.ui.components.SectionTitle
import com.kavach.scanner.ui.components.ShineButton
import com.kavach.scanner.ui.theme.K

@Composable
fun PasswordScreen(breach: Int?, breachRunning: Boolean, onBreachCheck: (String) -> Unit, onReset: () -> Unit, onBack: () -> Unit) {
    var pw by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val result = remember(pw) { PasswordChecker.check(pw) }
    val color = when (result.strength) {
        PasswordChecker.Strength.VERY_WEAK -> K.red
        PasswordChecker.Strength.WEAK -> Color2.orangeRed
        PasswordChecker.Strength.FAIR -> K.amber
        PasswordChecker.Strength.STRONG -> K.green
        PasswordChecker.Strength.VERY_STRONG -> K.green
    }
    val animColor by animateColorAsState(color, label = "c")
    val frac by animateFloatAsState(if (pw.isEmpty()) 0f else (result.strength.ordinal + 1) / 5f, label = "f")

    Column(Modifier.fillMaxSize().background(K.bg)) {
        KTopBar("Password check", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding().imePadding()) {
            KCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Key, K.amber)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("How strong is your password?", fontWeight = FontWeight.Bold, color = K.text)
                        Text("Checked on your phone. It is never stored or sent.", color = K.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pw, onValueChange = { pw = it; onReset() }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type a password to test", color = K.textDim) },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = K.orange, unfocusedBorderColor = K.outline, focusedTextColor = K.text, unfocusedTextColor = K.text, cursorColor = K.orange),
                    trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Show", tint = K.textDim) } },
                )
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(K.outline)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(animColor))
                }
                if (pw.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(result.strength.label, color = animColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Cracked in: ${result.crackTime}", color = K.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (pw.isNotEmpty() && result.problems.isNotEmpty()) {
                SectionTitle("Problems")
                KCard(Modifier.fillMaxWidth(), accent = K.red) {
                    result.problems.forEach {
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Icon(Icons.Filled.Warning, null, tint = K.red)
                            Spacer(Modifier.width(8.dp)); Text(it, color = K.text)
                        }
                    }
                }
            }

            SectionTitle("Has it leaked in a data breach?")
            KCard(Modifier.fillMaxWidth()) {
                Text(
                    "Uses Have I Been Pwned with k-anonymity: only the first 5 characters of a scrambled code (SHA-1 hash) are sent, never the password.",
                    color = K.textDim, style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                when {
                    breachRunning -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = K.orange, strokeWidth = 3.dp, modifier = Modifier.width(22.dp).height(22.dp))
                        Spacer(Modifier.width(10.dp)); Text("Checking…", color = K.text)
                    }
                    breach == null -> ShineButton("CHECK FOR LEAKS", Icons.Filled.CloudSync, Modifier.fillMaxWidth(), enabled = pw.isNotEmpty()) { onBreachCheck(pw) }
                    breach < 0 -> Text("Couldn't reach the breach service. Check your internet and try again.", color = K.amber)
                    breach == 0 -> Text("Good news: not found in known breaches.", color = K.green, fontWeight = FontWeight.Bold)
                    else -> Text("Leaked ${"%,d".format(breach)} times! Change it everywhere you use it.", color = K.red, fontWeight = FontWeight.Bold)
                }
            }

            SectionTitle("Tips")
            KCard(Modifier.fillMaxWidth(), accent = K.amber) {
                (result.tips.ifEmpty { listOf("Use 12+ characters", "Use a passphrase of 4 random words", "Turn on two-step verification") }).forEach {
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Icon(Icons.Filled.Lightbulb, null, tint = K.amber)
                        Spacer(Modifier.width(8.dp)); Text(it, color = K.text)
                    }
                }
            }
            Text("Follows NIST SP 800-63B: length matters most, and passwords found in breaches should never be used.", color = K.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

private object Color2 { val orangeRed = androidx.compose.ui.graphics.Color(0xFFEA580C) }
