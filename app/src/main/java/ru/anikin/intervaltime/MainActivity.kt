package ru.anikin.intervaltime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import ru.anikin.intervaltime.tts.VoiceAnnouncer
import ru.anikin.intervaltime.ui.navigation.IntervalTimeNavHost
import ru.anikin.intervaltime.ui.theme.IntervalTimeTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* без разрешения просто не будет видно системное уведомление таймера */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VoiceAnnouncer.init(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            IntervalTimeTheme {
                val navController = rememberNavController()
                IntervalTimeNavHost(navController)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
