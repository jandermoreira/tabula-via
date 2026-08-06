/**
 * ViewModel for the Class management.
 * Orchestrates UI state and business logic for classes, students, attendance, and skills.
 */

package edu.jm.tabulavia.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.AcademicClass
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.ActivityHighlightedSkill
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassBackup
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.model.ClassSkill
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.GroupMember
import edu.jm.tabulavia.model.SkillAssessment
import edu.jm.tabulavia.model.SkillAssessmentsSummary
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.SkillStatus
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.StudentDashboardItem
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.repository.AttendanceRepository
import edu.jm.tabulavia.repository.ClassRepository
import edu.jm.tabulavia.repository.CloudStorageRepository
import edu.jm.tabulavia.repository.EvidenceRepository
import edu.jm.tabulavia.repository.SaveAttendanceResult
import edu.jm.tabulavia.repository.SkillRepository
import edu.jm.tabulavia.repository.StudentRepository
import edu.jm.tabulavia.logic.MonitoringCalculator
import edu.jm.tabulavia.model.EvidenceHistoryItem
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentMonitoringSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * Represents a student's attendance status for display.
 */
data class AttendanceDetail(
    val studentName: String, val status: AttendanceStatus
)

/**
 * ViewModel for handling class-related operations and state management.
 *
 * The ClassViewModel class is responsible for managing the logic and data flow for classes,
 * including classes, students, activities, attendance, skill assessments, and associated repositories.
 * It provides a reactive data layer to the UI and ensures synchronization with local and remote data sources.
 * This ViewModel includes CRUD operations, state resets, synchronization, and utility methods
 * tailored to class and class session management.
 */
