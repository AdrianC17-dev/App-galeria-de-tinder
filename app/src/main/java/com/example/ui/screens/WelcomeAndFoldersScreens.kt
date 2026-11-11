package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Achievement
import com.example.model.AchievementCategory
import com.example.model.FolderIconType
import com.example.model.MediaFilter
import com.example.model.PhoneFolder
import com.example.ui.components.DeviceMediaThumbnail
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiscardCrimson
import com.example.ui.theme.KeepEmerald
import com.example.ui.theme.KeepEmeraldContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UndoAmber
import com.example.ui.theme.UndoAmberContainer
import com.example.ui.theme.VideoViolet
import com.example.ui.theme.VideoVioletContainer
import com.example.viewmodel.SwipeGalleryUiState
import java.util.Locale

@Composable
fun WelcomeHomeScreen(
    uiState: SwipeGalleryUiState,
    onRequestMediaPermission: () -> Unit,
    onStartFolderSelection: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenTutorial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_orb")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    Column(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 12.dp)
            .testTag("welcome_home_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Badge + Tutorial Trigger + Compact Trophy Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenTutorial,
                    shape = RoundedCornerShape(50),
                    color = KeepEmeraldContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("open_tutorial_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = KeepEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Ver tutorial interactivo",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                            color = KeepEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onOpenAchievements,
                    shape = RoundedCornerShape(50),
                    color = UndoAmberContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, UndoAmber),
                    modifier = Modifier.testTag("open_achievements_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Logros",
                            tint = UndoAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${uiState.unlockedAchievementsCount}/100",
                            style = MaterialTheme.typography.labelMedium.copy(
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

            Spacer(modifier = Modifier.height(22.dp))

            // Animated Storage & Multimedia Radar Ring
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    drawCircle(
                        color = ObsidianCardElevated,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(KeepEmerald, CyanAccent, VideoViolet, DiscardCrimson, KeepEmerald)
                        ),
                        startAngle = sweepAngle,
                        sweepAngle = 250f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "ESPACIO DEL TELÉFONO",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f GB", uiState.deviceTotalStorageGb),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f GB en uso", uiState.deviceUsedStorageGb),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = KeepEmeraldContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = String.format(Locale.US, "Liberado: %.1f MB", uiState.lifetimeFreedMb),
                            style = MaterialTheme.typography.labelLarge,
                            color = KeepEmerald,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Device Multimedia Summary Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WelcomeMetricItem(
                            value = "${uiState.folders.size}",
                            label = "Carpetas",
                            color = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        WelcomeMetricItem(
                            value = "${uiState.totalDevicePhotosCount}",
                            label = "Fotos reales",
                            color = KeepEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        WelcomeMetricItem(
                            value = "${uiState.totalDeviceVideosCount}",
                            label = "Videos reales",
                            color = VideoViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Achievements Preview Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Salón de los 100 Logros",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        TextButton(onClick = onOpenAchievements) {
                            Text(
                                text = "Ver los 100 →",
                                style = MaterialTheme.typography.labelLarge,
                                color = UndoAmber
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val previewAchievements = uiState.achievements
                            .sortedByDescending { it.unlocked }
                            .take(8)
                        previewAchievements.forEach { ach ->
                            AchievementMiniCard(
                                achievement = ach,
                                onClick = onOpenAchievements
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Action Button: Request Permission if not granted, or Open Real Multimedia Folders
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!uiState.hasMediaPermission) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = KeepEmerald,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permiso de Contenido Multimedia",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Se requiere tu permiso para detectar únicamente las carpetas de fotos y videos de tu teléfono.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Button(
                    onClick = onRequestMediaPermission,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KeepEmerald,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("grant_permission_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.welcome_cta_permission),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Button(
                    onClick = onStartFolderSelection,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KeepEmerald,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("start_folders_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.welcome_cta_start),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeMetricItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AchievementMiniCard(
    achievement: Achievement,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (achievement.unlocked) UndoAmber.copy(alpha = 0.16f) else ObsidianCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (achievement.unlocked) UndoAmber else ObsidianBorder
        ),
        modifier = Modifier.width(172.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.unlocked) UndoAmber else Color.White.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${achievement.number}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (achievement.unlocked) Color.Black else TextSecondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (achievement.unlocked) Color.White else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = achievement.badgeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (achievement.unlocked) UndoAmber else TextSecondary.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FolderSelectionScreen(
    uiState: SwipeGalleryUiState,
    onBackHome: () -> Unit,
    onToggleFolder: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onSelectMediaFilter: (MediaFilter) -> Unit,
    onRescanDevice: () -> Unit,
    onRequestPermission: () -> Unit,
    onLinkSafFolder: () -> Unit,
    onPickVisualMedia: () -> Unit,
    onOpenAchievements: () -> Unit,
    onStartAnalysis: () -> Unit,
    onSuggestDuplicates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackHome)

    Column(
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp)
            .testTag("folder_selection_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onBackHome,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("folders_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.folders_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.folders_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onOpenAchievements,
                    shape = RoundedCornerShape(50),
                    color = UndoAmberContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, UndoAmber),
                    modifier = Modifier.testTag("folders_achievements_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Logros",
                            tint = UndoAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${uiState.unlockedAchievementsCount}/100",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = UndoAmber,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IconButton(
                    onClick = onRescanDevice,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("rescan_device_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reescanear dispositivo",
                        tint = KeepEmerald,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips: Todo (Fotos + Videos) / Solo Fotos / Solo Videos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaFilter.entries.forEach { filter ->
                val selected = uiState.mediaFilter == filter
                val chipColor = when (filter) {
                    MediaFilter.ALL -> KeepEmerald
                    MediaFilter.PHOTOS_ONLY -> CyanAccent
                    MediaFilter.VIDEOS_ONLY -> VideoViolet
                }
                val matchingFolderCount = uiState.folders.count { it.matchesMediaFilter(filter) }

                Surface(
                    onClick = { onSelectMediaFilter(filter) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) chipColor.copy(alpha = 0.22f) else ObsidianSurface,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) chipColor else ObsidianBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("media_filter_${filter.name}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = when (filter) {
                                MediaFilter.ALL -> Icons.Default.PhotoAlbum
                                MediaFilter.PHOTOS_ONLY -> Icons.Default.Image
                                MediaFilter.VIDEOS_ONLY -> Icons.Default.Videocam
                            },
                            contentDescription = null,
                            tint = if (selected) chipColor else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${filter.label} ($matchingFolderCount)",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            color = if (selected) Color.White else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val visibleFolders = uiState.filteredFolders

        // Intuitive Selection Control Bar right above the folders list
        if (uiState.hasMediaPermission && !uiState.isScanningDevice && visibleFolders.isNotEmpty()) {
            val selectedVisibleCount = visibleFolders.count { it.id in uiState.selectedFolderIds }
            val allVisibleSelected = selectedVisibleCount == visibleFolders.size

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$selectedVisibleCount de ${visibleFolders.size} carpetas seleccionadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    onClick = { onSelectAll(!allVisibleSelected) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (allVisibleSelected) {
                        KeepEmerald.copy(alpha = 0.16f)
                    } else {
                        ObsidianSurface
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (allVisibleSelected) KeepEmerald else ObsidianBorder
                    ),
                    modifier = Modifier.testTag("toggle_all_folders_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (allVisibleSelected) {
                                Icons.Default.CheckBox
                            } else {
                                Icons.Default.CheckBoxOutlineBlank
                            },
                            contentDescription = if (allVisibleSelected) {
                                stringResource(R.string.folders_deselect_all)
                            } else {
                                stringResource(R.string.folders_select_all)
                            },
                            tint = if (allVisibleSelected) KeepEmerald else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (allVisibleSelected) "Quitar todo" else "Seleccionar todo",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (allVisibleSelected) KeepEmerald else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Folder List or Permission / Empty Device State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                !uiState.hasMediaPermission -> {
                    PermissionPromptCard(
                        onRequestPermission = onRequestPermission,
                        onLinkSafFolder = onLinkSafFolder,
                        onPickVisualMedia = onPickVisualMedia
                    )
                }

                uiState.isScanningDevice -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = KeepEmerald)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Escaneando carpetas de fotos y videos...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }

                uiState.folders.isEmpty() -> {
                    EmptyDeviceMultimediaCard(
                        onRescanDevice = onRescanDevice,
                        onLinkSafFolder = onLinkSafFolder,
                        onPickVisualMedia = onPickVisualMedia
                    )
                }

                visibleFolders.isEmpty() -> {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (uiState.mediaFilter == MediaFilter.VIDEOS_ONLY) {
                                    Icons.Default.Videocam
                                } else {
                                    Icons.Default.Image
                                },
                                contentDescription = null,
                                tint = if (uiState.mediaFilter == MediaFilter.VIDEOS_ONLY) {
                                    VideoViolet
                                } else {
                                    CyanAccent
                                },
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (uiState.mediaFilter == MediaFilter.VIDEOS_ONLY) {
                                    "No hay carpetas que contengan videos"
                                } else {
                                    "No hay carpetas que contengan fotos"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Prueba cambiando el filtro superior a «Todo» o vincula otra carpeta de tu dispositivo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { onSelectMediaFilter(MediaFilter.ALL) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, KeepEmerald)
                            ) {
                                Text("Mostrar todas las carpetas", color = KeepEmerald)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(visibleFolders, key = { it.id }) { folder ->
                            val isSelected = folder.id in uiState.selectedFolderIds
                            PhoneFolderItemCard(
                                folder = folder,
                                activeFilter = uiState.mediaFilter,
                                isSelected = isSelected,
                                onClick = {
                                    onToggleFolder(folder.id)
                                }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onLinkSafFolder,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = KeepEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Otra carpeta", color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = onPickVisualMedia,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = VideoViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Elegir archivos", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom CTAs: Suggest Duplicates (AI Cleanup) + Swipe Analysis
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.selectedFoldersMediaCount > 0) {
                OutlinedButton(
                    onClick = onSuggestDuplicates,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, CyanAccent.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyanAccent.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("suggest_duplicates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sugerir Duplicados y Baja Calidad (AI Cleanup)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = CyanAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onStartAnalysis,
                enabled = uiState.selectedFoldersMediaCount > 0,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepEmerald,
                    contentColor = Color.Black,
                    disabledContainerColor = ObsidianCard,
                    disabledContentColor = TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_analysis_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.selectedFoldersMediaCount > 0) {
                        String.format(
                            Locale.US,
                            "Deslizar %d archivos (%.2f MB)",
                            uiState.selectedFoldersMediaCount,
                            uiState.selectedFoldersTotalMb
                        )
                    } else {
                        "Selecciona carpetas con archivos multimedia"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun PermissionPromptCard(
    onRequestPermission: () -> Unit,
    onLinkSafFolder: () -> Unit,
    onPickVisualMedia: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.5.dp, KeepEmerald.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(KeepEmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = KeepEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Permite el acceso a Fotos y Videos",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Concede el permiso del sistema para listar automáticamente todas las carpetas multimedia de tu teléfono (Cámara, WhatsApp, Videos, Capturas, etc.).",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepEmerald,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Conceder permiso multimedia",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onLinkSafFolder,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Elegir carpeta", color = Color.White)
                }
                OutlinedButton(
                    onClick = onPickVisualMedia,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Selector galería", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyDeviceMultimediaCard(
    onRescanDevice: () -> Unit,
    onLinkSafFolder: () -> Unit,
    onPickVisualMedia: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ObsidianCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = KeepEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No se detectaron carpetas multimedia aún",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tu galería del teléfono parece estar vacía o aún no tiene fotos/videos guardados. Puedes reescanear o vincular cualquier carpeta directamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRescanDevice,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeepEmerald,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reescanear memoria del teléfono",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onLinkSafFolder,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = KeepEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vincular carpeta", color = Color.White)
                }
                OutlinedButton(
                    onClick = onPickVisualMedia,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = VideoViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Elegir archivos", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PhoneFolderItemCard(
    folder: PhoneFolder,
    activeFilter: MediaFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (folder.iconType) {
        FolderIconType.CAMERA -> Icons.Default.CameraAlt
        FolderIconType.WHATSAPP -> Icons.Default.Forum
        FolderIconType.SCREENSHOTS -> Icons.Default.Screenshot
        FolderIconType.DOWNLOADS -> Icons.Default.Download
        FolderIconType.VIDEOS -> Icons.Default.Movie
        FolderIconType.SOCIAL -> Icons.Default.Share
        FolderIconType.DEVICE_FOLDER -> Icons.Default.PhotoAlbum
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ObsidianCard else ObsidianSurface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) KeepEmerald else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = folder.photos.isNotEmpty(), onClick = onClick)
            .testTag("folder_card_${folder.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurface)
            ) {
                val previewItem = folder.filteredPreviewItem(activeFilter)
                if (previewItem != null) {
                    DeviceMediaThumbnail(
                        item = previewItem,
                        thumbSizePx = 256,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = folder.pathLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(7.dp))

                // Fila superior: Cantidad de Fotos y Videos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (folder.imageCount > 0 && activeFilter != MediaFilter.VIDEOS_ONLY) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = KeepEmerald.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${folder.imageCount} fotos",
                                style = MaterialTheme.typography.labelMedium,
                                color = KeepEmerald,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (folder.videoCount > 0 && activeFilter != MediaFilter.PHOTOS_ONLY) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = VideoVioletContainer.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "${folder.videoCount} videos",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Fila inferior: Cantidad de MB debajo para evitar cualquier superposición
                Row(
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
                        text = String.format(Locale.US, "%.2f MB", folder.filteredSizeMb(activeFilter)),
                        style = MaterialTheme.typography.labelLarge,
                        color = UndoAmber,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) KeepEmerald else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) KeepEmerald else TextSecondary.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionada",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementsGalleryScreen(
    uiState: SwipeGalleryUiState,
    onSelectCategory: (AchievementCategory) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val filteredAchievements = if (uiState.selectedAchievementCategory == AchievementCategory.ALL) {
        uiState.achievements
    } else {
        uiState.achievements.filter { it.category == uiState.selectedAchievementCategory }
    }

    Column(
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp)
            .testTag("achievements_gallery_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Colección de 100 Logros",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.unlockedAchievementsCount} de 100 desbloqueados",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = UndoAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = UndoAmber.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.7f)),
                modifier = Modifier.testTag("achievements_percentage_badge")
            ) {
                Text(
                    text = "${uiState.unlockedAchievementsCount}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = UndoAmber,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { uiState.unlockedAchievementsCount / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = UndoAmber,
            trackColor = ObsidianCard,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AchievementCategory.entries.forEach { cat ->
                val selected = uiState.selectedAchievementCategory == cat
                Surface(
                    onClick = { onSelectCategory(cat) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) UndoAmber else ObsidianSurface,
                    border = BorderStroke(1.dp, if (selected) UndoAmber else ObsidianBorder)
                ) {
                    Text(
                        text = cat.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filteredAchievements, key = { it.id }) { ach ->
                FullAchievementRowCard(achievement = ach)
            }
        }
    }
}

@Composable
private fun FullAchievementRowCard(achievement: Achievement) {
    val itemPercent = (achievement.progressFraction * 100f).toInt().coerceIn(0, 100)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) ObsidianCard else ObsidianSurface
        ),
        border = BorderStroke(
            width = if (achievement.unlocked) 1.5.dp else 1.dp,
            color = if (achievement.unlocked) UndoAmber else ObsidianBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.unlocked) UndoAmber else ObsidianCardElevated
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.unlocked) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "#${achievement.number}",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${achievement.number}. ${achievement.title}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (achievement.unlocked) Color.White else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$itemPercent%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (achievement.unlocked) KeepEmerald else UndoAmber,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (achievement.unlocked) {
                        KeepEmerald.copy(alpha = 0.2f)
                    } else {
                        ObsidianCardElevated
                    }
                ) {
                    Text(
                        text = achievement.badgeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = if (achievement.unlocked) KeepEmerald else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (achievement.unlocked) {
                        achievement.incentiveMessage
                    } else {
                        achievement.requirementText
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (achievement.unlocked) Color.White.copy(alpha = 0.85f) else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { achievement.progressFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (achievement.unlocked) KeepEmerald else UndoAmber,
                        trackColor = ObsidianCardElevated,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${achievement.progressCurrent}/${achievement.progressTarget}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (achievement.unlocked) KeepEmerald else TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
