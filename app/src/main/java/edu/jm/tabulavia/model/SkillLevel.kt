package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

@Serializable
enum class SkillLevel(
    val displayName: String,
    val score: Int?
) {
    NOT_APPLICABLE("Não se Aplica", null),
    LOW("Baixo", 1),
    MEDIUM("Médio", 2),
    HIGH("Alto", 3);

    fun isApplicable(): Boolean = score != null
}
