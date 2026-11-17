package com.kavach.scanner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kavach.scanner.ui.theme.K
import kotlin.math.cos
import kotlin.math.sin

enum class LogoState { IDLE, SCANNING, SAFE, WARNING, DANGER }

/**
 * The Kavach mark: a saffron shield with circuit traces, an orbiting dashed ring with satellite nodes,
 * a radar ping, and a centre glyph (padlock → tick / ! / ✕) that follows the scan verdict.
 *
 * @param onDark draw for an orange/dark background (white shield) instead of a white one (orange shield).
 */
@Composable
fun KavachLogo(state: LogoState, modifier: Modifier = Modifier, size: Dp = 120.dp, onDark: Boolean = false) {
    val p = K
    val accent = when (state) {
        LogoState.IDLE, LogoState.SCANNING -> p.orange
        LogoState.SAFE -> p.green
        LogoState.WARNING -> p.amber
        LogoState.DANGER -> p.red
    }
    val tint by animateColorAsState(accent, tween(600), label = "tint")
    val ringColor = if (onDark) Color.White else tint
    val shieldTop = if (onDark) Color.White else tint
    val shieldBottom = if (onDark) Color(0xFFFFE3D6) else tint.copy(red = tint.red * 0.85f, green = tint.green * 0.8f, blue = tint.blue * 0.8f)
    val glyph = if (onDark) tint else Color.White

    val t = rememberInfiniteTransition(label = "logo")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (state == LogoState.SCANNING) 2000 else 9000, easing = LinearEasing)), label = "orbit")
    val ping by t.animateFloat(0f, 1f, infiniteRepeatable(tween(if (state == LogoState.DANGER) 900 else 2200, easing = FastOutSlowInEasing)), label = "ping")
    val scan by t.animateFloat(0.18f, 0.86f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scan")
    val breathe by t.animateFloat(0.96f, 1.0f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")

    Canvas(modifier.size(size)) {
        val c = center
        val r = this.size.minDimension / 2f

        // Radar ping
        drawCircle(ringColor.copy(alpha = 0.35f * (1f - ping)), radius = r * (0.55f + 0.45f * ping), center = c, style = Stroke(r * 0.02f))
        // Orbit ring (dashed) + satellites
        rotate(orbit, c) {
            drawCircle(
                ringColor.copy(alpha = 0.55f), radius = r * 0.93f, center = c,
                style = Stroke(r * 0.018f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.12f, r * 0.07f))),
            )
            for (k in 0 until 3) {
                val a = Math.toRadians(k * 120.0)
                drawCircle(ringColor, r * 0.045f, Offset(c.x + (r * 0.93f * cos(a)).toFloat(), c.y + (r * 0.93f * sin(a)).toFloat()))
            }
        }
        rotate(-orbit * 0.6f, c) {
            drawArc(ringColor.copy(alpha = 0.35f), 20f, 80f, false, Offset(c.x - r * 0.8f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(r * 0.03f, cap = StrokeCap.Round))
            drawArc(ringColor.copy(alpha = 0.35f), 200f, 80f, false, Offset(c.x - r * 0.8f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(r * 0.03f, cap = StrokeCap.Round))
        }

        // Shield
        val sw = r * 1.05f * breathe
        val shield = shieldPath(Offset(c.x - sw / 2, c.y - sw * 0.6f), Size(sw, sw * 1.2f))
        drawPath(shield, Color.Black.copy(alpha = 0.12f), style = Stroke(r * 0.08f, join = StrokeJoin.Round))
        drawPath(shield, Brush.verticalGradient(listOf(shieldTop, shieldBottom), startY = c.y - sw * 0.6f, endY = c.y + sw * 0.6f))
        clipPath(shield) {
            // circuit traces
            val traceColor = (if (onDark) tint else Color.White).copy(alpha = 0.22f)
            val sx = c.x - sw / 2; val sy = c.y - sw * 0.6f
            fun trace(vararg pts: Float) {
                val path = Path().apply { moveTo(sx + pts[0] * sw, sy + pts[1] * sw * 1.2f); var i = 2; while (i < pts.size) { lineTo(sx + pts[i] * sw, sy + pts[i + 1] * sw * 1.2f); i += 2 } }
                drawPath(path, traceColor, style = Stroke(r * 0.02f))
                drawCircle(traceColor, r * 0.03f, Offset(sx + pts[pts.size - 2] * sw, sy + pts[pts.size - 1] * sw * 1.2f))
            }
            trace(0.0f, 0.3f, 0.18f, 0.3f, 0.26f, 0.38f)
            trace(1.0f, 0.3f, 0.82f, 0.3f, 0.74f, 0.38f)
            trace(0.0f, 0.62f, 0.2f, 0.62f, 0.28f, 0.7f)
            trace(1.0f, 0.62f, 0.8f, 0.62f, 0.72f, 0.7f)
            trace(0.5f, 1.0f, 0.5f, 0.86f)
            if (state == LogoState.SCANNING) {
                val y = sy + sw * 1.2f * scan
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, glyph.copy(alpha = 0.55f), Color.Transparent), startY = y - r * 0.12f, endY = y + r * 0.12f),
                    topLeft = Offset(sx, y - r * 0.12f), size = Size(sw, r * 0.24f),
                )
            }
        }
        // inner outline
        val inner = shieldPath(Offset(c.x - sw * 0.42f, c.y - sw * 0.5f), Size(sw * 0.84f, sw * 1.0f))
        drawPath(inner, glyph.copy(alpha = 0.5f), style = Stroke(r * 0.018f, join = StrokeJoin.Round))
        drawGlyph(state, glyph, c, r * 0.5f)
    }
}

