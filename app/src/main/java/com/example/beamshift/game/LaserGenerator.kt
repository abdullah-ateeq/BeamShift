package com.example.beamshift.game

import com.example.beamshift.data.Difficulty
import java.util.Random

data class LaserBarrier(
    val id: Long,
    val y: Float, // Normalized Y position (0.0 = top of playfield, 1.0 = bottom)
    val height: Float = 0.024f, // Normalized thickness of laser beam
    val safeLaneIndex: Int,
    val gapCenterX: Float, // Normalized X center of opening
    val gapWidth: Float, // Normalized width of opening
    val passed: Boolean = false,
    val colorIndex: Int = 0
) {
    val top: Float get() = y - height / 2f
    val bottom: Float get() = y + height / 2f
    val gapLeft: Float get() = (gapCenterX - gapWidth / 2f).coerceAtLeast(0f)
    val gapRight: Float get() = (gapCenterX + gapWidth / 2f).coerceAtMost(1f)

    fun isPositionSafe(playerX: Float, playerHalfWidth: Float, tolerance: Float = 0.015f): Boolean {
        val playerLeft = playerX - playerHalfWidth
        val playerRight = playerX + playerHalfWidth
        // Player must be completely within the safe gap (with small generous grace margin)
        return playerLeft >= (gapLeft - tolerance) && playerRight <= (gapRight + tolerance)
    }
}

class LaserGenerator(
    private val difficulty: Difficulty,
    private val random: Random = Random()
) {
    private var repeatCount = 0
    private var previousLane = difficulty.lanesCount / 2

    /**
     * Generates a new laser barrier with a safe opening guaranteed to be reachable.
     */
    fun generateNextBarrier(
        lastSafeLane: Int = previousLane,
        barrierId: Long = System.nanoTime(),
        initialY: Float = -0.05f
    ): LaserBarrier {
        val lanes = difficulty.lanesCount
        val maxShift = when (difficulty) {
            Difficulty.EASY -> 1 // at most 1 lane shift away (adjacent or same)
            Difficulty.MEDIUM -> 2 // at most 2 lanes shift
            Difficulty.HARD -> 3 // at most 3 lanes shift
        }

        val minLane = (lastSafeLane - maxShift).coerceAtLeast(0)
        val maxLane = (lastSafeLane + maxShift).coerceAtMost(lanes - 1)

        val candidateLanes = (minLane..maxLane).toMutableList()

        // Avoid repeating the exact same lane more than twice
        if (repeatCount >= 2 && candidateLanes.size > 1) {
            candidateLanes.remove(lastSafeLane)
        }

        val chosenLane = candidateLanes[random.nextInt(candidateLanes.size)]
        if (chosenLane == lastSafeLane) {
            repeatCount++
        } else {
            repeatCount = 0
        }
        previousLane = chosenLane

        val laneWidth = 1f / lanes
        val gapWidth = (laneWidth * difficulty.gapWidthMultiplier).coerceAtLeast(0.10f)
        val gapCenterX = (chosenLane + 0.5f) * laneWidth

        val colorIndex = random.nextInt(3)

        return LaserBarrier(
            id = barrierId,
            y = initialY,
            height = 0.024f,
            safeLaneIndex = chosenLane,
            gapCenterX = gapCenterX,
            gapWidth = gapWidth,
            colorIndex = colorIndex
        )
    }

    fun reset(initialLane: Int = difficulty.lanesCount / 2) {
        previousLane = initialLane
        repeatCount = 0
    }
}
