package edu.jm.tabulavia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Assessment // Ícone para Relatórios
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import edu.jm.tabulavia.viewmodel.CourseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDashboardScreen(
    classId: Long,
    viewModel: CourseViewModel,
    navController: NavController,
    onNavigateBack: () -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = classId) {
        viewModel.loadCourseDetails(classId)
    }

    val userMessage by viewModel.userMessage.collectAsState()
    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.onUserMessageShown()
            }
        }
    }

    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val students by viewModel.studentsForClass.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val courseSkills by viewModel.courseSkills.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val titleText = selectedCourse?.let {
                        "${it.className} ${it.academicYear}/${it.period}"
                    } ?: "Carregando..."
                    Text(titleText)
                },
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
        // Coluna principal que contém todos os elementos, com scroll
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(), // Garante que a coluna ocupe o espaço disponível
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espaçamento entre os itens da coluna
        ) {
            // --- Cards Existentes (Alunos, Frequência, Atividades) ---
            // Estes cards continuam na ordem original dentro da coluna principal
            Column(
                modifier = Modifier.fillMaxWidth(), // Ocupa a largura disponível
                verticalArrangement = Arrangement.spacedBy(16.dp) // Espaçamento entre estes cards
            ) {
                DashboardCard(
                    title = "Alunos",
                    subtitle = "${students.size} alunos cadastrados",
                    icon = Icons.Default.Group,
                    onClick = { navController.navigate("studentList/$classId") }
                )

                DashboardCard(
                    title = "Frequência",
                    subtitle = "Histórico de frequência",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    onClick = { navController.navigate("frequencyDashboard/$classId") }
                )

                DashboardCard(
                    title = "Atividades",
                    subtitle = "${activities.size} atividades criadas",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    onClick = { navController.navigate("activityList/$classId") }
                )
            }
            
            // --- Novo Grid Independente para Habilidades e Relatórios ---
            Spacer(modifier = Modifier.height(16.dp)) // Espaço entre os cards existentes e o novo grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Define 2 colunas fixas para este grid
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp), // Define uma altura máxima para que o scroll interno funcione se necessário
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Espaço entre colunas
                verticalArrangement = Arrangement.spacedBy(16.dp)  // Espaço entre linhas
            ) {
                // Card de Habilidades (Movido para cá)
                item { 
                    DashboardCard(
                        title = "Habilidades",
                        subtitle = "${courseSkills.size} habilidades definidas",
                        icon = Icons.Default.Psychology,
                        onClick = { navController.navigate("courseSkills/$classId") }
                    )
                }

                // Card de Relatórios
                item { 
                    DashboardCard(
                        title = "Relatórios",
                        subtitle = "Análise e desempenho",
                        icon = Icons.Default.Assessment, // Usando o ícone de Assessment
                        onClick = { /* TODO: Implementar navegação para a tela de Relatórios */ }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
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
