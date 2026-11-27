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
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import com.vela.app.R
import com.vela.app.ai.AiLyricsTranslator
import com.vela.app.ai.AiServiceConfig
import com.vela.app.constants.AiApiKeyKey
import com.vela.app.constants.AiApiValidationStatus
import com.vela.app.constants.AiApiValidationStatusKey
import com.vela.app.constants.AiCustomEndpointKey
import com.vela.app.constants.AiCustomModelKey
import com.vela.app.constants.AiProvider
import com.vela.app.constants.AiProviderKey
import com.vela.app.constants.AiSelectedModelKey
import com.vela.app.constants.TranslatorTargetLangKey
import com.vela.app.db.MusicDatabase
import com.vela.app.db.entities.LyricsEntity
import com.vela.app.extensions.toEnum
import com.vela.app.lyrics.LyricsHelper
import com.vela.app.lyrics.LyricsResult
import com.vela.app.models.MediaMetadata
import com.vela.app.utils.NetworkConnectivityObserver
import com.vela.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
 
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    private var job: Job? = null
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)
    val isRefetching = MutableStateFlow(false)
    val isAiTranslating = MutableStateFlow(false)

    private val _aiTranslationEvents = MutableSharedFlow<String>()
    val aiTranslationEvents: SharedFlow<String> = _aiTranslationEvents.asSharedFlow()

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            networkConnectivity.networkStatus.collect { isConnected ->
                _isNetworkAvailable.value = isConnected
            }
        }
        
        // Set initial state using synchronous check
        _isNetworkAvailable.value = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true // Assume connected as fallback
        }
    }

    fun search(
        mediaId: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ) {
        isLoading.value = true
        results.value = emptyList()
        job?.cancel()
        job =
            viewModelScope.launch(Dispatchers.IO) {
                lyricsHelper.getAllLyrics(mediaId, title, artist, album, duration) { result ->
                    results.update {
                        it + result
                    }
                }
                isLoading.value = false
            }
    }

    fun cancelSearch() {
        job?.cancel()
        job = null
    }

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isRefetching.value = true
            try {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    replaceLyrics(
                        id = mediaMetadata.id,
                        lyrics = lyrics,
                        source = LyricsEntity.Source.REMOTE.value,
                    )
                }
            } catch (_: Exception) {
            } finally {
                isRefetching.value = false
            }
        }
    }

    fun updateLyrics(
        mediaMetadata: MediaMetadata,
        lyrics: String,
        source: LyricsEntity.Source = LyricsEntity.Source.USER_EDIT,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            database.query {
                replaceLyrics(
                    id = mediaMetadata.id,
                    lyrics = lyrics,
                    source = source.value,
                )
            }
        }
    }

    fun translateLyricsWithAi(
        mediaMetadata: MediaMetadata,
        lyrics: String,
    ) {
        if (isAiTranslating.value || lyrics.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            isAiTranslating.value = true
            try {
                val prefs = context.dataStore.data.first()
                val translatedLyrics = AiLyricsTranslator().translate(
                    config = AiServiceConfig(
                        provider = prefs[AiProviderKey].toEnum(AiProvider.NONE),
                        apiKey = prefs[AiApiKeyKey].orEmpty(),
                        customEndpoint = prefs[AiCustomEndpointKey].orEmpty(),
                        model = if (prefs[AiProviderKey].toEnum(AiProvider.NONE) == AiProvider.CUSTOM) {
                            prefs[AiCustomModelKey].orEmpty()
                        } else {
                            prefs[AiSelectedModelKey].orEmpty()
                        },
                    ),
                    lyrics = lyrics,
                    targetLanguage = prefs[TranslatorTargetLangKey].orEmpty().ifBlank { "ENGLISH" },
                )
                database.query {
                    replaceLyrics(
                        id = mediaMetadata.id,
                        lyrics = translatedLyrics,
                        source = LyricsEntity.Source.AI_TRANSLATION.value,
                    )
                }
                context.dataStore.edit { settings ->
                    settings[AiApiValidationStatusKey] = AiApiValidationStatus.SUCCESS.name
                }
                _aiTranslationEvents.emit(context.getString(R.string.translation_success))
            } catch (e: Exception) {
                context.dataStore.edit { settings ->
                    settings[AiApiValidationStatusKey] = AiApiValidationStatus.FAILED.name
                }
                _aiTranslationEvents.emit(
                    context.getString(R.string.translation_failed) + ": " + (e.localizedMessage ?: e.toString()),
                )
            } finally {
                isAiTranslating.value = false
            }
        }
    }
}
