package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

@Serializable
enum class AttendanceStatus(val displayName: String) {
    PRESENT("Presente"),
    ABSENT("Ausente"),
    JUSTIFIED("Justificado")
}
