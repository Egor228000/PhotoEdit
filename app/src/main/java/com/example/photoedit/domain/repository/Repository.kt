package com.example.photoedit.domain.repository

import com.example.photoedit.data.local.entity.ImageUriEntity

interface Repository {
    suspend fun saveUri(entity: ImageUriEntity)
    suspend fun getUri(id: Int): ImageUriEntity?
}
