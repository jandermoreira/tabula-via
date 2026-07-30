package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import edu.jm.tabulavia.model.ClassSkill
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSkillsScreen(
    classId: String,
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit
) {
    MessageHandler(viewModel)

    val classSkills by viewModel.classSkills.collectAsState()
    var showAddSkillDialog by remember { mutableStateOf(false) }

    LaunchedEffect(classId) {
        viewModel.loadSkillsForClass(classId)
    }

    Scaffold(
        topBar = {
            TabulaTopBar(
                title = "Habilidades",
                viewModel = viewModel,
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
            items(classSkills) { skill ->
                ClassSkillItem(skill, onDelete = {
                    viewModel.deleteClassSkill(skill)
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
private fun ClassSkillItem(skill: ClassSkill, onDelete: () -> Unit) {
    TabulaCard(modifier = Modifier.fillMaxWidth()) {
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
private fun AddSkillDialog(viewModel: ClassViewModel, onDismiss: () -> Unit) {
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
                viewModel.addClassSkill(onDismiss)
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