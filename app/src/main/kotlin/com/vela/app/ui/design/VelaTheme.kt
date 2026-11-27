package com.vela.app.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Vela theme entry point. Wrap each screen (or the whole app) in it:
 *
 * ```
 * VelaTheme(colourway = colourwayState.value) {
 *     // screens read VelaTheme.colors, MaterialTheme.typography, VelaTheme.spacing
 * }
 * ```
 *
 * Colour is NOT derived from the wallpaper, the system, or the playing
 * song — it comes only from the user's chosen [VelaColourway]. The player
 * glow uses the colourway's own wave gradient ([VelaColors.waveGradient]),
 * never a colour extracted from album art.
 */

val LocalVelaColors = compositionLocalOf<VelaColors> { VelaColourway.DEFAULT.colors }
val LocalVelaColourway = compositionLocalOf<VelaColourway> { VelaColourway.DEFAULT }

object VelaTheme {
    /** Current colourway tokens. Read-only; changes when [VelaTheme] recomposes. */
    val colors: VelaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVelaColors.current

    val colourway: VelaColourway
        @Composable
        @ReadOnlyComposable
        get() = LocalVelaColourway.current

    val spacing: VelaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalVelaSpacing.current

    val elevation: VelaElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalVelaElevation.current
}

@Composable
fun VelaTheme(
    colourway: VelaColourway = VelaColourway.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colors = colourway.colors
    val colorScheme = remember(colors, colourway.isLight) { colors.toColorScheme(colourway.isLight) }
    CompositionLocalProvider(
        LocalVelaColors provides colors,
        LocalVelaColourway provides colourway,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography, // design typography held: needs res/font/inter.ttf (binary, not yet in repo); player's downloadable-font typography is canonical
            shapes = VelaShapes,
            content = content,
        )
    }
}

/** Maps Vela tokens onto Material3 so stock components inherit the identity. */
fun VelaColors.toColorScheme(isLight: Boolean): ColorScheme {
    return if (isLight) {
        ColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = surfaceVariant,
            onPrimaryContainer = text,
            secondary = accentLight,
            onSecondary = onAccent,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = text,
            tertiary = accentDeep,
            onTertiary = onAccent,
            tertiaryContainer = surfaceVariant,
            onTertiaryContainer = text,
            error = Color(0xFFB3261E),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textMuted,
            surfaceTint = accent,
            inverseSurface = Color(0xFF14121A),
            inverseOnSurface = Color(0xFFFFF5EC),
            inversePrimary = accentLight,
            surfaceBright = surface,
            surfaceDim = surfaceVariant,
            surfaceContainer = surface,
            surfaceContainerLow = surface,
            surfaceContainerLowest = background,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = surfaceVariant,
            outline = border,
            outlineVariant = border,
            scrim = scrim,
        )
    } else {
        ColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = surfaceVariant,
            onPrimaryContainer = text,
            secondary = accentLight,
            onSecondary = onAccent,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = text,
            tertiary = accentDeep,
            onTertiary = onAccent,
            tertiaryContainer = surfaceVariant,
            onTertiaryContainer = text,
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textMuted,
            surfaceTint = accent,
            inverseSurface = Color(0xFFFFF5EC),
            inverseOnSurface = Color(0xFF14121A),
            inversePrimary = accentDeep,
            surfaceBright = surfaceVariant,
            surfaceDim = background,
            surfaceContainer = surface,
            surfaceContainerLow = background,
            surfaceContainerLowest = background,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = surfaceVariant,
            outline = border,
            outlineVariant = border,
            scrim = scrim,
        )
    }
}

/**
 * Colourway selection state.
 *
 * The integrator owns persistence (DataStore, key
 * [VelaColourway.PREFERENCE_KEY]) and owns the single instance of this
 * class, exposed as a [StateFlow] to the UI. Settings calls [select];
 * [VelaTheme] reads the flow's value. Nothing else in the design system
 * holds mutable state.
 */
class VelaColourwayState(initial: VelaColourway = VelaColourway.DEFAULT) {
    private val _colourway = MutableStateFlow(initial)
    val colourway: StateFlow<VelaColourway> = _colourway

    val current: VelaColourway get() = _colourway.value

    fun select(colourway: VelaColourway) {
        _colourway.value = colourway
    }
}
