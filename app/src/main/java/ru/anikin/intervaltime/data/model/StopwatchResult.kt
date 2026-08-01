package ru.anikin.intervaltime.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StopwatchResult(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val dateMillis: Long,
    val totalMillis: Long,
    val laps: List<Long> = emptyList()
)
