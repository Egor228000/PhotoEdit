package com.example.photoedit.data.repository.impl

import com.example.photoedit.data.local.dao.ImageUriDao
import com.example.photoedit.data.local.entity.ImageUriEntity
import com.example.photoedit.domain.repository.Repository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl @Inject constructor(
    private val imageUriDao: ImageUriDao,
) : Repository {
    override suspend fun saveUri(entity: ImageUriEntity) =
        imageUriDao.saveUri(entity)

    override suspend fun getUri(id: Int): ImageUriEntity? =
        imageUriDao.getUri(id)

}