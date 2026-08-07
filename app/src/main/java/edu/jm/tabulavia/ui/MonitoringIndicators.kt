package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentMonitoringSummary
import edu.jm.tabulavia.ui.theme.Amber

/**
 * Visual component that displays monitoring indicators as a sequence of icons.
 * Follows the visual identity where colors represent status and grey represents unavailable data.
 */
@Composable
fun MonitoringIndicators(
    summary: StudentMonitoringSummary,
    evidenceType: EvidenceType? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // Regularity (Missing submissions)
        IndicatorIcon(
            icon = Icons.Default.Assignment,
            status = when {
                summary.regularity >= 2 -> MonitoringState.CRITICAL
                summary.regularity == 1 -> MonitoringState.ATTENTION
                else -> MonitoringState.ON_TRACK
            }
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // Performance (Pm)
        IndicatorIcon(
            icon = Icons.Default.QueryStats,
            status = when {
                summary.performance == null -> null
                summary.performance < 4.0 -> MonitoringState.CRITICAL
                summary.performance < 6.0 -> MonitoringState.ATTENTION
                else -> MonitoringState.ON_TRACK
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Attendance (A)
        IndicatorIcon(
            icon = Icons.Default.EventAvailable,
            status = when {
                summary.attendance >= 20.0 -> MonitoringState.CRITICAL
                summary.attendance >= 15.0 -> MonitoringState.ATTENTION
                else -> MonitoringState.ON_TRACK
            }
        )

        // Performance Discrepancy (Only for Consolidation or if explicitly requested via summary flag)
        if (evidenceType == EvidenceType.CONSOLIDATION || (evidenceType == null && summary.hasDiscrepancyFlag)) {
            Spacer(modifier = Modifier.width(8.dp))
            IndicatorIcon(
                icon = Icons.Default.Gavel,
                status = when {
                    summary.discrepancy == null -> null
                    summary.discrepancy >= 5.0 -> MonitoringState.CRITICAL
                    summary.discrepancy >= 3.0 -> MonitoringState.ATTENTION
                    else -> MonitoringState.ON_TRACK
                }
            )
        }
    }
}

@Composable
private fun IndicatorIcon(
    icon: ImageVector,
    status: MonitoringState?,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        MonitoringState.ON_TRACK -> MaterialTheme.colorScheme.secondary
        MonitoringState.ATTENTION -> Amber
        MonitoringState.CRITICAL -> MaterialTheme.colorScheme.error
        null -> Color.LightGray
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(18.dp)
    )
}
