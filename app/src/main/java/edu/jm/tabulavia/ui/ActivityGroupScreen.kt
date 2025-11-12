package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityGroupScreen(
    activityId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.studentsForClass.collectAsState()
    val activity by viewModel.selectedActivity.collectAsState()
    var selectedOption by remember { mutableStateOf("alunosPorGrupo") }
    var groupSize by remember { mutableStateOf("") }
    var numberOfGroups by remember { mutableStateOf("") }

    LaunchedEffect(activityId) {
        viewModel.loadActivityDetails(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.title ?: "Montar Grupos") },
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
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedOption == "alunosPorGrupo",
                    onClick = { selectedOption = "alunosPorGrupo" }
                )
                Text("Nº de alunos por grupo")
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = selectedOption == "numeroDeGrupos",
                    onClick = { selectedOption = "numeroDeGrupos" }
                )
                Text("Nº de grupos")
            }

            if (selectedOption == "alunosPorGrupo") {
                OutlinedTextField(
                    value = groupSize,
                    onValueChange = { groupSize = it },
                    label = { Text("Alunos por grupo") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = numberOfGroups,
                    onValueChange = { numberOfGroups = it },
                    label = { Text("Número de grupos") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = { /* TODO: Implement group creation logic */ }) {
                Text("Criar Grupos")
            }

            Spacer(Modifier.height(16.dp))

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum aluno encontrado.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students, key = { it.studentId }) { student ->
                        StudentItem(student = student)
                    }
                }
            }
        }
    }
}
