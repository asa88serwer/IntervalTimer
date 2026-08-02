package ru.anikin.intervaltime.tts

/** Голосовые оповещения о ходе тренировки. Платформенная реализация — в androidMain/iosMain. */
expect object VoiceAnnouncer {
    fun speak(text: String)
    fun stopSpeaking()
}
