package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.LavenderPurple
import com.example.ui.theme.NeonLime
import com.example.ui.theme.OffWhiteSurface
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryText

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: (displayName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasEntered = true
    }
    val entranceProgress by animateFloatAsState(
        targetValue = if (hasEntered || reducedMotion) 1f else 0f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(DayformMotion.SCREEN_NAV_MS, easing = FastOutSlowInEasing)
        },
        label = "onboarding_entrance"
    )

    var displayName by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .graphicsLayer {
                alpha = entranceProgress
                translationY = (1f - entranceProgress) * 18.dp.toPx()
            },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Brand Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonLime)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DAYFORM • DAILY TASK MANAGER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Shape your day with clarity.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    lineHeight = 44.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "An intentional, offline-first workspace for your daily priorities, creative projects, and deadlines.",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Visual preview cards echoing DAYFORM's Lavender & Lime identity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(LavenderPurple)
                        .padding(18.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryDarkText),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Track Streaks",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Build daily momentum",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(NeonLime)
                        .padding(18.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryDarkText),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = NeonLime,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Zero Clutter",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = PrimaryDarkText
                        )
                        Text(
                            text = "100% private & local",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryDarkText.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "What should we call you? (Optional)",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = {
                    Text(
                        text = "Enter your name (optional)",
                        color = SecondaryText
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CharcoalSurface,
                    unfocusedContainerColor = CharcoalSurface,
                    focusedBorderColor = NeonLime,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NeonLime
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_name_input")
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                onCompleteOnboarding(displayName.trim())
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonLime,
                contentColor = PrimaryDarkText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("onboarding_start_button")
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
