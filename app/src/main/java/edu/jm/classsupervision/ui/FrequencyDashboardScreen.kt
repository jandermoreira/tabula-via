package edu.jm.classsupervision.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.jm.classsupervision.model.AttendanceStatus
import edu.jm.classsupervision.model.ClassSession
import edu.jm.classsupervision.viewmodel.ClassViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FrequencyDashboardScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit,
    onStartNewAttendance: () -> Unit,
    onEditAttendance: (ClassSession) -> Unit
) {
    val classSessions by viewModel.classSessions.collectAsState()
    var selectedSessionForOptions by remember { mutableStateOf<ClassSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ClassSession?>(null) }
    var sessionToView by remember { mutableStateOf<ClassSession?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frequência") },
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
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onStartNewAttendance, modifier = Modifier.fillMaxWidth()) {
                Text("REGISTRAR FREQUÊNCIA", style = MaterialTheme.typography.titleMedium)
            }

            HorizontalDivider()

            if (classSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma frequência registrada para esta turma.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(classSessions) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onLongClick = { selectedSessionForOptions = session }, // Toque longo abre o dialog de opções
                                    onClick = { // Ação no clique simples
                                        scope.launch {
                                            viewModel.loadFrequencyDetails(session)
                                            sessionToView = session
                                        }
                                    }
                                )
                        ) {
                            Text(
                                text = "Frequência de ${session.timestamp.toFormattedDateString()}",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog de detalhes (apenas leitura)
    sessionToView?.let { session ->
        val details by viewModel.frequencyDetails.collectAsState()
        FrequencyDetailsDialog(
            session = session,
            details = details,
            onDismiss = {
                viewModel.clearFrequencyDetails()
                sessionToView = null
            }
        )
    }

    // Dialog de opções (Editar/Apagar)
    selectedSessionForOptions?.let { session ->
        AlertDialog(
            onDismissRequest = { selectedSessionForOptions = null },
            title = { Text("Opções de Frequência") },
            text = { Text("O que você deseja fazer com o registro de ${session.timestamp.toFormattedDateString()}?") },
            confirmButton = {
                TextButton(onClick = {
                    onEditAttendance(session)
                    selectedSessionForOptions = null
                }) {
                    Text("Editar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    sessionToDelete = session
                    selectedSessionForOptions = null
                }) {
                    Text("Apagar")
                }
            }
        )
    }

    // Dialog de Confirmação de Exclusão
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Esta ação é definitiva e não pode ser desfeita. Deseja apagar este registro de frequência?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteFrequencySession(session)
                    sessionToDelete = null
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FrequencyDetailsDialog(
    session: ClassSession,
    details: Map<String, AttendanceStatus>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes de Frequência") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Registro de: ${session.timestamp.toFormattedDateString()}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (details.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    LazyColumn {
                        items(details.entries.toList()) { (name, status) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name)
                                Text(status.displayName, color = if (status == AttendanceStatus.PRESENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

fun Long.toFormattedDateString(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
    return format.format(date)
}
