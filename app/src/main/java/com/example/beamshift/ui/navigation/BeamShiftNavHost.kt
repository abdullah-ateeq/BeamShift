package com.example.beamshift.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.beamshift.data.Difficulty
import com.example.beamshift.data.GameHapticsManager
import com.example.beamshift.data.GameSoundManager
import com.example.beamshift.data.PreferencesRepository
import com.example.beamshift.game.GameViewModel
import com.example.beamshift.ui.screens.about.AboutScreen
import com.example.beamshift.ui.screens.game.GameScreen
import com.example.beamshift.ui.screens.home.HomeScreen
import com.example.beamshift.ui.screens.result.ResultScreen
import com.example.beamshift.ui.screens.settings.SettingsScreen
import com.example.beamshift.ui.screens.splash.SplashScreen

@Composable
fun BeamShiftNavHost(
    navController: NavHostController,
    repository: PreferencesRepository,
    soundManager: GameSoundManager,
    hapticsManager: GameHapticsManager
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                soundManager = soundManager,
                hapticsManager = hapticsManager,
                onPlayClick = { difficulty ->
                    navController.navigate(Screen.Game.createRoute(difficulty))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAboutClick = {
                    navController.navigate(Screen.About.route)
                }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("difficulty") {
                    type = NavType.StringType
                    defaultValue = Difficulty.MEDIUM.id
                }
            )
        ) { backStackEntry ->
            val diffId = backStackEntry.arguments?.getString("difficulty")
            val difficulty = Difficulty.fromId(diffId)
            val gameViewModel: GameViewModel = viewModel()

            GameScreen(
                difficulty = difficulty,
                viewModel = gameViewModel,
                onGameOver = { score, streak, lasersPassed, isNewBest ->
                    navController.navigate(
                        Screen.Result.createRoute(
                            difficulty = difficulty,
                            score = score,
                            streak = streak,
                            lasersPassed = lasersPassed,
                            isNewBest = isNewBest
                        )
                    ) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType; defaultValue = 0 },
                navArgument("streak") { type = NavType.IntType; defaultValue = 0 },
                navArgument("lasersPassed") { type = NavType.IntType; defaultValue = 0 },
                navArgument("isNewBest") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val diffId = backStackEntry.arguments?.getString("difficulty")
            val difficulty = Difficulty.fromId(diffId)
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val streak = backStackEntry.arguments?.getInt("streak") ?: 0
            val lasersPassed = backStackEntry.arguments?.getInt("lasersPassed") ?: 0
            val isNewBest = backStackEntry.arguments?.getBoolean("isNewBest") ?: false

            ResultScreen(
                difficulty = difficulty,
                score = score,
                highestStreak = streak,
                lasersPassed = lasersPassed,
                isNewBest = isNewBest,
                repository = repository,
                soundManager = soundManager,
                hapticsManager = hapticsManager,
                onPlayAgainClick = { diff ->
                    navController.navigate(Screen.Game.createRoute(diff)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                repository = repository,
                soundManager = soundManager,
                hapticsManager = hapticsManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                soundManager = soundManager,
                hapticsManager = hapticsManager,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
