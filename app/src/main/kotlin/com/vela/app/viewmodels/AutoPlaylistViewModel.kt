

package com.vela.app.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vela.app.constants.ExportedSongIdsKey
import com.vela.app.constants.DownloadedSongSortDescendingKey
import com.vela.app.constants.DownloadedSongSortTypeKey
import com.vela.app.constants.HideExplicitKey
import com.vela.app.constants.HideVideoSongsKey
import com.vela.app.constants.SongSortDescendingKey
import com.vela.app.constants.SongSortType
import com.vela.app.constants.SongSortTypeKey
import com.vela.app.db.MusicDatabase
import com.vela.app.extensions.filterExplicit
import com.vela.app.extensions.filterVideoSongs
import com.vela.app.extensions.toEnum
import com.vela.app.utils.SyncUtils
import com.vela.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!
    private val sortTypeKey = if (playlist == "downloaded") DownloadedSongSortTypeKey else SongSortTypeKey
    private val sortDescendingKey =
        if (playlist == "downloaded") DownloadedSongSortDescendingKey else SongSortDescendingKey

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs =
        context.dataStore.data
            .map {
                Triple(
                    Triple(
                        (try { it[sortTypeKey] } catch(e: Exception) { null }).toEnum(SongSortType.CREATE_DATE) to ((try { it[sortDescendingKey] } catch(e: Exception) { null }) ?: true),
                        (try { it[HideExplicitKey] } catch(e: Exception) { null }) ?: false,
                        (try { it[HideVideoSongsKey] } catch(e: Exception) { null }) ?: false
                    ),
                    (try { it[ExportedSongIdsKey] } catch(e: Exception) { null }) ?: "",
                    Unit
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { (triple, exportedSongIds, _) ->
                val (sortDesc, hideExplicit, hideVideoSongs) = triple
                val (sortType, descending) = sortDesc
                when (playlist) {
                    "liked" -> database.likedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                    "downloaded" -> database.downloadedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                    "uploaded" -> database.uploadedSongs(sortType, descending)
                        .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }

                    "exported" -> {
                        val ids = exportedSongIds.split(",").filter { it.isNotBlank() }
                        database.getSongsByIdsFlow(ids)
                            .map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                    }

                    else -> kotlinx.coroutines.flow.flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            when (playlist) {
                "liked" -> syncUtils.syncLikedSongsSuspend()
                "uploaded" -> syncUtils.syncUploadedSongsSuspend()
            }
            _isRefreshing.value = false
        }
    }
}
