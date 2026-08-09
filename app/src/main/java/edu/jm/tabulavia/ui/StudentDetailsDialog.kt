package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.EvidenceHistoryItem
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.SkillTrend
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.ui.theme.Attention
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
    val dashboardItem = dashboardItems.find { it.student.studentId == student.studentId }
    val history = dashboardItem?.evidenceHistory ?: emptyList()
    val skillSummaries by viewModel.studentSkillStatuses.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(student.name)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Matrícula", style = MaterialTheme.typography.labelMedium)
                        Text(student.studentNumber, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Frequência Atual", style = MaterialTheme.typography.labelMedium)
                        if (attendancePercentage != null) {
                            Text(
                                "%.0f%%".format(attendancePercentage),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text(
                                "N/A",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Evolução do Monitoramento", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (history.isEmpty()) {
                    Text("Nenhuma evidência registrada.")
                } else {
                    history.forEach { item ->
                        EvidenceHistoryRow(item)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Habilidades", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (skillSummaries.isEmpty()) {
                    Text("Nenhuma habilidade registrada.")
                } else {
                    for (skillStatus in skillSummaries) {
                        SkillStatusRow(
                            skillStatus.trend,
                            skillStatus.skillName,
                            skillStatus.currentLevel.displayName
                        )
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
private fun EvidenceHistoryRow(item: EvidenceHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.evidenceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.score?.let { String.format(Locale.getDefault(), "%.1f", it) }
                        ?: "Faltante",
                    color = if (item.score == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonitoringIndicators(
                    summary = item.snapshot,
                    evidenceType = item.type,
                    showValues = true,
                    modifier = Modifier.weight(1f)
                )

                val stateLabel = when (item.snapshot.state) {
                    MonitoringState.ON_TRACK -> "OK"
                    MonitoringState.ATTENTION -> "ATN"
                    MonitoringState.CRITICAL -> "CRI"
                }
                val stateColor = when (item.snapshot.state) {
                    MonitoringState.ON_TRACK -> MaterialTheme.colorScheme.secondary
                    MonitoringState.ATTENTION -> Attention
                    MonitoringState.CRITICAL -> MaterialTheme.colorScheme.error
                }
                Text(
                    text = stateLabel,
                    color = stateColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun IndicatorMiniLabel(
    label: String,
    value: Double?,
    isPercentage: Boolean = false,
    isDiscrepancy: Boolean = false
) {
    val formattedValue = value?.let {
        if (isPercentage) "%.0f%%".format(it) else "%.1f".format(it)
    } ?: "N/A"

    val color =
        if (isDiscrepancy && value != null && value >= 3.0) Attention else MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
        Text(
            formattedValue,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SkillStatusRow(trend: SkillTrend, name: String, level: String) {
    val trendIcon = when (trend) {
        SkillTrend.IMPROVING -> Icons.Default.ArrowUpward
        SkillTrend.DECLINING -> Icons.Default.ArrowDownward
        SkillTrend.STABLE -> Icons.Default.DragHandle
    }
    val trendTint = when (trend) {
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
            contentDescription = null,
            tint = trendTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = level,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private const val PM_SYMBOL = "P\u2098"
private const val DELTA_D_SYMBOL = "\u0394D"
