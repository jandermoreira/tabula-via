package edu.jm.tabulavia.model

/**
 * Represents the performance trend of a student between evidences.
 */
enum class EvidenceTrend {
    /** Performance improved compared to the previous evidence. */
    IMPROVED,

    /** Performance remained the same compared to the previous evidence. */
    STABLE,

    /** Performance decreased compared to the previous evidence. */
    WORSENED,

    /** Not enough data to determine a trend. */
    UNKNOWN
}
