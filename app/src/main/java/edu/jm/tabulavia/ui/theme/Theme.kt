/**
 * Theme definition for the TabulaVia application.
 * Configures the Material 3 color schemes for light and dark modes.
 */
package edu.jm.tabulavia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Dark color scheme configuration.
 * Uses light variants of the palette for better contrast on dark backgrounds.
 */
private val DarkColorScheme = darkColorScheme(
    primary = LightBlue,
    onPrimary = Color.Black,
    secondary = LightGray,
    onSecondary = Color.Black,
    tertiary = LightRed,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1C1B1F),
    onSurface = Color.White
)

/**
 * Light color scheme configuration.
 * Uses dark variants of the palette for standard visibility.
 */
private val LightColorScheme = lightColorScheme(
    primary = DarkBlue,
    onPrimary = Color.White,
    secondary = DarkGray,
    onSecondary = Color.White,
    tertiary = DarkRed,
    onTertiary = Color.White,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F)
)

/**
 * Main theme composable for the application.
 * * @param darkTheme Whether the system is in dark mode.
 * @param dynamicColor Whether to use Android 12+ dynamic coloring (disabled by default).
 * @param content The composable content to be displayed.
 */
@Composable
fun TabulaViaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determine the color scheme based on settings and OS version
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}