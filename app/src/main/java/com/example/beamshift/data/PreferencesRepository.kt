package com.example.beamshift.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beamshift_prefs")

enum class AppTheme(val title: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class UserPreferences(
    val theme: AppTheme,
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val selectedDifficulty: Difficulty
)

data class DifficultyStats(
    val bestScore: Int,
    val bestStreak: Int,
    val totalPassed: Int
)

data class GameStats(
    val easy: DifficultyStats,
    val medium: DifficultyStats,
    val hard: DifficultyStats
) {
    fun forDifficulty(difficulty: Difficulty): DifficultyStats {
        return when (difficulty) {
            Difficulty.EASY -> easy
            Difficulty.MEDIUM -> medium
            Difficulty.HARD -> hard
        }
    }
}

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SELECTED_DIFFICULTY = stringPreferencesKey("selected_difficulty")

        fun bestScoreKey(diff: Difficulty) = intPreferencesKey("best_score_${diff.id}")
        fun bestStreakKey(diff: Difficulty) = intPreferencesKey("best_streak_${diff.id}")
        fun totalPassedKey(diff: Difficulty) = intPreferencesKey("total_passed_${diff.id}")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeString = prefs[PreferencesKeys.APP_THEME] ?: AppTheme.SYSTEM.name
        val theme = runCatching { AppTheme.valueOf(themeString) }.getOrDefault(AppTheme.SYSTEM)
        val sound = prefs[PreferencesKeys.SOUND_ENABLED] ?: true
        val haptics = prefs[PreferencesKeys.HAPTICS_ENABLED] ?: true
        val diffId = prefs[PreferencesKeys.SELECTED_DIFFICULTY] ?: Difficulty.MEDIUM.id
        val diff = Difficulty.fromId(diffId)

        UserPreferences(
            theme = theme,
            soundEnabled = sound,
            hapticsEnabled = haptics,
            selectedDifficulty = diff
        )
    }

    val gameStatsFlow: Flow<GameStats> = context.dataStore.data.map { prefs ->
        GameStats(
            easy = DifficultyStats(
                bestScore = prefs[PreferencesKeys.bestScoreKey(Difficulty.EASY)] ?: 0,
                bestStreak = prefs[PreferencesKeys.bestStreakKey(Difficulty.EASY)] ?: 0,
                totalPassed = prefs[PreferencesKeys.totalPassedKey(Difficulty.EASY)] ?: 0
            ),
            medium = DifficultyStats(
                bestScore = prefs[PreferencesKeys.bestScoreKey(Difficulty.MEDIUM)] ?: 0,
                bestStreak = prefs[PreferencesKeys.bestStreakKey(Difficulty.MEDIUM)] ?: 0,
                totalPassed = prefs[PreferencesKeys.totalPassedKey(Difficulty.MEDIUM)] ?: 0
            ),
            hard = DifficultyStats(
                bestScore = prefs[PreferencesKeys.bestScoreKey(Difficulty.HARD)] ?: 0,
                bestStreak = prefs[PreferencesKeys.bestStreakKey(Difficulty.HARD)] ?: 0,
                totalPassed = prefs[PreferencesKeys.totalPassedKey(Difficulty.HARD)] ?: 0
            )
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_THEME] = theme.name
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setSelectedDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_DIFFICULTY] = difficulty.id
        }
    }

    suspend fun recordGameResult(
        difficulty: Difficulty,
        score: Int,
        highestStreak: Int,
        lasersPassed: Int
    ): Boolean {
        var isNewBest = false
        context.dataStore.edit { prefs ->
            val scoreKey = PreferencesKeys.bestScoreKey(difficulty)
            val streakKey = PreferencesKeys.bestStreakKey(difficulty)
            val passedKey = PreferencesKeys.totalPassedKey(difficulty)

            val currentBest = prefs[scoreKey] ?: 0
            val currentBestStreak = prefs[streakKey] ?: 0
            val currentTotalPassed = prefs[passedKey] ?: 0

            if (score > currentBest) {
                prefs[scoreKey] = score
                isNewBest = true
            }
            if (highestStreak > currentBestStreak) {
                prefs[streakKey] = highestStreak
            }
            prefs[passedKey] = currentTotalPassed + lasersPassed
        }
        return isNewBest
    }

    suspend fun resetStatistics() {
        context.dataStore.edit { prefs ->
            Difficulty.entries.forEach { diff ->
                prefs[PreferencesKeys.bestScoreKey(diff)] = 0
                prefs[PreferencesKeys.bestStreakKey(diff)] = 0
                prefs[PreferencesKeys.totalPassedKey(diff)] = 0
            }
        }
    }

    suspend fun getStatsSnapshot(): GameStats {
        return gameStatsFlow.first()
    }
}
