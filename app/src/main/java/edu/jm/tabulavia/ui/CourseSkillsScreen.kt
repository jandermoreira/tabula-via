package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.CourseSkill
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSkillsScreen(
    courseId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val courseSkills by viewModel.courseSkills.collectAsState()
    var showAddSkillDialog by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        viewModel.loadSkillsForCourse(courseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habilidades'") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSkillDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Habilidade")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(courseSkills) { skill ->
                CourseSkillItem(skill, onDelete = {
                    viewModel.deleteCourseSkill(skill)
                })
            }
        }
    }

    if (showAddSkillDialog) {
        AddSkillDialog(
            viewModel = viewModel,
            onDismiss = { showAddSkillDialog = false }
        )
    }
}

@Composable
private fun CourseSkillItem(skill: CourseSkill, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = skill.skillName, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir Habilidade")
            }
        }
    }
}

@Composable
private fun AddSkillDialog(viewModel: CourseViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Habilidade") },
        text = {
            Column {
                OutlinedTextField(
                    value = viewModel.skillName,
                    onValueChange = { viewModel.skillName = it },
                    label = { Text("Nome da Habilidade") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                viewModel.addCourseSkill(onDismiss)
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
