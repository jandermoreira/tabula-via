/**
 * A row component for the class dashboard representing a student's monitoring status.
 * Reuses EmojiWithBlob from StudentItem.kt to maintain visual consistency.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentDashboardItem
import edu.jm.tabulavia.ui.theme.Alert
import edu.jm.tabulavia.ui.theme.Attention
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToColor
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToEmoji

/**
 * A row component for the class dashboard representing a student's monitoring status.
 * Reuses EmojiWithBlob from StudentItem.kt to maintain visual consistency.
 *
 * @param item The dashboard data for the student.
 * @param status The current attendance status for the student.
 * @param onClick Action to perform when the row is selected.
 * @param onLongClick Action to perform when the row is long-pressed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentMonitoringRow(
    item: StudentDashboardItem,
    status: AttendanceStatus,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val (backgroundColor, itemAlpha) = when (status) {
        AttendanceStatus.PRESENT -> mapIdToColor(item.student.studentNumber) to 1f
        AttendanceStatus.ABSENT -> Color.Yellow to 0.8f
        AttendanceStatus.EXCUSED -> Color.Gray to 0.8f
    }

    val emojiColor =
        if (status == AttendanceStatus.ABSENT) Color.Gray else MaterialTheme.colorScheme.onSurface

    TabulaCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(itemAlpha)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else Modifier
            ),
        onClick = if (onLongClick == null) onClick else null
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
                backgroundColor = backgroundColor,
                color = emojiColor,
                isDashed = status != AttendanceStatus.PRESENT,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.student.effectiveName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                MonitoringIndicators(
                    summary = item.summary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (item.summary.actions.isNotEmpty()) {
                val iconColor = when (item.summary.state) {
                    MonitoringState.CRITICAL -> Alert
                    MonitoringState.ATTENTION -> Attention
                    else -> MaterialTheme.colorScheme.primary
                }
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Ações Recomendadas",
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }
        }
    }
}
