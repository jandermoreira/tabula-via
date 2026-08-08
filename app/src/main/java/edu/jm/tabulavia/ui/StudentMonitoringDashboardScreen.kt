/**
 * StudentMonitoringDashboardScreen.kt
 *
 * Screen that displays the pedagogical monitoring dashboard for a class.
 * It provides a summary of students' monitoring states, a detailed list,
 * and dialogs for adding, editing, and managing students.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel

/**
 * Screen displaying the pedagogical monitoring dashboard for a class.
 *
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onStudentClick Callback when a student row is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMonitoringDashboardScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit,
    onStudentClick: (String) -> Unit = {}
) {
    MessageHandler(viewModel)

    // Dialog visibility states
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showStudentDetailsDialog by remember { mutableStateOf(false) }

    // Selection state
    var targetStudent by remember { mutableStateOf<Student?>(null) }
    var selectedStateFilter by remember { mutableStateOf<MonitoringState?>(null) }

    // Data observation
    val dashboardItems by viewModel.dashboardItems.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val todaysAttendance by viewModel.currentSessionAttendance.collectAsState()
    val selectedStudentDetails by viewModel.selectedStudentDetails.collectAsState()
    val attendancePercentage by viewModel.studentAttendancePercentage.collectAsState()

    Scaffold(
        topBar = {
            val titleText = selectedClass?.let {
                "${it.name} ${it.academicYear}/${it.period} - Acompanhamento"
            } ?: "Acompanhamento"

            TabulaTopBar(
                title = titleText,
                viewModel = viewModel,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.prepareNewStudent()
                showAddStudentDialog = true
            }) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Adicionar Aluno"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            MonitoringSummaryCards(
                items = dashboardItems,
                selectedState = selectedStateFilter,
                onStateClick = { selectedStateFilter = it },
                modifier = Modifier.padding(16.dp)
            )

            val filteredAndSortedItems = remember(dashboardItems, selectedStateFilter) {
                dashboardItems
                    .filter { selectedStateFilter == null || it.summary.state == selectedStateFilter }
                    .sortedBy { it.student.effectiveName }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAndSortedItems, key = { it.student.studentId }) { item ->
                    val status = todaysAttendance[item.student.studentId] ?: AttendanceStatus.PRESENT
                    StudentMonitoringRow(
                        item = item,
                        status = status,
                        onClick = {
                            viewModel.loadStudentDetails(item.student.studentId)
                            showStudentDetailsDialog = true
                            onStudentClick(item.student.studentId)
                        },
                        onLongClick = {
                            targetStudent = item.student
                            showOptionsDialog = true
                        }
                    )
                }
            }
        }
    }

    // Secondary action selection dialog
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Opções do Aluno") },
            text = { Text("Selecione uma ação para ${targetStudent?.name}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val student = targetStudent
                        if (student != null) {
                            viewModel.selectStudentForEditing(student)
                            showOptionsDialog = false
                            showEditStudentDialog = true
                        }
                    }
                ) {
                    Text("Editar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOptionsDialog = false
                        showDeleteConfirmDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remover")
                }
            }
        )
    }

    // Permanent removal confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Deseja realmente remover ${targetStudent?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val student = targetStudent
                        if (student != null) {
                            viewModel.deleteStudent(student)
                            showDeleteConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog components for addition, editing and details
    if (showAddStudentDialog) {
        AddStudentDialog(
            viewModel = viewModel,
            onDismiss = { showAddStudentDialog = false }
        )
    }

    if (showEditStudentDialog) {
        EditStudentDialog(
            viewModel = viewModel,
            onDismiss = { showEditStudentDialog = false }
        )
    }

    if (showStudentDetailsDialog) {
        val student = selectedStudentDetails
        if (student != null) {
            StudentDetailsDialog(
                student = student,
                attendancePercentage = attendancePercentage,
                viewModel = viewModel,
                onDismiss = {
                    viewModel.clearStudentDetails()
                    showStudentDetailsDialog = false
                }
            )
        }
    }
}
