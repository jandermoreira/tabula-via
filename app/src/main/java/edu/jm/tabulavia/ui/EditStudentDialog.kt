package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.viewmodel.CourseViewModel

@Composable
fun EditStudentDialog(
    viewModel: CourseViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Aluno") },
        text = {
            Column {
                OutlinedTextField(
                    value = viewModel.studentName,
                    onValueChange = { viewModel.studentName = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.studentDisplayName,
                    onValueChange = { viewModel.studentDisplayName = it },
                    label = { Text("Nome de Apresentação") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.studentNumber,
                    onValueChange = { viewModel.studentNumber = it },
                    label = { Text("Nº UFSCar") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateStudent(onDismiss)
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
