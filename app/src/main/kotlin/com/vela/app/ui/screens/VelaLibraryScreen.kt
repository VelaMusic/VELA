package com.vela.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.vela.app.LocalDatabase
import com.vela.app.LocalPlayerConnection
import com.vela.app.constants.SongFilter
import com.vela.app.constants.SongFilterKey
import com.vela.app.constants.SongSortType
import com.vela.app.constants.SongSortTypeKey
import com.vela.app.ui.component.CreatePlaylistDialog
import com.vela.app.ui.component.LocalMenuState
import com.vela.app.ui.component.TextFieldDialog
import com.vela.app.ui.component.vela.VelaArtistCircle
import com.vela.app.ui.component.vela.VelaChip
import com.vela.app.ui.component.vela.VelaEmptyState
import com.vela.app.ui.component.vela.VelaLocalCard
import com.vela.app.ui.component.vela.VelaLocalRow
import com.vela.app.ui.component.vela.VelaSectionHeader
import com.vela.app.ui.component.vela.VelaShapes
import com.vela.app.ui.component.vela.VelaSongRow
import com.vela.app.ui.component.vela.VelaSpaces
import com.vela.app.ui.component.vela.VelaWordmark
import com.vela.app.ui.component.vela.navigateToLocalItem
import com.vela.app.ui.component.vela.playLocalSongs
import com.vela.app.ui.component.vela.showLocalItemMenu
import com.vela.app.ui.component.vela.velaHeroGradient
import com.vela.app.utils.rememberEnumPreference
import com.vela.app.viewmodels.LibraryAlbumsViewModel
import com.vela.app.viewmodels.LibraryArtistsViewModel
import com.vela.app.viewmodels.LibraryMixViewModel
import com.vela.app.viewmodels.LibraryPlaylistsViewModel
import com.vela.app.viewmodels.LibrarySongsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/** Which part of the Shelf is showing. ALL is the mixed shelf; the rest are full lists. */
enum class VelaShelfFilter {
    ALL, PLAYLISTS, SONGS, ALBUMS, ARTISTS, VAULT
}

