/**
 * UI components for class management in the Tabulavia application.
 * This file contains the screen to add new academic classes.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel
import java.time.Year

/**
 * Screen that provides a form to add a new class to the system.
 * @param viewModel The state holder for class data.
 * @param onClassAdded Callback executed after successful class creation.
 * @param onNavigateBack Callback to return to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassScreen(
    viewModel: ClassViewModel, onClassAdded: () -> Unit, onNavigateBack: () -> Unit
) {
    MessageHandler(viewModel)

    val currentYear = Year.now().value

    Scaffold(
        topBar = {
            TabulaTopBar(
                title = "Adicionar Nova Turma",
                viewModel = viewModel,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }) { paddingValues ->
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
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
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
                value = if (viewModel.numberOfSessions > 0) viewModel.numberOfSessions.toString() else "",
                onValueChange = { viewModel.numberOfSessions = it.toIntOrNull() ?: 15 },
                label = { Text("Número de Aulas") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action button to save the class
            Button(
                onClick = { viewModel.addClass(onClassAdded) }, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Turma")
            }
        }
    }
}