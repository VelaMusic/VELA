package com.vela.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.vela.app.LocalPlayerConnection
import com.vela.app.db.entities.Album
import com.vela.app.db.entities.Artist
import com.vela.app.db.entities.Playlist
import com.vela.app.db.entities.Song
import com.vela.app.ui.component.LocalMenuState
import com.vela.app.ui.component.vela.VelaAccentDot
import com.vela.app.ui.component.vela.VelaChip
import com.vela.app.ui.component.vela.VelaEmptyState
import com.vela.app.ui.component.vela.VelaLoading
import com.vela.app.ui.component.vela.VelaLocalRow
import com.vela.app.ui.component.vela.VelaSearchField
import com.vela.app.ui.component.vela.VelaSectionHeader
import com.vela.app.ui.component.vela.VelaShapes
import com.vela.app.ui.component.vela.VelaSongRow
import com.vela.app.ui.component.vela.VelaSpaces
import com.vela.app.ui.component.vela.VelaYtCard
import com.vela.app.ui.component.vela.VelaYtRow
import com.vela.app.ui.component.vela.navigateToLocalItem
import com.vela.app.ui.component.vela.navigateToYtItem
import com.vela.app.ui.component.vela.playLocalSongs
import com.vela.app.ui.component.vela.showLocalItemMenu
import com.vela.app.ui.component.vela.showYtItemMenu
import com.vela.app.viewmodels.ExploreViewModel
import com.vela.app.viewmodels.LocalFilter
import com.vela.app.viewmodels.LocalSearchViewModel
import com.vela.app.viewmodels.MoodAndGenresViewModel
import com.vela.app.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.URLEncoder

/**
 * Search — the Compass.
 *
 * One field drives two engines at once: online suggestions
 * ([OnlineSearchSuggestionViewModel]) and the local library
 * ([LocalSearchViewModel]). Submitting goes to the online results screen
 * ("search/{query}"). With an empty query the screen is a compass rose of
 * discovery — recent searches, moods & genres, fresh releases, charts and
 * history — instead of a blank field.
 *
 * Nothing here rewrites search: the suggestion parsing (YouTube URLs resolve
 * straight to their artist/album/playlist), the local search grouping and the
 * online search itself all stay on the existing ViewModels.
 */
