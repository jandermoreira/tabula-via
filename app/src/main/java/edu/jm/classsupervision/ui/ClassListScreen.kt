package edu.jm.classsupervision.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.classsupervision.model.Class
import edu.jm.classsupervision.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassListScreen(
    viewModel: ClassViewModel,
    onAddClassClicked: () -> Unit,
    onClassClicked: (Class) -> Unit // Ação para quando uma turma for clicada
) {
    // Coleta o estado da lista de turmas do ViewModel.
    // O `collectAsState` garante que a UI será redesenhada quando a lista mudar.
    val classes by viewModel.classes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Turmas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClassClicked) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Turma")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            items(classes) { aClass ->
                ClassItem(aClass, onClick = { onClassClicked(aClass) })
            }
        }
    }
}

@Composable
fun ClassItem(aClass: Class, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = aClass.className,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${aClass.academicYear}/${aClass.period}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
    