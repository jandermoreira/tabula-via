package edu.jm.classsupervision.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.classsupervision.model.Activity
import edu.jm.classsupervision.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit
) {
    val activities by viewModel.activities.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atividades") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Abrir dialog de adicionar atividade */ }) {
                Icon(Icons.Default.PostAdd, contentDescription = "Adicionar Atividade")
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
                    ActivityItem(activity = activity)
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: Activity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = activity.title, style = MaterialTheme.typography.titleMedium)
            Text(text = activity.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
