package com.example.beamshift.game

import com.example.beamshift.data.Difficulty
import kotlin.math.abs

enum class CollisionResult {
    NONE,
    PASSED_SAFELY,
    COLLIDED
}

data class PlayerState(
    val currentLane: Int,
    val targetLane: Int,
    val x: Float, // Normalized X position (0f to 1f)
    val y: Float = 0.85f, // Fixed normalized Y position near arena bottom
    val width: Float,
    val height: Float = 0.024f,
    val isShifting: Boolean = false
) {
    val halfWidth: Float get() = width / 2f
    val halfHeight: Float get() = height / 2f
    val top: Float get() = y - halfHeight
    val bottom: Float get() = y + halfHeight
    val left: Float get() = x - halfWidth
    val right: Float get() = x + halfWidth
}

object CollisionDetector {

    /**
     * Checks if a laser barrier interacts with the player at their current positions.
     */
    fun check(player: PlayerState, barrier: LaserBarrier): CollisionResult {
        // Check vertical overlap
        val verticalOverlap = barrier.bottom >= player.top && barrier.top <= player.bottom
        if (!verticalOverlap) {
            return CollisionResult.NONE
        }

        // Barrier vertically intersects player. Check horizontal safe gap.
        val isSafe = barrier.isPositionSafe(
            playerX = player.x,
            playerHalfWidth = player.halfWidth,
            tolerance = 0.012f
        )

        return if (isSafe) {
            CollisionResult.PASSED_SAFELY
        } else {
            CollisionResult.COLLIDED
        }
    }
}

object ScoreCalculator {

    const val BASE_POINTS_PER_PASS = 100

    /**
     * Streak multiplier increases every 5 consecutive passes (1x, 2x, 3x, etc.).
     */
    fun getStreakMultiplier(streak: Int): Int {
        return 1 + (streak / 5)
    }

    /**
     * Small bonus added for maintaining a long streak.
     */
    fun getStreakBonus(streak: Int): Int {
        return streak * 10
    }

    /**
     * Calculates points for a single passed laser based on current streak.
     */
    fun calculatePointsForPass(currentStreak: Int): Int {
        val multiplier = getStreakMultiplier(currentStreak)
        val bonus = getStreakBonus(currentStreak)
        return (BASE_POINTS_PER_PASS * multiplier) + bonus
    }

    /**
     * Calculates current laser movement speed based on difficulty and lasers passed.
     */
    fun calculateCurrentSpeed(difficulty: Difficulty, lasersPassed: Int): Float {
        val calculated = difficulty.baseSpeed + (difficulty.speedIncrement * lasersPassed)
        return calculated.coerceAtMost(difficulty.maxSpeed)
    }
}

class ShiftAnimator(
    private val lanesCount: Int
) {
    /**
     * Updates player X position moving towards target lane center.
     * @return Pair of updated X and whether shifting completed.
     */
    fun step(currentX: Float, targetLane: Int, deltaSeconds: Float): Pair<Float, Boolean> {
        val targetX = (targetLane + 0.5f) / lanesCount
        val distance = targetX - currentX
        val speed = 9.0f // Normalized units per second for shifting

        if (abs(distance) < 0.005f) {
            return Pair(targetX, false)
        }

        val step = speed * deltaSeconds
        val newX = if (abs(distance) <= step) {
            targetX
        } else if (distance > 0) {
            currentX + step
        } else {
            currentX - step
        }

        val stillMoving = abs(targetX - newX) > 0.005f
        return Pair(newX, stillMoving)
    }
}
