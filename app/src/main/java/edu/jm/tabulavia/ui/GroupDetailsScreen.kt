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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import edu.jm.tabulavia.R
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.BatteryFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    groupNumber: Int,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.selectedGroupDetails.collectAsState()
    var showAssignSkillsDialog by remember { mutableStateOf(false) } // Reintroduzindo o controle do diálogo
    var selectedStudentForSkillAssignment by remember { mutableStateOf<Student?>(null) } // Reintroduzindo o aluno selecionado

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
        // Obter o contexto para carregar os drawables
        val context = LocalContext.current

        // Pré-calcular o mapa de ícones para otimizar a rolagem
        val studentIconMap = remember(students, context) {
            students.associate { student ->
                val iconIndex = (student.studentId.mod(80L) + 1).toInt()
                val iconName = "student_${iconIndex}"
                val drawableResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                // Retorna o ID do recurso, ou R.drawable.student_0 como fallback se não encontrado
                student.studentId to (drawableResId.takeIf { it != 0 } ?: R.drawable.student_0)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students, key = { it.studentId }) { student ->
                // Busca o drawableResId pré-calculado para este aluno do mapa
                val studentDrawableResId = studentIconMap[student.studentId] ?: R.drawable.student_0
                StudentItem(
                    student = student,
                    drawableResId = studentDrawableResId,
                    modifier = Modifier.clickable {
                        selectedStudentForSkillAssignment = student
                        viewModel.loadStudentDetails(student.studentId)
                        showAssignSkillsDialog = true
                    }
                )
            }
        }
    }

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

@Composable
fun AssignGroupSkillsDialog(
    student: Student,
    viewModel: CourseViewModel,
    onDismiss: () -> Unit
) {
    val courseSkills by viewModel.courseSkills.collectAsState() // Precisamos das habilidades do curso para exibir
    // Não precisamos mais carregar studentSkillSummaries, pois vamos iniciar com N.A.

    // Estado local para gerenciar os níveis selecionados para cada habilidade, inicializando como NOT_APPLICABLE
    var skillLevels by remember {
        mutableStateOf(
            courseSkills.associate {
                // Para cada habilidade do curso, inicializa o nível como NOT_APLICABLE
                it.skillName to SkillLevel.NOT_APPLICABLE
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avaliar Habilidades para ${student.displayName}") }, // Título mais descritivo
        text = {
            Column {
                if (courseSkills.isEmpty()) {
                    Text("Nenhuma habilidade definida para esta turma.")
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
                // Mapeia as seleções para o formato que o ViewModel espera (Pair<String, SkillLevel>)
                // Filtra apenas as habilidades que foram realmente alteradas de N.A.
                val assessmentsToSave = skillLevels.entries
                    .filter { it.value != SkillLevel.NOT_APPLICABLE } // Apenas salva se for diferente de N.A.
                    .map { Pair(it.key, it.value) }
                
                // Chama o ViewModel para adicionar um NOVO registro de habilidade
                // Assumindo que o ViewModel tem uma função como addSkillAssessment
                // Se não existir, precisaremos adicioná-la ao ViewModel.
                // Exemplo: viewModel.addSkillAssessment(student.studentId, skillName, level, AssessmentSource.PROFESSOR_OBSERVATION)
                // Por enquanto, vamos adicionar uma chamada genérica que salva um único nível por habilidade.
                // Se você precisar salvar múltiplos níveis por habilidade, ajuste esta parte.
                if (assessmentsToSave.isNotEmpty()) {
                    // Como a função addSkillAssessment espera um único nível, vamos chamar ela para cada habilidade selecionada
                    assessmentsToSave.forEach { (skill, level) ->
                        viewModel.addSkillAssessment(
                            studentId = student.studentId,
                            skillName = skill,
                            level = level,
                            source = AssessmentSource.PROFESSOR_OBSERVATION // Assumindo que esta é uma avaliação do professor
                        )
                    }
                }
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

// Reintroduzindo o Composable SkillAssignmentRow (necessário para o diálogo) - AGORA COM ÍCONES!
@Composable
fun SkillAssignmentRow(
    skillName: String,
    currentLevel: SkillLevel,
    onLevelSelected: (SkillLevel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = skillName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // LOW (1 Estrela)
            IconButton(onClick = { onLevelSelected(SkillLevel.LOW) }) {
                Icon(
                    imageVector = Icons.Default.Battery0Bar,
                    contentDescription = SkillLevel.LOW.displayName,
                    tint = if (currentLevel.ordinal >= SkillLevel.LOW.ordinal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // MEDIUM (2 Estrelas)
            IconButton(onClick = { onLevelSelected(SkillLevel.MEDIUM) }) {
                Icon(
                    imageVector = Icons.Default.Battery4Bar,
                    contentDescription = SkillLevel.MEDIUM.displayName,
                    tint = if (currentLevel.ordinal >= SkillLevel.MEDIUM.ordinal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // HIGH (3 Estrelas)
            IconButton(onClick = { onLevelSelected(SkillLevel.HIGH) }) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = SkillLevel.HIGH.displayName,
                    tint = if (currentLevel.ordinal >= SkillLevel.HIGH.ordinal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            IconButton(onClick = { onLevelSelected(SkillLevel.NOT_APPLICABLE) }) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = SkillLevel.NOT_APPLICABLE.displayName,
                    tint = if (currentLevel == SkillLevel.NOT_APPLICABLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
