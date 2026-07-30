/**
 * TabulaCard.kt
 *
 * A customized Card component for the TabulaVia application.
 * Ensures a consistent white background and elevation across the app.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    val cardColors = CardDefaults.cardColors(
        containerColor = Color.White,
        contentColor = Color.Black
    )
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            elevation = cardElevation,
            colors = cardColors,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            elevation = cardElevation,
            colors = cardColors,
            content = content
        )
    }
}
