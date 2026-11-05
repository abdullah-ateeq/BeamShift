package com.example.beamshift.ui.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.beamshift.data.Difficulty
import com.example.beamshift.game.GameEvent
import com.example.beamshift.game.GameState
import com.example.beamshift.game.GameUiState
import com.example.beamshift.game.GameViewModel
import com.example.beamshift.game.LaserBarrier
import com.example.beamshift.game.PlayerState
import com.example.beamshift.game.ScoreCalculator
import com.example.beamshift.ui.theme.BackgroundDark
import com.example.beamshift.ui.theme.LaserAmber
import com.example.beamshift.ui.theme.LaserCyan
import com.example.beamshift.ui.theme.LaserCyanCore
import com.example.beamshift.ui.theme.LaserCyanGlow
import com.example.beamshift.ui.theme.LaserHazardCore
import com.example.beamshift.ui.theme.LaserHazardGlow
import com.example.beamshift.ui.theme.LaserHazardRed
import com.example.beamshift.ui.theme.SafeGreen
import com.example.beamshift.ui.theme.SafeGreenGlow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameScreen(
    difficulty: Difficulty,
    viewModel: GameViewModel,
    onGameOver: (score: Int, streak: Int, lasersPassed: Int, isNewBest: Boolean) -> Unit,
    onNavigateHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize game on entry
    LaunchedEffect(difficulty) {
        viewModel.initGame(difficulty)
    }

    // Handle game over event
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is GameEvent.GameOver -> {
                    onGameOver(
                        event.finalScore,
                        event.highestStreak,
                        event.lasersPassed,
                        event.isNewBest
                    )
                }
            }
        }
    }

    // High performance 60/120Hz VSYNC frame loop
    LaunchedEffect(uiState.gameState) {
        var lastFrameTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val deltaSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                    viewModel.onFrame(deltaSeconds)
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    // Back button pauses game instead of quitting immediately
    BackHandler {
        if (uiState.gameState == GameState.PLAYING) {
            viewModel.pauseGame()
        } else {
            onNavigateHome()
        }
    }

    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 36.dp.toPx() }

    // Screen shake offset calculation
    val shakeOffset = remember(uiState.screenShakeMagnitude) {
        if (uiState.screenShakeMagnitude > 0f) {
            val offsetVal = sin(System.currentTimeMillis() * 0.08f) * uiState.screenShakeMagnitude * 16f
            IntOffset(offsetVal.roundToInt(), (offsetVal * 0.5f).roundToInt())
        } else {
            IntOffset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .offset { shakeOffset }
    ) {
        // Main Gameplay Arena Canvas with unified Tap / Drag gestures
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dragThresholdPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        var isDrag = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                // Pointer up without drag -> Tap action
                                if (!isDrag) {
                                    if (startX < size.width / 2f) {
                                        viewModel.shiftLeft()
                                    } else {
                                        viewModel.shiftRight()
                                    }
                                }
                                break
                            }

                            val dx = change.position.x - startX
                            if (kotlin.math.abs(dx) > dragThresholdPx) {
                                isDrag = true
                                if (dx > 0) {
                                    viewModel.shiftRight()
                                } else {
                                    viewModel.shiftLeft()
                                }
                                change.consume()

                                // Consume remaining move events until release
                                while (true) {
                                    val nextEvent = awaitPointerEvent()
                                    val nextChange = nextEvent.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!nextChange.pressed) break
                                }
                                break
                            }
                        }
                    }
                }
        ) {
            val arenaWidth = size.width
            val arenaHeight = size.height

            // 1. Draw subtle lane guide lines
            drawLanes(
                lanesCount = uiState.difficulty.lanesCount,
                width = arenaWidth,
                height = arenaHeight
            )

            // 2. Draw active laser barrier
            uiState.currentBarrier?.let { barrier ->
                drawLaserBarrier(
                    barrier = barrier,
                    width = arenaWidth,
                    height = arenaHeight
                )
            }

            // 3. Draw Player Marker
            drawPlayer(
                player = uiState.player,
                width = arenaWidth,
                height = arenaHeight,
                passPulse = uiState.passPulseAlpha
            )

            // 4. Draw Particles
            for (p in uiState.particles) {
                val particleColor = if (p.colorIndex == 0) {
                    LaserHazardRed.copy(alpha = p.alpha)
                } else {
                    LaserCyan.copy(alpha = p.alpha)
                }
                drawCircle(
                    color = particleColor,
                    radius = p.size,
                    center = Offset(p.x * arenaWidth, p.y * arenaHeight)
                )
            }

            // 5. Success wave flash on pass
            if (uiState.passPulseAlpha > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            LaserCyanGlow.copy(alpha = uiState.passPulseAlpha * 0.25f),
                            Color.Transparent
                        ),
                        startY = (uiState.player.y - 0.12f) * arenaHeight,
                        endY = (uiState.player.y + 0.12f) * arenaHeight
                    )
                )
            }
        }

        // Minimal Futuristic HUD
        GameHud(
            state = uiState,
            onPauseClick = { viewModel.pauseGame() }
        )

        // Countdown Overlay
        if (uiState.gameState == GameState.COUNTDOWN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99090B10)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.countdownSeconds}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 84.sp,
                            fontWeight = FontWeight.Black,
                            color = LaserCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "GET READY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 4.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Pause Dialog
        if (uiState.gameState == GameState.PAUSED) {
            PauseMenuDialog(
                score = uiState.score,
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.initGame(uiState.difficulty) },
                onQuit = onNavigateHome
            )
        }
    }
}

