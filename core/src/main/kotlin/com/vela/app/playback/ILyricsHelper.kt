package com.vela.app.playback

import com.vela.app.models.MediaMetadata

data class LyricsWithProvider(
    val lyrics: String?,
    val providerName: String
)

interface ILyricsHelper {
    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider
}
