package com.vela.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vela.app.utils.rememberEnumPreference

// ---------------------------------------------------------------------------
// Vela design tokens — single source of truth is ~/workspace/vela-design/
// tokens.md (v1.0, 2026-11-27). Every hex below is copied from that file.
// Hard rules: NO lime (#D2E756), NO mint (#8FE3BC), NO dot-matrix wordmark,
// NO Melo Speed Dial / Artist dial / mood tiles, NO the old app / Echo layouts.
// ---------------------------------------------------------------------------

/** DataStore key for the chosen colourway. Read by [VelaTheme] at the app root. */
val VelaColourwayKey = stringPreferencesKey("velaColourway")

/**
 * The four accent stops of one colourway. Kept separate from the enum so the
 * rendered colours can animate (400ms crossfade) while the enum stays fixed.
 */
data class VelaAccents(
    /** Primary accent — the wave itself. */
    val primary: Color,
    /** Secondary accent — gradient 60% stop. */
    val secondary: Color,
    /** Deep — gradient end stop. */
    val deep: Color,
    /** Gradient mid-stop. */
    val mid: Color,
)

/**
 * The five Vela colourways. Aurora is the default. Cream Mono is the only
 * light theme; the other four stay dark. The chrome never changes — only the
 * accent stops do.
 */
enum class VelaColourway(
    val accents: VelaAccents,
    /** True when this colourway renders the light theme. */
    val isLight: Boolean,
    /** Short human description shown in the colourway picker. */
    val tagline: String,
) {
    AURORA(
        accents = VelaAccents(
            primary = Color(0xFFB086FF),
            secondary = Color(0xFF7B4FE0),
            deep = Color(0xFF33256D),
            mid = Color(0xFFAA84FD),
        ),
        isLight = false,
        tagline = "Violet dusk · the dramatic one",
    ),
    ARCTIC(
        accents = VelaAccents(
            primary = Color(0xFFA9D8FE),
            secondary = Color(0xFF4E8FD6),
            deep = Color(0xFF254B79),
            mid = Color(0xFF96C5F7),
        ),
        isLight = false,
        tagline = "Ice blue on black · the clean one",
    ),
    ROSE(
        accents = VelaAccents(
            primary = Color(0xFFE88CA6),
            secondary = Color(0xFFB64E88),
            deep = Color(0xFF672A47),
            mid = Color(0xFFD96C8B),
        ),
        isLight = false,
        tagline = "Pink-magenta warmth · late nights",
    ),
    CREAM_MONO(
        accents = VelaAccents(
            primary = Color(0xFF000000),
            secondary = Color(0xFF14101D),
            deep = Color(0xFFF5F5F5),
            mid = Color(0xFF626262),
        ),
        isLight = true,
        tagline = "Black mark on cream · the only light one",
    ),
    DARK_MONO(
        accents = VelaAccents(
            primary = Color(0xFFFFFFFF),
            secondary = Color(0xFF9A97A8),
            deep = Color(0xFF23232E),
            mid = Color(0xFF9A97A8),
        ),
        isLight = false,
        tagline = "Pure white on black · one colour",
    );

    companion object {
        val DEFAULT: VelaColourway = AURORA
    }
}

/** Shared dark chrome — identical for every dark colourway (tokens.md §1). */
object VelaChrome {
    val Background = Color(0xFF0B0B0C) // "Vela Black" — never pure #000 in UI chrome
    val Surface = Color(0xFF0E0E16)
    val SurfaceVariant = Color(0xFF13131F)
    val OnBackground = Color(0xFFF4F2FA) // "Vela White"
    val Muted = Color(0xFFA7A3BA)
    val Faint = Color(0xFF6F6B85)
    val Divider = Color(0xFF23233A)

    // Cream Mono (light theme) chrome — tokens.md §1.4.
    val LightBackground = Color(0xFFF5F5F5) // sampled from the PNG, NOT #FFF5EB
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE9E9E9)
    val LightOnBackground = Color(0xFF14101D)
    val LightMuted = Color(0xFF626262)
    val LightDivider = Color(0xFFD8D8D8)
}

