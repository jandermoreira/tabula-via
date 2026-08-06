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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.SkillTrend
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.StudentMonitoringSummary
import edu.jm.tabulavia.ui.theme.Amber
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel
import java.util.Locale

@Composable
fun StudentDetailsDialog(
    student: Student,
    attendancePercentage: Float?,
    viewModel: ClassViewModel,
    onDismiss: () -> Unit
) {
    MessageHandler(viewModel)

    val dashboardItems by viewModel.dashboardItems.collectAsState()
    val summary = dashboardItems.find { it.student.studentId == student.studentId }?.summary
    val skillSummaries by viewModel.studentSkillStatuses.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(student.name)
                if (summary?.hasDiscrepancyFlag == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta de Discrepância",
                        modifier = Modifier.size(20.dp),
                        tint = Amber
                    )
                }
            }
        },
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

                if (summary != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Monitoramento (Ciclo Atual)", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    MonitoringSummaryContent(summary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Habilidades", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (skillSummaries.isEmpty()) {
                    Text("Nenhuma habilidade registrada para este aluno ou turma.")
                } else {
                    for (skillStatus in skillSummaries) {
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

@Composable
private fun MonitoringSummaryContent(summary: StudentMonitoringSummary) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Estado", style = MaterialTheme.typography.labelMedium)
                val stateColor = when (summary.state) {
                    MonitoringState.ON_TRACK -> MaterialTheme.colorScheme.primary
                    MonitoringState.ATTENTION -> Amber
                    MonitoringState.CRITICAL -> MaterialTheme.colorScheme.error
                }
                Text(
                    text = when (summary.state) {
                        MonitoringState.ON_TRACK -> "Em Dia"
                        MonitoringState.ATTENTION -> "Atenção"
                        MonitoringState.CRITICAL -> "Crítico"
                    },
                    color = stateColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Ritmo (Faltas)", style = MaterialTheme.typography.labelMedium)
                Text("${summary.regularity}", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Desempenho ($PM_SYMBOL)", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = summary.performance?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "N/A",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Discrepância ($DELTA_D_SYMBOL)", style = MaterialTheme.typography.labelMedium)
                val discrepancyText = summary.discrepancy?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "N/A"
                Text(
                    text = discrepancyText,
                    color = if (summary.hasDiscrepancyFlag) Amber else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private const val PM_SYMBOL = "P\u2098"
private const val DELTA_D_SYMBOL = "\u0394D"
