package com.vela.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import coil3.compose.AsyncImage
import com.vela.app.LocalDownloadUtil
import com.vela.app.LocalPlayerConnection
import com.vela.app.playback.ExoDownloadService
import com.vela.app.ui.theme.LocalVelaAccents
import com.vela.app.ui.theme.VelaGradientBrush
import com.vela.app.ui.theme.VelaShapes
import com.vela.app.ui.theme.VelaSpace
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.vela.app.extensions.move
import sh.calvin.reorderable.rememberReorderableLazyListState

// ---------------------------------------------------------------------------
// Vela Player — "The Current".
//
// One continuous river, top to bottom: artwork hero → title → gradient
// control card (progress + transport) → actions → synced lyrics → Queue
// River. Nothing here copies the old app's player (slider + buttons on a plain
// background, lyrics as a separate sheet) or YouTube Music's bottom sheet.
// The river is the design: now-playing flows down into lyrics, lyrics flow
// down into what's next.
//
// Wiring (all the old app backend, unchanged):
//  - Playback state: PlayerConnection.isPlaying / playbackState /
//    mediaMetadata / currentSong / error; transport via
//    togglePlayPause() / seekToNext() / seekToPrevious() / seekTo(ms).
//  - Position: ExoPlayer.currentPosition / duration, ticked every 100ms
//    while playing (same pattern as the old app's Player.kt).
//  - Like: currentSong.song.liked + toggleLike(); library: toggleLibrary().
//  - Download: LocalDownloadUtil.getDownload(id) + DownloadService
//    sendAddDownload / sendRemoveDownload (ExoDownloadService).
//  - Sleep timer: playerConnection.service.sleepTimer (start/clear/isActive).
//  - Radio: playerConnection.startRadioSeamlessly().
//  - Queue: QueueRiver.kt (queueWindows / currentWindowIndex).
//
// River item indices (the drag translation in onMove depends on this count):
//  0 top bar · 1 artwork · 2 title · 3 control card · 4 actions ·
//  5 lyrics header · 6 lyrics · 7 queue header · 8 queue current card
//  (only when a song is playing) · 9+ flowing queue rows.
// ---------------------------------------------------------------------------

private val SleepOptions = listOf(
    "Off" to 0,
    "10 minutes" to 10,
    "15 minutes" to 15,
    "30 minutes" to 30,
    "45 minutes" to 45,
    "60 minutes" to 60,
    "When this song ends" to -1,
)

