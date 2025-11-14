package edu.jm.tabulavia.model

data class SkillAssessmentsSummary(
    val skillName: String,
    val professorAssessment: SkillAssessment? = null,
    val selfAssessment: SkillAssessment? = null,
    val peerAssessment: SkillAssessment? = null
)
