package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InsightsSummary
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.DeadlineOrange
import com.example.ui.theme.LavenderPurple
import com.example.ui.theme.NeonLime
import com.example.ui.theme.OffWhiteSurface
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.parseHexColor

@Composable
fun InsightsScreen(
    insights: InsightsSummary,
    modifier: Modifier = Modifier
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    var hasRevealedCharts by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasRevealedCharts = true
    }

    val animToday by animateIntAsState(
        targetValue = insights.completedToday,
        animationSpec = if (reducedMotion) snap() else tween(400, easing = FastOutSlowInEasing),
        label = "insights_today"
    )
    val animWeek by animateIntAsState(
        targetValue = insights.completedThisWeek,
        animationSpec = if (reducedMotion) snap() else tween(400, easing = FastOutSlowInEasing),
        label = "insights_week"
    )
    val animStreak by animateIntAsState(
        targetValue = insights.currentDailyStreak,
        animationSpec = if (reducedMotion) snap() else tween(400, easing = FastOutSlowInEasing),
        label = "insights_streak"
    )
    val animRate by animateIntAsState(
        targetValue = insights.overallCompletionRatePercent,
        animationSpec = if (reducedMotion) snap() else tween(450, easing = FastOutSlowInEasing),
        label = "insights_rate"
    )
    val animWeeklyProgress by animateFloatAsState(
        targetValue = if (hasRevealedCharts || reducedMotion) insights.weeklyProgressFraction else 0f,
        animationSpec = if (reducedMotion) snap() else tween(DayformMotion.CHART_REVEAL_MS, easing = FastOutSlowInEasing),
        label = "insights_weekly_progress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 20.dp)
            .testTag("insights_screen"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real-time productivity metrics from your local workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        if (insights.totalTasksAllTime == 0) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.dp))
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(30.dp))
                        .padding(32.dp)
                        .testTag("insights_empty_state"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(LavenderPurple.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoGraph,
                            contentDescription = null,
                            tint = LavenderPurple,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No task history yet",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete your first tasks to unlock daily streaks, 7-day activity bars, and category breakdowns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@LazyColumn
        }

        // 1. Top Hero Metrics Row: Completed Today (Lime) & Completed This Week (Lavender)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(NeonLime)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = animToday.toString(),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 44.sp
                            ),
                            color = PrimaryDarkText,
                            modifier = Modifier.testTag("insights_completed_today")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Completed\nToday",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = PrimaryDarkText.copy(alpha = 0.85f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(LavenderPurple)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = animWeek.toString(),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 44.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("insights_completed_week")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Completed\nThis Week",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }

        // 2. Streak & Completion Rate Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Daily Completion Streak Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(26.dp))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DeadlineOrange.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = DeadlineOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Streak",
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryText
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$animStreak ${if (animStreak == 1) "day" else "days"}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("insights_streak_value")
                    )
                }

                // Overall Completion Rate Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(26.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Completion Rate",
                        style = MaterialTheme.typography.labelLarge,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "$animRate%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = NeonLime,
                        modifier = Modifier.testTag("insights_completion_rate")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${insights.totalCompletedAllTime} of ${insights.totalTasksAllTime} total tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        }

        // 3. Weekly Completion Progress Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(26.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weekly Completion Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "${(animWeeklyProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = NeonLime
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${insights.completedThisWeek} completed out of ${insights.plannedThisWeek.coerceAtLeast(insights.completedThisWeek)} tasks in the last 7 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animWeeklyProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = NeonLime,
                    trackColor = CharcoalElevated,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // 4. Seven-Day Activity Visualization
        item {
            val maxDaily = insights.sevenDayActivity
                .maxOfOrNull { maxOf(it.completedCount, it.plannedCount, 1) }
                ?.coerceAtLeast(1) ?: 1

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "7-Day Activity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tasks completed each day over the past week",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    insights.sevenDayActivity.forEachIndexed { index, stat ->
                        val targetRatio = if (stat.completedCount == 0) {
                            0.08f
                        } else {
                            (stat.completedCount.toFloat() / maxDaily.toFloat()).coerceIn(0.08f, 1f)
                        }
                        val animatedBarRatio by animateFloatAsState(
                            targetValue = if (hasRevealedCharts || reducedMotion) targetRatio else 0.08f,
                            animationSpec = if (reducedMotion) {
                                snap()
                            } else {
                                tween(
                                    durationMillis = DayformMotion.CHART_REVEAL_MS,
                                    delayMillis = index * 30,
                                    easing = FastOutSlowInEasing
                                )
                            },
                            label = "activity_bar_${stat.dayShortLabel}"
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = stat.completedCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (stat.completedCount > 0) Color.White else SecondaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(82.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(CharcoalElevated),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animatedBarRatio)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            when {
                                                stat.isToday -> NeonLime
                                                stat.completedCount > 0 -> LavenderPurple
                                                else -> CharcoalBorder
                                            }
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stat.dayShortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (stat.isToday) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (stat.isToday) NeonLime else SecondaryText
                            )
                        }
                    }
                }
            }
        }

        // 5. Tasks by Category Breakdown (Off-White Card echoing reference contrast)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(OffWhiteSurface)
                    .padding(22.dp)
            ) {
                Text(
                    text = "Tasks by Category",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = PrimaryDarkText
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    insights.categoryBreakdown.forEach { catStat ->
                        val catFraction = if (catStat.totalCount > 0) {
                            (catStat.completedCount.toFloat() / catStat.totalCount.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val animatedCatFraction by animateFloatAsState(
                            targetValue = if (hasRevealedCharts || reducedMotion) catFraction else 0f,
                            animationSpec = if (reducedMotion) {
                                snap()
                            } else {
                                tween(DayformMotion.CHART_REVEAL_MS, easing = FastOutSlowInEasing)
                            },
                            label = "cat_progress_${catStat.categoryName}"
                        )
                        val accentColor = parseHexColor(catStat.colorHex)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                            .border(1.dp, PrimaryDarkText.copy(alpha = 0.25f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = catStat.categoryName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = PrimaryDarkText
                                    )
                                }

                                Text(
                                    text = "${catStat.completedCount}/${catStat.totalCount} completed",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = SecondaryDarkText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedCatFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = PrimaryDarkText,
                                trackColor = PrimaryDarkText.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
