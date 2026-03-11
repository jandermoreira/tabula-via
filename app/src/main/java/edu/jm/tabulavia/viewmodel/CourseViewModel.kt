/**
 * ViewModel for the Course management.
 * Orchestrates UI state and business logic for courses, students, attendance, and skills.
 */
package edu.jm.tabulavia.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import com.google.firebase.storage.ktx.storage
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.*
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)

    private val courseRepository = CourseRepository(
        courseDao = db.courseDao(),
        activityDao = db.activityDao(),
        groupMemberDao = db.groupMemberDao(),
        firestore = Firebase.firestore
    )
    private val studentRepository = StudentRepository(
        studentDao = db.studentDao(),
        firestore = Firebase.firestore
    )
    private val attendanceRepository = AttendanceRepository(
        attendanceDao = db.attendanceDao()
    )
    private val skillRepository = SkillRepository(
        courseSkillDao = db.courseSkillDao(),
        skillAssessmentDao = db.skillAssessmentDao(),
        skillDao = db.skillDao(),
        activityHighlightedSkillDao = db.activityHighlightedSkillDao()
    )
    private val cloudStorageRepository = CloudStorageRepository(
        storage = Firebase.storage,
        auth = Firebase.auth
    )

    // --- UI State Streams ---
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()

    private val _selectedActivity = MutableStateFlow<Activity?>(null)
    val selectedActivity: StateFlow<Activity?> = _selectedActivity.asStateFlow()

    private val _studentsForClass = MutableStateFlow<List<Student>>(emptyList())
    val studentsForClass: StateFlow<List<Student>> = _studentsForClass.asStateFlow()

    private val _classSessions = MutableStateFlow<List<ClassSession>>(emptyList())
    val classSessions: StateFlow<List<ClassSession>> = _classSessions.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _frequencyDetails = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val frequencyDetails: StateFlow<Map<String, AttendanceStatus>> = _frequencyDetails.asStateFlow()

    private val _selectedStudentDetails = MutableStateFlow<Student?>(null)
    val selectedStudentDetails: StateFlow<Student?> = _selectedStudentDetails.asStateFlow()

    private val _studentAttendancePercentage = MutableStateFlow<Float?>(null)
    val studentAttendancePercentage: StateFlow<Float?> = _studentAttendancePercentage.asStateFlow()

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _skillAssessmentLog = MutableStateFlow<List<SkillAssessment>>(emptyList())
    val skillAssessmentLog: StateFlow<List<SkillAssessment>> = _skillAssessmentLog.asStateFlow()

    private val _studentSkillSummaries =
        MutableStateFlow<Map<String, SkillAssessmentsSummary>>(emptyMap())
    val studentSkillSummaries: StateFlow<Map<String, SkillAssessmentsSummary>> =
        _studentSkillSummaries.asStateFlow()

    private val _courseSkills = MutableStateFlow<List<CourseSkill>>(emptyList())
    val courseSkills: StateFlow<List<CourseSkill>> = _courseSkills.asStateFlow()

    private val _generatedGroups = MutableStateFlow<List<List<Student>>>(emptyList())
    val generatedGroups: StateFlow<List<List<Student>>> = _generatedGroups.asStateFlow()

    private val _todaysAttendance = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val todaysAttendance: StateFlow<Map<String, AttendanceStatus>> = _todaysAttendance.asStateFlow()

    private val _selectedGroupDetails = MutableStateFlow<List<Student>>(emptyList())
    val selectedGroupDetails: StateFlow<List<Student>> = _selectedGroupDetails.asStateFlow()

    private val _studentSkillStatuses = MutableStateFlow<List<SkillStatus>>(emptyList())
    val studentSkillStatuses: StateFlow<List<SkillStatus>> = _studentSkillStatuses.asStateFlow()

    private val _groupsLoaded = MutableStateFlow(false)
    val groupsLoaded: StateFlow<Boolean> = _groupsLoaded.asStateFlow()

    // --- Compose-driven Form State ---
    var courseName by mutableStateOf("")
    var academicYear by mutableStateOf("")
    var period by mutableStateOf("")
    var numberOfClasses by mutableIntStateOf(0)
    var studentName by mutableStateOf("")
    var studentDisplayName by mutableStateOf("")
    var studentNumber by mutableStateOf("")
    var bulkStudentText by mutableStateOf("")
    var newSessionCalendar by mutableStateOf(Calendar.getInstance())
    var editingSession by mutableStateOf<ClassSession?>(null)
    var activityName by mutableStateOf("")
    var activityType by mutableStateOf("Individual")
    var activityHighlightedSkills by mutableStateOf<Set<String>>(emptySet())
    var skillName by mutableStateOf("")

    var groupingCriterion by mutableStateOf("Aleatório")
    var groupFormationType by mutableStateOf("Número de grupos")
    var groupFormationValue by mutableStateOf("")

    // --- Loading and Clearing Logic ---

    /**
     * Loads the full list of available courses.
     */
    fun loadAllCourses() {
        viewModelScope.launch {
            _courses.value = courseRepository.getAllCourses()
        }
    }

    /**
     * Fetches details for a specific class including students, sessions, and activities.
     */
    fun loadCourseDetails(classId: String) {
        viewModelScope.launch {
            _selectedCourse.value = courseRepository.getCourseById(classId)
            _studentsForClass.value = studentRepository.getStudentsForClass(classId)
            val allSessions = attendanceRepository.getClassSessions(classId)
            _classSessions.value = allSessions
            _activities.value = courseRepository.getActivitiesForClass(classId)
            _courseSkills.value = skillRepository.getSkillsForCourse(classId)

            loadTodaysAttendance(allSessions)
        }
    }

    /**
     * Resets the UI state related to the currently selected course.
     */
    fun clearCourseDetails() {
        _selectedCourse.value = null
        _studentsForClass.value = emptyList()
        _classSessions.value = emptyList()
        _activities.value = emptyList()
        editingSession = null
        _todaysAttendance.value = emptyMap()
        _courseSkills.value = emptyList()
        _studentSkillStatuses.value = emptyList()
    }

    /**
     * Clears the user notification message state.
     */
    fun onUserMessageShown() {
        _userMessage.value = null
    }

    // --- Student Management Logic ---

    /**
     * Populates form fields for student editing.
     */
    fun selectStudentForEditing(student: Student) {
        studentName = student.name
        studentDisplayName = student.displayName
        studentNumber = student.studentNumber
        _selectedStudentDetails.value = student
    }

    /**
     * Persists updates to an existing student profile.
     */
    fun updateStudent(onDismiss: () -> Unit) {
        val studentToUpdate = _selectedStudentDetails.value ?: return
        val updatedStudent = studentToUpdate.copy(
            name = studentName,
            displayName = studentDisplayName,
            studentNumber = studentNumber
        )
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            studentRepository.insertStudent(updatedStudent, uid)
            loadCourseDetails(studentToUpdate.classId)
            onDismiss()
        }
    }

    /**
     * Loads comprehensive data for a student, including attendance percentage and skill status.
     */
    fun loadStudentDetails(studentId: String) {
        viewModelScope.launch {
            _selectedStudentDetails.value = studentRepository.getStudentById(studentId)

            val classId = _selectedCourse.value?.classId ?: return@launch
            val courseSkills = skillRepository.getSkillsForCourse(classId)
            _courseSkills.value = courseSkills

            val statuses = skillRepository.calculateStudentSkillStatuses(studentId, courseSkills)
            _studentSkillStatuses.value = statuses

            val totalClasses = _selectedCourse.value?.numberOfClasses ?: 0
            if (totalClasses > 0) {
                val absences = attendanceRepository.countStudentAbsences(studentId)
                _studentAttendancePercentage.value =
                    ((totalClasses.toFloat() - absences.toFloat()) / totalClasses.toFloat()) * 100
            } else {
                _studentAttendancePercentage.value = null
            }
        }
    }

    /**
     * Clears student-specific detail states.
     */
    fun clearStudentDetails() {
        _selectedStudentDetails.value = null
        _studentAttendancePercentage.value = null
        _studentSkillSummaries.value = emptyMap()
        _studentSkillStatuses.value = emptyList()
    }

    // --- Skill Assessment Logic ---
    private val defaultSkills = listOf(
        "Pensamento Analítico",
        "Capacidade de Abstração",
        "Resolução de Problemas",
        "Avaliação Crítica",
        "Trabalho em Equipe",
        "Comunicação",
        "Autogestão"
    )

    private val defaultComputerScienceSkills = listOf(
        "Rigor Analítico",
        "Abstração e Modelagem",
        "Projeto de Soluções",
        "Validação e Depuração",
        "Colaboração",
        "Comunicação Técnica",
        "Autogestão e Evolução Pessoal"
    )

    /**
     * Fetches skills associated with a course.
     */
    fun loadSkillsForCourse(courseId: String) {
        viewModelScope.launch {
            _courseSkills.value = skillRepository.getSkillsForCourse(courseId)
        }
    }

    /**
     * Loads the complete log of skill assessments.
     */
    fun loadSkillAssessmentLog() {
        viewModelScope.launch {
            val currentLog = skillRepository.getAllAssessments().first()
            _skillAssessmentLog.value = currentLog
        }
    }

    /**
     * Adds a new skill to the current course.
     */
    fun addCourseSkill(onSkillAdded: () -> Unit) {
        val courseId = _selectedCourse.value?.classId ?: return
        if (skillName.isNotBlank()) {
            viewModelScope.launch {
                skillRepository.insertCourseSkills(listOf(CourseSkill(courseId, skillName)))
                skillName = ""
                loadSkillsForCourse(courseId)
                onSkillAdded()
            }
        }
    }

    /**
     * Removes a skill from the course.
     */
    fun deleteCourseSkill(skill: CourseSkill) {
        viewModelScope.launch {
            skillRepository.deleteCourseSkill(skill)
            loadSkillsForCourse(skill.courseId)
        }
    }

    /**
     * records a single skill assessment for a student.
     */
    fun addSkillAssessment(
        studentId: String,
        skillName: String,
        level: SkillLevel,
        source: AssessmentSource,
        assessorId: Long? = null,
        timestamp: Long? = null
    ) {
        viewModelScope.launch {
            val assessment = SkillAssessment(
                studentId = studentId,
                skillName = skillName,
                level = level,
                source = source,
                assessorId = assessorId,
                timestamp = timestamp ?: System.currentTimeMillis()
            )
            skillRepository.insertAssessment(assessment)
            loadStudentDetails(studentId)
        }
    }

    /**
     * Records multiple professor observations for a student.
     */
    fun addProfessorSkillAssessments(
        studentId: String,
        assessments: List<Pair<String, SkillLevel>>
    ) {
        viewModelScope.launch {
            val newAssessments = assessments.map { (skillName, level) ->
                SkillAssessment(
                    studentId = studentId,
                    skillName = skillName,
                    level = level,
                    source = AssessmentSource.PROFESSOR_OBSERVATION,
                    assessorId = null
                )
            }
            skillRepository.insertAllAssessments(newAssessments)
            loadStudentDetails(studentId)
        }
    }

    // --- Activity and Grouping Logic ---
    // --- State Updates ---
    private val _loadedActivityId = MutableStateFlow<String?>(null)
    val loadedActivityId: StateFlow<String?> = _loadedActivityId.asStateFlow()

    /**
     * Loads an activity and its persisted groups.
     */
    fun loadActivityDetails(activityId: String) {
        _groupsLoaded.value = false
        _loadedActivityId.value = null

        viewModelScope.launch {
            val activity = courseRepository.getActivityById(activityId)
            _selectedActivity.value = activity
            if (activity != null) {
                loadPersistedGroups(activityId, activity.classId)
            } else {
                _generatedGroups.value = emptyList()
            }
            _loadedActivityId.value = activityId
            _groupsLoaded.value = true
        }
    }

    /**
     * Helper to load saved groups from the database.
     */
    private suspend fun loadPersistedGroups(activityId: String, classId: String) {
        val groupMembers = courseRepository.getGroupMembers(activityId)
        if (groupMembers.isNotEmpty()) {
            val students = studentRepository.getStudentsForClass(classId)
            val studentMap = students.associateBy { it.studentId }
            val groups = groupMembers.groupBy { it.groupNumber }
                .toSortedMap()
                .values
                .map { members -> members.mapNotNull { studentMap[it.studentId] } }
            _generatedGroups.value = groups
        } else {
            _generatedGroups.value = emptyList()
        }
    }

    /**
     * Resets activity and manual grouping states.
     */
    fun clearActivityState() {
        _groupsLoaded.value = false
        _loadedActivityId.value = null
        _generatedGroups.value = emptyList()

        isManualMode = false
        manualGroups.clear()
        unassignedStudents.clear()
    }

    /**
     * Creates a new activity for the current course.
     * Generates a persistent UUID and synchronizes with Firestore using the user's UID.
     */
    fun addActivity(onActivityAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        val uid = com.google.firebase.Firebase.auth.currentUser?.uid ?: return

        if (activityName.isNotBlank()) {
            viewModelScope.launch {
                val savedTitle = activityName
                val newActivityId = java.util.UUID.randomUUID().toString()

                val newActivity = Activity(
                    activityId = newActivityId,
                    title = savedTitle,
                    description = activityType,
                    classId = classId
                )

                courseRepository.insertActivity(newActivity, uid)

                val highlightedSkills = activityHighlightedSkills
                    .sorted()
                    .map { skillName ->
                        ActivityHighlightedSkill(
                            activityId = newActivityId,
                            skillName = skillName
                        )
                    }

                skillRepository.updateActivityHighlightedSkills(newActivityId, highlightedSkills)

                activityName = ""
                activityType = "Grupo"
                activityHighlightedSkills = emptySet()

                loadActivitiesForClass(classId)
                onActivityAdded()
                _userMessage.value = "Atividade '$savedTitle' adicionada."
            }
        }
    }

    /**
     * Fetches all activities for a class.
     */
    fun loadActivitiesForClass(classId: String) {
        viewModelScope.launch {
            _activities.value = courseRepository.getActivitiesForClass(classId)
        }
    }

    /**
     * Formats balanced groups based on the configured criteria and present students.
     */
    fun createBalancedGroups() {
        viewModelScope.launch {
            val activityId = _selectedActivity.value?.activityId ?: return@launch
            val value = groupFormationValue.toIntOrNull()
            if (value == null || value <= 0) {
                _userMessage.value = "Por favor, insira um valor válido."
                return@launch
            }

            val presentStudents = _studentsForClass.value
                .filter { _todaysAttendance.value[it.studentId] != AttendanceStatus.ABSENT }
                .shuffled()

            if (presentStudents.isEmpty()) {
                _userMessage.value = "Nenhum aluno presente para formar grupos."
                return@launch
            }

            val numGroups = if (groupFormationType == "Número de grupos") {
                if (value > presentStudents.size) {
                    _userMessage.value =
                        "O número de grupos não pode ser maior que o de alunos presentes."
                    return@launch
                }
                value
            } else {
                (presentStudents.size + value - 1) / value
            }

            val baseGroupSize = presentStudents.size / numGroups
            val remainder = presentStudents.size % numGroups

            val groups = MutableList(numGroups) { mutableListOf<Student>() }
            var studentIndex = 0

            for (i in 0 until numGroups) {
                val extra = if (i < remainder) 1 else 0
                val currentGroupSize = baseGroupSize + extra
                for (j in 0 until currentGroupSize) {
                    if (studentIndex < presentStudents.size) {
                        groups[i].add(presentStudents[studentIndex])
                        studentIndex++
                    }
                }
            }
            _generatedGroups.value = groups
            courseRepository.persistGroups(activityId, groups)
        }
    }

    /**
     * Selects a group to display its members.
     */
    fun loadGroupDetails(groupNumber: Int) {
        val group = _generatedGroups.value.getOrNull(groupNumber - 1)
        _selectedGroupDetails.value = group ?: emptyList()
    }

    /**
     * Clears the selected group members list.
     */
    fun clearGroupDetails() {
        _selectedGroupDetails.value = emptyList()
    }

