package edu.jm.tabulavia.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.viewmodel.CourseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showBatchSkillEntryDialog by remember { mutableStateOf(false) } // NOVO: Estado para o diálogo de entrada em lote

    val skillAssessmentLog by viewModel.skillAssessmentLog.collectAsState() // Coleta o log do ViewModel
    val studentsInClass by viewModel.studentsForClass.collectAsState() // Coleta a lista de alunos uma vez
    val courseSkills by viewModel.courseSkills.collectAsState() // Pega as skills do curso

    LaunchedEffect(showSkillLogDialog) {
        if (showSkillLogDialog) {
            viewModel.loadSkillAssessmentLog()
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

                // FAB para o Log de Habilidades
                FloatingActionButton(onClick = { showSkillLogDialog = true }) {
                    Icon(Icons.Default.Description, contentDescription = "Log de Habilidades")
                }

                // NOVO FAB para entrada de habilidades em lote
                FloatingActionButton(onClick = { showBatchSkillEntryDialog = true }) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = "Entrada de Habilidades em Lote"
                    )
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

        // Diálogo para exibir o log de habilidades (existente)
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
                                val studentName =
                                    studentsInClass.find { it.studentId == assessment.studentId }?.displayName
                                        ?: "Aluno Desconhecido"
                                val formattedDate = SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date(assessment.timestamp))
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

        // NOVO: Diálogo para entrada de habilidades em lote SIMPLIFICADO
        if (showBatchSkillEntryDialog) {
            BatchSkillEntryDialog(
                viewModel = viewModel,
                onDismiss = { showBatchSkillEntryDialog = false },
                onSaveBatch = { pastedText ->
                    Log.d("BatchEntryCheck", "onSaveBatch called") // Log para verificar se onSaveBatch é chamado
                    pastedText.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            val fields = line.split('\t').map { it.trim() }
                            if (fields.size >= 3 + courseSkills.size) { // Garante que há campos suficientes
                                try {
                                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                    val date = format.parse(fields[0])
                                    val timestamp = date?.time ?: System.currentTimeMillis()

                                    val evaluatorId = fields[1].toLongOrNull()
                                    val evaluatedId = fields[2].toLongOrNull()

//                                    // Log para verificar evaluatorId e evaluatedId
//                                    Log.d("BatchEntry", "EvaluatorId: $evaluatorId, EvaluatedId: $evaluatedId")
//                                    if (evaluatorId == null || evaluatedId == null) {
//                                        Log.w("BatchEntry", "Erro: evaluatorId ou evaluatedId não são números válidos na linha: '$line'")
//                                    }

                                    if (evaluatorId != null && evaluatedId != null && evaluatorId == evaluatedId) {
                                        // É uma autoavaliação
//                                        // Log para verificar o studentId e o conteúdo de studentsInClass
//                                        Log.d("StudentCheck", "Number of students in class: ${studentsInClass.size}")
//                                        if (studentsInClass.isNotEmpty()) {
//                                            Log.d("StudentCheck", "First studentId: ${studentsInClass.first().studentId}")
//                                            // Adicionar um log para verificar o evaluatedId antes de tentar encontrar o student
//                                            Log.d("StudentCheck", "Attempting to find student with ID: $evaluatedId")
//                                        }

//                                        // Log para exibir todos os studentIds da lista studentsInClass
//                                        if (studentsInClass.isNotEmpty()) {
//                                            val studentIds = studentsInClass.joinToString(", ") { it.studentId.toString() }
//                                            Log.d("StudentCheck", "All studentIds in studentsInClass: [$studentIds]")
//                                        }

                                        val student = studentsInClass.find { it.studentNumber == evaluatedId?.toString() } // Usando studentNumber para a busca
                                        if (student != null) {
//                                            Log.d("StudentCheck", "Aluno encontrado: ${student.displayName} com studentNumber ${student.studentNumber}")
                                            courseSkills.forEachIndexed { index, skill ->
                                                val skillValue = fields[3 + index].toIntOrNull()
                                                val skillLevel = when (skillValue) {
                                                    1 -> SkillLevel.LOW
                                                    2 -> SkillLevel.MEDIUM
                                                    3 -> SkillLevel.HIGH
                                                    else -> null
                                                }

                                                if (skillLevel != null) {
                                                    viewModel.addSkillAssessment(
                                                        studentId = student.studentId,
                                                        skillName = skill.skillName,
                                                        level = skillLevel,
                                                        source = AssessmentSource.SELF_ASSESSMENT
                                                        // timestamp = timestamp // Removido o parâmetro timestamp
                                                    )
                                                    Log.d("BatchEntry", "Autoavaliação salva para ${student.displayName}: ${skill.skillName} -> $skillLevel")
                                                }
                                            }
                                        } else {
                                            Log.w("StudentCheck", "Aluno com evaluatedId (studentNumber provável) $evaluatedId não encontrado na lista de alunos da turma.")
                                        }
                                    }

                                } catch (e: Exception) {
                                    Log.e("BatchEntry", "Erro ao processar linha: '$line'", e)
                                }
                            } else {
                                Log.w("BatchEntry", "Linha ignorada por ter campos insuficientes: '$line'")
                            }
                        }
                    }
                    showBatchSkillEntryDialog = false
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

// --- NOVO: Composable para o diálogo de entrada de habilidades em lote SIMPLIFICADO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchSkillEntryDialog(
    viewModel: CourseViewModel,
    onDismiss: () -> Unit,
    onSaveBatch: (String) -> Unit // Agora só recebe o texto colado
) {
    var pastedStudentData by remember { mutableStateOf("") }
    val courseSkills by viewModel.courseSkills.collectAsState()
    val labelText = "- Horário\n- Nº do avaliador\n- Nº do avaliado\n- " + courseSkills.joinToString("\n- ") { it.skillName.replace(" ", "-") }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habilidades (feita pelos estudantes)") },
        text = {
            Column {
                Text(
                    "Dados esperados por linha (nesta ordem):\n" + labelText,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pastedStudentData,
                    onValueChange = { pastedStudentData = it },
                    label = { Text("Cole os dados aqui") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveBatch(pastedStudentData)
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
