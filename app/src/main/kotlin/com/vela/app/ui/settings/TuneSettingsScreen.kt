package com.vela.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vela.app.BuildConfig
import com.vela.app.constants.AudioLoudnessPreset
import com.vela.app.constants.AudioLoudnessPresetKey
import com.vela.app.constants.AudioNormalizationKey
import com.vela.app.constants.AudioQuality
import com.vela.app.constants.AudioQualityKey
import com.vela.app.constants.AutoDownloadOnLikeKey
import com.vela.app.constants.AutomixCrossfadeKey
import com.vela.app.constants.CrossfadeDurationKey
import com.vela.app.constants.CrossfadeEnabledKey
import com.vela.app.constants.CrossfadeGaplessKey
import com.vela.app.constants.DownloadQuality
import com.vela.app.constants.DownloadQualityKey
import com.vela.app.constants.DownloadWithMetadataKey
import com.vela.app.constants.LyricsClickKey
import com.vela.app.constants.LyricsScrollKey
import com.vela.app.constants.LyricsTextSizeKey
import com.vela.app.constants.PersistentQueueKey
import com.vela.app.constants.PreferredLyricsProviderKey
import com.vela.app.constants.SkipSilenceKey
import com.vela.app.ui.theme.VelaGradientBrush
import com.vela.app.ui.theme.VelaSpace
import com.vela.app.utils.rememberEnumPreference
import com.vela.app.utils.rememberPreference

// ---------------------------------------------------------------------------
// Tune — Vela's Settings.
//
// Sections: colourway (live switcher) · playback · downloads · lyrics ·
// about. Every row writes the same DataStore keys the old app's backend reads,
// so nothing about playback, downloads or lyrics behaviour changes — only
// the room they live in.
//
// Deliberately NOT here (Vela decisions, see README.md in this staging dir):
//  - Dark mode (DarkModeKey ON/OFF/AUTO): the colourway IS the theme.
//    Cream Mono is the light theme; the other four are dark.
//  - Dynamic colour / theme colour pickers: Vela has five fixed colourways.
//  - the old app's account / Listen Together / AI / Discord / integrations
//    screens: owned by the Home & Library agent or the integrator; Tune
//    covers appearance, playback, downloads, lyrics and about only.
// ---------------------------------------------------------------------------

