package com.example.beamshift.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.beamshift.data.Difficulty
import com.example.beamshift.data.GameHapticsManager
import com.example.beamshift.data.GameSoundManager
import com.example.beamshift.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.max

enum class GameState {
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

data class SparkParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val alpha: Float,
    val size: Float,
    val colorIndex: Int
)

data class GameUiState(
    val gameState: GameState = GameState.COUNTDOWN,
    val countdownSeconds: Int = 3,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val player: PlayerState = PlayerState(
        currentLane = 2,
        targetLane = 2,
        x = 0.5f,
        width = 0.12f
    ),
    val currentBarrier: LaserBarrier? = null,
    val score: Int = 0,
    val streak: Int = 0,
    val highestStreak: Int = 0,
    val lasersPassed: Int = 0,
    val currentSpeed: Float = 0.46f,
    val passPulseAlpha: Float = 0f,
    val screenShakeMagnitude: Float = 0f,
    val lastScoreGain: Int? = null,
    val lastScoreGainAlpha: Float = 0f,
    val particles: List<SparkParticle> = emptyList(),
    val isNewBest: Boolean = false,
    val bestScore: Int = 0
)

sealed interface GameEvent {
    data class GameOver(
        val finalScore: Int,
        val highestStreak: Int,
        val lasersPassed: Int,
        val isNewBest: Boolean
    ) : GameEvent
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PreferencesRepository(application)
    private val soundManager = GameSoundManager()
    private val hapticsManager = GameHapticsManager(application)
    private val random = Random()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<GameEvent>()
    val eventFlow: SharedFlow<GameEvent> = _eventFlow.asSharedFlow()

    private var laserGenerator = LaserGenerator(Difficulty.MEDIUM)
    private var shiftAnimator = ShiftAnimator(Difficulty.MEDIUM.lanesCount)
    private var initialLane = 2

    // Barrier spawn delay after passing
    private var spawnDelayTimer = 0f
    private var gameOverTransitionTimer = 0f

    init {
        // Observe preferences for sound & haptics
        viewModelScope.launch {
            repository.userPreferencesFlow.collect { prefs ->
                soundManager.setSoundEnabled(prefs.soundEnabled)
                hapticsManager.setHapticsEnabled(prefs.hapticsEnabled)
            }
        }
    }

