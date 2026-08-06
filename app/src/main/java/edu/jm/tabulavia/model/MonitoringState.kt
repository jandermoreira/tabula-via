package edu.jm.tabulavia.model

/**
 * Categorizes students based on their work rhythm and required pedagogical intervention.
 */
enum class MonitoringState {
    /**
     * Indicates the student maintains the expected work rhythm.
     */
    ON_TRACK,

    /**
     * Indicates the student shows initial signals of rhythm interruption or low performance.
     */
    ATTENTION,

    /**
     * Indicates the student shows critical interruption of work rhythm or persistent low performance.
     */
    CRITICAL
}