private fun shieldPath(o: Offset, s: Size): Path = Path().apply {
    val w = s.width; val h = s.height
    moveTo(o.x + w * 0.5f, o.y)
    cubicTo(o.x + w * 0.68f, o.y + h * 0.08f, o.x + w * 0.86f, o.y + h * 0.1f, o.x + w, o.y + h * 0.1f)
    lineTo(o.x + w, o.y + h * 0.45f)
    cubicTo(o.x + w, o.y + h * 0.75f, o.x + w * 0.75f, o.y + h * 0.9f, o.x + w * 0.5f, o.y + h)
    cubicTo(o.x + w * 0.25f, o.y + h * 0.9f, o.x, o.y + h * 0.75f, o.x, o.y + h * 0.45f)
    lineTo(o.x, o.y + h * 0.1f)
    cubicTo(o.x + w * 0.14f, o.y + h * 0.1f, o.x + w * 0.32f, o.y + h * 0.08f, o.x + w * 0.5f, o.y)
    close()
}

private fun DrawScope.drawGlyph(state: LogoState, color: Color, c: Offset, u: Float) {
    val st = Stroke(width = u * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (state) {
        LogoState.SAFE -> drawPath(Path().apply {
            moveTo(c.x - u * 0.42f, c.y); lineTo(c.x - u * 0.1f, c.y + u * 0.32f); lineTo(c.x + u * 0.45f, c.y - u * 0.3f)
        }, color, style = st)
        LogoState.WARNING -> {
            drawLine(color, Offset(c.x, c.y - u * 0.45f), Offset(c.x, c.y + u * 0.12f), u * 0.17f, StrokeCap.Round)
            drawCircle(color, u * 0.1f, Offset(c.x, c.y + u * 0.4f))
        }
        LogoState.DANGER -> {
            drawLine(color, Offset(c.x - u * 0.32f, c.y - u * 0.32f), Offset(c.x + u * 0.32f, c.y + u * 0.32f), u * 0.17f, StrokeCap.Round)
            drawLine(color, Offset(c.x + u * 0.32f, c.y - u * 0.32f), Offset(c.x - u * 0.32f, c.y + u * 0.32f), u * 0.17f, StrokeCap.Round)
        }
        LogoState.IDLE, LogoState.SCANNING -> {
            // padlock with keyhole
            drawArc(color, 180f, 180f, false, Offset(c.x - u * 0.28f, c.y - u * 0.55f), Size(u * 0.56f, u * 0.6f), style = st)
            drawRoundRect(color, Offset(c.x - u * 0.45f, c.y - u * 0.22f), Size(u * 0.9f, u * 0.7f), CornerRadius(u * 0.14f))
            val hole = if (color == Color.White) Color(0xFFE64A19) else Color.White
            drawCircle(hole, u * 0.1f, Offset(c.x, c.y + u * 0.05f))
            drawLine(hole, Offset(c.x, c.y + u * 0.08f), Offset(c.x, c.y + u * 0.3f), u * 0.08f, StrokeCap.Round)
        }
    }
}
