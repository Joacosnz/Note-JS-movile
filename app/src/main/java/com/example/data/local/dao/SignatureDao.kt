package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SignatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureDao {

    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    fun getAllSignatures(): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Delete
    suspend fun deleteSignature(signature: SignatureEntity)

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteSignatureById(id: Long)
}
