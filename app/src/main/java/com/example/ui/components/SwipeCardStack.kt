package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.DeviceMediaScanner
import com.example.model.PhotoItem
import com.example.model.SwipeDecision
import com.example.ui.theme.DiscardCrimson
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UndoAmber
import com.example.ui.theme.VideoViolet
import com.example.ui.theme.VideoVioletContainer
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sign

data class ProgrammaticSwipeRequest(
    val photoId: String,
    val decision: SwipeDecision,
    val requestId: Long = System.nanoTime()
)

@Composable
fun DeviceMediaThumbnail(
    item: PhotoItem,
    modifier: Modifier = Modifier,
    thumbSizePx: Int = 1440
) {
    val context = LocalContext.current

    // Only use MediaStore frame extraction for videos; for photos, decode full-resolution URI via Coil
    val videoFrameBitmap by produceState<Bitmap?>(initialValue = null, item.contentUri, item.isVideo) {
        value = if (item.isVideo) {
            DeviceMediaScanner.loadThumbnailBitmap(
                context = context,
                contentUriString = item.contentUri,
                widthPx = 1280,
                heightPx = 1280
            )
        } else {
            null
        }
    }

    val imageRequest = remember(item.contentUri, thumbSizePx) {
        ImageRequest.Builder(context)
            .data(Uri.parse(item.contentUri))
            .size(thumbSizePx.coerceAtLeast(1080))
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier.background(ObsidianSurface),
        contentAlignment = Alignment.Center
    ) {
        if (!item.isVideo) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        } else if (videoFrameBitmap != null) {
            Image(
                bitmap = videoFrameBitmap!!.asImageBitmap(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = VideoViolet,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun SwipeCardStack(
    photos: List<PhotoItem>,
    programmaticSwipeRequest: ProgrammaticSwipeRequest?,
    onSwiped: (SwipeDecision) -> Unit,
    onDragProgressChanged: (Float) -> Unit,
    onInspectPhoto: (PhotoItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visibleStack = remember(photos) { photos.take(3) }
    val topPhoto = visibleStack.firstOrNull() ?: return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("card_stack_container"),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val containerHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val thresholdPx = containerWidthPx * 0.30f
        val stackOffsetStepPx = with(density) { 20.dp.toPx() }

        val scope = rememberCoroutineScope()
        val offsetAnim = remember(topPhoto.id) {
            Animatable(Offset.Zero, Offset.VectorConverter)
        }
        var grabPivotSign by remember(topPhoto.id) { mutableFloatStateOf(1f) }

        val rawDragX = offsetAnim.value.x
        val signedProgress = (rawDragX / thresholdPx).coerceIn(-1f, 1f)
        val absProgress = abs(signedProgress).coerceIn(0f, 1f)

        LaunchedEffect(signedProgress) {
            onDragProgressChanged(signedProgress)
        }

        LaunchedEffect(programmaticSwipeRequest?.requestId, topPhoto.id) {
            if (programmaticSwipeRequest != null && programmaticSwipeRequest.photoId == topPhoto.id) {
                val targetX = if (programmaticSwipeRequest.decision == SwipeDecision.KEEP) {
                    containerWidthPx * 1.45f
                } else {
                    -containerWidthPx * 1.45f
                }
                offsetAnim.animateTo(
                    targetValue = Offset(targetX, -containerHeightPx * 0.04f),
                    animationSpec = tween(
                        durationMillis = 230,
                        easing = FastOutLinearInEasing
                    )
                )
                onDragProgressChanged(0f)
                onSwiped(programmaticSwipeRequest.decision)
            }
        }

        for (index in (visibleStack.size - 1) downTo 0) {
            val item = visibleStack[index]
            key(item.id) {
                val isTopCard = index == 0

                val baseScale = 1f - (index * 0.065f)
                val nextScale = 1f - ((index - 1).coerceAtLeast(0) * 0.065f)
                val interpolatedScale = if (isTopCard) {
                    1f
                } else {
                    baseScale + (nextScale - baseScale) * absProgress
                }

                val baseTranslationY = index * stackOffsetStepPx
                val nextTranslationY = (index - 1).coerceAtLeast(0) * stackOffsetStepPx
                val interpolatedTranslationY = if (isTopCard) {
                    offsetAnim.value.y
                } else {
                    baseTranslationY + (nextTranslationY - baseTranslationY) * absProgress
                }

                val cardAlpha = if (isTopCard) {
                    1f
                } else {
                    val baseAlpha = 1f - (index * 0.22f)
                    val nextAlpha = 1f - ((index - 1).coerceAtLeast(0) * 0.22f)
                    baseAlpha + (nextAlpha - baseAlpha) * absProgress
                }

                val rotationDegrees = if (isTopCard) {
                    ((offsetAnim.value.x / containerWidthPx) * 14f * grabPivotSign)
                        .coerceIn(-15f, 15f)
                } else {
                    0f
                }

                val gestureModifier = if (isTopCard) {
                    Modifier.pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                grabPivotSign = if (startOffset.y < size.height * 0.72f) 1f else 0.75f
                                scope.launch {
                                    offsetAnim.stop()
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetAnim.animateTo(
                                        targetValue = Offset.Zero,
                                        animationSpec = spring(
                                            dampingRatio = 0.62f,
                                            stiffness = 480f
                                        )
                                    )
                                }
                            },
                            onDragEnd = {
                                val currentX = offsetAnim.value.x
                                val currentY = offsetAnim.value.y
                                if (abs(currentX) >= thresholdPx) {
                                    val directionSign = sign(currentX)
                                    val decision = if (directionSign > 0f) {
                                        SwipeDecision.KEEP
                                    } else {
                                        SwipeDecision.DISCARD
                                    }
                                    scope.launch {
                                        offsetAnim.animateTo(
                                            targetValue = Offset(
                                                x = directionSign * containerWidthPx * 1.48f,
                                                y = currentY + (currentY * 0.25f)
                                            ),
                                            animationSpec = tween(
                                                durationMillis = 220,
                                                easing = FastOutLinearInEasing
                                            )
                                        )
                                        onDragProgressChanged(0f)
                                        onSwiped(decision)
                                    }
                                } else {
                                    scope.launch {
                                        offsetAnim.animateTo(
                                            targetValue = Offset.Zero,
                                            animationSpec = spring(
                                                dampingRatio = 0.60f,
                                                stiffness = 460f
                                            )
                                        )
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetAnim.snapTo(
                                        Offset(
                                            x = offsetAnim.value.x + dragAmount.x,
                                            y = offsetAnim.value.y + (dragAmount.y * 0.75f)
                                        )
                                    )
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }

                PhotoSwipeCard(
                    photo = item,
                    isTopCard = isTopCard,
                    keepStampProgress = if (isTopCard) signedProgress.coerceIn(0f, 1f) else 0f,
                    discardStampProgress = if (isTopCard) (-signedProgress).coerceIn(0f, 1f) else 0f,
                    onInspectPhoto = { onInspectPhoto(item) },
                    modifier = Modifier
                        .widthIn(max = 430.dp)
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .graphicsLayer {
                            translationX = if (isTopCard) offsetAnim.value.x else 0f
                            translationY = interpolatedTranslationY
                            rotationZ = rotationDegrees
                            scaleX = interpolatedScale
                            scaleY = interpolatedScale
                            alpha = cardAlpha
                        }
                        .then(gestureModifier)
                        .testTag(if (isTopCard) "active_swipe_card" else "stack_card_$index")
                )
            }
        }
    }
}

@Composable
private fun PhotoSwipeCard(
    photo: PhotoItem,
    isTopCard: Boolean,
    keepStampProgress: Float,
    discardStampProgress: Float,
    onInspectPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlayingVideo by remember(photo.id) { mutableStateOf(false) }

    val activeBorderColor = when {
        keepStampProgress > 0.08f -> KeepEmerald.copy(alpha = (keepStampProgress * 0.9f).coerceAtMost(0.95f))
        discardStampProgress > 0.08f -> DiscardCrimson.copy(alpha = (discardStampProgress * 0.9f).coerceAtMost(0.95f))
        photo.isVideo -> VideoViolet.copy(alpha = 0.65f)
        else -> ObsidianBorder
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.5.dp, activeBorderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isTopCard) 20.dp else 8.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Media Content: inline VideoView when playing, high-res thumbnail otherwise
            if (isTopCard && photo.isVideo && isPlayingVideo) {
                InlineVideoPlayer(
                    contentUri = photo.contentUri,
                    onCompletion = { isPlayingVideo = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DeviceMediaThumbnail(
                    item = photo,
                    thumbSizePx = 960,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isTopCard && (keepStampProgress > 0.01f || discardStampProgress > 0.01f)) {
                val tintColor = if (keepStampProgress > discardStampProgress) {
                    KeepEmerald.copy(alpha = keepStampProgress * 0.24f)
                } else {
                    DiscardCrimson.copy(alpha = discardStampProgress * 0.24f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tintColor)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.38f),
                            0.35f to Color.Transparent,
                            0.68f to Color.Black.copy(alpha = 0.60f),
                            1.0f to Color.Black.copy(alpha = 0.94f)
                        )
                    )
            )

            // Top folder badge, Video duration pill & File Size MB pill (Strictly single-line, no vertical stretching)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .widthIn(max = 190.dp)
                        .height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = KeepEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = photo.folderName,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (photo.isVideo) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = VideoVioletContainer.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, VideoViolet),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = photo.formattedDuration,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.68f),
                        border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.55f)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                tint = UndoAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f MB", photo.sizeMb),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Center Tap Zone to open photo in full size (or Play/Pause + Fullscreen for Video)
            if (isTopCard) {
                if (photo.isVideo) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.68f),
                            border = BorderStroke(2.dp, VideoViolet),
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { isPlayingVideo = !isPlayingVideo }
                                .testTag("video_play_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlayingVideo) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingVideo) "Pausar video" else "Reproducir video",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(onClick = onInspectPhoto)
                                .testTag("center_open_fullscreen_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Abrir en grande",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Generous interactive center zone: tapping the middle of the photo opens it full screen
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(0.62f)
                            .clickable(onClick = onInspectPhoto)
                            .testTag("center_open_fullscreen_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Ver foto en grande",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Toca para ampliar",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            if (isTopCard) {
                SwipeFeedbackStamp(
                    title = stringResource(R.string.stamp_keep),
                    subtitle = stringResource(R.string.stamp_keep_sub),
                    color = KeepEmerald,
                    icon = Icons.Default.Check,
                    progress = keepStampProgress,
                    rotationDegrees = -13f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 22.dp, top = 72.dp)
                        .testTag("stamp_keep")
                )

                SwipeFeedbackStamp(
                    title = stringResource(R.string.stamp_discard),
                    subtitle = stringResource(R.string.stamp_discard_sub),
                    color = DiscardCrimson,
                    icon = Icons.Default.Close,
                    progress = discardStampProgress,
                    rotationDegrees = 13f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 22.dp, top = 72.dp)
                        .testTag("stamp_discard")
                )
            }

            // Bottom file metadata
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${photo.fileName} · ${photo.dateTaken} · ${photo.resolution}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InlineVideoPlayer(
    contentUri: String,
    onCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(contentUri) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                videoViewRef = this
                setVideoURI(Uri.parse(contentUri))
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
                setOnCompletionListener { onCompletion() }
                setOnErrorListener { _, _, _ ->
                    onCompletion()
                    true
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SwipeFeedbackStamp(
    title: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: Float,
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val scale = 0.82f + (0.22f * clamped)

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = clamped
                scaleX = scale
                scaleY = scale
                rotationZ = rotationDegrees
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .border(
                width = 3.dp,
                color = color,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    ),
                    color = color,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.6.sp
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
