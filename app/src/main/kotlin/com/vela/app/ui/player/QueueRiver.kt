package com.vela.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import com.vela.app.LocalPlayerConnection
import com.vela.app.extensions.metadata
import com.vela.app.models.MediaMetadata
import com.vela.app.ui.theme.VelaGradientBrush
import com.vela.app.ui.theme.VelaShapes
import com.vela.app.ui.theme.VelaSpace
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

// ---------------------------------------------------------------------------
// Queue River — the up-next queue as one flowing list inside the player
// river (VelaPlayerScreen's single LazyColumn). The current song rides as a
// gradient card at the head of the flow; everything after it streams below
// as rows you can tap to play, swipe away, or drag to reorder (long-press).
//
// Wiring (all the old app backend, unchanged):
//  - `PlayerConnection.queueWindows` — List<Timeline.Window>, one per queue
//    item; `window.mediaItem.metadata` is the MediaMetadata.
//  - `PlayerConnection.currentWindowIndex` — the playing window.
//  - Tap: `player.seekToDefaultPosition(window.firstPeriodIndex)` +
//    `playWhenReady = true` (mirrors the old app's Queue.kt).
//  - Remove: `player.removeMediaItem(window.firstPeriodIndex)`.
//  - Reorder: `player.moveMediaItem(from, to)` — or a shuffle-order rebuild
//    while shuffle is on (mirrors the old app's Queue.kt drag handling).
// ---------------------------------------------------------------------------

/** mm:ss for a duration in milliseconds. */
internal fun formatVelaDuration(ms: Long): String {
    if (ms < 0) return "--:--"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** mm:ss for a duration in whole seconds (MediaMetadata.duration). */
internal fun formatVelaDurationSeconds(seconds: Int): String =
    formatVelaDuration(seconds * 1000L)

/** Overline label used for river sections ("up next", "lyrics", ...). */
@Composable
internal fun VelaOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private val QueueArtworkShape = RoundedCornerShape(11.dp)

/**
 * The Queue River section, appended to the player river's LazyColumn.
 *
 * @param reorderableState the reorderable state created in VelaPlayerScreen
 * (bound to the river's LazyListState).
 * @param mutableQueueWindows the drag-time snapshot list owned by the player
 * screen (mirrors the old app's Queue.kt: the UI reorders live, the player
 * commits on drag end). Only windows after the current one flow here.
 */
@Composable
fun LazyListScope.queueRiver(
    reorderableState: ReorderableLazyListState,
    mutableQueueWindows: List<Timeline.Window>,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()

    val upNext = remember(queueWindows, currentWindowIndex) {
        if (currentWindowIndex in queueWindows.indices) {
            queueWindows.subList(currentWindowIndex + 1, queueWindows.size)
        } else {
            queueWindows
        }
    }
    // The flowing rows: the drag snapshot, filtered to windows after the
    // current one, in snapshot order (so a live drag reorders the river).
    val flowingWindows = remember(mutableQueueWindows, currentWindowIndex, queueWindows) {
        val liveIndexOf = { window: Timeline.Window ->
            queueWindows.indexOfFirst { it.uid == window.uid }
        }
        mutableQueueWindows.filter { liveIndexOf(it) > currentWindowIndex }
    }
    val currentWindow = remember(queueWindows, currentWindowIndex) {
        queueWindows.getOrNull(currentWindowIndex)
    }
    val upNextDurationMs = remember(upNext) {
        upNext.sumOf { (it.mediaItem.metadata?.duration ?: 0) * 1000L }
    }

    item(key = "queue-header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VelaSpace.S8, bottom = VelaSpace.S3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VelaOverline("up next")
            Text(
                text = if (upNext.isEmpty()) "queue clear"
                else "${upNext.size} songs · ${formatVelaDuration(upNextDurationMs)} left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // The current song rides the head of the river as a gradient card.
    currentWindow?.mediaItem?.metadata?.let { metadata ->
        item(key = "queue-current-${currentWindow.uid}") {
            QueueCurrentCard(metadata = metadata)
        }
    }

    itemsIndexed(
        items = flowingWindows,
        key = { _, window -> window.uid.toString() },
    ) { _, window ->
        ReorderableItem(
            state = reorderableState,
            key = window.uid.toString(),
        ) {
            QueueRiverRow(
                window = window,
                reorderableState = reorderableState,
                dragHandleModifier = Modifier.draggableHandle(),
                onPlay = {
                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                    playerConnection.player.playWhenReady = true
                },
                onRemove = {
                    playerConnection.player.removeMediaItem(window.firstPeriodIndex)
                },
            )
        }
    }

    if (upNext.isEmpty()) {
        item(key = "queue-empty") {
            Text(
                text = "Nothing after this — start a radio and the river keeps flowing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = VelaSpace.S4),
            )
        }
    }
}

/** The current song as a gradient card at the head of the river. */
@Composable
private fun QueueCurrentCard(metadata: MediaMetadata) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VelaShapes.Lg)
            .background(VelaGradientBrush())
            .padding(VelaSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = metadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(QueueArtworkShape),
        )
        Spacer(modifier = Modifier.width(VelaSpace.S3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.titleSmall,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metadata.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "playing",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        )
    }
    Spacer(modifier = Modifier.height(VelaSpace.S2))
}

/** One flowing row: tap to play, swipe to remove, long-press to drag. */
@Composable
private fun QueueRiverRow(
    window: Timeline.Window,
    reorderableState: ReorderableLazyListState,
    dragHandleModifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val metadata = window.mediaItem.metadata ?: return
    var processedDismiss by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance },
    )

    LaunchedEffect(dismissState.currentValue) {
        val value = dismissState.currentValue
        if (!processedDismiss &&
            (value == SwipeToDismissBoxValue.StartToEnd ||
                value == SwipeToDismissBoxValue.EndToStart)
        ) {
            processedDismiss = true
            onRemove()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(VelaShapes.Sm)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = VelaSpace.S4),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(VelaShapes.Sm)
                .background(MaterialTheme.colorScheme.background)
                .clickable { onPlay() }
                .padding(vertical = VelaSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (metadata.thumbnailUrl != null) {
                AsyncImage(
                    model = metadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(QueueArtworkShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(QueueArtworkShape)
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
            Text(
                text = formatVelaDurationSeconds(metadata.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(VelaSpace.S2))
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .then(dragHandleModifier),
            )
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}
