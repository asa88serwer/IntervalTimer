package ru.anikin.intervaltime.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.anikin.intervaltime.data.IntervalBuilder
import ru.anikin.intervaltime.data.ResultsRepository
import ru.anikin.intervaltime.data.RoundConfig
import ru.anikin.intervaltime.data.TemplateRepository
import ru.anikin.intervaltime.data.model.Workout
import ru.anikin.intervaltime.data.model.WorkoutMode
import ru.anikin.intervaltime.timer.TimerEngines
import ru.anikin.intervaltime.timer.TimerService
import ru.anikin.intervaltime.ui.components.PeriodRing
import ru.anikin.intervaltime.ui.theme.GradientBlack
import ru.anikin.intervaltime.ui.theme.GradientTurquoise
import ru.anikin.intervaltime.ui.theme.GradientTurquoiseBright
import ru.anikin.intervaltime.ui.theme.StartButtonColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onStarted: () -> Unit, onOpenStopwatches: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val customWorkouts by TemplateRepository.customWorkouts.collectAsState()
    val results by ResultsRepository.results.collectAsState()

    var mode by remember { mutableStateOf(WorkoutMode.INTERVALS) }
    var intervalCount by remember { mutableStateOf(5) }
    var workSeconds by remember { mutableStateOf(30) }
    var restSeconds by remember { mutableStateOf(15) }
    var restEnabled by remember { mutableStateOf(true) }
    var equalIntervals by remember { mutableStateOf(true) }
    val roundConfigs = remember { mutableStateListOf<RoundConfig>() }

    var showTemplates by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    LaunchedEffect(intervalCount) {
        while (roundConfigs.size < intervalCount) {
            roundConfigs.add(RoundConfig(workSeconds, restSeconds))
        }
        while (roundConfigs.size > intervalCount) {
            roundConfigs.removeAt(roundConfigs.size - 1)
        }
    }

    val periods = remember(mode, equalIntervals, intervalCount, workSeconds, restSeconds, restEnabled, roundConfigs.toList()) {
        when {
            mode != WorkoutMode.INTERVALS -> emptyList()
            equalIntervals -> IntervalBuilder.build(intervalCount, workSeconds, restSeconds, restEnabled)
            else -> IntervalBuilder.buildFromConfigs(roundConfigs.toList(), restEnabled)
        }
    }

    fun loadTemplate(workout: Workout) {
        mode = workout.mode
        if (workout.mode == WorkoutMode.INTERVALS) {
            val inferred = IntervalBuilder.inferIntervals(workout.periods)
            intervalCount = inferred.roundConfigs.size
            roundConfigs.clear()
            roundConfigs.addAll(inferred.roundConfigs)
            restEnabled = inferred.includeRest
            equalIntervals = inferred.isUniform
            val first = inferred.roundConfigs.first()
            workSeconds = first.workSeconds
            restSeconds = first.restSeconds
        }
        showTemplates = false
    }

    fun startCountdown() {
        val workout = Workout(name = "Обратный отсчёт", mode = WorkoutMode.INTERVALS, periods = periods)
        TimerEngines.countdown.startWorkout(workout)
        TimerService.start(context)
        onStarted()
    }

    fun onStartClick() {
        if (mode == WorkoutMode.INTERVALS) startCountdown() else onOpenStopwatches()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to GradientTurquoiseBright,
                        0.75f to GradientTurquoise,
                        1f to GradientBlack
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showTemplates = true }) {
                    Icon(Icons.Default.List, contentDescription = "Мои шаблоны", tint = Color.White)
                }
                Text("IntervalTime", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = { showResults = true }) {
                        Icon(Icons.Default.History, contentDescription = "Результаты", tint = Color.White)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Color.White)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = mode == WorkoutMode.INTERVALS,
                    onClick = { mode = WorkoutMode.INTERVALS },
                    label = { Text("Обратный отсчёт") }
                )
                FilterChip(
                    selected = mode == WorkoutMode.STOPWATCH,
                    onClick = { mode = WorkoutMode.STOPWATCH },
                    label = { Text("Секундомер") }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mode == WorkoutMode.INTERVALS) {
                    Text("Количество интервалов: $intervalCount", color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (n in 1..10) {
                            NumberButton(
                                number = n,
                                selected = n == intervalCount,
                                onClick = { intervalCount = n },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = equalIntervals,
                            onClick = { equalIntervals = true },
                            label = { Text("Одинаковые") }
                        )
                        FilterChip(
                            selected = !equalIntervals,
                            onClick = { equalIntervals = false },
                            label = { Text("Разные") }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = restEnabled,
                            onCheckedChange = { restEnabled = it },
                            colors = CheckboxDefaults.colors(uncheckedColor = Color.White)
                        )
                        Text("Отдых между раундами", color = Color.White)
                    }

                    if (equalIntervals) {
                        DurationStepper(
                            label = "Работа",
                            seconds = workSeconds,
                            onChange = { workSeconds = it.coerceIn(5, 3600) }
                        )
                        if (restEnabled) {
                            DurationStepper(
                                label = "Отдых",
                                seconds = restSeconds,
                                onChange = { restSeconds = it.coerceIn(5, 3600) }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(Modifier.width(20.dp))
                            Text(
                                "Работа",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(100.dp),
                                textAlign = TextAlign.Center
                            )
                            if (restEnabled) {
                                Text(
                                    "Отдых",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(100.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        roundConfigs.forEachIndexed { i, cfg ->
                            RoundConfigRow(
                                roundNumber = i + 1,
                                config = cfg,
                                restEnabled = restEnabled,
                                onChange = { updated -> roundConfigs[i] = updated }
                            )
                        }
                    }

                    val totalSeconds = periods.sumOf { it.durationSeconds }
                    Text(
                        "Итого: ${totalSeconds / 60} мин ${totalSeconds % 60} сек",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "Два независимых секундомера можно запускать одновременно — каждый со своей " +
                            "паузой и своими кругами. Считают время с сотыми долями секунды и работают в фоне.",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PeriodRing(periods = periods, modifier = Modifier.fillMaxSize())
                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .clickable { onStartClick() },
                        shape = CircleShape,
                        color = StartButtonColor,
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Старт",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("СТАРТ", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (mode == WorkoutMode.INTERVALS) {
                TextButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                ) {
                    Text("Сохранить как шаблон", color = Color.White)
                }
            } else {
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showTemplates) {
        ModalBottomSheet(onDismissRequest = { showTemplates = false }) {
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    Text(
                        "Мои шаблоны",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                if (customWorkouts.isEmpty()) {
                    item { Text("Пока нет сохранённых шаблонов") }
                }
                items(customWorkouts, key = { it.id }) { workout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { loadTemplate(workout) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(workout.name, style = MaterialTheme.typography.titleSmall)
                            val subtitle = if (workout.mode == WorkoutMode.STOPWATCH) {
                                "Секундомер"
                            } else {
                                "${workout.periods.size} период(ов) · ${workout.totalDurationSeconds / 60} мин"
                            }
                            Text(subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { TemplateRepository.deleteCustom(context, workout.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showResults) {
        ModalBottomSheet(onDismissRequest = { showResults = false }) {
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    Text(
                        "Результаты секундомера",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                if (results.isEmpty()) {
                    item { Text("Пока нет сохранённых результатов") }
                }
                items(results, key = { it.id }) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(formatResultTime(result.totalMillis), style = MaterialTheme.typography.titleSmall)
                            val subtitle = formatResultDate(result.dateMillis) +
                                if (result.laps.isNotEmpty()) " · ${result.laps.size} круг(ов)" else ""
                            Text(subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { ResultsRepository.delete(context, result.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить шаблон") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Название") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val workout = Workout(
                        name = templateName.ifBlank { "Без названия" },
                        mode = mode,
                        periods = periods
                    )
                    TemplateRepository.saveCustom(context, workout)
                    templateName = ""
                    showSaveDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun NumberButton(number: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) StartButtonColor else Color.White.copy(alpha = 0.1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                number.toString(),
                color = if (selected) Color.Black else Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DurationStepper(label: String, seconds: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Color.White, modifier = Modifier.width(64.dp))
        IconButton(onClick = { onChange(seconds - 5) }) {
            Icon(Icons.Default.Remove, contentDescription = "Уменьшить", tint = Color.White)
        }
        Text(
            formatMmSs(seconds),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onChange(seconds + 5) }) {
            Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = Color.White)
        }
    }
}

@Composable
private fun RoundConfigRow(
    roundNumber: Int,
    config: RoundConfig,
    restEnabled: Boolean,
    onChange: (RoundConfig) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$roundNumber", color = Color.White, modifier = Modifier.width(20.dp))
        CompactStepper(
            seconds = config.workSeconds,
            onChange = { onChange(config.copy(workSeconds = it.coerceIn(5, 3600))) }
        )
        if (restEnabled) {
            CompactStepper(
                seconds = config.restSeconds,
                onChange = { onChange(config.copy(restSeconds = it.coerceIn(5, 3600))) }
            )
        }
    }
}

@Composable
private fun CompactStepper(seconds: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(100.dp)) {
        IconButton(onClick = { onChange(seconds - 5) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Уменьшить", tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Text(
            formatMmSs(seconds),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onChange(seconds + 5) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

private fun formatResultTime(totalMillis: Long): String {
    val m = totalMillis / 60000
    val s = (totalMillis / 1000) % 60
    val centis = (totalMillis % 1000) / 10
    return "%d:%02d.%02d".format(m, s, centis)
}

private fun formatResultDate(dateMillis: Long): String {
    val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}
