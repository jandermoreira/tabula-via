package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel

@Composable
fun EditStudentDialog(
    viewModel: ClassViewModel,
    onDismiss: () -> Unit
) {
    MessageHandler(viewModel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Aluno") },
        text = {
            Column {
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
                OutlinedTextField(
                    value = viewModel.studentDisplayName,
                    onValueChange = { viewModel.studentDisplayName = it },
                    label = { Text("Nome de Apresentação") },
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
