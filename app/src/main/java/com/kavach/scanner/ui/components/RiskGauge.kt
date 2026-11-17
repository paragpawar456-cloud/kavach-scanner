package com.kavach.scanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavach.scanner.ui.theme.KColors

/** Animated 0-100 risk meter (green → amber → red). */
@Composable
fun RiskGauge(score: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(score) { anim.animateTo(score.toFloat(), tween(1200, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.08f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(KColors.Outline, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(KColors.Green, KColors.Amber, KColors.Red, KColors.Green)),
                135f, 270f * (anim.value / 100f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${anim.value.toInt()}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = color)
            Text("RISK SCORE", style = MaterialTheme.typography.labelSmall, color = KColors.TextDim)
        }
    }
}
