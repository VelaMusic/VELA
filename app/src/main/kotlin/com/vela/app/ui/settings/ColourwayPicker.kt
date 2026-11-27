package com.vela.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vela.app.ui.theme.LocalVelaColourway
import com.vela.app.ui.theme.VelaColourway
import com.vela.app.ui.theme.VelaColourwayKey
import com.vela.app.ui.theme.VelaSpace
import com.vela.app.utils.rememberEnumPreference

// ---------------------------------------------------------------------------
// Colourway picker — Settings → Tune → Colourway.
//
// Tapping a row writes VelaColourwayKey to DataStore. VelaTheme (at the app
// root) reads that key, so the whole app re-skins live: the accent
// crossfades over 400ms, the chrome never changes. Aurora is the default.
// Cream Mono switches the app to the light theme; the other four stay dark.
// ---------------------------------------------------------------------------

/** The 135deg gradient of one colourway, for its picker tile. */
private fun VelaColourway.tileBrush(): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to accents.primary,
        0.6f to accents.secondary,
        1.0f to accents.deep,
    ),
)

@Composable
fun ColourwayPicker(modifier: Modifier = Modifier) {
    var colourway by rememberEnumPreference(VelaColourwayKey, VelaColourway.DEFAULT)
    val active = LocalVelaColourway.current

    Column(modifier = modifier) {
        VelaColourway.entries.forEach { option ->
            val isActive = option == active
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { colourway = option }
                    .padding(vertical = VelaSpace.S2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The tile: the colourway's own gradient, wordmark on top.
                // Cream Mono is the one light tile — black wordmark on cream.
                val tileBackground = if (option == VelaColourway.CREAM_MONO) {
                    Brush.linearGradient(
                        listOf(Color(0xFFF5F5F5), Color(0xFFE9E9E9)),
                    )
                } else {
                    option.tileBrush()
                }
                val wordmarkColor = if (option == VelaColourway.CREAM_MONO) {
                    Color(0xFF14101D)
                } else {
                    Color.White
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18))
                        .background(tileBackground)
                        .then(
                            if (isActive) {
                                Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(18),
                                )
                            } else {
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(18),
                                )
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "vela",
                        style = MaterialTheme.typography.labelLarge,
                        color = wordmarkColor,
                    )
                }
                Spacer(modifier = Modifier.width(VelaSpace.S4))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.name.lowercase().replace('_', ' ') +
                            if (option == VelaColourway.DEFAULT) " · default" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = option.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "In use",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
