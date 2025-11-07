package edu.jm.classsupervision.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.jm.classsupervision.model.Student
import edu.jm.classsupervision.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit
) {
    var showAddStudentDialog by remember { mutableStateOf(false) }
    val students by viewModel.studentsForClass.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()

    var showStudentDetailsDialog by remember { mutableStateOf(false) }
    val selectedStudent by viewModel.selectedStudentDetails.collectAsState()
    val attendancePercentage by viewModel.studentAttendancePercentage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedClass?.className ?: "") },
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
                    imageVector = Icons.Filled.PersonAddAlt,
                    contentDescription = "Adicionar Aluno"
                )
            }
        }
    ) { paddingValues ->
        StudentsGrid(
            students = students,
            modifier = Modifier.padding(paddingValues),
            onStudentClick = { studentId ->
                viewModel.loadStudentDetails(studentId)
                showStudentDetailsDialog = true
            }
        )
    }

    if (showAddStudentDialog) {
        AddStudentDialog(
            viewModel = viewModel,
            onDismiss = { showAddStudentDialog = false }
        )
    }

    if (showStudentDetailsDialog) {
        selectedStudent?.let { student ->
            StudentDetailsDialog(
                student = student,
                attendancePercentage = attendancePercentage,
                onDismiss = {
                    viewModel.clearStudentDetails()
                    showStudentDetailsDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentsGrid(students: List<Student>, modifier: Modifier = Modifier, onStudentClick: (Long) -> Unit) {
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
            items(students, key = { it.studentId }) {
                student ->
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onStudentClick(student.studentId) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.bodyLarge,
//                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentDetailsDialog(
    student: Student,
    attendancePercentage: Float?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes do Aluno") },
        text = {
            Column {
                Text("Nome: ${student.name}")
                Text("Matrícula: ${student.studentNumber}")
                Spacer(modifier = Modifier.height(8.dp))
                if (attendancePercentage != null) {
                    Text("Frequência: %.0f%%".format(attendancePercentage))
                } else {
                    Text("Frequência: Impossível", color = MaterialTheme.colorScheme.error)
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
