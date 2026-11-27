package com.vela.app.ui.design

import androidx.compose.ui.graphics.Color

/**
 * Vela colourways.
 *
 * Every hex below was sampled from the user's own logo artwork
 * (~/workspace/vela-logo/refined/vela_*_symbol.png) — the wave gradient's
 * dark / mean / light stops — not picked from a palette site.
 *
 * Aurora is the default. There is no lime and no mint anywhere in Vela:
 * lime (#D2E756) was removed by the user, mint (#8FE3BC) belongs to Melo.
 */
enum class VelaColourway(
    val displayName: String,
    val isLight: Boolean,
    val colors: VelaColors,
) {
    AURORA(
        displayName = "Aurora",
        isLight = false,
        colors = VelaColors(
            background = Color(0xFF090711),
            surface = Color(0xFF14101F),
            surfaceVariant = Color(0xFF1E1930),
            accent = Color(0xFFB08DEB),
            accentLight = Color(0xFFEAC1FF),
            accentDeep = Color(0xFF3C2B7C),
            onAccent = Color(0xFF1B1030),
            text = Color(0xFFF2EEFB),
            textMuted = Color(0xFFA79DC4),
            border = Color(0xFF2B2442),
        ),
    ),
    ARCTIC(
        displayName = "Arctic",
        isLight = false,
        colors = VelaColors(
            background = Color(0xFF090712),
            surface = Color(0xFF101623),
            surfaceVariant = Color(0xFF182338),
            accent = Color(0xFF9FC9EA),
            accentLight = Color(0xFFD5F7FD),
            accentDeep = Color(0xFF2A5482),
            onAccent = Color(0xFF0A1E33),
            text = Color(0xFFEDF6FD),
            textMuted = Color(0xFF9DB4C9),
            border = Color(0xFF223349),
        ),
    ),
    ROSE(
        displayName = "Rose",
        isLight = false,
        colors = VelaColors(
            background = Color(0xFF080812),
            surface = Color(0xFF171019),
            surfaceVariant = Color(0xFF241726),
            accent = Color(0xFFE88CA6),
            accentLight = Color(0xFFFFB0AE),
            accentDeep = Color(0xFF6D2B49),
            onAccent = Color(0xFF2B0A18),
            text = Color(0xFFFBEEF2),
            textMuted = Color(0xFFC4A3B2),
            border = Color(0xFF33222C),
        ),
    ),
    CREAM_MONOCHROME(
        displayName = "Cream Mono",
        isLight = true,
        colors = VelaColors(
            // The only light colourway: the cream tile of the user's
            // Cream Monochrome mark (#FFF5EC) is the background, the
            // near-black mark (#080810) is the accent.
            background = Color(0xFFFFF5EC),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF3E9DB),
            accent = Color(0xFF14121A),
            accentLight = Color(0xFF636063),
            accentDeep = Color(0xFF080810),
            onAccent = Color(0xFFFFF5EC),
            text = Color(0xFF14121A),
            textMuted = Color(0xFF6E6A72),
            border = Color(0xFFE3D6C6),
        ),
    ),
    DARK_MONOCHROME(
        displayName = "Dark Mono",
        isLight = false,
        colors = VelaColors(
            background = Color(0xFF07050F),
            surface = Color(0xFF121016),
            surfaceVariant = Color(0xFF1D1B22),
            accent = Color(0xFFE4E4E5),
            accentLight = Color(0xFFFFFFFF),
            accentDeep = Color(0xFF5C5C63),
            onAccent = Color(0xFF0B0B0E),
            text = Color(0xFFF2F2F3),
            textMuted = Color(0xFF9C9CA3),
            border = Color(0xFF26242C),
        ),
    );

    companion object {
        val DEFAULT: VelaColourway = AURORA

        /** DataStore / preferences key the integrator should persist the selection under. */
        const val PREFERENCE_KEY: String = "vela_colourway"

        fun fromStoredName(name: String?): VelaColourway =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * The full token set for one colourway. Screens should read these through
 * [VelaTheme.colors], never hard-code a hex.
 *
 * @property background App background.
 * @property surface Cards, sheets, rows.
 * @property surfaceVariant Tonal buttons, chips (unselected), pressed states.
 * @property accent The colourway's voice — the wave mark's own colour.
 *   Used sparingly: primary actions, the playing state, the progress line.
 * @property accentLight The light end of the wave gradient (highlights, glows).
 * @property accentDeep The deep end of the wave gradient (gradient bases).
 * @property onAccent Text / icons drawn on top of [accent].
 * @property text Primary text.
 * @property textMuted Secondary text, captions, metadata.
 * @property border Hairlines, card outlines, dividers.
 */
data class VelaColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val accent: Color,
    val accentLight: Color,
    val accentDeep: Color,
    val onAccent: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
) {
    /** Scrim for sheets/dialogs — always black, independent of colourway. */
    val scrim: Color = Color(0x99000000)

    /** The wave gradient, deep -> accent -> light. Use for the player glow and hero art. */
    val waveGradient: List<Color> get() = listOf(accentDeep, accent, accentLight)
}
