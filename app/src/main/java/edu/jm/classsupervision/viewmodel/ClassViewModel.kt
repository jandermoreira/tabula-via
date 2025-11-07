package edu.jm.classsupervision.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import edu.jm.classsupervision.db.DatabaseProvider
import edu.jm.classsupervision.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import java.util.Calendar

class ClassViewModel(application: Application) : AndroidViewModel(application.applicationContext as Application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val classDao = db.classDao()
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()

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

    // Novo estado para o aluno selecionado
    private val _selectedStudentDetails = MutableStateFlow<Student?>(null)
    val selectedStudentDetails: StateFlow<Student?> = _selectedStudentDetails.asStateFlow()

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
        viewModelScope.launch { _classes.value = classDao.getAllClasses() }
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

    // Nova função para carregar detalhes do aluno
    fun loadStudentDetails(studentId: Long) {
        viewModelScope.launch {
            _selectedStudentDetails.value = studentDao.getStudentById(studentId)
        }
    }

    fun clearStudentDetails() {
        _selectedStudentDetails.value = null
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

    // --- LÓGICA DE BACKUP E RESTAURAÇÃO ---
    fun backup() { /* ... */ }
    fun restore() { /* ... */ }
    
    // --- LÓGICA DE TURMAS E ALUNOS ---
    fun addClass(onClassAdded: () -> Unit) { /* ... */ }
    fun addStudent(onStudentsAdded: () -> Unit) { /* ... */ }
    fun addStudentsInBulk(onStudentsAdded: () -> Unit) { /* ... */ }
}
