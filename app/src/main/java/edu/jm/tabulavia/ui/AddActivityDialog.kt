package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityDialog(
    viewModel: CourseViewModel,
    onDismiss: () -> Unit
) {
    val types = listOf("Individual", "Grupo")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Atividade") },
        text = {
            Column {
                OutlinedTextField(
                    value = viewModel.activityName,
                    onValueChange = { viewModel.activityName = it },
                    label = { Text("Nome da Atividade") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                            onClick = { viewModel.activityType = type },
                            selected = type == viewModel.activityType
                        ) {
                            Text(type)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addActivity(onDismiss)
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
