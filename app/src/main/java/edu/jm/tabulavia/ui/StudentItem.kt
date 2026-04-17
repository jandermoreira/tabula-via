/**
 * UI components for student-related displays in the TabulaVia system.
 * This file contains the primary item for student lists and its supporting visual elements.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToColor
import edu.jm.tabulavia.utils.EmojiColorHelper.mapIdToEmoji

/**
 * Custom organic shape used as a background highlight for icons and emojis.
 */
private val BlobShape = GenericShape { size, _ ->
    moveTo(size.width * 0.1f, size.height * 0.4f)
    cubicTo(
        size.width * 0.6f, size.height * -0.2f,
        size.width * 1.4f, size.height * 0.2f,
        size.width * 0.9f, size.height * 0.6f
    )
    cubicTo(
        size.width * 1.2f, size.height * 1.0f,
        size.width * 0.4f, size.height * 1.2f,
        size.width * 0.2f, size.height * 0.8f
    )
    cubicTo(
        size.width * -0.1f, size.height * 0.6f,
        size.width * 0.0f, size.height * 0.3f,
        size.width * 0.1f, size.height * 0.4f
    )
    close()
}
/**
 * Displays a student entry with an emoji avatar and their display name.
 * @param student The student data model.
 * @param modifier Decorator for the root layout of this item.
 * @param isAbsent Flag to trigger visual styling for missing students.
 */
@Composable
fun StudentItem(
    student: Student,
    modifier: Modifier = Modifier,
    isAbsent: Boolean
) {
    val backgroundColor = if (isAbsent) Color.Gray else mapIdToColor(student.studentNumber)
    val emojiColor = if (isAbsent) Color.Gray else MaterialTheme.colorScheme.onSurface

    val emoji = mapIdToEmoji(student.studentNumber)

    Column(
        modifier = modifier.alpha(if (isAbsent) 0.5f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual representation component
        EmojiWithBlob(
            emoji = emoji,
            color = emojiColor,
            backgroundColor = backgroundColor,
            modifier = modifier.alpha(0.9f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Student identification text
        Text(
            text = student.effectiveName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = modifier.alpha(if (isAbsent) 0.7f else 1f),
            )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Composites an emoji over a decorative blob shape.
 * * @param emoji The text-based emoji to display.
 * @param backgroundColor The base color for the background shape.
 * @param modifier Decorator for the emoji container.
 */
@Composable
fun EmojiWithBlob(
    emoji: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background decorative element
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 52.dp)
                .rotate(-10f)
                .background(
                    color = backgroundColor.copy(),
                    shape = BlobShape
                )
        )

        // Centered emoji with optical alignment adjustment
        Text(
            text = emoji,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 44.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .offset(x = (-4).dp)
        )
    }
}