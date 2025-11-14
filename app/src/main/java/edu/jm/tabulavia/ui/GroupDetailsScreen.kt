package edu.jm.tabulavia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    groupNumber: Int,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.selectedGroupDetails.collectAsState()
    var showAssignSkillsDialog by remember { mutableStateOf(false) } // Controle para o novo diálogo
    var selectedStudentForSkillAssignment by remember { mutableStateOf<Student?>(null) } // Aluno selecionado para atribuir habilidades

    LaunchedEffect(groupNumber) {
        viewModel.loadGroupDetails(groupNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupo $groupNumber") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearGroupDetails()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // O botão de psicologia pode ser removido ou ter outra função se desejar
                    IconButton(onClick = { /* TODO: Nova ação se necessário */ }) {
                        Icon(Icons.Default.Psychology, contentDescription = "Informações do Grupo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students, key = { it.studentId }) { student ->
                StudentItem(
                    student = student,
                    modifier = Modifier.clickable {
                        selectedStudentForSkillAssignment = student
                        viewModel.loadStudentDetails(student.studentId) // Carregar detalhes para o diálogo
                        showAssignSkillsDialog = true
                    }
                )
            }
        }
    }

    // Novo diálogo para atribuir habilidades
    if (showAssignSkillsDialog) {
        selectedStudentForSkillAssignment?.let { student ->
            AssignGroupSkillsDialog(
                student = student,
                viewModel = viewModel,
                onDismiss = {
                    showAssignSkillsDialog = false
                    selectedStudentForSkillAssignment = null
                }
            )
        }
    }
}

// Novo Composable para o diálogo de atribuição de habilidades
@Composable
fun AssignGroupSkillsDialog(
    student: Student,
    viewModel: CourseViewModel,
    onDismiss: () -> Unit
) {
    val courseSkills by viewModel.courseSkills.collectAsState()
    val studentSkillSummaries by viewModel.studentSkillSummaries.collectAsState()

    // Estado local para gerenciar os níveis selecionados para cada habilidade
    var skillLevels by remember {
        mutableStateOf(
            courseSkills.associate { skill ->
                val currentLevel = studentSkillSummaries[skill.skillName]?.professorAssessment?.level
                skill.skillName to (currentLevel ?: SkillLevel.NOT_APPLICABLE)
            }
        )
    }

    // Atualiza o estado local se as avaliações do ViewModel mudarem
    LaunchedEffect(studentSkillSummaries) {
        skillLevels = courseSkills.associate { skill ->
            val currentLevel = studentSkillSummaries[skill.skillName]?.professorAssessment?.level
            skill.skillName to (currentLevel ?: SkillLevel.NOT_APPLICABLE)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.displayName ) },
        text = {
            Column {
                if (courseSkills.isEmpty()) {
                    Text("Nenhuma habilidade definida")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp) // Limita a altura do scroll
                    ) {
                        items(courseSkills, key = { it.skillName }) { courseSkill ->
                            SkillAssignmentRow(
                                skillName = courseSkill.skillName,
                                currentLevel = skillLevels[courseSkill.skillName] ?: SkillLevel.NOT_APPLICABLE,
                                onLevelSelected = { newLevel ->
                                    skillLevels = skillLevels.toMutableMap().apply {
                                        this[courseSkill.skillName] = newLevel
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val assessmentsToSave = skillLevels.entries
                    .filter { it.value != SkillLevel.NOT_APPLICABLE } // Ignora as que não foram avaliadas
                    .map { Pair(it.key, it.value) }
                viewModel.addProfessorSkillAssessments(student.studentId, assessmentsToSave)
                onDismiss()
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Composable auxiliar para uma linha de habilidade dentro do diálogo
@Composable
fun SkillAssignmentRow(
    skillName: String,
    currentLevel: SkillLevel,
    onLevelSelected: (SkillLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = skillName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(currentLevel.displayName)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                (SkillLevel.entries.toList() - SkillLevel.NOT_APPLICABLE).forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.displayName) },
                        onClick = {
                            onLevelSelected(level)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
