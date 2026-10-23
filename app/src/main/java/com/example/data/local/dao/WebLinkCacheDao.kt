package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.WebLinkCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebLinkCacheDao {
    @Query("SELECT * FROM web_link_cache WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): WebLinkCacheEntity?

    @Query("SELECT * FROM web_link_cache ORDER BY fetchedAt DESC")
    fun observeAll(): Flow<List<WebLinkCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WebLinkCacheEntity)

    @Query("UPDATE web_link_cache SET displayMode = :displayMode WHERE url = :url")
    suspend fun updateDisplayMode(url: String, displayMode: String)

    @Query("DELETE FROM web_link_cache WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}
