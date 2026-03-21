/**
 * UI components for course management in the Tabulavia application.
 * This file contains the screen to add new academic courses.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel
import java.time.Year

/**
 * Screen that provides a form to add a new course to the system.
 * * @param viewModel The state holder for course data.
 * @param onCourseAdded Callback executed after successful course creation.
 * @param onNavigateBack Callback to return to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(
    viewModel: ClassViewModel,
    onCourseAdded: () -> Unit,
    onNavigateBack: () -> Unit
) {
    MessageHandler(viewModel)

    val currentYear = Year.now().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Nova Turma") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Main form layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.courseName,
                onValueChange = { viewModel.courseName = it },
                label = { Text("Nome da Turma (ex: CAP)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.academicYear,
                onValueChange = {
                    if (it.all { char -> char.isDigit() } && it.length <= 4) {
                        viewModel.academicYear = it
                    }
                },
                label = { Text("Ano (ex: $currentYear)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.period,
                onValueChange = { viewModel.period = it },
                label = { Text("Período/Semestre (ex: 1, Verão...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = if (viewModel.numberOfClasses > 0) viewModel.numberOfClasses.toString() else "",
                onValueChange = { viewModel.numberOfClasses = it.toIntOrNull() ?: 0 },
                label = { Text("Número de Aulas") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action button to save the course
            Button(
                onClick = { viewModel.addCourse(onCourseAdded) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Turma")
            }
        }
    }
}