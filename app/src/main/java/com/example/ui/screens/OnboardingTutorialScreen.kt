package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SwipeDecision
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiscardCrimson
import com.example.ui.theme.DiscardCrimsonContainer
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.KeepEmeraldContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UndoAmber
import com.example.ui.theme.VideoViolet
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class TutorialStepCard(
    val stepIndex: Int,
    val badgeText: String,
    val title: String,
    val subtitle: String,
    val cardTitle: String,
    val cardMeta: String,
    val cardSizeLabel: String,
    val expectedDecision: SwipeDecision?, // null means either direction works
    val accentColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
    val hintText: String
)

private val TUTORIAL_STEPS = listOf(
    TutorialStepCard(
        stepIndex = 0,
        badgeText = "PASO 1 DE 3 · CONSERVAR",
        title = "Desliza a la derecha para quedarte tus favoritos",
        subtitle = "Arrastra la tarjeta hacia la derecha (→) para conservar tus mejores fotos y videos.",
        cardTitle = "Recuerdo Especial HD",
        cardMeta = "Cámara · Foto nítida",
        cardSizeLabel = "3.4 MB",
        expectedDecision = SwipeDecision.KEEP,
        accentColor = KeepEmerald,
        secondaryColor = CyanAccent,
        icon = Icons.Default.Favorite,
        hintText = "Desliza la tarjeta hacia la DERECHA →"
    ),
    TutorialStepCard(
        stepIndex = 1,
        badgeText = "PASO 2 DE 3 · LIBERAR ESPACIO",
        title = "Desliza a la izquierda para enviar a la cola de borrado",
        subtitle = "Arrastra hacia la izquierda (←) capturas repetidas, videos borrosos o memes que ya no quieras.",
        cardTitle = "Captura_Repetida_04.png",
        cardMeta = "Screenshots · Archivo duplicado",
        cardSizeLabel = "6.8 MB",
        expectedDecision = SwipeDecision.DISCARD,
        accentColor = DiscardCrimson,
        secondaryColor = UndoAmber,
        icon = Icons.Default.DeleteSweep,
        hintText = "← Desliza la tarjeta hacia la IZQUIERDA"
    ),
    TutorialStepCard(
        stepIndex = 2,
        badgeText = "PASO 3 DE 3 · CONTROL TOTAL Y RACHAS",
        title = "¡Nada se borra sin tu confirmación final!",
        subtitle = "Al terminar podrás revisar cada archivo, salvar cualquiera con 1 toque y sumar rachas x1, x2, x3.",
        cardTitle = "Todo listo para organizar",
        cardMeta = "Desliza hacia cualquier lado para comenzar",
        cardSizeLabel = "+MB Libres",
        expectedDecision = null,
        accentColor = UndoAmber,
        secondaryColor = VideoViolet,
        icon = Icons.Default.RestoreFromTrash,
        hintText = "← Desliza a cualquier lado para empezar →"
    )
)

