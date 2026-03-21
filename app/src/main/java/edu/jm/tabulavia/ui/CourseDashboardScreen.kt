/**
 * File: CourseDashboardScreen.kt
 * Description: UI components for the course dashboard, displaying students, activities, skills, and reports.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.CourseViewModel

/**
 * Main screen for the course dashboard.
 * * @param classId The unique identifier of the class.
 * @param viewModel The ViewModel handling course logic.
 * @param navController Controller for app navigation.
 * @param onNavigateBack Callback for the back navigation action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDashboardScreen(
    classId: String,
    viewModel: CourseViewModel,
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    // Handle specific view model messages
    MessageHandler(viewModel)

    val snackbarHostState = remember { SnackbarHostState() }

    // Trigger data loading when the class ID changes
    LaunchedEffect(key1 = classId) {
        viewModel.loadCourseDetails(classId)
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
        // Main layout container with vertical spacing
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary management cards (Students, Attendance, Activities)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary grid for Skills and Reports
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardCard(
                        title = "Habilidades",
                        subtitle = "${courseSkills.size} habilidades definidas",
                        icon = Icons.Default.Psychology,
                        onClick = { navController.navigate("courseSkills/$classId") }
                    )
                }

                item {
                    DashboardCard(
                        title = "Relatórios",
                        subtitle = "Análise e desempenho",
                        icon = Icons.Default.Assessment,
                        onClick = { /* TODO: Implementar navegação para a tela de Relatórios */ }
                    )
                }
            }
        }
    }
}

/**
 * A reusable card component for dashboard navigation.
 * * @param title The text label of the card.
 * @param subtitle Descriptive text below the title.
 * @param icon The vector icon to display.
 * @param onClick Action to perform when the card is clicked.
 */
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