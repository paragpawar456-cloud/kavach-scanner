package com.kavach.scanner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavach.scanner.ui.theme.KColors

@Composable
fun CyberCard(modifier: Modifier = Modifier, accent: Color = KColors.Outline, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = KColors.Surface),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.7f), KColors.Outline))),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun Chip(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp),
    ) { Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.padding(top = 8.dp, bottom = 6.dp), style = MaterialTheme.typography.labelSmall, color = KColors.Cyan)
}

/** Subtle animated background grid shared by all screens. */
@Composable
fun CyberBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF0A1630), KColors.Bg, KColors.Bg))),
    ) { content() }
}
