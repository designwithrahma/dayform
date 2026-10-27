package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.LavenderPurple
import com.example.ui.theme.LightText
import com.example.ui.theme.NeonLime
import com.example.ui.theme.OffWhiteSurface
import com.example.ui.theme.PrimaryDarkText
import com.example.util.DateTimeUtil

@Composable
fun AsymmetricStatCards(
    completedCount: Int,
    plannedCount: Int,
    completedPlannedTodayCount: Int = 0,
    onCompletedCardClick: () -> Unit,
    onPlannedCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val datePillText = remember { DateTimeUtil.formatPlannedCardDatePill() }
    val progressFraction = if (plannedCount > 0) {
        (completedPlannedTodayCount.toFloat() / plannedCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = 420, easing = FastOutSlowInEasing)
        },
        label = "todayProgressFraction"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CompletedStatCard(
                completedCount = completedCount,
                reducedMotion = reducedMotion,
                onClick = onCompletedCardClick,
                modifier = Modifier.weight(1f)
            )

            PlannedStatCard(
                plannedCount = plannedCount,
                datePillText = datePillText,
                reducedMotion = reducedMotion,
                onClick = onPlannedCardClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Today's Progress Bar Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CharcoalSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedPlannedTodayCount of $plannedCount tasks completed today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightText,
                        modifier = Modifier.testTag("today_progress_text")
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonLime,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CharcoalElevated),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (animatedProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceIn(0.03f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(NeonLime)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 3.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(NeonLime)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedStatCard(
    completedCount: Int,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.96f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "completedCardScale"
    )

    Box(
        modifier = modifier
            .height(195.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(LavenderPurple)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "$completedCount tasks completed. Tap to view completed tasks."
            }
            .testTag("stat_card_completed")
    ) {
        // Decorative dashed concentric arcs in bottom-right corner
        Canvas(
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.BottomEnd)
        ) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f)
            val strokeColor = PrimaryDarkText.copy(alpha = 0.28f)
            drawArc(
                color = strokeColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
                size = Size(size.width * 1.4f, size.height * 1.4f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashEffect)
            )
            drawArc(
                color = strokeColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width * 0.38f, size.height * 0.38f),
                size = Size(size.width * 1.0f, size.height * 1.0f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashEffect)
            )
        }

        // Top-right cutout notch with dark circular arrow button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(CharcoalBg),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = LightText,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(-45f)
                )
            }
        }

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            AnimatedStatNumber(
                value = completedCount,
                textColor = LightText,
                reducedMotion = reducedMotion,
                testTag = "stat_completed_value",
                modifier = Modifier.align(Alignment.TopStart)
            )

            Text(
                text = stringResource(R.string.tasks_completed_label),
                style = MaterialTheme.typography.titleMedium,
                color = LightText,
                lineHeight = 20.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(PrimaryDarkText),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OffWhiteSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PlannedStatCard(
    plannedCount: Int,
    datePillText: String,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.96f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "plannedCardScale"
    )

    Box(
        modifier = modifier
            .height(195.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(NeonLime)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "$plannedCount tasks planned for today. Tap to view today's tasks."
            }
            .testTag("stat_card_planned")
    ) {
        // Top-right cutout notch with dark circular arrow button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(CharcoalBg),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = LightText,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(-45f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedStatNumber(
                value = plannedCount,
                textColor = PrimaryDarkText,
                reducedMotion = reducedMotion,
                testTag = "stat_planned_value"
            )

            Column {
                Text(
                    text = stringResource(R.string.tasks_planned_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryDarkText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = PrimaryDarkText,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = datePillText,
                        style = MaterialTheme.typography.labelMedium,
                        color = LightText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStatNumber(
    value: Int,
    textColor: Color,
    reducedMotion: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = DayformMotion.counterRollTransition(reducedMotion),
        label = testTag
    ) { targetCount ->
        Text(
            text = targetCount.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = textColor,
            modifier = Modifier.testTag(testTag)
        )
    }
}
