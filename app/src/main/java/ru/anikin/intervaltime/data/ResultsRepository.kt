package ru.anikin.intervaltime.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.anikin.intervaltime.data.model.StopwatchResult

private val Context.resultsDataStore by preferencesDataStore(name = "stopwatch_results")

/** Хранилище сохранённых результатов секундомера (персистентное, DataStore). */
object ResultsRepository {

    private val RESULTS_KEY = stringPreferencesKey("results_json")
    private val json = Json { ignoreUnknownKeys = true }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _results = MutableStateFlow<List<StopwatchResult>>(emptyList())
    val results: StateFlow<List<StopwatchResult>> = _results.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        repoScope.launch {
            val prefs = context.applicationContext.resultsDataStore.data.first()
            val raw = prefs[RESULTS_KEY]
            if (!raw.isNullOrBlank()) {
                runCatching { json.decodeFromString<List<StopwatchResult>>(raw) }
                    .onSuccess { _results.value = it }
            }
        }
    }

    fun save(context: Context, result: StopwatchResult) {
        val updated = listOf(result) + _results.value
        _results.value = updated
        persist(context, updated)
    }

    fun delete(context: Context, resultId: String) {
        val updated = _results.value.filterNot { it.id == resultId }
        _results.value = updated
        persist(context, updated)
    }

    private fun persist(context: Context, results: List<StopwatchResult>) {
        repoScope.launch {
            context.applicationContext.resultsDataStore.edit { prefs ->
                prefs[RESULTS_KEY] = json.encodeToString(results)
            }
        }
    }
}
