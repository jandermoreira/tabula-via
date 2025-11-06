package edu.jm.classsupervision.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import edu.jm.classsupervision.viewmodel.ClassViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDashboardScreen(
    classId: Long,
    viewModel: ClassViewModel,
    navController: NavController,
    onNavigateBack: () -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = classId) {
        viewModel.loadClassDetails(classId)
    }

    val userMessage by viewModel.userMessage.collectAsState()
    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.userMessageShown()
            }
        }
    }

    val selectedClass by viewModel.selectedClass.collectAsState()
    val students by viewModel.studentsForClass.collectAsState()
    val activitiesCount = 0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(selectedClass?.className ?: "Carregando...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para a lista de turmas"
                        )
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
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardCard(
                title = "Alunos",
                subtitle = "${students.size} alunos cadastrados",
                onClick = { navController.navigate("studentList/$classId") }
            )

            DashboardCard(
                title = "Frequência",
                subtitle = "Histórico de frequência",
                onClick = { navController.navigate("frequencyDashboard/$classId") }
            )

            DashboardCard(
                title = "Atividades",
                subtitle = "$activitiesCount atividades criadas",
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
fun DashboardCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Acessar $title",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
