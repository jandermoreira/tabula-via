package edu.jm.tabulavia.model

/**
 * Represents the diagnostic state of a student based on evidence analysis.
 */
enum class MonitoringState {
    /**
     * Learning is compatible with expectations.
     */
    NORMAL,

    /**
     * Localized difficulty detected in a monitoring evidence.
     */
    REVIEW,

    /**
     * Persistent difficulties or rhythm failures detected.
     */
    PRIORITY,

    /**
     * Knowledge gaps confirmed by consolidation evidence.
     */
    RECOVERY
}
