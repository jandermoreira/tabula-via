package edu.jm.classsupervision.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import edu.jm.classsupervision.db.DatabaseProvider
import edu.jm.classsupervision.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Calendar

class ClassViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val classDao = db.classDao()
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()
    private val activityDao = db.activityDao()

    private val storage = Firebase.storage
    private val auth = Firebase.auth

    // --- Estados da UI ---
    private val _classes = MutableStateFlow<List<Class>>(emptyList())
    val classes: StateFlow<List<Class>> = _classes.asStateFlow()

    private val _selectedClass = MutableStateFlow<Class?>(null)
    val selectedClass: StateFlow<Class?> = _selectedClass.asStateFlow()

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

    // --- Estados para Formulários ---
    var className by mutableStateOf("")
    var academicYear by mutableStateOf("")
    var period by mutableStateOf("")
    var numberOfClasses by mutableIntStateOf(0)
    var studentName by mutableStateOf("")
    var studentNumber by mutableStateOf("")
    var bulkStudentText by mutableStateOf("")
    var newSessionCalendar by mutableStateOf(Calendar.getInstance())
    var editingSession by mutableStateOf<ClassSession?>(null)

    init {
        loadAllClasses()
    }

    // --- LÓGICA DE CARREGAMENTO E LIMPEZA ---
    private fun loadAllClasses() {
        viewModelScope.launch { _classes.value = classDao.getAllClasses() }
    }

    fun loadClassDetails(classId: Long) {
        viewModelScope.launch {
            _selectedClass.value = classDao.getClassById(classId)
            _studentsForClass.value = studentDao.getStudentsForClass(classId)
            _classSessions.value = attendanceDao.getClassSessionsForClass(classId)
            _activities.value = activityDao.getActivitiesForClass(classId)
        }
    }

    fun clearClassDetails() {
        _selectedClass.value = null
        _studentsForClass.value = emptyList()
        _classSessions.value = emptyList()
        _activities.value = emptyList()
        editingSession = null
    }

    fun userMessageShown() {
        _userMessage.value = null
    }

    // --- LÓGICA DE ALUNOS ---
    fun loadStudentDetails(studentId: Long) {
        viewModelScope.launch {
            _selectedStudentDetails.value = studentDao.getStudentById(studentId)
            val totalClasses = _selectedClass.value?.numberOfClasses ?: 0
            if (totalClasses > 0) {
                val abscences = attendanceDao.countStudentAbsences(studentId)
                _studentAttendancePercentage.value =
                    ((totalClasses.toFloat() - abscences.toFloat()) / totalClasses.toFloat()) * 100
            } else {
                _studentAttendancePercentage.value = null
            }
        }
    }

    fun clearStudentDetails() {
        _selectedStudentDetails.value = null
        _studentAttendancePercentage.value = null
    }

    // --- LÓGICA DE ATIVIDADES ---
    fun loadActivitiesForClass(classId: Long) {
        viewModelScope.launch {
            _activities.value = activityDao.getActivitiesForClass(classId)
        }
    }

    // --- LÓGICA DE FREQUÊNCIA ---
    suspend fun loadFrequencyDetails(session: ClassSession) {
        val records = attendanceDao.getAttendanceRecordsForSession(session.sessionId)
        val studentNameMap = studentsForClass.value.associate { it.studentId to it.name }
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
            loadClassDetails(session.classId)
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
        val classId = _selectedClass.value?.classId ?: return
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
            loadClassDetails(classId)
            _userMessage.value = "Frequência de ${records.size} alunos salva com sucesso."
            onSaveComplete()
        }
    }

    // --- LÓGICA DE BACKUP E RESTAURAÇÃO ---
    suspend fun backup() {
        val userId = auth.currentUser?.uid ?: run {
            _userMessage.value = "Usuário não logado."
            return
        }
        _userMessage.value = "Iniciando backup..."
        try {
            val classes = classDao.getAllClasses()
            val students = classes.flatMap { studentDao.getStudentsForClass(it.classId) }
            val sessions =
                classes.flatMap { attendanceDao.getClassSessionsForClass(it.classId) }
            val records =
                sessions.flatMap { attendanceDao.getAttendanceRecordsForSession(it.sessionId) }

            val backupData = BackupData(classes, students, sessions, records)
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
                classDao.insertAll(backupData.classes)
                studentDao.insertAll(backupData.students)
                attendanceDao.insertAllSessions(backupData.classSessions)
                attendanceDao.insertAttendanceRecords(backupData.attendanceRecords)
            }

            loadAllClasses()
            _userMessage.value = "Restauração concluída com sucesso!"
        } catch (e: Exception) {
            _userMessage.value = "Erro na restauração: ${e.message}"
        }
    }

    // --- LÓGICA DE TURMAS E ALUNOS ---
    fun addClass(onClassAdded: () -> Unit) {
        if (className.isNotBlank() && academicYear.isNotBlank() && period.isNotBlank()) {
            val newClass = Class(
                className = className,
                academicYear = academicYear,
                period = period,
                numberOfClasses = numberOfClasses
            )
            viewModelScope.launch {
                classDao.insertClass(newClass)
                className = ""
                academicYear = ""
                period = ""
                numberOfClasses = 0
                loadAllClasses()
                onClassAdded()
            }
        }
    }

    fun addStudent(onStudentsAdded: () -> Unit) {
        val classId = _selectedClass.value?.classId ?: return
        if (studentName.isNotBlank() && studentNumber.isNotBlank()) {
            viewModelScope.launch {
                val existingStudent = studentDao.getStudentByNumberInClass(studentNumber, classId)
                if (existingStudent == null) {
                    val newStudent = Student(
                        name = studentName,
                        studentNumber = studentNumber,
                        classId = classId
                    )
                    studentDao.insertStudent(newStudent)
                    _userMessage.value = "Aluno '${studentName}' adicionado com sucesso."
                    onStudentsAdded()
                } else {
                    _userMessage.value = "Erro: Aluno com matrícula '${studentNumber}' já existe."
                }
                studentName = ""
                studentNumber = ""
                loadClassDetails(classId)
            }
        }
    }

    fun addStudentsInBulk(onStudentsAdded: () -> Unit) {
        val classId = _selectedClass.value?.classId ?: return
        if (bulkStudentText.isNotBlank()) {
            viewModelScope.launch {
                val existingNumbers = studentDao.getStudentNumbersForClass(classId).toSet()
                val ignoredStudents = mutableListOf<String>()
                var addedCount = 0

                bulkStudentText.lines().forEach { line ->
                    val parts = line.trim().split(Regex("\\s+"), 2)
                    if (parts.size == 2) {
                        val number = parts[0]
                        val name = parts[1]
                        if (number.isNotBlank() && name.isNotBlank()) {
                            if (existingNumbers.contains(number)) {
                                ignoredStudents.add(name)
                            } else {
                                studentDao.insertStudent(
                                    Student(
                                        name = name,
                                        studentNumber = number,
                                        classId = classId
                                    )
                                )
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
                loadClassDetails(classId)
                onStudentsAdded()
            }
        }
    }
}
