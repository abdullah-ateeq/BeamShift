package com.example.beamshift.ui.navigation

import com.example.beamshift.data.Difficulty

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Game : Screen("game/{difficulty}") {
        fun createRoute(difficulty: Difficulty): String = "game/${difficulty.id}"
    }
    data object Result : Screen("result/{difficulty}/{score}/{streak}/{lasersPassed}/{isNewBest}") {
        fun createRoute(
            difficulty: Difficulty,
            score: Int,
            streak: Int,
            lasersPassed: Int,
            isNewBest: Boolean
        ): String = "result/${difficulty.id}/$score/$streak/$lasersPassed/$isNewBest"
    }
    data object Settings : Screen("settings")
    data object About : Screen("about")
}
