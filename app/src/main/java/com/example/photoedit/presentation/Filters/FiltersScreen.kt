package com.example.photoedit.presentation.Filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photoedit.CustomIconButton
import com.example.photoedit.R
import com.example.photoedit.presentation.ColorSelectedItem
import com.example.photoedit.presentation.CustomChipButton
import com.example.photoedit.presentation.CustomFiltersButton
import com.example.photoedit.presentation.CustomSaveButton
import com.example.photoedit.presentation.CustomSlider
import com.example.photoedit.presentation.DrawableImage
import com.example.photoedit.presentation.Gallery.GalleryViewModel
import com.example.photoedit.presentation.StrokeWidthSlider
import com.example.photoedit.presentation.uriToBitmap
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.tanishranjan.cropkit.CropColors
import com.tanishranjan.cropkit.CropOptions
import com.tanishranjan.cropkit.CropShape
import com.tanishranjan.cropkit.GridLinesType
import com.tanishranjan.cropkit.GridLinesVisibility
import com.tanishranjan.cropkit.ImageCropper
import com.tanishranjan.cropkit.rememberCropController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FiltersScreen(
    galleryViewModel: GalleryViewModel,
    uri: String?,
    onNavigateGallery: () -> Unit,
) {
    val selectedIndex by galleryViewModel.selectedIndex.collectAsStateWithLifecycle()
    val selectedCrop by galleryViewModel.selectedCrop.collectAsStateWithLifecycle()
    val selectedImage by galleryViewModel.selectedImage.collectAsStateWithLifecycle()
    val resultBitman by galleryViewModel.resultBitman.collectAsStateWithLifecycle()
    val selectedFiltes by galleryViewModel.selectedFiltes.collectAsStateWithLifecycle()
    val selectedColors by galleryViewModel.selectedColors.collectAsStateWithLifecycle()
    val colorSelectedItem by galleryViewModel.colorSelectedItem.collectAsStateWithLifecycle()

    val rotationZImage = remember { mutableFloatStateOf(0f) }
    val isRotated = remember { mutableStateOf(false) }
    val rotationYImage = animateFloatAsState(
        targetValue = if (isRotated.value) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    var aspectRatio by remember { mutableStateOf(1f) }

    var watchOriginalImage by remember { mutableStateOf(false) }

    aspectRatio = when (selectedCrop) {
        1 -> 1f
        2 -> 1.5f
        3 -> 1.25f
        4 -> 1.7777777f
        else -> 1f
    }
    val items = listOf(
        R.drawable.baseline_crop_free_24 to "Настроить",
        R.drawable.baseline_crop_square_24 to "1:1",
        R.drawable.baseline_crop_3_2_24 to "3:2",
        R.drawable.baseline_crop_5_4_24 to "5:4",
        R.drawable.baseline_crop_16_9_24 to "16:9",
    )
    val itemsFilters = listOf(
        Color.DarkGray to "Черно-белый",
        Color.Green to "Темно-зеленый",
        Color.Red to "3:2",
        Color.Blue to "5:4",
        Color.Cyan to "16:9",
        Color.LightGray to "3"
    )
    val itemsColorsSelected = listOf(
        Color.Black,
        Color.DarkGray,
        Color.Gray,
        Color.LightGray,
        Color.White,
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Cyan,
        Color.Yellow,
        Color.Magenta,
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFF795548),
        Color(0xFF009688)
    )
    val itemsColors = listOf(
        "Яркость",
        "Контраст",
        "Насыщенность",
        "Инвертировать цвет",
    )


    val colorFilters = remember { mutableStateOf<ColorFilter?>(null) }
    val strokes = remember { mutableStateListOf<SnapshotStateList<Offset>>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        galleryViewModel.addSelectedImage(uri?.toUri())
    }

    if (selectedImage.toString() == "null") {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Button(
                onClick = { onNavigateGallery() },
                colors = ButtonDefaults.buttonColors(Color(0xFF0C7EF0)),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Выберите фотографию") }

        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(uri) {
                uri?.toUri()?.let { uri ->
                    bitmap = withContext(Dispatchers.IO) {
                        context.uriToBitmap(uri)
                    }
                }
            }

            bitmap?.let { bmp ->
                val cropController = rememberCropController(
                    bitmap = (resultBitman ?: bmp),
                    cropOptions = CropOptions(
                        contentScale = ContentScale.Fit,
                        cropShape =
                            if (selectedCrop == 0) CropShape.FreeForm
                            else CropShape.AspectRatio(aspectRatio),
                        gridLinesVisibility = GridLinesVisibility.ON_TOUCH,
                        gridLinesType = GridLinesType.GRID,
                        handleRadius = 10.dp,
                        touchPadding = 16.dp
                    ),
                    cropColors = CropColors(
                        overlay = Color.Transparent,
                        overlayActive = Color(0x68000000),
                        gridlines = Color.White,
                        cropRectangle = Color.Black,
                        handle = Color.Black
                    )
                )

                if (selectedIndex == 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(bottom = 250.dp, end = 16.dp, start = 16.dp)
                    ) {
                        ImageCropper(
                            modifier = Modifier
                                .graphicsLayer(
                                    rotationZ = rotationZImage.floatValue,
                                    rotationY = rotationYImage.value
                                )
                                .fillMaxSize(),
                            cropController = cropController
                        )
                    }

                } else {
                    when (selectedIndex) {
                        3 -> {}
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if (!watchOriginalImage) {

                                    Image(
                                        bitmap = (resultBitman ?: bmp).asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        alignment = Alignment.Center,
                                        contentScale = ContentScale.Fit,
                                        colorFilter = colorFilters.value
                                    )
                                } else {

                                    CoilImage(
                                        imageModel = { selectedImage },
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        imageOptions = ImageOptions(
                                            contentScale = ContentScale.Fit

                                        )

                                    )

                                }
                                Icon(
                                    painterResource(R.drawable.outline_photo_size_select_small_24),
                                    null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            forEachGesture {
                                                awaitPointerEventScope {
                                                    val down = awaitFirstDown()

                                                    val longPressJob = scope.launch {
                                                        delay(100)
                                                        watchOriginalImage = true


                                                    }

                                                    val up = waitForUpOrCancellation()

                                                    longPressJob.cancel()

                                                    if (up != null) {
                                                        val duration =
                                                            up.uptimeMillis - down.uptimeMillis
                                                        if (duration < 100) {
                                                            watchOriginalImage = false


                                                        } else {
                                                            scope.launch {
                                                                watchOriginalImage = false


                                                            }
                                                        }
                                                    } else {
                                                        longPressJob.cancel()
                                                        watchOriginalImage = false


                                                    }
                                                }
                                            }
                                        }
                                        .align(Alignment.BottomCenter)
                                )

                            }


                        }
                    }
                }

                if (selectedIndex == 0) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            IconButton(
                                onClick = { isRotated.value = !isRotated.value }
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_horizontal_distribute_24),
                                    null
                                )
                            }
                            IconButton(
                                onClick = {
                                    rotationZImage.floatValue = when (rotationZImage.floatValue) {
                                        0f -> 90f
                                        90f -> 180f
                                        180f -> 270f
                                        else -> 0f
                                    }
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_crop_rotate_24),
                                    null
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(50.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE5EBF2))
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            items.forEachIndexed { index, (icon, text) ->
                                CustomIconButton(
                                    icon = icon,
                                    text = text,
                                    isSelected = selectedCrop == index,
                                    onClick = { galleryViewModel.addSelectedCrop(index) }
                                )
                            }
                        }
                        CustomSaveButton(
                            galleryViewModel,
                            rotationZImage,
                            isRotated,
                            colorFilters,
                            strokes,
                            onSave = {
                                scope.launch {

                                    val raw = runCatching { cropController.crop() }.getOrNull()
                                        ?: galleryViewModel.selectedImage.value?.let {
                                            context.uriToBitmap(it)
                                        } ?: return@launch

                                    val cropped = if (raw.config == Bitmap.Config.HARDWARE) {
                                        raw.copy(Bitmap.Config.ARGB_8888, false)
                                    } else raw

                                    val matrix = Matrix().apply {
                                        postRotate(
                                            rotationZImage.floatValue,
                                            cropped.width / 2f,
                                            cropped.height / 2f
                                        )
                                        if (rotationYImage.value == 180f) {
                                            postScale(
                                                -1f,
                                                1f,
                                                cropped.width / 2f,
                                                cropped.height / 2f
                                            )
                                        }
                                    }
                                    val finalBitmap = Bitmap.createBitmap(
                                        cropped, 0, 0, cropped.width, cropped.height, matrix, true
                                    ).copy(Bitmap.Config.ARGB_8888, true)
                                    galleryViewModel.addResultBitmap(finalBitmap)
                                    galleryViewModel.addSelectedIndex(-1)
                                }
                            }
                        )
                    }
                } else if (selectedIndex == 1) {
                    colorFilters.value = when (selectedFiltes) {
                        0 -> ColorFilter.tint(Color.DarkGray, blendMode = BlendMode.Multiply)
                        1 -> ColorFilter.tint(Color.Green, blendMode = BlendMode.Multiply)
                        2 -> ColorFilter.tint(Color.Red, blendMode = BlendMode.Multiply)
                        3 -> ColorFilter.tint(Color.Blue, blendMode = BlendMode.Multiply)
                        4 -> ColorFilter.tint(Color.Cyan, blendMode = BlendMode.Multiply)
                        5 -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                        else -> {
                            ColorFilter.tint(Color.Transparent, blendMode = BlendMode.Multiply)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE5EBF2))
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            itemsFilters.forEachIndexed { index, (color, text) ->
                                CustomFiltersButton(
                                    color,
                                    text,
                                    isSelected = selectedFiltes == index,
                                    onClick = { galleryViewModel.addSelectedFiltes(index) }
                                )
                            }
                        }
                        CustomSaveButton(
                            galleryViewModel,
                            rotationZImage,
                            isRotated,
                            colorFilters,
                            strokes,
                            onSave = {
                                scope.launch {
                                    val raw = galleryViewModel.resultBitman.value
                                        ?: galleryViewModel.selectedImage.value?.let { uri ->
                                            context.uriToBitmap(uri)
                                        }
                                        ?: return@launch

                                    val finalBitmap = if (raw.config == Bitmap.Config.HARDWARE) {
                                        raw.copy(Bitmap.Config.ARGB_8888, true)
                                    } else {
                                        raw.copy(Bitmap.Config.ARGB_8888, true)
                                    }

                                    val filter = colorFilters.value
                                    val outputBitmap = if (filter != null) {
                                        createBitmap(
                                            finalBitmap.width,
                                            finalBitmap.height
                                        ).also { bmpWithFilter ->
                                            Canvas(bmpWithFilter).drawBitmap(
                                                finalBitmap, 0f, 0f,
                                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                    colorFilter = filter.asAndroidColorFilter()
                                                }
                                            )
                                        }
                                    } else {
                                        finalBitmap
                                    }

                                    galleryViewModel.addResultBitmap(outputBitmap)
                                    galleryViewModel.allClear()
                                    colorFilters.value = null
                                    rotationZImage.floatValue = 0f
                                    isRotated.value = false
                                }
                            }
                        )
                    }
                } else if (selectedIndex == 2) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE5EBF2))
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            itemsColors.forEachIndexed { index, text ->
                                CustomChipButton(
                                    text,
                                    isSelected = selectedColors == index,
                                    onClick = { galleryViewModel.addSelectedColors(index) }
                                )
                            }
                        }
                        var progressBrightness by remember { mutableFloatStateOf(0f) }
                        var progressContrast by remember { mutableFloatStateOf(1f) }
                        var progressSaturation by remember { mutableFloatStateOf(1f) }

                        when (selectedColors) {

                            0 -> {
                                CustomSlider(
                                    value = progressBrightness,
                                    onValueChange = { progressBrightness = it },
                                    valueRange = -255f..255f
                                )
                            }

                            1 -> {
                                CustomSlider(
                                    value = progressContrast,
                                    onValueChange = { progressContrast = it },
                                    valueRange = 0f..4f
                                )

                            }

                            2 -> {
                                CustomSlider(
                                    value = progressSaturation,
                                    onValueChange = { progressSaturation = it },
                                    valueRange = 0f..2f
                                )

                            }

                            3 -> {
                                Text("Инверсия цвета")
                            }
                        }
                        val colorMatrix = remember(
                            progressBrightness,
                            progressContrast,
                            progressSaturation,
                            selectedColors
                        ) {
                            ColorMatrix().apply {
                                this *= when (selectedColors) {
                                    0 -> galleryViewModel.brightnessMatrix(progressBrightness)
                                    1 -> galleryViewModel.contrastMatrix(progressContrast)
                                    2 -> galleryViewModel.saturationMatrix(progressSaturation)
                                    3 -> galleryViewModel.invertMatrix()
                                    else -> ColorMatrix()
                                }
                            }
                        }

                        LaunchedEffect(colorMatrix) {
                            colorFilters.value = ColorFilter.colorMatrix(colorMatrix)
                        }
                        CustomSaveButton(
                            galleryViewModel,
                            rotationZImage,
                            isRotated,
                            colorFilters,
                            strokes,
                            onSave = {
                                scope.launch {
                                    val raw = galleryViewModel.resultBitman.value
                                        ?: galleryViewModel.selectedImage.value?.let { uri ->
                                            context.uriToBitmap(uri)
                                        }
                                        ?: return@launch

                                    val finalBitmap = if (raw.config == Bitmap.Config.HARDWARE) {
                                        raw.copy(Bitmap.Config.ARGB_8888, true)
                                    } else {
                                        raw.copy(Bitmap.Config.ARGB_8888, true)
                                    }

                                    val filter = colorFilters.value
                                    val outputBitmap = filter?.let {
                                        createBitmap(
                                            finalBitmap.width,
                                            finalBitmap.height
                                        ).also { bmpWithFilter ->
                                            Canvas(bmpWithFilter).drawBitmap(
                                                finalBitmap, 0f, 0f,
                                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                    colorFilter = filter.asAndroidColorFilter()
                                                }
                                            )
                                        }
                                    } ?: finalBitmap

                                    galleryViewModel.addResultBitmap(outputBitmap)
                                    galleryViewModel.allClear()


                                }
                            }
                        )
                    }
                } else if (selectedIndex == 3) {

                    val captureLayer = rememberGraphicsLayer()
                    var drawAreaSize by remember { mutableStateOf(IntSize.Zero) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .onSizeChanged { drawAreaSize = it }
                                .drawWithContent {
                                    val parent = this
                                    captureLayer.record { parent.drawContent() }
                                    parent.drawContent()
                                    drawLayer(captureLayer)
                                }
                        ) {
                            Image(
                                bitmap = (resultBitman ?: bmp).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                                colorFilter = colorFilters.value
                            )
                            val strokes = remember { mutableStateListOf<ColoredStroke>() }
                            var currentStroke by remember { mutableStateOf<ColoredStroke?>(null) }

                            DrawableImage(
                                modifier = Modifier.matchParentSize(),
                                strokes = strokes,
                                currentStroke = currentStroke,
                                onStrokeStart = { offset, color, widthPx ->
                                    val newPoints = mutableStateListOf(offset)
                                    val newStroke = ColoredStroke(newPoints, color, widthPx)
                                    currentStroke = newStroke
                                    strokes.add(newStroke)
                                },
                                onStroke = { pos -> currentStroke?.points?.add(pos) },
                                onStrokeEnd = { currentStroke = null },
                                galleryViewModel = galleryViewModel
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE5EBF2))
                                .horizontalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            itemsColorsSelected.forEachIndexed { index, color ->
                                ColorSelectedItem(
                                    color,
                                    isSelected = colorSelectedItem == index,
                                    onClick = { galleryViewModel.addcolorSelectedItem(index) }
                                )
                            }
                        }
                        StrokeWidthSlider(
                            viewModel = galleryViewModel,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        CustomSaveButton(
                            galleryViewModel,
                            rotationZImage,
                            isRotated,
                            colorFilters,
                            strokes,
                            onSave = {
                                scope.launch {
                                    val fullBitmap = captureLayer.toImageBitmap().asAndroidBitmap()
                                    val viewW = drawAreaSize.width.toFloat()
                                    val viewH = drawAreaSize.height.toFloat()

                                    val bmpOrig = resultBitman ?: bmp
                                    val bmpW = bmpOrig.width.toFloat()
                                    val bmpH = bmpOrig.height.toFloat()

                                    val scale = min(viewW / bmpW, viewH / bmpH)
                                    val dispW = (bmpW * scale).roundToInt()
                                    val dispH = (bmpH * scale).roundToInt()

                                    val offsetX = ((viewW - dispW) / 2f).roundToInt()
                                    val offsetY = ((viewH - dispH) / 2f).roundToInt()

                                    val cropped = Bitmap.createBitmap(
                                        fullBitmap,
                                        offsetX,
                                        offsetY,
                                        dispW.coerceAtMost(fullBitmap.width - offsetX),
                                        dispH.coerceAtMost(fullBitmap.height - offsetY)
                                    )

                                    galleryViewModel.addResultBitmap(cropped)
                                    galleryViewModel.allClear()

                                }
                            }
                        )
                    }

                } else if (selectedIndex == 4) {
                    galleryViewModel.removeBackgroundFromBitmap(
                        bitmap = resultBitman ?: bmp,
                        scope
                    ) { output ->
                        output?.let { bgRemoved ->
                            val mutableBitmap = if (bgRemoved.config == Bitmap.Config.HARDWARE) {
                                bgRemoved.copy(Bitmap.Config.ARGB_8888, true)
                            } else {
                                bgRemoved.copy(Bitmap.Config.ARGB_8888, true)
                            }

                            galleryViewModel.addResultBitmap(mutableBitmap)

                            galleryViewModel.allClear()
                            colorFilters.value = null
                            rotationZImage.floatValue = 0f
                            isRotated.value = false
                        }
                    }
                }
            }
        }
    }
}


data class ColoredStroke(
    val points: SnapshotStateList<Offset>,
    val color: Color,
    val strokeWidth: Float,
)

