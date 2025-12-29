package edu.jm.tabulavia.utils

import edu.jm.tabulavia.model.SkillLevel

/**
 * Maps a consolidated numeric value to its corresponding SkillLevel.
 * Based on Section 12 of the technical documentation.
 */
fun Double.toSkillLevel(): SkillLevel {
    return when {
        this < 1.67 -> SkillLevel.LOW
        this < 2.33 -> SkillLevel.MEDIUM
        else -> SkillLevel.HIGH
    }
}