package ru.anikin.intervaltime.ui.screens.stopwatch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.anikin.intervaltime.data.ResultsRepository
import ru.anikin.intervaltime.data.model.StopwatchResult
import ru.anikin.intervaltime.data.model.Workout
import ru.anikin.intervaltime.data.model.WorkoutMode
import ru.anikin.intervaltime.timer.TimerEngine
import ru.anikin.intervaltime.timer.TimerEngines
import ru.anikin.intervaltime.timer.TimerService
import ru.anikin.intervaltime.timer.TimerStatus
import ru.anikin.intervaltime.ui.theme.AppBackground

/**
 * Два независимых секундомера на одном экране — каждый работает через свой экземпляр
 * TimerEngine (см. TimerEngines), поэтому оба можно запустить, поставить на паузу
 * и остановить по отдельности, не влияя друг на друга.
 */
@Composable
fun DualStopwatchScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
            Text("Секундомеры", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        StopwatchPanel(
            label = "Секундомер 1",
            engine = TimerEngines.stopwatchA,
            hueSeed = 170f,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        StopwatchPanel(
            label = "Секундомер 2",
            engine = TimerEngines.stopwatchB,
            hueSeed = 300f,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StopwatchPanel(
    label: String,
    engine: TimerEngine,
    hueSeed: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by engine.state.collectAsState()

    // Приятный "живой" градиент: три оттенка одного цветового семейства (свой у каждого
    // секундомера — hueSeed) непрерывно и медленно смещаются по кругу.
    val infiniteTransition = rememberInfiniteTransition(label = "gradient-$label")
    val hueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "hueShift"
    )

    val hue1 = (hueSeed + hueShift) % 360f
    val hue2 = (hueSeed + hueShift + 70f) % 360f
    val hue3 = (hueSeed + hueShift + 140f) % 360f
    val color1 = Color.hsv(hue1, 0.55f, 0.60f)
    val color2 = Color.hsv(hue2, 0.60f, 0.42f)
    val color3 = Color.hsv(hue3, 0.55f, 0.28f)

    fun stopAndSave() {
        if (state.elapsedMillis > 0) {
            ResultsRepository.save(
                context,
                StopwatchResult(
                    label = label,
                    dateMillis = System.currentTimeMillis(),
                    totalMillis = state.elapsedMillis,
                    laps = state.laps
                )
            )
        }
        engine.stop()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(color1, color2, color3)))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                formatStopwatch(state.elapsedMillis),
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            when (state.status) {
                TimerStatus.IDLE -> {
                    Button(onClick = {
                        engine.startWorkout(Workout(name = label, mode = WorkoutMode.STOPWATCH))
                        TimerService.start(context)
                    }) { Text("Старт") }
                }
                TimerStatus.RUNNING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { engine.pause() }) { Text("Пауза") }
                        OutlinedButton(onClick = { engine.recordLap() }) { Text("Круг") }
                        OutlinedButton(onClick = { stopAndSave() }) { Text("Стоп") }
                    }
                }
                TimerStatus.PAUSED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { engine.resume() }) { Text("Продолжить") }
                        OutlinedButton(onClick = { stopAndSave() }) { Text("Стоп") }
                    }
                }
                TimerStatus.FINISHED -> Unit
            }

            if (state.laps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(state.laps.size) { i ->
                        val lapMillis = state.laps[i]
                        val lapNumber = state.laps.size - i
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Круг $lapNumber",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                formatStopwatch(lapMillis),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatStopwatch(totalMillis: Long): String {
    val safe = totalMillis.coerceAtLeast(0)
    val m = safe / 60000
    val s = (safe / 1000) % 60
    val centis = (safe % 1000) / 10
    return "%d:%02d.%02d".format(m, s, centis)
}
