package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

@Serializable
enum class AssessmentSource(val displayName: String) {
    PROFESSOR_OBSERVATION("Observação do Professor"),
    SELF_ASSESSMENT("Autoavaliação"),
    PEER_ASSESSMENT("Avaliação entre Pares")
}