@Composable
fun VelaPlayerScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val accents = LocalVelaAccents.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val error by playerConnection.error.collectAsState()
    val shuffleEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()

    // --- position ticker (the old app Player.kt pattern) ---
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(100)
                if (sliderPosition == null) {
                    position = playerConnection.player.currentPosition
                    duration = playerConnection.player.duration
                }
            }
        }
    }
    LaunchedEffect(mediaMetadata?.id) {
        position = playerConnection.player.currentPosition
        duration = playerConnection.player.duration
        sliderPosition = null
    }

    // --- queue drag snapshot (mirrors the old app's Queue.kt) ---
    val mutableQueueWindows = remember { mutableStateListOf<androidx.media3.common.Timeline.Window>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(queueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows)
        }
    }

    val riverState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = riverState,
        scrollThresholdPadding = PaddingValues(top = 72.dp, bottom = 72.dp),
    ) { from, to ->
        val windows = playerConnection.queueWindows.value
        val currentIdx = playerConnection.currentWindowIndex.value
        // River items before the flowing rows: 7 fixed + the current card.
        val queueStart = 7 + if (currentIdx in windows.indices) 1 else 0
        val firstFlowing = (currentIdx + 1).coerceAtLeast(0)
        val maxFlowing = (windows.size - 1 - firstFlowing).coerceAtLeast(0)
        val safeFrom = (from.index - queueStart).coerceIn(0, maxFlowing)
        val safeTo = (to.index - queueStart).coerceIn(0, maxFlowing)
        val windowFrom = firstFlowing + safeFrom
        val windowTo = firstFlowing + safeTo
        dragInfo = (dragInfo?.first ?: windowFrom) to windowTo
        mutableQueueWindows.move(windowFrom, windowTo)
    }
    // Commit the drag to the player when it ends (shuffle-aware, the old app's way).
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                val player = playerConnection.player
                if (!player.shuffleModeEnabled) {
                    player.moveMediaItem(from, to)
                } else {
                    player.setShuffleOrder(
                        DefaultShuffleOrder(
                            playerConnection.queueWindows.value
                                .map { it.firstPeriodIndex }
                                .toMutableList()
                                .move(from, to)
                                .toIntArray(),
                            System.currentTimeMillis(),
                        ),
                    )
                }
                dragInfo = null
            }
        }
    }

    // --- download state for the current song ---
    val download by LocalDownloadUtil.current
        .getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    // --- overflow menu + sleep timer dialog ---
    var showMenu by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    val sleepTimer = playerConnection.service.sleepTimer

    if (showSleepDialog) {
        SleepTimerDialog(
            isActive = sleepTimer.isActive,
            onPick = { minutes ->
                if (minutes == 0) sleepTimer.clear() else sleepTimer.start(minutes)
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false },
        )
    }

    LazyColumn(
        state = riverState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = VelaSpace.S5),
    ) {
        // 0 — top bar.
        item(key = "top-bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VelaSpace.S3, bottom = VelaSpace.S2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "now playing",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (sleepTimer.isActive) {
                        val remaining = sleepTimer.triggerTime - System.currentTimeMillis()
                        Text(
                            text = if (sleepTimer.pauseWhenSongEnd) "sleep when this ends"
                            else "sleep in ${formatVelaDuration(remaining)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accents.primary,
                        )
                    }
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sleep timer") },
                        leadingIcon = {
                            Icon(Icons.Default.Bedtime, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            showSleepDialog = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Start radio") },
                        leadingIcon = {
                            Icon(Icons.Default.Radio, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            playerConnection.startRadioSeamlessly()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Add to library") },
                        leadingIcon = {
                            Icon(Icons.Default.LibraryAdd, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            playerConnection.toggleLibrary()
                        },
                    )
                }
            }
        }

        // 1 — artwork hero, with a soft accent glow behind it (the only
        // glow allowed anywhere in Vela, per tokens.md §5).
        item(key = "artwork") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VelaSpace.S2),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accents.primary.copy(alpha = 0.32f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = mediaMetadata?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .clip(VelaShapes.Lg),
                )
            }
        }

        // 2 — title block.
        item(key = "title") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VelaSpace.S5),
            ) {
                Text(
                    text = mediaMetadata?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = mediaMetadata?.artists?.joinToString(", ") { it.name }
                        ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = VelaSpace.S1),
                )
                val albumTitle = mediaMetadata?.album?.title
                if (albumTitle != null) {
                    Text(
                        text = albumTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = VelaSpace.S1),
                    )
                }
                if (error != null) {
                    Text(
                        text = "couldn't play this one — tap retry below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = VelaSpace.S2),
                    )
                }
            }
        }

        // 3 — the gradient control card: progress + transport.
        item(key = "controls") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VelaSpace.S4)
                    .clip(VelaShapes.Lg)
                    .background(VelaGradientBrush())
                    .padding(VelaSpace.S4),
            ) {
                val shownPosition = sliderPosition ?: position
                val shownDuration = duration.coerceAtLeast(0L)
                Slider(
                    value = shownPosition.toFloat(),
                    onValueChange = { sliderPosition = it.toLong() },
                    onValueChangeFinished = {
                        sliderPosition?.let { playerConnection.seekTo(it) }
                        sliderPosition = null
                    },
                    valueRange = 0f..shownDuration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatVelaDuration(shownPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = formatVelaDuration(shownDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = VelaSpace.S2),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            playerConnection.player.shuffleModeEnabled = !shuffleEnabled
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) Color.White
                            else Color.White.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = { playerConnection.seekToPrevious() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    // White 44dp play circle with a dark glyph (tokens.md §6).
                    IconButton(
                        onClick = { playerConnection.togglePlayPause() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause
                            else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF0B0B0C),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    IconButton(onClick = { playerConnection.seekToNext() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            playerConnection.player.repeatMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                                Color.White.copy(alpha = 0.6f)
                            } else {
                                Color.White
                            },
                        )
                    }
                }
            }
        }

        // 4 — actions: like / download / library / radio.
        item(key = "actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VelaSpace.S4),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val isLiked = currentSong?.song?.liked == true
                VelaActionButton(
                    icon = if (isLiked) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    label = if (isLiked) "liked" else "like",
                    tint = if (isLiked) accents.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { playerConnection.toggleLike() },
                )
                val downloadState = download?.state
                VelaActionButton(
                    icon = when (downloadState) {
                        Download.STATE_COMPLETED -> Icons.Default.DownloadDone
                        Download.STATE_DOWNLOADING,
                        Download.STATE_QUEUED -> Icons.Default.Downloading
                        else -> Icons.Default.Download
                    },
                    label = when (downloadState) {
                        Download.STATE_COMPLETED -> "saved"
                        Download.STATE_DOWNLOADING,
                        Download.STATE_QUEUED -> "saving…"
                        else -> "save"
                    },
                    tint = if (downloadState == Download.STATE_COMPLETED) accents.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        val id = mediaMetadata?.id ?: return@VelaActionButton
                        if (downloadState == Download.STATE_COMPLETED ||
                            downloadState == Download.STATE_DOWNLOADING ||
                            downloadState == Download.STATE_QUEUED
                        ) {
                            DownloadService.sendRemoveDownload(
                                context, ExoDownloadService::class.java, id, false,
                            )
                        } else {
                            val request = DownloadRequest.Builder(id, id.toUri())
                                .setCustomCacheKey(id)
                                .setData((mediaMetadata?.title ?: id).toByteArray())
                                .build()
                            DownloadService.sendAddDownload(
                                context, ExoDownloadService::class.java, request, false,
                            )
                        }
                    },
                )
                VelaActionButton(
                    icon = if (currentSong?.song?.inLibrary != null) {
                        Icons.Default.LibraryAddCheck
                    } else {
                        Icons.Default.LibraryAdd
                    },
                    label = "library",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { playerConnection.toggleLibrary() },
                )
                VelaActionButton(
                    icon = Icons.Default.Radio,
                    label = "radio",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { playerConnection.startRadioSeamlessly() },
                )
            }
        }

        // 5 — lyrics header.
        item(key = "lyrics-header") {
            VelaOverline(
                text = "lyrics",
                modifier = Modifier.padding(top = VelaSpace.S8, bottom = VelaSpace.S2),
            )
        }

        // 6 — synced lyrics, in their own scrollable well inside the river.
        item(key = "lyrics") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
            ) {
                VelaSyncedLyrics(positionProvider = { position.takeIf { it >= 0 } })
            }
        }

        // 7+ — the Queue River.
        queueRiver(
            reorderableState = reorderableState,
            mutableQueueWindows = mutableQueueWindows,
        )

        item(key = "river-end") {
            Spacer(modifier = Modifier.height(VelaSpace.S12))
        }
    }
}

@Composable
private fun VelaActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepTimerDialog(
    isActive: Boolean,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                SleepOptions.forEach { (label, minutes) ->
                    TextButton(
                        onClick = { onPick(minutes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = label +
                                if (minutes == 0 && isActive) " (turn off)" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
