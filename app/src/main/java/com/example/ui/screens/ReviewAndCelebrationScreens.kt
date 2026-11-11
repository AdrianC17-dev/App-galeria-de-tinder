package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PhotoItem
import com.example.model.SmartPhotoTag
import com.example.model.SwipeDecision
import com.example.ui.components.DeviceMediaThumbnail
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiscardCrimson
import com.example.ui.theme.DiscardCrimsonContainer
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.KeepEmeraldContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UndoAmber
import com.example.ui.theme.VideoViolet
import com.example.ui.theme.VideoVioletContainer
import com.example.viewmodel.SwipeGalleryUiState
import java.util.Locale

@Composable
fun PreDeletionReviewScreen(
    uiState: SwipeGalleryUiState,
    onSelectTab: (SwipeDecision) -> Unit,
    onRescuePhotoFromDeleteList: (PhotoItem) -> Unit,
    onMoveKeptToDeleteList: (PhotoItem) -> Unit,
    onResumeSwiping: () -> Unit,
    onBackToFolders: () -> Unit,
    onRequestConfirmDialog: (Boolean) -> Unit,
    onConfirmPermanentDeletion: () -> Unit,
    onInspectPhotoFullscreen: (PhotoItem) -> Unit = {},
    onSuggestDuplicates: () -> Unit = {},
    onSelectSmartTagFilter: (SmartPhotoTag) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (uiState.remainingPhotos.isNotEmpty()) {
            onResumeSwiping()
        } else {
            onBackToFolders()
        }
    }

    if (uiState.showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onRequestConfirmDialog(false) },
            containerColor = ObsidianSurface,
            iconContentColor = DiscardCrimson,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DiscardCrimsonContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = DiscardCrimson,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.confirm_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                val photoDelCount = uiState.discardedPhotos.count { !it.isVideo }
                val videoDelCount = uiState.discardedPhotos.count { it.isVideo }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "Vas a eliminar definitivamente %d archivos (%d fotos y %d videos) y recuperar %.2f MB de espacio.",
                            uiState.discardedPhotos.size,
                            photoDelCount,
                            videoDelCount,
                            uiState.queuedDeleteMb
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = KeepEmeraldContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                tint = KeepEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Espacio a liberar: +%.2f MB",
                                    uiState.queuedDeleteMb
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = KeepEmerald
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmPermanentDeletion,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscardCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("dialog_confirm_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.confirm_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onRequestConfirmDialog(false) },
                    modifier = Modifier.testTag("dialog_cancel_delete_button")
                ) {
                    Text(
                        text = stringResource(R.string.confirm_dialog_cancel),
                        color = Color.White
                    )
                }
            }
        )
    }

    val animatedQueuedMb by animateFloatAsState(
        targetValue = uiState.queuedDeleteMb,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "animated_queued_mb"
    )
    val deletedPhotosBreakdown = uiState.discardedPhotos.count { !it.isVideo }
    val deletedVideosBreakdown = uiState.discardedPhotos.count { it.isVideo }
    val keptTotalMb = uiState.keptPhotos.sumOf { it.sizeMb.toDouble() }.toFloat()
    val totalReviewedMb = (uiState.queuedDeleteMb + keptTotalMb).coerceAtLeast(0.01f)
    val reclaimRatio by animateFloatAsState(
        targetValue = if (uiState.discardedPhotos.isEmpty()) 0f else (uiState.queuedDeleteMb / totalReviewedMb).coerceIn(0.04f, 1f),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "reclaim_ratio"
    )

    Column(
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 6.dp, bottom = 10.dp)
            .testTag("pre_deletion_review_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clean & Breathable Top Summary Dashboard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review_summary_dashboard"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Revisión antes de eliminar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                // Primary Dashboard Metrics: Items Selected for Deletion & Total Space to Reclaim
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric 1: Number of items selected for deletion
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DiscardCrimsonContainer.copy(alpha = 0.40f),
                        border = BorderStroke(1.dp, DiscardCrimson.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_discarded_count")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DiscardCrimson.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = DiscardCrimson,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${uiState.discardedPhotos.size} archivos",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Para eliminar",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = DiscardCrimson,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Metric 2: Total space that will be reclaimed upon confirmation
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = KeepEmeraldContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_reclaimed_space")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(KeepEmerald.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SdStorage,
                                    contentDescription = null,
                                    tint = KeepEmerald,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(Locale.US, "+%.2f MB", animatedQueuedMb),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = KeepEmerald,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Espacio a liberar",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Compact Reclaim Progress Bar & Kept Items Summary
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { reclaimRatio },
                        color = KeepEmerald,
                        trackColor = ObsidianCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.keptPhotos.size} conservados",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("summary_kept_count")
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%d%% a liberar",
                                (reclaimRatio * 100f).toInt()
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = KeepEmerald,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReviewTabButton(
                title = "A borrar (${uiState.discardedPhotos.size})",
                selected = uiState.selectedSummaryTab == SwipeDecision.DISCARD,
                accentColor = DiscardCrimson,
                onClick = { onSelectTab(SwipeDecision.DISCARD) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_discarded_button")
            )
            ReviewTabButton(
                title = "Conservados (${uiState.keptPhotos.size})",
                selected = uiState.selectedSummaryTab == SwipeDecision.KEEP,
                accentColor = KeepEmerald,
                onClick = { onSelectTab(SwipeDecision.KEEP) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_kept_button")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val displayedPhotos = if (uiState.selectedSummaryTab == SwipeDecision.DISCARD) {
            uiState.discardedPhotos
        } else {
            uiState.keptPhotos
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (displayedPhotos.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = KeepEmerald,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.selectedSummaryTab == SwipeDecision.DISCARD) {
                            "No hay archivos marcados para eliminar"
                        } else {
                            stringResource(R.string.no_photos_in_tab)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedPhotos, key = { it.id }) { photo ->
                        ReviewPhotoItemCard(
                            photo = photo,
                            isMarkedForDeletion = uiState.selectedSummaryTab == SwipeDecision.DISCARD,
                            onToggleAction = {
                                if (uiState.selectedSummaryTab == SwipeDecision.DISCARD) {
                                    onRescuePhotoFromDeleteList(photo)
                                } else {
                                    onMoveKeptToDeleteList(photo)
                                }
                            },
                            onInspectFullscreen = {
                                onInspectPhotoFullscreen(photo)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.remainingPhotos.isNotEmpty()) {
                OutlinedButton(
                    onClick = onResumeSwiping,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("resume_swiping_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Seguir",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onBackToFolders,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("back_to_folders_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Carpetas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (uiState.discardedPhotos.isNotEmpty()) {
                Button(
                    onClick = { onRequestConfirmDialog(true) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscardCrimson,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("confirm_delete_cta_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "Borrar %d (%.1f MB)",
                            uiState.discardedPhotos.size,
                            uiState.queuedDeleteMb
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Button(
                    onClick = onBackToFolders,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KeepEmerald,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("reset_gallery_button")
                ) {
                    Text(
                        text = "Elegir carpetas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewTabButton(
    title: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accentColor.copy(alpha = 0.20f) else ObsidianSurface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else ObsidianBorder
        ),
        modifier = modifier.height(44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) Color.White else TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReviewPhotoItemCard(
    photo: PhotoItem,
    isMarkedForDeletion: Boolean,
    onToggleAction: () -> Unit,
    onInspectFullscreen: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isMarkedForDeletion) DiscardCrimson.copy(alpha = 0.65f) else KeepEmerald.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DeviceMediaThumbnail(
                item = photo,
                thumbSizePx = 1080,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onInspectFullscreen)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.42f),
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.90f)
                        )
                    )
            )

            // Center button to open photo in full size
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clickable(onClick = onInspectFullscreen)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = "Ver foto en grande",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Single-line Top Bar: File Size MB (Left) + Action Chip "Salvar"/"Borrar" (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (photo.isVideo) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = VideoViolet,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f MB", photo.sizeMb),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = UndoAmber,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Action Chip to remove from deletion list ("Salvar") or mark to delete ("Borrar")
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isMarkedForDeletion) KeepEmerald else DiscardCrimsonContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isMarkedForDeletion) KeepEmerald else DiscardCrimson
                    ),
                    modifier = Modifier
                        .height(26.dp)
                        .clickable(onClick = onToggleAction)
                        .testTag("rescue_photo_${photo.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isMarkedForDeletion) {
                                Icons.Default.RestoreFromTrash
                            } else {
                                Icons.Default.DeleteOutline
                            },
                            contentDescription = stringResource(R.string.restore_photo_cd),
                            tint = if (isMarkedForDeletion) Color.Black else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isMarkedForDeletion) "Salvar" else "Borrar",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isMarkedForDeletion) Color.Black else Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = photo.folderName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CelebrationDoneScreen(
    uiState: SwipeGalleryUiState,
    onAnalyzeMoreFolders: () -> Unit,
    onOpenAchievements: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onReturnHome)

    var triggerAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        triggerAnim = true
    }
    val animatedFreedMb by animateFloatAsState(
        targetValue = if (triggerAnim) uiState.lastSessionFreedMb else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "freed_mb_counter"
    )

    Column(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 10.dp)
            .testTag("celebration_done_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top mini bar with quick home & compact trophy badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = KeepEmeraldContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = KeepEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Limpieza completada",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = KeepEmerald
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (uiState.lastSessionMaxStreak > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.6f))
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
                                text = "x${uiState.lastSessionMaxStreak}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = UndoAmber
                            )
                        }
                    }
                }

                Surface(
                    onClick = onOpenAchievements,
                    shape = RoundedCornerShape(50),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Logros",
                            tint = UndoAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${uiState.unlockedAchievementsCount}/100",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = UndoAmber,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Comfortable Scrollable Summary Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Freed Space Card (Compact & Breathable)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.2.dp, KeepEmerald.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(KeepEmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = KeepEmerald,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ESPACIO RECUPERADO AHORA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextSecondary
                            )
                            Text(
                                text = String.format(Locale.US, "+%.2f MB", animatedFreedMb),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = KeepEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Column Session Breakdown (Photos Deleted, Videos Deleted, Kept Safe)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CelebrationStatPill(
                            icon = Icons.Default.Image,
                            value = "${uiState.lastSessionDeletedPhotosCount}",
                            label = "Fotos borradas",
                            accentColor = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        CelebrationStatPill(
                            icon = Icons.Default.Videocam,
                            value = "${uiState.lastSessionDeletedVideosCount}",
                            label = "Videos borrados",
                            accentColor = VideoViolet,
                            modifier = Modifier.weight(1f)
                        )
                        CelebrationStatPill(
                            icon = Icons.Default.CheckCircle,
                            value = "${uiState.lastSessionKeptCount}",
                            label = "Conservados",
                            accentColor = KeepEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Phone Storage & Cumulative Savings Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Memoria de tu teléfono",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1f / %.1f GB",
                                uiState.deviceUsedStorageGb,
                                uiState.deviceTotalStorageGb
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanAccent
                        )
                    }

                    val storageFraction = if (uiState.deviceTotalStorageGb > 0f) {
                        (uiState.deviceUsedStorageGb / uiState.deviceTotalStorageGb).coerceIn(0.05f, 1f)
                    } else 0.5f

                    LinearProgressIndicator(
                        progress = { storageFraction },
                        color = CyanAccent,
                        trackColor = ObsidianCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total liberado con SwipeFoto:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.2f MB (%d archivos)",
                                uiState.lifetimeFreedMb,
                                uiState.lifetimeDeletedCount
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = KeepEmerald
                        )
                    }
                }
            }

            // Compact & Comfortable Achievements Row Card
            Surface(
                onClick = onOpenAchievements,
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(UndoAmber.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = UndoAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Salón de Logros",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${uiState.unlockedAchievementsCount} de 100 desbloqueados",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = UndoAmber.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Ver todos →",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = UndoAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ergonomic One-Handed Bottom Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onReturnHome,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("celebration_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Inicio",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = onAnalyzeMoreFolders,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepEmerald,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("celebration_more_folders_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Otra carpeta",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CelebrationStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
