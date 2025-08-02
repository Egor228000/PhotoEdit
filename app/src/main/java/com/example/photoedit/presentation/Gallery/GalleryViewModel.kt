package com.example.photoedit.presentation.Gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class GalleryViewModel @Inject constructor( @ApplicationContext private val context: Context) : ViewModel() {

    private val _listImage = MutableStateFlow<List<Uri>>(emptyList())
    val listImage: StateFlow<List<Uri>> = _listImage.asStateFlow()
    fun addlistImage(list: List<Uri>) {
        _listImage.value += list
    }

    private val _selectedImage = MutableStateFlow<Uri?>(null)
    val selectedImage: StateFlow<Uri?> = _selectedImage.asStateFlow()
    fun addSelectedImage(image: Uri?) {
        _selectedImage.value = image
    }

    private val _fullSizeImage = MutableStateFlow<Uri?>(null)
    val fullSizeImage: StateFlow<Uri?> = _fullSizeImage.asStateFlow()


    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()
    fun addSelectedIndex(add: Int) {
        _selectedIndex.value = add
    }

    fun clearSelectedIndex() {
        _selectedIndex.value = -1
    }

    private val _selectedCrop = MutableStateFlow(0)
    val selectedCrop: StateFlow<Int> = _selectedCrop.asStateFlow()
    fun addSelectedCrop(add: Int) {
        _selectedCrop.value = add
    }

    private val _selectedFiltes = MutableStateFlow(-1)
    val selectedFiltes: StateFlow<Int> = _selectedFiltes.asStateFlow()
    fun addSelectedFiltes(add: Int) {
        _selectedFiltes.value = add
    }

    private val _selectedColors = MutableStateFlow(-1)
    val selectedColors: StateFlow<Int> = _selectedColors.asStateFlow()
    fun addSelectedColors(add: Int) {
        _selectedColors.value = add
    }

    private val _resultBitmap = MutableStateFlow<Bitmap?>(null)
    val resultBitman: StateFlow<Bitmap?> = _resultBitmap.asStateFlow()
    fun addResultBitmap(bitmap: Bitmap) {
        _resultBitmap.value = bitmap
    }

    fun clearResultBitmap() {
        _resultBitmap.value = null
    }

    fun addFullSizeImage(image: Uri) {
        _fullSizeImage.value = image
    }

    fun clearFullSizeImage() {
        _fullSizeImage.value = null
    }

    fun clearSelectedImage() {
        _selectedImage.value = null
    }

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission
        .asStateFlow()

    fun addHasPermission(bool: Boolean) {
        _hasPermission.value = bool
    }

    private val _colorSelectedItem = MutableStateFlow(0)
    val colorSelectedItem: StateFlow<Int> = _colorSelectedItem.asStateFlow()
    fun addcolorSelectedItem(add: Int) {
        _colorSelectedItem.value = add
    }

    private val resolver = context.contentResolver

    companion object {
        private const val PAGE_SIZE = 50
    }

    private var currentOffset = 0
    private var isLastPage = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    fun resetPagination() {
        currentOffset = 0
        isLastPage = false
        _listImage.value = emptyList()
    }

    val strokeWidthDp = mutableStateOf(4.dp)

    // Загрузка медиа
    fun loadNextPage() {
        if (_isLoading.value || isLastPage) return

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
            )
            val newItems = mutableListOf<Pair<Uri, Long>>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val args = Bundle().apply {
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Images.Media.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, currentOffset)
                }
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    args,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val date = cursor.getLong(dateCol)
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        )
                        newItems += uri to date
                    }
                }
            } else {
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    repeat(currentOffset) {
                        if (!cursor.moveToNext()) return@use
                    }
                    while (cursor.moveToNext() && newItems.size < PAGE_SIZE) {
                        val id = cursor.getLong(idCol)
                        val date = cursor.getLong(dateCol)
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        )
                        newItems += uri to date
                    }
                }
            }

            if (newItems.size < PAGE_SIZE) isLastPage = true
            currentOffset += newItems.size
            _listImage.update { it + newItems.map { it.first } }
            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun removeBackgroundFromBitmap(
        bitmap: Bitmap,
        scope: CoroutineScope,
        onResult: (Bitmap?) -> Unit
    ) {
        scope.launch {
            val result: Bitmap? = try {
                val options = SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .enableRawSizeMask()
                    .build()
                val segmenter = Segmentation.getClient(options)

                val input = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val inputImage = InputImage.fromBitmap(input, 0)
                val mask = segmenter.process(inputImage).await()

                withContext(Dispatchers.Default) {
                    val buffer = mask.buffer
                    val maskW = mask.width
                    val maskH = mask.height

                    buffer.rewind()
                    val rawMask = Bitmap.createBitmap(maskW, maskH, Bitmap.Config.ALPHA_8)
                    for (y in 0 until maskH) {
                        for (x in 0 until maskW) {
                            val conf = buffer.getFloat()
                            val a = (conf * 255).toInt().coerceIn(0, 255)
                            // в ALPHA_8 важен только старший байт
                            rawMask.setPixel(x, y, (a shl 24))
                        }
                    }

                    val scaledMask = Bitmap.createScaledBitmap(rawMask, input.width, input.height, true)

                    val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
                    for (y in 0 until input.height) {
                        for (x in 0 until input.width) {
                            val alpha = Color.alpha(scaledMask.getPixel(x, y))
                            val pix   = input.getPixel(x, y)
                            output.setPixel(
                                x, y,
                                Color.argb(alpha, Color.red(pix), Color.green(pix), Color.blue(pix))
                            )
                        }
                    }
                    output
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            onResult(result)
        }
    }
    suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

    // Матрица для регулировки яркости
    fun brightnessMatrix(brightness: Float): ColorMatrix {
        val b = brightness
        return ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    // Матрица для регулировки контраста
    fun contrastMatrix(contrast: Float): ColorMatrix {
        val c = contrast
        val t = (1f - c) * 128f
        return ColorMatrix(floatArrayOf(
            c,   0f,  0f,  0f,  t,
            0f,   c,  0f,  0f,  t,
            0f,   0f,  c,  0f,  t,
            0f,   0f,  0f,  1f,  0f
        ))
    }

    // Матрица для регулировки насыщенности
    fun saturationMatrix(sat: Float): ColorMatrix {
        return ColorMatrix().apply { setToSaturation(sat) }
    }

    // Инвертирует цвета
    fun invertMatrix(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            -1f, 0f,  0f,  0f, 255f,
            0f,-1f,  0f,  0f, 255f,
            0f, 0f, -1f,  0f, 255f,
            0f, 0f,  0f,  1f,   0f
        ))
    }

}