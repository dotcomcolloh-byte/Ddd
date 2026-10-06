package com.telefam.company.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.telefam.company.app.R
import com.telefam.company.app.ui.theme.LocalTelefamColors
import kotlin.math.max
import kotlin.math.min

@Composable
fun TelefamComPhotoCropperDialog(
    sourceBitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    var zoomScale by remember { mutableFloatStateOf(1.15f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 600)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = telefamColors.cardBackground,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = "Crop Photo",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.crop_photo_title),
                            color = telefamColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate 90 degrees",
                                tint = telefamColors.textPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                zoomScale = 1.15f
                                rotationDegrees = 0f
                                panOffset = Offset.Zero
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Crop",
                                tint = telefamColors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Circular Crop Viewport
                val imageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF101216))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(0.6f, 4.0f)
                                panOffset += pan
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height
                        val center = Offset(cw / 2f, ch / 2f)
                        val cropRadius = min(cw, ch) * 0.42f

                        val baseScale = max(
                            (cropRadius * 2f) / sourceBitmap.width.toFloat(),
                            (cropRadius * 2f) / sourceBitmap.height.toFloat()
                        )
                        val totalScale = baseScale * zoomScale

                        withTransform({
                            translate(left = center.x + panOffset.x, top = center.y + panOffset.y)
                            rotate(degrees = rotationDegrees, pivot = Offset.Zero)
                            scale(scaleX = totalScale, scaleY = totalScale, pivot = Offset.Zero)
                        }) {
                            drawImage(
                                image = imageBitmap,
                                topLeft = Offset(
                                    -sourceBitmap.width / 2f,
                                    -sourceBitmap.height / 2f
                                )
                            )
                        }

                        // Circular crop guide ring and rule-of-thirds grid lines
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = cropRadius,
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = telefamColors.brandRed,
                            radius = cropRadius + 3.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zoom Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom Level",
                        tint = telefamColors.brandRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = zoomScale,
                        onValueChange = { zoomScale = it },
                        valueRange = 0.6f..4.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = telefamColors.brandRed,
                            activeTrackColor = telefamColors.brandRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Pinch or drag to adjust your circular profile photo",
                    color = telefamColors.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = telefamColors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val cropped = renderCircularCroppedBitmap(
                                source = sourceBitmap,
                                viewportWidth = canvasSize.width.coerceAtLeast(280),
                                viewportHeight = canvasSize.height.coerceAtLeast(280),
                                zoomScale = zoomScale,
                                rotationDegrees = rotationDegrees,
                                panOffset = panOffset
                            )
                            onCropConfirmed(cropped)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = telefamColors.brandRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_crop_button")
                    ) {
                        Text(
                            text = stringResource(R.string.apply_crop),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun renderCircularCroppedBitmap(
    source: Bitmap,
    viewportWidth: Int,
    viewportHeight: Int,
    zoomScale: Float,
    rotationDegrees: Float,
    panOffset: Offset
): Bitmap {
    val outputSize = 512
    val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)

    val cropRadiusViewport = min(viewportWidth, viewportHeight) * 0.42f
    val viewportToOutputRatio = (outputSize / 2f) / cropRadiusViewport

    val baseScale = max(
        (cropRadiusViewport * 2f) / source.width.toFloat(),
        (cropRadiusViewport * 2f) / source.height.toFloat()
    )
    val finalScale = baseScale * zoomScale * viewportToOutputRatio

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Draw circular mask first
    canvas.drawCircle(outputSize / 2f, outputSize / 2f, outputSize / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    val matrix = Matrix().apply {
        postTranslate(-source.width / 2f, -source.height / 2f)
        postScale(finalScale, finalScale)
        postRotate(rotationDegrees)
        postTranslate(
            (outputSize / 2f) + (panOffset.x * viewportToOutputRatio),
            (outputSize / 2f) + (panOffset.y * viewportToOutputRatio)
        )
    }

    canvas.drawBitmap(source, matrix, paint)
    return output
}
