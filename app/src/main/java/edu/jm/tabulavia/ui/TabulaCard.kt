/**
 * TabulaCard.kt
 *
 * A customized Card component for the TabulaVia application.
 * Ensures a consistent white background and elevation across the app.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A custom Card with a fixed white background and standard elevation.
 *
 * @param modifier Modifier to be applied to the card.
 * @param onClick Optional callback for click events.
 * @param content The composable content inside the card.
 */
@Composable
fun TabulaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Color.Black // Ensuring high contrast on white
            ),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            content = content
        )
    }
}
