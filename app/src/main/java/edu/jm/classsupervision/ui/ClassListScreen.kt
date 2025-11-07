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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.classsupervision.model.Class
import edu.jm.classsupervision.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassListScreen(
    viewModel: ClassViewModel,
    onAddClassClicked: () -> Unit,
    onClassClicked: (Class) -> Unit,
    onBackupClicked: () -> Unit // Novo callback
) {
    val classes by viewModel.classes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Turmas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    // Botão para navegar para a tela de backup
                    IconButton(onClick = onBackupClicked) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Backup e Restauração")
                    }
                }
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
