package ru.anikin.intervaltime.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Period(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phase: PhaseType,
    val durationSeconds: Int
)
