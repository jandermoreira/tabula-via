/**
 * A row component for the class dashboard representing a student's tracking status.
 * Reuses EmojiWithBlob from StudentItem.kt to maintain visual consistency.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.EvidenceTrend
import edu.jm.tabulavia.model.StudentDashboardItem
import edu.jm.tabulavia.model.StudentTrackingState
import edu.jm.tabulavia.ui.theme.Amber
import edu.jm.tabulavia.ui.theme.SkyBlue
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToColor
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToEmoji

/**
 * A row component for the class dashboard representing a student's tracking status.
 * Reuses EmojiWithBlob from StudentItem.kt to maintain visual consistency.
 *
 * @param item The dashboard data for the student.
 * @param onClick Action to perform when the row is selected.
 */
@Composable
fun StudentTrackingRow(
    item: StudentDashboardItem,
    onClick: () -> Unit
) {
    TabulaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reusing existing component from StudentItem.kt
            EmojiWithBlob(
                emoji = mapIdToEmoji(item.student.studentNumber),
                backgroundColor = mapIdToColor(item.student.studentNumber),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.student.effectiveName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Nível Atual: ${item.currentLevel.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.isConsistent) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Consistente",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                TrendIcon(trend = item.trend)
                StateTag(state = item.state)
            }
        }
    }
}

@Composable
private fun TrendIcon(trend: EvidenceTrend) {
    val (icon, color) = when (trend) {
        EvidenceTrend.IMPROVED -> Icons.AutoMirrored.Filled.TrendingUp to MaterialTheme.colorScheme.secondary
        EvidenceTrend.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
        EvidenceTrend.WORSENED -> Icons.AutoMirrored.Filled.TrendingDown to MaterialTheme.colorScheme.error
        EvidenceTrend.UNKNOWN -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.outline
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun StateTag(state: StudentTrackingState) {
    val (label, color) = when (state) {
        StudentTrackingState.NORMAL -> "Normal" to MaterialTheme.colorScheme.secondary
        StudentTrackingState.GUIDED_REVISION -> "Revisão" to SkyBlue
        StudentTrackingState.PRIORITIZED_TRACKING -> "Prioritário" to Amber
        StudentTrackingState.RECOVERY -> "Recuperação" to MaterialTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}
