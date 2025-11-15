package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Added for remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.R
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudentListScreen(
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSkills: (Long) -> Unit
) {
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf(false) }
    val students by viewModel.studentsForClass.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val todaysAttendance by viewModel.todaysAttendance.collectAsState()

    var showStudentDetailsDialog by remember { mutableStateOf(false) }
    val selectedStudent by viewModel.selectedStudentDetails.collectAsState()
    val attendancePercentage by viewModel.studentAttendancePercentage.collectAsState()
    // Removed: val studentSkills by viewModel.studentSkills.collectAsState()
    // Removed: val courseSkills by viewModel.courseSkills.collectAsState()

    // Removed: val filteredSkills = remember(studentSkills, courseSkills) { ... }

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
                            contentDescription = "Voltar"
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
                    painter = painterResource(id = R.drawable.student_0),
                    contentDescription = "Adicionar Aluno"
                )
            }
        }
    ) { paddingValues ->
        StudentsGrid(
            students = students,
            todaysAttendance = todaysAttendance,
            modifier = Modifier.padding(paddingValues),
            onStudentClick = { studentId ->
                viewModel.loadStudentDetails(studentId)
                showStudentDetailsDialog = true
            },
            onStudentLongClick = { student ->
                viewModel.selectStudentForEditing(student)
                showEditStudentDialog = true
            }
        )
    }

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
        selectedStudent?.let { student ->
            StudentDetailsDialog(
                student = student,
                attendancePercentage = attendancePercentage,
                // Removed: skills = filteredSkills, // Usa a lista filtrada
                viewModel = viewModel, // Pass the viewModel to StudentDetailsDialog
                onDismiss = {
                    viewModel.clearStudentDetails()
                    showStudentDetailsDialog = false
                },
                onEditSkills = {
                    onNavigateToSkills(student.studentId)
                    showStudentDetailsDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentsGrid(
    students: List<Student>,
    todaysAttendance: Map<Long, AttendanceStatus>,
    modifier: Modifier = Modifier,
    onStudentClick: (Long) -> Unit,
    onStudentLongClick: (Student) -> Unit
) {
    if (students.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum aluno cadastrado nesta turma.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
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
