package edu.jm.tabulavia.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.*
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.*
import edu.jm.tabulavia.utils.SkillTrendCalculator
import edu.jm.tabulavia.utils.TrendCalculationMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Calendar

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    /* ============================================================
     * Database / Firebase
     * ============================================================ */

    private val db = DatabaseProvider.getDatabase(application)

    private val courseDao = db.courseDao()
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()
    private val activityDao = db.activityDao()
    private val skillDao = db.skillDao()
    private val groupMemberDao = db.groupMemberDao()
    private val courseSkillDao = db.courseSkillDao()
    private val skillAssessmentDao = db.skillAssessmentDao()
    private val activityHighlightedSkillDao = db.activityHighlightedSkillDao()

    private val storage = Firebase.storage
    private val auth = Firebase.auth

    /* ============================================================
     * StateFlow – dados principais
     * ============================================================ */

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

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _courseSkills = MutableStateFlow<List<CourseSkill>>(emptyList())
    val courseSkills: StateFlow<List<CourseSkill>> = _courseSkills.asStateFlow()

    private val _generatedGroups = MutableStateFlow<List<List<Student>>>(emptyList())
    val generatedGroups: StateFlow<List<List<Student>>> = _generatedGroups.asStateFlow()

    private val _todaysAttendance = MutableStateFlow<Map<Long, AttendanceStatus>>(emptyMap())
    val todaysAttendance: StateFlow<Map<Long, AttendanceStatus>> = _todaysAttendance.asStateFlow()

    private val _selectedGroupDetails = MutableStateFlow<List<Student>>(emptyList())
    val selectedGroupDetails: StateFlow<List<Student>> = _selectedGroupDetails.asStateFlow()

    private val _frequencyDetails = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val frequencyDetails: StateFlow<Map<String, AttendanceStatus>> = _frequencyDetails.asStateFlow()

    private val _selectedStudentDetails = MutableStateFlow<Student?>(null)
    val selectedStudentDetails: StateFlow<Student?> = _selectedStudentDetails.asStateFlow()

    private val _studentAttendancePercentage = MutableStateFlow<Float?>(null)
    val studentAttendancePercentage: StateFlow<Float?> =
        _studentAttendancePercentage.asStateFlow()

    private val _skillAssessmentLog = MutableStateFlow<List<SkillAssessment>>(emptyList())
    val skillAssessmentLog: StateFlow<List<SkillAssessment>> =
        _skillAssessmentLog.asStateFlow()

    private val _studentSkillSummaries =
        MutableStateFlow<Map<String, SkillAssessmentsSummary>>(emptyMap())
    val studentSkillSummaries: StateFlow<Map<String, SkillAssessmentsSummary>> =
        _studentSkillSummaries.asStateFlow()

    private val _studentSkillStatuses = MutableStateFlow<List<SkillStatus>>(emptyList())
    val studentSkillStatuses: StateFlow<List<SkillStatus>> =
        _studentSkillStatuses.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /* ============================================================
     * Estados auxiliares de atividade
     * ============================================================ */

    private val _groupsLoaded = MutableStateFlow(false)
    val groupsLoaded: StateFlow<Boolean> = _groupsLoaded.asStateFlow()

    private val _loadedActivityId = MutableStateFlow<Long?>(null)
    val loadedActivityId: StateFlow<Long?> = _loadedActivityId.asStateFlow()

    /* ============================================================
     * Estado mutável de formulários
     * ============================================================ */

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

    /* ============================================================
     * Inicialização
     * ============================================================ */

    init {
        loadAllCourses()
    }

    /* ============================================================
     * Cursos
     * ============================================================ */

    private fun loadAllCourses() {
        viewModelScope.launch {
            _courses.value = courseDao.getAllCourses()
        }
    }

    fun loadCourseDetails(classId: Long) {
        viewModelScope.launch {
            _selectedCourse.value = courseDao.getCourseById(classId)
            _studentsForClass.value = studentDao.getStudentsForClass(classId)
            val sessions = attendanceDao.getClassSessionsForClass(classId)
            _classSessions.value = sessions
            _activities.value = activityDao.getActivitiesForClass(classId)
            _courseSkills.value = courseSkillDao.getSkillsForCourse(classId)
            loadTodaysAttendance(sessions)
        }
    }

    private fun loadTodaysAttendance(sessions: List<ClassSession>) {
        viewModelScope.launch {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val end = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val lastToday = sessions
                .filter { it.timestamp in start..end }
                .maxByOrNull { it.timestamp }

            _todaysAttendance.value =
                lastToday?.let {
                    attendanceDao
                        .getAttendanceRecordsForSession(it.sessionId)
                        .associate { r -> r.studentId to r.status }
                } ?: emptyMap()
        }
    }

    fun clearCourseDetails() {
        _selectedCourse.value = null
        _studentsForClass.value = emptyList()
        _classSessions.value = emptyList()
        _activities.value = emptyList()
        _courseSkills.value = emptyList()
        _todaysAttendance.value = emptyMap()
        _studentSkillStatuses.value = emptyList()
        editingSession = null
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    /* ============================================================
     * Alunos
     * ============================================================ */

    fun selectStudentForEditing(student: Student) {
        studentName = student.name
        studentDisplayName = student.displayName
        studentNumber = student.studentNumber
        _selectedStudentDetails.value = student
    }

    fun updateStudent(onDismiss: () -> Unit) {
        val current = _selectedStudentDetails.value ?: return
        val updated = current.copy(
            name = studentName,
            displayName = studentDisplayName,
            studentNumber = studentNumber
        )
        viewModelScope.launch {
            studentDao.updateStudent(updated)
            loadCourseDetails(current.classId)
            onDismiss()
        }
    }

    fun loadStudentDetails(studentId: Long) {
        viewModelScope.launch {
            _selectedStudentDetails.value = studentDao.getStudentById(studentId)

            val classId = _selectedCourse.value?.classId ?: return@launch
            _courseSkills.value = courseSkillDao.getSkillsForCourse(classId)

            calculateStudentSkillStatus(studentId)

            val total = _selectedCourse.value?.numberOfClasses ?: 0
            _studentAttendancePercentage.value =
                if (total > 0) {
                    val absences = attendanceDao.countStudentAbsences(studentId)
                    ((total - absences).toFloat() / total) * 100
                } else null
        }
    }

    fun clearStudentDetails() {
        _selectedStudentDetails.value = null
        _studentAttendancePercentage.value = null
        _studentSkillSummaries.value = emptyMap()
        _studentSkillStatuses.value = emptyList()
    }

    /* ============================================================
     * Habilidades
     * ============================================================ */

    private val defaultSkills = listOf(
        "Participação", "Comunicação", "Escuta",
        "Organização", "Técnica", "Colaboração", "Reflexão"
    )

    fun loadSkillsForCourse(courseId: Long) {
        viewModelScope.launch {
            _courseSkills.value = courseSkillDao.getSkillsForCourse(courseId)
        }
    }

    fun loadSkillAssessmentLog() {
        viewModelScope.launch {
            _skillAssessmentLog.value =
                skillAssessmentDao.getAllAssessments().first()
        }
    }

    fun addCourseSkill(onSkillAdded: () -> Unit) {
        val courseId = _selectedCourse.value?.classId ?: return
        if (skillName.isBlank()) return

        viewModelScope.launch {
            courseSkillDao.insertCourseSkills(
                listOf(CourseSkill(courseId, skillName))
            )
            skillName = ""
            loadSkillsForCourse(courseId)
            onSkillAdded()
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
            skillAssessmentDao.insert(
                SkillAssessment(
                    studentId = studentId,
                    skillName = skillName,
                    level = level,
                    source = source,
                    assessorId = assessorId,
                    timestamp = timestamp ?: System.currentTimeMillis()
                )
            )
            loadStudentDetails(studentId)
        }
    }

    fun addProfessorSkillAssessments(
        studentId: Long,
        assessments: List<Pair<String, SkillLevel>>
    ) {
        viewModelScope.launch {
            skillAssessmentDao.insertAll(
                assessments.map { (name, level) ->
                    SkillAssessment(
                        studentId = studentId,
                        skillName = name,
                        level = level,
                        source = AssessmentSource.PROFESSOR_OBSERVATION
                    )
                }
            )
            loadStudentDetails(studentId)
        }
    }

    private suspend fun calculateStudentSkillStatus(
        studentId: Long,
        historyCount: Int = 3
    ) {
        val assessments =
            skillAssessmentDao.getAllAssessmentsForStudent(studentId).first()
        val skills = _courseSkills.value

        _studentSkillStatuses.value = skills.map { courseSkill ->
            val relevant = assessments
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
                val statuses = relevant.map {
                    SkillStatus(
                        skillName = it.skillName,
                        currentLevel = it.level,
                        trend = SkillTrend.STABLE,
                        assessmentCount = relevant.size,
                        lastAssessedTimestamp = it.timestamp
                    )
                }

                val trend =
                    if (statuses.size < 2) SkillTrend.STABLE
                    else {
                        val scores = statuses.mapNotNull { s -> s.currentLevel.score }.distinct()
                        if (scores.size < 2) SkillTrend.STABLE
                        else SkillTrendCalculator.calculateTrend(
                            statuses,
                            TrendCalculationMethod.LINEAR_REGRESSION,
                            historyCount
                        )
                    }

                statuses.first().copy(trend = trend)
            }
        }
    }

    /* ============================================================
     * Atividades e grupos
     * ============================================================ */

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
        groupMemberDao.insertGroupMembers(
            groups.flatMapIndexed { index, list ->
                list.map {
                    GroupMember(
                        activityId = activityId,
                        studentId = it.studentId,
                        groupNumber = index + 1
                    )
                }
            }
        )
    }

    private suspend fun loadPersistedGroups(activityId: Long) {
        val members = groupMemberDao.getGroupMembersForActivity(activityId)
        if (members.isEmpty()) {
            _generatedGroups.value = emptyList()
            return
        }

        val students =
            studentDao.getStudentsForClass(_selectedCourse.value?.classId ?: 0)
                .associateBy { it.studentId }

        _generatedGroups.value = members
            .groupBy { it.groupNumber }
            .mapValues { (_, m) -> m.mapNotNull { students[it.studentId] } }
            .values
            .toList()
    }

    /* ============================================================
     * Agrupamento manual
     * ============================================================ */

    var isManualMode by mutableStateOf(false)
        private set

    val manualGroups = mutableStateListOf<Group>()
    val unassignedStudents = mutableStateListOf<Student>()

    private var nextManualGroupId = 1

    private fun generateManualGroupId(): Int = nextManualGroupId++

    fun enterManualMode() {
        if (isManualMode) return

        manualGroups.clear()
        unassignedStudents.clear()
        nextManualGroupId = 1

        val allStudents = _studentsForClass.value
        val existingGroups = _generatedGroups.value

        val assignedIds = mutableSetOf<Long>()

        existingGroups.forEach { group ->
            if (group.isNotEmpty()) {
                manualGroups += Group(
                    id = generateManualGroupId(),
                    students = group.toMutableStateList()
                )
                group.forEach { assignedIds += it.studentId }
            }
        }

        allStudents
            .filterNot { it.studentId in assignedIds }
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
            Location.Unassigned -> unassignedStudents.remove(student)
            is Location.Group -> {
                val group = manualGroups.firstOrNull { it.id == from.groupId }
                group?.students?.remove(student)
                if (group != null && group.students.isEmpty()) {
                    manualGroups.remove(group)
                }
            }
        }

        when (to) {
            DropTarget.Unassigned -> unassignedStudents.add(student)
            DropTarget.NewGroup ->
                manualGroups.add(
                    Group(
                        id = generateManualGroupId(),
                        students = mutableStateListOf(student)
                    )
                )

            is DropTarget.ExistingGroup ->
                manualGroups.firstOrNull { it.id == to.groupId }
                    ?.students
                    ?.add(student)
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

    /* ============================================================
     * Frequência
     * ============================================================ */

    suspend fun loadFrequencyDetails(session: ClassSession) {
        val records = attendanceDao.getAttendanceRecordsForSession(session.sessionId)
        val names = studentsForClass.value.associate { it.studentId to it.displayName }
        _frequencyDetails.value =
            records.mapNotNull { r -> names[r.studentId]?.let { it to r.status } }.toMap()
    }

    fun clearFrequencyDetails() {
        _frequencyDetails.value = emptyMap()
    }

    fun prepareNewFrequencySession() {
        editingSession = null
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val defaultHour = when {
            hour >= 16 -> 16
            hour >= 14 -> 14
            hour >= 10 -> 10
            else -> 8
        }

        calendar.set(Calendar.HOUR_OF_DAY, defaultHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        newSessionCalendar = calendar
    }

    suspend fun prepareToEditFrequencySession(
        session: ClassSession
    ): Map<Long, AttendanceStatus> {
        editingSession = session
        newSessionCalendar = Calendar.getInstance().apply {
            timeInMillis = session.timestamp
        }
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

    fun saveFrequency(
        attendanceMap: Map<Long, AttendanceStatus>,
        onSaveComplete: () -> Unit
    ) {
        val classId = _selectedCourse.value?.classId ?: return
        if (attendanceMap.isEmpty()) {
            _userMessage.value = "Nenhum aluno para registrar."
            return
        }

        viewModelScope.launch {
            val sessionId = editingSession?.sessionId
                ?: attendanceDao.insertClassSession(
                    ClassSession(
                        classId = classId,
                        timestamp = newSessionCalendar.timeInMillis
                    )
                )

            val records = attendanceMap.map { (studentId, status) ->
                AttendanceRecord(
                    sessionId = sessionId,
                    studentId = studentId,
                    status = status
                )
            }

            attendanceDao.insertAttendanceRecords(records)
            loadCourseDetails(classId)
            _userMessage.value =
                "Frequência de ${records.size} alunos salva com sucesso."
            onSaveComplete()
        }
    }

    /* ============================================================
     * Backup / Restore
     * ============================================================ */

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
                skillAssessments = skillAssessmentDao.getAllAssessments().first(),
                courseSkills = courseSkillDao.getAllCourseSkills(),
                activityHighlightedSkills = activityHighlightedSkillDao.getAll()
            )

            val json = Json.encodeToString(
                BackupData.serializer(),
                backupData
            )

            storage.reference
                .child("backups/$userId/backup.json")
                .putBytes(json.toByteArray())
                .await()

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
            val bytes = storage.reference
                .child("backups/$userId/backup.json")
                .getBytes(1024 * 1024)
                .await()

            val backupData = Json.decodeFromString(
                BackupData.serializer(),
                String(bytes)
            )

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
                activityHighlightedSkillDao.insertAll(
                    backupData.activityHighlightedSkills
                )
            }

            loadAllCourses()
            _userMessage.value = "Restauração concluída com sucesso!"
        } catch (e: Exception) {
            _userMessage.value = "Erro na restauração: ${e.message}"
        }
    }

    /* ============================================================
     * Criação de cursos e alunos
     * ============================================================ */

    fun addCourse(onCourseAdded: () -> Unit) {
        if (courseName.isBlank() ||
            academicYear.isBlank() ||
            period.isBlank()
        ) return

        viewModelScope.launch {
            val courseId = courseDao.insertCourse(
                Course(
                    className = courseName,
                    academicYear = academicYear,
                    period = period,
                    numberOfClasses = numberOfClasses
                )
            )

            courseSkillDao.insertCourseSkills(
                defaultSkills.map {
                    CourseSkill(courseId = courseId, skillName = it)
                }
            )

            courseName = ""
            academicYear = ""
            period = ""
            numberOfClasses = 0

            loadAllCourses()
            onCourseAdded()
        }
    }

    fun addStudent(onStudentsAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        if (studentName.isBlank() || studentNumber.isBlank()) return

        viewModelScope.launch {
            val existing =
                studentDao.getStudentByNumberInClass(studentNumber, classId)

            if (existing == null) {
                studentDao.insertStudent(
                    Student(
                        name = studentName,
                        displayName = studentName,
                        studentNumber = studentNumber,
                        classId = classId
                    )
                )
                _userMessage.value =
                    "Aluno '$studentName' adicionado com sucesso."
                onStudentsAdded()
            } else {
                _userMessage.value =
                    "Erro: Aluno com matrícula '$studentNumber' já existe."
            }

            studentName = ""
            studentNumber = ""
            loadCourseDetails(classId)
        }
    }

    fun addStudentsInBulk(onStudentsAdded: () -> Unit) {
        val classId = _selectedCourse.value?.classId ?: return
        if (bulkStudentText.isBlank()) return

        viewModelScope.launch {
            val existingNumbers =
                studentDao.getStudentNumbersForClass(classId).toSet()

            val ignored = mutableListOf<String>()
            var added = 0

            bulkStudentText.lines().forEach { line ->
                val parts = line.trim().split(Regex("\\s+"), 2)
                if (parts.size == 2) {
                    val number = parts[0]
                    val name = parts[1]
                    if (number !in existingNumbers) {
                        studentDao.insertStudent(
                            Student(
                                name = name,
                                displayName = name,
                                studentNumber = number,
                                classId = classId
                            )
                        )
                        added++
                    } else {
                        ignored += name
                    }
                }
            }

            _userMessage.value = when {
                ignored.isEmpty() ->
                    "$added alunos adicionados com sucesso."

                added == 0 ->
                    "Nenhum aluno adicionado. ${ignored.size} já existiam: ${
                        ignored.joinToString()
                    }"

                else ->
                    "$added alunos adicionados. ${ignored.size} ignorados (já existiam): ${
                        ignored.joinToString()
                    }"
            }

            bulkStudentText = ""
            loadCourseDetails(classId)
            onStudentsAdded()
        }
    }
}
