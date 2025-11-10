package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.SkillState
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.StudentSkill
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSkills: (Long) -> Unit // Adicionado para navegação
) {
    var showAddStudentDialog by remember { mutableStateOf(false) }
    val students by viewModel.studentsForClass.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()

    var showStudentDetailsDialog by remember { mutableStateOf(false) }
    val selectedStudent by viewModel.selectedStudentDetails.collectAsState()
    val attendancePercentage by viewModel.studentAttendancePercentage.collectAsState()
    val studentSkills by viewModel.studentSkills.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCourse?.className ?: "") },
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
                viewModel.loadStudentDetails(studentId) // Carrega detalhes, incluindo habilidades
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
                skills = studentSkills,
                onDismiss = {
                    viewModel.clearStudentDetails()
                    showStudentDetailsDialog = false
                },
                onEditSkills = {
                    onNavigateToSkills(student.studentId)
                    showStudentDetailsDialog = false // Fecha o dialog atual
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
                            fontWeight = FontWeight.Bold,
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
    skills: List<StudentSkill>,
    onDismiss: () -> Unit,
    onEditSkills: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes de ${student.name}") },
        text = {
            Column {
                Text("Matrícula: ${student.studentNumber}")
                Spacer(modifier = Modifier.height(8.dp))
                if (attendancePercentage != null) {
                    Text("Frequência: %.0f%%".format(attendancePercentage))
                } else {
                    Text("Frequência: Impossível", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Habilidades", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (skills.isEmpty()) {
                    Text("Nenhuma habilidade registrada.")
                } else {
                    skills.forEach { skill ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = skill.state.toIcon(),
                                contentDescription = skill.state.displayName,
                                tint = skill.state.toColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(skill.skillName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        dismissButton = {
            Button(onClick = onEditSkills) {
                Icon(Icons.Default.Edit, contentDescription = "Editar Habilidades", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Editar")
            }
        }
    )
}

fun SkillState.toIcon(): ImageVector {
    return when (this) {
        SkillState.ALTO -> Icons.Filled.ArrowUpward
        SkillState.MEDIO -> Icons.Filled.DragHandle
        SkillState.BAIXO -> Icons.Filled.ArrowDownward
        SkillState.NAO_SE_APLICA -> Icons.Filled.Remove
    }
}

@Composable
fun SkillState.toColor(): Color {
    return when (this) {
        SkillState.ALTO -> Color(0xFF4CAF50) // Verde
        SkillState.MEDIO -> Color(0xFFFFC107) // Amarelo
        SkillState.BAIXO -> MaterialTheme.colorScheme.error // Vermelho
        SkillState.NAO_SE_APLICA -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Cinza
    }
}
