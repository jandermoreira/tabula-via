package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
    modifier: Modifier = Modifier,
    evidenceType: EvidenceType? = null
) {
    Row(modifier = modifier) {
        // Regularity (Missing submissions)
        IndicatorIcon(
            icon = Icons.AutoMirrored.Filled.Assignment,
            status = summary.regularityState
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // Performance (Pm)
        IndicatorIcon(
            icon = Icons.Default.QueryStats,
            status = summary.performanceState
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Attendance (A)
        IndicatorIcon(
            icon = Icons.Default.EventAvailable,
            status = summary.attendanceState
        )

        // Performance Discrepancy (Only for Consolidation or if explicitly requested via summary flag)
        if (evidenceType == EvidenceType.CONSOLIDATION || (evidenceType == null && summary.hasDiscrepancyFlag)) {
            Spacer(modifier = Modifier.width(8.dp))
            IndicatorIcon(
                icon = Icons.Default.Gavel,
                status = summary.discrepancyState
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
