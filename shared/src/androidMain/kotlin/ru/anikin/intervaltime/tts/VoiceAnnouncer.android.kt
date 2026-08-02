package ru.anikin.intervaltime.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Обёртка над Android TextToSpeech. Синглтон, живёт в масштабе процесса —
 * доступен и из UI, и из фонового TimerService без лишней синхронизации.
 */
actual object VoiceAnnouncer {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pendingQueue = mutableListOf<String>()
    private var speechRate = 1.0f
    private var locale: Locale = Locale("ru", "RU")

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = locale
                tts?.setSpeechRate(speechRate)
                pendingQueue.forEach { enqueue(it) }
                pendingQueue.clear()
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun setLanguage(newLocale: Locale) {
        locale = newLocale
        tts?.language = newLocale
    }

    actual fun speak(text: String) {
        if (!ready) {
            pendingQueue.add(text)
            return
        }
        enqueue(text)
    }

    private fun enqueue(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    actual fun stopSpeaking() {
        tts?.stop()
        pendingQueue.clear()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
