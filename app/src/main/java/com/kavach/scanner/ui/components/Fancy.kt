package com.kavach.scanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavach.scanner.ui.theme.K
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Subtle hexagon "cyber mesh" texture with a slow light sweep. Draw behind hero content. */
@Composable
fun HexMesh(modifier: Modifier = Modifier, color: Color = Color.White) {
    val t = rememberInfiniteTransition(label = "hex")
    val sweep by t.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "sweep")
    Canvas(modifier) {
        val r = 22.dp.toPx()
        val w = sqrt(3f) * r
        val h = 1.5f * r
        var row = 0
        var y = 0f
        while (y < size.height + r) {
            var x = if (row % 2 == 0) 0f else w / 2
            while (x < size.width + w) {
                val d = (x / size.width) - sweep
                val glow = (1f - (d * d * 12f)).coerceIn(0f, 1f)
                val p = Path()
                for (k in 0..5) {
                    val a = Math.toRadians((60.0 * k) - 30.0)
                    val px = x + r * cos(a).toFloat(); val py = y + r * sin(a).toFloat()
                    if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
                }
                p.close()
                drawPath(p, color.copy(alpha = 0.06f + 0.12f * glow), style = Stroke(1.2f))
                x += w
            }
            y += h; row++
        }
    }
}

/** Segmented security-score gauge (60 ticks) with an animated fill, glow dot and centre label. */
@Composable
fun SegmentedGauge(
    score: Int, modifier: Modifier = Modifier, size: Dp = 170.dp, activeColor: Color = Color.White,
    trackColor: Color = Color.White.copy(alpha = 0.25f), label: String = "SECURITY", textColor: Color = Color.White,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(score) { anim.animateTo(score.toFloat(), tween(1400, easing = FastOutSlowInEasing)) }
    val t = rememberInfiniteTransition(label = "g")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "spin")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val c = center; val R = this.size.minDimension / 2
            val ticks = 60
            val filled = (anim.value / 100f * ticks).toInt()
            for (i in 0 until ticks) {
                val a = Math.toRadians(-90.0 + i * 360.0 / ticks)
                val inner = R * 0.80f; val outer = R * (if (i % 5 == 0) 0.97f else 0.92f)
                drawLine(
                    if (i < filled) activeColor else trackColor,
                    Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                    Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                    strokeWidth = R * 0.035f, cap = StrokeCap.Round,
                )
            }
            rotate(spin, c) {
                drawArc(activeColor.copy(alpha = 0.35f), 0f, 70f, false, Offset(c.x - R * 0.7f, c.y - R * 0.7f), Size(R * 1.4f, R * 1.4f), style = Stroke(R * 0.02f, cap = StrokeCap.Round))
                drawArc(activeColor.copy(alpha = 0.35f), 180f, 70f, false, Offset(c.x - R * 0.7f, c.y - R * 0.7f), Size(R * 1.4f, R * 1.4f), style = Stroke(R * 0.02f, cap = StrokeCap.Round))
            }
            val ea = Math.toRadians(-90.0 + anim.value / 100.0 * 360.0)
            val dot = Offset(c.x + R * 0.88f * cos(ea).toFloat(), c.y + R * 0.88f * sin(ea).toFloat())
            drawCircle(activeColor.copy(alpha = 0.35f), R * 0.09f, dot)
            drawCircle(activeColor, R * 0.045f, dot)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${anim.value.toInt()}", color = textColor, fontSize = (size.value * 0.27f).sp, fontWeight = FontWeight.Black)
            Text(label, color = textColor.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Round gradient action button with expanding "sonar" rings, icon and caption. */
@Composable
fun PulseAction(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, size: Dp = 62.dp, delayMs: Int = 0, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "pa")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), initialStartOffset = StartOffset(delayMs)), label = "ring")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size + 18.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size + 18.dp)) {
                val base = size.toPx() / 2
                drawCircle(color.copy(alpha = 0.25f * (1f - ring)), base + (this.size.minDimension / 2 - base) * ring)
            }
            Box(
                Modifier.size(size).shadow(8.dp, CircleShape, ambientColor = color, spotColor = color).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color)))
                    .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .pressable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(size * 0.45f)) }
        }
        Text(label, color = K.text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Small coloured status pill like "ON", "82/100", "AT RISK". */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(if (filled) color else color.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, color = if (filled) Color.White else color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, maxLines = 1) }
}

/** Thin animated progress bar. */
@Composable
fun KProgress(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(K.outline)) {
        Box(Modifier.fillMaxWidth(anim.value).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color))))
    }
}

/** Gradient square icon tile (used for modules). */
@Composable
fun GradientIcon(icon: ImageVector, color: Color, size: Dp = 46.dp) {
    Box(
        Modifier.size(size).shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = color, spotColor = color).clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.75f), color))),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.52f)) }
}

/** A breathing glow dot used for live status. */
@Composable
fun LiveDot(color: Color, size: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(size * 2), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 2).clip(CircleShape).background(color.copy(alpha = 0.25f * a)))
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

@Suppress("unused") private val keep = Modifier.padding(0.dp)