/**
 * Library — the Shelf, with the Offline Vault on top.
 *
 * One scrollable shelf instead of the old app's tabbed library. The Vault is a
 * first-class section, not a filter buried in a menu: every downloaded song
 * lives there, playable offline, with play-all and shuffle. Everything else
 * is the same backend through the existing Library* ViewModels — playlists,
 * songs (with the same sort/filter preferences the old screen used),
 * albums, artists, local device songs, create/import playlist flows.
 * Nothing is cut; only the layout is Vela's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelaLibraryScreen(
    navController: NavController,
    mixViewModel: LibraryMixViewModel = hiltViewModel(),
    songsViewModel: LibrarySongsViewModel = hiltViewModel(),
    artistsViewModel: LibraryArtistsViewModel = hiltViewModel(),
    albumsViewModel: LibraryAlbumsViewModel = hiltViewModel(),
    playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val database = LocalDatabase.current

    var shelfFilter by rememberSaveable { mutableStateOf(VelaShelfFilter.ALL) }

    val mixArtists by mixViewModel.artists.collectAsState()
    val mixAlbums by mixViewModel.albums.collectAsState()
    val mixPlaylists by mixViewModel.playlists.collectAsState()
    val allSongs by songsViewModel.allSongs.collectAsState()
    val allArtists by artistsViewModel.allArtists.collectAsState()
    val allAlbums by albumsViewModel.allAlbums.collectAsState()
    val allPlaylists by playlistsViewModel.allPlaylists.collectAsState()
    val downloadedSongs by database
        .downloadedSongs(SongSortType.CREATE_DATE, true)
        .collectAsState(initial = emptyList())

    // The songs list answers to these preferences (same keys the old screen used).
    var songSort by rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    var songFilter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()
    val nowPlayingId = mediaMetadata?.id
    val isPlaying by remember(playerConnection) {
        playerConnection?.isPlaying ?: MutableStateFlow(false)
    }.collectAsState()

    var showImportMenu by rememberSaveable { mutableStateOf(false) }
    var showYoutubeImportDialog by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistOptionsDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
            // -- Header ------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VelaWordmark(fontSize = 27.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { mixViewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Sync library",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "your shelf",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // -- Shelf filter chips ------------------------------------------
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2)) {
                    items(VelaShelfFilter.entries) { filter ->
                        VelaChip(
                            text = when (filter) {
                                VelaShelfFilter.ALL -> "all"
                                VelaShelfFilter.PLAYLISTS -> "playlists"
                                VelaShelfFilter.SONGS -> "songs"
                                VelaShelfFilter.ALBUMS -> "albums"
                                VelaShelfFilter.ARTISTS -> "artists"
                                VelaShelfFilter.VAULT -> "vault"
                            },
                            selected = shelfFilter == filter,
                            onClick = { shelfFilter = filter },
                        )
                    }
                }
            }

            // -- Offline Vault ------------------------------------------------
            if (shelfFilter == VelaShelfFilter.ALL || shelfFilter == VelaShelfFilter.VAULT) {
                item {
                    VelaVaultCard(
                        downloadedCount = downloadedSongs.size,
                        onPlayAll = {
                            playLocalSongs(playerConnection, downloadedSongs, 0)
                        },
                        onShuffle = {
                            playLocalSongs(
                                playerConnection,
                                downloadedSongs.shuffled(),
                                0,
                            )
                        },
                        onOpen = { navController.navigate("auto_playlist/downloaded") },
                    )
                }
            }

            when (shelfFilter) {
                VelaShelfFilter.ALL -> {
                    // -- Songs preview ---------------------------------------
                    if (allSongs.isNotEmpty()) {
                        item {
                            Column {
                                VelaSectionHeader(
                                    title = "Songs",
                                    kicker = "from your library",
                                    onSeeAll = { shelfFilter = VelaShelfFilter.SONGS },
                                )
                                Spacer(modifier = Modifier.height(VelaSpaces.s2))
                                allSongs.take(8).forEachIndexed { index, song ->
                                    VelaSongRow(
                                        song = song,
                                        nowPlayingId = nowPlayingId,
                                        isPlaying = isPlaying,
                                        onClick = {
                                            playLocalSongs(playerConnection, allSongs, index)
                                        },
                                        onMoreClick = {
                                            showLocalItemMenu(menuState, song, navController, scope)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    // -- Artists strip ---------------------------------------
                    if (mixArtists.isNotEmpty()) {
                        item {
                            Column {
                                VelaSectionHeader(
                                    title = "Artists",
                                    kicker = "you follow",
                                    onSeeAll = { shelfFilter = VelaShelfFilter.ARTISTS },
                                )
                                Spacer(modifier = Modifier.height(VelaSpaces.s4))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3),
                                ) {
                                    items(mixArtists) { artist ->
                                        VelaArtistCircle(
                                            artist = artist,
                                            onClick = {
                                                navigateToLocalItem(navController, artist)
                                            },
                                            onMoreClick = {
                                                showLocalItemMenu(
                                                    menuState, artist, navController, scope,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // -- Albums strip ----------------------------------------
                    if (mixAlbums.isNotEmpty()) {
                        item {
                            Column {
                                VelaSectionHeader(
                                    title = "Albums",
                                    kicker = "you saved",
                                    onSeeAll = { shelfFilter = VelaShelfFilter.ALBUMS },
                                )
                                Spacer(modifier = Modifier.height(VelaSpaces.s4))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3),
                                ) {
                                    items(mixAlbums) { album ->
                                        VelaLocalCard(
                                            item = album,
                                            onClick = {
                                                navigateToLocalItem(navController, album)
                                            },
                                            onMoreClick = {
                                                showLocalItemMenu(
                                                    menuState, album, navController, scope,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // -- Playlists strip -------------------------------------
                    if (mixPlaylists.isNotEmpty()) {
                        item {
                            Column {
                                VelaSectionHeader(
                                    title = "Playlists",
                                    kicker = "yours",
                                    onSeeAll = { shelfFilter = VelaShelfFilter.PLAYLISTS },
                                )
                                Spacer(modifier = Modifier.height(VelaSpaces.s4))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3),
                                ) {
                                    items(mixPlaylists) { playlist ->
                                        VelaLocalCard(
                                            item = playlist,
                                            onClick = {
                                                navigateToLocalItem(navController, playlist)
                                            },
                                            onMoreClick = {
                                                showLocalItemMenu(
                                                    menuState, playlist, navController, scope,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // -- On this device --------------------------------------
                    item {
                        VelaDoorwayRow(
                            title = "On this device",
                            subtitle = "songs stored on your phone",
                            onClick = { navController.navigate("local_songs") },
                        )
                    }
                    if (allSongs.isEmpty() && mixArtists.isEmpty() &&
                        mixAlbums.isEmpty() && mixPlaylists.isEmpty()
                    ) {
                        item {
                            VelaEmptyState(
                                icon = Icons.Filled.MusicNote,
                                title = "Your shelf is empty",
                                subtitle = "Like a song or save an album and it will live here.",
                            )
                        }
                    }
                }

                VelaShelfFilter.PLAYLISTS -> {
                    if (allPlaylists.isEmpty()) {
                        item {
                            VelaEmptyState(
                                icon = Icons.Filled.MusicNote,
                                title = "No playlists yet",
                                subtitle = "Create one below, or import from Spotify or YouTube Music.",
                            )
                        }
                    } else {
                        items(allPlaylists) { playlist ->
                            VelaLocalRow(
                                item = playlist,
                                onClick = { navigateToLocalItem(navController, playlist) },
                                onMoreClick = {
                                    showLocalItemMenu(menuState, playlist, navController, scope)
                                },
                            )
                        }
                    }
                }

                VelaShelfFilter.SONGS -> {
                    item {
                        Column {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2),
                            ) {
                                items(
                                    listOf(
                                        SongSortType.CREATE_DATE to "newest",
                                        SongSortType.NAME to "name",
                                        SongSortType.ARTIST to "artist",
                                        SongSortType.PLAY_TIME to "most played",
                                    ),
                                ) { (sort, label) ->
                                    VelaChip(
                                        text = label,
                                        selected = songSort == sort,
                                        onClick = { songSort = sort },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(VelaSpaces.s2))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2),
                            ) {
                                items(
                                    listOf(
                                        SongFilter.LIKED to "liked",
                                        SongFilter.LIBRARY to "library",
                                        SongFilter.DOWNLOADED to "downloaded",
                                        SongFilter.UPLOADED to "uploaded",
                                    ),
                                ) { (filter, label) ->
                                    VelaChip(
                                        text = label,
                                        selected = songFilter == filter,
                                        onClick = { songFilter = filter },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(VelaSpaces.s4))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2),
                            ) {
                                VelaGradientButton(
                                    text = "play all",
                                    onClick = {
                                        playLocalSongs(playerConnection, allSongs, 0)
                                    },
                                )
                                VelaOutlineButton(
                                    text = "shuffle",
                                    onClick = {
                                        playLocalSongs(
                                            playerConnection,
                                            allSongs.shuffled(),
                                            0,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    if (allSongs.isEmpty()) {
                        item {
                            VelaEmptyState(
                                icon = Icons.Filled.MusicNote,
                                title = "No songs here",
                                subtitle = "Try another filter, or like a song to start the list.",
                            )
                        }
                    } else {
                        itemsIndexed(allSongs) { index, song ->
                            VelaSongRow(
                                song = song,
                                nowPlayingId = nowPlayingId,
                                isPlaying = isPlaying,
                                onClick = {
                                    playLocalSongs(playerConnection, allSongs, index)
                                },
                                onMoreClick = {
                                    showLocalItemMenu(menuState, song, navController, scope)
                                },
                            )
                        }
                    }
                }

                VelaShelfFilter.ALBUMS -> {
                    if (allAlbums.isEmpty()) {
                        item {
                            VelaEmptyState(
                                icon = Icons.Filled.MusicNote,
                                title = "No albums yet",
                                subtitle = "Save an album and it will live here.",
                            )
                        }
                    } else {
                        items(allAlbums.chunked(2)) { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                row.forEach { album ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        VelaLocalCard(
                                            item = album,
                                            fillMaxWidth = true,
                                            onClick = {
                                                navigateToLocalItem(navController, album)
                                            },
                                            onMoreClick = {
                                                showLocalItemMenu(
                                                    menuState, album, navController, scope,
                                                )
                                            },
                                        )
                                    }
                                }
                                repeat(2 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                VelaShelfFilter.ARTISTS -> {
                    if (allArtists.isEmpty()) {
                        item {
                            VelaEmptyState(
                                icon = Icons.Filled.MusicNote,
                                title = "No artists yet",
                                subtitle = "Follow an artist and they will live here.",
                            )
                        }
                    } else {
                        items(allArtists) { artist ->
                            VelaLocalRow(
                                item = artist,
                                onClick = { navigateToLocalItem(navController, artist) },
                                onMoreClick = {
                                    showLocalItemMenu(menuState, artist, navController, scope)
                                },
                            )
                        }
                    }
                }

                VelaShelfFilter.VAULT -> {
                    if (downloadedSongs.isEmpty()) {
                        item {
                            VelaEmptyState(
                                icon = Icons.Filled.CloudOff,
                                title = "Vault is empty",
                                subtitle = "Long-press any song and download it — it will wait here, ready offline.",
                            )
                        }
                    } else {
                        itemsIndexed(downloadedSongs) { index, song ->
                            VelaSongRow(
                                song = song,
                                nowPlayingId = nowPlayingId,
                                isPlaying = isPlaying,
                                onClick = {
                                    playLocalSongs(playerConnection, downloadedSongs, index)
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

        // -- Create / import actions (playlists view) ------------------------
        if (shelfFilter == VelaShelfFilter.PLAYLISTS || shelfFilter == VelaShelfFilter.ALL) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(end = VelaSpaces.s4, bottom = 96.dp),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalArrangement = Arrangement.spacedBy(VelaSpaces.s4),
                    horizontalAlignment = Alignment.End,
                ) {
                    ExtendedFloatingActionButton(
                        text = { Text("Create playlist") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                            )
                        },
                        onClick = { showCreatePlaylistOptionsDialog = true },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                    ExtendedFloatingActionButton(
                        text = { Text("Import playlist") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                            )
                        },
                        onClick = { showImportMenu = true },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    // -- Import sheet ---------------------------------------------------------
    if (showImportMenu) {
        ModalBottomSheet(
            onDismissRequest = { showImportMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = VelaSpaces.s5, end = VelaSpaces.s5, bottom = VelaSpaces.s8),
                verticalArrangement = Arrangement.spacedBy(VelaSpaces.s2),
            ) {
                Text(
                    text = "import playlist",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                VelaDoorwayRow(
                    title = "Import from Spotify",
                    subtitle = "authenticate, pick playlists, import",
                    onClick = {
                        showImportMenu = false
                        navController.navigate(
                            "settings/backup_restore?highlightKey=spotify_import",
                        )
                    },
                )
                VelaDoorwayRow(
                    title = "Import from YouTube Music",
                    subtitle = "paste a public playlist link",
                    onClick = {
                        showImportMenu = false
                        showYoutubeImportDialog = true
                    },
                )
            }
        }
    }

    // -- YouTube Music import dialog ------------------------------------------
    if (showYoutubeImportDialog) {
        var url by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(""))
        }
        TextFieldDialog(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = null,
                )
            },
            title = {
                Column {
                    Text(text = "Import playlist from YouTube Music")
                    Text(
                        text = "Paste the public YouTube Music playlist link below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = VelaSpaces.s1),
                    )
                }
            },
            initialTextFieldValue = url,
            autoFocus = true,
            onDismiss = { showYoutubeImportDialog = false },
            onDone = { finalUrl ->
                val listId = Regex("[?&]list=([a-zA-Z0-9_-]+)")
                    .find(finalUrl)?.groupValues?.get(1)
                if (listId != null) {
                    navController.navigate("online_playlist/$listId")
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Invalid playlist URL",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
                showYoutubeImportDialog = false
            },
        )
    }

    // -- Create playlist dialogs ----------------------------------------------
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = null,
            allowSyncing = true,
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            },
        )
    }
    if (showCreatePlaylistOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistOptionsDialog = false },
            title = { Text("Create playlist") },
            text = { Text("How would you like to create your playlist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreatePlaylistOptionsDialog = false
                        navController.navigate("generate")
                    },
                ) { Text("Create with AI") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreatePlaylistOptionsDialog = false
                        showCreatePlaylistDialog = true
                    },
                ) { Text("Normally") }
            },
        )
    }
    // "Create with AI" navigates to the Generate screen (the AI playlist
    // generator); there is no separate AI dialog in the backend.
}

/**
 * The Offline Vault card: every downloaded song, ready offline. The vault
 * fills itself — downloads land here automatically (the download actions
 * live in the song/album menus, unchanged).
 */
@Composable
private fun VelaVaultCard(
    downloadedCount: Int,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(VelaShapes.lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VelaSpaces.s5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(velaHeroGradient(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.width(VelaSpaces.s4))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "offline vault",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (downloadedCount == 0) {
                        "nothing downloaded yet"
                    } else {
                        "$downloadedCount songs · ready offline"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(VelaSpaces.s3))
                Row(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2)) {
                    VelaGradientButton(text = "play all", onClick = onPlayAll)
                    VelaOutlineButton(text = "shuffle", onClick = onShuffle)
                }
            }
        }
    }
}

/** Primary button: pill, gradient fill, white text (tokens.md §6). */
@Composable
private fun VelaGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .background(velaHeroGradient())
                .padding(horizontal = VelaSpaces.s4, vertical = VelaSpaces.s2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

/** Secondary button: pill, transparent fill, 1px outline border. */
@Composable
private fun VelaOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = VelaSpaces.s4,
                vertical = VelaSpaces.s2,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun VelaDoorwayRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(VelaShapes.lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VelaSpaces.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
