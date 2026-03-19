/**
 * StudentListScreen.kt
 *
 * Provides the user interface for listing, adding, editing, and removing students
 * within a specific course. It manages the visibility of various action dialogs
 * and handles interactions with the CourseViewModel.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.CourseViewModel

/**
 * Composable function that represents the student list screen.
 *
 * @param viewModel The ViewModel providing data and logic for student management.
 * @param onNavigateBack Callback executed when the back navigation is triggered.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudentListScreen(
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
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

    // Data observation
    val students by viewModel.studentsForClass.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val todaysAttendance by viewModel.todaysAttendance.collectAsState()
    val selectedStudentDetails by viewModel.selectedStudentDetails.collectAsState()
    val attendancePercentage by viewModel.studentAttendancePercentage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = selectedCourse?.let {
                        "${it.className} ${it.academicYear}/${it.period} - Alunos"
                    } ?: ""
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Student"
                )
            }
        }
    ) { paddingValues ->
        // Main student display area
        StudentsGrid(
            students = students,
            todaysAttendance = todaysAttendance,
            modifier = Modifier.padding(paddingValues),
            onStudentClick = { studentId ->
                viewModel.loadStudentDetails(studentId)
                showStudentDetailsDialog = true
            },
            onStudentLongClick = { student ->
                targetStudent = student
                showOptionsDialog = true
            }
        )
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

/**
 * Renders a grid of students with support for click and long-press actions.
 *
 * @param students List of students to be displayed.
 * @param todaysAttendance Map containing the attendance status for the current date.
 * @param modifier Modifier for the grid layout.
 * @param onStudentClick Callback for single-click events.
 * @param onStudentLongClick Callback for long-press events.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentsGrid(
    students: List<Student>,
    todaysAttendance: Map<String, AttendanceStatus>,
    modifier: Modifier = Modifier,
    onStudentClick: (String) -> Unit,
    onStudentLongClick: (Student) -> Unit
) {
    if (students.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Nenhum aluno cadastrado nesta turma.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students, key = { it.studentId }) { student ->
                val isAbsent = todaysAttendance[student.studentId] == AttendanceStatus.ABSENT
                StudentItem(
                    student = student,
                    isAbsent = isAbsent,
                    modifier = Modifier.combinedClickable(
                        onClick = { onStudentClick(student.studentId) },
                        onLongClick = { onStudentLongClick(student) }
                    )
                )
            }
        }
    }
}