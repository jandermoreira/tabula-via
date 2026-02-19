package edu.jm.tabulavia.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.toMutableStateList
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.*
import edu.jm.tabulavia.utils.SkillTrendCalculator
import edu.jm.tabulavia.utils.TrendCalculationMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Calendar
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.model.grouping.DropTarget

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val courseDao = db.courseDao()
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()
    private val activityDao = db.activityDao()
    private val skillDao = db.skillDao() // This DAO might become obsolete or need refactoring
    private val groupMemberDao = db.groupMemberDao()
    private val courseSkillDao = db.courseSkillDao()
    private val skillAssessmentDao = db.skillAssessmentDao() // New DAO
    private val activityHighlightedSkillDao = db.activityHighlightedSkillDao()

    private val storage = Firebase.storage
    private val auth = Firebase.auth

    // --- UI State ---
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

    private val _todaysAttendance = MutableStateFlow<Map<Long, AttendanceStatus>>(emptyMap())
    val todaysAttendance: StateFlow<Map<Long, AttendanceStatus>> = _todaysAttendance.asStateFlow()

    private val _selectedGroupDetails = MutableStateFlow<List<Student>>(emptyList())
    val selectedGroupDetails: StateFlow<List<Student>> = _selectedGroupDetails.asStateFlow()

    // --- NOVO: StateFlow para os status de habilidades calculados ---
    private val _studentSkillStatuses = MutableStateFlow<List<SkillStatus>>(emptyList())
    val studentSkillStatuses: StateFlow<List<SkillStatus>> = _studentSkillStatuses.asStateFlow()

    // --- Form State ---
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

    init {
        loadAllCourses()
    }

    // --- LOADING AND CLEARING LOGIC ---
    private fun loadAllCourses() {
        viewModelScope.launch { _courses.value = courseDao.getAllCourses() }
    }

    fun loadCourseDetails(classId: Long) {
        viewModelScope.launch {
            _selectedCourse.value = courseDao.getCourseById(classId)
            _studentsForClass.value = studentDao.getStudentsForClass(classId)
            val allSessions = attendanceDao.getClassSessionsForClass(classId)
            _classSessions.value = allSessions
            _activities.value = activityDao.getActivitiesForClass(classId)
            _courseSkills.value = courseSkillDao.getSkillsForCourse(classId)

            // Load today\'s attendance
            loadTodaysAttendance(allSessions)
        }
    }

    private fun loadTodaysAttendance(sessions: List<ClassSession>) {
        viewModelScope.launch {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val lastSessionToday = sessions
                .filter { it.timestamp in todayStart..todayEnd }
                .maxByOrNull { it.timestamp }

            if (lastSessionToday != null) {
                val records =
                    attendanceDao.getAttendanceRecordsForSession(lastSessionToday.sessionId)
                _todaysAttendance.value = records.associate { it.studentId to it.status }
            } else {
                _todaysAttendance.value = emptyMap()
            }
        }
    }

    fun clearCourseDetails() {
        _selectedCourse.value = null
        _studentsForClass.value = emptyList()
        _classSessions.value = emptyList()
        _activities.value = emptyList()
        editingSession = null
        _todaysAttendance.value = emptyMap()
        _courseSkills.value = emptyList()
        _studentSkillStatuses.value = emptyList() // Limpar também os status de habilidades
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    // --- STUDENT LOGIC ---
    fun selectStudentForEditing(student: Student) {
        studentName = student.name
        studentDisplayName = student.displayName
        studentNumber = student.studentNumber
        _selectedStudentDetails.value = student
    }

    fun updateStudent(onDismiss: () -> Unit) {
        val studentToUpdate = _selectedStudentDetails.value ?: return
        val updatedStudent = studentToUpdate.copy(
            name = studentName,
            displayName = studentDisplayName,
            studentNumber = studentNumber
        )
        viewModelScope.launch {
            studentDao.updateStudent(updatedStudent)
            loadCourseDetails(studentToUpdate.classId)
            onDismiss()
        }
    }

    fun loadStudentDetails(studentId: Long) {
        viewModelScope.launch {
            _selectedStudentDetails.value = studentDao.getStudentById(studentId)

            val classId = _selectedCourse.value?.classId ?: return@launch
            val courseSkills = courseSkillDao.getSkillsForCourse(classId)
            _courseSkills.value = courseSkills

            calculateStudentSkillStatus(studentId)

            val totalClasses = _selectedCourse.value?.numberOfClasses ?: 0
            if (totalClasses > 0) {
                val absences = attendanceDao.countStudentAbsences(studentId)
                _studentAttendancePercentage.value =
                    ((totalClasses.toFloat() - absences.toFloat()) / totalClasses.toFloat()) * 100
            } else {
                _studentAttendancePercentage.value = null
            }
        }
    }

    fun clearStudentDetails() {
        _selectedStudentDetails.value = null
        _studentAttendancePercentage.value = null
        _studentSkillSummaries.value = emptyMap()
        _studentSkillStatuses.value = emptyList()
    }

    // --- SKILL LOGIC ---
    private val defaultSkills = listOf(
        "Participação", "Comunicação", "Escuta", "Organização",
        "Técnica", "Colaboração", "Reflexão"
    )

    fun loadSkillsForCourse(courseId: Long) {
        viewModelScope.launch {
            _courseSkills.value = courseSkillDao.getSkillsForCourse(courseId)
        }
    }

    fun loadSkillAssessmentLog() {
        viewModelScope.launch {
            _skillAssessmentLog.value = skillAssessmentDao.getAllAssessments().first()
        }
    }

    fun addCourseSkill(onSkillAdded: () -> Unit) {
        val courseId = _selectedCourse.value?.classId ?: return
        if (skillName.isNotBlank()) {
            viewModelScope.launch {
                courseSkillDao.insertCourseSkills(listOf(CourseSkill(courseId, skillName)))
                skillName = ""
                loadSkillsForCourse(courseId)
                onSkillAdded()
            }
        }
    }

    fun deleteCourseSkill(skill: CourseSkill) {
        viewModelScope.launch {
            courseSkillDao.deleteCourseSkill(skill)
            loadSkillsForCourse(skill.courseId)
        }
    }

    fun addSkillAssessment(
        studentId: Long,
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
            skillAssessmentDao.insert(assessment)
            loadStudentDetails(studentId)
        }
    }

    fun addProfessorSkillAssessments(studentId: Long, assessments: List<Pair<String, SkillLevel>>) {
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
            skillAssessmentDao.insertAll(newAssessments)
            loadStudentDetails(studentId) // Reload to update UI
        }
    }

    // --- NOVO: Lógica para calcular o status das habilidades do aluno ---
    private suspend fun calculateStudentSkillStatus(studentId: Long, historyCount: Int = 3) {
        Log.d(
            "CourseViewModel",
            "Iniciando cálculo de status de habilidades para studentId: $studentId"
        )
        val allAssessments = skillAssessmentDao.getAllAssessmentsForStudent(studentId).first()
        val courseSkills = _courseSkills.value

        Log.d(
            "CourseViewModel",
            "Total de avaliações encontradas para studentId $studentId: ${allAssessments.size}"
        )
        Log.d("CourseViewModel", "Habilidades do curso carregadas: ${courseSkills.size}")

        val statuses = courseSkills.map { courseSkill ->
            Log.d("CourseViewModel", "Avaliando habilidade: ${courseSkill.skillName}")
            val relevantAssessments = allAssessments
                .filter { it.skillName == courseSkill.skillName }
                .sortedByDescending { it.timestamp }
                .distinctBy { it.timestamp }

            Log.d(
                "CourseViewModel",
                "  ${relevantAssessments.size} avaliações relevantes encontradas para ${courseSkill.skillName}."
            )

            if (relevantAssessments.isEmpty()) {
                Log.d(
                    "CourseViewModel",
                    "  Nenhuma avaliação encontrada. Definindo como NOT_APPLICABLE."
                )
                SkillStatus(
                    skillName = courseSkill.skillName,
                    currentLevel = SkillLevel.NOT_APPLICABLE,
                    trend = SkillTrend.STABLE,
                    assessmentCount = 0,
                    lastAssessedTimestamp = 0L
                )
            } else {
                val skillStatusesForTrend = relevantAssessments.map { assessment ->
                    SkillStatus(
                        skillName = assessment.skillName,
                        currentLevel = assessment.level,
                        trend = SkillTrend.STABLE,
                        assessmentCount = relevantAssessments.size,
                        lastAssessedTimestamp = assessment.timestamp
                    )
                }

                val calculatedTrend =
                    if (skillStatusesForTrend.size < 2) {
                        SkillTrend.STABLE
                    } else {
                        val distinctScores = skillStatusesForTrend
                            .mapNotNull { it.currentLevel.score }
                            .distinct()

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

                val currentLevel = skillStatusesForTrend.first().currentLevel
                val lastAssessedTimestamp = skillStatusesForTrend.first().lastAssessedTimestamp

                Log.d(
                    "CourseViewModel",
                    "  Habilidade: ${courseSkill.skillName}, Nível Atual: ${currentLevel.name}, Tendência Calculada: ${calculatedTrend.name}"
                )

                SkillStatus(
                    skillName = courseSkill.skillName,
                    currentLevel = currentLevel,
                    trend = calculatedTrend,
                    assessmentCount = relevantAssessments.size,
                    lastAssessedTimestamp = lastAssessedTimestamp
                )
            }
        }
        _studentSkillStatuses.value = statuses
    }

    // --- ACTIVITY LOGIC ---
    private val _groupsLoaded = MutableStateFlow(false)
    val groupsLoaded: StateFlow<Boolean> = _groupsLoaded.asStateFlow()

    private val _loadedActivityId = MutableStateFlow<Long?>(null)
    val loadedActivityId: StateFlow<Long?> = _loadedActivityId.asStateFlow()

    fun loadActivityDetails(activityId: Long) {
        _groupsLoaded.value = false
        _loadedActivityId.value = null

        viewModelScope.launch {
            _selectedActivity.value = activityDao.getActivityById(activityId)
            loadPersistedGroups(activityId)
            _loadedActivityId.value = activityId
            _groupsLoaded.value = true
        }
    }

    fun clearActivityState() {
        _groupsLoaded.value = false
        _loadedActivityId.value = null
        _generatedGroups.value = emptyList()

        isManualMode = false
        manualGroups.clear()
        unassignedStudents.clear()
    }

    private suspend fun persistGroupsToDatabase(
        activityId: Long,
        groups: List<List<Student>>
    ) {
        groupMemberDao.clearGroupMembersForActivity(activityId)

        val groupMembers = groups.flatMapIndexed { groupIndex, studentList ->
            studentList.map { student ->
                GroupMember(
                    activityId = activityId,
                    studentId = student.studentId, // Usei studentId pois é a chave primária correta
                    groupNumber = groupIndex + 1
                )
            }
        }
        groupMemberDao.insertGroupMembers(groupMembers)
    }


    private suspend fun loadPersistedGroups(activityId: Long) {
        val groupMembers = groupMemberDao.getGroupMembersForActivity(activityId)
        if (groupMembers.isNotEmpty()) {
            val students = studentDao.getStudentsForClass(_selectedCourse.value?.classId ?: 0)
            val studentMap = students.associateBy { it.studentId }
            val groups = groupMembers.groupBy { it.groupNumber }
                .mapValues { (_, members) ->
                    members.mapNotNull { studentMap[it.studentId] }
                }
                .values.toList()
            _generatedGroups.value = groups
        } else {
            _generatedGroups.value = emptyList()
        }
    }

    fun addActivity(onActivityAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        if (activityName.isNotBlank()) {
            viewModelScope.launch {
                val savedTitle = activityName
                val newActivity = Activity(
                    title = savedTitle,
                    description = activityType, // Using description for the type
                    classId = classId
                )

                val activityId = activityDao.insert(newActivity)

                val rows = activityHighlightedSkills
                    .sorted()
                    .map { skillName ->
                        ActivityHighlightedSkill(
                            activityId = activityId,
                            skillName = skillName
                        )
                    }

                activityHighlightedSkillDao.clearForActivity(activityId)
                activityHighlightedSkillDao.insertAll(rows)

                activityName = ""
                activityType = "Individual"
                activityHighlightedSkills = emptySet()

                loadActivitiesForClass(classId)
                onActivityAdded()
                _userMessage.value = "Atividade '$savedTitle' adicionada."

            }
        }
    }

    fun loadActivitiesForClass(classId: Long) {
        viewModelScope.launch {
            _activities.value = activityDao.getActivitiesForClass(classId)
        }
    }

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
            } else { // "Alunos por grupo"
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
            persistGroupsToDatabase(activityId, groups)
        }
    }


    fun loadGroupDetails(groupNumber: Int) {
        val group = _generatedGroups.value.getOrNull(groupNumber - 1)
        _selectedGroupDetails.value = group ?: emptyList()
    }

    fun clearGroupDetails() {
        _selectedGroupDetails.value = emptyList()
    }


    // --- FREQUENCY LOGIC ---
    suspend fun loadFrequencyDetails(session: ClassSession) {
        val records = attendanceDao.getAttendanceRecordsForSession(session.sessionId)
        val studentNameMap = studentsForClass.value.associate { it.studentId to it.displayName }
        _frequencyDetails.value = records.mapNotNull { record ->
            studentNameMap[record.studentId]?.let { name -> name to record.status }
        }.toMap()
    }

    fun clearFrequencyDetails() {
        _frequencyDetails.value = emptyMap()
    }

    fun prepareNewFrequencySession() {
        editingSession = null
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val defaultHour = when {
            currentHour >= 16 -> 16
            currentHour >= 14 -> 14
            currentHour >= 10 -> 10
            else -> 8
        }
        calendar.set(Calendar.HOUR_OF_DAY, defaultHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        newSessionCalendar = calendar
    }

    suspend fun prepareToEditFrequencySession(session: ClassSession): Map<Long, AttendanceStatus> {
        editingSession = session
        newSessionCalendar = Calendar.getInstance().apply { timeInMillis = session.timestamp }
        return attendanceDao.getAttendanceRecordsForSession(session.sessionId)
            .associate { it.studentId to it.status }
    }

    fun deleteFrequencySession(session: ClassSession) {
        viewModelScope.launch {
            attendanceDao.deleteSession(session)
            loadCourseDetails(session.classId)
            _userMessage.value = "Registro de frequência apagado."
        }
    }

    fun updateNewSessionDate(year: Int, month: Int, day: Int) {
        val calendar = newSessionCalendar.clone() as Calendar
        calendar.set(year, month, day)
        newSessionCalendar = calendar
    }

    fun updateNewSessionTime(hour: Int) {
        val calendar = newSessionCalendar.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        newSessionCalendar = calendar
    }

    fun saveFrequency(attendanceMap: Map<Long, AttendanceStatus>, onSaveComplete: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        if (attendanceMap.isEmpty()) {
            _userMessage.value = "Nenhum aluno para registrar."
            return
        }
        viewModelScope.launch {
            val sessionId = editingSession?.sessionId ?: attendanceDao.insertClassSession(
                ClassSession(classId = classId, timestamp = newSessionCalendar.timeInMillis)
            )
            val records = attendanceMap.map { (studentId, status) ->
                AttendanceRecord(sessionId = sessionId, studentId = studentId, status = status)
            }
            attendanceDao.insertAttendanceRecords(records)
            loadCourseDetails(classId)
            _userMessage.value = "Frequência de ${records.size} alunos salva com sucesso."
            onSaveComplete()
        }
    }

    // --- BACKUP AND RESTORE LOGIC ---
    suspend fun backup() {
        val userId = auth.currentUser?.uid ?: run {
            _userMessage.value = "Usuário não logado."
            return
        }
        _userMessage.value = "Iniciando backup..."
        try {
            val backupData = BackupData(
                courses = courseDao.getAllCourses(),
                students = studentDao.getAllStudents(),
                classSessions = attendanceDao.getAllSessions(),
                attendanceRecords = attendanceDao.getAllRecords(),
                activities = activityDao.getAllActivities(),
                groupMembers = groupMemberDao.getAllGroupMembers(),
                skillAssessments = skillAssessmentDao.getAllAssessments().first(), // Corrigido
                courseSkills = courseSkillDao.getAllCourseSkills(),
                activityHighlightedSkills = activityHighlightedSkillDao.getAll()
            )
            val jsonString = Json.encodeToString(BackupData.serializer(), backupData)

            val storageRef = storage.reference.child("backups/$userId/backup.json")
            storageRef.putBytes(jsonString.toByteArray()).await()

            _userMessage.value = "Backup concluído com sucesso!"
        } catch (e: Exception) {
            _userMessage.value = "Erro no backup: ${e.message}"
        }
    }

    suspend fun restore() {
        val userId = auth.currentUser?.uid ?: run {
            _userMessage.value = "Usuário não logado."
            return
        }
        _userMessage.value = "Iniciando restauração..."
        try {
            val storageRef = storage.reference.child("backups/$userId/backup.json")
            val ONE_MEGABYTE: Long = 1024 * 1024
            val bytes = storageRef.getBytes(ONE_MEGABYTE).await()
            val jsonString = String(bytes)

            val backupData = Json.decodeFromString(BackupData.serializer(), jsonString)

            withContext(Dispatchers.IO) {
                db.clearAllTables()
                courseDao.insertAll(backupData.courses)
                studentDao.insertAll(backupData.students)
                activityDao.insertAll(backupData.activities)
                attendanceDao.insertAllSessions(backupData.classSessions)
                attendanceDao.insertAttendanceRecords(backupData.attendanceRecords)
                groupMemberDao.insertAll(backupData.groupMembers)
                skillAssessmentDao.insertAll(backupData.skillAssessments)
                courseSkillDao.insertCourseSkills(backupData.courseSkills)

                activityHighlightedSkillDao.insertAll(backupData.activityHighlightedSkills)
            }

            loadAllCourses()
            _userMessage.value = "Restauração concluída com sucesso!"
        } catch (e: Exception) {
            _userMessage.value = "Erro na restauração: ${e.message}"
        }
    }

    // --- CLASS AND STUDENT LOGIC ---
    fun addCourse(onCourseAdded: () -> Unit) {
        if (courseName.isNotBlank() && academicYear.isNotBlank() && period.isNotBlank()) {
            viewModelScope.launch {
                val newCourse = Course(
                    className = courseName,
                    academicYear = academicYear,
                    period = period,
                    numberOfClasses = numberOfClasses
                )
                val courseId = courseDao.insertCourse(newCourse)

                // Adicionar habilidades padrão para o novo curso
                val skills = defaultSkills.map { CourseSkill(courseId = courseId, skillName = it) }
                courseSkillDao.insertCourseSkills(skills)

                courseName = ""
                academicYear = ""
                period = ""
                numberOfClasses = 0
                loadAllCourses()
                onCourseAdded()
            }
        }
    }

    fun addStudent(onStudentsAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        if (studentName.isNotBlank() && studentNumber.isNotBlank()) {
            viewModelScope.launch {
                val existingStudent = studentDao.getStudentByNumberInClass(studentNumber, classId)
                if (existingStudent == null) {
                    val newStudent = Student(
                        name = studentName,
                        displayName = studentName,
                        studentNumber = studentNumber,
                        classId = classId
                    )
                    val newStudentId = studentDao.insertStudent(newStudent)

                    // REMOVED: Obter habilidades do curso e criar para o aluno
                    // REMOVED: val courseSkills = courseSkillDao.getSkillsForCourse(classId)
                    // REMOVED: val skillsToCreate = courseSkills.map { courseSkill ->
                    // REMOVED: StudentSkill(studentId = newStudentId, skillName = courseSkill.skillName, state = SkillLevel.NOT_APPLICABLE) // Changed state to SkillLevel
                    // REMOVED: }
                    // REMOVED: skillDao.insertOrUpdateSkills(skillsToCreate)

                    _userMessage.value = "Aluno '$studentName' adicionado com sucesso."
                    onStudentsAdded()
                } else {
                    _userMessage.value = "Erro: Aluno com matrícula '$studentNumber' já existe."
                }
                studentName = ""
                studentNumber = ""
                loadCourseDetails(classId)
            }
        }
    }

    fun addStudentsInBulk(onStudentsAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        if (bulkStudentText.isNotBlank()) {
            viewModelScope.launch {
                val existingNumbers = studentDao.getStudentNumbersForClass(classId).toSet()
                val ignoredStudents = mutableListOf<String>()
                var addedCount = 0

                // REMOVED: Obter habilidades do curso
                // REMOVED: val courseSkills = courseSkillDao.getSkillsForCourse(classId)

                bulkStudentText.lines().forEach { line ->
                    val parts = line.trim().split(Regex("\\s+"), 2)
                    if (parts.size == 2) {
                        val number = parts[0]
                        val name = parts[1]
                        if (number.isNotBlank() && name.isNotBlank()) {
                            if (existingNumbers.contains(number)) {
                                ignoredStudents.add(name)
                            } else {
                                val newStudent = Student(
                                    name = name,
                                    displayName = name,
                                    studentNumber = number,
                                    classId = classId
                                )
                                val newStudentId = studentDao.insertStudent(newStudent)

                                // REMOVED: val skillsToCreate = courseSkills.map { courseSkill ->
                                // REMOVED: StudentSkill(studentId = newStudentId, skillName = courseSkill.skillName, state = SkillLevel.NOT_APPLICABLE) // Changed state to SkillLevel
                                // REMOVED: }
                                // REMOVED: skillDao.insertOrUpdateSkills(skillsToCreate)
                                addedCount++
                            }
                        }
                    }
                }
                val message = when {
                    ignoredStudents.isEmpty() -> "$addedCount alunos adicionados com sucesso."
                    addedCount == 0 -> "Nenhum aluno adicionado. ${ignoredStudents.size} já existiam: ${ignoredStudents.joinToString()}"
                    else -> "$addedCount alunos adicionados. ${ignoredStudents.size} ignorados (já existiam): ${ignoredStudents.joinToString()}"
                }
                _userMessage.value = message
                bulkStudentText = ""
                loadCourseDetails(classId)
                onStudentsAdded()
            }
        }
    }

// --- MANUAL GROUPING STATE ---

    var isManualMode by mutableStateOf(false)
        private set

    var manualGroups = mutableStateListOf<Group>()
    var unassignedStudents = mutableStateListOf<Student>()

    private var nextManualGroupId = 1

    private fun generateManualGroupId(): Int = nextManualGroupId++

    fun enterManualMode() {
        if (isManualMode) return

        manualGroups.clear()
        unassignedStudents.clear()
        nextManualGroupId = 1

        val allStudents = _studentsForClass.value
        val existingGroups = _generatedGroups.value

        var assignedStudentIds = mutableSetOf<Long>()

        existingGroups.forEach { groupStudents ->
            if (groupStudents.isNotEmpty()) {
                manualGroups += Group(
                    id = generateManualGroupId(),
                    students = groupStudents.toMutableStateList()
                )
                groupStudents.forEach { assignedStudentIds += it.studentId }
            }
        }

        allStudents
            .filterNot { it.studentId in assignedStudentIds }
            .forEach { unassignedStudents += it }

        isManualMode = true
    }

    fun exitManualMode() {
        isManualMode = false
        manualGroups.clear()
        unassignedStudents.clear()
    }


    fun moveStudent(
        student: Student,
        from: Location,
        to: DropTarget
    ) {
        when (from) {
            Location.Unassigned -> {
                unassignedStudents.remove(student)
            }

            is Location.Group -> {
                val group = manualGroups.firstOrNull { it.id == from.groupId }
                group?.students?.remove(student)

                if (group != null && group.students.isEmpty()) {
                    manualGroups.remove(group)
                }
            }
        }

        when (to) {
            DropTarget.Unassigned -> {
                unassignedStudents.add(student)
            }

            DropTarget.NewGroup -> {
                manualGroups.add(
                    Group(
                        id = generateManualGroupId(),
                        students = mutableStateListOf(student)
                    )
                )
            }

            is DropTarget.ExistingGroup -> {
                manualGroups
                    .firstOrNull { it.id == to.groupId }
                    ?.students
                    ?.add(student)
            }
        }

        // >>> ORDENAÇÃO AQUI <<<

        unassignedStudents.sortBy { it.displayName.lowercase() }

        manualGroups.forEach { group ->
            group.students.sortBy { it.displayName.lowercase() }
        }

        commitManualGroups()
    }

    private fun commitManualGroups() {
        val groups = manualGroups.map { it.students.toList() }

        _generatedGroups.value = groups

        viewModelScope.launch {
            persistGroupsToDatabase(
                activityId = _loadedActivityId.value ?: return@launch,
                groups = groups
            )
        }
    }
}

