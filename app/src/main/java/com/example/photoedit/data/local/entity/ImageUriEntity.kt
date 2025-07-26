package com.example.photoedit.data.local.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ImageUriEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: Uri,
    val name: String
)
