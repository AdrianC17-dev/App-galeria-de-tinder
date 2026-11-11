package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.data.DuplicateCluster
import com.example.data.DuplicateClusterType
import com.example.model.PhotoItem
import com.example.ui.components.DeviceMediaThumbnail
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiscardCrimson
import com.example.ui.theme.DiscardCrimsonContainer
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.KeepEmeraldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UndoAmber
import com.example.ui.theme.UndoAmberContainer
import com.example.ui.theme.VideoViolet
import com.example.viewmodel.SwipeGalleryUiState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Full-screen high-resolution photo & video inspector modal that opens when the user taps the center
 * of a photo card. Renders the native full-resolution image without pixelation and shows only a clean
 * return icon button to go back to normal mode.
 */
@Composable
fun FullscreenPhotoInspectorModal(
    photo: PhotoItem,
    onDismiss: () -> Unit,
    onKeepPhoto: (() -> Unit)? = null,
    onDiscardPhoto: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var zoomScale by remember(photo.id) { mutableFloatStateOf(1f) }
    var panOffset by remember(photo.id) { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = zoomScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "inspector_zoom"
    )

    val fullResImageRequest = remember(photo.contentUri) {
        ImageRequest.Builder(context)
            .data(Uri.parse(photo.contentUri))
            .size(Size.ORIGINAL)
            .crossfade(false)
            .build()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.97f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("fullscreen_photo_inspector")
        ) {
            // Zoomable / Pannable Full-Resolution Media Viewer (Tap anywhere to return to normal mode, no button)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(photo.id) {
                        detectTapGestures(
                            onTap = {
                                onDismiss()
                            },
                            onDoubleTap = {
                                if (zoomScale > 1.05f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 2.4f
                                }
                            }
                        )
                    }
                    .pointerInput(photo.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                            zoomScale = newScale
                            if (newScale > 1.02f) {
                                val maxPanX = (size.width * (newScale - 1f)) * 0.5f
                                val maxPanY = (size.height * (newScale - 1f)) * 0.5f
                                panOffset = Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                    y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                )
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photo.isVideo) {
                    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
                    DisposableEffect(photo.contentUri) {
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
                                setVideoURI(Uri.parse(photo.contentUri))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                                setOnClickListener {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    AsyncImage(
                        model = fullResImageRequest,
                        contentDescription = photo.title,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    )
                }
            }
        }
    }
}

/**
 * AI Cleanup / Suggest Duplicates screen where machine learning / perceptual clustering groups
 * duplicate, burst, and low-quality photos for fast bulk deletion.
 */
