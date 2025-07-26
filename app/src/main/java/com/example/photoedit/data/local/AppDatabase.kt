package com.example.photoedit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.photoedit.core.di.Converters
import com.example.photoedit.data.local.dao.ImageUriDao
import com.example.photoedit.data.local.entity.ImageUriEntity

@Database(
    entities = [ImageUriEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageUriDao(): ImageUriDao
}
