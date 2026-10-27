package com.example.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.MainTab
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.NeonLime
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.pressScaleClickable

const val SHARED_CREATE_TASK_FAB_KEY = "shared_create_task_fab_container"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DayformBottomNavBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()

    val addButtonSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = SHARED_CREATE_TASK_FAB_KEY),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = DayformMotion.createTaskBoundsTransform(reducedMotion),
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(CircleShape)
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CharcoalBg,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home (Custom pentagon house icon matching reference image)
            HomeNavIconItem(
                selected = activeTab == MainTab.HOME,
                reducedMotion = reducedMotion,
                onClick = { onSelectTab(MainTab.HOME) },
                modifier = Modifier.testTag("nav_home")
            )

            // 2. Tasks / Search
            NavIconButton(
                selected = activeTab == MainTab.TASKS,
                reducedMotion = reducedMotion,
                contentDesc = "Tasks and Search",
                onClick = { onSelectTab(MainTab.TASKS) },
                modifier = Modifier.testTag("nav_tasks")
            ) { tint ->
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }

            // 3. Large Central Lime Add Button with shared-bounds morph into CreateTaskSheet
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .then(addButtonSharedModifier)
                    .clip(CircleShape)
                    .background(NeonLime)
                    .pressScaleClickable(
                        pressedScale = 0.91f,
                        onClick = onAddClick
                    )
                    .semantics { contentDescription = "Create new task" }
                    .testTag("nav_add_task_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PrimaryDarkText,
                    modifier = Modifier.size(30.dp)
                )
            }

            // 4. Insights (Rounded square chart icon matching reference image)
            NavIconButton(
                selected = activeTab == MainTab.INSIGHTS,
                reducedMotion = reducedMotion,
                contentDesc = "Insights",
                onClick = { onSelectTab(MainTab.INSIGHTS) },
                modifier = Modifier.testTag("nav_insights")
            ) { tint ->
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .border(
                            width = 1.8.dp,
                            color = tint,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // 5. Profile
            NavIconButton(
                selected = activeTab == MainTab.PROFILE,
                reducedMotion = reducedMotion,
                contentDesc = "Profile and Settings",
                onClick = { onSelectTab(MainTab.PROFILE) },
                modifier = Modifier.testTag("nav_profile")
            ) { tint ->
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeNavIconItem(
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "home_nav_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) CharcoalBorder.copy(alpha = 0.45f) else Color.Transparent,
        animationSpec = if (reducedMotion) snap() else tween(DayformMotion.SMALL_STATE_MS),
        label = "home_nav_bg"
    )

    Box(
        modifier = modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .pressScaleClickable(pressedScale = 0.92f, onClick = onClick)
            .semantics { contentDescription = "Home Dashboard" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val w = size.width
            val h = size.height
            val housePath = Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.92f, h * 0.42f)
                lineTo(w * 0.92f, h * 0.85f)
                quadraticTo(w * 0.92f, h * 0.94f, w * 0.80f, h * 0.94f)
                lineTo(w * 0.20f, h * 0.94f)
                quadraticTo(w * 0.08f, h * 0.94f, w * 0.08f, h * 0.85f)
                lineTo(w * 0.08f, h * 0.42f)
                close()
            }
            if (selected) {
                drawPath(
                    path = housePath,
                    color = NeonLime
                )
                drawRoundRect(
                    color = PrimaryDarkText,
                    topLeft = Offset(w * 0.34f, h * 0.64f),
                    size = Size(w * 0.32f, h * 0.11f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            } else {
                drawPath(
                    path = housePath,
                    color = SecondaryText,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
private fun NavIconButton(
    selected: Boolean,
    reducedMotion: Boolean,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable (Color) -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) NeonLime else Color.White.copy(alpha = 0.78f),
        animationSpec = if (reducedMotion) snap() else tween(DayformMotion.SMALL_STATE_MS),
        label = "nav_icon_tint"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) CharcoalBorder.copy(alpha = 0.55f) else Color.Transparent,
        animationSpec = if (reducedMotion) snap() else tween(DayformMotion.SMALL_STATE_MS),
        label = "nav_icon_bg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "nav_icon_scale"
    )

    Box(
        modifier = modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .pressScaleClickable(pressedScale = 0.92f, onClick = onClick)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        iconContent(tint)
    }
}
