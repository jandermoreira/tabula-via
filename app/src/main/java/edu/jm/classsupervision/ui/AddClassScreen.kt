package edu.jm.classsupervision.ui

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
import edu.jm.classsupervision.viewmodel.ClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassScreen(viewModel: ClassViewModel, onClassAdded: () -> Unit, onNavigateBack: () -> Unit) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.className,
                onValueChange = { viewModel.className = it },
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
                label = { Text("Ano (ex: 2025)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.period,
                onValueChange = { viewModel.period = it },
                label = { Text("Período/Semestre (ex: 1)") },
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

            Button(
                onClick = { viewModel.addClass(onClassAdded) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Turma")
            }
        }
    }
}
