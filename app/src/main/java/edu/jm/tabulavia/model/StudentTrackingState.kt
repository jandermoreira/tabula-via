package edu.jm.tabulavia.model

/**
 * Represents the diagnostic state of a student's progress based on evidence analysis.
 *
 * This state is calculated by crossing chronology, performance levels, and
 * consistency across multiple evidence sources.
 */
enum class StudentTrackingState {
    /**
     * Learning progress is compatible with expectations.
     * Corresponds to "Acompanhamento Normal".
     */
    NORMAL,

    /**
     * Localized difficulty detected in a single monitoring evidence.
     * Requires specific review of the content.
     * Corresponds to "Revisão Dirigida".
     */
    GUIDED_REVISION,

    /**
     * Persistent difficulties or lack of data (rhythm gaps) detected.
     * Requires closer attention from the teacher.
     * Corresponds to "Acompanhamento Prioritário".
     */
    PRIORITIZED_TRACKING,

    /**
     * Performance gaps confirmed by a consolidation evidence (e.g., exam).
     * Requires recovery intervention.
     * Corresponds to "Recuperação".
     */
    RECOVERY
}
