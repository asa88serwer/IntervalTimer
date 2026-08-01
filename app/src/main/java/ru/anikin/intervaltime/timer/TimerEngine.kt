package ru.anikin.intervaltime.timer

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.anikin.intervaltime.data.model.PhaseType
import ru.anikin.intervaltime.data.model.Workout
import ru.anikin.intervaltime.data.model.WorkoutMode
import ru.anikin.intervaltime.tts.VoiceAnnouncer

/**
 * Движок таймера/секундомера. Не зависит от Android UI-компонентов, поэтому состояние
 * экземпляра переживает повороты экрана и работу в фоне — Activity и TimerService читают
 * один и тот же StateFlow конкретного экземпляра (см. TimerEngines).
 *
 * Класс, а не object — чтобы можно было создавать несколько независимых экземпляров
 * (например, два параллельно работающих секундомера). Каждый экземпляр полностью изолирован:
 * свой поток-диспетчер, своё состояние, свой тикающий цикл.
 *
 * Время считается через SystemClock.elapsedRealtime() (не System.currentTimeMillis
 * и не delay-аккумуляцию), чтобы не накапливать дрейф, если тики иногда опаздывают.
 */
class TimerEngine {

    // Все мутации состояния идут через один и тот же поток (parallelism = 1):
    // и тиканье, и команды паузы/пропуска/стопа с UI-потока сериализуются здесь,
    // без чего запись в periodStartRealtime/stopwatchStartRealtime могла бы гоняться
    // между потоком UI и потоком тикающей корутины.
    private val engineDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val engineScope = CoroutineScope(SupervisorJob() + engineDispatcher)
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var periodStartRealtime: Long = 0L
    private var stopwatchStartRealtime: Long = 0L

    private var lastAnnouncedCountdownSecond: Int = -1

    fun startWorkout(workout: Workout) {
        engineScope.launch { startWorkoutInternal(workout) }
    }

    fun pause() {
        engineScope.launch { pauseInternal() }
    }

    fun resume() {
        engineScope.launch { resumeInternal() }
    }

    fun skipToNext() {
        engineScope.launch { skipToNextInternal() }
    }

    fun stop() {
        engineScope.launch { stopInternal() }
    }

    fun recordLap() {
        engineScope.launch { recordLapInternal() }
    }

    private fun startWorkoutInternal(workout: Workout) {
        tickerJob?.cancel()
        lastAnnouncedCountdownSecond = -1

        if (workout.mode == WorkoutMode.STOPWATCH) {
            stopwatchStartRealtime = SystemClock.elapsedRealtime()
            _state.value = TimerUiState(
                status = TimerStatus.RUNNING,
                workout = workout,
                elapsedMillis = 0L,
                laps = emptyList(),
                currentPhase = PhaseType.NEUTRAL,
                currentPeriodName = "Секундомер"
            )
            VoiceAnnouncer.speak("Секундомер запущен")
            runStopwatchLoop()
        } else {
            val first = workout.periods.firstOrNull() ?: return
            periodStartRealtime = SystemClock.elapsedRealtime()
            _state.value = TimerUiState(
                status = TimerStatus.RUNNING,
                workout = workout,
                currentPeriodIndex = 0,
                remainingSeconds = first.durationSeconds,
                currentPhase = first.phase,
                currentPeriodName = first.name
            )
            VoiceAnnouncer.speak("Начали. ${first.name}")
            runIntervalsLoop()
        }
    }

    private fun pauseInternal() {
        val s = _state.value
        if (s.status != TimerStatus.RUNNING) return
        // Тикер уже успел записать в state точное значение на момент отмены — просто
        // останавливаем цикл, ничего пересчитывать не нужно, поэтому фон не может "убежать" вперёд.
        tickerJob?.cancel()
        _state.value = s.copy(status = TimerStatus.PAUSED)
        VoiceAnnouncer.speak("Пауза")
    }

