/**
 * Dashboard screen for viewing and managing class attendance history.
 * Provides functionality to view details, edit, or delete existing frequency sessions.
 */
package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.viewmodel.CourseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main dashboard for displaying and interacting with a list of attendance sessions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FrequencyDashboardScreen(
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit,
    onStartNewAttendance: () -> Unit,
    onEditAttendance: (ClassSession) -> Unit
) {
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val classSessions by viewModel.classSessions.collectAsState()
    var selectedSessionForOptions by remember { mutableStateOf<ClassSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ClassSession?>(null) }
    var sessionToView by remember { mutableStateOf<ClassSession?>(null) }

    val scope = rememberCoroutineScope()

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
                                        scope.launch {
                                            viewModel.loadFrequencyDetails(session)
                                            sessionToView = session
                                        }
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

    // Read-only details dialog
    sessionToView?.let { session ->
        val details by viewModel.frequencyDetails.collectAsState()
        FrequencyDetailsDialog(
            session = session,
            details = details,
            onDismiss = {
                viewModel.clearFrequencyDetails()
                sessionToView = null
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

    // Delete confirmation dialog with effective execution
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
 * Dialog displaying a list of students and their attendance status for a given session.
 */
@Composable
private fun FrequencyDetailsDialog(
    session: ClassSession,
    details: Map<String, AttendanceStatus>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes de Frequência") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Registro de: ${session.timestamp.toFormattedDateString()}",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (details.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(details.entries.toList()) { (name, status) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, modifier = Modifier.weight(1f))
                                Text(
                                    status.displayName,
                                    color = if (status == AttendanceStatus.PRESENT)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
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
    val format = SimpleDateFormat("dd/MM/yyyy '-' HH:mm", Locale.getDefault())
    return format.format(date)
}