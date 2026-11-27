package com.vela.app.playback

import com.vela.app.db.entities.SongEntity

interface ISyncUtils {
    fun likeSong(song: SongEntity)
}
