package com.example.ui.theme

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Centralized motion tokens, easing curves, shared-element bounds transforms,
 * reusable spring/tween specs, and reduced-motion utilities for DAYFORM.
 *
 * Timing hierarchy:
 * - Micro-interactions (press, icon pop, chip tint): 120ms – 180ms
 * - Component state changes (checkbox, filter chip, inline expand): 180ms – 240ms
 * - Screen & tab transitions: 260ms – 360ms
 * - Shared element & modal sheet morphs: 320ms – 420ms
 */
object DayformMotion {
    const val MICRO_INTERACTION_MS = 140
    const val QUICK_FEEDBACK_MS = 150
    const val SMALL_STATE_MS = 210
    const val SUPPORTING_CONTENT_MS = 220
    const val TAB_NAV_MS = 280
    const val SCREEN_NAV_MS = 340
    const val CREATE_TASK_MORPH_MS = 380
    const val SHARED_BOUNDS_MORPH_MS = 420
    const val CHART_REVEAL_MS = 520

    // Material 3 emphasized & standard easing curves
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardDecelerate = LinearOutSlowInEasing
    val StandardAccelerate = FastOutLinearInEasing

    /**
     * Checks whether the user has enabled Reduced Motion / disabled animator duration scale
     * in Android system accessibility/developer settings.
     */
    @Composable
    fun rememberReducedMotion(): Boolean {
        val context = LocalContext.current
        return remember(context) {
            try {
                val scale = Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                )
                scale == 0f
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun <T> microTween(reducedMotion: Boolean): FiniteAnimationSpec<T> {
        return if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = QUICK_FEEDBACK_MS, easing = FastOutSlowInEasing)
        }
    }

