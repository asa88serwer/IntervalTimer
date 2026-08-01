package ru.anikin.intervaltime.timer

/**
 * Именованные экземпляры движка: один — для обратного отсчёта, два независимых — для
 * секундомеров, которые благодаря этому можно запускать одновременно и управлять ими
 * по отдельности (свои паузы, свои круги), не затрагивая друг друга.
 */
object TimerEngines {
    val countdown = TimerEngine()
    val stopwatchA = TimerEngine()
    val stopwatchB = TimerEngine()
}
