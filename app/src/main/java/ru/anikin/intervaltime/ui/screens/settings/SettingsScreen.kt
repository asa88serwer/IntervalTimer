package ru.anikin.intervaltime.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.anikin.intervaltime.tts.VoiceAnnouncer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var speechRate by remember { mutableStateOf(1.0f) }
    var isRussian by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Голос", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isRussian,
                    onClick = {
                        isRussian = true
                        VoiceAnnouncer.setLanguage(Locale("ru", "RU"))
                    },
                    label = { Text("Русский") }
                )
                FilterChip(
                    selected = !isRussian,
                    onClick = {
                        isRussian = false
                        VoiceAnnouncer.setLanguage(Locale.ENGLISH)
                    },
                    label = { Text("English") }
                )
            }

            Column {
                Text("Скорость речи: %.1fx".format(speechRate))
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        VoiceAnnouncer.setSpeechRate(it)
                    },
                    valueRange = 0.5f..2.0f
                )
            }

            OutlinedButton(onClick = {
                VoiceAnnouncer.speak(if (isRussian) "Проверка голоса. Три. Два. Один." else "Voice check. Three. Two. One.")
            }) {
                Text("Проверить голос")
            }
        }
    }
}