/** Shape tokens — tokens.md §3. Never a different radius per corner. */
object VelaShapes {
    val Xs = RoundedCornerShape(8.dp)
    val Sm = RoundedCornerShape(12.dp)
    val Md = RoundedCornerShape(16.dp)
    val Lg = RoundedCornerShape(20.dp) // cards, panels, featured player card
    val Xl = RoundedCornerShape(28.dp) // hero / featured surfaces
    val Pill = RoundedCornerShape(50) // buttons, chips, progress bars, toggles
}

/** Spacing scale — 4dp base unit, tokens.md §4. */
object VelaSpace {
    val S1 = 4.dp
    val S2 = 8.dp
    val S3 = 12.dp
    val S4 = 16.dp
    val S5 = 20.dp
    val S6 = 24.dp
    val S8 = 32.dp
    val S12 = 48.dp
    val S16 = 64.dp
}

/** Motion tokens — tokens.md §5. Colourway switches crossfade in 400ms. */
object VelaMotion {
    const val MicroMs = 150
    const val StandardMs = 250
    const val AccentMs = 400 // colourway / accent crossfade
    const val ToastMs = 300
}

// ---------------------------------------------------------------------------
// Typography — Plus Jakarta Sans (headings) + Inter (body), downloadable via
// the Google Fonts provider. Font files are NOT bundled: they are binary and
// cannot be pushed through the GitHub App route. Falls back to the system
// sans-serif while a family is still downloading, never blocks first paint.
//
// INTEGRATOR NOTE: add `androidx.compose.ui:ui-text-google-fonts` and a
// `res/values/font_certs.xml` holding `com_google_android_gms_fonts_certs`
// (standard Google Fonts provider setup) for the download to work.
// ---------------------------------------------------------------------------

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.vela.app.R.array.com_google_android_gms_fonts_certs,
)

private val JakartaSansFamily = FontFamily(
    Font(GoogleFont("Plus Jakarta Sans", GoogleFontProvider), FontWeight.Medium),
    Font(GoogleFont("Plus Jakarta Sans", GoogleFontProvider), FontWeight.SemiBold),
    Font(GoogleFont("Plus Jakarta Sans", GoogleFontProvider), FontWeight.Bold),
    Font(GoogleFont("Plus Jakarta Sans", GoogleFontProvider), FontWeight.ExtraBold),
)

private val InterFamily = FontFamily(
    Font(GoogleFont("Inter", GoogleFontProvider), FontWeight.Normal),
    Font(GoogleFont("Inter", GoogleFontProvider), FontWeight.Medium),
    Font(GoogleFont("Inter", GoogleFontProvider), FontWeight.SemiBold),
)

/** Vela type scale — tokens.md §2. Headings Jakarta Sans, body Inter. */
val VelaTypography = Typography(
    // Display / wordmark — lowercase "vela", ExtraBold, tight tracking.
    displayLarge = TextStyle(
        fontFamily = JakartaSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp,
        lineHeight = 57.sp,
        letterSpacing = (-2.6).sp,
    ),
    // Headline — screen titles.
    headlineMedium = TextStyle(
        fontFamily = JakartaSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.5).sp,
    ),
    // Title — card titles, song titles.
    titleLarge = TextStyle(
        fontFamily = JakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = JakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Title small — list rows (song title in queue / library rows).
    titleSmall = TextStyle(
        fontFamily = JakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.1).sp,
    ),
    // Body — artist names, descriptions.
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    // Body emphasis / UI text.
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    // Label / UI text.
    labelLarge = TextStyle(
        fontFamily = JakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = JakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    // Caption / metadata.
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
)

/** The chosen colourway (enum), available to any composable below [VelaTheme]. */
val LocalVelaColourway = staticCompositionLocalOf { VelaColourway.DEFAULT }

