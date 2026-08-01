package ru.anikin.intervaltime.ui.screens.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.anikin.intervaltime.data.model.PhaseType
import ru.anikin.intervaltime.timer.TimerEngines
import ru.anikin.intervaltime.timer.TimerStatus
import ru.anikin.intervaltime.ui.theme.NeutralColor
import ru.anikin.intervaltime.ui.theme.RestColor
import ru.anikin.intervaltime.ui.theme.WorkColor

/** Экран работающего обратного отсчёта. Секундомеры показываются на отдельном DualStopwatchScreen. */
@Composable
fun TimerRunningScreen(onFinishedClose: () -> Unit) {
    val engine = TimerEngines.countdown
    val state by engine.state.collectAsState()

    if (state.status == TimerStatus.IDLE) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Нет активного таймера", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onFinishedClose) { Text("Назад") }
            }
        }
        return
    }

    val phaseColor by animateColorAsState(
        targetValue = when (state.currentPhase) {
            PhaseType.WORK -> WorkColor
            PhaseType.REST -> RestColor
            PhaseType.NEUTRAL -> NeutralColor
        },
        label = "phaseColor"
    )

    val backgroundColor = if (state.status == TimerStatus.FINISHED) {
        MaterialTheme.colorScheme.background
    } else {
        phaseColor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = state.currentPeriodName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(text = formatTime(state.remainingSeconds), fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (state.status != TimerStatus.FINISHED) {
                Text(
                    "Период ${(state.currentPeriodIndex + 1).coerceAtMost(state.totalPeriods)} из ${state.totalPeriods}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(32.dp))

            when (state.status) {
                TimerStatus.RUNNING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { engine.pause() }) { Text("Пауза") }
                        OutlinedButton(onClick = { engine.skipToNext() }) { Text("Далее") }
                        OutlinedButton(onClick = { engine.stop() }) { Text("Стоп") }
                    }
                }
                TimerStatus.PAUSED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { engine.resume() }) { Text("Продолжить") }
                        OutlinedButton(onClick = { engine.stop() }) { Text("Стоп") }
                    }
                }
                TimerStatus.FINISHED -> {
                    Text("Готово!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        engine.stop()
                        onFinishedClose()
                    }) { Text("Закрыть") }
                }
                TimerStatus.IDLE -> Unit
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val m = safe / 60
    val s = safe % 60
    return "%d:%02d".format(m, s)
}
