package edu.jm.classsupervision.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.classsupervision.model.Class
import edu.jm.classsupervision.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClassListScreen(
    viewModel: ClassViewModel,
    onAddClassClicked: () -> Unit,
    onClassClicked: (Class) -> Unit,
    onBackupClicked: () -> Unit
) {
    val classes by viewModel.classes.collectAsState()
    val groupedClasses = classes.groupBy { it.academicYear }.toSortedMap(compareByDescending { it })

    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.userMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Turmas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(onClick = onBackupClicked) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Backup e Restauração")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClassClicked) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Adicionar Turma")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            groupedClasses.forEach { (year, classesInYear) ->
                stickyHeader {
                    Text(
                        text = year,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(classesInYear) {
                    aClass ->
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        ClassItem(aClass, onClick = { onClassClicked(aClass) })
                    }
                }
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Ícone de turma",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
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
}
