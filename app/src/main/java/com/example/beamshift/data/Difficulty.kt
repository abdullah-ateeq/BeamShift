package com.example.beamshift.data

enum class Difficulty(
    val id: String,
    val title: String,
    val lanesCount: Int,
    val baseSpeed: Float, // Fraction of arena height per second
    val speedIncrement: Float, // Speed increase per laser passed
    val maxSpeed: Float,
    val gapWidthMultiplier: Float, // Relative to lane width
    val description: String
) {
    EASY(
        id = "easy",
        title = "Easy",
        lanesCount = 3,
        baseSpeed = 0.36f,
        speedIncrement = 0.010f,
        maxSpeed = 0.75f,
        gapWidthMultiplier = 1.30f,
        description = "3 lanes • Slow speed • Wide opening"
    ),
    MEDIUM(
        id = "medium",
        title = "Medium",
        lanesCount = 5,
        baseSpeed = 0.46f,
        speedIncrement = 0.016f,
        maxSpeed = 1.05f,
        gapWidthMultiplier = 1.05f,
        description = "5 lanes • Normal speed • Standard opening"
    ),
    HARD(
        id = "hard",
        title = "Hard",
        lanesCount = 7,
        baseSpeed = 0.56f,
        speedIncrement = 0.022f,
        maxSpeed = 1.35f,
        gapWidthMultiplier = 0.88f,
        description = "7 lanes • Fast speed • Narrow opening"
    );

    companion object {
        fun fromId(id: String?): Difficulty {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
        }
    }
}
