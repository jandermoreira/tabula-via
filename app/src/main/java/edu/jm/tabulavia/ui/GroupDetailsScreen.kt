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
import androidx.compose.ui.platform.LocalContext // Importar LocalContext
import androidx.compose.ui.res.painterResource
import edu.jm.tabulavia.R // Importar R
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.draw.alpha

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
                val drawableResId =
                    context.resources.getIdentifier(iconName, "drawable", context.packageName)
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

    // Estado local para gerenciar os níveis selecionados para cada habilidade, inicializando como NOT_APPLICABLE
    var skillLevels by remember {
        mutableStateOf(
            courseSkills.associate {
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
                                },
                                // O parâmetro isSelected foi removido, a lógica de alpha será aplicada diretamente aqui
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val assessmentsToSave = skillLevels.entries
                    .filter { it.value != SkillLevel.NOT_APPLICABLE } // Apenas salva se for diferente de N.A.
                    .map { Pair(it.key, it.value) }
                
                if (assessmentsToSave.isNotEmpty()) {
                    assessmentsToSave.forEach { (skill, level) ->
                        viewModel.addSkillAssessment(
                            studentId = student.studentId,
                            skillName = skill,
                            level = level,
                            source = AssessmentSource.PROFESSOR_OBSERVATION // Adicionado o parâmetro 'source' corretamente
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Adicionando padding para agrupar melhor os ícones e evitar que fiquem grudados nas bordas
            modifier = Modifier.padding(vertical = 1.dp, horizontal = 0.dp)
        ) {
            // Ícone para NOT_APPLICABLE
            IconButton(onClick = { onLevelSelected(SkillLevel.NOT_APPLICABLE) }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = SkillLevel.NOT_APPLICABLE.displayName,
                    // Tinta: Primária se selecionado, cinza suave se não selecionado
                    tint = if (currentLevel == SkillLevel.NOT_APPLICABLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    // Alfa: 1f se selecionado, 0.7f se não selecionado
                    modifier = Modifier.alpha(if (currentLevel == SkillLevel.NOT_APPLICABLE) 1f else 0.7f) 
                )
            }

            // Ícone para LOW
            IconButton(onClick = { onLevelSelected(SkillLevel.LOW) }) {
                Icon(
                    imageVector = Icons.Default.ThumbDown, // Usando ThumbDown para LOW
                    contentDescription = SkillLevel.LOW.displayName,
                    // Tinta: Cor de erro (vermelho) se selecionado, cinza suave se não
                    tint = if (currentLevel == SkillLevel.LOW) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    // Alfa: 1f se selecionado, 0.7f se não selecionado
                    modifier = Modifier.alpha(if (currentLevel == SkillLevel.LOW) 1f else 0.7f)
                )
            }

            // Ícone para MEDIUM
            IconButton(onClick = { onLevelSelected(SkillLevel.MEDIUM) }) {
                Icon(
                    imageVector = Icons.Default.Circle, // Usando Circle para MEDIUM
                    contentDescription = SkillLevel.MEDIUM.displayName,
                    // Tinta: Laranja se selecionado, cinza suave se não
                    tint = if (currentLevel == SkillLevel.MEDIUM) Color(0xFFFFA500) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    // Alfa: 1f se selecionado, 0.7f se não selecionado
                    modifier = Modifier.alpha(if (currentLevel == SkillLevel.MEDIUM) 1f else 0.7f)
                )
            }

            // Ícone para HIGH
            IconButton(onClick = { onLevelSelected(SkillLevel.HIGH) }) {
                Icon(
                    imageVector = Icons.Default.ThumbUp, // Usando ThumbUp para HIGH
                    contentDescription = SkillLevel.HIGH.displayName,
                    // Tinta: Verde se selecionado, cinza suave se não
                    tint = if (currentLevel == SkillLevel.HIGH) Color(0xFF008000) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    // Alfa: 1f se selecionado, 0.7f se não selecionado
                    modifier = Modifier.alpha(if (currentLevel == SkillLevel.HIGH) 1f else 0.7f),
                )
            }
        }
    }
}
