package ru.anikin.intervaltime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.anikin.intervaltime.ui.screens.main.MainScreen
import ru.anikin.intervaltime.ui.screens.settings.SettingsScreen
import ru.anikin.intervaltime.ui.screens.stopwatch.DualStopwatchScreen
import ru.anikin.intervaltime.ui.screens.timer.TimerRunningScreen

object Routes {
    const val MAIN = "main"
    const val TIMER = "timer"
    const val STOPWATCHES = "stopwatches"
    const val SETTINGS = "settings"
}

@Composable
fun IntervalTimeNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onStarted = { navController.navigate(Routes.TIMER) },
                onOpenStopwatches = { navController.navigate(Routes.STOPWATCHES) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.TIMER) {
            TimerRunningScreen(
                onFinishedClose = { navController.popBackStack(Routes.MAIN, inclusive = false) }
            )
        }
        composable(Routes.STOPWATCHES) {
            DualStopwatchScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
