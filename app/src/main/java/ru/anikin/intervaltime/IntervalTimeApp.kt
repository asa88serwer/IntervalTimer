package ru.anikin.intervaltime

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import ru.anikin.intervaltime.data.ResultsRepository
import ru.anikin.intervaltime.data.TemplateRepository

class IntervalTimeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        TemplateRepository.init(this)
        ResultsRepository.init(this)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            TIMER_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val TIMER_CHANNEL_ID = "timer_channel"
    }
}
