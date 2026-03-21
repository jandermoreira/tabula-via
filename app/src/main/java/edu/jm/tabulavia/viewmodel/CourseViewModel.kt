/**
 * ViewModel for the Course management.
 * Orchestrates UI state and business logic for courses, students, attendance, and skills.
 */
package edu.jm.tabulavia.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.*
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.repository.*
import edu.jm.tabulavia.repository.AttendanceRepository
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Represents a student's attendance status for display.
 */
data class AttendanceDetail(
    val studentName: String,
    val status: AttendanceStatus
)

/**
 * ViewModel for handling course-related data and operations.
 *
 * This class manages the state and logic for courses, students, skills, activities,
 * attendance, and other course-related functionalities. It interacts with repositories
 * to fetch and persist data, coordinates UI state updates, and maintains observable states
 * for real-time synchronization and data changes.
 */
class CourseViewModel(application: Application) : BaseAndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)

    private val courseRepository = CourseRepository(
        context = application.applicationContext,
        courseDao = db.courseDao(),
        studentDao = db.studentDao(),
        activityDao = db.activityDao(),
        groupMemberDao = db.groupMemberDao(),
        firestore = Firebase.firestore
    )
    private val attendanceRepository = AttendanceRepository(
        attendanceDao = db.attendanceDao()
    )
    private val studentRepository = StudentRepository(
        studentDao = db.studentDao(),
        firestore = Firebase.firestore,
        attendanceRepository = attendanceRepository
    )
    private val skillRepository = SkillRepository(
        courseSkillDao = db.courseSkillDao(), firestore = Firebase.firestore, scope = viewModelScope
    )

    private val cloudStorageRepository = CloudStorageRepository(
        storage = Firebase.storage, auth = Firebase.auth
    )

    // --- UI State Streams ---
    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()

    private val _selectedActivity = MutableStateFlow<Activity?>(null)
    val selectedActivity: StateFlow<Activity?> = _selectedActivity.asStateFlow()

    private val _studentsForClass = MutableStateFlow<List<Student>>(emptyList())
    val studentsForClass: StateFlow<List<Student>> = _studentsForClass.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _classSessions = _selectedCourse.flatMapLatest { course ->
        if (course != null) {
            attendanceRepository.getClassSessionsFlow(course.classId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val classSessions: StateFlow<List<ClassSession>> = _classSessions

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _frequencyDetails = MutableStateFlow<List<AttendanceDetail>>(emptyList())
    val frequencyDetails: StateFlow<List<AttendanceDetail>> = _frequencyDetails.asStateFlow()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val todaysAttendance: StateFlow<Map<String, AttendanceStatus>> =
        _classSessions.flatMapLatest { sessions ->
            val lastSessionToday = attendanceRepository.getLastSessionToday(sessions)
            if (lastSessionToday != null) {
                attendanceRepository.getAttendanceRecordsFlow(lastSessionToday.sessionId)
                    .map { records -> records.associate { it.studentId to it.status } }
            } else {
                kotlinx.coroutines.flow.flowOf(emptyMap())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

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

    val courses: StateFlow<List<Course>> = courseRepository.getAllCoursesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Starts real-time synchronization when the ViewModel is initialized
        Firebase.auth.currentUser?.uid?.let { uid ->
            courseRepository.startCoursesSync(uid)
        }
    }

    /**
     * Cleans up all resources when the ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        courseRepository.stopCoursesSync()
        courseRepository.stopStudentsSync()
        courseRepository.stopActivitiesSync()
        attendanceRepository.stopAttendanceSync()
        skillRepository.stopAllListeners()
    }

    /**
     * Refreshes all course data by pulling information from the cloud provider.
     */
    fun refreshAllData(uid: String) {
        viewModelScope.launch {
            try {
                courseRepository.syncCoursesFromCloud(uid)
            } catch (e: Exception) {
                showMessage("Sync failed: ${e.message}")
            }
        }
    }

    /**
     * Fetches details for a specific class including students, sessions, and activities.
     * Initiates real-time synchronization and observes database changes via Flow.
     */
    fun loadCourseDetails(classId: String) {
        // Clear previous course details and stop any active listeners
        clearCourseDetails()

        // Store the current class ID to manage listener cleanup later
        currentClassId = classId

        // Start real-time sync for all related data
        Firebase.auth.currentUser?.uid?.let { uid ->
            courseRepository.startStudentsSync(uid, classId)
            courseRepository.startActivitiesSync(uid, classId)
            attendanceRepository.startAttendanceSync(classId)
            skillRepository.startListeningToCourseSkills(uid, classId) // <- new
        }

        // Launch coroutines to collect Flows from Room (they will emit initial data and updates)
        viewModelScope.launch {
            // Get the selected course (one-shot)
            _selectedCourse.value = courseRepository.getCourseById(classId)

            // Collect students (real-time)
            studentRepository.getStudentsForClass(classId).collect { studentsList ->
                _studentsForClass.value = studentsList
            }
        }

        viewModelScope.launch {
            // Collect activities (real-time)
            courseRepository.getActivitiesForClass(classId).collect { activitiesList ->
                _activities.value = activitiesList
            }
        }

        viewModelScope.launch {
            // Collect course skills (real-time via the new Flow from SkillRepository)
            skillRepository.getSkillsFlowForCourse(classId).collect { skillsList ->
                _courseSkills.value = skillsList
            }
        }
    }

    /**
     * Resets the UI state for the current course and stops all active Firestore listeners.
     */
    fun clearCourseDetails() {
        currentClassId?.let { skillRepository.stopListeningToCourseSkills(it) }
        currentClassId = null

        _selectedCourse.value = null
        _studentsForClass.value = emptyList()
        _activities.value = emptyList()
        editingSession = null
        _courseSkills.value = emptyList()
        _studentSkillStatuses.value = emptyList()
    }

//    /**
//     * Clears the user notification message state.
//     */
//    fun onUserMessageShown() {
//        _userMessage.value = null
//    }

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
            name = studentName, displayName = studentDisplayName, studentNumber = studentNumber
        )
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            studentRepository.insertStudent(updatedStudent, uid)
            loadCourseDetails(studentToUpdate.classId)
            onDismiss()
        }
    }

    /**
     * Loads comprehensive data for a student, calculating attendance percentage.
     * Truncates decimals by converting to Int before updating the Float StateFlow.
     */
    fun loadStudentDetails(studentId: String) {
        viewModelScope.launch {
            _selectedStudentDetails.value = studentRepository.getStudentById(studentId)

            val classId = _selectedCourse.value?.classId ?: return@launch
            val courseSkills = skillRepository.getSkillsForCourse(classId)
            _courseSkills.value = courseSkills

//            val statuses = skillRepository.calculateStudentSkillStatuses(studentId, courseSkills)
//            _studentSkillStatuses.value = statuses

            attendanceRepository.countStudentAbsencesFlow(studentId).collect { absences ->
                val totalClasses = _selectedCourse.value?.numberOfClasses ?: 0

                if (totalClasses > 0) {
                    val presenceCount = (totalClasses - absences).toFloat()
                    val exactPercentage = (presenceCount / totalClasses.toFloat()) * 100f

                    _studentAttendancePercentage.value = exactPercentage.toInt().toFloat()
                } else {
                    _studentAttendancePercentage.value = null
                }
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
//        viewModelScope.launch {
//            val currentLog = skillRepository.getAllAssessments().first()
//            _skillAssessmentLog.value = currentLog
//        }
    }

    /**
     * Adds a new skill to the current course.
     */
    fun addCourseSkill(onSkillAdded: () -> Unit) {
        val uid = com.google.firebase.Firebase.auth.currentUser?.uid ?: return
        val courseId = _selectedCourse.value?.classId ?: return

        if (skillName.isNotBlank()) {
            viewModelScope.launch {
                val newSkill = CourseSkill(
                    courseId = courseId,
                    skillName = skillName,
                    firestoreId = java.util.UUID.randomUUID().toString()
                )

                skillRepository.insertCourseSkills(uid, courseId, listOf(newSkill))

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
        val uid = Firebase.auth.currentUser?.uid ?: return
        val classId = _selectedCourse.value?.classId ?: return

        viewModelScope.launch {
            skillRepository.deleteCourseSkill(uid, classId, skill)
            loadSkillsForCourse(classId)
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
//        viewModelScope.launch {
//            val assessment = SkillAssessment(
//                studentId = studentId,
//                skillName = skillName,
//                level = level,
//                source = source,
//                assessorId = assessorId,
//                timestamp = timestamp ?: System.currentTimeMillis()
//            )
//            skillRepository.insertAssessment(assessment)
//            loadStudentDetails(studentId)
//        }
    }

    /**
     * Records multiple professor observations for a student.
     */
    fun addProfessorSkillAssessments(
        studentId: String, assessments: List<Pair<String, SkillLevel>>
    ) {
//        viewModelScope.launch {
//            val newAssessments = assessments.map { (skillName, level) ->
//                SkillAssessment(
//                    studentId = studentId,
//                    skillName = skillName,
//                    level = level,
//                    source = AssessmentSource.PROFESSOR_OBSERVATION,
//                    assessorId = null
//                )
//            }
//            skillRepository.insertAllAssessments(newAssessments)
//            loadStudentDetails(studentId)
//        }
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
     * Helper to observe and load saved groups from the database.
     * Combines group members and students flows to maintain a reactive UI state.
     */
    private fun loadPersistedGroups(activityId: String, classId: String) {
        viewModelScope.launch {
            // Combines both flows to reactively reconstruct the groups list
            courseRepository.getGroupMembers(activityId)
                .combine(studentRepository.getStudentsForClass(classId)) { members, students ->
                    if (members.isNotEmpty()) {
                        val studentMap = students.associateBy { it.studentId }

                        members.groupBy { it.groupNumber }.toSortedMap().values.map { groupList ->
                            groupList.mapNotNull { member -> studentMap[member.studentId] }
                        }
                    } else {
                        emptyList()
                    }
                }.collect { groups ->
                    _generatedGroups.value = groups
                }
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

                val highlightedSkills = activityHighlightedSkills.sorted().map { skillName ->
                    ActivityHighlightedSkill(
                        activityId = newActivityId, skillName = skillName
                    )
                }

//                skillRepository.updateActivityHighlightedSkills(newActivityId, highlightedSkills)

                activityName = ""
                activityType = "Grupo"
                activityHighlightedSkills = emptySet()

                loadActivitiesForClass(classId)
                onActivityAdded()
                showMessage("Atividade '$savedTitle' adicionada.")
            }
        }
    }

    /**
     * Fetches all activities for a class.
     */
    fun loadActivitiesForClass(classId: String) {
        viewModelScope.launch {
            courseRepository.getActivitiesForClass(classId).collect { activitiesList ->
                _activities.value = activitiesList
            }
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
                showMessage("Por favor, insira um valor válido.")
                return@launch
            }

            val presentStudents =
                _studentsForClass.value.filter { _todaysAttendance.value[it.studentId] != AttendanceStatus.ABSENT }
                    .shuffled()

            if (presentStudents.isEmpty()) {
                showMessage("Nenhum aluno presente para formar grupos.")
                return@launch
            }

            val numGroups = if (groupFormationType == "Número de grupos") {
                if (value > presentStudents.size) {
                    showMessage("O número de grupos não pode ser maior que o de alunos presentes.")
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
    /**
     * Loads the attendance records for a given session and maps them to student names.
     * * @param session The class session to load details for.
     */
    fun loadFrequencyDetails(session: ClassSession) {
        viewModelScope.launch {
            val records = attendanceRepository.getRecordsForSession(session.sessionId)

            val currentStudents = studentsForClass.value
            val studentMap = currentStudents.associateBy { it.studentId }

            _frequencyDetails.value = records.mapNotNull { record ->
                studentMap[record.studentId]?.let { student ->
                    AttendanceDetail(
                        studentName = student.effectiveName,
                        status = record.status
                    )
                }
            }.sortedBy { it.studentName }
        }
    }

    /**
     * Resets the current attendance details state.
     */
    fun clearFrequencyDetails() {
        _frequencyDetails.value = emptyList()
    }

    /**
     * Map holding the current attendance status for each student.
     */
    val attendanceMap = mutableStateMapOf<String, AttendanceStatus>()

    private var isSavingAttendance by mutableStateOf(false)
    private var attendanceErrorMessage by mutableStateOf<String?>(null)

    /**
     * The identifier for the currently loaded class within the `CourseViewModel`.
     */
    private var currentClassId: String? = null

    /**
     * Helper: Sets the default session time based on specific hour windows.
     */
    private fun getRoundedSessionHour(currentHour: Int): Int {
        return when {
            currentHour in 0 until 10 -> 8
            currentHour in 10 until 12 -> 10
            currentHour in 12 until 16 -> 14
            currentHour in 16 until 19 -> 16
            currentHour in 19 until 21 -> 19
            else -> 21
        }
    }

    /**
     * Prepares the state for a new frequency session with specialized time rounding.
     */
    fun prepareNewFrequencySession() {
        editingSession = null
        attendanceMap.clear()

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        // Apply the custom hour logic
        now.set(Calendar.HOUR_OF_DAY, getRoundedSessionHour(currentHour))
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        newSessionCalendar = now

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

            studentsForClass.value.forEach { student ->
                attendanceMap[student.studentId] =
                    statusMap[student.studentId] ?: AttendanceStatus.PRESENT
            }
        }
    }

    /**
     * Deletes a specific class session from local and remote storage.
     */
    fun deleteSession(session: ClassSession, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                attendanceRepository.deleteSession(session)

                // Clear editing state if the deleted session was being edited
                if (editingSession?.sessionId == session.sessionId) {
                    editingSession = null
                    attendanceMap.clear()
                }

                loadCourseDetails(session.classId)
                showMessage("Registro de frequência apagado.")
                onComplete()
            } catch (e: Exception) {
                showMessage("Erro ao apagar registro: ${e.message}")
            }
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
     * Persists the current attendance state to the repository.
     *
     * This function performs a defensive filtering of the [attendanceMap] to ensure
     * only students currently assigned to the course are included, preventing
     * foreign key constraint violations during database insertion.
     */
    fun saveAttendance(classId: String, onSelectionConfirmed: () -> Unit) {
        if (isSavingAttendance) return

        isSavingAttendance = true
        attendanceErrorMessage = null

        viewModelScope.launch {
            val validStudentIds = _studentsForClass.value.map { it.studentId }.toSet()
            val filteredAttendanceMap = attendanceMap.filterKeys { it in validStudentIds }

            val result = attendanceRepository.saveAttendance(
                classId = classId,
                timestamp = newSessionCalendar.timeInMillis,
                attendanceMap = filteredAttendanceMap,
                editingSession = editingSession
            )

            when (result) {
                is SaveAttendanceResult.Success -> {
                    resetFrequencyState()
                    onSelectionConfirmed()
                }

                is SaveAttendanceResult.Error -> {
                    showMessage(result.message)
                }
            }
            isSavingAttendance = false
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
        showMessage("Backup não implementado.")
//        val userId = Firebase.auth.currentUser?.uid ?: run {
//            showMessage("Usuário não logado.")
//            return
//        }
//        showMessage("Iniciando backup...")
//        try {
//            val backupData = BackupData(
//                courses = courseRepository.getAllCourses(),
//                students = studentRepository.getAllStudents(),
//                classSessions = attendanceRepository.getAllSessions(),
//                attendanceRecords = attendanceRepository.getAllRecords(),
//                activities = courseRepository.getAllActivities(),
//                groupMembers = courseRepository.getAllGroupMembers(),
////                skillAssessments = skillRepository.getAllAssessments().first(),
////                courseSkills = skillRepository.getAllCourseSkills(),
////                activityHighlightedSkills = skillRepository.getAllHighlightedSkills(),
////                studentSkills = skillRepository.getAllStudentSkills()
//            )
//            val result = cloudStorageRepository.uploadBackupData(backupData)
//            showMessage(result.message)
//        } catch (e: Exception) {
//            showMessage("Erro no backup: ${e.message}")
//        }
    }

    /**
     * Downloads and restores data from cloud storage.
     */
    suspend fun restore() {
        showMessage("Restauração não implementada.")
//        val userId = Firebase.auth.currentUser?.uid ?: run {
//            showMessage("Usuário não logado.")
//            return
//        }
//        showMessage("Iniciando restauração...")
//        try {
//            val result = cloudStorageRepository.downloadBackupData()
//            if (result.data == null) {
//                showMessage(result.message)
//                return
//            }
//            val backupData = result.data
//
//            withContext(Dispatchers.IO) {
//                db.clearAllTables()
//                courseRepository.insertAllCourses(backupData.courses)
//                studentRepository.insertAllStudents(backupData.students, userId)
//                courseRepository.insertAllActivities(backupData.activities)
//                attendanceRepository.insertAllSessions(backupData.classSessions)
//                attendanceRepository.insertAllAttendanceRecords(backupData.attendanceRecords)
//                courseRepository.insertAllGroupMembers(backupData.groupMembers)
////                skillRepository.insertAllAssessments(backupData.skillAssessments)
////                skillRepository.insertCourseSkills(backupData.courseSkills)
////                skillRepository.insertAllHighlightedSkills(backupData.activityHighlightedSkills)
////                skillRepository.insertOrUpdateStudentSkills(backupData.studentSkills)
//            }
//
//            showMessage("Restauração concluída com sucesso!")
//        } catch (e: Exception) {
//            showMessage("Erro na restauração: ${e.message}")
//        }
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

                _selectedCourse.value = null
                _studentsForClass.value = emptyList()
                _generatedGroups.value = emptyList()

                showMessage("Base de dados limpa com sucesso.")
            } catch (e: Exception) {
                showMessage("Erro ao limpar a base: ${e.message}")
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
            val snapshot =
                firestore.collection(collectionPath).limit(batchSize.toLong()).get().await()

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
     * Creates a new course and populates it with default skills.
     * Generates unique identifiers for both the course and its skills to ensure offline stability.
     */
    fun addCourse(onCourseAdded: () -> Unit) {
        if (isAddingCourse) return

        if (courseName.isNotBlank() && academicYear.isNotBlank() && period.isNotBlank()) {
            val uid = Firebase.auth.currentUser?.uid ?: return
            isAddingCourse = true

            viewModelScope.launch {
                try {
                    val generatedCourseId = java.util.UUID.randomUUID().toString()

                    val newCourse = Course(
                        classId = generatedCourseId,
                        className = courseName,
                        academicYear = academicYear,
                        period = period,
                        numberOfClasses = numberOfClasses
                    )

                    courseRepository.insertCourse(newCourse, uid)

                    val skills = defaultComputerScienceSkills.map { skillName ->
                        CourseSkill(
                            courseId = generatedCourseId,
                            skillName = skillName,
                            firestoreId = java.util.UUID.randomUUID().toString()
                        )
                    }

                    skillRepository.insertCourseSkills(uid, generatedCourseId, skills)

                    loadCourseDetails(generatedCourseId)

                    resetCourseForm()
                    onCourseAdded()
                } finally {
                    isAddingCourse = false
                }
            }
        }
    }

    /**
     * Resets the input fields for course creation.
     */
    private fun resetCourseForm() {
        courseName = ""
        academicYear = ""
        period = ""
        numberOfClasses = 0
    }

    /**
     * Adds a single student to the selected course, avoiding duplicates by studentNumber.
     */
    fun addStudent(onStudentsAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        val uid = Firebase.auth.currentUser?.uid ?: run {
            showMessage("Usuário não logado.")
            return
        }

        if (studentName.isNotBlank() && studentNumber.isNotBlank()) {
            viewModelScope.launch {
                val exists = studentRepository.studentExistsInClass(studentNumber, classId)
                if (exists) {
                    showMessage("Já existe um aluno com o número $studentNumber nesta turma.")
                    return@launch
                }

                val newStudent = Student(
                    studentId = java.util.UUID.randomUUID().toString(),
                    name = studentName.trim(),
                    displayName = studentDisplayName.trim(),
                    studentNumber = studentNumber.trim(),
                    classId = classId
                )

                try {
                    studentRepository.insertStudent(newStudent, uid)

                    showMessage("Aluno adicionado com sucesso.")

                    studentName = ""
                    studentDisplayName = ""
                    studentNumber = ""
                } catch (e: Exception) {
                    showMessage("Erro ao adicionar aluno: ${e.message}")
                }

                loadCourseDetails(classId)
                onStudentsAdded()
            }
        }
    }

    /**
     * Removes a student from the repository and updates the local UI state.
     *
     * This function handles the deletion process by calling the repository,
     * removing the student from the local attendance map, and refreshing
     * the course details to ensure the UI reflects the current database state.
     */
    fun deleteStudent(student: Student) {
        val uid = Firebase.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                studentRepository.deleteStudent(student, uid)
                attendanceMap.remove(student.studentId)

                loadCourseDetails(student.classId)

                showMessage("Student ${student.displayName} removed.")
            } catch (e: Exception) {
                showMessage("Error removing student: ${e.message}")
            }
        }
    }

    /**
     * Adds students in bulk to a specified class based on the provided input.
     * Parses a list of student data from a bulk text input, validates and processes it,
     * and adds it to the class if the data is valid and non-duplicate.
     *
     * @param onStudentsAdded A callback function that is invoked after students are successfully added.
     */
    fun addStudentsInBulk(onStudentsAdded: () -> Unit) {
        val targetClassId = _selectedCourse.value?.classId ?: return
        val currentUserId = Firebase.auth.currentUser?.uid ?: run {
            showMessage("Usuário não logado.")
            return
        }

        if (bulkStudentText.isBlank()) {
            showMessage("O texto de entrada está vazio.")
            return
        }

        viewModelScope.launch {
            try {
                val existingNumbers =
                    studentRepository.getExistingStudentNumbersForClass(targetClassId).toSet()

                val processedNumbers = mutableSetOf<String>()
                val studentsToInsert = mutableListOf<Student>()
                var duplicateInBatchCount = 0

                bulkStudentText.lineSequence()
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        val lineParts = line.trim().split(Regex("\\s+"), limit = 2)
                        if (lineParts.size == 2) {
                            val number = lineParts[0]
                            val fullName = lineParts[1]

                            when {
                                number in existingNumbers -> { /* ignores */
                                }

                                number in processedNumbers -> {
                                    duplicateInBatchCount++
                                }

                                else -> {
                                    val nameSegments =
                                        fullName.split(Regex("\\s+")).filter { it.isNotBlank() }

                                    val formattedDisplayName = when {
                                        nameSegments.size <= 2 -> ""
                                        nameSegments.size > 2 -> "${nameSegments.first()} ${nameSegments.last()}"
                                        else -> fullName
                                    }

                                    studentsToInsert.add(
                                        Student(
                                            studentId = java.util.UUID.randomUUID().toString(),
                                            name = fullName.trim(),
                                            displayName = formattedDisplayName.trim(),
                                            studentNumber = number.trim(),
                                            classId = targetClassId
                                        )
                                    )
                                    processedNumbers.add(number)
                                }
                            }
                        }
                    }

                if (studentsToInsert.isEmpty()) {
                    showMessage("Nenhuma inserção: todos já existem ou formato é inválido.")
                    return@launch
                }

                val totalIgnored = (bulkStudentText.lineSequence().filter { it.isNotBlank() }
                    .count() - studentsToInsert.size)

                studentRepository.insertAllStudents(studentsToInsert, currentUserId)

                val message = if (totalIgnored > 0) {
                    "${studentsToInsert.size} aluno(s) adicionado(s). $totalIgnored linha(s) ignorada(s) (duplicatas)."
                } else {
                    "${studentsToInsert.size} aluno(s) adicionado(s) com sucesso."
                }
                showMessage(message)

                bulkStudentText = ""
                onStudentsAdded()

            } catch (e: Exception) {
                showMessage("Erro ao processar inserção: ${e.message}")
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
                        id = index + 1, students = groupStudents.toMutableStateList()
                    )
                )
                groupStudents.forEach { assignedStudentIds.add(it.studentId) }
            }
        }

        allStudents.filterNot { it.studentId in assignedStudentIds }
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
        student: Student, from: Location, to: DropTarget
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
                activityId = _loadedActivityId.value ?: return@launch, groups = groups
            )
        }
    }
}