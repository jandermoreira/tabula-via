/**
 * Attendance management screen for tracking student presence.
 */
package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToColor
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToEmoji
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Main screen for recording student attendance sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit
) {
    MessageHandler(viewModel)

    val students by viewModel.studentsForClass.collectAsState()
    val calendar = viewModel.newSessionCalendar
    val editingSession = viewModel.editingSession

    // Access reactive attendance map from ViewModel
    val attendanceMap = viewModel.attendanceMap
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timeOptions = (0..23).toList()

    // Sync ViewModel data when screen loads
    LaunchedEffect(students, editingSession) {
        if (students.isNotEmpty() && attendanceMap.isEmpty()) {
            if (editingSession != null) {
                viewModel.prepareToEditFrequencySession(editingSession)
            } else {
                // Initialize default presence for new sessions
                students.forEach { student ->
                    attendanceMap[student.studentId] = AttendanceStatus.PRESENT
                }
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
                val courseId = students.firstOrNull()?.classId ?: ""
                viewModel.saveAttendance(courseId, onNavigateBack)
            }) {
                Icon(
                    imageVector = if (editingSession != null) Icons.Filled.CheckCircle else Icons.Filled.Check,
                    contentDescription = "Salvar Frequência"
                )
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
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(calendar.toFormattedDateString())
                }
                Box(modifier = Modifier.weight(0.7f)) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

            // Attendance Statistics
            val presentCount = attendanceMap.values.count { it == AttendanceStatus.PRESENT }
            val absentCount = attendanceMap.values.count { it == AttendanceStatus.ABSENT }
            val excusedCount = attendanceMap.values.count { it == AttendanceStatus.EXCUSED }
            val totalCount = presentCount + absentCount

            // Calculate attendance percentage using double for precision
            var formattedAttendance = "N/A"
            if (totalCount > 0) {
                val attendancePercentage =
                    (presentCount.toDouble() / totalCount.toDouble()) * 100.0
                formattedAttendance = String.format("%.1f%%", attendancePercentage)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$presentCount Presentes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$absentCount Faltas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "$excusedCount Dispensados",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF57C00)
                )
                Text(
                    text = formattedAttendance,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Scrollable list of students
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
                                attendanceMap[student.studentId] = newStatus
                            }
                        )
                    }
                }
            }
        }
    }

    // Date selection logic
    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = utcMillis
                        }
                        viewModel.updateNewSessionDate(
                            utcCalendar.get(Calendar.YEAR),
                            utcCalendar.get(Calendar.MONTH),
                            utcCalendar.get(Calendar.DAY_OF_MONTH)
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
 * Extension to format Calendar for standard date display.
 */
fun Calendar.toFormattedDateString(): String {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return format.format(this.time)
}

/**
 * List item representing a student and their attendance selector with 3 states.
 */
@Composable
fun AttendanceItem(
    student: Student,
    status: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    val isAbsent = status == AttendanceStatus.ABSENT
    val isExcused = status == AttendanceStatus.EXCUSED
    val isPresent = status == AttendanceStatus.PRESENT

    Card(
        onClick = {
            // Cycle through PRESENT -> ABSENT -> EXCUSED -> PRESENT
            val nextStatus = when (status) {
                AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
                AttendanceStatus.ABSENT -> AttendanceStatus.EXCUSED
                AttendanceStatus.EXCUSED -> AttendanceStatus.PRESENT
//                AttendanceStatus.JUSTIFIED -> AttendanceStatus.PRESENT
            }
            onStatusChange(nextStatus)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                AttendanceStatus.EXCUSED -> Color(0xFFFFF8E1) // Light Amber
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPresent) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.scale(0.8f)) {
                EmojiWithBlob(
                    emoji = mapIdToEmoji(student.studentNumber),
                    backgroundColor = when (status) {
                        AttendanceStatus.ABSENT -> Color.Gray
                        AttendanceStatus.EXCUSED -> Color(0xFFFFB300)
                        else -> mapIdToColor(student.studentNumber)
                    },
                    color = if (isAbsent) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            }

            val displayName = student.displayName.ifBlank { student.name }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (status) {
                        AttendanceStatus.ABSENT -> Color.Gray
                        AttendanceStatus.EXCUSED -> Color(0xFF795548)
                        else -> Color.Unspecified
                    }
                )

                val mutedGreen = Color(0xFF2E7D32)
                Text(
                    text = student.studentNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (status) {
                        AttendanceStatus.ABSENT -> Color.Gray.copy(alpha = 0.7f)
                        AttendanceStatus.EXCUSED -> Color(0xFF8D6E63)
                        else -> mutedGreen
                    }
                )
            }

            Icon(
                imageVector = when (status) {
                    AttendanceStatus.ABSENT -> Icons.Default.Close
                    AttendanceStatus.EXCUSED -> Icons.Default.RemoveCircle
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when (status) {
                    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    AttendanceStatus.EXCUSED -> Color(0xFFF57C00)
                    else -> Color.Green
                },
                modifier = Modifier.size(28.dp)
            )
        }
    }
}