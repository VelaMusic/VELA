package com.vela.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.vela.app.LocalPlayerConnection
import com.vela.app.db.entities.Song
import com.vela.app.playback.queues.YouTubeQueue
import com.vela.app.ui.component.LocalMenuState
import com.vela.app.ui.component.vela.VelaArtwork
import com.vela.app.ui.component.vela.VelaChip
import com.vela.app.ui.component.vela.VelaEmptyState
import com.vela.app.ui.component.vela.VelaHeroCard
import com.vela.app.ui.component.vela.VelaLoading
import com.vela.app.ui.component.vela.VelaLocalCard
import com.vela.app.ui.component.vela.VelaSectionHeader
import com.vela.app.ui.component.vela.VelaShapes
import com.vela.app.ui.component.vela.VelaSongRow
import com.vela.app.ui.component.vela.VelaSpaces
import com.vela.app.ui.component.vela.VelaWordmark
import com.vela.app.ui.component.vela.VelaYtCard
import com.vela.app.ui.component.vela.VelaYtRow
import com.vela.app.ui.component.vela.navigateToLocalItem
import com.vela.app.ui.component.vela.navigateToPlaylistItem
import com.vela.app.ui.component.vela.navigateToYtItem
import com.vela.app.ui.component.vela.playLocalSongs
import com.vela.app.ui.component.vela.playYtItem
import com.vela.app.ui.component.vela.playYtSong
import com.vela.app.ui.component.vela.showLocalItemMenu
import com.vela.app.ui.component.vela.showYtItemMenu
import com.vela.app.viewmodels.CommunityPlaylistItem
import com.vela.app.viewmodels.DailyDiscoverItem
import com.vela.app.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Home — the Flow Deck.
 *
 * A vertical deck of currents, not the old app's tabbed rows and not Melo's dial:
 * a gradient hero for the top pick, then each source of music as its own
 * current — keep listening, quick picks, daily discover, forgotten favorites,
 * more-like-this, your playlists, community picks, and YouTube's home
 * sections. No Speed Dial anywhere (banned); the shuffle action lives in the
 * header and plays a random pick from what Home already loaded.
 *
 * Wired to [HomeViewModel] only. Every backend feature the old app's home had
 * (chips, radio, pinning via menus, account playlists, AI playlist via the
 * Library) stays reachable; the presentation is Vela's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelaHomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()

    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val dailyDiscover by viewModel.dailyDiscover.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val communityPlaylists by viewModel.communityPlaylists.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()

    // Now-playing state, hook-safe when the player connection is not ready yet:
    // the remembered flow is stable across recompositions, so collectAsState
    // is always called on the same flow object.
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()
    val nowPlayingId = mediaMetadata?.id
    val isPlaying by remember(playerConnection) {
        playerConnection?.isPlaying ?: MutableStateFlow(false)
    }.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VelaSpaces.s5,
                end = VelaSpaces.s5,
                top = VelaSpaces.s2,
                // Clears the Thread bar + the now-playing strip (player agent).
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(VelaSpaces.s8),
        ) {
            // -- Header: wordmark, shuffle, account -------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VelaWordmark()
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            scope.launch {
                                val item = viewModel.getRandomItem()
                                if (item != null) {
                                    playYtItem(playerConnection, item)
                                } else {
                                    snackbarHostState.showSnackbar("nothing to shuffle yet")
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle a pick",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { navController.navigate("account") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (accountImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(navController.context)
                                    .data(accountImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = accountName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = accountName.firstOrNull()?.uppercase().orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // -- Home chips (YouTube's own filters) -------------------------
            homePage?.chips?.takeIf { it.isNotEmpty() }?.let { chips ->
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2)) {
                        items(chips) { chip ->
                            VelaChip(
                                text = chip.title,
                                selected = selectedChip == chip,
                                onClick = { viewModel.toggleChip(chip) },
                            )
                        }
                    }
                }
            }

            // -- Hero: the top pick, or the welcome card when the library is empty
            item {
                val topPick = quickPicks?.firstOrNull()
                if (topPick != null) {
                    val picks = quickPicks.orEmpty()
                    VelaHeroCard(
                        title = topPick.title,
                        subtitle = topPick.artists.joinToString(", ") { it.name },
                        artworkUrl = topPick.thumbnailUrl,
                        kicker = "quick pick",
                        onPlay = { playLocalSongs(playerConnection, picks, 0) },
                        onRadio = {
                            playerConnection?.playQueue(
                                YouTubeQueue(WatchEndpoint(videoId = topPick.id)),
                            )
                        },
                        onCardClick = { playLocalSongs(playerConnection, picks, 0) },
                    )
                } else if (!isLoading) {
                    VelaWelcomeCard(onExplore = { navController.navigate("search_input") })
                }
            }

            // -- Keep listening ----------------------------------------------
            keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                item {
                    Column {
                        VelaSectionHeader(
                            title = "Keep listening",
                            kicker = "pick up where you left off",
                        )
                        Spacer(modifier = Modifier.height(VelaSpaces.s4))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                            items(items) { item ->
                                VelaLocalCard(
                                    item = item,
                                    onClick = {
                                        when (item) {
                                            is Song -> {
                                                val songs = items.filterIsInstance<Song>()
                                                playLocalSongs(
                                                    playerConnection,
                                                    songs,
                                                    songs.indexOf(item),
                                                )
                                            }
                                            else -> navigateToLocalItem(navController, item)
                                        }
                                    },
                                    onMoreClick = {
                                        showLocalItemMenu(menuState, item, navController, scope)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // -- Quick picks: a vertical flow, not a grid --------------------
            quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                item {
                    Column {
                        VelaSectionHeader(
                            title = "Quick picks",
                            kicker = "made for right now",
                        )
                        Spacer(modifier = Modifier.height(VelaSpaces.s2))
                        picks.take(10).forEachIndexed { index, song ->
                            VelaSongRow(
                                song = song,
                                nowPlayingId = nowPlayingId,
                                isPlaying = isPlaying,
                                onClick = {
                                    playLocalSongs(playerConnection, picks, index)
                                },
                                onMoreClick = {
                                    showLocalItemMenu(menuState, song, navController, scope)
                                },
                            )
                        }
                    }
                }
            }

            // -- Daily discover: because you liked X -------------------------
            dailyDiscover?.takeIf { it.isNotEmpty() }?.let { items ->
                item {
                    Column {
                        VelaSectionHeader(
                            title = "Daily discover",
                            kicker = "because you liked",
                        )
                        Spacer(modifier = Modifier.height(VelaSpaces.s4))
                        Column(verticalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                            items.forEach { discover ->
                                VelaDiscoverCard(
                                    item = discover,
                                    onPlay = { playYtItem(playerConnection, discover.recommendation) },
                                )
                            }
                        }
                    }
                }
            }

            // -- Forgotten favorites -----------------------------------------
            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { songs ->
                item {
                    Column {
                        VelaSectionHeader(
                            title = "Forgotten favorites",
                            kicker = "from your library",
                        )
                        Spacer(modifier = Modifier.height(VelaSpaces.s4))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                            items(songs) { song ->
                                VelaLocalCard(
                                    item = song,
                                    onClick = {
                                        playLocalSongs(playerConnection, songs, songs.indexOf(song))
                                    },
                                    onMoreClick = {
                                        showLocalItemMenu(menuState, song, navController, scope)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // -- More like this ----------------------------------------------
            similarRecommendations?.forEach { recommendation ->
                item {
                    Column {
                        VelaSectionHeader(
                            title = "More like ${recommendation.title.title}",
                            kicker = "similar",
                        )
                        Spacer(modifier = Modifier.height(VelaSpaces.s4))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                            items(recommendation.items) { ytItem ->
                                VelaYtCard(
                                    item = ytItem,
                                    onClick = {
                                        navigateToYtItem(navController, ytItem, playerConnection)
                                    },
                                    onMoreClick = {
                                        showYtItemMenu(menuState, ytItem, navController, scope)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // -- Your playlists (account) ------------------------------------
            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                item {
                    Column {
                        VelaSectionHeader(title = "Your playlists", kicker = "from your account")
                        Spacer(modifier = Modifier.height(VelaSpaces.s4))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                            items(playlists) { playlist ->
                                VelaYtCard(
                                    item = playlist,
                                    onClick = {
                                        navigateToPlaylistItem(navController, playlist)
                                    },
                                    onMoreClick = {
                                        showYtItemMenu(menuState, playlist, navController, scope)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // -- From the community ------------------------------------------
            communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                item {
                    Column {
                        VelaSectionHeader(
                            title = "From the community",
                            kicker = "playlists people love",
                        )
                        Spacer(modifier = Modifier.height(VelaSpaces.s4))
                        Column(verticalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                            playlists.forEach { community ->
                                VelaCommunityCard(
                                    item = community,
                                    navController = navController,
                                    onSongClick = { song -> playYtSong(playerConnection, song) },
                                )
                            }
                        }
                    }
                }
            }

            // -- YouTube home sections ---------------------------------------
            homePage?.sections?.forEach { section ->
                item {
                    Column {
                        VelaSectionHeader(
                            title = section.title,
                            kicker = section.label,
                            onSeeAll = section.endpoint?.let { endpoint ->
                                {
                                    navController.navigate(
                                        "youtube_browse/${endpoint.browseId}" +
                                            "?params=${endpoint.params}",
                                    )
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(VelaSpaces.s4))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                            items(section.items) { ytItem ->
                                VelaYtCard(
                                    item = ytItem,
                                    onClick = {
                                        navigateToYtItem(navController, ytItem, playerConnection)
                                    },
                                    onMoreClick = {
                                        showYtItemMenu(menuState, ytItem, navController, scope)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // -- Moods & genres doorway --------------------------------------
            item {
                Surface(
                    shape = RoundedCornerShape(VelaShapes.lg),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                    ),
                    onClick = { navController.navigate("mood_and_genres") },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(VelaSpaces.s5),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "moods & genres",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "find the sound for right now",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // -- Loading / empty / pagination --------------------------------
            if (isLoading && quickPicks == null && homePage == null) {
                item { VelaLoading() }
            }
            if (!isLoading && quickPicks.isNullOrEmpty() && homePage == null) {
                item {
                    VelaEmptyState(
                        icon = Icons.Filled.PlayArrow,
                        title = "Nothing here yet",
                        subtitle = "Play something you love and your flow will build itself.",
                    )
                }
            }
            homePage?.continuation?.let { continuation ->
                item {
                    LaunchedEffect(continuation) {
                        viewModel.loadMoreYouTubeItems(continuation)
                    }
                    VelaLoading()
                }
            }
        }
    }
}

/** Welcome card for an empty library — carries the picked tagline. */
@Composable
private fun VelaWelcomeCard(onExplore: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(VelaShapes.lg),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
        onClick = onExplore,
    ) {
        Column(modifier = Modifier.padding(VelaSpaces.s5)) {
            Text(
                text = "Your music. Your wave.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(VelaSpaces.s1))
            Text(
                text = "Search for a song, an artist, or a mood — your flow builds itself from what you play.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(VelaSpaces.s4))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "start exploring",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(VelaSpaces.s1))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** "Because you liked {seed}" — seed on the left, recommendation on the right. */
@Composable
private fun VelaDiscoverCard(
    item: DailyDiscoverItem,
    onPlay: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(VelaShapes.lg),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VelaSpaces.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VelaArtwork(
                url = item.seed.thumbnailUrl,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(VelaShapes.md)),
                contentDescription = item.seed.title,
            )
            Spacer(modifier = Modifier.width(VelaSpaces.s3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "because you liked ${item.seed.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.recommendation.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (val rec = item.recommendation) {
                        is SongItem -> rec.artists.joinToString(", ") { it.name }
                        is AlbumItem -> rec.artists?.joinToString(", ") { it.name }.orEmpty()
                        is ArtistItem -> "Artist"
                        is PlaylistItem -> rec.author?.name.orEmpty()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Community playlist with its first songs inline. */
@Composable
private fun VelaCommunityCard(
    item: CommunityPlaylistItem,
    navController: NavController,
    onSongClick: (SongItem) -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()
    val nowPlayingId = mediaMetadata?.id
    val isPlaying by remember(playerConnection) {
        playerConnection?.isPlaying ?: MutableStateFlow(false)
    }.collectAsState()

    Surface(
        shape = RoundedCornerShape(VelaShapes.lg),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(modifier = Modifier.padding(VelaSpaces.s4)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigateToPlaylistItem(navController, item.playlist) },
            ) {
                VelaArtwork(
                    url = item.playlist.thumbnail,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(VelaShapes.md)),
                    contentDescription = item.playlist.title,
                )
                Spacer(modifier = Modifier.width(VelaSpaces.s3))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            item.playlist.author?.name,
                            item.playlist.songCountText,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = {
                        item.playlist.playEndpoint?.let {
                            playerConnection?.playQueue(YouTubeQueue(it))
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play playlist",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            item.songs.take(3).forEach { song ->
                VelaYtRow(
                    item = song,
                    nowPlayingId = nowPlayingId,
                    isPlaying = isPlaying,
                    onClick = { onSongClick(song) },
                    onMoreClick = {
                        showYtItemMenu(menuState, song, navController, scope)
                    },
                )
            }
        }
    }
}
