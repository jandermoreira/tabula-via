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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    groupNumber: Int,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.selectedGroupDetails.collectAsState()
    // var showAssignSkillsDialog by remember { mutableStateOf(false) } // Removido: Controle para o novo diálogo
    // var selectedStudentForSkillAssignment by remember { mutableStateOf<Student?>(null) } // Removido: Aluno selecionado para atribuir habilidades

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
                val studentDrawableResId = studentIconMap[student.studentId] ?: R.drawable.student_0 // Fallback
                StudentItem(
                    student = student,
                    drawableResId = studentDrawableResId // Passa o ID do drawable
                    // Removido: Modifier.clickable que abria o diálogo de habilidades
                    // modifier = Modifier.clickable { ... }
                )
            }
        }
    }

    // Diálogo de atribuição de habilidades removido completamente:
    /*
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
    */
}

// Removido o Composable AssignGroupSkillsDialog
/*
@Composable
fun AssignGroupSkillsDialog(
    student: Student,
    viewModel: CourseViewModel,
    onDismiss: () -> Unit
) {
    // ... conteúdo do diálogo ...
}
*/

// Removido o Composable SkillAssignmentRow
/*
@Composable
fun SkillAssignmentRow(
    skillName: String,
    currentLevel: SkillLevel,
    onLevelSelected: (SkillLevel) -> Unit
) {
    // ... conteúdo da linha de habilidade ...
}
*/
