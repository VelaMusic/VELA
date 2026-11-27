package com.vela.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.vela.app.LocalPlayerConnection
import com.vela.app.ui.component.LocalMenuState
import com.vela.app.ui.component.vela.VelaChip
import com.vela.app.ui.component.vela.VelaEmptyState
import com.vela.app.ui.component.vela.VelaLoading
import com.vela.app.ui.component.vela.VelaSearchField
import com.vela.app.ui.component.vela.VelaSectionHeader
import com.vela.app.ui.component.vela.VelaSpaces
import com.vela.app.ui.component.vela.VelaYtRow
import com.vela.app.ui.component.vela.navigateToYtItem
import com.vela.app.ui.component.vela.showYtItemMenu
import com.vela.app.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.URLEncoder

/**
 * Online search results — the far end of the Compass.
 *
 * Wired to [OnlineSearchViewModel]: the summary page (filter == null) or one
 * filtered page per [YouTube.SearchFilter], with pagination through
 * [OnlineSearchViewModel.loadMore]. The query field at the top re-runs the
 * search in place instead of stacking search screens.
 */
@Composable
fun VelaOnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()

    val filter by viewModel.filter.collectAsState()
    val summaryPage = viewModel.summaryPage
    val filteredPage = filter?.let { viewModel.viewStateMap[it.value] }

    var fieldText by rememberSaveable { mutableStateOf(viewModel.query) }

    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    }.collectAsState()
    val nowPlayingId = mediaMetadata?.id
    val isPlaying by remember(playerConnection) {
        playerConnection?.isPlaying ?: MutableStateFlow(false)
    }.collectAsState()

    fun runSearch(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == viewModel.query) return
        navController.navigate("search/${URLEncoder.encode(trimmed, "UTF-8")}") {
            popUpTo("search/{query}") { inclusive = true }
            launchSingleTop = true
        }
    }

    val filters = listOf(
        null to "all",
        YouTube.SearchFilter.FILTER_SONG to "songs",
        YouTube.SearchFilter.FILTER_VIDEO to "videos",
        YouTube.SearchFilter.FILTER_ALBUM to "albums",
        YouTube.SearchFilter.FILTER_ARTIST to "artists",
        YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to "playlists",
        YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST to "community",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = VelaSpaces.s2,
                    end = VelaSpaces.s5,
                    top = VelaSpaces.s2,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            VelaSearchField(
                value = fieldText,
                onValueChange = { fieldText = it },
                onSubmit = { runSearch(fieldText) },
                placeholder = viewModel.query,
            )
        }

        Spacer(modifier = Modifier.height(VelaSpaces.s3))
        LazyRow(
            contentPadding = PaddingValues(horizontal = VelaSpaces.s5),
            horizontalArrangement = Arrangement.spacedBy(VelaSpaces.s2),
        ) {
            items(filters) { (searchFilter, label) ->
                VelaChip(
                    text = label,
                    selected = filter == searchFilter,
                    onClick = { viewModel.filter.value = searchFilter },
                )
            }
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
            if (filter == null) {
                val page = summaryPage
                if (page == null) {
                    item { VelaLoading() }
                } else if (page.summaries.isEmpty()) {
                    item {
                        VelaEmptyState(
                            icon = Icons.Filled.Search,
                            title = "No results",
                            subtitle = "Nothing matched \"${viewModel.query}\" — try different words.",
                        )
                    }
                } else {
                    page.summaries.forEach { summary ->
                        item {
                            Column {
                                VelaSectionHeader(title = summary.title)
                                Spacer(modifier = Modifier.height(VelaSpaces.s2))
                                summary.items.forEach { ytItem ->
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
                }
            } else {
                val page = filteredPage
                if (page == null) {
                    item { VelaLoading() }
                } else if (page.items.isEmpty()) {
                    item {
                        VelaEmptyState(
                            icon = Icons.Filled.Search,
                            title = "No results",
                            subtitle = "Nothing matched \"${viewModel.query}\" here.",
                        )
                    }
                } else {
                    items(page.items, key = { it.id }) { ytItem ->
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
                    if (page.continuation != null) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMore() }
                            VelaLoading()
                        }
                    }
                }
            }
        }
    }
}
