package ru.anikin.intervaltime.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import ru.anikin.intervaltime.data.model.PhaseType
import ru.anikin.intervaltime.data.model.Period
import ru.anikin.intervaltime.ui.theme.NeutralColor
import ru.anikin.intervaltime.ui.theme.RestColor
import ru.anikin.intervaltime.ui.theme.WorkColor

/**
 * Кольцо из дуг-сегментов вокруг центральной кнопки Старт — по одной дуге на период.
 * Медленно вращается непрерывно, а при изменении числа периодов (activeIndex == -1, режим
 * настройки) делает пружинный "пульс" — это и есть "динамическое движение вокруг кнопки".
 */
@Composable
fun PeriodRing(
    periods: List<Period>,
    activeIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "rotation"
    )

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(periods.size) {
        pulse.snapTo(1.12f)
        pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
            rotationZ = rotation
        }
    ) {
        val strokeWidth = 14.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        if (periods.isEmpty()) {
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        val gapDegrees = if (periods.size == 1) 0f else 6f
        val sweepEach = 360f / periods.size - gapDegrees
        var startAngle = -90f
        periods.forEachIndexed { index, period ->
            val color = when (period.phase) {
                PhaseType.WORK -> WorkColor
                PhaseType.REST -> RestColor
                PhaseType.NEUTRAL -> NeutralColor
            }
            val alpha = if (activeIndex == -1 || index == activeIndex) 1f else 0.3f
            drawArc(
                color = color.copy(alpha = alpha),
                startAngle = startAngle,
                sweepAngle = sweepEach,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += 360f / periods.size
        }
    }
}
