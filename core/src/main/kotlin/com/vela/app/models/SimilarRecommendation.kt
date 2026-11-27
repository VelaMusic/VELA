

package com.vela.app.models

import com.music.innertube.models.YTItem
import com.vela.app.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
