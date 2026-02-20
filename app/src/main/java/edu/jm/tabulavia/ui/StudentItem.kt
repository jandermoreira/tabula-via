package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.ui.StudentEmojiColorHelper.generateColorFromId

/**
 * Common component to display a student with their emoji and name.
 * Shows a visual transparency effect if the student is marked as absent.
 */
@Composable
fun StudentItem(
    student: Student,
    emoji: String,
    modifier: Modifier = Modifier,
    isAbsent: Boolean = false
) {
    val backgroundColor = remember(student.studentId) {
        if (isAbsent)
            Color.Gray
        else
            generateColorFromId(student.studentId)
    }

    Column(
        modifier = modifier.alpha(if (isAbsent) 0.5f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = student.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}