package com.example.beamshift.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beamshift.ui.theme.LaserCyan
import com.example.beamshift.ui.theme.LaserCyanGlow
import com.example.beamshift.ui.theme.LaserHazardRed
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val laserScanAnim = remember { Animatable(0f) }
    val glowPulseAnim = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        glowPulseAnim.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        laserScanAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
        delay(350)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNavigateToHome() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stylized Futuristic Geometric Laser Icon
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // Outer Hex/Diamond ring
                    drawCircle(
                        color = LaserCyanGlow.copy(alpha = 0.25f * glowPulseAnim.value),
                        radius = w * 0.46f,
                        style = Stroke(width = 6f)
                    )

                    // Passing laser guide marks
                    drawLine(
                        color = LaserHazardRed.copy(alpha = 0.8f),
                        start = Offset(w * 0.1f, h * 0.42f),
                        end = Offset(w * 0.38f, h * 0.42f),
                        strokeWidth = 5f
                    )
                    drawLine(
                        color = LaserHazardRed.copy(alpha = 0.8f),
                        start = Offset(w * 0.62f, h * 0.42f),
                        end = Offset(w * 0.9f, h * 0.42f),
                        strokeWidth = 5f
                    )

                    // Safe Gap in middle telegraphed in glowing cyan
                    drawLine(
                        color = LaserCyan,
                        start = Offset(w * 0.38f, h * 0.42f),
                        end = Offset(w * 0.62f, h * 0.42f),
                        strokeWidth = 2f
                    )

                    // Player beacon shifted into the gap
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, LaserCyan, Color.Transparent),
                            center = center,
                            radius = 28f * glowPulseAnim.value
                        ),
                        radius = 18f,
                        center = center
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Game Title
            Text(
                text = "BEAMSHIFT",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "FIND THE OPENING • SHIFT IN TIME",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.5.sp,
                    color = LaserCyan.copy(alpha = 0.85f)
                )
            )
        }
    }
}
