package com.vela.app.musicflow

import androidx.datastore.preferences.core.Preferences
import com.vela.app.constants.CanvasThumbnailAnimationKey
import com.vela.app.constants.DataSaverEnabledKey
import com.vela.app.constants.MusicFlowAnimatedBackgroundKey
import com.vela.app.constants.MusicFlowAutoNextKey
import com.vela.app.constants.MusicFlowAutoplayKey
import com.vela.app.constants.MusicFlowCellularPrefetchKey
import com.vela.app.constants.MusicFlowEnabledKey
import com.vela.app.constants.MusicFlowHideRecentKey
import com.vela.app.constants.MusicFlowLyricsKey

data class MusicFlowPreferences(
  val enabled: Boolean = true,
  val autoplay: Boolean = true,
  val autoNext: Boolean = true,
  val animatedBackground: Boolean = false,
  val lyrics: Boolean = true,
  val cellularPrefetch: Boolean = true,
  val hideRecent: Boolean = true,
  val dataSaver: Boolean = false,
) {
  companion object {
    fun from(preferences: Preferences) =
      MusicFlowPreferences(
        enabled = preferences[MusicFlowEnabledKey] ?: true,
        autoplay = preferences[MusicFlowAutoplayKey] ?: true,
        autoNext = preferences[MusicFlowAutoNextKey] ?: true,
        animatedBackground =
          preferences[MusicFlowAnimatedBackgroundKey]
            ?: preferences[CanvasThumbnailAnimationKey]
            ?: false,
        lyrics = preferences[MusicFlowLyricsKey] ?: true,
        cellularPrefetch = preferences[MusicFlowCellularPrefetchKey] ?: true,
        hideRecent = preferences[MusicFlowHideRecentKey] ?: true,
        dataSaver = preferences[DataSaverEnabledKey] ?: false,
      )
  }
}
