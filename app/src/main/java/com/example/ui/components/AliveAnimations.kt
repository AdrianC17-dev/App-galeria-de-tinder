package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Achievement
import com.example.ui.theme.DiscardCrimson
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.UndoAmber
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Floating ambient particles & breathing aurora background that makes the app feel alive.
 */
@Composable
fun AliveAmbientBackground(
    dragProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alive_bg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val keepAlpha = (dragProgress.coerceAtLeast(0f) * 0.22f) + 0.09f
        val discardAlpha = ((-dragProgress).coerceAtLeast(0f) * 0.22f) + 0.07f

        val rightCenter = Offset(
            x = w * (0.82f + 0.06f * cos(phase)),
            y = h * (0.28f + 0.05f * sin(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(KeepEmerald.copy(alpha = keepAlpha), Color.Transparent),
                center = rightCenter,
                radius = w * 0.72f * pulse
            ),
            center = rightCenter,
            radius = w * 0.72f * pulse
        )

        val leftCenter = Offset(
            x = w * (0.18f + 0.06f * sin(phase)),
            y = h * (0.62f + 0.05f * cos(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DiscardCrimson.copy(alpha = discardAlpha), Color.Transparent),
                center = leftCenter,
                radius = w * 0.68f * pulse
            ),
            center = leftCenter,
            radius = w * 0.68f * pulse
        )

        val cyanColor = Color(0xFF00E5FF)
        val violetColor = Color(0xFFA855F7)
        val sparkleSeeds = listOf(
            Triple(0.10f, 0.12f, KeepEmerald),
            Triple(0.22f, 0.08f, cyanColor),
            Triple(0.38f, 0.16f, Color.White),
            Triple(0.55f, 0.10f, UndoAmber),
            Triple(0.74f, 0.14f, KeepEmerald),
            Triple(0.88f, 0.11f, cyanColor),
            Triple(0.08f, 0.29f, DiscardCrimson),
            Triple(0.19f, 0.34f, KeepEmerald),
            Triple(0.33f, 0.27f, violetColor),
            Triple(0.67f, 0.31f, UndoAmber),
            Triple(0.83f, 0.26f, Color.White),
            Triple(0.92f, 0.36f, KeepEmerald),
            Triple(0.12f, 0.48f, cyanColor),
            Triple(0.26f, 0.54f, UndoAmber),
            Triple(0.48f, 0.44f, KeepEmerald),
            Triple(0.76f, 0.52f, violetColor),
            Triple(0.90f, 0.49f, DiscardCrimson),
            Triple(0.09f, 0.68f, KeepEmerald),
            Triple(0.24f, 0.74f, Color.White),
            Triple(0.42f, 0.69f, cyanColor),
            Triple(0.62f, 0.73f, KeepEmerald),
            Triple(0.80f, 0.67f, UndoAmber),
            Triple(0.91f, 0.76f, cyanColor),
            Triple(0.15f, 0.88f, violetColor),
            Triple(0.36f, 0.86f, KeepEmerald),
            Triple(0.58f, 0.90f, Color.White),
            Triple(0.79f, 0.87f, UndoAmber),
            Triple(0.90f, 0.92f, KeepEmerald)
        )
        sparkleSeeds.forEachIndexed { idx, (bx, by, color) ->
            val driftRadiusX = 16f + (idx % 4) * 7f
            val driftRadiusY = 18f + (idx % 5) * 6f
            val speedFactor = if (idx % 2 == 0) 1f else -1f
            val offsetX = w * bx + driftRadiusX * cos((phase * speedFactor) + idx * 0.95f)
            val offsetY = h * by + driftRadiusY * sin(phase + idx * 1.25f)
            val baseRadius = (2.4f + (idx % 4) * 1.35f) * pulse
            val twinkleAlpha = (0.20f + 0.14f * ((sin(phase * 2f + idx * 0.8f) + 1f) * 0.5f))

            // Soft outer halo glow
            drawCircle(
                color = color.copy(alpha = twinkleAlpha * 0.35f),
                radius = baseRadius * 2.3f,
                center = Offset(offsetX, offsetY)
            )
            // Core luminous particle
            drawCircle(
                color = color.copy(alpha = twinkleAlpha),
                radius = baseRadius,
                center = Offset(offsetX, offsetY)
            )
        }
    }
}

