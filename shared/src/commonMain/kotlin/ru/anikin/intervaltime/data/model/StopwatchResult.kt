package ru.anikin.intervaltime.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class StopwatchResult(
    val id: String = Uuid.random().toString(),
    val label: String = "",
    val dateMillis: Long,
    val totalMillis: Long,
    val laps: List<Long> = emptyList()
)