    fun initGame(difficulty: Difficulty) {
        val lanes = difficulty.lanesCount
        initialLane = lanes / 2
        val initialX = (initialLane + 0.5f) / lanes
        val playerWidth = (0.55f / lanes).coerceIn(0.06f, 0.18f)

        laserGenerator = LaserGenerator(difficulty)
        shiftAnimator = ShiftAnimator(lanes)

        viewModelScope.launch {
            val stats = repository.getStatsSnapshot()
            val best = stats.forDifficulty(difficulty).bestScore

            _uiState.update {
                GameUiState(
                    gameState = GameState.COUNTDOWN,
                    countdownSeconds = 3,
                    difficulty = difficulty,
                    player = PlayerState(
                        currentLane = initialLane,
                        targetLane = initialLane,
                        x = initialX,
                        width = playerWidth
                    ),
                    currentBarrier = null,
                    score = 0,
                    streak = 0,
                    highestStreak = 0,
                    lasersPassed = 0,
                    currentSpeed = difficulty.baseSpeed,
                    bestScore = best
                )
            }
            startCountdown()
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(countdownSeconds = i) }
                soundManager.playClick()
                kotlinx.coroutines.delay(650)
            }
            // Spawn initial barrier
            val barrier = laserGenerator.generateNextBarrier(initialLane, initialY = -0.1f)
            _uiState.update {
                it.copy(
                    gameState = GameState.PLAYING,
                    countdownSeconds = 0,
                    currentBarrier = barrier
                )
            }
        }
    }

    fun shiftLeft() {
        if (_uiState.value.gameState != GameState.PLAYING) return
        val currentTarget = _uiState.value.player.targetLane
        if (currentTarget > 0) {
            val newTarget = currentTarget - 1
            soundManager.playShift()
            hapticsManager.vibrateShift()
            _uiState.update {
                it.copy(
                    player = it.player.copy(
                        targetLane = newTarget,
                        isShifting = true
                    )
                )
            }
        }
    }

    fun shiftRight() {
        if (_uiState.value.gameState != GameState.PLAYING) return
        val lanes = _uiState.value.difficulty.lanesCount
        val currentTarget = _uiState.value.player.targetLane
        if (currentTarget < lanes - 1) {
            val newTarget = currentTarget + 1
            soundManager.playShift()
            hapticsManager.vibrateShift()
            _uiState.update {
                it.copy(
                    player = it.player.copy(
                        targetLane = newTarget,
                        isShifting = true
                    )
                )
            }
        }
    }

    fun pauseGame() {
        if (_uiState.value.gameState == GameState.PLAYING) {
            _uiState.update { it.copy(gameState = GameState.PAUSED) }
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GameState.PAUSED) {
            _uiState.update { it.copy(gameState = GameState.PLAYING) }
        }
    }

    fun onFrame(deltaSeconds: Float) {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) {
            // Decay effects even when game over
            if (state.gameState == GameState.GAME_OVER) {
                updateEffectsOnly(deltaSeconds)
                handleGameOverDelay(deltaSeconds)
            }
            return
        }

        // Clamp delta to prevent huge jumps on lag spikes
        val dt = deltaSeconds.coerceIn(0.001f, 0.05f)

        // 1. Update Player Shift Animation
        val (newX, isStillMoving) = shiftAnimator.step(
            currentX = state.player.x,
            targetLane = state.player.targetLane,
            deltaSeconds = dt
        )
        val updatedPlayer = state.player.copy(
            x = newX,
            currentLane = if (!isStillMoving) state.player.targetLane else state.player.currentLane,
            isShifting = isStillMoving
        )

        // 2. Update Laser Movement
        var currentBarrier = state.currentBarrier
        var score = state.score
        var streak = state.streak
        var highestStreak = state.highestStreak
        var lasersPassed = state.lasersPassed
        var currentSpeed = state.currentSpeed
        var passPulse = (state.passPulseAlpha - dt * 2.5f).coerceAtLeast(0f)
        var scoreGainAlpha = (state.lastScoreGainAlpha - dt * 1.8f).coerceAtLeast(0f)
        var lastScoreGain = state.lastScoreGain
        val particles = updateParticles(state.particles, dt).toMutableList()

        if (currentBarrier != null) {
            val nextY = currentBarrier.y + currentSpeed * dt
            val movedBarrier = currentBarrier.copy(y = nextY)

            // Check collision with player
            val collision = CollisionDetector.check(updatedPlayer, movedBarrier)

            when (collision) {
                CollisionResult.COLLIDED -> {
                    triggerGameOver(updatedPlayer, movedBarrier)
                    return
                }

                CollisionResult.PASSED_SAFELY -> {
                    if (!movedBarrier.passed && movedBarrier.y >= updatedPlayer.y) {
                        // Successfully passed through the safe gap
                        val pointsAwarded = ScoreCalculator.calculatePointsForPass(streak)
                        score += pointsAwarded
                        streak += 1
                        highestStreak = max(highestStreak, streak)
                        lasersPassed += 1
                        currentSpeed = ScoreCalculator.calculateCurrentSpeed(state.difficulty, lasersPassed)

                        passPulse = 1.0f
                        lastScoreGain = pointsAwarded
                        scoreGainAlpha = 1.0f

                        soundManager.playPass()
                        hapticsManager.vibratePass()

                        // Add reward particles at gap
                        val gapLeft = movedBarrier.gapLeft
                        val gapRight = movedBarrier.gapRight
                        for (i in 0 until 10) {
                            particles.add(
                                SparkParticle(
                                    x = if (i % 2 == 0) gapLeft else gapRight,
                                    y = updatedPlayer.y,
                                    vx = (random.nextFloat() - 0.5f) * 0.4f,
                                    vy = -random.nextFloat() * 0.35f - 0.1f,
                                    alpha = 1.0f,
                                    size = random.nextFloat() * 8f + 4f,
                                    colorIndex = 1 // Electric cyan/amber
                                )
                            )
                        }

                        currentBarrier = movedBarrier.copy(passed = true)
                    } else {
                        currentBarrier = movedBarrier
                    }
                }

                CollisionResult.NONE -> {
                    // Check if passed barrier left bottom of arena
                    if (movedBarrier.y > 1.15f) {
                        currentBarrier = null
                        spawnDelayTimer = 0.12f // brief breather before next barrier
                    } else {
                        currentBarrier = movedBarrier
                    }
                }
            }
        } else {
            // Spawn next barrier after short delay
            spawnDelayTimer -= dt
            if (spawnDelayTimer <= 0f) {
                val nextBarrier = laserGenerator.generateNextBarrier(
                    lastSafeLane = updatedPlayer.targetLane,
                    initialY = -0.06f
                )
                currentBarrier = nextBarrier
            }
        }

        // Screen shake decay
        val screenShake = (state.screenShakeMagnitude - dt * 4.0f).coerceAtLeast(0f)

        _uiState.update {
            it.copy(
                player = updatedPlayer,
                currentBarrier = currentBarrier,
                score = score,
                streak = streak,
                highestStreak = highestStreak,
                lasersPassed = lasersPassed,
                currentSpeed = currentSpeed,
                passPulseAlpha = passPulse,
                lastScoreGain = lastScoreGain,
                lastScoreGainAlpha = scoreGainAlpha,
                screenShakeMagnitude = screenShake,
                particles = particles
            )
        }
    }

    private fun triggerGameOver(player: PlayerState, barrier: LaserBarrier) {
        soundManager.playGameOver()
        hapticsManager.vibrateCollision()

        val state = _uiState.value
        val shockParticles = mutableListOf<SparkParticle>()
        for (i in 0 until 22) {
            shockParticles.add(
                SparkParticle(
                    x = player.x,
                    y = player.y,
                    vx = (random.nextFloat() - 0.5f) * 0.8f,
                    vy = (random.nextFloat() - 0.5f) * 0.8f,
                    alpha = 1.0f,
                    size = random.nextFloat() * 10f + 5f,
                    colorIndex = 0 // Hazard red
                )
            )
        }

        _uiState.update {
            it.copy(
                gameState = GameState.GAME_OVER,
                screenShakeMagnitude = 1.0f,
                particles = it.particles + shockParticles
            )
        }

        gameOverTransitionTimer = 0.65f // short screen feedback before result navigation

        viewModelScope.launch {
            val isNewBest = repository.recordGameResult(
                difficulty = state.difficulty,
                score = state.score,
                highestStreak = state.highestStreak,
                lasersPassed = state.lasersPassed
            )
            _uiState.update { it.copy(isNewBest = isNewBest) }
        }
    }

    private fun handleGameOverDelay(deltaSeconds: Float) {
        if (gameOverTransitionTimer > 0f) {
            gameOverTransitionTimer -= deltaSeconds
            if (gameOverTransitionTimer <= 0f) {
                val state = _uiState.value
                viewModelScope.launch {
                    _eventFlow.emit(
                        GameEvent.GameOver(
                            finalScore = state.score,
                            highestStreak = state.highestStreak,
                            lasersPassed = state.lasersPassed,
                            isNewBest = state.isNewBest
                        )
                    )
                }
            }
        }
    }

    private fun updateEffectsOnly(dt: Float) {
        _uiState.update { state ->
            val screenShake = (state.screenShakeMagnitude - dt * 3.5f).coerceAtLeast(0f)
            val passPulse = (state.passPulseAlpha - dt * 2.0f).coerceAtLeast(0f)
            val updatedParticles = updateParticles(state.particles, dt)
            state.copy(
                screenShakeMagnitude = screenShake,
                passPulseAlpha = passPulse,
                particles = updatedParticles
            )
        }
    }

    private fun updateParticles(particles: List<SparkParticle>, dt: Float): List<SparkParticle> {
        if (particles.isEmpty()) return emptyList()
        val result = mutableListOf<SparkParticle>()
        for (p in particles) {
            val newAlpha = p.alpha - dt * 1.8f
            if (newAlpha > 0.05f) {
                result.add(
                    p.copy(
                        x = p.x + p.vx * dt,
                        y = p.y + p.vy * dt,
                        alpha = newAlpha
                    )
                )
            }
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
