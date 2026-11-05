package com.example.beamshift.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beamshift.data.Difficulty
import com.example.beamshift.data.GameHapticsManager
import com.example.beamshift.data.GameSoundManager
import com.example.beamshift.data.PreferencesRepository
import com.example.beamshift.ui.theme.LaserAmber
import com.example.beamshift.ui.theme.LaserCyan
import com.example.beamshift.ui.theme.LaserCyanGlow
import com.example.beamshift.ui.theme.LaserHazardRed
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: PreferencesRepository,
    soundManager: GameSoundManager,
    hapticsManager: GameHapticsManager,
    onPlayClick: (Difficulty) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val preferences by repository.userPreferencesFlow.collectAsState(
        initial = com.example.beamshift.data.UserPreferences(
            theme = com.example.beamshift.data.AppTheme.DARK,
            soundEnabled = true,
            hapticsEnabled = true,
            selectedDifficulty = Difficulty.MEDIUM
        )
    )
    val stats by repository.gameStatsFlow.collectAsState(
        initial = com.example.beamshift.data.GameStats(
            easy = com.example.beamshift.data.DifficultyStats(0, 0, 0),
            medium = com.example.beamshift.data.DifficultyStats(0, 0, 0),
            hard = com.example.beamshift.data.DifficultyStats(0, 0, 0)
        )
    )
    val coroutineScope = rememberCoroutineScope()
    val selectedDifficulty = preferences.selectedDifficulty
    val currentDifficultyStats = stats.forDifficulty(selectedDifficulty)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Top Action Bar (Settings & About)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    soundManager.playClick()
                    hapticsManager.vibrateButton()
                    onSettingsClick()
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    soundManager.playClick()
                    hapticsManager.vibrateButton()
                    onAboutClick()
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Logo Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BEAMSHIFT",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 5.sp,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Find the opening. Shift in time.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LaserCyan,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Stats Card (Best Score & Best Streak)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(vertical = 18.dp, horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BEST SCORE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentDifficultyStats.bestScore}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = LaserCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BEST STREAK",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentDifficultyStats.bestStreak}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = LaserAmber,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

            // Difficulty Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DIFFICULTY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        val isSelected = diff == selectedDifficulty
                        val targetBgColor by animateColorAsState(
                            targetValue = if (isSelected) LaserCyan else Color.Transparent,
                            animationSpec = tween(200),
                            label = "difficulty_bg"
                        )
                        val targetTextColor by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF090B10) else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(200),
                            label = "difficulty_text"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(targetBgColor)
                                .clickable {
                                    if (diff != selectedDifficulty) {
                                        soundManager.playClick()
                                        hapticsManager.vibrateButton()
                                        coroutineScope.launch {
                                            repository.setSelectedDifficulty(diff)
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff.title.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = targetTextColor,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectedDifficulty.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }

            // Large Play Button
            Button(
                onClick = {
                    soundManager.playClick()
                    hapticsManager.vibrateButton()
                    onPlayClick(selectedDifficulty)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LaserCyan
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF090B10),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFF090B10),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp
                        )
                    )
                }
            }
        }
    }
}