@Composable
fun VelaSearchScreen(
    navController: NavController,
    suggestionViewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
    localViewModel: LocalSearchViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    moodViewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    // One query drives both search engines.
    LaunchedEffect(query) {
        suggestionViewModel.query.value = query
        localViewModel.query.value = query
    }

    val suggestionState by suggestionViewModel.viewState.collectAsState()
    val localResult by localViewModel.result.collectAsState()
    val localFilter by localViewModel.filter.collectAsState()
    val explorePage by exploreViewModel.explorePage.collectAsState()
    val moodAndGenres by moodViewModel.moodAndGenres.collectAsState()

    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()
    val nowPlayingId = mediaMetadata?.id
    val isPlaying by remember(playerConnection) {
        playerConnection?.isPlaying ?: MutableStateFlow(false)
    }.collectAsState()

    fun submitSearch(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        navController.navigate("search/${URLEncoder.encode(trimmed, "UTF-8")}")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = VelaSpaces.s5,
                    end = VelaSpaces.s5,
                    top = VelaSpaces.s2,
                ),
        ) {
            Text(
                text = "compass",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(VelaSpaces.s3))
            VelaSearchField(
                value = query,
                onValueChange = { query = it },
                onSubmit = { submitSearch(query) },
                placeholder = "Songs, artists, albums…",
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VelaSpaces.s5,
                end = VelaSpaces.s5,
                top = VelaSpaces.s6,
                // Clears the Thread bar + the now-playing strip (player agent).
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(VelaSpaces.s8),
        ) {
            if (query.isBlank()) {
                // -- Recent searches ----------------------------------------
                suggestionState.history.takeIf { it.isNotEmpty() }?.let { history ->
                    item {
                        Column {
                            VelaSectionHeader(title = "Recent", kicker = "your searches")
                            Spacer(modifier = Modifier.height(VelaSpaces.s2))
                            history.take(5).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { submitSearch(entry.query) }
                                        .padding(vertical = VelaSpaces.s3),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(VelaSpaces.s3))
                                    Text(
                                        text = entry.query,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                // -- Moods & genres: the compass rose -----------------------
                moodAndGenres?.forEach { section ->
                    item {
                        Column {
                            VelaSectionHeader(
                                title = section.title,
                                kicker = "moods & genres",
                                onSeeAll = { navController.navigate("mood_and_genres") },
                            )
                            Spacer(modifier = Modifier.height(VelaSpaces.s4))
                            Column(verticalArrangement = Arrangement.spacedBy(VelaSpaces.s2)) {
                                section.items.chunked(2).forEach { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        rowItems.forEach { mood ->
                                            Surface(
                                                shape = RoundedCornerShape(VelaShapes.md),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                ),
                                                onClick = {
                                                    navController.navigate(
                                                        "youtube_browse/${mood.endpoint.browseId}" +
                                                            "?params=${mood.endpoint.params}",
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(64.dp)
                                                        .padding(horizontal = VelaSpaces.s4),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    VelaAccentDot()
                                                    Spacer(modifier = Modifier.width(VelaSpaces.s2))
                                                    Text(
                                                        text = mood.title,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                        repeat(2 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (moodAndGenres == null) {
                    item { VelaLoading() }
                }

                // -- Fresh releases ------------------------------------------
                explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { albums ->
                    item {
                        Column {
                            VelaSectionHeader(
                                title = "Fresh releases",
                                kicker = "new this week",
                                onSeeAll = { navController.navigate("new_release") },
                            )
                            Spacer(modifier = Modifier.height(VelaSpaces.s4))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s3)) {
                                items(albums) { album ->
                                    VelaYtCard(
                                        item = album,
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onMoreClick = {
                                            showYtItemMenu(menuState, album, navController, scope)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // -- Doorways: charts, history -------------------------------
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(VelaSpaces.s2)) {
                        VelaDoorwayRow(
                            title = "Charts",
                            subtitle = "what everyone is playing",
                            onClick = { navController.navigate("charts_screen") },
                        )
                        VelaDoorwayRow(
                            title = "History",
                            subtitle = "everything you played",
                            onClick = { navController.navigate("history") },
                        )
                    }
                }
            } else {
                // -- Live suggestions ----------------------------------------
                suggestionState.items.takeIf { it.isNotEmpty() }?.let { parsed ->
                    item {
                        Column {
                            VelaSectionHeader(title = "Matches", kicker = "from your link")
                            Spacer(modifier = Modifier.height(VelaSpaces.s2))
                            parsed.forEach { ytItem ->
                                VelaYtRow(
                                    item = ytItem,
                                    nowPlayingId = nowPlayingId,
                                    isPlaying = isPlaying,
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
                suggestionState.suggestions.takeIf { it.isNotEmpty() }?.let { suggestions ->
                    item {
                        Column {
                            suggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { submitSearch(suggestion) }
                                        .padding(vertical = VelaSpaces.s3),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(VelaSpaces.s3))
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                // -- Local library results -----------------------------------
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2)) {
                        items(LocalFilter.entries) { filter ->
                            VelaChip(
                                text = when (filter) {
                                    LocalFilter.ALL -> "all"
                                    LocalFilter.SONG -> "songs"
                                    LocalFilter.ALBUM -> "albums"
                                    LocalFilter.ARTIST -> "artists"
                                    LocalFilter.PLAYLIST -> "playlists"
                                },
                                selected = localFilter == filter,
                                onClick = { localViewModel.filter.value = filter },
                            )
                        }
                    }
                }
                if (localResult.map.isEmpty()) {
                    item {
                        VelaEmptyState(
                            icon = Icons.Filled.Search,
                            title = "Nothing in your library",
                            subtitle = "Try the full search below — it looks everywhere.",
                        )
                    }
                }
                localResult.map[LocalFilter.SONG]?.takeIf { it.isNotEmpty() }?.let { songs ->
                    val songList = songs.filterIsInstance<Song>()
                    item {
                        Column {
                            VelaSectionHeader(title = "Songs", kicker = "in your library")
                            Spacer(modifier = Modifier.height(VelaSpaces.s2))
                            songList.take(5).forEachIndexed { index, song ->
                                VelaSongRow(
                                    song = song,
                                    nowPlayingId = nowPlayingId,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        playLocalSongs(playerConnection, songList, index)
                                    },
                                    onMoreClick = {
                                        showLocalItemMenu(menuState, song, navController, scope)
                                    },
                                )
                            }
                        }
                    }
                }
                localResult.map[LocalFilter.ALBUM]?.takeIf { it.isNotEmpty() }?.let { albums ->
                    item {
                        Column {
                            VelaSectionHeader(title = "Albums", kicker = "in your library")
                            Spacer(modifier = Modifier.height(VelaSpaces.s2))
                            albums.filterIsInstance<Album>().take(5).forEach { album ->
                                VelaLocalRow(
                                    item = album,
                                    onClick = { navigateToLocalItem(navController, album) },
                                    onMoreClick = {
                                        showLocalItemMenu(menuState, album, navController, scope)
                                    },
                                )
                            }
                        }
                    }
                }
                localResult.map[LocalFilter.ARTIST]?.takeIf { it.isNotEmpty() }?.let { artists ->
                    item {
                        Column {
                            VelaSectionHeader(title = "Artists", kicker = "in your library")
                            Spacer(modifier = Modifier.height(VelaSpaces.s2))
                            artists.filterIsInstance<Artist>().take(5).forEach { artist ->
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
                }
                localResult.map[LocalFilter.PLAYLIST]?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item {
                        Column {
                            VelaSectionHeader(title = "Playlists", kicker = "in your library")
                            Spacer(modifier = Modifier.height(VelaSpaces.s2))
                            playlists.filterIsInstance<Playlist>().take(5).forEach { playlist ->
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
                }

                // -- Hand-off to the full online search -----------------------
                item {
                    Surface(
                        shape = RoundedCornerShape(VelaShapes.lg),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                        onClick = { submitSearch(query) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(VelaSpaces.s5),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Search everywhere",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = "YouTube Music results for \"$query\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
            }
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
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
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
