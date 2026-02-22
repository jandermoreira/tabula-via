/**
 * Attendance management screen for tracking student presence.
 * This version integrates with the StudentEmojiColorHelper and applies 
 * the organic blob visual style for student avatars.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.CourseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Main screen for recording student attendance sessions.
 * * @param viewModel The state holder for course and session data.
 * @param onNavigateBack Callback to return to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.studentsForClass.collectAsState()
    val calendar = viewModel.newSessionCalendar
    val editingSession = viewModel.editingSession

    var attendanceMap by remember { mutableStateOf<Map<Long, AttendanceStatus>>(emptyMap()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timeOptions = (0..23).toList()

    // Sync local state with ViewModel data
    LaunchedEffect(students, editingSession) {
        if (students.isNotEmpty()) {
            attendanceMap = if (editingSession != null) {
                viewModel.prepareToEditFrequencySession(editingSession)
            } else {
                students.associate { it.studentId to AttendanceStatus.PRESENT }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingSession != null) "Editar Frequência" else "Registrar Frequência") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.saveFrequency(attendanceMap, onNavigateBack)
            }) {
                Icon(Icons.Filled.Check, contentDescription = "Salvar Frequência")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Date and Time selection header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(calendar.toFormattedDateString())
                }
                Box(modifier = Modifier.weight(0.7f)) {
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${calendar.get(Calendar.HOUR_OF_DAY)}h")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showTimePicker,
                        onDismissRequest = { showTimePicker = false }
                    ) {
                        timeOptions.forEach { hour ->
                            DropdownMenuItem(
                                text = { Text("${hour}h") },
                                onClick = {
                                    viewModel.updateNewSessionTime(hour)
                                    showTimePicker = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Scrollable list of students with attendance toggles
            if (attendanceMap.isEmpty() && students.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(students, key = { it.studentId }) { student ->
                        AttendanceItem(
                            student = student,
                            status = attendanceMap[student.studentId] ?: AttendanceStatus.PRESENT,
                            onStatusChange = { newStatus ->
                                attendanceMap = attendanceMap + (student.studentId to newStatus)
                            }
                        )
                    }
                }
            }
        }
    }

    // Date selection dialog logic
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedCalendar = Calendar.getInstance().apply { timeInMillis = it }
                        viewModel.updateNewSessionDate(
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH),
                            selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Extension to format Calendar for Brazil-standard date display.
 */
fun Calendar.toFormattedDateString(): String {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return format.format(this.time)
}

/**
 * List item representing a student and their attendance selector.
 * Uses the organic blob style for the avatar.
 * * @param student The student model.
 * @param status Current selected attendance status.
 * @param onStatusChange Callback for updating the status.
 */
@Composable
fun AttendanceItem(
    student: Student,
    status: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    val isAbsent = status == AttendanceStatus.ABSENT

    // Resolve deterministic emoji and color
    val studentEmoji = remember(student.studentId) {
        StudentEmojiColorHelper.mapStudentIdToEmoji(student.studentId)
    }
    val backgroundColor = remember(student.studentId, isAbsent) {
        if (isAbsent) androidx.compose.ui.graphics.Color.Gray
        else StudentEmojiColorHelper.generateColorFromId(student.studentId)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isAbsent) 0.6f else 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual avatar using shared Blob logic
            EmojiWithBlob(
                emoji = studentEmoji,
                backgroundColor = backgroundColor,
                modifier = Modifier.size(56.dp) // Smaller footprint for list rows
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = student.displayName,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Presence/Absence toggle row
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = status == AttendanceStatus.PRESENT,
                    onClick = { onStatusChange(AttendanceStatus.PRESENT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("P")
                }
                SegmentedButton(
                    selected = status == AttendanceStatus.ABSENT,
                    onClick = { onStatusChange(AttendanceStatus.ABSENT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("F")
                }
            }
        }
    }
}