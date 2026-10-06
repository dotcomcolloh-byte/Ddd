package com.telefam.company.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import com.telefam.company.app.ui.TelefamComViewModel
import com.telefam.company.app.ui.TelefamScreenDestination
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.GoogleBrandIcon
import com.telefam.company.app.ui.components.TelefamPaperPlaneLogo
import com.telefam.company.app.ui.components.TelefamTopBottomWaveBackdrop
import com.telefam.company.app.ui.theme.LocalTelefamColors

@Composable
fun TelefamComAuthScreen(
    uiState: TelefamUiState,
    onEmailChanged: (String) -> Unit,
    onSendOtpClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onOtpDigitsChanged: (String) -> Unit,
    onVerifyOtpClicked: () -> Unit,
    onResendOtpClicked: () -> Unit,
    onChangeEmailClicked: () -> Unit,
    onOpenEmailClientWithOtp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telefamColors = LocalTelefamColors.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(telefamColors.pageBackground)
    ) {
        // Exact Top soft blush curve & Bottom crimson wave matching Reference Image 1
        TelefamTopBottomWaveBackdrop(
            isDark = telefamColors.isDark,
            brandRed = telefamColors.brandRed
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(42.dp))

            // Red Telegram-style Paper Plane Logo
            TelefamPaperPlaneLogo(size = 86.dp)

            Spacer(modifier = Modifier.height(14.dp))

            // "Telefam" Title in bold crimson
            Text(
                text = stringResource(R.string.app_name),
                color = telefamColors.brandRed,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // "Stay connected. Stay together."
            Text(
                text = stringResource(R.string.app_tagline),
                color = telefamColors.textSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Elevated Card matching Reference Image 1 left & right exactly
            Surface(
                color = telefamColors.cardBackground,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 12.dp,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
            ) {
                AnimatedContent(
                    targetState = uiState.currentScreen,
                    transitionSpec = {
                        if (targetState == TelefamScreenDestination.AUTH_OTP) {
                            (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 2 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith
                                (slideOutHorizontally { it / 2 } + fadeOut())
                        }
                    },
                    label = "auth_card_step"
                ) { screen ->
                    if (screen == TelefamScreenDestination.AUTH_OTP) {
                        EnterOtpCardContent(
                            email = uiState.emailInput,
                            otpDigits = uiState.otpDigits,
                            otpError = uiState.otpError,
                            isVerifying = uiState.isVerifyingOtp,
                            countdownSeconds = uiState.countdownSeconds,
                            onOtpDigitsChanged = onOtpDigitsChanged,
                            onVerifyOtpClicked = onVerifyOtpClicked,
                            onResendOtpClicked = onResendOtpClicked,
                            onChangeEmailClicked = onChangeEmailClicked
                        )
                    } else {
                        EnterEmailCardContent(
                            emailInput = uiState.emailInput,
                            emailError = uiState.emailError,
                            isSendingOtp = uiState.isSendingOtp,
                            onEmailChanged = onEmailChanged,
                            onSendOtpClicked = onSendOtpClicked,
                            onGoogleSignInClicked = onGoogleSignInClicked
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun EnterEmailCardContent(
    emailInput: String,
    emailError: String?,
    isSendingOtp: Boolean,
    onEmailChanged: (String) -> Unit,
    onSendOtpClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.enter_your_email),
            color = telefamColors.brandRed,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.enter_email_subtitle),
            color = telefamColors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.email_label),
            color = telefamColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = emailInput,
            onValueChange = onEmailChanged,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.email_placeholder),
                    color = telefamColors.textSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "Email Icon",
                    tint = telefamColors.brandRed,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onSendOtpClicked()
                }
            ),
            isError = emailError != null,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = telefamColors.inputBackground,
                unfocusedContainerColor = telefamColors.inputBackground,
                focusedBorderColor = telefamColors.brandRed,
                unfocusedBorderColor = telefamColors.inputBorder,
                focusedTextColor = telefamColors.textPrimary,
                unfocusedTextColor = telefamColors.textPrimary,
                cursorColor = telefamColors.brandRed
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input_field")
        )

        if (emailError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = emailError,
                color = Color(0xFFD32F2F),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                onSendOtpClicked()
            },
            enabled = !isSendingOtp,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = telefamColors.brandRed,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("send_otp_button")
        ) {
            if (isSendingOtp) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.send_otp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // "or" horizontal divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(
                color = telefamColors.inputBorder,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.or_divider),
                color = telefamColors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(
                color = telefamColors.inputBorder,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // "Continue with Google" button matching Reference Image 1
        OutlinedButton(
            onClick = onGoogleSignInClicked,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, telefamColors.inputBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = telefamColors.inputBackground,
                contentColor = telefamColors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("continue_with_google_button")
        ) {
            GoogleBrandIcon(size = 20.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.continue_with_google),
                color = telefamColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EnterOtpCardContent(
    email: String,
    otpDigits: String,
    otpError: String?,
    isVerifying: Boolean,
    countdownSeconds: Int,
    onOtpDigitsChanged: (String) -> Unit,
    onVerifyOtpClicked: () -> Unit,
    onResendOtpClicked: () -> Unit,
    onChangeEmailClicked: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.enter_otp),
            color = telefamColors.brandRed,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.we_sent_verification_code),
            color = telefamColors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = email,
            color = telefamColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        // 6 Rounded OTP Boxes with dot placeholders matching Reference Image 1 (right)
        BasicTextField(
            value = otpDigits,
            onValueChange = { value ->
                onOtpDigitsChanged(value)
                if (value.filter { it.isDigit() }.length == 6) {
                    keyboardController?.hide()
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onVerifyOtpClicked()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag("otp_code_input"),
            decorationBox = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (index in 0 until 6) {
                        val char = otpDigits.getOrNull(index)
                        val isFocusedBox = otpDigits.length == index
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(telefamColors.inputBackground)
                                .border(
                                    width = if (isFocusedBox) 1.8.dp else 1.dp,
                                    color = if (isFocusedBox) telefamColors.brandRed else telefamColors.inputBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            if (char != null) {
                                Text(
                                    text = char.toString(),
                                    color = telefamColors.textPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(telefamColors.textSecondary.copy(alpha = 0.65f))
                                )
                            }
                        }
                    }
                }
            }
        )

        if (otpError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = otpError,
                color = Color(0xFFD32F2F),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                onVerifyOtpClicked()
            },
            enabled = !isVerifying,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = telefamColors.brandRed,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("verify_otp_button")
        ) {
            if (isVerifying) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.verify_otp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val formattedTime = TelefamComViewModel.formatTimer(countdownSeconds)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = telefamColors.textSecondary)) {
                    append("Resend code in ")
                }
                withStyle(SpanStyle(color = telefamColors.brandRed, fontWeight = FontWeight.Bold)) {
                    append(formattedTime)
                }
            },
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "After 00:30 you can ",
                color = telefamColors.textSecondary,
                fontSize = 12.sp
            )
            Text(
                text = stringResource(R.string.resend_code),
                color = if (countdownSeconds == 0) {
                    telefamColors.brandRed
                } else {
                    telefamColors.brandRed.copy(alpha = 0.55f)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = countdownSeconds == 0) { onResendOtpClicked() }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("resend_otp_button")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onChangeEmailClicked,
            modifier = Modifier.testTag("change_email_button")
        ) {
            Text(
                text = stringResource(R.string.change_email),
                color = telefamColors.brandRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
