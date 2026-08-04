package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.StudentDashboardItem
import edu.jm.tabulavia.model.StudentTrackingState
import edu.jm.tabulavia.ui.theme.Amber
import edu.jm.tabulavia.ui.theme.SkyBlue

/**
 * A grid of summary cards showing the count of students in each tracking state.
 * Uses the project's theme colors and follows the pedagogical tracking specification.
 *
 * @param items The list of student dashboard items to summarize.
 * @param modifier Decorator for the grid layout.
 */
@Composable
fun TrackingSummaryCards(
    items: List<StudentDashboardItem>,
    modifier: Modifier = Modifier
) {
    val counts = items.groupingBy { it.state }.eachCount()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        item {
            SummaryCard(
                label = "Normal",
                count = counts[StudentTrackingState.NORMAL] ?: 0,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            SummaryCard(
                label = "Revisão",
                count = counts[StudentTrackingState.GUIDED_REVISION] ?: 0,
                color = SkyBlue
            )
        }
        item {
            SummaryCard(
                label = "Prioritário",
                count = counts[StudentTrackingState.PRIORITIZED_TRACKING] ?: 0,
                color = Amber
            )
        }
        item {
            SummaryCard(
                label = "Recuperação",
                count = counts[StudentTrackingState.RECOVERY] ?: 0,
                color = MaterialTheme.colorScheme.error
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
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 1.sp
            )
        }
    }
}