    fun <T> stateTween(reducedMotion: Boolean): FiniteAnimationSpec<T> {
        return if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = SMALL_STATE_MS, easing = FastOutSlowInEasing)
        }
    }

    fun <T> microSpring(reducedMotion: Boolean): FiniteAnimationSpec<T> {
        return if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        }
    }

    fun <T> bouncyCheckSpring(reducedMotion: Boolean): FiniteAnimationSpec<T> {
        return if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    }

    fun itemPlacementSpec(reducedMotion: Boolean): FiniteAnimationSpec<IntOffset> {
        return if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        }
    }

    fun contentSizeSpec(reducedMotion: Boolean): FiniteAnimationSpec<IntSize> {
        return if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold
            )
        }
    }

    fun inlineBannerEnter(reducedMotion: Boolean): EnterTransition {
        if (reducedMotion) return EnterTransition.None
        return expandVertically(
            animationSpec = tween(SMALL_STATE_MS, easing = EmphasizedDecelerate),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(QUICK_FEEDBACK_MS))
    }

    fun inlineBannerExit(reducedMotion: Boolean): ExitTransition {
        if (reducedMotion) return ExitTransition.None
        return shrinkVertically(
            animationSpec = tween(QUICK_FEEDBACK_MS, easing = EmphasizedAccelerate),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(MICRO_INTERACTION_MS))
    }

    fun badgePopEnter(reducedMotion: Boolean): EnterTransition {
        if (reducedMotion) return EnterTransition.None
        return scaleIn(
            initialScale = 0.65f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(animationSpec = tween(MICRO_INTERACTION_MS))
    }

    fun badgePopExit(reducedMotion: Boolean): ExitTransition {
        if (reducedMotion) return ExitTransition.None
        return scaleOut(
            targetScale = 0.65f,
            animationSpec = tween(MICRO_INTERACTION_MS)
        ) + fadeOut(animationSpec = tween(MICRO_INTERACTION_MS))
    }

    fun <S : Comparable<S>> counterRollTransition(
        reducedMotion: Boolean
    ): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        if (reducedMotion) {
            EnterTransition.None togetherWith ExitTransition.None
        } else {
            val isIncreasing = targetState > initialState
            val direction = if (isIncreasing) 1 else -1
            (slideInVertically(
                animationSpec = tween(SMALL_STATE_MS, easing = EmphasizedDecelerate)
            ) { height -> direction * (height / 2) } +
                fadeIn(tween(QUICK_FEEDBACK_MS))) togetherWith
                (slideOutVertically(
                    animationSpec = tween(QUICK_FEEDBACK_MS, easing = EmphasizedAccelerate)
                ) { height -> -direction * (height / 2) } +
                    fadeOut(tween(MICRO_INTERACTION_MS))) using SizeTransform(clip = false)
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    fun taskCardBoundsTransform(reducedMotion: Boolean) =
        androidx.compose.animation.BoundsTransform { _: Rect, _: Rect ->
            if (reducedMotion) {
                snap()
            } else {
                tween(
                    durationMillis = SHARED_BOUNDS_MORPH_MS,
                    easing = EmphasizedDecelerate
                )
            }
        }

    @OptIn(ExperimentalSharedTransitionApi::class)
    fun createTaskBoundsTransform(reducedMotion: Boolean) =
        androidx.compose.animation.BoundsTransform { _: Rect, _: Rect ->
            if (reducedMotion) {
                snap()
            } else {
                tween(
                    durationMillis = CREATE_TASK_MORPH_MS,
                    easing = EmphasizedDecelerate
                )
            }
        }

    fun tabEnterTransition(forward: Boolean, reducedMotion: Boolean): EnterTransition {
        if (reducedMotion) return EnterTransition.None
        val offsetSign = if (forward) 1 else -1
        return fadeIn(
            animationSpec = tween(TAB_NAV_MS, easing = StandardDecelerate)
        ) + slideInHorizontally(
            animationSpec = tween(TAB_NAV_MS, easing = EmphasizedDecelerate),
            initialOffsetX = { fullWidth -> offsetSign * (fullWidth / 14) }
        )
    }

    fun tabExitTransition(forward: Boolean, reducedMotion: Boolean): ExitTransition {
        if (reducedMotion) return ExitTransition.None
        val offsetSign = if (forward) -1 else 1
        return fadeOut(
            animationSpec = tween(QUICK_FEEDBACK_MS, easing = StandardAccelerate)
        ) + slideOutHorizontally(
            animationSpec = tween(TAB_NAV_MS, easing = FastOutSlowInEasing),
            targetOffsetX = { fullWidth -> offsetSign * (fullWidth / 18) }
        )
    }

    fun detailsEnterTransition(reducedMotion: Boolean): EnterTransition {
        if (reducedMotion) return EnterTransition.None
        return fadeIn(
            animationSpec = tween(SCREEN_NAV_MS, easing = EmphasizedDecelerate)
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(SCREEN_NAV_MS, easing = EmphasizedDecelerate)
        )
    }

    fun detailsExitTransition(reducedMotion: Boolean): ExitTransition {
        if (reducedMotion) return ExitTransition.None
        return fadeOut(
            animationSpec = tween(SUPPORTING_CONTENT_MS, easing = EmphasizedAccelerate)
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = tween(SUPPORTING_CONTENT_MS, easing = EmphasizedAccelerate)
        )
    }

    fun sheetEnterTransition(reducedMotion: Boolean): EnterTransition {
        if (reducedMotion) return EnterTransition.None
        return slideInVertically(
            animationSpec = tween(CREATE_TASK_MORPH_MS, easing = EmphasizedDecelerate),
            initialOffsetY = { fullHeight -> fullHeight / 3 }
        ) + fadeIn(
            animationSpec = tween(SUPPORTING_CONTENT_MS, easing = StandardDecelerate)
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(CREATE_TASK_MORPH_MS, easing = EmphasizedDecelerate)
        )
    }

    fun sheetExitTransition(reducedMotion: Boolean): ExitTransition {
        if (reducedMotion) return ExitTransition.None
        return slideOutVertically(
            animationSpec = tween(TAB_NAV_MS, easing = EmphasizedAccelerate),
            targetOffsetY = { fullHeight -> fullHeight / 4 }
        ) + fadeOut(
            animationSpec = tween(QUICK_FEEDBACK_MS, easing = StandardAccelerate)
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(TAB_NAV_MS, easing = EmphasizedAccelerate)
        )
    }
}

/**
 * Tactile press-scale modifier that respects Android reduced-motion settings.
 * Uses graphicsLayer for GPU-friendly scale interpolation without triggering layout passes.
 */
fun Modifier.pressScaleClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) pressedScale else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
