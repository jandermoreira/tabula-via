/**
 * Dashboard screen for viewing and managing class attendance history.
 * Provides functionality to view details, edit, or delete existing frequency sessions.
 */
package edu.jm.tabulavia.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.viewmodel.AttendanceDetail
import edu.jm.tabulavia.viewmodel.ClassViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main dashboard for displaying and interacting with a list of attendance sessions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AttendanceDashboardScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit,
    onStartNewAttendance: () -> Unit,
    onEditAttendance: (ClassSession) -> Unit
) {
    val selectedCourse by viewModel.selectedClass.collectAsState()
    val classSessions by viewModel.classSessions.collectAsState()
    var selectedSessionForOptions by remember { mutableStateOf<ClassSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ClassSession?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var currentSession by remember { mutableStateOf<ClassSession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = selectedCourse?.let {
                        "${it.className} ${it.academicYear}/${it.period} - Frequência"
                    } ?: ""
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartNewAttendance) {
                Icon(
                    imageVector = Icons.Filled.AddTask,
                    contentDescription = "Adicionar tarefa"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (classSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma frequência registrada para esta turma.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(classSessions) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onLongClick = { selectedSessionForOptions = session },
                                    onClick = {
                                        currentSession = session
                                        showDialog = true
                                    }
                                )
                        ) {
                            Text(
                                text = "Frequência de ${session.timestamp.toFormattedDateString()}",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Load attendance details when a session is selected
    LaunchedEffect(currentSession) {
        Log.d("AttendanceFlow", "LaunchedEffect com currentSession = $currentSession")
        currentSession?.let {
            viewModel.loadAttendanceDetails(it)
        }
    }

    // Display student list dialog with progress indicator while loading
    if (showDialog && currentSession != null) {
        val details by viewModel.attendanceDetails.collectAsState()

        StudentsDialog(
            session = currentSession!!,
            details = details,
            onDismiss = {
                viewModel.clearFrequencyDetails()
                showDialog = false
                currentSession = null
            }
        )
    }

    // Contextual options dialog (Edit/Delete)
    selectedSessionForOptions?.let { session ->
        AlertDialog(
            onDismissRequest = { selectedSessionForOptions = null },
            title = { Text("Opções de Frequência") },
            text = { Text("O que você deseja fazer com o registro de ${session.timestamp.toFormattedDateString()}?") },
            confirmButton = {
                TextButton(onClick = {
                    onEditAttendance(session)
                    selectedSessionForOptions = null
                }) {
                    Text("Editar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    sessionToDelete = session
                    selectedSessionForOptions = null
                }) {
                    Text("Apagar")
                }
            }
        )
    }

    // Delete confirmation dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Esta ação é definitiva e não pode ser desfeita. Deseja apagar este registro de frequência?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Dialog that displays a list of students extracted from attendance details.
 * Shows a progress indicator while data is loading, then the list of names in alphabetical order.
 *
 * @param session Unused parameter (maintained for signature compatibility).
 * @param details Map containing student names and their attendance status; only names are displayed.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
/**
 * Dialog that displays a list of students extracted from attendance details.
 * Temporary diagnostic version with high-visibility debugging.
 *
 * @param session Unused parameter (maintained for signature compatibility).
 * @param details Map containing student names and their attendance status; only names are displayed.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
@Composable
fun StudentsDialog(
    session: ClassSession,
    details: List<AttendanceDetail>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lista de Frequência") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp) // Limita a altura para permitir scroll
            ) {
                Text(
                    text = "${details.size} aluno(s)",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium
                )

                if (details.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    /* Scrollable container for the student list */
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        details.forEach { detail ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${detail.studentName}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                // Opcional: Badge de status
                                Surface(
                                    color = when(detail.status) {
                                        AttendanceStatus.PRESENT -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        AttendanceStatus.ABSENT -> Color(0xFFF44336).copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = detail.status.displayName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when(detail.status) {
                                            AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
                                            AttendanceStatus.ABSENT -> Color(0xFFC62828)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

/**
 * Extension function to format timestamp into human-readable date and time.
 */
fun Long.toFormattedDateString(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd/MM/yyyy '-' HH'h'", Locale.getDefault())
    return format.format(date)
}





//
///**
// * Dialog that displays attendance details for a selected class session.
// * Shows the session timestamp and a sorted list of students with their attendance status.
// *
// * @param session The class session whose details are being displayed.
// * @param details A map associating student names with their attendance status.
// * @param onDismiss Callback invoked when the dialog is dismissed.
// */
//@Composable
//fun AttendanceDetailsDialog(
//    session: ClassSession,
//    details: Map<String, AttendanceStatus>,
//    onDismiss: () -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Detalhes de Frequência") },
//        text = {
//            Column(modifier = Modifier.fillMaxWidth()) {
//                // Session timestamp line: date and hour (e.g., "18/03/2026 - 16h")
//                Text(
//                    text = session.timestamp.toFormattedDateString(),
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(bottom = 16.dp)
//                )
//
//                // Student list sorted alphabetically by name
//                val sortedEntries = details.entries.sortedBy { it.key }
//
//                if (sortedEntries.isEmpty()) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .heightIn(min = 100.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text("Nenhum aluno encontrado para esta sessão.")
//                    }
//                } else {
//                    LazyColumn(
//                        modifier = Modifier.heightIn(max = 400.dp),
//                        verticalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        items(sortedEntries) { (studentName, status) ->
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                Text(
//                                    text = studentName,
//                                    modifier = Modifier.weight(1f)
//                                )
//                                Text(
//                                    text = status.displayName,
//                                    color = when (status) {
//                                        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary
//                                        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
//                                        AttendanceStatus.JUSTIFIED -> MaterialTheme.colorScheme.tertiary
//                                    },
//                                    fontWeight = FontWeight.Medium
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Fechar")
//            }
//        }
//    )
//}
