package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.CourseViewModel

@Composable
fun StudentDetailsDialog(
    student: Student,
    attendancePercentage: Float?,
    viewModel: CourseViewModel, // Adicionado o ViewModel
    onDismiss: () -> Unit,
    onEditSkills: () -> Unit
) {
    val skillSummaries by viewModel.studentSkillSummaries.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.name) },
        text = {
            Column {
                Text("Matrícula: ${student.studentNumber}")
                Spacer(modifier = Modifier.height(8.dp))
                if (attendancePercentage != null) {
                    Text("Frequência: %.0f%%".format(attendancePercentage))
                } else {
                    Text("Frequência: Impossível", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Habilidades", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (skillSummaries.isEmpty()) {
                    Text("Nenhuma habilidade registrada para este aluno ou turma.")
                } else {
                    skillSummaries.values.forEach { skillSummary ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            val hasAnyAssessment = skillSummary.professorAssessment != null ||
                                    skillSummary.selfAssessment != null ||
                                    skillSummary.peerAssessment != null

                            // Exibe um ícone simples para indicar se a habilidade tem alguma avaliação
                            // Para detalhes, o usuário deve ir para a tela de habilidades
                            Icon(
                                imageVector = if (hasAnyAssessment) Icons.Filled.CheckCircle else Icons.Filled.Remove,
                                contentDescription = if (hasAnyAssessment) "Habilidade Avaliada" else "Habilidade Não Avaliada",
                                tint = if (hasAnyAssessment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(skillSummary.skillName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        dismissButton = {
            Button(onClick = onEditSkills) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar Habilidades",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Editar")
            }
        }
    )
}
