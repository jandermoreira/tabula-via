package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    viewModel: CourseViewModel,
    onDismiss: () -> Unit
) {
    val skillSummaries by viewModel.studentSkillStatuses.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.name) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Matrícula", style = MaterialTheme.typography.labelMedium)
                        Text(student.studentNumber, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Frequência", style = MaterialTheme.typography.labelMedium)
                        if (attendancePercentage != null) {
                            Text("%.0f%%".format(attendancePercentage), style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text("N/A", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Habilidades", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (skillSummaries.isEmpty()) {
                    Text("Nenhuma habilidade registrada para este aluno ou turma.")
                } else {
                    skillSummaries.forEach { skillStatus ->
                        // Checking trend
                        val trendIcon = when (skillStatus.trend) {
                            SkillTrend.IMPROVING -> Icons.Default.ArrowUpward
                            SkillTrend.DECLINING -> Icons.Default.ArrowDownward
                            SkillTrend.STABLE -> Icons.Default.DragHandle
                        }
                        val trendTint = when (skillStatus.trend) {
                            SkillTrend.IMPROVING -> MaterialTheme.colorScheme.primary
                            SkillTrend.DECLINING -> MaterialTheme.colorScheme.error
                            SkillTrend.STABLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = "Tendência: ${skillStatus.trend.name}",
                                tint = trendTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = skillStatus.skillName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = skillStatus.currentLevel.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
    )
}
