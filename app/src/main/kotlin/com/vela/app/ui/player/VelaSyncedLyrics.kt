package com.vela.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vela.app.LocalPlayerConnection
import com.vela.app.constants.LyricsClickKey
import com.vela.app.constants.LyricsScrollKey
import com.vela.app.constants.LyricsTextSizeKey
import com.vela.app.db.entities.LyricsEntity
import com.vela.app.ui.theme.LocalVelaAccents
import com.vela.app.ui.theme.VelaSpace
import com.vela.app.utils.rememberPreference

// ---------------------------------------------------------------------------
// Vela synced lyrics — a fresh renderer built on the the old app lyrics backend.
// It reads only `PlayerConnection.currentLyrics` (Room `lyrics` table, fed by
// the existing providers: LRCLIB / Kugou / etc.) and the playback position.
// It does NOT reimplement fetching, romanization or translation: the full
// featured path stays in `com.vela.app.ui.component.Lyrics` (the old app's
// composable, re-skinned by VelaTheme). This view is the Vela-styled synced
// renderer: LRC lines, accent active line, tap-to-seek, auto-scroll.
// ---------------------------------------------------------------------------

/** One timestamped lyric line. */
data class VelaLyricLine(
    val timeMs: Long,
    val text: String,
)

private val LrcTagRegex = Regex("""\[(\d+):(\d+)(?:[.:](\d{1,3}))?\]""")

/**
 * Parses LRC text (`[mm:ss.cc] line`) into timestamped lines, sorted by time.
 * A line may carry several timestamps — each becomes its own entry. Returns
 * an empty list when the text carries no timestamps (plain lyrics).
 */
fun parseVelaLyrics(lrc: String): List<VelaLyricLine> {
    val lines = mutableListOf<VelaLyricLine>()
    for (raw in lrc.lineSequence()) {
        val tags = LrcTagRegex.findAll(raw).toList()
        if (tags.isEmpty()) continue
        val text = raw.substringAfterLast(']').trim()
        if (text.isEmpty()) continue
        for (tag in tags) {
            val minutes = tag.groupValues[1].toLongOrNull() ?: continue
            val seconds = tag.groupValues[2].toLongOrNull() ?: continue
            val fraction = tag.groupValues[3]
            val millis = when (fraction.length) {
                1 -> fraction.toLong() * 100 // tenths
                2 -> fraction.toLong() * 10 // hundredths
                3 -> fraction.toLong() // milliseconds
                else -> 0L
            }
            lines += VelaLyricLine(
                timeMs = minutes * 60_000 + seconds * 1_000 + millis,
                text = text,
            )
        }
    }
    return lines.sortedBy { it.timeMs }
}

/**
 * Synced lyrics for the current song.
 *
 * @param positionProvider current playback position in ms. Read during
 * composition — the parent (VelaPlayerScreen) already ticks its position
 * state while playing, so recomposition drives the active line, exactly
 * like the old app's `Lyrics(sliderPositionProvider)` contract.
 * @param onSeek called with the tapped line's timestamp when tap-to-seek is
 * on; defaults to `playerConnection.seekTo`.
 */
@Composable
fun VelaSyncedLyrics(
    positionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    onSeek: ((Long) -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)

    // Lyrics display preferences — the same keys the old app's player honours.
    val autoScroll by rememberPreference(LyricsScrollKey, true)
    val tapToSeek by rememberPreference(LyricsClickKey, true)
    val textSize by rememberPreference(LyricsTextSizeKey, 24f)

    val accents = LocalVelaAccents.current
    val position = positionProvider()

    val lines = remember(lyricsEntity) {
        parseVelaLyrics(lyricsEntity?.lyrics.orEmpty())
    }
    val translatedLines = remember(lyricsEntity) {
        parseVelaLyrics(lyricsEntity?.translatedLyrics.orEmpty())
    }
    val plainText = remember(lyricsEntity) {
        lyricsEntity?.lyrics.orEmpty().trim()
    }
    val provider = lyricsEntity?.provider?.takeIf { it != "Unknown" }

    val activeIndex = remember(lines, position) {
        if (position == null) -1 else lines.indexOfLast { it.timeMs <= position }
    }

    val listState = rememberLazyListState()

    // Follow the song: keep the active line centred while it plays.
    LaunchedEffect(activeIndex) {
        if (!autoScroll || activeIndex < 0) return@LaunchedEffect
        val viewportHeight = listState.layoutInfo.viewportSize.height
        val centreOffset = if (viewportHeight > 0) -(viewportHeight / 2 - 120) else 0
        runCatching {
            listState.animateScrollToItem(activeIndex, scrollOffset = centreOffset)
        }
    }

    Column(modifier = modifier) {
        if (provider != null) {
            Text(
                text = "via $provider",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = VelaSpace.S2),
            )
        }

        when {
            lyricsEntity == null -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "finding lyrics…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            plainText.isEmpty() || plainText == LyricsEntity.LYRICS_NOT_FOUND -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "no lyrics for this song yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            lines.isEmpty() -> {
                // Plain (unsynced) lyrics — no timestamps to follow.
                Text(
                    text = plainText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = (textSize * 1.6).sp,
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Breathing room above the first line so it can centre.
                    item { Spacer(modifier = Modifier.height(VelaSpace.S16)) }

                    itemsIndexed(lines, key = { index, line -> "$index-${line.timeMs}" }) { index, line ->
                        val isActive = index == activeIndex
                        val translation = translatedLines.getOrNull(index)?.text
                            .takeIf { !it.isNullOrBlank() && it != line.text }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = tapToSeek) {
                                    (onSeek ?: { playerConnection.seekTo(it) })(line.timeMs)
                                }
                                .padding(vertical = VelaSpace.S2)
                                .alpha(if (isActive) 1f else 0.45f),
                        ) {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = if (isActive) textSize.sp else (textSize * 0.85f).sp,
                                    lineHeight = (textSize * 1.35f).sp,
                                ),
                                color = if (isActive) accents.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isActive && translation != null) {
                                Text(
                                    text = translation,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = VelaSpace.S1),
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(VelaSpace.S16)) }
                }
            }
        }
    }
}
