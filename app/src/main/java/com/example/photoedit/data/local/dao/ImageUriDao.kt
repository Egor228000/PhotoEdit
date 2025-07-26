package com.example.photoedit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photoedit.data.local.entity.ImageUriEntity


@Dao
interface ImageUriDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUri(entity: ImageUriEntity)

    @Query("SELECT * FROM ImageUriEntity WHERE id = :id")
    suspend fun getUri(id: Int): ImageUriEntity?
}

