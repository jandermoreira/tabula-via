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

// Data class para armazenar as avaliações por pares antes da agregação
private data class PeerAssessmentData(
    val evaluatedStudentId: Long,
    val skillName: String,
    val skillValue: Int, // 1=LOW, 2=MEDIUM, 3=HIGH
    val timestamp: Long
)

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
    var showSkillLogDialog by remember { mutableStateOf(false) }
    var showBatchSkillEntryDialog by remember { mutableStateOf(false) }

    val skillAssessmentLog by viewModel.skillAssessmentLog.collectAsState()
    val studentsInClass by viewModel.studentsForClass.collectAsState()
    val courseSkills by viewModel.courseSkills.collectAsState()

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
        }
        else {
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

        // NOVO: Diálogo para entrada de habilidades em lote
        if (showBatchSkillEntryDialog) {
            BatchSkillEntryDialog(
                viewModel = viewModel,
                onDismiss = { showBatchSkillEntryDialog = false },
                onSaveBatch = { pastedText ->
                    Log.d("BatchEntryCheck", "onSaveBatch called")

                    val peerAssessmentsToAggregate = mutableListOf<PeerAssessmentData>() // Usando a nova data class

                    pastedText.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            val fields = line.split('	').map { it.trim() }
                            if (fields.size >= 3 + courseSkills.size) { // Garante que há campos suficientes
                                try {
                                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                    val date = format.parse(fields[0])
                                    // Usa o timestamp extraído do arquivo, ou 0L se falhar.
                                    val timestamp = date?.time ?: 0L

                                    val evaluatorId = fields[1].toLongOrNull()
                                    val evaluatedId = fields[2].toLongOrNull()

                                    if (evaluatorId != null && evaluatedId != null) {
                                        if (evaluatorId == evaluatedId) {
                                            // É uma autoavaliação (lógica existente)
                                            val student = studentsInClass.find { it.studentNumber == evaluatedId.toString() } // Usando studentNumber para a busca
                                            if (student != null) {
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
                                                            source = AssessmentSource.SELF_ASSESSMENT,
                                                            timestamp = timestamp
                                                        )
                                                        Log.d("BatchEntry", "Autoavaliação salva para ${student.displayName}: ${skill.skillName} -> $skillLevel")
                                                    }
                                                }
                                            } else {
                                                Log.d("StudentCheck", "Aluno com evaluatedId (studentNumber provável) $evaluatedId não encontrado na lista de alunos da turma.")
                                            }
                                        } else {
                                            // É uma avaliação entre pares, coletar para agregação
                                            val evaluatorStudent = studentsInClass.find { it.studentNumber == evaluatorId.toString() }
                                            val evaluatedStudent = studentsInClass.find { it.studentNumber == evaluatedId.toString() }

                                            if (evaluatorStudent != null && evaluatedStudent != null) {
                                                courseSkills.forEachIndexed { index, skill ->
                                                    val skillValue = fields[3 + index].toIntOrNull()
                                                    if (skillValue != null && skillValue in 1..3) { // Garantir que o valor é válido
                                                        peerAssessmentsToAggregate.add(
                                                            PeerAssessmentData(
                                                                evaluatedStudentId = evaluatedStudent.studentId,
                                                                skillName = skill.skillName,
                                                                skillValue = skillValue,
                                                                timestamp = timestamp
                                                            )
                                                        )
                                                    }
                                                }
                                            } else {
                                                Log.d("StudentCheck", "Aluno avaliador ($evaluatorId) ou aluno avaliado ($evaluatedId) não encontrado para avaliação por pares.")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("BatchEntry", "Erro ao processar linha: '${line}'", e)
                                }
                            }
                            else {
                                Log.w("BatchEntry", "Linha ignorada por ter campos insuficientes: '${line}'")
                            }
                        }
                    }

                    // Processar e agregar avaliações por pares após todas as linhas serem lidas
                    val aggregatedPeerAssessments = peerAssessmentsToAggregate
                        .groupBy { it.evaluatedStudentId } // Agrupar por evaluatedStudentId
                        .mapValues { (_, assessmentsByStudent) ->
                            assessmentsByStudent.groupBy { it.skillName } // Agrupar por skillName
                                .mapValues { (_, assessmentsForSkill) ->
                                    Pair(
                                        assessmentsForSkill.map { it.skillValue },
                                        assessmentsForSkill.map { it.timestamp }
                                    )
                                }
                        }

                    aggregatedPeerAssessments.forEach { (evaluatedStudentId, skillsMap) ->
                        val evaluatedStudent = studentsInClass.find { it.studentId == evaluatedStudentId }
                        if (evaluatedStudent != null) {
                            skillsMap.forEach { (skillName, skillData) ->
                                val (skillValues, timestamps) = skillData
                                if (skillValues.isNotEmpty()) {
                                    val averageSkillValue = skillValues.average()
                                    val latestTimestamp = timestamps.maxOrNull() ?: 0L

                                    val averagedSkillLevel = when {
                                        averageSkillValue <= 1.5 -> SkillLevel.LOW
                                        averageSkillValue <= 2.5 -> SkillLevel.MEDIUM
                                        else -> SkillLevel.HIGH
                                    }
                                    viewModel.addSkillAssessment(
                                        studentId = evaluatedStudentId,
                                        skillName = skillName,
                                        level = averagedSkillLevel,
                                        source = AssessmentSource.PEER_ASSESSMENT,
                                        timestamp = latestTimestamp // O timestamp da agregação é o mais recente
                                    )
                                    Log.d("BatchEntry", "Avaliação por pares agregada salva para ${evaluatedStudent.displayName} em $skillName -> $averagedSkillLevel (Média: $averageSkillValue, Timestamp Mais Recente: $latestTimestamp)")
                                }
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