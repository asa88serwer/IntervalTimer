package ru.anikin.intervaltime.timer

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import ru.anikin.intervaltime.notification.TimerNotifications

/**
 * Foreground-сервис держит процесс живым, пока хотя бы один из движков (обратный отсчёт,
 * секундомер 1, секундомер 2) работает в фоне, и показывает постоянное уведомление.
 * Сама логика отсчёта живёт в TimerEngines — сервис только не даёт системе убить процесс
 * и синхронизирует уведомление с состоянием движков.
 */
class TimerService : LifecycleService() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var collectorStarted = false
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IntervalTime:TimerWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // startForeground вызывается один раз при первом запуске — все последующие обновления
        // уведомления идут только через refreshNotification (реакция на подтверждённое состояние
        // движков). Иначе повторный startForeground на каждое действие читал бы state.value
        // синхронно, до того как асинхронная команда успевала примениться.
        if (!foregroundStarted) {
            foregroundStarted = true
            startForeground(NOTIFICATION_ID, buildCurrentNotification())
        }

        when (intent?.action) {
            ACTION_PAUSE -> TimerEngines.countdown.pause()
            ACTION_RESUME -> TimerEngines.countdown.resume()
            ACTION_SKIP -> TimerEngines.countdown.skipToNext()
            ACTION_STOP -> TimerEngines.countdown.stop()
            ACTION_STOP_STOPWATCHES -> {
                TimerEngines.stopwatchA.stop()
                TimerEngines.stopwatchB.stop()
            }
            else -> Unit
        }

        if (!collectorStarted) {
            collectorStarted = true
            merge(TimerEngines.countdown.state, TimerEngines.stopwatchA.state, TimerEngines.stopwatchB.state)
                .onEach { refreshNotification() }
                .launchIn(lifecycleScope)
        }

        return START_NOT_STICKY
    }

    private fun buildCurrentNotification(): Notification {
        val countdown = TimerEngines.countdown.state.value
        return if (countdown.status != TimerStatus.IDLE) {
            TimerNotifications.build(this, countdown)
        } else {
            TimerNotifications.buildStopwatches(this, TimerEngines.stopwatchA.state.value, TimerEngines.stopwatchB.state.value)
        }
    }

    private fun refreshNotification() {
        val countdown = TimerEngines.countdown.state.value
        val swA = TimerEngines.stopwatchA.state.value
        val swB = TimerEngines.stopwatchB.state.value

        val anyActive = countdown.status != TimerStatus.IDLE || swA.status != TimerStatus.IDLE || swB.status != TimerStatus.IDLE
        if (!anyActive) {
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val anyRunning = countdown.status == TimerStatus.RUNNING ||
            swA.status == TimerStatus.RUNNING ||
            swB.status == TimerStatus.RUNNING
        if (anyRunning) acquireWakeLock() else releaseWakeLock()

        val notification = if (countdown.status != TimerStatus.IDLE) {
            TimerNotifications.build(this, countdown)
        } else {
            TimerNotifications.buildStopwatches(this, swA, swB)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val wl = wakeLock ?: return
        if (!wl.isHeld) wl.acquire(2 * 60 * 60 * 1000L)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ru.anikin.intervaltime.action.START"
        const val ACTION_PAUSE = "ru.anikin.intervaltime.action.PAUSE"
        const val ACTION_RESUME = "ru.anikin.intervaltime.action.RESUME"
        const val ACTION_SKIP = "ru.anikin.intervaltime.action.SKIP"
        const val ACTION_STOP = "ru.anikin.intervaltime.action.STOP"
        const val ACTION_STOP_STOPWATCHES = "ru.anikin.intervaltime.action.STOP_STOPWATCHES"

        fun start(context: Context) {
            val intent = Intent(context, TimerService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, TimerService::class.java).setAction(action)
            context.startService(intent)
        }
    }
}
