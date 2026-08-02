package ru.anikin.intervaltime.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Workout(
    val id: String = Uuid.random().toString(),
    val name: String,
    val mode: WorkoutMode = WorkoutMode.INTERVALS,
    val periods: List<Period> = emptyList()
) {
    val totalDurationSeconds: Int
        get() = periods.sumOf { it.durationSeconds }
}
