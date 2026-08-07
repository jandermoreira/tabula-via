/**
 * Theme definition for the TabulaVia application.
 * Configures the Material 3 color schemes for light and dark modes.
 */
package edu.jm.tabulavia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TabulaColorScheme = lightColorScheme(

    primary = ForestGreen,
    onPrimary = SnowWhite,
    primaryContainer = PineGreen,
    onPrimaryContainer = SnowWhite,

    secondary = EmeraldGreen,
    onSecondary = SnowWhite,
    secondaryContainer = SageGreen,
    onSecondaryContainer = Charcoal,

    tertiary = MintGreen,
    onTertiary = Charcoal,
    tertiaryContainer = MistGreen,
    onTertiaryContainer = Charcoal,

    background = MistGreen,
    onBackground = Charcoal,

    surface = CloudWhite,
    onSurface = Charcoal,

    surfaceVariant = SilverGray,
    onSurfaceVariant = Slate,

    surfaceDim = PearlGray,
    surfaceBright = SnowWhite,

    surfaceContainerLowest = CloudWhite,
    surfaceContainerLow = SnowWhite,
    surfaceContainer = PearlGray,
    surfaceContainerHigh = SilverGray,
    surfaceContainerHighest = SageGreen,

    outline = Stone,
    outlineVariant = SilverGray,

    error = Alert,
    onError = SnowWhite,
    errorContainer = Rose,
    onErrorContainer = Alert,

    inverseSurface = Charcoal,
    inverseOnSurface = SnowWhite,
    inversePrimary = MintGreen,

    scrim = BlackScrim
)

/**
 * Main theme composable for the application.
 *
 * @param darkTheme Whether the system is in dark mode.
 * @param dynamicColor Whether to use Android 12+ dynamic coloring.
 * @param vibrant Whether to use the high-saturation vibrant color scheme.
 * @param content The composable content to be displayed.
 */
@Composable
fun TabulaViaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    vibrant: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determine the color scheme based on settings and OS version
    val colorScheme = TabulaColorScheme
    // when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//        vibrant -> if (darkTheme) VibrantDarkColorScheme else VibrantLightColorScheme
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
