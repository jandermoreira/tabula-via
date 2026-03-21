/**
 * UI components for activity management within a course.
 * This file handles the creation of new group activities.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel

/**
 * Dialog for adding a new activity to a course.
 * All activities are now set to "Grupo" by default as per requirements.
 * * @param viewModel The state holder for course and activity data.
 * @param onDismiss Callback to close the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityDialog(
    viewModel: ClassViewModel,
    onDismiss: () -> Unit
) {
    MessageHandler(viewModel)

    val courseSkills by viewModel.classSkills.collectAsState()

    // Ensure the activity type is always set to "Grupo" when the dialog opens
    LaunchedEffect(Unit) {
        viewModel.activityType = "Grupo"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Atividade") },
        text = {
            Column {
                // Input field for the activity name
                OutlinedTextField(
                    value = viewModel.activityName,
                    onValueChange = { viewModel.activityName = it },
                    label = { Text("Nome da Atividade") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Highlighted skills section
                Text("Habilidades em destaque", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                val sortedSkillNames = courseSkills.map { it.skillName }.sorted()

                // Layout for skill selection chips
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                ) {
                    sortedSkillNames.forEach { skillName ->
                        val isSelected = viewModel.activityHighlightedSkills.contains(skillName)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.activityHighlightedSkills =
                                    if (isSelected) viewModel.activityHighlightedSkills - skillName
                                    else viewModel.activityHighlightedSkills + skillName
                            },
                            label = { Text(skillName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.addActivity(onDismiss) }) {
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