/**
 * TabulaTopBar.kt
 *
 * Custom TopAppBar for the Tabula Via application.
 * Provides a consistent style and includes a real-time synchronization indicator.
 */

package edu.jm.tabulavia.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.BuildConfig
import edu.jm.tabulavia.viewmodel.ClassViewModel

/**
 * A custom top app bar that displays a synchronization indicator when active.
 *
 * @param title The text to be displayed in the app bar.
 * @param viewModel The ViewModel providing the synchronization state.
 * @param navigationIcon Optional composable for the navigation icon.
 * @param actions Optional composable for the actions in the app bar.
 * @param colors The colors to be used for the app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabulaTopBar(
    title: String,
    viewModel: ClassViewModel,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
//    todo: check colors
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Determine the title color based on flavor and title content
                val isMainTitle = title.startsWith("Tabula Via")
                val titleColor = if (isMainTitle && BuildConfig.FLAVOR == "dev")
                    Color.Yellow else MaterialTheme.colorScheme.onPrimaryContainer

                Text(
                    text = title,
                    color = titleColor
                )

                // Sync indicator state
                val isSyncing by viewModel.isSyncing.collectAsState()

                if (isSyncing) {
                    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    // Elevated pulsing dot
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(6.dp)
                            .align(Alignment.Top)
                            .offset(y = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                shape = CircleShape
                            )
                    )
                }
            }
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors
    )
}
