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
import edu.jm.tabulavia.model.SkillTrend
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.CourseViewModel

@Composable
fun StudentDetailsDialog(
    student: Student,
    attendancePercentage: Float?,
    viewModel: CourseViewModel, // Adicionado o ViewModel
    onDismiss: () -> Unit
    // onEditSkills: () -> Unit // Removido: A funcionalidade de edição de habilidades foi removida
) {
    val skillSummaries by viewModel.studentSkillStatuses.collectAsState() // Coleta os status de habilidades calculados

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
                    // Itera sobre os SkillStatus calculados
                    skillSummaries.forEach { skillStatus ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            // Ícone indicando a tendência
                            val trendIcon: ImageVector = when (skillStatus.trend) {
                                SkillTrend.IMPROVING -> Icons.Default.ArrowUpward
                                SkillTrend.DECLINING -> Icons.Default.ArrowDownward
                                SkillTrend.STABLE -> Icons.Default.DragHandle // Um ícone neutro para estável
                            }
                            val trendTint: Color = when (skillStatus.trend) {
                                SkillTrend.IMPROVING -> MaterialTheme.colorScheme.primary // Verde ou cor positiva
                                SkillTrend.DECLINING -> MaterialTheme.colorScheme.error // Vermelho ou cor de alerta
                                SkillTrend.STABLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }

                            Icon(
                                imageVector = trendIcon,
                                contentDescription = "Tendência: ${skillStatus.trend.name}",
                                tint = trendTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Exibe o nome da habilidade e seu nível atual
                            Text(
                                text = "${skillStatus.skillName}: ${skillStatus.currentLevel.displayName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
        // Dismiss button removido
    )
}
