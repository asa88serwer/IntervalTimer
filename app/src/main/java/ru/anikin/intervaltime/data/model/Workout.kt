package ru.anikin.intervaltime.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mode: WorkoutMode = WorkoutMode.INTERVALS,
    val periods: List<Period> = emptyList()
) {
    val totalDurationSeconds: Int
        get() = periods.sumOf { it.durationSeconds }
}
