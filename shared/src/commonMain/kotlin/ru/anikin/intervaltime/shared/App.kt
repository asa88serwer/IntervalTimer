package ru.anikin.intervaltime.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.anikin.intervaltime.data.IntervalBuilder
import ru.anikin.intervaltime.data.model.Workout
import ru.anikin.intervaltime.data.model.WorkoutMode
import ru.anikin.intervaltime.timer.TimerEngines
import ru.anikin.intervaltime.timer.TimerStatus
import ru.anikin.intervaltime.ui.components.PeriodRing

/**
 * Демо-экран: короткая тренировка (3 раунда по 5с работы / 3с отдыха), запускаемая через
 * тот же TimerEngine, что и настоящее приложение — проверка, что общая логика таймера
 * и голосовые оповещения реально работают на обеих платформах.
 */
@Composable
fun App() {
    val demoWorkout = remember {
        Workout(
            name = "Демо",
            mode = WorkoutMode.INTERVALS,
            periods = IntervalBuilder.build(count = 3, workSeconds = 5, restSeconds = 3, includeRest = true)
        )
    }
    val state by TimerEngines.countdown.state.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("IntervalTime — пилот на Compose Multiplatform")

                Box(modifier = Modifier.padding(32.dp).size(220.dp), contentAlignment = Alignment.Center) {
                    PeriodRing(periods = demoWorkout.periods, activeIndex = state.currentPeriodIndex)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.currentPeriodName.ifBlank { "Готово" })
                        Text("${state.remainingSeconds} с")
                    }
                }

                Button(onClick = {
                    if (state.status == TimerStatus.RUNNING) {
                        TimerEngines.countdown.stop()
                    } else {
                        TimerEngines.countdown.startWorkout(demoWorkout)
                    }
                }) {
                    Text(if (state.status == TimerStatus.RUNNING) "Стоп" else "Старт демо-тренировки")
                }
            }
        }
    }
}
