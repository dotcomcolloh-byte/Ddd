package com.telefam.company.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Crisp 24x24 Lucide-style vector icons rendered on Compose Canvas with rounded stroke caps & joins.
 */
object TelefamComLucideIcons {

    @Composable
    fun ClockIcon(
        tint: Color,
        size: Dp = 14.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.11f).coerceAtLeast(1.4.dp.toPx())
            drawCircle(
                color = tint,
                radius = w * 0.42f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val handPath = Path().apply {
                moveTo(w * 0.50f, h * 0.28f)
                lineTo(w * 0.50f, h * 0.50f)
                lineTo(w * 0.66f, h * 0.58f)
            }
            drawPath(
                path = handPath,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun SingleTickIcon(
        tint: Color,
        size: Dp = 14.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.13f).coerceAtLeast(1.5.dp.toPx())
            val path = Path().apply {
                moveTo(w * 0.18f, h * 0.54f)
                lineTo(w * 0.42f, h * 0.76f)
                lineTo(w * 0.84f, h * 0.26f)
            }
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun DeliveredRedBadgeIcon(
        badgeColor: Color = Color(0xFFC80815),
        checkColor: Color = Color.White,
        size: Dp = 15.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            drawCircle(
                color = badgeColor,
                radius = w * 0.46f,
                center = Offset(w / 2f, h / 2f),
                style = Fill
            )
            val stroke = (w * 0.13f).coerceAtLeast(1.4.dp.toPx())
            val checkPath = Path().apply {
                moveTo(w * 0.28f, h * 0.52f)
                lineTo(w * 0.44f, h * 0.68f)
                lineTo(w * 0.74f, h * 0.34f)
            }
            drawPath(
                path = checkPath,
                color = checkColor,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun MicVoiceIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.6.dp.toPx())

            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.36f, h * 0.10f),
                size = Size(w * 0.28f, h * 0.46f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = tint,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.22f, h * 0.24f),
                size = Size(w * 0.56f, h * 0.48f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.50f, h * 0.72f),
                end = Offset(w * 0.50f, h * 0.88f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.34f, h * 0.88f),
                end = Offset(w * 0.66f, h * 0.88f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun StickerSmileIcon(
        tint: Color,
        size: Dp = 20.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.095f).coerceAtLeast(1.6.dp.toPx())

            val outer = Path().apply {
                moveTo(w * 0.22f, h * 0.12f)
                lineTo(w * 0.78f, h * 0.12f)
                lineTo(w * 0.88f, h * 0.22f)
                lineTo(w * 0.88f, h * 0.64f)
                lineTo(w * 0.64f, h * 0.88f)
                lineTo(w * 0.22f, h * 0.88f)
                close()
            }
            drawPath(
                path = outer,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            val crease = Path().apply {
                moveTo(w * 0.62f, h * 0.88f)
                lineTo(w * 0.62f, h * 0.62f)
                lineTo(w * 0.88f, h * 0.62f)
            }
            drawPath(
                path = crease,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(color = tint, radius = stroke * 0.65f, center = Offset(w * 0.38f, h * 0.40f))
            drawCircle(color = tint, radius = stroke * 0.65f, center = Offset(w * 0.60f, h * 0.40f))
            drawArc(
                color = tint,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.34f, h * 0.40f),
                size = Size(w * 0.30f, h * 0.22f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }

    @Composable
    fun PaperclipIcon(
        tint: Color,
        size: Dp = 20.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.095f).coerceAtLeast(1.6.dp.toPx())
            val path = Path().apply {
                moveTo(w * 0.72f, h * 0.44f)
                lineTo(w * 0.40f, h * 0.76f)
                cubicTo(w * 0.28f, h * 0.88f, w * 0.12f, h * 0.72f, w * 0.24f, h * 0.60f)
                lineTo(w * 0.62f, h * 0.22f)
                cubicTo(w * 0.70f, h * 0.14f, w * 0.82f, h * 0.26f, w * 0.74f, h * 0.34f)
                lineTo(w * 0.38f, h * 0.70f)
                cubicTo(w * 0.34f, h * 0.74f, w * 0.28f, h * 0.68f, w * 0.32f, h * 0.64f)
                lineTo(w * 0.62f, h * 0.34f)
            }
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun ShieldCheckIcon(
        tint: Color,
        size: Dp = 16.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.4.dp.toPx())
            val shield = Path().apply {
                moveTo(w * 0.50f, h * 0.10f)
                lineTo(w * 0.84f, h * 0.24f)
                lineTo(w * 0.84f, h * 0.52f)
                cubicTo(w * 0.84f, h * 0.74f, w * 0.68f, h * 0.86f, w * 0.50f, h * 0.92f)
                cubicTo(w * 0.32f, h * 0.86f, w * 0.16f, h * 0.74f, w * 0.16f, h * 0.52f)
                lineTo(w * 0.16f, h * 0.24f)
                close()
            }
            drawPath(
                path = shield,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            val check = Path().apply {
                moveTo(w * 0.35f, h * 0.52f)
                lineTo(w * 0.46f, h * 0.63f)
                lineTo(w * 0.67f, h * 0.40f)
            }
            drawPath(
                path = check,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun PhoneCallLucideIcon(
        tint: Color,
        size: Dp = 20.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.095f).coerceAtLeast(1.6.dp.toPx())
            val path = Path().apply {
                moveTo(w * 0.24f, h * 0.16f)
                lineTo(w * 0.38f, h * 0.16f)
                lineTo(w * 0.45f, h * 0.34f)
                lineTo(w * 0.35f, h * 0.42f)
                cubicTo(w * 0.42f, h * 0.55f, w * 0.52f, h * 0.64f, w * 0.62f, h * 0.70f)
                lineTo(w * 0.70f, h * 0.58f)
                lineTo(w * 0.86f, h * 0.65f)
                lineTo(w * 0.86f, h * 0.80f)
                cubicTo(w * 0.86f, h * 0.86f, w * 0.78f, h * 0.90f, w * 0.70f, h * 0.88f)
                cubicTo(w * 0.38f, h * 0.80f, w * 0.18f, h * 0.56f, w * 0.14f, h * 0.26f)
                cubicTo(w * 0.13f, h * 0.20f, w * 0.18f, h * 0.16f, w * 0.24f, h * 0.16f)
                close()
            }
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun VideoCallLucideIcon(
        tint: Color,
        size: Dp = 20.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.095f).coerceAtLeast(1.6.dp.toPx())
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.10f, h * 0.26f),
                size = Size(w * 0.54f, h * 0.48f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val lens = Path().apply {
                moveTo(w * 0.64f, h * 0.42f)
                lineTo(w * 0.88f, h * 0.28f)
                lineTo(w * 0.88f, h * 0.72f)
                lineTo(w * 0.64f, h * 0.58f)
                close()
            }
            drawPath(
                path = lens,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun ViewOnceOneIcon(
        tint: Color,
        isActive: Boolean,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawCircle(
                color = tint,
                radius = w * 0.42f,
                center = Offset(w / 2f, h / 2f),
                style = if (isActive) Fill else Stroke(width = stroke)
            )
            val numColor = if (isActive) Color.White else tint
            val onePath = Path().apply {
                moveTo(w * 0.42f, h * 0.38f)
                lineTo(w * 0.52f, h * 0.30f)
                lineTo(w * 0.52f, h * 0.70f)
            }
            drawPath(
                path = onePath,
                color = numColor,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    // ---------------------------------------------------------
    // New Lucide Icons for Swipe-to-Reply & Long-Press Context Menus
    // ---------------------------------------------------------

    @Composable
    fun ReplyCurvedLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            val arrowHead = Path().apply {
                moveTo(w * 0.38f, h * 0.24f)
                lineTo(w * 0.14f, h * 0.46f)
                lineTo(w * 0.38f, h * 0.68f)
            }
            drawPath(
                path = arrowHead,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            val curve = Path().apply {
                moveTo(w * 0.16f, h * 0.46f)
                lineTo(w * 0.58f, h * 0.46f)
                cubicTo(w * 0.76f, h * 0.46f, w * 0.86f, h * 0.58f, w * 0.86f, h * 0.78f)
            }
            drawPath(
                path = curve,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun PinLucideIcon(
        tint: Color,
        isFilled: Boolean = false,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            val pinBody = Path().apply {
                moveTo(w * 0.32f, h * 0.16f)
                lineTo(w * 0.68f, h * 0.16f)
                lineTo(w * 0.64f, h * 0.46f)
                lineTo(w * 0.76f, h * 0.58f)
                lineTo(w * 0.24f, h * 0.58f)
                lineTo(w * 0.36f, h * 0.46f)
                close()
            }
            drawPath(
                path = pinBody,
                color = tint,
                style = if (isFilled) Fill else Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.50f, h * 0.58f),
                end = Offset(w * 0.50f, h * 0.88f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun StarFavouriteLucideIcon(
        tint: Color,
        isFilled: Boolean = false,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            val star = Path().apply {
                moveTo(w * 0.50f, h * 0.10f)
                lineTo(w * 0.62f, h * 0.36f)
                lineTo(w * 0.90f, h * 0.40f)
                lineTo(w * 0.70f, h * 0.60f)
                lineTo(w * 0.75f, h * 0.88f)
                lineTo(w * 0.50f, h * 0.75f)
                lineTo(w * 0.25f, h * 0.88f)
                lineTo(w * 0.30f, h * 0.60f)
                lineTo(w * 0.10f, h * 0.40f)
                lineTo(w * 0.38f, h * 0.36f)
                close()
            }
            drawPath(
                path = star,
                color = tint,
                style = if (isFilled) Fill else Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun CopyLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.34f, h * 0.34f),
                size = Size(w * 0.52f, h * 0.52f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val backRect = Path().apply {
                moveTo(w * 0.24f, h * 0.64f)
                lineTo(w * 0.18f, h * 0.64f)
                cubicTo(w * 0.14f, h * 0.64f, w * 0.14f, h * 0.60f, w * 0.14f, h * 0.56f)
                lineTo(w * 0.14f, h * 0.22f)
                cubicTo(w * 0.14f, h * 0.16f, w * 0.16f, h * 0.14f, w * 0.22f, h * 0.14f)
                lineTo(w * 0.56f, h * 0.14f)
                cubicTo(w * 0.60f, h * 0.14f, w * 0.64f, h * 0.14f, w * 0.64f, h * 0.20f)
                lineTo(w * 0.64f, h * 0.24f)
            }
            drawPath(
                path = backRect,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun CheckSquareSelectLucideIcon(
        tint: Color,
        isChecked: Boolean = false,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.14f, h * 0.14f),
                size = Size(w * 0.72f, h * 0.72f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                style = if (isChecked) Fill else Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val checkColor = if (isChecked) Color.White else tint
            val check = Path().apply {
                moveTo(w * 0.30f, h * 0.52f)
                lineTo(w * 0.45f, h * 0.66f)
                lineTo(w * 0.72f, h * 0.36f)
            }
            drawPath(
                path = check,
                color = checkColor,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun EditPencilLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            val pencil = Path().apply {
                moveTo(w * 0.68f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.32f)
                lineTo(w * 0.34f, h * 0.84f)
                lineTo(w * 0.14f, h * 0.86f)
                lineTo(w * 0.16f, h * 0.66f)
                close()
            }
            drawPath(
                path = pencil,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.56f, h * 0.26f),
                end = Offset(w * 0.74f, h * 0.44f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun TrashDeleteLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            // Top lid bar
            drawLine(
                color = tint,
                start = Offset(w * 0.16f, h * 0.24f),
                end = Offset(w * 0.84f, h * 0.24f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            // Handle
            val handle = Path().apply {
                moveTo(w * 0.36f, h * 0.24f)
                lineTo(w * 0.36f, h * 0.14f)
                lineTo(w * 0.64f, h * 0.14f)
                lineTo(w * 0.64f, h * 0.24f)
            }
            drawPath(
                path = handle,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Bin body
            val bin = Path().apply {
                moveTo(w * 0.24f, h * 0.24f)
                lineTo(w * 0.28f, h * 0.86f)
                lineTo(w * 0.72f, h * 0.86f)
                lineTo(w * 0.76f, h * 0.24f)
            }
            drawPath(
                path = bin,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.42f, h * 0.42f),
                end = Offset(w * 0.42f, h * 0.70f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.58f, h * 0.42f),
                end = Offset(w * 0.58f, h * 0.70f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun UnsendRotateCcwLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawArc(
                color = tint,
                startAngle = -140f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(w * 0.18f, h * 0.18f),
                size = Size(w * 0.64f, h * 0.64f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val arrowHead = Path().apply {
                moveTo(w * 0.16f, h * 0.18f)
                lineTo(w * 0.16f, h * 0.40f)
                lineTo(w * 0.38f, h * 0.40f)
            }
            drawPath(
                path = arrowHead,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun SmilePlusReactLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawCircle(
                color = tint,
                radius = w * 0.40f,
                center = Offset(w * 0.48f, h * 0.52f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawCircle(color = tint, radius = stroke * 0.65f, center = Offset(w * 0.36f, h * 0.44f))
            drawCircle(color = tint, radius = stroke * 0.65f, center = Offset(w * 0.58f, h * 0.44f))
            drawArc(
                color = tint,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.32f, h * 0.46f),
                size = Size(w * 0.30f, h * 0.22f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }

    @Composable
    fun FolderMediaLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            val folder = Path().apply {
                moveTo(w * 0.12f, h * 0.24f)
                lineTo(w * 0.38f, h * 0.24f)
                lineTo(w * 0.48f, h * 0.36f)
                lineTo(w * 0.88f, h * 0.36f)
                lineTo(w * 0.88f, h * 0.80f)
                lineTo(w * 0.12f, h * 0.80f)
                close()
            }
            drawPath(
                path = folder,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    @Composable
    fun SearchLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawCircle(
                color = tint,
                radius = w * 0.32f,
                center = Offset(w * 0.44f, h * 0.44f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.67f, h * 0.67f),
                end = Offset(w * 0.88f, h * 0.88f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun TimerLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawLine(
                color = tint,
                start = Offset(w * 0.38f, h * 0.12f),
                end = Offset(w * 0.62f, h * 0.12f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = tint,
                radius = w * 0.36f,
                center = Offset(w * 0.50f, h * 0.56f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.50f, h * 0.56f),
                end = Offset(w * 0.64f, h * 0.42f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun BanBlockLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            drawCircle(
                color = tint,
                radius = w * 0.40f,
                center = Offset(w * 0.50f, h * 0.50f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.22f, h * 0.22f),
                end = Offset(w * 0.78f, h * 0.78f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun FlagReportLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.10f).coerceAtLeast(1.5.dp.toPx())
            val flag = Path().apply {
                moveTo(w * 0.20f, h * 0.16f)
                cubicTo(w * 0.36f, h * 0.10f, w * 0.56f, h * 0.24f, w * 0.82f, h * 0.16f)
                lineTo(w * 0.82f, h * 0.56f)
                cubicTo(w * 0.56f, h * 0.64f, w * 0.36f, h * 0.50f, w * 0.20f, h * 0.56f)
                close()
            }
            drawPath(
                path = flag,
                color = tint,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.20f, h * 0.14f),
                end = Offset(w * 0.20f, h * 0.88f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun XCloseLucideIcon(
        tint: Color,
        size: Dp = 18.dp,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.11f).coerceAtLeast(1.5.dp.toPx())
            drawLine(
                color = tint,
                start = Offset(w * 0.22f, h * 0.22f),
                end = Offset(w * 0.78f, h * 0.78f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.78f, h * 0.22f),
                end = Offset(w * 0.22f, h * 0.78f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
