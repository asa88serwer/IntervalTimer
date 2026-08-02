package ru.anikin.intervaltime.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PhaseType {
    WORK,
    REST,
    NEUTRAL
}
