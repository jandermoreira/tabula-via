package edu.jm.tabulavia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.viewmodel.CourseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListScreen(
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit,
    onActivityClicked: (Activity) -> Unit
) {
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val activities by viewModel.activities.collectAsState()
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var showSkillLogDialog by remember { mutableStateOf(false) } // Novo estado para o log de habilidades
    val skillAssessmentLog by viewModel.skillAssessmentLog.collectAsState() // Coleta o log do ViewModel
    val studentsInClass by viewModel.studentsForClass.collectAsState() // Coleta a lista de alunos uma vez

    LaunchedEffect(showSkillLogDialog) {
        if (showSkillLogDialog) {
            viewModel.loadSkillAssessmentLog() // Carrega o log quando o diálogo for exibido
            // Removed: viewModel.loadStudentsForClass(classId) as students are loaded with loadCourseDetails
        }
    }

    if (showAddActivityDialog) {
        AddActivityDialog(viewModel = viewModel) { showAddActivityDialog = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = selectedCourse?.let {
                        "${it.className} ${it.academicYear}/${it.period} - Atividades"
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FAB para adicionar atividade (existente)
                FloatingActionButton(onClick = { showAddActivityDialog = true }) {
                    Icon(Icons.Default.PostAdd, contentDescription = "Adicionar Atividade")
                }

                // NOVO FAB temporário para o log de habilidades
                FloatingActionButton(onClick = { showSkillLogDialog = true }) {
                    Icon(Icons.Default.Description, contentDescription = "Log de Habilidades")
                }
            }
        }
    ) { paddingValues ->
        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma atividade cadastrada.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activities) { activity ->
                    ActivityItem(activity = activity, onActivityClicked = onActivityClicked)
                }
            }
        }

        // Diálogo para exibir o log de habilidades
        if (showSkillLogDialog) {
            AlertDialog(
                onDismissRequest = { showSkillLogDialog = false },
                title = { Text("Log de Avaliações de Habilidades") },
                text = {
                    if (skillAssessmentLog.isEmpty()) {
                        Text("Nenhum registro de avaliação de habilidade encontrado.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(skillAssessmentLog) { assessment ->
                                // Busca o aluno pelo ID para exibir o nome
                                val studentName = studentsInClass.find { it.studentId == assessment.studentId }?.displayName ?: "Aluno Desconhecido"
                                val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(assessment.timestamp))
                                Text(
                                    text = "$formattedDate - $studentName - ${assessment.skillName}: ${assessment.level.displayName} (${assessment.source.displayName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSkillLogDialog = false }) {
                        Text("Fechar")
                    }
                }
            )
        }
    }
}

@Composable
fun ActivityItem(activity: Activity, onActivityClicked: (Activity) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActivityClicked(activity) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = activity.title, style = MaterialTheme.typography.titleMedium)
            Text(text = activity.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
