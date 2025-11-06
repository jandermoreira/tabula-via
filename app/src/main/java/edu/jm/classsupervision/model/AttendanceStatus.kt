package edu.jm.classsupervision.model

import kotlinx.serialization.Serializable

@Serializable // Anotação para serialização
enum class AttendanceStatus(val displayName: String) {
    PRESENT("Presente"),
    ABSENT("Ausente"),
    JUSTIFIED("Justificado")
}
