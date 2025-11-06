package edu.jm.classsupervision.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd // Ícone alterado
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.classsupervision.model.Student
import edu.jm.classsupervision.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit
) {
    var showAddStudentDialog by remember { mutableStateOf(false) }
    val students by viewModel.studentsForClass.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedClass?.className ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                // O ícone e a descrição foram atualizados aqui
                Icon(
                    imageVector = Icons.Filled.PersonAdd, 
                    contentDescription = "Adicionar Aluno"
                )
            }
        }
    ) { paddingValues ->
        StudentsGrid(
            students = students,
            modifier = Modifier.padding(paddingValues)
        )
    }

    if (showAddStudentDialog) {
        AddStudentDialog(
            viewModel = viewModel,
            onDismiss = { showAddStudentDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentsGrid(students: List<Student>, modifier: Modifier = Modifier) {
    if (students.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum aluno cadastrado nesta turma.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students) { student ->
                Card(
                    modifier = Modifier.aspectRatio(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = student.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