@Composable
private fun GameHud(
    state: GameUiState,
    onPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Score & Multiplier
        Column {
            Text(
                text = "${state.score}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STREAK ${state.streak}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (state.streak >= 5) LaserAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                if (state.streak >= 5) {
                    val multiplier = ScoreCalculator.getStreakMultiplier(state.streak)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LaserAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${multiplier}X",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = LaserAmber,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }
            }

            // Score gain floating toast
            AnimatedVisibility(
                visible = state.lastScoreGainAlpha > 0.05f,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Text(
                    text = "+${state.lastScoreGain ?: 100}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = LaserCyan.copy(alpha = state.lastScoreGainAlpha),
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Pause Button
        IconButton(
            onClick = onPauseClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x331E293B))
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PauseMenuDialog(
    score: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2A3650), RoundedCornerShape(20.dp)),
            color = Color(0xFF101524)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAUSED",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CURRENT SCORE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                )

                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = LaserCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LaserCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color(0xFF090B10)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF090B10),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESTART",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onQuit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QUIT TO MENU",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// Custom Drawing Helpers
private fun DrawScope.drawLanes(
    lanesCount: Int,
    width: Float,
    height: Float
) {
    val laneWidth = width / lanesCount
    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f), 0f)

    for (i in 1 until lanesCount) {
        val x = i * laneWidth
        drawLine(
            color = Color(0x16FFFFFF),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1.5f,
            pathEffect = dashPathEffect
        )
    }
}

private fun DrawScope.drawLaserBarrier(
    barrier: LaserBarrier,
    width: Float,
    height: Float
) {
    val barrierY = barrier.y * height
    val beamThickness = barrier.height * height
    val gapLeft = barrier.gapLeft * width
    val gapRight = barrier.gapRight * width

    // Left hazardous beam segment
    if (gapLeft > 0f) {
        // Outer glow
        drawRect(
            color = LaserHazardGlow,
            topLeft = Offset(0f, barrierY - beamThickness),
            size = Size(gapLeft, beamThickness * 2f)
        )
        // Core laser beam
        drawRect(
            color = LaserHazardRed,
            topLeft = Offset(0f, barrierY - beamThickness / 2f),
            size = Size(gapLeft, beamThickness)
        )
        // Hot laser filament
        drawRect(
            color = LaserHazardCore,
            topLeft = Offset(0f, barrierY - beamThickness * 0.18f),
            size = Size(gapLeft, beamThickness * 0.36f)
        )
    }

    // Right hazardous beam segment
    if (gapRight < width) {
        val rightWidth = width - gapRight
        // Outer glow
        drawRect(
            color = LaserHazardGlow,
            topLeft = Offset(gapRight, barrierY - beamThickness),
            size = Size(rightWidth, beamThickness * 2f)
        )
        // Core laser beam
        drawRect(
            color = LaserHazardRed,
            topLeft = Offset(gapRight, barrierY - beamThickness / 2f),
            size = Size(rightWidth, beamThickness)
        )
        // Hot laser filament
        drawRect(
            color = LaserHazardCore,
            topLeft = Offset(gapRight, barrierY - beamThickness * 0.18f),
            size = Size(rightWidth, beamThickness * 0.36f)
        )
    }

    // Safe Gap Indicator: Glowing cyan guide posts & soft corridor
    val gapWidth = gapRight - gapLeft

    // Soft safe corridor pass tint
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                SafeGreenGlow.copy(alpha = 0.25f),
                Color.Transparent
            ),
            startY = barrierY - beamThickness * 2f,
            endY = barrierY + beamThickness * 2f
        ),
        topLeft = Offset(gapLeft, barrierY - beamThickness * 2f),
        size = Size(gapWidth, beamThickness * 4f)
    )

    // Gap edge guide brackets
    val bracketLength = 16f
    // Left bracket
    drawLine(
        color = LaserCyan,
        start = Offset(gapLeft, barrierY - bracketLength),
        end = Offset(gapLeft, barrierY + bracketLength),
        strokeWidth = 3.5f
    )
    // Right bracket
    drawLine(
        color = LaserCyan,
        start = Offset(gapRight, barrierY - bracketLength),
        end = Offset(gapRight, barrierY + bracketLength),
        strokeWidth = 3.5f
    )
}

private fun DrawScope.drawPlayer(
    player: PlayerState,
    width: Float,
    height: Float,
    passPulse: Float
) {
    val px = player.x * width
    val py = player.y * height
    val pw = player.width * width
    val ph = player.height * height

    // Glowing halo around player marker
    val haloRadius = pw * (0.85f + passPulse * 0.5f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                LaserCyanGlow.copy(alpha = 0.85f),
                LaserCyanGlow.copy(alpha = 0.35f),
                Color.Transparent
            ),
            center = Offset(px, py),
            radius = haloRadius
        ),
        radius = haloRadius,
        center = Offset(px, py)
    )

    // Player marker body: Sleek futuristic chevron capsule
    val markerCorner = CornerRadius(ph / 2f, ph / 2f)

    // Outer glow capsule
    drawRoundRect(
        color = LaserCyan,
        topLeft = Offset(px - pw / 2f, py - ph / 2f),
        size = Size(pw, ph),
        cornerRadius = markerCorner,
        style = Stroke(width = 3.5f)
    )

    // Filled core
    drawRoundRect(
        color = LaserCyanCore,
        topLeft = Offset(px - pw * 0.38f, py - ph * 0.28f),
        size = Size(pw * 0.76f, ph * 0.56f),
        cornerRadius = CornerRadius(ph * 0.28f, ph * 0.28f),
        style = Fill
    )

    // Center focal beacon
    drawCircle(
        color = Color.White,
        radius = ph * 0.32f,
        center = Offset(px, py)
    )
}
