package com.vela.app.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vela shapes, spacing and elevation.
 *
 * Voice: calm, firm, minimal. Generous radii, flat surfaces — depth comes
 * from tone (surface vs surfaceVariant), not from shadows. No glass, no
 * blur, no heavy elevation anywhere.
 */
val VelaShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Pill shape for buttons, chips and the player controls. */
val VelaPillShape: RoundedCornerShape = RoundedCornerShape(50)

/** Artwork corners: album art, artist images, playlist covers. */
val VelaArtworkShape: RoundedCornerShape = RoundedCornerShape(16.dp)

@Immutable
data class VelaSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
    /** Standard screen edge padding. */
    val screenEdge: Dp = 20.dp,
    /** Minimum touch target, per Material guidance. */
    val minTouch: Dp = 48.dp,
)

val LocalVelaSpacing: androidx.compose.runtime.ProvidableCompositionLocal<VelaSpacing> =
    staticCompositionLocalOf { VelaSpacing() }

/**
 * Elevation tokens. Vela is deliberately flat: 0dp almost everywhere,
 * 1dp for a raised card, 2dp for a sheet. Nothing floats higher.
 */
@Immutable
data class VelaElevation(
    val none: Dp = 0.dp,
    val card: Dp = 1.dp,
    val sheet: Dp = 2.dp,
)

val LocalVelaElevation: androidx.compose.runtime.ProvidableCompositionLocal<VelaElevation> =
    staticCompositionLocalOf { VelaElevation() }
