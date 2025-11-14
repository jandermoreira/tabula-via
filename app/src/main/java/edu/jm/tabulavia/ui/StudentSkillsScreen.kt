package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.SkillAssessmentsSummary
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSkillsScreen(
    studentId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val student by viewModel.selectedStudentDetails.collectAsState()
    val skillSummaries by viewModel.studentSkillSummaries.collectAsState()
    val courseSkills by viewModel.courseSkills.collectAsState() // Still useful for displaying all skills

    // Filter skillSummaries to show only skills that exist in the courseSkills
    val filteredSkillSummaries = remember(skillSummaries, courseSkills) {
        val validSkillNames = courseSkills.map { it.skillName }.toSet()
        skillSummaries.filterKeys { it in validSkillNames }.values.toList()
            .sortedBy { it.skillName } // Optional: keep them sorted
    }

    LaunchedEffect(studentId) {
        viewModel.loadStudentDetails(studentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habilidades de ${student?.name ?: "..."}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
                // Removed actions for save button
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSkillSummaries, key = { it.skillName }) { skillSummary ->
                SkillItemRow(
                    studentId = studentId,
                    skillSummary = skillSummary,
                    onAssessmentChange = { level, source ->
                        viewModel.addSkillAssessment(studentId, skillSummary.skillName, level, source)
                    }
                )
            }
        }
    }
}

@Composable
fun SkillItemRow(
    studentId: Long,
    skillSummary: SkillAssessmentsSummary,
    onAssessmentChange: (SkillLevel, AssessmentSource) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = skillSummary.skillName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            // Professor Observation
            AssessmentSourceRow(
                studentId = studentId,
                skillName = skillSummary.skillName,
                source = AssessmentSource.PROFESSOR_OBSERVATION,
                currentLevel = skillSummary.professorAssessment?.level,
                onAssessmentChange = onAssessmentChange
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Self-Assessment
            AssessmentSourceRow(
                studentId = studentId,
                skillName = skillSummary.skillName,
                source = AssessmentSource.SELF_ASSESSMENT,
                currentLevel = skillSummary.selfAssessment?.level,
                onAssessmentChange = onAssessmentChange
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Peer Assessment
            AssessmentSourceRow(
                studentId = studentId,
                skillName = skillSummary.skillName,
                source = AssessmentSource.PEER_ASSESSMENT,
                currentLevel = skillSummary.peerAssessment?.level,
                onAssessmentChange = onAssessmentChange
            )
        }
    }
}

@Composable
fun AssessmentSourceRow(
    studentId: Long,
    skillName: String,
    source: AssessmentSource,
    currentLevel: SkillLevel?,
    onAssessmentChange: (SkillLevel, AssessmentSource) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = source.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(currentLevel?.displayName ?: "Avaliar")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SkillLevel.entries.filter { it != SkillLevel.NOT_APPLICABLE }.forEach { level -> // Exclude NOT_APPLICABLE from direct selection
                    DropdownMenuItem(
                        text = { Text(level.displayName) },
                        onClick = {
                            onAssessmentChange(level, source)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}