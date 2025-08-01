package com.example.photoedit.presentation.Gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel,
    onNavigateFilters: (String) -> Unit,
) {
    val listImage by galleryViewModel.listImage.collectAsStateWithLifecycle()
    val hasPermission by galleryViewModel.hasPermission.collectAsStateWithLifecycle()
    val fullSizeImage by galleryViewModel.fullSizeImage.collectAsStateWithLifecycle()
    val isLoading by galleryViewModel.isLoading.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        galleryViewModel.addHasPermission(permissions.values.any { it })
    }

    val scope = rememberCoroutineScope()
    val stateScroll = rememberLazyGridState()
    val context = LocalContext.current



    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imagesGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val videosGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            galleryViewModel.addHasPermission(imagesGranted && videosGranted)

            if (!imagesGranted || !videosGranted) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                )
            }
        } else {
            val storageGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            galleryViewModel.addHasPermission(storageGranted)

            if (!storageGranted) {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                )
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            galleryViewModel.resetPagination()
            galleryViewModel.loadNextPage()
            galleryViewModel.clearSelectedImage()
            galleryViewModel.clearResultBitmap()
            galleryViewModel.clearSelectedIndex()
            galleryViewModel.addSelectedFiltes(-1)

        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { stateScroll.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .filter { it >= listImage.size - 5 }
            .collect {
                galleryViewModel.loadNextPage()
            }
    }
    LazyVerticalGrid(
        modifier = Modifier.blur(
            if (fullSizeImage.toString() == "null") 0.dp else 10.dp
        ),
        state = stateScroll,
        columns = GridCells.Adaptive(170.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(listImage) { uri ->
            CoilImage(
                imageModel = { uri },
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {
                                val down = awaitFirstDown()

                                val longPressJob = scope.launch {
                                    delay(200)
                                    galleryViewModel.addFullSizeImage(uri)

                                }

                                val up = waitForUpOrCancellation()

                                longPressJob.cancel()

                                if (up != null) {
                                    val duration = up.uptimeMillis - down.uptimeMillis
                                    if (duration < 100) {
                                        onNavigateFilters(uri.toString())


                                    } else {
                                        scope.launch {
                                            galleryViewModel.clearFullSizeImage()

                                            galleryViewModel.clearFullSizeImage()


                                        }
                                    }
                                } else {
                                    longPressJob.cancel()
                                    galleryViewModel.clearFullSizeImage()


                                }
                            }
                        }
                    },
                imageOptions = ImageOptions(contentScale = ContentScale.Crop)

            )
        }
        if (isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    AnimatedVisibility(
        visible = fullSizeImage != null,
        enter = scaleIn(animationSpec = tween(500)),
        exit = scaleOut(animationSpec = tween(300))
    ) {

        CoilImage(
            imageModel = { fullSizeImage },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(10.dp)),
            imageOptions = ImageOptions(contentScale = ContentScale.Fit)

        )
    }

}
