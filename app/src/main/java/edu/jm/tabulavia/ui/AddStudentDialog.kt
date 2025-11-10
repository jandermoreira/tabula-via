package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.viewmodel.CourseViewModel

@Composable
fun AddStudentDialog(
    viewModel: CourseViewModel,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Individual", "Em Massa")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Alunos") },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Conteúdo que muda de acordo com a aba
                when (selectedTabIndex) {
                    0 -> IndividualStudentForm(viewModel)
                    1 -> BulkStudentForm(viewModel)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTabIndex == 0) {
                        viewModel.addStudent(onStudentsAdded = onDismiss)
                    } else {
                        viewModel.addStudentsInBulk(onStudentsAdded = onDismiss)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun IndividualStudentForm(viewModel: CourseViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = viewModel.studentNumber,
            onValueChange = { viewModel.studentNumber = it },
            label = { Text("Nº UFSCar") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = viewModel.studentName,
            onValueChange = { viewModel.studentName = it },
            label = { Text("Nome Completo") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BulkStudentForm(viewModel: CourseViewModel) {
    OutlinedTextField(
        value = viewModel.bulkStudentText,
        onValueChange = { viewModel.bulkStudentText = it },
        label = { Text("Cole a lista aqui (Nº Nome)") },
        placeholder = { Text("123456 João da Silva\\n789012 Maria Oliveira") },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}