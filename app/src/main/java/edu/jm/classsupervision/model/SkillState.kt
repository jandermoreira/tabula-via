package edu.jm.classsupervision.model

import kotlinx.serialization.Serializable

@Serializable
enum class SkillState(val displayName: String) {
    ALTO("Alto"),
    MEDIO("Médio"),
    BAIXO("Baixo"),
    NAO_SE_APLICA("Não se Aplica")
}