// --- Frequency Management Logic ---

    /**
     * Identifica e carrega registros de frequência para a sessão mais recente de hoje.
     */
    private fun loadTodaysAttendance(sessions: List<ClassSession>) {
        viewModelScope.launch {
            val lastSessionToday = attendanceRepository.getLastSessionToday(sessions)

            _todaysAttendance.value = if (lastSessionToday != null) {
                val records = attendanceRepository.getRecordsForSession(lastSessionToday.sessionId)
                records.associate { it.studentId to it.status }
            } else {
                emptyMap()
            }
        }
    }

    /**
     * Carrega detalhes de frequência para uma sessão específica.
     */
    suspend fun loadFrequencyDetails(session: ClassSession) {
        val records = attendanceRepository.getRecordsForSession(session.sessionId)
        val studentNameMap = studentsForClass.value.associate { it.studentId to it.displayName }

        _frequencyDetails.value = records.mapNotNull { record ->
            studentNameMap[record.studentId]?.let { name -> name to record.status }
        }.toMap()
    }

    /**
     * Map holding the current attendance status for each student.
     */
    val attendanceMap = mutableStateMapOf<String, AttendanceStatus>()

    /**
     * Prepares the state for a new frequency session.
     * Resets the current session and initializes attendance for all students.
     */
    fun prepareNewFrequencySession() {
        editingSession = null
        newSessionCalendar = java.util.Calendar.getInstance()
        attendanceMap.clear()

        // Initializes all students as present by default
        studentsForClass.value.forEach { student ->
            attendanceMap[student.studentId] = AttendanceStatus.PRESENT
        }
    }

    /**
     * Prepares the state to edit an existing session.
     * Launches a coroutine to fetch records and updates the observable UI state.
     */
    fun prepareToEditFrequencySession(session: ClassSession) {
        viewModelScope.launch {
            editingSession = session

            newSessionCalendar = java.util.Calendar.getInstance().apply {
                timeInMillis = session.timestamp
            }

            val records = attendanceRepository.getRecordsForSession(session.sessionId)
            val statusMap = records.associate { it.studentId to it.status }

            attendanceMap.clear()
            attendanceMap.putAll(statusMap)
        }
    }

    /**
     * Resets frequency detail state.
     */
    fun clearFrequencyDetails() {
        _frequencyDetails.value = emptyMap()
    }

    /**
     * Deletes a specific class session from local and remote storage.
     */
    fun deleteSession(session: ClassSession, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            attendanceRepository.deleteSession(session)
            loadCourseDetails(session.classId)
            _userMessage.value = "Registro de frequência apagado."
            onComplete()
        }
    }

    /**
     * Updates the date fields for the new/edited session.
     */
    fun updateNewSessionDate(year: Int, month: Int, day: Int) {
        val calendar = newSessionCalendar.clone() as Calendar
        calendar.set(year, month, day)
        newSessionCalendar = calendar
    }

    /**
     * Updates the hour for the new/edited session.
     */
    fun updateNewSessionTime(hour: Int) {
        val calendar = newSessionCalendar.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        newSessionCalendar = calendar
    }

    /**
     * Saves the current attendance state.
     * Syncs data to the repository and resets the frequency UI state.
     */
    fun saveFrequency(classId: String, onComplete: () -> Unit) {
        val currentTimestamp = newSessionCalendar.timeInMillis

        viewModelScope.launch {
            val result = attendanceRepository.saveAttendance(
                classId = classId,
                timestamp = currentTimestamp,
                attendanceMap = attendanceMap.toMap(),
                editingSession = editingSession
            )

            if (result is SaveAttendanceResult.Success) {
                resetFrequencyState()
                onComplete()
            } else if (result is SaveAttendanceResult.Error) {
                _userMessage.value = "Erro ao salvar frequência: ${result.message}"
            }
        }
    }

    /**
     * Resets the internal state related to attendance frequency editing.
     */
    private fun resetFrequencyState() {
        editingSession = null
        attendanceMap.clear()
    }

    // --- Backup and Restore Logic ---

    /**
     * Uploads all local data to cloud storage.
     */
    suspend fun backup() {
        val userId = Firebase.auth.currentUser?.uid ?: run {
            _userMessage.value = "Usuário não logado."
            return
        }
        _userMessage.value = "Iniciando backup..."
        try {
            val backupData = BackupData(
                courses = courseRepository.getAllCourses(),
                students = studentRepository.getAllStudents(),
                classSessions = attendanceRepository.getAllSessions(),
                attendanceRecords = attendanceRepository.getAllRecords(),
                activities = courseRepository.getAllActivities(),
                groupMembers = courseRepository.getAllGroupMembers(),
                skillAssessments = skillRepository.getAllAssessments().first(),
                courseSkills = skillRepository.getAllCourseSkills(),
                activityHighlightedSkills = skillRepository.getAllHighlightedSkills(),
                studentSkills = skillRepository.getAllStudentSkills()
            )
            val result = cloudStorageRepository.uploadBackupData(backupData)
            _userMessage.value = result.message
        } catch (e: Exception) {
            _userMessage.value = "Erro no backup: ${e.message}"
        }
    }

    /**
     * Downloads and restores data from cloud storage.
     */
    suspend fun restore() {
        val userId = Firebase.auth.currentUser?.uid ?: run {
            _userMessage.value = "Usuário não logado."
            return
        }
        _userMessage.value = "Iniciando restauração..."
        try {
            val result = cloudStorageRepository.downloadBackupData()
            if (result.data == null) {
                _userMessage.value = result.message
                return
            }
            val backupData = result.data

            withContext(Dispatchers.IO) {
                db.clearAllTables()
                courseRepository.insertAllCourses(backupData.courses)
                studentRepository.insertAllStudents(backupData.students)
                courseRepository.insertAllActivities(backupData.activities)
                attendanceRepository.insertAllSessions(backupData.classSessions)
                attendanceRepository.insertAllAttendanceRecords(backupData.attendanceRecords)
                courseRepository.insertAllGroupMembers(backupData.groupMembers)
                skillRepository.insertAllAssessments(backupData.skillAssessments)
                skillRepository.insertCourseSkills(backupData.courseSkills)
                skillRepository.insertAllHighlightedSkills(backupData.activityHighlightedSkills)
                skillRepository.insertOrUpdateStudentSkills(backupData.studentSkills)
            }

            loadAllCourses()
            _userMessage.value = "Restauração concluída com sucesso!"
        } catch (e: Exception) {
            _userMessage.value = "Erro na restauração: ${e.message}"
        }
    }

    /**
     * Clears all local data and Firestore documents of the current user.
     */
    fun clearDatabase() {
        viewModelScope.launch {
            try {
                clearFirestoreDatabaseForCurrentUser()

                withContext(Dispatchers.IO) {
                    db.clearAllTables()
                }

                _courses.value = emptyList()
                _selectedCourse.value = null
                _studentsForClass.value = emptyList()
                _generatedGroups.value = emptyList()

                _userMessage.value = "Base de dados limpa com sucesso."

            } catch (e: Exception) {
                _userMessage.value = "Erro ao limpar a base: ${e.message}"
            }
        }
    }

    /**
     * Deletes Firestore collections only for the current user.
     */
    private suspend fun clearFirestoreDatabaseForCurrentUser() {
        val uid = Firebase.auth.currentUser?.uid ?: return

        deleteCollection("users/$uid/courses")
        deleteCollection("users/$uid/students")
        deleteCollection("users/$uid/attendance")
        deleteCollection("users/$uid/skills")
        deleteCollection("users/$uid/activities")
    }

    /**
     * Deletes a collection in batches.
     */
    private suspend fun deleteCollection(collectionPath: String, batchSize: Int = 100) {
        val firestore = Firebase.firestore

        while (true) {
            val snapshot = firestore.collection(collectionPath)
                .limit(batchSize.toLong())
                .get()
                .await()

            if (snapshot.isEmpty) break

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    // --- Course and Student Creation Logic ---

    private var isAddingCourse = false

    /**
     * Inserts a new course and initializes it with default skills.
     */
    /**
     * Inserts a new course and initializes it with default skills.
     */
    fun addCourse(onCourseAdded: () -> Unit) {

        if (isAddingCourse) return

        if (courseName.isNotBlank() && academicYear.isNotBlank() && period.isNotBlank()) {

            val uid = Firebase.auth.currentUser?.uid ?: return

            isAddingCourse = true

            viewModelScope.launch {
                try {

                    val newCourse = Course(
                        classId = java.util.UUID.randomUUID().toString(),
                        className = courseName,
                        academicYear = academicYear,
                        period = period,
                        numberOfClasses = numberOfClasses
                    )

                    val courseId = courseRepository.insertCourse(newCourse, uid)

                    val skills = defaultComputerScienceSkills.map {
                        CourseSkill(
                            courseId = courseId,
                            skillName = it
                        )
                    }

                    skillRepository.insertCourseSkills(skills)

                    courseName = ""
                    academicYear = ""
                    period = ""
                    numberOfClasses = 0

                    loadAllCourses()

                    onCourseAdded()

                } finally {
                    isAddingCourse = false
                }
            }
        }
    }

    /**
     * Adds a single student to the selected course.
     */
    fun addStudent(onStudentsAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        val uid = Firebase.auth.currentUser?.uid ?: run {
            _userMessage.value = "Usuário não logado."
            return
        }

        if (studentName.isNotBlank() && studentNumber.isNotBlank()) {
            viewModelScope.launch {
                val newStudent = Student(
                    studentId = java.util.UUID.randomUUID().toString(),
                    name = studentName,
                    displayName = if (studentDisplayName.isBlank()) studentName else studentDisplayName,
                    studentNumber = studentNumber,
                    classId = classId
                )

                try {
                    studentRepository.insertStudent(newStudent, uid)

                    _userMessage.value = "Aluno adicionado com sucesso."

                    studentName = ""
                    studentDisplayName = ""
                    studentNumber = ""
                } catch (e: Exception) {
                    _userMessage.value = "Erro ao adicionar aluno: ${e.message}"
                }

                loadCourseDetails(classId)
                onStudentsAdded()
            }
        }
    }

    /**
     * Processes and inserts multiple students from a bulk text input.
     * Expected format per line: "studentNumber Full Name"
     * Sets displayName as "First Name + Last Name".
     */
    fun addStudentsInBulk(onStudentsAdded: () -> Unit) {
        val targetClassId = _selectedCourse.value?.classId ?: return
        val currentUserId = Firebase.auth.currentUser?.uid ?: run {
            _userMessage.value = "Usuário não logado."
            return
        }

        if (bulkStudentText.isBlank()) {
            _userMessage.value = "O texto de entrada está vazio."
            return
        }

        viewModelScope.launch {
            try {
                val studentsToInsert = bulkStudentText.lineSequence()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        // Splits by the first whitespace: [0] = Number, [1] = Full Name
                        val lineParts = line.trim().split(Regex("\\s+"), limit = 2)

                        if (lineParts.size == 2) {
                            val number = lineParts[0]
                            val fullName = lineParts[1]

                            // Split the name to extract first and last parts
                            val nameSegments = fullName.split(Regex("\\s+")).filter { it.isNotBlank() }

                            val formattedDisplayName = when {
                                nameSegments.size >= 2 -> "${nameSegments.first()} ${nameSegments.last()}"
                                nameSegments.isNotEmpty() -> nameSegments.first()
                                else -> fullName
                            }

                            Student(
                                studentId = java.util.UUID.randomUUID().toString(),
                                name = fullName,
                                displayName = formattedDisplayName,
                                studentNumber = number,
                                classId = targetClassId
                            )
                        } else null
                    }
                    .toList()

                if (studentsToInsert.isEmpty()) {
                    _userMessage.value = "Nenhum aluno válido encontrado. Use: Nº Nome"
                    return@launch
                }

                // Persist each student to the repository
                for (student in studentsToInsert) {
                    studentRepository.insertStudent(student, currentUserId)
                }

                _userMessage.value = "${studentsToInsert.size} aluno(s) adicionado(s) com sucesso."
                bulkStudentText = ""

                loadCourseDetails(targetClassId)
                onStudentsAdded()

            } catch (e: Exception) {
                _userMessage.value = "Erro ao processar inserção: ${e.message}"
            }
        }
    }

    // --- Manual Grouping Management ---
    var isManualMode by mutableStateOf(false)
        private set

    var manualGroups = mutableStateListOf<Group>()
    var unassignedStudents = mutableStateListOf<Student>()

    private var nextManualGroupId = 1

    /**
     * Synchronizes the manual editor state with the currently generated groups.
     */
    fun enterManualMode(forceRefresh: Boolean = false) {
        if (isManualMode && !forceRefresh) return

        manualGroups.clear()
        unassignedStudents.clear()

        val allStudents = _studentsForClass.value
        val currentGroups = _generatedGroups.value
        val assignedStudentIds = mutableSetOf<String>()

        currentGroups.forEachIndexed { index, groupStudents ->
            if (groupStudents.isNotEmpty()) {
                manualGroups.add(
                    Group(
                        id = index + 1,
                        students = groupStudents.toMutableStateList()
                    )
                )
                groupStudents.forEach { assignedStudentIds.add(it.studentId) }
            }
        }

        allStudents
            .filterNot { it.studentId in assignedStudentIds }
            .forEach { unassignedStudents.add(it) }

        nextManualGroupId = (manualGroups.maxOfOrNull { it.id } ?: 0) + 1

        isManualMode = true
    }

    /**
     * Disables manual grouping mode.
     */
    fun exitManualMode() {
        isManualMode = false
        manualGroups.clear()
        unassignedStudents.clear()
    }

    /**
     * Orchestrates student movement between pools and groups.
     */
    fun moveStudent(
        student: Student,
        from: Location,
        to: DropTarget
    ) {
        if (from is Location.Group && to is DropTarget.ExistingGroup && from.groupId == to.groupId) {
            return
        }

        val targetGroup = if (to is DropTarget.ExistingGroup) {
            manualGroups.firstOrNull { it.id == to.groupId }
        } else null

        if (to is DropTarget.ExistingGroup && targetGroup == null) return

        when (to) {
            is DropTarget.ExistingGroup -> targetGroup!!.students.add(student)
            DropTarget.NewGroup -> manualGroups.add(
                Group(id = generateManualGroupId(), students = mutableStateListOf(student))
            )

            DropTarget.Unassigned -> {}
        }

        when (from) {
            Location.Unassigned -> unassignedStudents.remove(student)
            is Location.Group -> {
                val sourceGroup = manualGroups.firstOrNull { it.id == from.groupId }
                sourceGroup?.students?.remove(student)
            }
        }

        if (to is DropTarget.Unassigned) unassignedStudents.add(student)

        unassignedStudents.sortBy { it.displayName.lowercase() }
        manualGroups.forEach { it.students.sortBy { s -> s.displayName.lowercase() } }

        manualGroups.removeAll { it.students.isEmpty() }
        commitManualGroups()
    }

    /**
     * Generates a unique identifier for manual groups.
     */
    private fun generateManualGroupId(): Int {
        return nextManualGroupId++
    }

    /**
     * Persists the manual group configuration to the repository.
     */
    private fun commitManualGroups() {
        val groups = manualGroups.map { it.students.toList() }
        _generatedGroups.value = groups

        viewModelScope.launch {
            courseRepository.persistGroups(
                activityId = _loadedActivityId.value ?: return@launch,
                groups = groups
            )
        }
    }


}