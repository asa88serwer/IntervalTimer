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
import ru.anikin.intervaltime.data.model.Workout

private val Context.templatesDataStore by preferencesDataStore(name = "custom_templates")

/** Хранилище пользовательских шаблонов (персистентное, DataStore). */
object TemplateRepository {

    private val CUSTOM_KEY = stringPreferencesKey("custom_workouts_json")
    private val json = Json { ignoreUnknownKeys = true }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _customWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val customWorkouts: StateFlow<List<Workout>> = _customWorkouts.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        repoScope.launch {
            val prefs = context.applicationContext.templatesDataStore.data.first()
            val raw = prefs[CUSTOM_KEY]
            if (!raw.isNullOrBlank()) {
                runCatching { json.decodeFromString<List<Workout>>(raw) }
                    .onSuccess { _customWorkouts.value = it }
            }
        }
    }

    fun saveCustom(context: Context, workout: Workout) {
        val updated = _customWorkouts.value.filterNot { it.id == workout.id } + workout
        _customWorkouts.value = updated
        persist(context, updated)
    }

    fun deleteCustom(context: Context, workoutId: String) {
        val updated = _customWorkouts.value.filterNot { it.id == workoutId }
        _customWorkouts.value = updated
        persist(context, updated)
    }

    private fun persist(context: Context, workouts: List<Workout>) {
        repoScope.launch {
            context.applicationContext.templatesDataStore.edit { prefs ->
                prefs[CUSTOM_KEY] = json.encodeToString(workouts)
            }
        }
    }
}
