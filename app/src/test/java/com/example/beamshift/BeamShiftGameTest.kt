package com.example.beamshift

import com.example.beamshift.data.Difficulty
import com.example.beamshift.game.CollisionDetector
import com.example.beamshift.game.CollisionResult
import com.example.beamshift.game.LaserBarrier
import com.example.beamshift.game.LaserGenerator
import com.example.beamshift.game.PlayerState
import com.example.beamshift.game.ScoreCalculator
import com.example.beamshift.game.ShiftAnimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs

class BeamShiftGameTest {

    @Test
    fun testSafeGapGeneration_isWithinBoundsAndCorrectLane() {
        Difficulty.entries.forEach { diff ->
            val generator = LaserGenerator(diff, Random(42))
            val lanes = diff.lanesCount

            for (i in 0 until 50) {
                val barrier = generator.generateNextBarrier()

                // Check lane index bounds
                assertTrue(
                    "Safe lane should be within [0, ${lanes - 1}] for ${diff.title}",
                    barrier.safeLaneIndex in 0 until lanes
                )

                // Check center X aligns with safe lane
                val expectedCenterX = (barrier.safeLaneIndex + 0.5f) / lanes
                assertEquals(
                    "Gap center X must align with lane center",
                    expectedCenterX,
                    barrier.gapCenterX,
                    0.001f
                )

                // Check gap width is positive and reasonable
                assertTrue("Gap width must be > 0", barrier.gapWidth > 0f)
                assertTrue("Gap width must be <= 1.0", barrier.gapWidth <= 1.0f)

                // Check gap bounds
                assertTrue("Gap left bound >= 0f", barrier.gapLeft >= 0f)
                assertTrue("Gap right bound <= 1.0f", barrier.gapRight <= 1.0f)
            }
        }
    }

    @Test
    fun testValidLaserGeneration_reachableOpenings() {
        // Test that across 100 barriers, the shift between consecutive barriers is always reachable
        Difficulty.entries.forEach { diff ->
            val generator = LaserGenerator(diff, Random(12345))
            var lastLane = diff.lanesCount / 2
            val maxAllowedShift = when (diff) {
                Difficulty.EASY -> 1
                Difficulty.MEDIUM -> 2
                Difficulty.HARD -> 3
            }

            for (i in 0 until 100) {
                val barrier = generator.generateNextBarrier(lastSafeLane = lastLane)
                val shift = abs(barrier.safeLaneIndex - lastLane)

                assertTrue(
                    "Barrier shift $shift exceeded max reachable shift $maxAllowedShift on ${diff.title}",
                    shift <= maxAllowedShift
                )
                lastLane = barrier.safeLaneIndex
            }
        }
    }

    @Test
    fun testCollisionDetection_noVerticalOverlap() {
        val player = PlayerState(
            currentLane = 1,
            targetLane = 1,
            x = 0.5f,
            y = 0.85f,
            width = 0.1f
        )
        // Laser barrier high above player
        val barrierAbove = LaserBarrier(
            id = 1,
            y = 0.3f,
            height = 0.024f,
            safeLaneIndex = 0,
            gapCenterX = 0.166f,
            gapWidth = 0.3f
        )

        val result = CollisionDetector.check(player, barrierAbove)
        assertEquals(CollisionResult.NONE, result)
    }

    @Test
    fun testCollisionDetection_safePassThroughOpening() {
        val lanes = 3
        val safeLane = 1
        val laneWidth = 1f / lanes
        val gapCenterX = (safeLane + 0.5f) * laneWidth
        val gapWidth = laneWidth * 1.3f

        // Player is at lane 1 (center = 0.5f)
        val player = PlayerState(
            currentLane = safeLane,
            targetLane = safeLane,
            x = 0.5f,
            y = 0.85f,
            width = 0.12f
        )

        // Barrier vertically intersects player at y = 0.85f with gap centered at lane 1
        val barrier = LaserBarrier(
            id = 2,
            y = 0.85f,
            height = 0.024f,
            safeLaneIndex = safeLane,
            gapCenterX = gapCenterX,
            gapWidth = gapWidth
        )

        val result = CollisionDetector.check(player, barrier)
        assertEquals(CollisionResult.PASSED_SAFELY, result)
    }

