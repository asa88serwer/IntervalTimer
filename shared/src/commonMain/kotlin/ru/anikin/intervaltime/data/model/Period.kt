package ru.anikin.intervaltime.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Period(
    val id: String = Uuid.random().toString(),
    val name: String,
    val phase: PhaseType,
    val durationSeconds: Int
)
