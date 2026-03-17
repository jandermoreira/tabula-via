package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

@Serializable
enum class SkillState(val displayName: String) {
    HIGH("Alto"),
    MEDIUM("Médio"),
    LOW("Baixo"),
    NOT_APPLICABLE("Não se Aplica")
}
