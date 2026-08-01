package ru.anikin.intervaltime.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ru.anikin.intervaltime.IntervalTimeApp
import ru.anikin.intervaltime.MainActivity
import ru.anikin.intervaltime.data.model.WorkoutMode
import ru.anikin.intervaltime.timer.TimerService
import ru.anikin.intervaltime.timer.TimerStatus
import ru.anikin.intervaltime.timer.TimerUiState

/** Собирает постоянное уведомление таймера — с прогрессом периода и кнопками управления. */
object TimerNotifications {

    fun build(context: Context, state: TimerUiState): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = state.currentPeriodName.ifBlank { "IntervalTime" }
        val text = when {
            state.workout?.mode == WorkoutMode.STOPWATCH -> formatTime((state.elapsedMillis / 1000).toInt())
            state.status == TimerStatus.FINISHED -> "Готово"
            else -> "${formatTime(state.remainingSeconds)} · период ${state.currentPeriodIndex + 1} из ${state.totalPeriods}"
        }

        val builder = NotificationCompat.Builder(context, IntervalTimeApp.TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(state.status == TimerStatus.RUNNING || state.status == TimerStatus.PAUSED)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)

        val period = state.currentPeriod
        if (state.workout?.mode == WorkoutMode.INTERVALS && period != null && state.status != TimerStatus.FINISHED) {
            val elapsed = (period.durationSeconds - state.remainingSeconds).coerceIn(0, period.durationSeconds)
            builder.setProgress(period.durationSeconds, elapsed, false)
        }

        when (state.status) {
            TimerStatus.RUNNING -> {
                builder.addAction(actionOf(context, android.R.drawable.ic_media_pause, "Пауза", TimerService.ACTION_PAUSE))
                if (state.workout?.mode == WorkoutMode.INTERVALS) {
                    builder.addAction(actionOf(context, android.R.drawable.ic_media_next, "Далее", TimerService.ACTION_SKIP))
                }
                builder.addAction(actionOf(context, android.R.drawable.ic_menu_close_clear_cancel, "Стоп", TimerService.ACTION_STOP))
            }
            TimerStatus.PAUSED -> {
                builder.addAction(actionOf(context, android.R.drawable.ic_media_play, "Продолжить", TimerService.ACTION_RESUME))
                builder.addAction(actionOf(context, android.R.drawable.ic_menu_close_clear_cancel, "Стоп", TimerService.ACTION_STOP))
            }
            else -> {
                builder.addAction(actionOf(context, android.R.drawable.ic_menu_close_clear_cancel, "Закрыть", TimerService.ACTION_STOP))
            }
        }

        return builder.build()
    }

    /** Комбинированное уведомление для двух параллельно работающих секундомеров. */
    fun buildStopwatches(context: Context, a: TimerUiState, b: TimerUiState): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lineA = "Секундомер 1: " + describeStopwatch(a)
        val lineB = "Секундомер 2: " + describeStopwatch(b)

        val builder = NotificationCompat.Builder(context, IntervalTimeApp.TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Секундомеры")
            .setContentText("$lineA · $lineB")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$lineA\n$lineB"))
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(a.status != TimerStatus.IDLE || b.status != TimerStatus.IDLE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .addAction(actionOf(context, android.R.drawable.ic_menu_close_clear_cancel, "Стоп всё", TimerService.ACTION_STOP_STOPWATCHES))

        return builder.build()
    }

    private fun describeStopwatch(state: TimerUiState): String = when (state.status) {
        TimerStatus.IDLE -> "—"
        TimerStatus.PAUSED -> "${formatTime((state.elapsedMillis / 1000).toInt())} (пауза)"
        else -> formatTime((state.elapsedMillis / 1000).toInt())
    }

    private fun actionOf(context: Context, icon: Int, label: String, action: String): NotificationCompat.Action {
        val intent = Intent(context, TimerService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(icon, label, pendingIntent)
    }

    private fun formatTime(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%d:%02d".format(m, s)
    }
}
