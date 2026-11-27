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

package com.vela.app.repository

import com.vela.app.models.NewsItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor() {

    @Volatile private var metadataCache: List<NewsItem>? = null

    // No Vela news source exists: there is no Vela equivalent of the original
    // developer's news repository, so the news feed is empty by design.
    // The hardcoded fallback posts from the original developer and the
    // original news URLs were removed rather than repointed.
    suspend fun fetchNews(): List<NewsItem> {
        val fallback = emptyList<NewsItem>()
        metadataCache = fallback
        return fallback
    }

    suspend fun fetchNewsContent(id: String): String {
        return "No content available for this article."
    }

    fun getCachedItem(id: String): NewsItem? = metadataCache?.find { it.id == id }
}
