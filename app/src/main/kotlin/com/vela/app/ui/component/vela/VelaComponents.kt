package com.vela.app.ui.component.vela

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.vela.app.LocalDatabase
import com.vela.app.db.entities.Album
import com.vela.app.db.entities.Artist
import com.vela.app.db.entities.LocalItem
import com.vela.app.db.entities.Playlist
import com.vela.app.db.entities.Song
import com.vela.app.extensions.toMediaItem
import com.vela.app.playback.PlayerConnection
import com.vela.app.playback.queues.ListQueue
import com.vela.app.playback.queues.YouTubeAlbumRadio
import com.vela.app.playback.queues.YouTubeQueue
import com.vela.app.ui.component.MenuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.vela.app.ui.menu.AlbumMenu
import com.vela.app.ui.menu.ArtistMenu
import com.vela.app.ui.menu.PlaylistMenu
import com.vela.app.ui.menu.SongMenu
import com.vela.app.ui.menu.YouTubeAlbumMenu
import com.vela.app.ui.menu.YouTubeArtistMenu
import com.vela.app.ui.menu.YouTubePlaylistMenu
import com.vela.app.ui.menu.YouTubeSongMenu
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Playback + navigation helpers (backend behaviour, Vela surface)
// ---------------------------------------------------------------------------

/** Play a local [Song] list from [startIndex]; toggles pause if it is already current. */
fun playLocalSongs(
    playerConnection: PlayerConnection?,
    songs: List<Song>,
    startIndex: Int = 0,
    title: String? = null,
) {
    if (songs.isEmpty()) return
    val currentId = playerConnection?.mediaMetadata?.value?.id
    if (currentId != null && currentId == songs.getOrNull(startIndex)?.id) {
        playerConnection.togglePlayPause()
        return
    }
    playerConnection?.playQueue(
        ListQueue(
            title = title,
            items = songs.map { it.toMediaItem() },
            startIndex = startIndex,
        ),
    )
}

/** Play an online [SongItem]; toggles pause if it is already current. */
fun playYtSong(playerConnection: PlayerConnection?, item: SongItem) {
    if (playerConnection?.mediaMetadata?.value?.id == item.id) {
        playerConnection.togglePlayPause()
        return
    }
    playerConnection?.playQueue(
        YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id)),
    )
}

/** Play any online item the way its type wants: song, album radio, playlist, artist radio. */
fun playYtItem(playerConnection: PlayerConnection?, item: YTItem) {
    when (item) {
        is SongItem -> playYtSong(playerConnection, item)
        is AlbumItem -> playerConnection?.playQueue(YouTubeAlbumRadio(item.playlistId))
        is PlaylistItem -> {
            val endpoint = item.playEndpoint ?: item.radioEndpoint ?: return
            playerConnection?.playQueue(YouTubeQueue(endpoint))
        }
        is ArtistItem -> {
            val endpoint = item.radioEndpoint ?: item.playEndpoint ?: return
            playerConnection?.playQueue(YouTubeQueue(endpoint))
        }
    }
}

/** Route for an online item. Mirrors the existing nav graph — no new routes invented. */
fun navigateToYtItem(
    navController: NavController,
    item: YTItem,
    playerConnection: PlayerConnection?,
) {
    when (item) {
        is SongItem -> playYtItem(playerConnection, item)
        is AlbumItem -> navController.navigate("album/${item.id}")
        is ArtistItem -> navController.navigate("artist/${item.id}")
        is PlaylistItem -> navigateToPlaylistItem(navController, item)
    }
}

/** Online playlist routing: LM = liked auto-playlist, SE = downloaded auto-playlist. */
fun navigateToPlaylistItem(navController: NavController, playlist: PlaylistItem) {
    when (val playlistId = playlist.id.removePrefix("VL")) {
        "LM" -> navController.navigate("auto_playlist/liked")
        "SE" -> navController.navigate("auto_playlist/downloaded")
        else -> navController.navigate("online_playlist/$playlistId")
    }
}

