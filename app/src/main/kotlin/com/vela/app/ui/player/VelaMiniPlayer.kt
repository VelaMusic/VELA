package com.vela.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vela.app.LocalPlayerConnection
import com.vela.app.ui.theme.LocalVelaAccents
import com.vela.app.ui.theme.VelaShapes
import com.vela.app.ui.theme.VelaSpace
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ---------------------------------------------------------------------------
// Vela mini-player — the now-playing strip that rides above the bottom bar.
//
// Tokens.md §6, bottom bar: NOT YouTube Music's red bar. A `colorSurface`
// card with an accent progress line, 20dp radius, 1px divider border.
// 40dp rounded-square artwork, title SemiBold 13.5sp, artist muted 12sp,
// accent heart at the trailing edge. Tap anywhere opens the full player.
//
// Wiring (the old app backend, unchanged): PlayerConnection.mediaMetadata /
// isPlaying / currentSong (liked) / togglePlayPause() / toggleLike() /
// seekToNext(). Progress from ExoPlayer.currentPosition / duration,
// ticked every 500ms while playing.
// ---------------------------------------------------------------------------

private val MiniArtworkShape = RoundedCornerShape(11.dp)

@Composable
fun VelaMiniPlayer(
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val accents = LocalVelaAccents.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    // Nothing to show until a song is loaded.
    val metadata = mediaMetadata ?: return

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, metadata.id) {
        while (isActive) {
            val player = playerConnection.player
            val duration = player.duration
            progress = if (duration > 0) {
                (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            } else {
                0f
            }
            if (!isPlaying) break
            delay(500)
        }
    }

    val isLiked = currentSong?.song?.liked == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(VelaShapes.Lg)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, VelaShapes.Lg)
            .clickable { onOpenPlayer() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = VelaSpace.S3,
                    end = VelaSpace.S2,
                    top = VelaSpace.S2,
                    bottom = VelaSpace.S2,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (metadata.thumbnailUrl != null) {
                AsyncImage(
                    model = metadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MiniArtworkShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MiniArtworkShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(VelaSpace.S3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metadata.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { playerConnection.toggleLike() },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Liked" else "Like",
                    tint = if (isLiked) accents.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = { playerConnection.togglePlayPause() },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
            IconButton(
                onClick = { playerConnection.seekToNext() },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // Accent progress line — 2dp, rides the bottom edge of the card.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(accents.primary),
            )
        }
    }
}
