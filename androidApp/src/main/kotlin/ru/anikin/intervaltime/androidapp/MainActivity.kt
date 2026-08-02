package ru.anikin.intervaltime.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.anikin.intervaltime.shared.App
import ru.anikin.intervaltime.tts.VoiceAnnouncer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VoiceAnnouncer.init(this)
        setContent {
            App()
        }
    }
}