/** Route for a local library item. Songs play; everything else opens its screen. */
fun navigateToLocalItem(navController: NavController, item: LocalItem) {
    when (item) {
        is Song -> Unit // songs are played by the caller, not navigated to
        is Album -> navController.navigate("album/${item.id}")
        is Artist -> navController.navigate("artist/${item.id}")
        is Playlist -> navController.navigate("local_playlist/${item.playlist.id}")
    }
}

// ---------------------------------------------------------------------------
// Artwork
// ---------------------------------------------------------------------------

/**
 * Artwork with the Vela shape language. Falls back to a surface tile with a
 * music note when there is no image — never a broken-image box.
 */
@Composable
fun VelaArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(VelaShapes.sm),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

// ---------------------------------------------------------------------------
// Rows — the Vela list language (tokens.md §6: 40dp artwork, 12dp radius,
// title SemiBold 13.5sp, artist muted 12sp, accent heart at the trailing edge)
// ---------------------------------------------------------------------------

/** Three tiny accent bars marking the currently playing row. */
@Composable
fun VelaPlayingMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(10.dp, 16.dp, 7.dp).forEach { h ->
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = h)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
            )
        }
    }
}

/** The "E" explicit badge. */
@Composable
fun VelaExplicitBadge() {
    Box(
        modifier = Modifier
            .background(Color.Transparent, RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "E",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun VelaSongRow(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    nowPlayingId: String? = null,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isCurrent = nowPlayingId == song.id
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = VelaSpaces.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VelaArtwork(
            url = song.thumbnailUrl,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(VelaShapes.sm)),
            contentDescription = song.title,
        )
        Spacer(modifier = Modifier.width(VelaSpaces.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (song.song.explicit) {
                    Spacer(modifier = Modifier.width(VelaSpaces.s1))
                    VelaExplicitBadge()
                }
            }
        }
        if (isCurrent && isPlaying) {
            VelaPlayingMark(modifier = Modifier.padding(end = VelaSpaces.s2))
        }
        Icon(
            imageVector = if (song.song.liked) Icons.Filled.Favorite
            else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = if (song.song.liked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.size(18.dp),
        )
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One row for any local library item (Song / Album / Artist / Playlist). */
@Composable
fun VelaLocalRow(
    item: LocalItem,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    nowPlayingId: String? = null,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is Song -> VelaSongRow(
            song = item,
            onClick = onClick,
            onMoreClick = onMoreClick,
            nowPlayingId = nowPlayingId,
            isPlaying = isPlaying,
            modifier = modifier,
        )
        is Album -> VelaGenericRow(
            title = item.title,
            subtitle = (item.artists.joinToString(", ") { it.name })
                .ifBlank { "Album" },
            thumbnailUrl = item.thumbnailUrl,
            circular = false,
            onClick = onClick,
            onMoreClick = onMoreClick,
            modifier = modifier,
        )
        is Artist -> VelaGenericRow(
            title = item.artist.name,
            subtitle = "${item.songCount} songs",
            thumbnailUrl = item.artist.thumbnailUrl,
            circular = true,
            onClick = onClick,
            onMoreClick = onMoreClick,
            modifier = modifier,
        )
        is Playlist -> VelaGenericRow(
            title = item.playlist.name,
            subtitle = "${item.songCount} songs",
            thumbnailUrl = item.thumbnailUrl,
            circular = false,
            onClick = onClick,
            onMoreClick = onMoreClick,
            modifier = modifier,
        )
    }
}

@Composable
fun VelaGenericRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    circular: Boolean,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = VelaSpaces.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VelaArtwork(
            url = thumbnailUrl,
            modifier = Modifier
                .size(40.dp)
                .clip(if (circular) CircleShape else RoundedCornerShape(VelaShapes.sm)),
            contentDescription = title,
        )
        Spacer(modifier = Modifier.width(VelaSpaces.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One row for any online item. Songs show the liked heart from the local DB. */
@Composable
fun VelaYtRow(
    item: YTItem,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    nowPlayingId: String? = null,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val localSong by produceState<Song?>(initialValue = null, item.id) {
        if (item is SongItem) {
            value = withContext(Dispatchers.IO) { database.song(item.id).firstOrNull() }
        }
    }
    val isCurrent = nowPlayingId == item.id
    val subtitle = when (item) {
        is SongItem -> buildString {
            append(item.artists.joinToString(", ") { it.name })
            item.album?.let { append(" · ${it.name}") }
        }
        is AlbumItem -> buildString {
            append(item.artists?.joinToString(", ") { it.name }.orEmpty())
            item.year?.let { append(" · $it") }
        }
        is ArtistItem -> "Artist"
        is PlaylistItem -> buildString {
            item.author?.name?.let { append(it) }
            item.songCountText?.let { append(" · $it") }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = VelaSpaces.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VelaArtwork(
            url = item.thumbnail,
            modifier = Modifier
                .size(40.dp)
                .clip(
                    if (item is ArtistItem) CircleShape
                    else RoundedCornerShape(VelaShapes.sm),
                ),
            contentDescription = item.title,
        )
        Spacer(modifier = Modifier.width(VelaSpaces.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.explicit) {
                    Spacer(modifier = Modifier.width(VelaSpaces.s1))
                    VelaExplicitBadge()
                }
            }
        }
        if (isCurrent && isPlaying) {
            VelaPlayingMark(modifier = Modifier.padding(end = VelaSpaces.s2))
        }
        if (item is SongItem && localSong?.song?.liked == true) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Cards — horizontal strips and grids
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VelaYtCard(
    item: YTItem,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val subtitle = when (item) {
        is SongItem -> item.artists.joinToString(", ") { it.name }
        is AlbumItem -> item.artists?.joinToString(", ") { it.name }.orEmpty()
        is ArtistItem -> "Artist"
        is PlaylistItem -> item.author?.name ?: item.songCountText.orEmpty()
    }
    Column(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(148.dp))
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick),
    ) {
        VelaArtwork(
            url = item.thumbnail,
            modifier = Modifier
                .then(
                    if (fillMaxWidth) Modifier.fillMaxWidth().aspectRatio(1f)
                    else Modifier.size(148.dp),
                )
                .clip(RoundedCornerShape(VelaShapes.lg)),
            contentDescription = item.title,
        )
        Spacer(modifier = Modifier.height(VelaSpaces.s2))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VelaLocalCard(
    item: LocalItem,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val subtitle = when (item) {
        is Song -> item.artists.joinToString(", ") { it.name }
        is Album -> item.artists.joinToString(", ") { it.name }
        is Artist -> "${item.songCount} songs"
        is Playlist -> "${item.songCount} songs"
    }
    Column(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(148.dp))
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick),
    ) {
        VelaArtwork(
            url = item.thumbnailUrl,
            modifier = Modifier
                .then(
                    if (fillMaxWidth) Modifier.fillMaxWidth().aspectRatio(1f)
                    else Modifier.size(148.dp),
                )
                .clip(RoundedCornerShape(VelaShapes.lg)),
            contentDescription = item.title,
        )
        Spacer(modifier = Modifier.height(VelaSpaces.s2))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VelaArtistCircle(
    artist: Artist,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(104.dp)
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VelaArtwork(
            url = artist.artist.thumbnailUrl,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentDescription = artist.artist.name,
        )
        Spacer(modifier = Modifier.height(VelaSpaces.s2))
        Text(
            text = artist.artist.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${artist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Section headers, chips, hero card, search field, empty state
// ---------------------------------------------------------------------------

/**
 * Section header: lowercase kicker over an ExtraBold title (tokens.md §2 —
 * section labels stay lowercase where they are Vela's own words).
 */
@Composable
fun VelaSectionHeader(
    title: String,
    kicker: String? = null,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (kicker != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VelaAccentDot()
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = kicker,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(VelaSpaces.s1))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onSeeAll != null) {
                Text(
                    text = "see all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onSeeAll)
                        .padding(start = VelaSpaces.s3, top = VelaSpaces.s1, bottom = VelaSpaces.s1),
                )
            }
        }
    }
}

/** Pill chip, 32dp. Active = accent border + accent text, never a full accent fill. */
@Composable
fun VelaChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
        ),
        onClick = onClick,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = VelaSpaces.s3,
                vertical = VelaSpaces.s2,
            ),
            maxLines = 1,
        )
    }
}

/**
 * The signature featured card (tokens.md §6): gradient, white text, white
 * 44dp circular play button, optional radio action.
 */
@Composable
fun VelaHeroCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    kicker: String,
    onPlay: () -> Unit,
    onRadio: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VelaShapes.lg),
        color = Color.Transparent,
        onClick = onCardClick ?: {},
    ) {
        Box(
            modifier = Modifier
                .background(velaHeroGradient())
                .padding(VelaSpaces.s5),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VelaArtwork(
                    url = artworkUrl,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(VelaShapes.md)),
                    contentDescription = title,
                )
                Spacer(modifier = Modifier.width(VelaSpaces.s4))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kicker,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(modifier = Modifier.height(VelaSpaces.s1))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(VelaSpaces.s3))
                    Row(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2)) {
                        IconButton(
                            onClick = onPlay,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        if (onRadio != null) {
                            IconButton(
                                onClick = onRadio,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Radio,
                                    contentDescription = "Start radio",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pill search field: surfaceVariant fill, no underline, search + clear actions. */
@Composable
fun VelaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun VelaEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(VelaSpaces.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(VelaSpaces.s4))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(VelaSpaces.s1))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun VelaLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(VelaSpaces.s8),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// ---------------------------------------------------------------------------
// Menus — plain helpers, called from click handlers (never during composition).
// They open the existing backend menus through the shared MenuState bottom
// sheet (same plumbing the old app uses). Callers read LocalMenuState.current
// once and pass it in.
// ---------------------------------------------------------------------------

/** Open the right long-press menu for a local song. */
fun showLocalSongMenu(
    menuState: MenuState,
    song: Song,
    navController: NavController,
) {
    menuState.show {
        SongMenu(
            originalSong = song,
            navController = navController,
            onDismiss = menuState::dismiss,
        )
    }
}

/** Open the right long-press menu for an online item. */
fun showYtItemMenu(
    menuState: MenuState,
    item: YTItem,
    navController: NavController,
    scope: CoroutineScope,
) {
    when (item) {
        is SongItem -> menuState.show {
            YouTubeSongMenu(
                song = item,
                navController = navController,
                onDismiss = menuState::dismiss,
            )
        }
        is AlbumItem -> menuState.show {
            YouTubeAlbumMenu(
                albumItem = item,
                navController = navController,
                onDismiss = menuState::dismiss,
            )
        }
        is ArtistItem -> menuState.show {
            YouTubeArtistMenu(
                artist = item,
                onDismiss = menuState::dismiss,
            )
        }
        is PlaylistItem -> menuState.show {
            YouTubePlaylistMenu(
                playlist = item,
                coroutineScope = scope,
                onDismiss = menuState::dismiss,
            )
        }
    }
}

/** Open the right long-press menu for a local library item. */
fun showLocalItemMenu(
    menuState: MenuState,
    item: LocalItem,
    navController: NavController,
    scope: CoroutineScope,
) {
    when (item) {
        is Song -> menuState.show {
            SongMenu(
                originalSong = item,
                navController = navController,
                onDismiss = menuState::dismiss,
            )
        }
        is Album -> menuState.show {
            AlbumMenu(
                originalAlbum = item,
                navController = navController,
                onDismiss = menuState::dismiss,
            )
        }
        is Artist -> menuState.show {
            ArtistMenu(
                originalArtist = item,
                coroutineScope = scope,
                onDismiss = menuState::dismiss,
            )
        }
        is Playlist -> menuState.show {
            PlaylistMenu(
                playlist = item,
                coroutineScope = scope,
                onDismiss = menuState::dismiss,
            )
        }
    }
}
