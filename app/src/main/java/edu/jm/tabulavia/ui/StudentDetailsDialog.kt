/**
 * UI components for displaying student details in a dialog.
 */
package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.EvidenceHistoryItem
import edu.jm.tabulavia.model.InterventionAction
import edu.jm.tabulavia.model.SkillTrend
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.ui.theme.Attention
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.ClassViewModel
import java.util.Locale

/**
 * Displays a dialog with detailed information about a student, including performance indicators,
 * recommended actions, evidence history, and skill statuses.
 *
 * @param student The student whose details are displayed.
 * @param attendancePercentage The current attendance percentage of the student.
 * @param viewModel The view model providing class data and dashboard items.
 * @param onDismiss Callback to be invoked when the dialog is dismissed.
 */
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
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Information Container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Matrícula", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = student.studentNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Frequência Atual", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = attendancePercentage?.let { "%.0f%%".format(it) } ?: "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (attendancePercentage == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (dashboardItem != null) {
                    val summary = dashboardItem.summary
                    
                    SectionHeader("Indicadores de Ritmo")
                    MonitoringIndicators(
                        summary = summary,
                        showValues = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    if (summary.actions.isNotEmpty()) {
                        SectionHeader("Ações Recomendadas")
                        summary.actions.forEach { action ->
                            InterventionActionRow(action)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                SectionHeader("Evolução do Monitoramento")
                if (history.isEmpty()) {
                    Text(
                        text = "Nenhuma evidência registrada.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    history.forEach { item ->
                        EvidenceHistoryRow(item)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                SectionHeader("Habilidades")
                if (skillSummaries.isEmpty()) {
                    Text(
                        text = "Nenhuma habilidade registrada.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    skillSummaries.forEach { skillStatus ->
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

/**
 * Renders a standardized section header with a title and divider.
 *
 * @param title The text to display as the section title.
 */
@Composable
private fun SectionHeader(title: String) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * Displays a row representing a recommended intervention action.
 *
 * @param action The intervention action to display.
 */
@Composable
private fun InterventionActionRow(action: InterventionAction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = action.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * Displays a row representing an item in the student's evidence history.
 *
 * @param item The evidence history item to display.
 */
@Composable
private fun EvidenceHistoryRow(item: EvidenceHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.evidenceName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.score?.let { String.format(Locale.getDefault(), "%.1f", it) }
                        ?: "Sem nota",
                    color = if (item.score == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

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

//                Spacer(modifier = Modifier.width(12.dp))
//
//                val stateLabel = when (item.snapshot.state) {
//                    MonitoringState.ON_TRACK -> "EM DIA"
//                    MonitoringState.ATTENTION -> "ATENÇÃO"
//                    MonitoringState.CRITICAL -> "CRÍTICO"
//                }
//                val stateColor = when (item.snapshot.state) {
//                    MonitoringState.ON_TRACK -> MaterialTheme.colorScheme.secondary
//                    MonitoringState.ATTENTION -> Attention
//                    MonitoringState.CRITICAL -> MaterialTheme.colorScheme.error
//                }
//                Text(
//                    text = stateLabel,
//                    color = stateColor,
//                    fontWeight = FontWeight.ExtraBold,
//                    fontSize = 10.sp
//                )
            }
        }
    }
}

/**
 * Displays a small label for a monitoring indicator.
 *
 * @param label The text label for the indicator.
 * @param value The numerical value of the indicator.
 * @param isPercentage Whether the value should be formatted as a percentage.
 * @param isDiscrepancy Whether the indicator represents a discrepancy that may require attention.
 */
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
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
        Text(
            text = formattedValue,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

/**
 * Displays a row representing the status and trend of a specific skill.
 *
 * @param trend The trend of the skill (improving, declining, or stable).
 * @param name The name of the skill.
 * @param level The current level of the skill.
 */
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
            .padding(vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Icon(
            imageVector = trendIcon,
            contentDescription = null,
            tint = trendTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = level,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}

private const val PM_SYMBOL = "P\u2098"
private const val DELTA_D_SYMBOL = "\u0394D"