@Composable
fun OnboardingTutorialScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var feedbackNotice by remember { mutableStateOf<String?>(null) }
    val step = TUTORIAL_STEPS[currentStepIndex.coerceIn(0, TUTORIAL_STEPS.lastIndex)]

    val scope = rememberCoroutineScope()
    val offsetX = remember(currentStepIndex) { Animatable(0f) }
    val offsetY = remember(currentStepIndex) { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "tutorial_guide")
    val guideOffset by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "guide_offset"
    )

    fun advanceOrFinish() {
        feedbackNotice = null
        if (currentStepIndex < TUTORIAL_STEPS.lastIndex) {
            currentStepIndex += 1
        } else {
            onFinishOnboarding()
        }
    }

    Column(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp)
            .testTag("onboarding_tutorial_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header with Progress Dots & Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Swipe,
                            contentDescription = null,
                            tint = KeepEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Tutorial Interactivo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }

                // Step progress pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TUTORIAL_STEPS.forEachIndexed { idx, _ ->
                        val active = idx == currentStepIndex
                        val passed = idx < currentStepIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 22.dp else 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when {
                                        active -> step.accentColor
                                        passed -> KeepEmerald.copy(alpha = 0.6f)
                                        else -> ObsidianBorder
                                    }
                                )
                        )
                    }
                }
            }

            TextButton(
                onClick = onFinishOnboarding,
                modifier = Modifier.testTag("skip_onboarding_button")
            ) {
                Text(
                    text = "Saltar",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }
        }

        // Step Title & Explanation
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
            },
            label = "tutorial_step_header"
        ) { targetStep ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = targetStep.accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, targetStep.accentColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = targetStep.badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = targetStep.accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = targetStep.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = targetStep.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Interactive Practice Card Stack Area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val thresholdPx = widthPx * 0.26f

            val dragFraction = (offsetX.value / thresholdPx).coerceIn(-1f, 1f)
            val keepAlpha = dragFraction.coerceIn(0f, 1f)
            val discardAlpha = (-dragFraction).coerceIn(0f, 1f)
            val rotationDeg = (offsetX.value / widthPx) * 14f

            // Background ghost card for stack depth
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.6f)),
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxSize(0.86f)
                    .graphicsLayer {
                        scaleX = 0.93f + (abs(dragFraction) * 0.05f)
                        scaleY = 0.93f + (abs(dragFraction) * 0.05f)
                        translationY = 18.dp.toPx() * (1f - abs(dragFraction) * 0.5f)
                        alpha = 0.55f
                    }
            ) {}

            // Active Interactive Tutorial Card
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = when {
                        keepAlpha > 0.15f -> KeepEmerald
                        discardAlpha > 0.15f -> DiscardCrimson
                        else -> step.accentColor.copy(alpha = 0.65f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        rotationZ = rotationDeg
                    }
                    .pointerInput(currentStepIndex) {
                        detectDragGestures(
                            onDragEnd = {
                                val currentX = offsetX.value
                                val swipedDecision = when {
                                    currentX > thresholdPx -> SwipeDecision.KEEP
                                    currentX < -thresholdPx -> SwipeDecision.DISCARD
                                    else -> null
                                }

                                val expected = step.expectedDecision
                                if (swipedDecision != null && (expected == null || swipedDecision == expected)) {
                                    val targetX = if (swipedDecision == SwipeDecision.KEEP) widthPx * 1.35f else -widthPx * 1.35f
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = targetX,
                                            animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)
                                        )
                                        advanceOrFinish()
                                    }
                                } else {
                                    if (swipedDecision != null && expected != null && swipedDecision != expected) {
                                        feedbackNotice = if (expected == SwipeDecision.KEEP) {
                                            "En este paso desliza hacia la DERECHA (→) para conservar"
                                        } else {
                                            "En este paso desliza hacia la IZQUIERDA (←) para eliminar"
                                        }
                                    }
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    scope.launch {
                                        offsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, spring()) }
                                scope.launch { offsetY.animateTo(0f, spring()) }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y * 0.35f)
                                }
                            }
                        )
                    }
                    .testTag("tutorial_interactive_card")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Decorative gradient canvas inside the practice card
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    step.accentColor.copy(alpha = 0.25f),
                                    ObsidianCardElevated,
                                    step.secondaryColor.copy(alpha = 0.22f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(step.accentColor.copy(alpha = 0.30f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.42f),
                                radius = size.minDimension * 0.48f
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.42f),
                            radius = size.minDimension * 0.48f
                        )
                    }

                    // Top metadata bar inside card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = step.cardSizeLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = step.accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Mini streak badge preview on Step 3
                        if (currentStepIndex >= 1) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ObsidianCard.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = UndoAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "x${currentStepIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = UndoAmber
                                    )
                                }
                            }
                        }
                    }

                    // Center Animated Gesture Guide inside Card
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val animatedShift = when (step.expectedDecision) {
                            SwipeDecision.KEEP -> abs(guideOffset)
                            SwipeDecision.DISCARD -> -abs(guideOffset)
                            null -> guideOffset
                        }

                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .graphicsLayer {
                                    translationX = animatedShift
                                }
                                .clip(CircleShape)
                                .background(step.accentColor.copy(alpha = 0.2f))
                                .border(2.dp, step.accentColor.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.accentColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                    .graphicsLayer {
                                        translationX = animatedShift * 0.45f
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (step.expectedDecision == SwipeDecision.DISCARD || step.expectedDecision == null) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = if (step.expectedDecision == SwipeDecision.DISCARD) DiscardCrimson else UndoAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = step.hintText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (step.expectedDecision == SwipeDecision.KEEP || step.expectedDecision == null) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = if (step.expectedDecision == SwipeDecision.KEEP) KeepEmerald else UndoAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Progressive LIKE / QUEDAR Stamp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 56.dp, start = 18.dp)
                            .graphicsLayer {
                                alpha = keepAlpha
                                rotationZ = -12f
                                scaleX = 0.85f + (keepAlpha * 0.2f)
                                scaleY = 0.85f + (keepAlpha * 0.2f)
                            }
                            .border(3.dp, KeepEmerald, RoundedCornerShape(12.dp))
                            .background(KeepEmeraldContainer.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "QUEDAR",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = KeepEmerald
                        )
                    }

                    // Progressive NOPE / ELIMINAR Stamp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 56.dp, end = 18.dp)
                            .graphicsLayer {
                                alpha = discardAlpha
                                rotationZ = 12f
                                scaleX = 0.85f + (discardAlpha * 0.2f)
                                scaleY = 0.85f + (discardAlpha * 0.2f)
                            }
                            .border(3.dp, DiscardCrimson, RoundedCornerShape(12.dp))
                            .background(DiscardCrimsonContainer.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ELIMINAR",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = DiscardCrimson
                        )
                    }

                    // Bottom File Info inside Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = step.cardTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = step.cardMeta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Gentle feedback message if user swiped opposite direction on a guided step
        if (feedbackNotice != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = UndoAmber.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.5f)),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = feedbackNotice!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = UndoAmber,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom Interactive Controls (Tap alternative or Finish button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (step.expectedDecision) {
                SwipeDecision.KEEP -> {
                    Button(
                        onClick = {
                            scope.launch {
                                offsetX.animateTo(600f, tween(200))
                                advanceOrFinish()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeepEmerald,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("tutorial_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probar deslizar a la derecha (Conservar) →",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                SwipeDecision.DISCARD -> {
                    Button(
                        onClick = {
                            scope.launch {
                                offsetX.animateTo(-600f, tween(200))
                                advanceOrFinish()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DiscardCrimson,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("tutorial_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "← Probar deslizar a la izquierda (Eliminar)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                null -> {
                    Button(
                        onClick = onFinishOnboarding,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeepEmerald,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("finish_onboarding_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¡Entendido, empezar a limpiar!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