@Composable
fun TuneSettingsScreen(
    navController: NavController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = VelaSpace.S5),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.padding(top = VelaSpace.S3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(modifier = Modifier.padding(bottom = VelaSpace.S4)) {
                    Text(
                        text = "tune",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Your music. Your wave.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = VelaSpace.S1),
                    )
                }
            }
        }

        // -- colourway: the live switcher --------------------------------
        item(key = "section-colourway") { VelaSection("colourway") }
        item(key = "colourway-picker") {
            ColourwayPicker(
                modifier = Modifier.padding(bottom = VelaSpace.S2),
            )
            Text(
                text = "Switching re-skins the whole app. Cream Mono is the light theme — the rest stay dark.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = VelaSpace.S4),
            )
        }

        // -- playback ------------------------------------------------------
        item(key = "section-playback") { VelaSection("playback") }
        item(key = "playback-quality") {
            val audioQuality by rememberEnumPreference(AudioQualityKey, AudioQuality.OPUS)
            VelaInfoRow(
                title = "Audio quality",
                value = audioQuality.name.lowercase() + " · best available",
            )
        }
        item(key = "playback-normalization") {
            var normalization by rememberPreference(AudioNormalizationKey, true)
            VelaSwitchRow(
                title = "Normalize volume",
                description = "Even out loudness between tracks.",
                checked = normalization,
                onCheckedChange = { normalization = it },
            )
        }
        item(key = "playback-loudness") {
            var preset by rememberEnumPreference(
                AudioLoudnessPresetKey, AudioLoudnessPreset.NORMAL,
            )
            VelaSelectRow(
                title = "Loudness level",
                description = "Overall listening level on top of normalization.",
                value = preset.name.lowercase(),
                options = AudioLoudnessPreset.entries.map { it.name.lowercase() },
                onSelect = { name ->
                    preset = AudioLoudnessPreset.valueOf(name.uppercase())
                },
            )
        }
        item(key = "playback-crossfade") {
            var enabled by rememberPreference(CrossfadeEnabledKey, false)
            VelaSwitchRow(
                title = "Crossfade",
                description = "Blend the end of one song into the next.",
                checked = enabled,
                onCheckedChange = { enabled = it },
            )
        }
        item(key = "playback-crossfade-duration") {
            var enabled by rememberPreference(CrossfadeEnabledKey, false)
            var duration by rememberPreference(CrossfadeDurationKey, 5f)
            if (enabled) {
                VelaSliderRow(
                    title = "Crossfade length",
                    description = "${duration.toInt()} seconds",
                    value = duration,
                    range = 1f..12f,
                    onChange = { duration = it },
                )
            }
        }
        item(key = "playback-gapless") {
            var gapless by rememberPreference(CrossfadeGaplessKey, true)
            VelaSwitchRow(
                title = "Gapless playback",
                description = "No silence between continuous tracks.",
                checked = gapless,
                onCheckedChange = { gapless = it },
            )
        }
        item(key = "playback-automix") {
            var automix by rememberPreference(AutomixCrossfadeKey, false)
            VelaSwitchRow(
                title = "Automix crossfade",
                description = "Crossfade while automix builds the next track.",
                checked = automix,
                onCheckedChange = { automix = it },
            )
        }
        item(key = "playback-persistent-queue") {
            var persistent by rememberPreference(PersistentQueueKey, true)
            VelaSwitchRow(
                title = "Keep queue after restart",
                description = "Pick up where you left off.",
                checked = persistent,
                onCheckedChange = { persistent = it },
            )
        }
        item(key = "playback-skip-silence") {
            var skipSilence by rememberPreference(SkipSilenceKey, false)
            VelaSwitchRow(
                title = "Skip silence",
                description = "Jump over quiet gaps inside tracks.",
                checked = skipSilence,
                onCheckedChange = { skipSilence = it },
            )
        }

        // -- downloads -----------------------------------------------------
        item(key = "section-downloads") { VelaSection("downloads") }
        item(key = "downloads-quality") {
            val quality by rememberEnumPreference(
                DownloadQualityKey, DownloadQuality.YOUTUBE,
            )
            VelaInfoRow(
                title = "Download quality",
                value = quality.name.lowercase() + " · best available",
            )
        }
        item(key = "downloads-auto-like") {
            var autoDownload by rememberPreference(AutoDownloadOnLikeKey, false)
            VelaSwitchRow(
                title = "Save songs you like",
                description = "Automatically download a song when you like it.",
                checked = autoDownload,
                onCheckedChange = { autoDownload = it },
            )
        }
        item(key = "downloads-metadata") {
            var withMetadata by rememberPreference(DownloadWithMetadataKey, true)
            VelaSwitchRow(
                title = "Keep song info in downloads",
                description = "Store title, artist and artwork with the file.",
                checked = withMetadata,
                onCheckedChange = { withMetadata = it },
            )
        }

        // -- lyrics --------------------------------------------------------
        item(key = "section-lyrics") { VelaSection("lyrics") }
        item(key = "lyrics-provider") {
            val provider by rememberPreference(PreferredLyricsProviderKey, "")
            VelaInfoRow(
                title = "Lyrics provider",
                value = provider.ifBlank { "automatic" },
            )
        }
        item(key = "lyrics-autoscroll") {
            var autoScroll by rememberPreference(LyricsScrollKey, true)
            VelaSwitchRow(
                title = "Follow the song",
                description = "Scroll the lyrics along with the music.",
                checked = autoScroll,
                onCheckedChange = { autoScroll = it },
            )
        }
        item(key = "lyrics-tap-seek") {
            var tapToSeek by rememberPreference(LyricsClickKey, true)
            VelaSwitchRow(
                title = "Tap a line to jump there",
                description = "Seeking from the lyrics, line by line.",
                checked = tapToSeek,
                onCheckedChange = { tapToSeek = it },
            )
        }
        item(key = "lyrics-text-size") {
            var textSize by rememberPreference(LyricsTextSizeKey, 24f)
            VelaSliderRow(
                title = "Lyrics text size",
                description = "${textSize.toInt()} sp",
                value = textSize,
                range = 16f..32f,
                onChange = { textSize = it },
            )
        }

        // -- about ---------------------------------------------------------
        item(key = "section-about") { VelaSection("about") }
        item(key = "about-card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(VelaGradientBrush())
                    .padding(VelaSpace.S5),
            ) {
                Column {
                    Text(
                        text = "vela",
                        style = MaterialTheme.typography.displayLarge,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                    Text(
                        text = "Your music. Your wave.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = VelaSpace.S1),
                    )
                }
            }
            Spacer(modifier = Modifier.height(VelaSpace.S4))
        }
        item(key = "about-version") {
            VelaInfoRow(
                title = "Version",
                value = BuildConfig.VERSION_NAME,
            )
        }
        item(key = "about-github") {
            VelaLinkRow(
                title = "Vela on GitHub",
                description = "Source code, issues and releases.",
                onClick = { uriHandler.openUri("https://github.com/VelaMusic/VELA") },
            )
        }
        item(key = "about-license") {
            VelaInfoRow(
                title = "License",
                value = "GPL-3.0 · free software",
            )
        }
        item(key = "about-all-settings") {
            VelaLinkRow(
                title = "All settings",
                description = "Account, appearance, content, player and more.",
                onClick = { navController.navigate("settings/classic") },
            )
        }
        item(key = "end") {
            Spacer(modifier = Modifier.height(VelaSpace.S12))
        }
    }
}

// ---------------------------------------------------------------------------
// Rows — Vela's own settings components (not the old app's Preference rows).
// Surface cards, 20dp radius, 1px divider border, 16dp padding.
// ---------------------------------------------------------------------------

@Composable
private fun VelaSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = VelaSpace.S6, bottom = VelaSpace.S2),
    )
}

@Composable
private fun VelaRowCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = VelaSpace.S2)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier,
            )
            .padding(VelaSpace.S4),
    ) {
        content()
    }
}

@Composable
private fun VelaSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    VelaRowCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = VelaSpace.S1),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun VelaSelectRow(
    title: String,
    description: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    options.forEach { option ->
                        TextButton(
                            onClick = {
                                onSelect(option)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (option == value) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        )
    }
    VelaRowCard(onClick = { showDialog = true }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = VelaSpace.S1),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VelaSliderRow(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    VelaRowCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = VelaSpace.S1, bottom = VelaSpace.S2),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
private fun VelaInfoRow(title: String, value: String) {
    VelaRowCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VelaLinkRow(title: String, description: String, onClick: () -> Unit) {
    VelaRowCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = VelaSpace.S1),
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