    private fun resumeInternal() {
        val s = _state.value
        if (s.status != TimerStatus.PAUSED) return
        val now = SystemClock.elapsedRealtime()
        // Переустанавливаем точку отсчёта так, чтобы "сейчас минус старт" сразу же снова
        // равнялось замороженному на паузе значению — без отдельного счётчика накопленной паузы.
        if (s.workout?.mode == WorkoutMode.STOPWATCH) {
            stopwatchStartRealtime = now - s.elapsedMillis
        } else {
            val period = s.currentPeriod
            val elapsedInPeriodMs = if (period != null) {
                (period.durationSeconds - s.remainingSeconds).toLong().coerceAtLeast(0) * 1000
            } else {
                0L
            }
            periodStartRealtime = now - elapsedInPeriodMs
        }
        _state.value = s.copy(status = TimerStatus.RUNNING)
        VoiceAnnouncer.speak("Продолжили")
        if (s.workout?.mode == WorkoutMode.STOPWATCH) runStopwatchLoop() else runIntervalsLoop()
    }

    private fun skipToNextInternal() {
        val s = _state.value
        if (s.workout == null || s.workout.mode != WorkoutMode.INTERVALS) return
        if (s.status != TimerStatus.RUNNING && s.status != TimerStatus.PAUSED) return
        advanceToPeriod(s.currentPeriodIndex + 1)
    }

    private fun stopInternal() {
        tickerJob?.cancel()
        VoiceAnnouncer.stopSpeaking()
        _state.value = TimerUiState()
    }

    private fun recordLapInternal() {
        val s = _state.value
        if (s.workout?.mode != WorkoutMode.STOPWATCH || s.status != TimerStatus.RUNNING) return
        _state.value = s.copy(laps = listOf(s.elapsedMillis) + s.laps)
    }

    private fun runIntervalsLoop() {
        tickerJob = engineScope.launch {
            while (isActive) {
                val s = _state.value
                if (s.status != TimerStatus.RUNNING) break
                val period = s.currentPeriod ?: break
                val elapsedMs = SystemClock.elapsedRealtime() - periodStartRealtime
                val remaining = period.durationSeconds - (elapsedMs / 1000).toInt()
                if (remaining <= 0) {
                    advanceToPeriod(s.currentPeriodIndex + 1)
                } else {
                    if (remaining != s.remainingSeconds) {
                        announceCountdownIfNeeded(remaining)
                        _state.value = s.copy(remainingSeconds = remaining)
                    }
                    delay(120)
                }
            }
        }
    }

    private fun runStopwatchLoop() {
        tickerJob = engineScope.launch {
            while (isActive) {
                val s = _state.value
                if (s.status != TimerStatus.RUNNING) break
                val elapsedMs = SystemClock.elapsedRealtime() - stopwatchStartRealtime
                val previousSeconds = (s.elapsedMillis / 1000).toInt()
                val currentSeconds = (elapsedMs / 1000).toInt()
                if (currentSeconds != previousSeconds && currentSeconds > 0 && currentSeconds % 60 == 0) {
                    VoiceAnnouncer.speak("${currentSeconds / 60} минут")
                }
                _state.value = s.copy(elapsedMillis = elapsedMs)
                delay(30)
            }
        }
    }

    private fun announceCountdownIfNeeded(remaining: Int) {
        if (remaining in 1..3 && remaining != lastAnnouncedCountdownSecond) {
            lastAnnouncedCountdownSecond = remaining
            VoiceAnnouncer.speak(remaining.toString())
        }
    }

    private fun advanceToPeriod(index: Int) {
        val s = _state.value
        val workout = s.workout ?: return
        lastAnnouncedCountdownSecond = -1
        if (index >= workout.periods.size) {
            tickerJob?.cancel()
            _state.value = s.copy(status = TimerStatus.FINISHED, remainingSeconds = 0)
            VoiceAnnouncer.speak("Тренировка завершена")
            return
        }
        val next = workout.periods[index]
        periodStartRealtime = SystemClock.elapsedRealtime()
        _state.value = s.copy(
            currentPeriodIndex = index,
            remainingSeconds = next.durationSeconds,
            currentPhase = next.phase,
            currentPeriodName = next.name
        )
        VoiceAnnouncer.speak(next.name)
    }
}
