package com.example.beamshift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.beamshift.data.AppTheme
import com.example.beamshift.data.Difficulty
import com.example.beamshift.data.GameHapticsManager
import com.example.beamshift.data.GameSoundManager
import com.example.beamshift.data.PreferencesRepository
import com.example.beamshift.data.UserPreferences
import com.example.beamshift.ui.navigation.BeamShiftNavHost
import com.example.beamshift.ui.theme.BeamShiftTheme

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: GameSoundManager
    private lateinit var hapticsManager: GameHapticsManager
    private lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesRepository = PreferencesRepository(applicationContext)
        soundManager = GameSoundManager()
        hapticsManager = GameHapticsManager(applicationContext)

        setContent {
            val preferences by preferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences(
                    theme = AppTheme.DARK,
                    soundEnabled = true,
                    hapticsEnabled = true,
                    selectedDifficulty = Difficulty.MEDIUM
                )
            )

            // Update sound and haptics managers with user preferences
            remember(preferences.soundEnabled) {
                soundManager.setSoundEnabled(preferences.soundEnabled)
            }
            remember(preferences.hapticsEnabled) {
                hapticsManager.setHapticsEnabled(preferences.hapticsEnabled)
            }

            BeamShiftTheme(appTheme = preferences.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    BeamShiftNavHost(
                        navController = navController,
                        repository = preferencesRepository,
                        soundManager = soundManager,
                        hapticsManager = hapticsManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
