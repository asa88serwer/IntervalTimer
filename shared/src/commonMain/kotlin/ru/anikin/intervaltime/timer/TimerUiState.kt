package ru.anikin.intervaltime.timer

import ru.anikin.intervaltime.data.model.PhaseType
import ru.anikin.intervaltime.data.model.Period
import ru.anikin.intervaltime.data.model.Workout

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class TimerUiState(
    val status: TimerStatus = TimerStatus.IDLE,
    val workout: Workout? = null,
    val currentPeriodIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val elapsedMillis: Long = 0L,
    val laps: List<Long> = emptyList(),
    val currentPhase: PhaseType = PhaseType.NEUTRAL,
    val currentPeriodName: String = ""
) {
    val totalPeriods: Int get() = workout?.periods?.size ?: 0
    val currentPeriod: Period? get() = workout?.periods?.getOrNull(currentPeriodIndex)
}
