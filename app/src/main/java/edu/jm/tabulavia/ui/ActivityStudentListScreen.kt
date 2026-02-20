package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.viewmodel.CourseViewModel

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

    // Use the EmojiMapper to get the emoji for each student
    val studentEmojiMap = remember(students) {
        students.associate {
            it.studentId to StudentEmojiColorHelper.mapStudentIdToEmoji(it.studentId)
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
    ) {
        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum aluno encontrado.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students, key = { it.studentId }) { student ->
                    val studentEmoji = studentEmojiMap[student.studentId] ?: "❓"
                    val isStudentAbsent = true

                    StudentItem(
                        student = student,
                        emoji = studentEmoji,
                        isAbsent = isStudentAbsent,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .alpha(if (isStudentAbsent) 0.5f else 1f)
                    )
                }
            }
        }
    }
}
