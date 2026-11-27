/*
 * NomaTune (2026)
 * © Shahdullah — github.com/shahdullah
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Based on ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.vela.app.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vela.app.innertube.YouTube
import com.vela.app.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.vela.app.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.vela.app.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.vela.app.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.vela.app.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.vela.app.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.vela.app.innertube.models.filterExplicit
import com.vela.app.innertube.models.filterVideo
import com.vela.app.innertube.pages.SearchSummaryPage
import com.vela.app.constants.HideExplicitKey
import com.vela.app.constants.HideVideoKey
import com.vela.app.models.ItemsPage
import com.vela.app.ui.screens.search.OnlineSearchResultArgument
import com.vela.app.ui.screens.search.decodeOnlineSearchQuery
import com.vela.app.utils.dataStore
import com.vela.app.utils.get
import com.vela.app.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query =
        decodeOnlineSearchQuery(
            savedStateHandle.get<String>(OnlineSearchResultArgument).orEmpty()
        )
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    private val allModeFilters =
        listOf(
            FILTER_SONG,
            FILTER_VIDEO,
            FILTER_ALBUM,
            FILTER_ARTIST,
            FILTER_COMMUNITY_PLAYLIST,
            FILTER_FEATURED_PLAYLIST,
        )
    private var isSummaryLoading = false
    private val loadingFilters = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            filter.collect { selectedFilter ->
                if (selectedFilter == null) {
                    viewModelScope.launch {
                        loadSummaryIfNeeded()
                    }
                    allModeFilters.forEach { allModeFilter ->
                        viewModelScope.launch {
                            loadFilterIfNeeded(allModeFilter)
                        }
                    }
                } else {
                    loadFilterIfNeeded(selectedFilter)
                }
            }
        }
    }

    private suspend fun loadSummaryIfNeeded() {
        if (summaryPage != null || isSummaryLoading) return

        isSummaryLoading = true
        try {
            YouTube
                .searchSummary(query)
                .onSuccess {
                    summaryPage =
                        it.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                            .filterVideo(context.dataStore.get(HideVideoKey, false))
                }.onFailure {
                    reportException(it)
                }
        } finally {
            isSummaryLoading = false
        }
    }

    private suspend fun loadFilterIfNeeded(filter: YouTube.SearchFilter) {
        val filterKey = filter.value
        if (viewStateMap.containsKey(filterKey) || !loadingFilters.add(filterKey)) return

        try {
            YouTube
                .search(query, filter)
                .onSuccess { result ->
                    viewStateMap[filterKey] =
                        ItemsPage(
                            result.items
                                .distinctBy { it.id }
                                .filterExplicit(
                                    context.dataStore.get(
                                        HideExplicitKey,
                                        false
                                    )
                                ).filterVideo(context.dataStore.get(HideVideoKey, false)),
                            result.continuation,
                        )
                }.onFailure {
                    reportException(it)
                }
        } finally {
            loadingFilters.remove(filterKey)
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}