@Composable
fun AiCleanupDuplicatesScreen(
    uiState: SwipeGalleryUiState,
    onBackToFolders: () -> Unit,
    onToggleDuplicateItem: (String) -> Unit,
    onSelectAllSuggestedDuplicates: (Boolean) -> Unit,
    onApplyBulkDeleteToQueue: () -> Unit,
    onInspectPhotoFullscreen: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToFolders)

    val analysis = uiState.aiCleanupResult
    val selectedDuplicateIds = uiState.selectedAiDuplicateIds

    val allSuggestedPhotos = remember(analysis) {
        analysis?.clusters?.flatMap { it.suggestedToDelete } ?: emptyList()
    }
    val selectedPhotos = remember(allSuggestedPhotos, selectedDuplicateIds) {
        allSuggestedPhotos.filter { it.id in selectedDuplicateIds }
    }
    val selectedTotalMb = remember(selectedPhotos) {
        ((selectedPhotos.sumOf { it.sizeMb.toDouble() } * 100.0).roundToInt() / 100f)
    }
    val allSelected = allSuggestedPhotos.isNotEmpty() &&
        allSuggestedPhotos.all { it.id in selectedDuplicateIds }

    Column(
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp)
            .testTag("ai_cleanup_duplicates_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBackToFolders,
                    modifier = Modifier.testTag("ai_cleanup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a carpetas",
                        tint = Color.White
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI Cleanup · Duplicados",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Agrupación inteligente de fotos similares y borrosas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isRunningAiCleanup) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = CyanAccent)
                        Text(
                            text = "Analizando duplicados y nitidez...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Comparando huellas visuales, ráfagas y resolución en tus carpetas seleccionadas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (analysis != null) {
            // Short in-app notice when Firebase AI Logic is not enabled
            if (analysis.isPlaceholderAi) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = UndoAmberContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_ai_notice_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = UndoAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = analysis.firebaseRequiredNotice,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = UndoAmber,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Summary Card + Select All Suggested Bar
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = analysis.aiSummaryBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

                    if (allSuggestedPhotos.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedPhotos.size} de ${allSuggestedPhotos.size} marcados (${String.format(Locale.US, "%.2f MB", selectedTotalMb)})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = KeepEmerald
                            )

                            Surface(
                                onClick = { onSelectAllSuggestedDuplicates(!allSelected) },
                                shape = RoundedCornerShape(50),
                                color = if (allSelected) ObsidianCard else CyanAccent.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                                modifier = Modifier.testTag("ai_select_all_duplicates_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (allSelected) "Quitar selección" else "Marcar todos",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clusters List
            if (analysis.clusters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = KeepEmerald,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "¡No se detectaron duplicados ni fotos borrosas!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tus carpetas seleccionadas están limpias o tienen menos de 2 archivos para comparar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(analysis.clusters, key = { it.id }) { cluster ->
                        DuplicateClusterCard(
                            cluster = cluster,
                            selectedDuplicateIds = selectedDuplicateIds,
                            onToggleDuplicateItem = onToggleDuplicateItem,
                            onInspectPhotoFullscreen = onInspectPhotoFullscreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Bulk Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackToFolders,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Carpetas")
                }

                Button(
                    onClick = onApplyBulkDeleteToQueue,
                    enabled = selectedPhotos.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscardCrimson,
                        contentColor = Color.White,
                        disabledContainerColor = ObsidianCard,
                        disabledContentColor = TextSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("ai_bulk_delete_cta_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedPhotos.isNotEmpty()) {
                            String.format(
                                Locale.US,
                                "Revisar y borrar %d (%.2f MB)",
                                selectedPhotos.size,
                                selectedTotalMb
                            )
                        } else {
                            "Selecciona archivos a borrar"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateClusterCard(
    cluster: DuplicateCluster,
    selectedDuplicateIds: Set<String>,
    onToggleDuplicateItem: (String) -> Unit,
    onInspectPhotoFullscreen: (PhotoItem) -> Unit
) {
    val accentColor = when (cluster.clusterType) {
        DuplicateClusterType.DUPLICATE_OR_BURST -> CyanAccent
        DuplicateClusterType.LOW_QUALITY_OR_BLUR -> UndoAmber
        DuplicateClusterType.LARGE_REDUNDANT -> VideoViolet
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = cluster.clusterType.title.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cluster.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = cluster.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = DiscardCrimsonContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, DiscardCrimson.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = String.format(Locale.US, "-%.2f MB", cluster.reclaimableMb),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DiscardCrimson,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // AI Reason Tag
            Text(
                text = cluster.aiReasonLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = accentColor
            )

            // Horizontal Scroll of Best Photo (if any) + Suggested Duplicates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (cluster.bestPhotoToKeep != null) {
                    ClusterPhotoTile(
                        photo = cluster.bestPhotoToKeep,
                        isBestToKeep = true,
                        isSelectedForDelete = false,
                        onToggleSelect = {},
                        onInspectFullscreen = { onInspectPhotoFullscreen(cluster.bestPhotoToKeep) }
                    )
                }

                cluster.suggestedToDelete.forEach { dupPhoto ->
                    val isSelected = dupPhoto.id in selectedDuplicateIds
                    ClusterPhotoTile(
                        photo = dupPhoto,
                        isBestToKeep = false,
                        isSelectedForDelete = isSelected,
                        onToggleSelect = { onToggleDuplicateItem(dupPhoto.id) },
                        onInspectFullscreen = { onInspectPhotoFullscreen(dupPhoto) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClusterPhotoTile(
    photo: PhotoItem,
    isBestToKeep: Boolean,
    isSelectedForDelete: Boolean,
    onToggleSelect: () -> Unit,
    onInspectFullscreen: () -> Unit
) {
    val borderColor = when {
        isBestToKeep -> KeepEmerald
        isSelectedForDelete -> DiscardCrimson
        else -> ObsidianBorder
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(if (isBestToKeep || isSelectedForDelete) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .width(142.dp)
            .height(178.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Tapping the center opens fullscreen photo inspector
            DeviceMediaThumbnail(
                item = photo,
                thumbSizePx = 360,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onInspectFullscreen)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.45f),
                            0.5f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )

            // Center subtle zoom icon pill
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clickable(onClick = onInspectFullscreen)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = "Ver foto en grande",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Top Badge: either "MEJOR TOMA" or Selection Checkbox for deletion
            if (isBestToKeep) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = KeepEmerald,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Conservar",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else {
                Surface(
                    onClick = onToggleSelect,
                    shape = RoundedCornerShape(50),
                    color = if (isSelectedForDelete) DiscardCrimson else Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, if (isSelectedForDelete) DiscardCrimson else Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelectedForDelete) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isSelectedForDelete) "Borrar" else "Omitir",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Bottom File Name & Size MB
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SdStorage,
                        contentDescription = null,
                        tint = UndoAmber,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f MB", photo.sizeMb),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = UndoAmber
                    )
                }
            }
        }
    }
}
