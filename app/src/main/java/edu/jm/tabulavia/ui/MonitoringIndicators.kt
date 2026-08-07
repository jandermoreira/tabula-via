package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.SwapHorizontalCircle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentMonitoringSummary
import edu.jm.tabulavia.ui.theme.Regular
import edu.jm.tabulavia.ui.theme.Attention
import edu.jm.tabulavia.ui.theme.Alert
import java.util.Locale
import kotlin.math.abs

/**
 * Visual component that displays monitoring indicators as a sequence of icons.
 * Follows the visual identity where colors represent status and grey represents unavailable data.
 */
@Composable
fun MonitoringIndicators(
    summary: StudentMonitoringSummary,
    modifier: Modifier = Modifier,
    evidenceType: EvidenceType? = null,
    showValues: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (evidenceType == EvidenceType.MONITORING) {
            // Regularity (Missing submissions)
            IndicatorIcon(
                icon = Icons.AutoMirrored.Filled.Assignment,
                status = summary.regularityState,
                value = if (showValues)
                    if (summary.regularity == 0)
                        "Em dia"
                    else
                        String.format(
                            Locale.US,
                            "%d atraso(s)",
                            summary.regularity
                        )
                else null
            )

            Spacer(modifier = Modifier.width(if (showValues) 12.dp else 8.dp))

            // Performance (Pm)
            IndicatorIcon(
                icon = Icons.Default.QueryStats,
                status = summary.performanceState,
                value = if (showValues) summary.performance?.let {
                    String.format(
                        Locale.US,
                        "Média %.1f",
                        it
                    )
                } else null
            )

            Spacer(modifier = Modifier.width(if (showValues) 12.dp else 8.dp))

            // Attendance (A)
            IndicatorIcon(
                icon = Icons.Default.EventAvailable,
                status = summary.attendanceState,
                value = if (showValues) String.format(
                    Locale.US,
                    "Ausências %.0f%%",
                    summary.attendance
                ) else null
            )
        }

        // Performance Discrepancy (Only for Consolidation or if explicitly requested via summary flag)
        if (evidenceType == EvidenceType.CONSOLIDATION || (evidenceType == null && summary.hasDiscrepancyFlag)) {
            Spacer(modifier = Modifier.width(if (showValues) 12.dp else 8.dp))
            IndicatorIcon(
                icon = Icons.Default.SwapHorizontalCircle,
                status = summary.discrepancyState,
                value = if (showValues) summary.discrepancy?.let {
                    String.format(
                        Locale.US,
                        "%s%.1f pontos",
                        if (it > 0) "+" else "-",
                        abs(it)
                    )
                } else null
            )
        }
    }
}

@Composable
private fun IndicatorIcon(
    icon: ImageVector,
    status: MonitoringState?,
    modifier: Modifier = Modifier,
    value: String? = null
) {
    val color = when (status) {
        MonitoringState.ON_TRACK -> Regular
        MonitoringState.ATTENTION -> Attention
        MonitoringState.CRITICAL -> Alert
        null -> Color.LightGray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                if (value != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = value,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
