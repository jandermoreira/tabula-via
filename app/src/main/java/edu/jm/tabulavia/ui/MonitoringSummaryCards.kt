package edu.jm.tabulavia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentDashboardItem
import edu.jm.tabulavia.ui.theme.Attention

/**
 * A grid of summary cards showing the count of students in each monitoring state.
 * Uses the project's theme colors and follows the pedagogical monitoring specification.
 *
 * @param items The list of student dashboard items to summarize.
 * @param selectedState The currently selected monitoring state filter.
 * @param onStateClick Callback when a state card is clicked.
 * @param modifier Decorator for the grid layout.
 */
@Composable
fun MonitoringSummaryCards(
    items: List<StudentDashboardItem>,
    selectedState: MonitoringState?,
    onStateClick: (MonitoringState?) -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = items.groupingBy { it.summary.state }.eachCount()
    val hasAttentionOrCritical = (counts[MonitoringState.ATTENTION] ?: 0) > 0 ||
            (counts[MonitoringState.CRITICAL] ?: 0) > 0

    if (!hasAttentionOrCritical) return

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        item {
            SummaryCard(
                label = "Em Dia",
                count = counts[MonitoringState.ON_TRACK] ?: 0,
                color = MaterialTheme.colorScheme.secondary,
                isSelected = selectedState == MonitoringState.ON_TRACK,
                isAnySelected = selectedState != null,
                onClick = {
                    onStateClick(if (selectedState == MonitoringState.ON_TRACK) null else MonitoringState.ON_TRACK)
                }
            )
        }
        item {
            SummaryCard(
                label = "Atenção",
                count = counts[MonitoringState.ATTENTION] ?: 0,
                color = Attention,
                isSelected = selectedState == MonitoringState.ATTENTION,
                isAnySelected = selectedState != null,
                onClick = {
                    onStateClick(if (selectedState == MonitoringState.ATTENTION) null else MonitoringState.ATTENTION)
                }
            )
        }
        item {
            SummaryCard(
                label = "Crítico",
                count = counts[MonitoringState.CRITICAL] ?: 0,
                color = MaterialTheme.colorScheme.error,
                isSelected = selectedState == MonitoringState.CRITICAL,
                isAnySelected = selectedState != null,
                onClick = {
                    onStateClick(if (selectedState == MonitoringState.CRITICAL) null else MonitoringState.CRITICAL)
                }
            )
        }
    }
}

/**
 * Individual summary card for a specific state.
 */
@Composable
private fun SummaryCard(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    isAnySelected: Boolean,
    onClick: () -> Unit
) {
    val displayColor = if (isAnySelected && !isSelected) Color.Gray else color
    val borderStroke = if (isSelected) BorderStroke(2.dp, color) else null

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(displayColor.copy(alpha = 0.12f))
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(12.dp)) else Modifier)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = displayColor
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = displayColor,
                letterSpacing = 1.sp
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remover filtro",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                tint = color
            )
        }
    }
}