/**
 * The currently rendered accent stops (animated on switch). Gradient brushes
 * and accent-tinted surfaces read this, so a colourway change crossfades
 * everywhere at once instead of snapping.
 */
val LocalVelaAccents = staticCompositionLocalOf { VelaColourway.DEFAULT.accents }

/** The 135-degree signature gradient: accent → secondary (60%) → deep. */
@Composable
fun VelaGradientBrush(): Brush {
    val accents = LocalVelaAccents.current
    return Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to accents.primary,
            0.6f to accents.secondary,
            1.0f to accents.deep,
        ),
    )
}

private fun darkSchemeFor(accents: VelaAccents): ColorScheme = darkColorScheme(
    primary = accents.primary,
    secondary = accents.secondary,
    tertiary = accents.mid,
    background = VelaChrome.Background,
    surface = VelaChrome.Surface,
    surfaceVariant = VelaChrome.SurfaceVariant,
    onBackground = VelaChrome.OnBackground,
    onSurface = VelaChrome.OnBackground,
    onSurfaceVariant = VelaChrome.Muted,
    outline = VelaChrome.Divider,
    outlineVariant = VelaChrome.Divider,
    // Text on the gradient card is white on every dark colourway.
    onPrimary = Color.White,
    onSecondary = Color.White,
)

private fun lightSchemeFor(accents: VelaAccents): ColorScheme = lightColorScheme(
    primary = accents.primary, // Cream Mono: near-black mark
    secondary = accents.secondary,
    tertiary = accents.mid,
    background = VelaChrome.LightBackground,
    surface = VelaChrome.LightSurface,
    surfaceVariant = VelaChrome.LightSurfaceVariant,
    onBackground = VelaChrome.LightOnBackground,
    onSurface = VelaChrome.LightOnBackground,
    onSurfaceVariant = VelaChrome.LightMuted,
    outline = VelaChrome.LightDivider,
    outlineVariant = VelaChrome.LightDivider,
    onPrimary = Color.White,
    onSecondary = Color.White,
)

/**
 * Vela theme root. Reads the persisted colourway ([VelaColourwayKey], Aurora
 * by default) and re-themes the whole app when it changes — the accent
 * crossfades over 400ms, it never snaps. Cream Mono switches the app to the
 * light theme; the other four stay dark.
 *
 * The colourway IS the theme choice in Vela: the old app's DarkModeKey
 * (ON/OFF/AUTO) is intentionally not wired — the picker replaces it.
 */
@Composable
fun VelaTheme(
    content: @Composable () -> Unit,
) {
    var colourway by rememberEnumPreference(VelaColourwayKey, VelaColourway.DEFAULT)
    val target = colourway.accents

    // 400ms accent crossfade on colourway switch (tokens.md §5).
    val animatedPrimary by animateColorAsState(
        targetValue = target.primary,
        animationSpec = tween(VelaMotion.AccentMs),
        label = "velaPrimary",
    )
    val animatedSecondary by animateColorAsState(
        targetValue = target.secondary,
        animationSpec = tween(VelaMotion.AccentMs),
        label = "velaSecondary",
    )
    val animatedDeep by animateColorAsState(
        targetValue = target.deep,
        animationSpec = tween(VelaMotion.AccentMs),
        label = "velaDeep",
    )
    val animatedAccents = VelaAccents(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        deep = animatedDeep,
        mid = target.mid,
    )

    val colorScheme = if (colourway.isLight) {
        lightSchemeFor(animatedAccents)
    } else {
        darkSchemeFor(animatedAccents)
    }

    CompositionLocalProvider(
        LocalVelaColourway provides colourway,
        LocalVelaAccents provides animatedAccents,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VelaTypography,
            shapes = MaterialTheme.shapes.copy(
                extraSmall = VelaShapes.Xs,
                small = VelaShapes.Sm,
                medium = VelaShapes.Md,
                large = VelaShapes.Lg,
                extraLarge = VelaShapes.Xl,
            ),
            content = content,
        )
    }
}
