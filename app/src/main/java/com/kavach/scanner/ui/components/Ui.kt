package com.kavach.scanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavach.scanner.ui.theme.K

/** Springy press feedback for any clickable element. */
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press")
    return this.scale(scale).clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), enabled = enabled, onClick = onClick)
}

/** Fades and slides content in, delayed by [index] for a staggered entrance. */
@Composable
fun Modifier.enter(index: Int = 0): Modifier {
    val st = still
    val a = remember { Animatable(if (st) 1f else 0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60L * index)
        a.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 40f }
}

@Composable
fun KCard(modifier: Modifier = Modifier, accent: Color? = null, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = K.surface),
        border = BorderStroke(1.dp, accent?.copy(alpha = 0.45f) ?: K.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = if (K.isDark) 0.dp else 1.dp),
    ) { Column(Modifier.padding(padding), content = content) }
}

/** Primary call-to-action: orange gradient with a moving shine and press animation. */
@Composable
fun ShineButton(text: String, icon: ImageVector?, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = K.orange, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "shine")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "x")
    Box(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Brush.horizontalGradient(listOf(color, color.copy(red = (color.red * 0.9f)))) else Brush.horizontalGradient(listOf(K.outline, K.outline)))
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                        start = Offset(w * x - 120f, 0f), end = Offset(w * x + 120f, size.height),
                    ),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

/** Round tinted icon badge used on tiles and list rows. */
@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = if (K.isDark) 0.18f else 0.12f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(K.orange))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = K.text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, color = K.orange, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp))
        }
    }
}

/** Top app bar used by inner screens. */
@Composable
fun KTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().background(K.surface).statusBarsPadding().height(58.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = K.text) }
        else Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = K.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

/** Animated circular score meter. [goodHigh] = true when a high number is good (device score). */
@Composable
fun ScoreRing(score: Int, color: Color, label: String, modifier: Modifier = Modifier, size: Dp = 150.dp, trackColor: Color = K.outline, textColor: Color = color, labelColor: Color = K.textDim) {
    val st = still
    val anim = remember { Animatable(if (st) score.toFloat() else 0f) }
    LaunchedEffect(score) { anim.animateTo(score.toFloat(), tween(1200, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.085f
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(trackColor, 135f, 270f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * (anim.value / 100f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${anim.value.toInt()}", fontSize = (size.value * 0.26f).sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

/** A list row with a badge, title, subtitle and optional trailing content. */
@Composable
fun KRow(icon: ImageVector, color: Color, title: String, subtitle: String?, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, color, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = K.text)
            if (subtitle != null) Text(subtitle, color = K.textDim, style = MaterialTheme.typography.bodyMedium)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color).border(0.dp, Color.Transparent, CircleShape))
}

/** True in design previews and screenshot tests, where animations start at their end state. */
internal val still: Boolean
    @Composable get() = androidx.compose.ui.platform.LocalInspectionMode.current
