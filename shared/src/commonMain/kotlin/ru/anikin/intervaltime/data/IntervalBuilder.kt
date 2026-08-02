package ru.anikin.intervaltime.data

import ru.anikin.intervaltime.data.model.PhaseType
import ru.anikin.intervaltime.data.model.Period

/** Настройки одного раунда — работа и (опционально) отдых после неё. */
data class RoundConfig(val workSeconds: Int, val restSeconds: Int)

data class InferredIntervals(
    val roundConfigs: List<RoundConfig>,
    val includeRest: Boolean,
    val isUniform: Boolean
)

/** Строит плоский список периодов из схемы "N раундов работы (+ отдых)" — раунды могут отличаться по длительности. */
object IntervalBuilder {

    fun build(count: Int, workSeconds: Int, restSeconds: Int, includeRest: Boolean): List<Period> {
        val safeCount = count.coerceIn(1, 10)
        val configs = List(safeCount) { RoundConfig(workSeconds, restSeconds) }
        return buildFromConfigs(configs, includeRest)
    }

    fun buildFromConfigs(configs: List<RoundConfig>, includeRest: Boolean): List<Period> {
        val periods = mutableListOf<Period>()
        configs.forEachIndexed { i, cfg ->
            val round = i + 1
            periods += Period(
                name = "Раунд $round",
                phase = PhaseType.WORK,
                durationSeconds = cfg.workSeconds.coerceAtLeast(1)
            )
            if (includeRest) {
                periods += Period(
                    name = "Отдых $round",
                    phase = PhaseType.REST,
                    durationSeconds = cfg.restSeconds.coerceAtLeast(1)
                )
            }
        }
        return periods
    }

    /** Восстанавливает раунды из плоского списка периодов (для загрузки сохранённого шаблона). */
    fun inferIntervals(periods: List<Period>): InferredIntervals {
        val workPeriods = periods.filter { it.phase == PhaseType.WORK }
        val restPeriods = periods.filter { it.phase == PhaseType.REST }
        val includeRest = restPeriods.isNotEmpty()
        val defaultRest = restPeriods.firstOrNull()?.durationSeconds ?: 15
        val configs = workPeriods.mapIndexed { i, work ->
            RoundConfig(work.durationSeconds, restPeriods.getOrNull(i)?.durationSeconds ?: defaultRest)
        }.ifEmpty { listOf(RoundConfig(30, 15)) }.take(10)
        val isUniform = configs.all {
            it.workSeconds == configs.first().workSeconds && it.restSeconds == configs.first().restSeconds
        }
        return InferredIntervals(roundConfigs = configs, includeRest = includeRest, isUniform = isUniform)
    }
}
