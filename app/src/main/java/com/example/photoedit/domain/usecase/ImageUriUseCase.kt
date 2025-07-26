package com.example.photoedit.domain.usecase

import android.net.Uri
import com.example.photoedit.data.local.entity.ImageUriEntity
import com.example.photoedit.domain.repository.Repository
import javax.inject.Inject

class GetImageUriUseCase @Inject constructor(
    private val repo: Repository
) {
    suspend operator fun invoke(id: Int): ImageUriEntity? =
        repo.getUri(id)
}

class SaveImageUriUseCase @Inject constructor(
    private val repo: Repository
) {
    suspend operator fun invoke(uri: Uri, name: String) =
        repo.saveUri(ImageUriEntity(uri = uri, name = name))
}
