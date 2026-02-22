/**
 * Repository for managing skills, course-specific competencies, and assessments.
 * Handles skill status calculations and historical evaluation data.
 */
package edu.jm.tabulavia.repository

import edu.jm.tabulavia.dao.CourseSkillDao
import edu.jm.tabulavia.dao.SkillAssessmentDao
import edu.jm.tabulavia.dao.SkillDao
import edu.jm.tabulavia.dao.ActivityHighlightedSkillDao
import edu.jm.tabulavia.model.*
import edu.jm.tabulavia.utils.SkillTrendCalculator
import edu.jm.tabulavia.utils.TrendCalculationMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SkillRepository(
    private val courseSkillDao: CourseSkillDao,
    private val skillAssessmentDao: SkillAssessmentDao,
    private val skillDao: SkillDao,
    private val activityHighlightedSkillDao: ActivityHighlightedSkillDao
) {

    /**
     * Retrieves all skills configured for a specific course.
     * @param courseId The course identifier.
     * @return List of CourseSkill objects.
     */
    suspend fun getSkillsForCourse(courseId: Long): List<CourseSkill> {
        return courseSkillDao.getSkillsForCourse(courseId)
    }

    /**
     * Inserts a list of skills for a course.
     * @param skills List of CourseSkill objects to insert.
     */
    suspend fun insertCourseSkills(skills: List<CourseSkill>) {
        courseSkillDao.insertCourseSkills(skills)
    }

    /**
     * Removes a specific skill from a course.
     * @param skill The CourseSkill object to delete.
     */
    suspend fun deleteCourseSkill(skill: CourseSkill) {
        courseSkillDao.deleteCourseSkill(skill)
    }

    /**
     * Persists a single skill assessment record.
     * @param assessment The SkillAssessment to insert.
     */
    suspend fun insertAssessment(assessment: SkillAssessment) {
        skillAssessmentDao.insert(assessment)
    }

    /**
     * Persists multiple skill assessments at once.
     * @param assessments List of SkillAssessment objects to insert.
     */
    suspend fun insertAllAssessments(assessments: List<SkillAssessment>) {
        skillAssessmentDao.insertAll(assessments)
    }

    /**
     * Calculates the current status and trend for each skill of a student.
     * @param studentId The student's identifier.
     * @param courseSkills List of course skills to evaluate.
     * @param historyCount Number of historical assessments to consider for trend calculation.
     * @return List of SkillStatus objects with current level, trend, and metadata.
     */
    suspend fun calculateStudentSkillStatuses(
        studentId: Long,
        courseSkills: List<CourseSkill>,
        historyCount: Int = 3
    ): List<SkillStatus> {
        val allAssessments = skillAssessmentDao.getAllAssessmentsForStudent(studentId).first()

        return courseSkills.map { courseSkill ->
            val relevant = allAssessments
                .filter { it.skillName == courseSkill.skillName }
                .sortedByDescending { it.timestamp }
                .distinctBy { it.timestamp }

            if (relevant.isEmpty()) {
                SkillStatus(
                    skillName = courseSkill.skillName,
                    currentLevel = SkillLevel.NOT_APPLICABLE,
                    trend = SkillTrend.STABLE,
                    assessmentCount = 0,
                    lastAssessedTimestamp = 0L
                )
            } else {
                val skillStatusesForTrend = relevant.map { assessment ->
                    SkillStatus(
                        skillName = assessment.skillName,
                        currentLevel = assessment.level,
                        trend = SkillTrend.STABLE,
                        assessmentCount = relevant.size,
                        lastAssessedTimestamp = assessment.timestamp
                    )
                }

                val calculatedTrend = if (skillStatusesForTrend.size < 2) {
                    SkillTrend.STABLE
                } else {
                    val distinctScores = skillStatusesForTrend.mapNotNull { it.currentLevel.score }.distinct()
                    if (distinctScores.size < 2) {
                        SkillTrend.STABLE
                    } else {
                        SkillTrendCalculator.calculateTrend(
                            assessments = skillStatusesForTrend,
                            method = TrendCalculationMethod.LINEAR_REGRESSION,
                            historyCount = historyCount
                        )
                    }
                }

                SkillStatus(
                    skillName = courseSkill.skillName,
                    currentLevel = skillStatusesForTrend.first().currentLevel,
                    trend = calculatedTrend,
                    assessmentCount = relevant.size,
                    lastAssessedTimestamp = skillStatusesForTrend.first().lastAssessedTimestamp
                )
            }
        }
    }

    /**
     * Updates highlighted skills for a specific activity.
     * Replaces any existing highlighted skills for that activity with the new list.
     * @param activityId The activity identifier.
     * @param skills List of ActivityHighlightedSkill objects to associate with the activity.
     */
    suspend fun updateActivityHighlightedSkills(activityId: Long, skills: List<ActivityHighlightedSkill>) {
        activityHighlightedSkillDao.clearForActivity(activityId)
        activityHighlightedSkillDao.insertAll(skills)
    }

    /**
     * Inserts multiple highlighted skills without clearing any existing data.
     * Used primarily during database restoration.
     * @param skills List of ActivityHighlightedSkill objects to insert.
     */
    suspend fun insertAllHighlightedSkills(skills: List<ActivityHighlightedSkill>) {
        activityHighlightedSkillDao.insertAll(skills)
    }

    // --- Backup and Restore Operations ---

    /**
     * Retrieves all skill assessments for backup purposes.
     * @return Flow emitting the list of all SkillAssessment objects.
     */
    fun getAllAssessments(): Flow<List<SkillAssessment>> {
        return skillAssessmentDao.getAllAssessments()
    }

    /**
     * Retrieves all course skills for backup purposes.
     * @return List of all CourseSkill objects.
     */
    suspend fun getAllCourseSkills(): List<CourseSkill> {
        return courseSkillDao.getAllCourseSkills()
    }

    /**
     * Retrieves all student skills for backup purposes.
     * @return List of all StudentSkill objects.
     */
    suspend fun getAllStudentSkills(): List<StudentSkill> {
        return skillDao.getAllSkills()
    }

    /**
     * Restores student skills into the database.
     * @param skills List of StudentSkill objects to insert or update.
     */
    suspend fun insertOrUpdateStudentSkills(skills: List<StudentSkill>) {
        skillDao.insertOrUpdateSkills(skills)
    }

    /**
     * Retrieves all activity highlighted skills for backup purposes.
     * @return List of all ActivityHighlightedSkill objects.
     */
    suspend fun getAllHighlightedSkills(): List<ActivityHighlightedSkill> {
        return activityHighlightedSkillDao.getAll()
    }
}