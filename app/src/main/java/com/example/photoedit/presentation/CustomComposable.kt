package com.example.photoedit.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photoedit.presentation.Filters.ColoredStroke
import com.example.photoedit.presentation.Gallery.GalleryViewModel
import kotlin.math.roundToInt

@Composable
fun CustomSaveButton(
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(Color(0xFFE5EBF2)),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Отмена", color = Color.Black) }

        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(Color(0xFF0C7EF0)),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Сохранить") }
    }
}


@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    value: Float = 0.5f,
    onValueChange: (Float) -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    Slider(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor       = Color(0xFF0C7EF0),
            activeTrackColor = Color(0xFF105293),
            activeTickColor  = Color(0xFF105293),
            inactiveTrackColor = Color(0xFF0C7EF0),
            inactiveTickColor  = Color(0xFF105293),
        )
    )
}

@Composable
fun ColorSelectedItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tintColor = if (isSelected) Color.White else Color.Transparent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))

            .size(30.dp)
            .background(color)
            .border(2.dp, tintColor, shape = RoundedCornerShape(50.dp))
            .clickable(onClick = {onClick()})

    ) {
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawableImage(
    modifier: Modifier = Modifier,
    strokes: List<ColoredStroke>,
    currentStroke: ColoredStroke?,
    onStrokeStart: (Offset, Color, Float) -> Unit,
    onStroke: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    galleryViewModel: GalleryViewModel
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val currentColor = when (galleryViewModel.colorSelectedItem.value) {
                            0  -> Color.Black
                            1  -> Color.DarkGray
                            2  -> Color.Gray
                            3  -> Color.LightGray
                            4  -> Color.White
                            5  -> Color.Red
                            6  -> Color.Green
                            7  -> Color.Blue
                            8  -> Color.Cyan
                            9  -> Color.Yellow
                            10 -> Color.Magenta
                            11 -> Color(0xFFFF9800)
                            12 -> Color(0xFF9C27B0)
                            13 -> Color(0xFF795548)
                            14 -> Color(0xFF009688)
                            else -> Color.Black
                        }

                        val widthPx = with(density) { galleryViewModel.strokeWidthDp.value.toPx() }
                        onStrokeStart(offset, currentColor, widthPx)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onStroke(change.position)
                    },
                    onDragEnd = { onStrokeEnd() },
                    onDragCancel = { onStrokeEnd() }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {

            for (stroke in strokes) {
                if (stroke.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        stroke.points.drop(1).forEach { pt ->
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            currentStroke?.let { stroke ->
                if (stroke.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        stroke.points.drop(1).forEach { pt ->
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun StrokeWidthSlider(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier,
) {
    val strokeWidthDp by viewModel.strokeWidthDp.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Толщина:",
            modifier = Modifier.padding(end = 8.dp),
            fontSize = 14.sp
        )

        CustomSlider(
            modifier = Modifier.weight(1f),
            value = strokeWidthDp.value,
            onValueChange = { newValue ->
                viewModel.addStrokeWidthDp(newValue.dp)
            },
            valueRange = 1f..20f,
            steps = 19
        )

        Text(
            text = "${strokeWidthDp.value.roundToInt()} dp",
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}









@Composable
fun CustomChipButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tintColor = if (isSelected) Color(0xFF0C7EF0) else Color.LightGray
    Button(
        onClick = { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(tintColor)
    ) {
        Text(text, color = Color.White)
    }
}

@Composable
fun CustomFiltersButton(
    color: Color,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tintColor = if (isSelected) Color.White else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, tintColor, shape = RoundedCornerShape(10.dp))
            .size(50.dp)
            .background(color)
    ) {
    }
}

fun Context.uriToBitmap(uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}




