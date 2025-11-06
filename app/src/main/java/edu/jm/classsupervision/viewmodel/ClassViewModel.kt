package edu.jm.classsupervision.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.jm.classsupervision.db.DatabaseProvider
import edu.jm.classsupervision.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ClassViewModel(application: Application) : AndroidViewModel(application) {

    private val classDao = DatabaseProvider.getDatabase(application).classDao()
    private val studentDao = DatabaseProvider.getDatabase(application).studentDao()
    private val attendanceDao = DatabaseProvider.getDatabase(application).attendanceDao()

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

    // --- Estados para Formulários ---
    var className by mutableStateOf("")
    var academicYear by mutableStateOf("")
    var period by mutableStateOf("")

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
        viewModelScope.launch {
            _classes.value = classDao.getAllClasses()
        }
    }

    fun loadClassDetails(classId: Long) {
        viewModelScope.launch {
            _selectedClass.value = classDao.getClassById(classId)
            _studentsForClass.value = studentDao.getStudentsForClass(classId)
            _classSessions.value = attendanceDao.getClassSessionsForClass(classId)
        }
    }

    fun clearClassDetails() {
        _selectedClass.value = null
        _studentsForClass.value = emptyList()
        _classSessions.value = emptyList()
        editingSession = null
    }

    fun userMessageShown() {
        _userMessage.value = null
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
            val sessionId = editingSession?.sessionId ?:
            attendanceDao.insertClassSession(ClassSession(classId = classId, timestamp = newSessionCalendar.timeInMillis))

            val records = attendanceMap.map { (studentId, status) ->
                AttendanceRecord(sessionId = sessionId, studentId = studentId, status = status)
            }

            attendanceDao.insertAttendanceRecords(records)

            loadClassDetails(classId)
            _userMessage.value = "Frequência de ${records.size} alunos salva com sucesso."
            onSaveComplete()
        }
    }

    // --- LÓGICA DE TURMAS E ALUNOS ---

    fun addClass(onClassAdded: () -> Unit) {
        if (className.isNotBlank() && academicYear.isNotBlank() && period.isNotBlank()) {
            val newClass = Class(className = className, academicYear = academicYear, period = period)
            viewModelScope.launch {
                classDao.insertClass(newClass)
                className = ""
                academicYear = ""
                period = ""
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
                    val newStudent = Student(name = studentName, studentNumber = studentNumber, classId = classId)
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
                                studentDao.insertStudent(Student(name = name, studentNumber = number, classId = classId))
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