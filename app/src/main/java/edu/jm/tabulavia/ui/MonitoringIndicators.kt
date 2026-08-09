/**
 * UI components for displaying monitoring indicators.
 */
package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SwapHorizontalCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentMonitoringSummary
import edu.jm.tabulavia.ui.theme.Alert
import edu.jm.tabulavia.ui.theme.Attention
import edu.jm.tabulavia.ui.theme.Regular
import java.util.Locale
import kotlin.math.abs

/**
 * Visual component that displays monitoring indicators as a sequence of icons.
 *
 * Follows the visual identity where colors represent status and grey represents unavailable data.
 *
 * @param summary The monitoring summary data for the student.
 * @param modifier The modifier to be applied to the layout.
 * @param evidenceType The type of evidence (Monitoring or Consolidation) to filter indicators.
 * @param showValues Whether to display numeric/text values alongside icons.
 */
@Composable
fun MonitoringIndicators(
    summary: StudentMonitoringSummary,
    modifier: Modifier = Modifier,
    evidenceType: EvidenceType? = null,
    showValues: Boolean = false
) {
    Row(
        modifier = modifier.then(if (showValues) Modifier.fillMaxWidth() else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val effectiveType = evidenceType ?: summary.activeEvidenceType ?: EvidenceType.MONITORING

        if (effectiveType == EvidenceType.MONITORING) {
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
                else null,
                modifier = if (showValues) Modifier.weight(1f) else Modifier
            )

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
                } else null,
                modifier = if (showValues) Modifier.weight(1f) else Modifier
            )

            // Attendance (A)
            IndicatorIcon(
                icon = Icons.Default.EventAvailable,
                status = summary.attendanceState,
                value = if (showValues) String.format(
                    Locale.US,
                    "Ausências %.0f%%",
                    summary.attendance
                ) else null,
                modifier = if (showValues) Modifier.weight(1f) else Modifier
            )
        } else {
            // Performance Discrepancy (Only for Consolidation)
            IndicatorIcon(
                icon = Icons.Default.SwapHorizontalCircle,
                status = summary.discrepancyState,
                value = if (showValues) summary.discrepancy?.let {
                    if (it == 0.0)
                        "Sem queda"
                    else
                        String.format(
                            Locale.US,
                            "Queda %.1f pts",
                            abs(it)
                        )
                } else null,
                modifier = if (showValues) Modifier.weight(1f) else Modifier
            )
        }
    }
}

/**
 * Renders an individual indicator icon with optional text value.
 *
 * @param icon The vector icon to display.
 * @param status The monitoring state that determines the icon color.
 * @param modifier The modifier to be applied to the layout.
 * @param value Optional text value to display below the icon.
 */
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(4.dp),
        contentAlignment = Alignment.Center
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
                Spacer(modifier = Modifier.height(4.dp))
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
