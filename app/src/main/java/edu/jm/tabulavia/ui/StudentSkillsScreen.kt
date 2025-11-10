package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.SkillState
import edu.jm.tabulavia.model.StudentSkill
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSkillsScreen(
    studentId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val student by viewModel.selectedStudentDetails.collectAsState()
    val skills by viewModel.studentSkills.collectAsState()
    var localSkills by remember { mutableStateOf<List<StudentSkill>>(emptyList()) }

    LaunchedEffect(studentId) {
        viewModel.loadStudentDetails(studentId) // Para obter o nome do aluno para o título
        viewModel.loadSkillsForStudent(studentId)
    }

    // Quando as habilidades são carregadas do viewModel, atualiza a cópia local
    LaunchedEffect(skills) {
        localSkills = skills
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habilidades de ${student?.name ?: "..."}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.updateStudentSkills(localSkills)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Save, "Salvar Habilidades")
                    }
                }
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
            items(localSkills) { skill ->
                SkillItemRow(
                    skill = skill,
                    onStateChange = { newState ->
                        val updatedList = localSkills.map {
                            if (it.skillName == skill.skillName) {
                                it.copy(state = newState)
                            } else {
                                it
                            }
                        }
                        localSkills = updatedList
                    }
                )
            }
        }
    }
}

@Composable
fun SkillItemRow(
    skill: StudentSkill,
    onStateChange: (SkillState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = skill.skillName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(skill.state.displayName)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Acesso ao Enum ajustado para .entries
                    SkillState.entries.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state.displayName) },
                            onClick = {
                                onStateChange(state)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
