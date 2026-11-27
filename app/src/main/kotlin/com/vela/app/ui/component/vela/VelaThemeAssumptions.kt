package com.vela.app.ui.component.vela

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * VELA THEME ASSUMPTIONS — read before merging.
 *
 * The design-system agent owns the real theme. This file records every token
 * this workstream's screens assume, so the integrator can align names instead
 * of guessing. Source of truth: `~/workspace/vela-design/tokens.md` (v1.0),
 * itself derived from the `vela-identity` board and the five code-refined
 * logo masters. Nothing here invents a token.
 *
 * ASSUMED MaterialTheme.colorScheme MAPPING (Aurora default colourway):
 *   primary          = #B086FF  (wave / primary accent)
 *   secondary        = #7B4FE0  (secondary accent; #AA84FD is the gradient mid-stop)
 *   tertiary         = #33256D  (deep — the gradient's third stop)
 *   background       = #0B0B0C  (Vela Black — never pure #000000 in UI chrome)
 *   surface          = #0E0E16  (cards, panels, sheets)
 *   surfaceVariant   = #13131F  (chips, inputs, raised surfaces)
 *   onBackground     = #F4F2FA  (Vela White — primary text)
 *   onSurface        = #F4F2FA
 *   onSurfaceVariant = #A7A3BA  (muted — secondary text)
 *   outline          = #23233A  (dividers, 1px card borders)
 *   onPrimary        = #FFFFFF  (text on the gradient)
 *   inverseOnSurface = #6F6B85  (faint — tertiary text, inactive icons)
 * Other colourways (Arctic / Rose / Dark Mono) re-theme primary/secondary/
 * tertiary only; the chrome above never changes. Cream Mono swaps the whole
 * scheme to the light theme in tokens.md §1.4 — these screens do not branch
 * on it; they read colorScheme and follow whatever scheme is active.
 *
 * ASSUMED MaterialTheme.typography SLOTS (design-system agent sets these to
 * the tokens.md §2 sizes; the slots are used here, never hard-coded sizes
 * except the wordmark):
 *   displayMedium    -> wordmark "vela" (tokens: Plus Jakarta Sans ExtraBold 57sp;
 *                       the slot is a stand-in until the theme sets display sizes)
 *   headlineSmall    -> screen / section titles (tokens: 27sp ExtraBold)
 *   titleLarge       -> card + song titles (tokens: 19-21sp Bold)
 *   titleMedium      -> row titles (tokens: 13.5sp SemiBold — slot is a stand-in)
 *   bodyLarge        -> artist names / body (tokens: 14-15sp Inter Regular)
 *   bodySmall        -> captions / metadata (tokens: 12sp Inter Regular)
 *   labelLarge       -> labels / chips (tokens: 12-13sp SemiBold)
 *   labelSmall       -> overline kickers (tokens: 11sp SemiBold, uppercase, +10% tracking)
 *
 * ASSUMED FONTS: Plus Jakarta Sans (500/600/700/800) + Inter (400/500/600)
 * via downloadable Google Fonts (provider com.google.android.gms.fonts).
 * No font files are bundled — see tokens.md §2. If the provider is
 * unreachable the theme falls back to system sans-serif; these screens
 * never reference a font resource directly.
 *
 * SHAPES (tokens.md §3): card 20dp, inner card 16dp, pill full, row artwork
 * 12dp, hero 28dp. SPACING (§4): 4dp base — 4/8/12/16/20/24/32/48/64.
 * MOTION (§5): press 250ms, accent crossfade 400ms.
 *
 * HARD RULES (non-negotiable): NO lime, NO mint, NO dot-matrix, NO Melo
 * Speed Dial / Artist dial / mood tiles, NO old-app layouts or strings,
 * NO red anywhere.
 */

/** Corner radii from tokens.md §3. */
object VelaShapes {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 28.dp
}

/** Spacing steps from tokens.md §4 (4dp base unit). */
object VelaSpaces {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp
    val s12 = 48.dp
    val s16 = 64.dp
}

/**
 * The signature Vela gradient: 135deg, accent -> secondary at 60% -> deep.
 * Built from colorScheme so a colourway switch re-themes it (400ms crossfade
 * handled by the theme). Assumes secondary = #7B4FE0 and tertiary = #33256D
 * in the Aurora scheme (see mapping above).
 */
@Composable
fun velaHeroGradient(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.linearGradient(
        colors = listOf(scheme.primary, scheme.secondary, scheme.tertiary),
        // 135deg diagonal: top-left to bottom-right. Offset.Infinite resolves
        // against the draw size in Brush.createShader — this is the documented
        // diagonal-gradient idiom, not a magic number.
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

/**
 * The wordmark: always lowercase "vela", Plus Jakarta Sans ExtraBold, tight
 * tracking. Rendered as text so it never depends on a binary logo asset
 * (the wave-mark PNGs cannot land via the App push route).
 */
@Composable
fun VelaWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 32.sp,
) {
    Text(
        text = "vela",
        modifier = modifier,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            // -4.5% tracking from tokens.md §2.
            letterSpacing = fontSize * -0.045f,
        ),
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** Small accent dot used on mood tiles and section kickers. */
@Composable
fun VelaAccentDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(6.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
    )
}

/** A colour swatch that must never appear in Vela — kept here so a reviewer can grep for it. */
@Suppress("unused")
object VelaForbidden {
    val lime = Color(0xFFD2E756)
    val mint = Color(0xFF8FE3BC)
    val forbiddenRed = Color(0xFFE5232E)
}
