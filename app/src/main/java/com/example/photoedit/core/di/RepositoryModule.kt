package com.example.photoedit.core.di

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.TypeConverter
import com.example.photoedit.data.local.AppDatabase
import com.example.photoedit.data.local.dao.ImageUriDao
import com.example.photoedit.data.repository.impl.RepositoryImpl
import com.example.photoedit.domain.repository.Repository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

object Converters {
    @TypeConverter
    fun uriToString(uri: Uri): String = uri.toString()

    @TypeConverter
    fun stringToUri(value: String): Uri = Uri.parse(value)
}



@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "photo_db")
            .fallbackToDestructiveMigration(true)
            .addTypeConverter(Converters)
            .build()

    @Provides fun provideImageUriDao(db: AppDatabase): ImageUriDao = db.imageUriDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(
        impl: RepositoryImpl
    ): Repository
}