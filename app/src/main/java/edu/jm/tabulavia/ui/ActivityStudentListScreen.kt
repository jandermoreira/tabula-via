package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.viewmodel.CourseViewModel
import androidx.compose.ui.platform.LocalContext // Necessário para obter o contexto
import androidx.compose.ui.res.painterResource
import edu.jm.tabulavia.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityStudentListScreen(
    activityId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.studentsForClass.collectAsState()
    val activity by viewModel.selectedActivity.collectAsState()

    LaunchedEffect(activityId) {
        viewModel.loadActivityDetails(activityId)
    }

    // Pré-calcula o mapa de ícones para otimizar a rolagem
    val context = LocalContext.current
    val studentIconMap = remember(students, context) {
        students.associate { student ->
            val iconIndex = (student.studentId.mod(80L) + 1).toInt()
            val iconName = "student_${iconIndex}"
            val drawableResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            student.studentId to (drawableResId.takeIf { it != 0 } ?: R.drawable.student_0) // Fallback icon
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.title ?: "Alunos da Atividade") },
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
        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum aluno encontrado.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students, key = { it.studentId }) { student ->
                    // Busca o drawableResId pré-calculado do mapa
                    val studentDrawableResId = studentIconMap[student.studentId] ?: R.drawable.student_0 // Fallback
                    StudentItem(
                        student = student,
                        drawableResId = studentDrawableResId // Passa o ID do drawable pré-calculado
                    )
                }
            }
        }
    }
}
