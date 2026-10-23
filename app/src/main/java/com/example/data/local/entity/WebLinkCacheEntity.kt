package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "web_link_cache")
data class WebLinkCacheEntity(
    @PrimaryKey val url: String,
    val domain: String,
    val title: String,
    val description: String = "",
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val siteName: String = "",
    val faviconUrl: String = "",
    val localFaviconPath: String? = null,
    val articleExcerpt: String = "",
    val displayMode: String = "LARGE",
    val fetchedAt: Long = System.currentTimeMillis(),
    val isLoadedFromRemote: Boolean = false
)