/**
 * Compact, non-invasive slide-in/slide-out achievement pill that slides out from the right edge,
 * shows the unlocked achievement briefly (~1.9s), and slides right back in without obstructing swiping.
 */
@Composable
fun AchievementIncentivePopup(
    achievement: Achievement?,
    queuedMb: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(achievement?.id) {
        if (achievement != null) {
            delay(1950)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = achievement != null,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth + 40 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(170)),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth + 40 },
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(150)),
        modifier = modifier
    ) {
        if (achievement != null) {
            Surface(
                shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                color = ObsidianCard.copy(alpha = 0.94f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(UndoAmber, KeepEmerald)
                    )
                ),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .widthIn(max = 248.dp)
                    .clickable(onClick = onDismiss)
                    .testTag("achievement_popup")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(UndoAmber.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = UndoAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = achievement.badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = UndoAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ultra-compact lateral streak multiplier badge (x1, x2, x3...) that sits discreetly on the side edge
 * during swiping so streak feedback is immediate, alive, and completely non-invasive.
 */
@Composable
fun StreakMultiplierSideBadge(
    streakCount: Int,
    isDeleteStreak: Boolean,
    modifier: Modifier = Modifier
) {
    val popScale = remember { Animatable(1f) }

    LaunchedEffect(streakCount) {
        if (streakCount >= 1) {
            popScale.snapTo(1.26f)
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val accentColor = when {
        streakCount >= 10 -> UndoAmber
        isDeleteStreak -> DiscardCrimson
        else -> KeepEmerald
    }

    AnimatedVisibility(
        visible = streakCount >= 1,
        enter = slideInHorizontally(
            initialOffsetX = { it + 20 },
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180)),
        exit = slideOutHorizontally(
            targetOffsetX = { it + 20 },
            animationSpec = tween(160)
        ) + fadeOut(tween(140)) + scaleOut(targetScale = 0.85f, animationSpec = tween(150)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 10.dp, bottomEnd = 10.dp),
            color = ObsidianCard.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                }
                .testTag("streak_multiplier_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = if (streakCount >= 5) Icons.Default.LocalFireDepartment else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "x$streakCount",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Dynamic, interruptible opening animation when launching the app: fanning cards spring out with
 * hardware-accelerated transform & opacity while the main interface scales smoothly into place.
 */
@Composable
fun AppDynamicLaunchAnimation(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fanProgress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            fanProgress.snapTo(0f)
            fanProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            delay(360)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(240, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 1.08f, animationSpec = tween(240, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07090E).copy(alpha = 0.94f))
                .clickable(onClick = onDismiss)
                .testTag("app_dynamic_launch_overlay"),
            contentAlignment = Alignment.Center
        ) {
            val p = fanProgress.value

            // Left fanning card (Discard Crimson accent)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianCard,
                border = BorderStroke(1.5.dp, DiscardCrimson.copy(alpha = 0.75f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .size(width = 112.dp, height = 148.dp)
                    .graphicsLayer {
                        translationX = -68f * p
                        translationY = 10f * p
                        rotationZ = -14f * p
                        scaleX = 0.72f + (0.22f * p)
                        scaleY = 0.72f + (0.22f * p)
                        alpha = (p * 0.9f).coerceIn(0f, 1f)
                    }
            ) {}

            // Right fanning card (Keep Emerald accent)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianCard,
                border = BorderStroke(1.5.dp, KeepEmerald.copy(alpha = 0.75f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .size(width = 112.dp, height = 148.dp)
                    .graphicsLayer {
                        translationX = 68f * p
                        translationY = 10f * p
                        rotationZ = 14f * p
                        scaleX = 0.72f + (0.22f * p)
                        scaleY = 0.72f + (0.22f * p)
                        alpha = (p * 0.9f).coerceIn(0f, 1f)
                    }
            ) {}

            // Center Hero Card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = ObsidianCard,
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(KeepEmerald, UndoAmber, DiscardCrimson))
                ),
                shadowElevation = 20.dp,
                modifier = Modifier
                    .size(width = 124.dp, height = 162.dp)
                    .graphicsLayer {
                        scaleX = 0.65f + (0.35f * p)
                        scaleY = 0.65f + (0.35f * p)
                        alpha = p.coerceIn(0f, 1f)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(KeepEmerald.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = KeepEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "SwipeFoto",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}
