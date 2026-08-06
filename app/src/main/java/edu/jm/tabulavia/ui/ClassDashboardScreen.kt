/**
 * File: ClassDashboardScreen.kt
 * Description: UI components for the class dashboard, displaying students, activities, skills, and reports.
 */

package edu.jm.tabulavia.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel
import kotlinx.coroutines.launch

/**
 * Main screen for the class dashboard.
 * * @param classId The unique identifier of the class.
 * @param viewModel The ViewModel handling class logic.
 * @param navController Controller for app navigation.
 * @param onNavigateBack Callback for the back navigation action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDashboardScreen(
    classId: String,
    viewModel: ClassViewModel,
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    // Handle specific view model messages
    MessageHandler(viewModel)

    val snackbarHostState = remember { SnackbarHostState() }

    // Trigger data loading when the class ID changes
    LaunchedEffect(key1 = classId) {
        viewModel.loadClassDetails(classId)
    }

    val selectedClass by viewModel.selectedClass.collectAsState()
    val students by viewModel.studentsForClass.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val classSkills by viewModel.classSkills.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    /**
     * Launcher for exporting the current class to a JSON file.
     */
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                selectedClass?.let { clazz ->
                    viewModel.exportClassBackup(clazz) { jsonString ->
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.write(jsonString.toByteArray())
                                outputStream.flush()
                            }
                        } catch (e: Exception) {
                            viewModel.showMessage("Erro ao salvar arquivo")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val titleText = selectedClass?.let {
                "${it.name} ${it.academicYear}/${it.period}"
            } ?: "Carregando..."

            TabulaTopBar(
                title = titleText,
                viewModel = viewModel,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para a lista de turmas"
                        )
                    }
                },
                actions = {
                    selectedClass?.let { clazz ->
                        IconButton(onClick = {
                            exportLauncher.launch("${clazz.name}_backup.json")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Exportar turma"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Main layout container as a single scrollable grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary management cards (Full width)
            item(span = { GridItemSpan(2) }) {
                DashboardCard(
                    title = "Alunos",
                    subtitle = "${students.size} alunos cadastrados",
                    icon = Icons.Default.Group,
                    onClick = { navController.navigate("studentList/$classId") }
                )
            }

            item(span = { GridItemSpan(2) }) {
                DashboardCard(
                    title = "Frequência",
                    subtitle = "Histórico de frequência",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    onClick = { navController.navigate("frequencyDashboard/$classId") }
                )
            }

            item(span = { GridItemSpan(2) }) {
                DashboardCard(
                    title = "Atividades",
                    subtitle = "${activities.size} atividades criadas",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    onClick = { navController.navigate("activityList/$classId") }
                )
            }

            // Secondary grid for Skills and Reports (Half width)
            item {
                DashboardCard(
                    title = "Habilidades",
                    subtitle = "${classSkills.size} habilidades definidas",
                    icon = Icons.Default.Psychology,
                    onClick = { navController.navigate("classSkills/$classId") }
                )
            }

            item {
                DashboardCard(
                    title = "Relatórios",
                    subtitle = "Análise e desempenho",
                    icon = Icons.Default.Assessment,
                    onClick = { navController.navigate("reportList/$classId") }
                )
            }
        }
    }
}

/**
 * A reusable card component for dashboard navigation.
 * @param title The text label of the card.
 * @param subtitle Descriptive text below the title.
 * @param icon The vector icon to display.
 * @param onClick Action to perform when the card is clicked.
 */
@Composable
fun DashboardCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    TabulaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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