class ClassViewModel(application: Application) : BaseAndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)

    private val classRepository = ClassRepository(
        context = application.applicationContext,
        classDao = db.classDao(),
        studentDao = db.studentDao(),
        activityDao = db.activityDao(),
        groupMemberDao = db.groupMemberDao(),
        firestore = Firebase.firestore
    )
    private val attendanceRepository = AttendanceRepository(
        attendanceDao = db.attendanceDao(),
        applicationContext = application.applicationContext
    )
    private val studentRepository = StudentRepository(
        studentDao = db.studentDao(),
        firestore = Firebase.firestore,
        attendanceRepository = attendanceRepository,
        applicationContext = application.applicationContext
    )
    private val skillRepository = SkillRepository(
        classSkillDao = db.classSkillDao(),
        skillAssessmentDao = db.skillAssessmentDao(),
        firestore = Firebase.firestore,
        applicationContext = application.applicationContext
    )

    private val cloudStorageRepository = CloudStorageRepository(
        storage = Firebase.storage, auth = Firebase.auth
    )

    private val evidenceRepository = EvidenceRepository(
        firestore = Firebase.firestore,
        evidenceDao = db.evidenceDao()
    )

    // --- UI State Streams ---
    private val _selectedClass = MutableStateFlow<AcademicClass?>(null)
    val selectedClass: StateFlow<AcademicClass?> = _selectedClass.asStateFlow()

    private val _selectedActivity = MutableStateFlow<Activity?>(null)
    val selectedActivity: StateFlow<Activity?> = _selectedActivity.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _studentsForClass = _selectedClass.flatMapLatest { academicClass ->
        if (academicClass != null) {
            studentRepository.getStudentsForClass(academicClass.classId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val studentsForClass: StateFlow<List<Student>> = _studentsForClass

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _classSessions = _selectedClass.flatMapLatest { academicClass ->
        if (academicClass != null) {
            attendanceRepository.getClassSessionsFlow(academicClass.classId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val classSessions: StateFlow<List<ClassSession>> = _classSessions

    @OptIn(ExperimentalCoroutinesApi::class)
    val dashboardItems: StateFlow<List<StudentDashboardItem>> =
        _selectedClass.flatMapLatest { academicClass ->
            if (academicClass != null) {
                combine(
                    studentRepository.getStudentsForClass(academicClass.classId),
                    evidenceRepository.getEvidences(academicClass.classId),
                    evidenceRepository.getAllScoresByClass(academicClass.classId),
                    attendanceRepository.getClassSessionsFlow(academicClass.classId),
                    attendanceRepository.getAttendanceRecordsForClassFlow(academicClass.classId)
                ) { students, evidences, allScores, sessions, allAttendance ->
                    val scoresByStudent = allScores.groupBy { it.studentId }
                    val attendanceByStudent = allAttendance.groupBy { it.studentId }
                    
                    students.map { student ->
                        val studentScores = scoresByStudent[student.studentId] ?: emptyList()
                        val summary = MonitoringCalculator.calculate(
                            student = student,
                            classId = academicClass.classId,
                            evidences = evidences,
                            scores = studentScores,
                            sessions = sessions,
                            attendance = attendanceByStudent[student.studentId] ?: emptyList()
                        )
                        val evidenceHistory = studentScores.mapNotNull { score ->
                            evidences.find { it.evidenceId == score.evidenceId }?.let { evidence ->
                                EvidenceHistoryItem(
                                    evidenceName = evidence.name,
                                    deadline = evidence.deadline,
                                    score = score.score
                                )
                            }
                        }.sortedBy { it.deadline }

                        StudentDashboardItem(
                            student = student,
                            summary = summary,
                            evidenceHistory = evidenceHistory
                        )
                    }.sortedWith(
                        compareByDescending<StudentDashboardItem> { it.summary.state.ordinal }
                            .thenBy { it.student.effectiveName }
                    )
                }
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _attendanceDetails = MutableStateFlow<List<AttendanceDetail>>(emptyList())
    val attendanceDetails: StateFlow<List<AttendanceDetail>> = _attendanceDetails.asStateFlow()

    private val _selectedStudentDetails = MutableStateFlow<Student?>(null)
    val selectedStudentDetails: StateFlow<Student?> = _selectedStudentDetails.asStateFlow()

    private val _studentAttendancePercentage = MutableStateFlow<Float?>(null)
    val studentAttendancePercentage: StateFlow<Float?> = _studentAttendancePercentage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _activities = _selectedClass.flatMapLatest { academicClass ->
        if (academicClass != null) {
            classRepository.getActivitiesForClass(academicClass.classId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val activities: StateFlow<List<Activity>> = _activities

    private val _skillAssessmentLog = MutableStateFlow<List<SkillAssessment>>(emptyList())
    val skillAssessmentLog: StateFlow<List<SkillAssessment>> = _skillAssessmentLog.asStateFlow()

    private val _studentSkillSummaries =
        MutableStateFlow<Map<String, SkillAssessmentsSummary>>(emptyMap())
    val studentSkillSummaries: StateFlow<Map<String, SkillAssessmentsSummary>> =
        _studentSkillSummaries.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _classSkills = _selectedClass.flatMapLatest { academicClass ->
        if (academicClass != null) {
            skillRepository.getSkillsFlowForClass(academicClass.classId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val classSkills: StateFlow<List<ClassSkill>> = _classSkills

    private val _generatedGroups = MutableStateFlow<List<List<Student>>>(emptyList())
    val generatedGroups: StateFlow<List<List<Student>>> = _generatedGroups.asStateFlow()

    private val _currentSessionAttendance =
        MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSessionAttendance: StateFlow<Map<String, AttendanceStatus>> =
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
    var name by mutableStateOf("")
    var academicYear by mutableStateOf("")
    var period by mutableStateOf("")
    var numberOfSessions by mutableIntStateOf(15)
    var studentName by mutableStateOf("")
    var studentDisplayName by mutableStateOf("")
    var studentNumber by mutableStateOf("")
    var rawStudentListData by mutableStateOf("")
    var newSessionCalendar by mutableStateOf(Calendar.getInstance())
    var editingSession by mutableStateOf<ClassSession?>(null)
    var activityName by mutableStateOf("")
    var activityType by mutableStateOf("Individual")
    var activityHighlightedSkills by mutableStateOf<Set<String>>(emptySet())
    var skillName by mutableStateOf("")

    var groupingCriterion by mutableStateOf("Aleatório")
    var groupFormationType by mutableStateOf("Número de grupos")
    var groupFormationValue by mutableStateOf("")

    private val _isInitialSyncing = MutableStateFlow(false)
    val isInitialSyncing: StateFlow<Boolean> = _isInitialSyncing.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var syncJob: Job? = null
    private var syncActivityJob: Job? = null
    private var lastSyncedEmail: String? = null

    /**
     * Activates the sync indicator for 2 seconds to notify real-time activity.
     * Cancels any existing sync job to ensure the pulse duration is reset on new activity.
     */
    private fun notifySyncActivity() {
        syncActivityJob?.cancel()
        syncActivityJob = viewModelScope.launch {
            _isSyncing.value = true
            try {
                kotlinx.coroutines.delay(2000)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val email = auth.currentUser?.email
        if (email != null) {
            startInitialSync(email)
        } else {
            lastSyncedEmail = null
            classRepository.stopClassesSync()
            studentRepository.stopStudentsSync()
            classRepository.stopActivitiesSync()
            attendanceRepository.stopAttendanceSync()
            skillRepository.stopAllListeners()
            syncJob?.cancel()
            _isInitialSyncing.value = false
        }
    }

    // --- Loading and Clearing Logic ---

    val classes: StateFlow<List<AcademicClass>> = classRepository.getAllClassesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Firebase.auth.addAuthStateListener(authStateListener)
    }

    /**
     * Cleans up all resources when the ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        Firebase.auth.removeAuthStateListener(authStateListener)
        classRepository.stopClassesSync()
        studentRepository.stopStudentsSync()
        classRepository.stopActivitiesSync()
        attendanceRepository.stopAttendanceSync()
        skillRepository.stopAllListeners()
    }

    /**
     * Starts the initial data synchronization from Firestore.
     */
    private fun startInitialSync(email: String) {
        if (lastSyncedEmail == email) return
        lastSyncedEmail = email

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _isInitialSyncing.value = true
            try {
                classRepository.syncClassesFromCloud(email)

                val allClasses = classRepository.getAllClasses()
                allClasses.forEach { academicClass ->
                    val classId = academicClass.classId
                    val studentsJob =
                        launch { studentRepository.syncStudentsFromCloud(email, classId) }
                    val activitiesJob =
                        launch { classRepository.syncActivitiesFromCloud(email, classId) }
                    val sessionsJob =
                        launch { attendanceRepository.syncSessionsFromCloud(email, classId) }
                    val skillsJob = launch { skillRepository.syncSkillsFromCloud(email, classId) }

                    studentsJob.join()
                    activitiesJob.join()
                    sessionsJob.join()
                    skillsJob.join()
                }

                classRepository.startClassesSync(email, onSyncActivity = { notifySyncActivity() })
            } catch (e: Exception) {
                Log.e("ClassViewModel", "Initial sync failed: ${e.message}")
            } finally {
                _isInitialSyncing.value = false
            }
        }
    }

    /**
     * Refreshes all class data by pulling information from the cloud provider.
     */
    fun refreshAllData(email: String) {
        viewModelScope.launch {
            try {
                classRepository.syncClassesFromCloud(email)
            } catch (e: Exception) {
                showMessage("Falha na sincronização: ${e.message}")
            }
        }
    }

    /**
     * Fetches details for a specific class including students, sessions, and activities.
     * Initiates real-time synchronization and observes database changes via Flow.
     */
    fun loadClassDetails(classId: String) {
        if (currentClassId == classId && _selectedClass.value != null) {
            return
        }

        resetClassState()

        // Store the current class ID to manage listener cleanup later
        currentClassId = classId

        // Start real-time sync for all related data
        Firebase.auth.currentUser?.email?.let { email ->
            studentRepository.startStudentsSync(
                email,
                classId,
                onSyncActivity = { notifySyncActivity() })
            classRepository.startActivitiesSync(
                email,
                classId,
                onSyncActivity = { notifySyncActivity() })
            attendanceRepository.startAttendanceSync(
                classId,
                onSyncActivity = { notifySyncActivity() })
            skillRepository.startClassSkillsSync(
                email,
                classId,
                onSyncActivity = { notifySyncActivity() })
            evidenceRepository.startEvidencesSync(
                email,
                classId,
                onSyncActivity = { notifySyncActivity() })
        }

        // Launch coroutine to load the selected class.
        // Reactive flows (students, activities, skills, sessions) will automatically
        // react to the change in _selectedClass.
        viewModelScope.launch {
            _selectedClass.value = classRepository.getClassById(classId)
        }
    }

    /**
     * Resets the UI state for the current class and stops all active Firestore listeners.
     */
    fun resetClassState() {
        // Stop listeners
        studentRepository.stopStudentsSync()
        classRepository.stopActivitiesSync()
        attendanceRepository.stopAttendanceSync()
        evidenceRepository.stopEvidencesSync()
        currentClassId?.let { skillRepository.stopListeningToClassSkills(it) }
        currentClassId = null

        _selectedClass.value = null
        editingSession = null
        _studentSkillStatuses.value = emptyList()

        // Ensure sync indicator is reset
        syncActivityJob?.cancel()
        _isSyncing.value = false
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
            name = studentName, displayName = studentDisplayName, studentNumber = studentNumber
        )
        val email = Firebase.auth.currentUser?.email ?: return
        viewModelScope.launch {
            studentRepository.insertStudent(updatedStudent, email)
            loadClassDetails(studentToUpdate.classId)
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

            attendanceRepository.countStudentAbsencesFlow(studentId).collect { absences ->
                val totalClasses = _selectedClass.value?.numberOfSessions ?: 0

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
     * Fetches skills associated with a class.
     */
    fun loadSkillsForClass(classId: String) {
        // Redundant with reactive classSkills flow
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
     * Adds a new skill to the current class.
     */
    fun addClassSkill(onSkillAdded: () -> Unit) {
        val email = com.google.firebase.Firebase.auth.currentUser?.email ?: return
        val classId = _selectedClass.value?.classId ?: return

        if (skillName.isNotBlank()) {
            viewModelScope.launch {
                val newSkill = ClassSkill(
                    classId = classId,
                    skillName = skillName,
                    firestoreId = java.util.UUID.randomUUID().toString()
                )

                skillRepository.insertClassSkills(email, classId, listOf(newSkill))

                skillName = ""
                onSkillAdded()
            }
        }
    }

    /**
     * Removes a skill from the class.
     */
    fun deleteClassSkill(skill: ClassSkill) {
        val email = Firebase.auth.currentUser?.email ?: return
        val classId = _selectedClass.value?.classId ?: return

        viewModelScope.launch {
            skillRepository.deleteClassSkill(email, classId, skill)
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
    fun recordProfessorObservations(
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
            val activity = classRepository.getActivityById(activityId)
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
            classRepository.getGroupMembers(activityId)
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
     * Creates a new activity for the current class.
     * Generates a persistent UUID and synchronizes with Firestore using the user's email.
     */
    fun addActivity(onActivityAdded: () -> Unit) {
        val classId = _selectedClass.value?.classId ?: return
        val email = com.google.firebase.Firebase.auth.currentUser?.email ?: return

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

                classRepository.insertActivity(newActivity, email)

                val highlightedSkills = activityHighlightedSkills.sorted().map { skillName ->
                    ActivityHighlightedSkill(
                        activityId = newActivityId, skillName = skillName
                    )
                }

//                skillRepository.updateActivityHighlightedSkills(newActivityId, highlightedSkills)

                activityName = ""
                activityType = "Grupo"
                activityHighlightedSkills = emptySet()

                onActivityAdded()
                showMessage("Atividade '$savedTitle' adicionada.")
            }
        }
    }

    /**
     * Fetches all activities for a class.
     */
    fun loadActivitiesForClass(classId: String) {
        // Redundant with reactive activities flow
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
                _studentsForClass.value.filter { _currentSessionAttendance.value[it.studentId] != AttendanceStatus.ABSENT }
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
            classRepository.persistGroups(activityId, groups)
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
     * Identifies and loads attendance records for the most recent session today.
     */
    private fun loadTodaysAttendance(sessions: List<ClassSession>) {
        viewModelScope.launch {
            val lastSessionToday = attendanceRepository.getLastSessionToday(sessions)

            _currentSessionAttendance.value = if (lastSessionToday != null) {
                val records = attendanceRepository.getRecordsForSession(lastSessionToday.sessionId)
                records.associate { it.studentId to it.status }
            } else {
                emptyMap()
            }
        }
    }

    /**
     * Loads the attendance records for a given session and maps them to student names.
     *
     * @param session The class session to load details for.
     */
    fun loadAttendanceDetails(session: ClassSession) {
        viewModelScope.launch {
            val records = attendanceRepository.getRecordsForSession(session.sessionId)

            val currentStudents = studentsForClass.value
            val studentMap = currentStudents.associateBy { it.studentId }

            _attendanceDetails.value = records.mapNotNull { record ->
                studentMap[record.studentId]?.let { student ->
                    AttendanceDetail(
                        studentName = student.effectiveName, status = record.status
                    )
                }
            }.sortedBy { it.studentName }
        }
    }

    /**
     * Resets the current attendance details state.
     */
    fun clearFrequencyDetails() {
        _attendanceDetails.value = emptyList()
    }

    /**
     * Map holding the current attendance status for each student.
     */
    val attendanceMap = mutableStateMapOf<String, AttendanceStatus>()

    private var isSavingAttendance by mutableStateOf(false)
    private var attendanceErrorMessage by mutableStateOf<String?>(null)

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
     * Prepares a new session by resetting the session state, clearing attendance records,
     * initializing the session timestamp, and marking all students as present by default.
     */
    fun prepareNewSession() {
        stopAttendanceObservation()
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

    private var attendanceObservationJob: Job? = null

    private fun stopAttendanceObservation() {
        attendanceObservationJob?.cancel()
        attendanceObservationJob = null
    }

    /**
     * Prepares the state to edit an existing session.
     * Launches a coroutine to fetch records and updates the observable UI state.
     */
    fun prepareToEditFrequencySession(session: ClassSession) {
        stopAttendanceObservation()
        editingSession = session
        newSessionCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = session.timestamp
        }

        attendanceObservationJob = viewModelScope.launch {
            attendanceRepository.observeRecordsForSession(session.sessionId).collect { records ->
                val statusMap = records.associate { it.studentId to it.status }

                // Synchronize existing records into the map
                statusMap.forEach { (studentId, status) ->
                    attendanceMap[studentId] = status
                }

                // Ensure all current students in class have an entry
                studentsForClass.value.forEach { student ->
                    if (!attendanceMap.containsKey(student.studentId)) {
                        attendanceMap[student.studentId] = AttendanceStatus.PRESENT
                    }
                }
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

                loadClassDetails(session.classId)
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
     * only students currently assigned to the class are included, preventing
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
                    stopAttendanceObservation()
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

    /**
     * Generates a formatted string for the attendance report.
     * Includes student number and attendance percentage separated by a tab.
     */
    suspend fun generateAttendanceReport(): String = withContext(Dispatchers.IO) {
        val currentClass = _selectedClass.value ?: return@withContext "Turma não encontrada."
        val totalClasses = currentClass.numberOfSessions

        if (totalClasses <= 0) {
            return@withContext "O número total de aulas deve ser configurado para calcular a frequência."
        }

        val students = _studentsForClass.value.sortedBy { it.studentNumber }
        val reportBuilder = StringBuilder()

        for (student in students) {
            val absences = attendanceRepository.countStudentAbsences(student.studentId)
            val presenceCount = (totalClasses - absences).coerceAtLeast(0).toFloat()
            val percentage = ((presenceCount / totalClasses.toFloat()) * 100f).toInt()
            reportBuilder.append("${student.studentNumber}\t$percentage\n")
        }

        return@withContext reportBuilder.toString().trim()
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
//                classes = classRepository.getAllClasses(),
//                students = studentRepository.getAllStudents(),
//                classSessions = attendanceRepository.getAllSessions(),
//                attendanceRecords = attendanceRepository.getAllRecords(),
//                activities = classRepository.getAllActivities(),
//                groupMembers = classRepository.getAllGroupMembers(),
////                skillAssessments = skillRepository.getAllAssessments().first(),
////                classSkills = skillRepository.getAllClassSkills(),
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
//                classRepository.insertAllClasses(backupData.classes)
//                studentRepository.insertAllStudents(backupData.students, userId)
//                classRepository.insertAllActivities(backupData.activities)
//                attendanceRepository.insertAllSessions(backupData.classSessions)
//                attendanceRepository.insertAllAttendanceRecords(backupData.attendanceRecords)
//                classRepository.insertAllGroupMembers(backupData.groupMembers)
////                skillRepository.insertAllAssessments(backupData.skillAssessments)
////                skillRepository.insertClassSkills(backupData.classSkills)
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
     * This is a destructive operation intended for development and total reset.
     */
    fun clearDatabase() {
        viewModelScope.launch {
            try {
                // Step 1: Silence repositories to prevent reactive re-insertion during cleanup
                studentRepository.stopStudentsSync()
                // attendanceRepository.stopAttendanceSync() // Ensure this is implemented in your repository

                // Step 2: Cancel all background sync operations
                WorkManager.getInstance(getApplication()).cancelAllWork()

                // Step 3: Comprehensive remote cleanup
                clearFirestoreDatabaseForCurrentUser()

                // Step 4: Atomic local cleanup
                withContext(Dispatchers.IO) {
                    db.clearAllTables()
                }

                // Step 5: Reset UI and memory state
                _selectedClass.value = null
                _generatedGroups.value = emptyList()

                // Clear additional state flows if necessary
                _loadedActivityId.value = null

                showMessage("Base de dados limpa com sucesso.")
            } catch (e: Exception) {
                showMessage("Erro ao limpar a base: ${e.message}")
            }
        }
    }

    /**
     * Deletes Firestore collections only for the current user, including nested subcollections.
     * Iterates through classes to ensure orphans are not left behind.
     */
    private suspend fun clearFirestoreDatabaseForCurrentUser() {
        val email = Firebase.auth.currentUser?.email ?: return
        val firestore = Firebase.firestore

        // Delete independent top-level collections
        val userCollections = listOf("attendance", "skills", "activities")
        userCollections.forEach { collection ->
            deleteCollection("users/$email/$collection")
        }

        // Process classes and their nested subcollections (students, sessions, etc.)
        val classesRef = firestore.collection("users/$email/classes")
        val classesSnapshot = classesRef.get().await()

        for (classDoc in classesSnapshot.documents) {
            val classId = classDoc.id
            // Explicitly clear subcollections that Firestore won't delete automatically
            deleteCollection("users/$email/classes/$classId/students")
            // deleteCollection("users/$email/classes/$classId/sessions")

            // Delete the class document itself
            classDoc.reference.delete().await()
        }
    }

    /**
     * Deletes all documents within a given collection path using a WriteBatch.
     *
     * @param collectionPath The full path to the Firestore collection.
     */
    private suspend fun deleteCollection(collectionPath: String) {
        val firestore = Firebase.firestore
        val snapshot = firestore.collection(collectionPath).get().await()

        if (snapshot.isEmpty) return

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    // --- Class and Student Creation Logic ---

    private var isAddingClass = false

    /**
     * Creates a new class and populates it with default skills.
     * Generates unique identifiers for both the class and its skills to ensure offline stability.
     */
    fun addClass(onClassAdded: () -> Unit) {
        if (isAddingClass) return

        if (name.isNotBlank() && academicYear.isNotBlank() && period.isNotBlank()) {
            val email = Firebase.auth.currentUser?.email ?: return
            isAddingClass = true

            viewModelScope.launch {
                try {
                    val generatedClassId = java.util.UUID.randomUUID().toString()

                    val newClass = AcademicClass(
                        classId = generatedClassId,
                        name = name,
                        academicYear = academicYear,
                        period = period,
                        numberOfSessions = numberOfSessions
                    )

                    classRepository.insertClass(newClass, email)

                    val skills = defaultComputerScienceSkills.map { skillName ->
                        ClassSkill(
                            classId = generatedClassId,
                            skillName = skillName,
                            firestoreId = java.util.UUID.randomUUID().toString()
                        )
                    }

                    skillRepository.insertClassSkills(email, generatedClassId, skills)

                    loadClassDetails(generatedClassId)

                    resetClassForm()
                    onClassAdded()
                } finally {
                    isAddingClass = false
                }
            }
        }
    }

    /**
     * Resets the input fields for class creation.
     */
    private fun resetClassForm() {
        name = ""
        academicYear = ""
        period = ""
        numberOfSessions = 0
    }

    /**
     * Adds a single student to the selected class, avoiding duplicates by studentNumber.
     */
    fun addStudent(onStudentsAdded: () -> Unit) {
        val classId = _selectedClass.value?.classId ?: return
        val email = Firebase.auth.currentUser?.email ?: run {
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
                    studentRepository.insertStudent(newStudent, email)

                    showMessage("Aluno adicionado com sucesso.")

                    studentName = ""
                    studentDisplayName = ""
                    studentNumber = ""
                } catch (e: Exception) {
                    showMessage("Erro ao adicionar aluno: ${e.message}")
                }

                loadClassDetails(classId)
                onStudentsAdded()
            }
        }
    }

    /**
     * Removes a student from the repository and updates the local UI state.
     *
     * This function handles the deletion process by calling the repository,
     * removing the student from the local attendance map, and refreshing
     * the class details to ensure the UI reflects the current database state.
     */
    fun deleteStudent(student: Student) {
        val email = Firebase.auth.currentUser?.email ?: return

        viewModelScope.launch {
            try {
                studentRepository.deleteStudent(student, email)
                attendanceMap.remove(student.studentId)

                loadClassDetails(student.classId)

                showMessage("Aluno ${student.displayName} removido.")
            } catch (e: Exception) {
                showMessage("Erro ao remover aluno: ${e.message}")
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
        val targetClassId = _selectedClass.value?.classId ?: return
        val email = Firebase.auth.currentUser?.email ?: run {
            showMessage("Usuário não logado.")
            return
        }

        if (rawStudentListData.isBlank()) {
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

                rawStudentListData.lineSequence().filter { it.isNotBlank() }.forEach { line ->
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

                val totalIgnored = (rawStudentListData.lineSequence().filter { it.isNotBlank() }
                    .count() - studentsToInsert.size)

                studentRepository.insertAllStudents(studentsToInsert, email)

                val message = if (totalIgnored > 0) {
                    "${studentsToInsert.size} aluno(s) adicionado(s). $totalIgnored linha(s) ignorada(s) (duplicatas)."
                } else {
                    "${studentsToInsert.size} aluno(s) adicionado(s) com sucesso."
                }
                showMessage(message)

                rawStudentListData = ""
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

    private var groupCounter = 1

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

        groupCounter = (manualGroups.maxOfOrNull { it.id } ?: 0) + 1

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
        manualGroups.forEach { it.students.sortBy { s -> s.effectiveName.lowercase() } }

        manualGroups.removeAll { it.students.isEmpty() }
        commitManualGroups()
    }

    /**
     * Generates a unique identifier for manual groups.
     */
    private fun generateManualGroupId(): Int {
        return groupCounter++
    }

    /**
     * Persists the manual group configuration to the repository.
     */
    private fun commitManualGroups() {
        val groups = manualGroups.map { it.students.toList() }
        _generatedGroups.value = groups

        viewModelScope.launch {
            classRepository.persistGroups(
                activityId = _loadedActivityId.value ?: return@launch, groups = groups
            )
        }
    }

    /**
     * Collects and serializes all data for a specific class into a JSON string.
     * This operation is performed locally using the Room database.
     */
    fun exportClassBackup(academicClass: AcademicClass, onBackupReady: (String) -> Unit) {
        val classId = academicClass.classId

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Collect students directly from DAO for maximum reliability
                val students = db.studentDao().getStudentsForClassList(classId)

                // Collect sessions and their records
                val sessions = db.attendanceDao().getClassSessionsForClass(classId)
                val allRecords = mutableListOf<AttendanceRecord>()
                sessions.forEach { session ->
                    allRecords.addAll(
                        db.attendanceDao().getAttendanceRecordsForSession(session.sessionId)
                    )
                }

                // Collect activities and their group members
                val activities = db.activityDao().getActivitiesForClassList(classId)
                val groupMembers = mutableListOf<GroupMember>()
                activities.forEach { activity ->
                    groupMembers.addAll(
                        db.groupMemberDao().getGroupMembersForActivityList(activity.activityId)
                    )
                }

                // Collect highlighted skills for these activities
                val highlightedSkills =
                    db.activityHighlightedSkillDao().getHighlightedSkillsForClass(classId)

                // Collect skills and assessments
                val skills = db.classSkillDao().getSkillsForClass(classId)
                val assessments = mutableListOf<SkillAssessment>()
                val studentSkills = db.skillDao().getSkillsForClass(classId)

                students.forEach { student ->
                    assessments.addAll(
                        db.skillAssessmentDao().getAssessmentsForStudentList(student.studentId)
                    )
                }

                val backup = ClassBackup(
                    clazz = academicClass,
                    students = students,
                    sessions = sessions,
                    attendance = allRecords,
                    activities = activities,
                    groupMembers = groupMembers,
                    skills = skills,
                    highlightedSkills = highlightedSkills,
                    assessments = assessments,
                    studentSkills = studentSkills
                )

                // Serialize using Kotlinx Serialization
                val json = kotlinx.serialization.json.Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }

                val jsonString = try {
                    json.encodeToString(ClassBackup.serializer(), backup)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showMessage("Erro na serialização: ${e.message}")
                    }
                    ""
                }

                if (jsonString.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        onBackupReady(jsonString)
                        showMessage("Backup de ${academicClass.name} gerado com sucesso!")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showMessage("Falha ao gerar conteúdo do backup.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("Erro ao gerar backup: ${e.message}")
                }
            }
        }
    }

    /**
     * Restores an academic class and its associated data from a JSON backup string and syncs it with Firestore.
     *
     * @param jsonString The raw JSON string containing class backup data.
     * @param customName Optional custom name to override the restored class name.
     */
    fun importClassBackup(jsonString: String, customName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = com.google.firebase.Firebase.auth.currentUser
                val email: String = user?.email?.lowercase() ?: run {
                    Log.e(
                        "ClassViewModel",
                        "Remote synchronization aborted: user email is null or blank."
                    )
                    withContext(Dispatchers.Main) {
                        showMessage("Erro na importação: Usuário não autenticado.")
                    }
                    return@launch
                }

                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val backup = json.decodeFromString(ClassBackup.serializer(), jsonString)

                // 1. Prepare Data and Mappings
                val newClassId = UUID.randomUUID().toString()
                val restoredClass = backup.clazz.copy(
                    classId = newClassId,
                    name = customName ?: "${backup.clazz.name} (Importado)"
                )

                val studentIdMap = mutableMapOf<String, String>()
                val restoredStudents = backup.students.map { student ->
                    val newId = UUID.randomUUID().toString()
                    studentIdMap[student.studentId] = newId
                    student.copy(studentId = newId, classId = newClassId)
                }

                val sessionIdMap = mutableMapOf<String, String>()
                val restoredSessions = backup.sessions.map { session ->
                    val newId = UUID.randomUUID().toString()
                    sessionIdMap[session.sessionId] = newId
                    session.copy(sessionId = newId, classId = newClassId)
                }

                val activityIdMap = mutableMapOf<String, String>()
                val restoredActivities = backup.activities.map { activity ->
                    val newId = UUID.randomUUID().toString()
                    activityIdMap[activity.activityId] = newId
                    activity.copy(activityId = newId, classId = newClassId)
                }

                // 2. Consolidated Local Database Transaction
                db.withTransaction {
                    // 2.1 Class Insertion
                    db.classDao().insertClass(restoredClass)

                    // 2.2 Students Insertion
                    if (restoredStudents.isNotEmpty()) {
                        db.studentDao().insertAll(restoredStudents)
                    }

                    // 2.3 Sessions and Attendance Mapping and Insertion
                    if (restoredSessions.isNotEmpty()) {
                        db.attendanceDao().insertAllSessions(restoredSessions)

                        val allRestoredRecords = mutableListOf<AttendanceRecord>()
                        backup.sessions.forEachIndexed { index, oldSession ->
                            val newSessionId = restoredSessions[index].sessionId
                            val recordsForThisSession =
                                backup.attendance.filter { record -> record.sessionId == oldSession.sessionId }

                            recordsForThisSession.forEach { record ->
                                val newStudentId = studentIdMap[record.studentId]
                                if (newStudentId != null) {
                                    allRestoredRecords.add(
                                        AttendanceRecord(
                                            sessionId = newSessionId,
                                            studentId = newStudentId,
                                            status = record.status
                                        )
                                    )
                                }
                            }
                        }

                        if (allRestoredRecords.isNotEmpty()) {
                            db.attendanceDao().insertAttendanceRecords(allRestoredRecords)
                        }
                    }

                    // 2.4 Activities and Group Members
                    if (restoredActivities.isNotEmpty()) {
                        db.activityDao().insertAll(restoredActivities)
                    }

                    val restoredMembers = backup.groupMembers.mapNotNull { member ->
                        val newActivityId = activityIdMap[member.activityId]
                        val newStudentId = studentIdMap[member.studentId]
                        if (newActivityId != null && newStudentId != null) {
                            member.copy(activityId = newActivityId, studentId = newStudentId)
                        } else null
                    }

                    if (restoredMembers.isNotEmpty()) {
                        db.groupMemberDao().insertAll(restoredMembers)
                    }

                    // 2.5 Skills and Assessments
                    val restoredSkills = backup.skills.map { skill ->
                        skill.copy(classId = newClassId, firestoreId = UUID.randomUUID().toString())
                    }
                    if (restoredSkills.isNotEmpty()) {
                        db.classSkillDao().insertClassSkills(restoredSkills)
                    }

                    val restoredHighlightedSkills =
                        backup.highlightedSkills.mapNotNull { highlightedSkill ->
                            activityIdMap[highlightedSkill.activityId]?.let { newActivityId ->
                                highlightedSkill.copy(
                                    activityId = newActivityId,
                                    firestoreId = UUID.randomUUID().toString()
                                )
                            }
                        }
                    if (restoredHighlightedSkills.isNotEmpty()) {
                        db.activityHighlightedSkillDao().insertAll(restoredHighlightedSkills)
                    }

                    val restoredAssessments = backup.assessments.mapNotNull { assessment ->
                        studentIdMap[assessment.studentId]?.let { newStudentId ->
                            assessment.copy(
                                id = 0,
                                studentId = newStudentId,
                                firestoreId = UUID.randomUUID().toString()
                            )
                        }
                    }
                    if (restoredAssessments.isNotEmpty()) {
                        db.skillAssessmentDao().insertAll(restoredAssessments)
                    }

                    val restoredStudentSkills = backup.studentSkills.mapNotNull { studentSkill ->
                        studentIdMap[studentSkill.studentId]?.let { newStudentId ->
                            studentSkill.copy(
                                studentId = newStudentId,
                                firestoreId = UUID.randomUUID().toString()
                            )
                        }
                    }
                    if (restoredStudentSkills.isNotEmpty()) {
                        db.skillDao().insertOrUpdateSkills(restoredStudentSkills)
                    }
                }

                // 3. Cloud Synchronization
                val userClassesRef =
                    Firebase.firestore.collection("users").document(email).collection("classes")

                // 3.1 Sync Class
                userClassesRef.document(newClassId).set(restoredClass).await()

                // 3.2 Sync Students Subcollection
                if (restoredStudents.isNotEmpty()) {
                    val studentBatch = Firebase.firestore.batch()
                    val studentsRef = userClassesRef.document(newClassId).collection("students")
                    restoredStudents.forEach { student ->
                        val studentMap = mapOf(
                            "studentId" to student.studentId,
                            "name" to student.name,
                            "displayName" to student.displayName,
                            "studentNumber" to student.studentNumber,
                            "classId" to student.classId,
                            "status" to student.status.name
                        )
                        studentBatch.set(studentsRef.document(student.studentId), studentMap)
                    }
                    studentBatch.commit().await()
                }

                // 3.3 Sync Activities Subcollection
                if (restoredActivities.isNotEmpty()) {
                    val activityBatch = Firebase.firestore.batch()
                    val activitiesRef = userClassesRef.document(newClassId).collection("activities")
                    restoredActivities.forEach { activity ->
                        activityBatch.set(activitiesRef.document(activity.activityId), activity)
                    }
                    activityBatch.commit().await()
                }

                // 3.4 Sync Sessions and Attendance Subcollection
                if (restoredSessions.isNotEmpty()) {
                    val sessionBatch = Firebase.firestore.batch()
                    val sessionsRef = userClassesRef.document(newClassId).collection("sessions")

                    restoredSessions.forEachIndexed { index, session ->
                        val oldSessionId = backup.sessions[index].sessionId
                        val records =
                            backup.attendance.filter { record -> record.sessionId == oldSessionId }
                        val attendanceMap = records.mapNotNull { record ->
                            studentIdMap[record.studentId]?.let { newStudentId ->
                                newStudentId to record.status.name
                            }
                        }.toMap()

                        val firestoreSession = mapOf(
                            "sessionId" to session.sessionId,
                            "classId" to newClassId,
                            "timestamp" to session.timestamp,
                            "attendance" to attendanceMap
                        )

                        sessionBatch.set(sessionsRef.document(session.sessionId), firestoreSession)
                    }
                    sessionBatch.commit().await()
                }

                // 3.5 Sync Class Skills Subcollection
                val restoredSkills = backup.skills.map { skill ->
                    skill.copy(classId = newClassId, firestoreId = UUID.randomUUID().toString())
                }
                if (restoredSkills.isNotEmpty()) {
                    val skillBatch = Firebase.firestore.batch()
                    val skillsRef = userClassesRef.document(newClassId).collection("skills")
                    restoredSkills.forEach { skill ->
                        val docId = skill.firestoreId ?: UUID.randomUUID().toString()
                        skillBatch.set(skillsRef.document(docId), skill)
                    }
                    skillBatch.commit().await()
                }

                // 3.6 Sync Highlighted Skills Subcollection
                val restoredHighlightedSkills =
                    backup.highlightedSkills.mapNotNull { highlightedSkill ->
                        activityIdMap[highlightedSkill.activityId]?.let { newActivityId ->
                            highlightedSkill.copy(
                                activityId = newActivityId,
                                firestoreId = UUID.randomUUID().toString()
                            )
                        }
                    }
                if (restoredHighlightedSkills.isNotEmpty()) {
                    val highlightedSkillBatch = Firebase.firestore.batch()
                    val highlightedSkillsRef =
                        userClassesRef.document(newClassId).collection("highlightedSkills")
                    restoredHighlightedSkills.forEach { highlightedSkill ->
                        val docId = highlightedSkill.firestoreId ?: UUID.randomUUID().toString()
                        highlightedSkillBatch.set(
                            highlightedSkillsRef.document(docId),
                            highlightedSkill
                        )
                    }
                    highlightedSkillBatch.commit().await()
                }

                // 3.7 Sync Skill Assessments Subcollection
                val restoredAssessments = backup.assessments.mapNotNull { assessment ->
                    studentIdMap[assessment.studentId]?.let { newStudentId ->
                        assessment.copy(
                            id = 0,
                            studentId = newStudentId,
                            firestoreId = UUID.randomUUID().toString()
                        )
                    }
                }
                if (restoredAssessments.isNotEmpty()) {
                    val assessmentBatch = Firebase.firestore.batch()
                    val assessmentsRef =
                        userClassesRef.document(newClassId).collection("assessments")
                    restoredAssessments.forEach { assessment ->
                        val docId = assessment.firestoreId ?: UUID.randomUUID().toString()
                        assessmentBatch.set(assessmentsRef.document(docId), assessment)
                    }
                    assessmentBatch.commit().await()
                }

                // 3.8 Sync Student Skills Subcollection
                val restoredStudentSkills = backup.studentSkills.mapNotNull { studentSkill ->
                    studentIdMap[studentSkill.studentId]?.let { newStudentId ->
                        studentSkill.copy(
                            studentId = newStudentId,
                            firestoreId = UUID.randomUUID().toString()
                        )
                    }
                }
                if (restoredStudentSkills.isNotEmpty()) {
                    val studentSkillBatch = Firebase.firestore.batch()
                    val studentSkillsRef =
                        userClassesRef.document(newClassId).collection("studentSkills")
                    restoredStudentSkills.forEach { studentSkill ->
                        val docId = studentSkill.firestoreId ?: UUID.randomUUID().toString()
                        studentSkillBatch.set(studentSkillsRef.document(docId), studentSkill)
                    }
                    studentSkillBatch.commit().await()
                }

                withContext(Dispatchers.Main) {
                    val activitiesCount = restoredActivities.size
                    val studentsCount = restoredStudents.size
                    val summary =
                        "Importado e sincronizado: $studentsCount alunos, $activitiesCount atividades."
                    showMessage(summary)
                    refreshAllData(email)
                }
            } catch (e: Exception) {
                Log.e("ClassViewModel", "Error executing importClassBackup", e)
                withContext(Dispatchers.Main) {
                    showMessage("Erro na importação: ${e.localizedMessage}")
                }
            }
        }
    }
}