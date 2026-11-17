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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kavach.scanner.ui.theme.KColors

enum class LogoState { IDLE, SCANNING, SAFE, WARNING, DANGER }

/**
 * Animated cyber-shield: a glowing outline with a rotating radar sweep, a moving scan line while scanning,
 * and a centre symbol that changes with the verdict (lock → check / ! / ✕).
 */
@Composable
fun ShieldLogo(state: LogoState, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val target = when (state) {
        LogoState.IDLE, LogoState.SCANNING -> KColors.Cyan
        LogoState.SAFE -> KColors.Green
        LogoState.WARNING -> KColors.Amber
        LogoState.DANGER -> KColors.Red
    }
    val color by animateColorAsState(target, tween(600), label = "logoColor")
    val t = rememberInfiniteTransition(label = "logo")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (state == LogoState.SCANNING) 1400 else 4200, easing = LinearEasing)), label = "sweep")
    val pulse by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(if (state == LogoState.DANGER) 500 else 1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val scan by t.animateFloat(0.12f, 0.9f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scan")

    Canvas(modifier.size(size)) {
        val shield = shieldPath(this.size)
        val stroke = this.size.minDimension * 0.045f

        // Outer glow
        for (i in 3 downTo 1) {
            drawPath(shield, color.copy(alpha = 0.07f * pulse * i), style = Stroke(width = stroke * (2.5f * i), join = StrokeJoin.Round))
        }
        // Body
        drawPath(shield, Brush.verticalGradient(listOf(Color(0xFF13223B), Color(0xFF060A12))))

        clipPath(shield) {
            // Circuit grid
            val step = this.size.width / 8f
            for (k in 1..7) {
                drawLine(color.copy(alpha = 0.08f), Offset(step * k, 0f), Offset(step * k, this.size.height), 1f)
                drawLine(color.copy(alpha = 0.08f), Offset(0f, step * k), Offset(this.size.width, step * k), 1f)
            }
            // Radar sweep
            rotate(sweep, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, color.copy(alpha = 0.35f)), center),
                    startAngle = 0f, sweepAngle = 360f, useCenter = true,
                    topLeft = Offset(center.x - this.size.width, center.y - this.size.width),
                    size = Size(this.size.width * 2, this.size.width * 2),
                )
            }
            if (state == LogoState.SCANNING) {
                val y = this.size.height * scan
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.55f), Color.Transparent), startY = y - 18f, endY = y + 18f),
                    topLeft = Offset(0f, y - 18f), size = Size(this.size.width, 36f),
                )
            }
        }
        // Outline
        drawPath(shield, Brush.linearGradient(listOf(color, color.copy(alpha = 0.5f), color)), style = Stroke(width = stroke, join = StrokeJoin.Round))
        drawSymbol(state, color, stroke * 1.4f)
    }
}

private fun shieldPath(s: Size): Path = Path().apply {
    val w = s.width; val h = s.height
    moveTo(w * 0.5f, h * 0.05f)
    lineTo(w * 0.88f, h * 0.19f)
    lineTo(w * 0.88f, h * 0.48f)
    cubicTo(w * 0.88f, h * 0.73f, w * 0.7f, h * 0.88f, w * 0.5f, h * 0.96f)
    cubicTo(w * 0.3f, h * 0.88f, w * 0.12f, h * 0.73f, w * 0.12f, h * 0.48f)
    lineTo(w * 0.12f, h * 0.19f)
    close()
}

private fun DrawScope.drawSymbol(state: LogoState, color: Color, width: Float) {
    val w = size.width; val h = size.height
    val st = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (state) {
        LogoState.SAFE -> drawPath(Path().apply {
            moveTo(w * 0.34f, h * 0.5f); lineTo(w * 0.46f, h * 0.62f); lineTo(w * 0.68f, h * 0.38f)
        }, color, style = st)
        LogoState.WARNING -> {
            drawLine(color, Offset(w * 0.5f, h * 0.3f), Offset(w * 0.5f, h * 0.56f), width, StrokeCap.Round)
            drawCircle(color, width * 0.7f, Offset(w * 0.5f, h * 0.68f))
        }
        LogoState.DANGER -> {
            drawLine(color, Offset(w * 0.38f, h * 0.36f), Offset(w * 0.62f, h * 0.62f), width, StrokeCap.Round)
            drawLine(color, Offset(w * 0.62f, h * 0.36f), Offset(w * 0.38f, h * 0.62f), width, StrokeCap.Round)
        }
        LogoState.IDLE, LogoState.SCANNING -> {
            // Padlock
            val bodyTop = h * 0.47f
            drawRoundRect(color, Offset(w * 0.37f, bodyTop), Size(w * 0.26f, h * 0.2f), androidx.compose.ui.geometry.CornerRadius(width), style = st)
            drawArc(color, 180f, 180f, false, Offset(w * 0.415f, h * 0.33f), Size(w * 0.17f, h * 0.2f), style = st)
            drawCircle(color, width * 0.55f, Offset(w * 0.5f, bodyTop + h * 0.09f))
        }
    }
}
