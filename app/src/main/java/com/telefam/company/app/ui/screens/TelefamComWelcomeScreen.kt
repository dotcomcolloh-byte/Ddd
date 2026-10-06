package com.telefam.company.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import com.telefam.company.app.ui.components.TelefamPaperPlaneLogo
import com.telefam.company.app.ui.components.TelefamTopBottomWaveBackdrop
import com.telefam.company.app.ui.theme.LocalTelefamColors
import kotlinx.coroutines.delay

@Composable
fun TelefamComWelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telefamColors = LocalTelefamColors.current
    var revealStage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(80)
        revealStage = 1
        delay(220)
        revealStage = 2
        delay(220)
        revealStage = 3
    }

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_pulse")
    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_1"
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_1"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plane_float"
    )

    val logoSpringScale by animateFloatAsState(
        targetValue = if (revealStage >= 1) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_spring_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(telefamColors.pageBackground)
    ) {
        TelefamTopBottomWaveBackdrop(
            isDark = telefamColors.isDark,
            brandRed = telefamColors.brandRed
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Red Telegram-style Paper Plane with concentric radar rings
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(144.dp)
                        .scale(logoSpringScale)
                        .offset(y = floatOffset.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = telefamColors.brandRed.copy(alpha = ringAlpha1),
                            radius = (size.minDimension / 2f) * ringScale1,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = telefamColors.brandRed.copy(alpha = (ringAlpha1 * 0.6f).coerceIn(0f, 1f)),
                            radius = (size.minDimension / 2f) * ((ringScale1 + 0.22f).coerceAtMost(1.6f)),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    TelefamPaperPlaneLogo(size = 96.dp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = revealStage >= 1,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 2 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = telefamColors.brandRed,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.8).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.app_tagline),
                            color = telefamColors.textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                AnimatedVisibility(
                    visible = revealStage >= 2,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
                ) {
                    Surface(
                        color = telefamColors.cardBackground,
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            WelcomeFeatureRow(
                                icon = Icons.Default.VerifiedUser,
                                title = "Passwordless Email OTP & Google",
                                subtitle = "Instant verification with built-in security rate limiting",
                                brandRed = telefamColors.brandRed,
                                brandTint = telefamColors.brandRedTint,
                                textPrimary = telefamColors.textPrimary,
                                textSecondary = telefamColors.textSecondary
                            )
                            WelcomeFeatureRow(
                                icon = Icons.Default.Groups,
                                title = "Connected Family & Friends",
                                subtitle = "Minimalist, fast messaging and community feeds",
                                brandRed = telefamColors.brandRed,
                                brandTint = telefamColors.brandRedTint,
                                textPrimary = telefamColors.textPrimary,
                                textSecondary = telefamColors.textSecondary
                            )
                            WelcomeFeatureRow(
                                icon = Icons.Default.Lock,
                                title = "Smart Identity & Profile Setup",
                                subtitle = "Auto-filled credentials, global country codes, and photo crop",
                                brandRed = telefamColors.brandRed,
                                brandTint = telefamColors.brandRedTint,
                                textPrimary = telefamColors.textPrimary,
                                textSecondary = telefamColors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                AnimatedVisibility(
                    visible = revealStage >= 3,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 2 }
                ) {
                    Button(
                        onClick = onGetStarted,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = telefamColors.brandRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("welcome_get_started_button")
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_get_started),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue to Login"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    brandRed: Color,
    brandTint: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(color = brandTint, shape = CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = brandRed,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
