package ru.anikin.intervaltime.tts

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/** Обёртка над AVSpeechSynthesizer (iOS). */
actual object VoiceAnnouncer {

    private val synthesizer = AVSpeechSynthesizer()
    private var languageCode = "ru-RU"

    fun setLanguage(code: String) {
        languageCode = code
    }

    actual fun speak(text: String) {
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(languageCode)
        synthesizer.speakUtterance(utterance)
    }

    actual fun stopSpeaking() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}
