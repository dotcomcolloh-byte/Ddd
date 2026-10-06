package com.telefam.company.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TelefamPaperPlaneLogo(
    size: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = Color(0x66C80815),
                ambientColor = Color(0x33C80815)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE61927),
                        Color(0xFFC80815),
                        Color(0xFFA6030E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .semantics { contentDescription = "Telefam Red Paper Plane Logo" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.56f)) {
            val w = this.size.width
            val h = this.size.height

            // Iconic Telegram-style paper plane geometry in crisp white
            // Main outer wing polygon
            val mainWingPath = Path().apply {
                moveTo(w * 0.95f, h * 0.10f) // Top-right nose tip
                lineTo(w * 0.06f, h * 0.46f) // Far left wingtip
                lineTo(w * 0.36f, h * 0.59f) // Center-left fold
                lineTo(w * 0.84f, h * 0.21f) // Inner crease toward nose
                lineTo(w * 0.47f, h * 0.66f) // Lower center crease
                lineTo(w * 0.76f, h * 0.89f) // Bottom-right wingtip
                close()
            }

            // Under-fold tail triangle
            val tailFoldPath = Path().apply {
                moveTo(w * 0.36f, h * 0.59f)
                lineTo(w * 0.43f, h * 0.86f)
                lineTo(w * 0.55f, h * 0.72f)
                lineTo(w * 0.47f, h * 0.66f)
                close()
            }

            drawPath(
                path = tailFoldPath,
                color = Color(0xFFFFD6D9),
                style = Fill
            )

            drawPath(
                path = mainWingPath,
                color = Color.White,
                style = Fill
            )
        }
    }
}

@Composable
fun GoogleBrandIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Google Logo" }
    ) {
        val s = this.size.minDimension
        val stroke = s * 0.19f
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(s - stroke, s - stroke)
        val topLeft = Offset(inset, inset)

        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -145f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        // Yellow left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        // Blue right arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -12f,
            sweepAngle = 57f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        // Horizontal blue crossbar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(s * 0.52f, s * 0.5f),
            end = Offset(s * 0.94f, s * 0.5f),
            strokeWidth = stroke * 0.92f,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
fun TelefamTopBottomWaveBackdrop(
    isDark: Boolean,
    brandRed: Color,
    modifier: Modifier = Modifier
) {
    val topWaveTint = if (isDark) {
        Color(0xFF281215)
    } else {
        Color(0xFFF9EAEB)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top soft pink/crimson curve matching Reference Image 1
        val topPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.11f)
            cubicTo(
                w * 0.78f, h * 0.03f,
                w * 0.25f, h * 0.04f,
                0f, h * 0.07f
            )
            close()
        }
        drawPath(path = topPath, color = topWaveTint)

        // Bottom bold crimson wave matching Reference Image 1
        val bottomWavePath = Path().apply {
            moveTo(0f, h * 0.80f)
            cubicTo(
                w * 0.22f, h * 0.90f,
                w * 0.52f, h * 0.81f,
                w * 0.78f, h * 0.83f
            )
            cubicTo(
                w * 0.90f, h * 0.84f,
                w * 0.96f, h * 0.87f,
                w, h * 0.85f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = bottomWavePath, color = brandRed)
    }
}