    @Test
    fun testCollisionDetection_collisionInLaserBeam() {
        val lanes = 5
        val safeLane = 4 // gap is at lane 4 (far right)
        val laneWidth = 1f / lanes
        val gapCenterX = (safeLane + 0.5f) * laneWidth
        val gapWidth = laneWidth * 1.0f

        // Player is at lane 0 (far left, x = 0.1f)
        val player = PlayerState(
            currentLane = 0,
            targetLane = 0,
            x = 0.1f,
            y = 0.85f,
            width = 0.1f
        )

        // Barrier vertically intersects player
        val barrier = LaserBarrier(
            id = 3,
            y = 0.85f,
            height = 0.024f,
            safeLaneIndex = safeLane,
            gapCenterX = gapCenterX,
            gapWidth = gapWidth
        )

        val result = CollisionDetector.check(player, barrier)
        assertEquals(CollisionResult.COLLIDED, result)
    }

    @Test
    fun testDifficultyProgression_speedCalculation() {
        val diff = Difficulty.MEDIUM
        val initialSpeed = ScoreCalculator.calculateCurrentSpeed(diff, lasersPassed = 0)
        assertEquals(diff.baseSpeed, initialSpeed, 0.001f)

        val speedAfter10 = ScoreCalculator.calculateCurrentSpeed(diff, lasersPassed = 10)
        assertTrue(
            "Speed after 10 passes ($speedAfter10) should be > base speed ($initialSpeed)",
            speedAfter10 > initialSpeed
        )

        // Speed should never exceed maxSpeed
        val speedAfter1000 = ScoreCalculator.calculateCurrentSpeed(diff, lasersPassed = 1000)
        assertEquals(diff.maxSpeed, speedAfter1000, 0.001f)
    }

    @Test
    fun testDifficultyProgression_harderTiersAreFasterAndNarrower() {
        assertTrue(Difficulty.EASY.baseSpeed < Difficulty.MEDIUM.baseSpeed)
        assertTrue(Difficulty.MEDIUM.baseSpeed < Difficulty.HARD.baseSpeed)

        assertTrue(Difficulty.EASY.gapWidthMultiplier > Difficulty.MEDIUM.gapWidthMultiplier)
        assertTrue(Difficulty.MEDIUM.gapWidthMultiplier > Difficulty.HARD.gapWidthMultiplier)

        assertTrue(Difficulty.EASY.lanesCount < Difficulty.MEDIUM.lanesCount)
        assertTrue(Difficulty.MEDIUM.lanesCount < Difficulty.HARD.lanesCount)
    }

    @Test
    fun testScoreCalculation_basePointsAndStreakBonus() {
        // Streak 0: base points only = 100 * 1 + 0 = 100
        val points0 = ScoreCalculator.calculatePointsForPass(currentStreak = 0)
        assertEquals(100, points0)

        // Streak 3: multiplier 1, bonus 30 -> 100 * 1 + 30 = 130
        val points3 = ScoreCalculator.calculatePointsForPass(currentStreak = 3)
        assertEquals(130, points3)

        // Streak 5: multiplier 2, bonus 50 -> 100 * 2 + 50 = 250
        val points5 = ScoreCalculator.calculatePointsForPass(currentStreak = 5)
        assertEquals(250, points5)

        // Streak 10: multiplier 3, bonus 100 -> 100 * 3 + 100 = 400
        val points10 = ScoreCalculator.calculatePointsForPass(currentStreak = 10)
        assertEquals(400, points10)

        assertTrue("Higher streaks must award strictly more points", points10 > points5)
        assertTrue("Higher streaks must award strictly more points", points5 > points3)
    }

    @Test
    fun testShiftAnimator_movesSmoothlyTowardsTarget() {
        val lanes = 5
        val animator = ShiftAnimator(lanes)
        val initialX = 0.5f // center lane (lane 2)
        val targetLane = 3 // target right lane (center = 0.7f)

        val (nextX, isMoving) = animator.step(
            currentX = initialX,
            targetLane = targetLane,
            deltaSeconds = 0.016f
        )

        assertTrue("Player should have shifted right towards 0.7f", nextX > initialX)
        assertTrue("Player should still be moving in a single frame", isMoving)

        // Simulate multiple steps to complete movement
        var x = nextX
        var moving = isMoving
        for (step in 0 until 30) {
            val result = animator.step(x, targetLane, 0.016f)
            x = result.first
            moving = result.second
            if (!moving) break
        }

        assertFalse("Player should have reached target lane", moving)
        assertEquals(0.7f, x, 0.005f)
    }
}
