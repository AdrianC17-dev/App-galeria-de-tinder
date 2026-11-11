package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.DeviceMediaScanner
import com.example.model.AppScreen
import com.example.model.SmartPhotoTag
import com.example.model.SwipeDecision
import com.example.ui.components.AchievementIncentivePopup
import com.example.ui.components.AliveAmbientBackground
import com.example.ui.components.AppDynamicLaunchAnimation
import com.example.ui.components.ProgrammaticSwipeRequest
import com.example.ui.components.StreakMultiplierSideBadge
import com.example.ui.components.SwipeCardStack
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
import com.example.viewmodel.SwipeGalleryUiState
import com.example.viewmodel.SwipeGalleryViewModel
import java.util.Locale

@Composable
fun SwipeGalleryScreen(
    viewModel: SwipeGalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var programmaticSwipe by remember { mutableStateOf<ProgrammaticSwipeRequest?>(null) }
    var previousScreenBeforeAchievements by remember { mutableStateOf(AppScreen.HOME_WELCOME) }
    var showLaunchIntro by remember { mutableStateOf(true) }
    val appEntranceScale by animateFloatAsState(
        targetValue = if (showLaunchIntro) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "app_entrance_scale"
    )

    // Runtime permission launcher for READ_MEDIA_IMAGES + READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE
    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        val anyGranted = resultMap.values.any { it }
        viewModel.onPermissionResult(context, anyGranted)
    }

    // Check permission on first launch and request automatically so user can immediately scan phone media
    LaunchedEffect(Unit) {
        viewModel.checkAndLoadMediaFolders(context)
        if (!DeviceMediaScanner.hasMediaPermissions(context)) {
            mediaPermissionsLauncher.launch(DeviceMediaScanner.requiredPermissions())
        }
    }

    // Android 11+ MediaStore OS deletion confirmation launcher
    val osDeleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.finalizePermanentDeletion(context)
        } else {
            // Even if the OS dialog was dismissed or unavailable on a virtual bucket, allow user to keep reviewing
            viewModel.setShowConfirmDeleteDialog(false)
        }
    }

    // SAF folder picker launcher (optional manual folder addition)
    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.importFromDeviceFolder(context, treeUri)
        }
    }

    // Android Photo & Video Picker launcher
    val multipleMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromPhotoPicker(context, uris)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AliveAmbientBackground(dragProgress = dragProgress)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        scaleX = appEntranceScale
                        scaleY = appEntranceScale
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = uiState.currentScreen,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )) togetherWith fadeOut(tween(160))
                    },
                    label = "screen_router",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        AppScreen.ONBOARDING_TUTORIAL -> {
                            OnboardingTutorialScreen(
                                onFinishOnboarding = viewModel::completeOnboarding
                            )
                        }

                        AppScreen.HOME_WELCOME -> {
                            WelcomeHomeScreen(
                                uiState = uiState,
                                onRequestMediaPermission = {
                                    mediaPermissionsLauncher.launch(DeviceMediaScanner.requiredPermissions())
                                },
                                onStartFolderSelection = {
                                    viewModel.scanDeviceMultimedia(context, navigateToFoldersAfterScan = true)
                                },
                                onOpenAchievements = {
                                    previousScreenBeforeAchievements = AppScreen.HOME_WELCOME
                                    viewModel.navigateToAchievements()
                                },
                                onOpenTutorial = viewModel::openOnboardingTutorial
                            )
                        }

                        AppScreen.FOLDER_SELECTION -> {
                            FolderSelectionScreen(
                                uiState = uiState,
                                onBackHome = viewModel::navigateHome,
                                onToggleFolder = viewModel::toggleFolderSelection,
                                onSelectAll = viewModel::selectAllFolders,
                                onSelectMediaFilter = viewModel::setMediaFilter,
                                onRescanDevice = {
                                    viewModel.scanDeviceMultimedia(context, navigateToFoldersAfterScan = false)
                                },
                                onRequestPermission = {
                                    mediaPermissionsLauncher.launch(DeviceMediaScanner.requiredPermissions())
                                },
                                onLinkSafFolder = { folderTreeLauncher.launch(null) },
                                onPickVisualMedia = {
                                    multipleMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onOpenAchievements = {
                                    previousScreenBeforeAchievements = AppScreen.FOLDER_SELECTION
                                    viewModel.navigateToAchievements()
                                },
                                onStartAnalysis = {
                                    dragProgress = 0f
                                    programmaticSwipe = null
                                    viewModel.startSelectedFoldersAnalysis()
                                },
                                onSuggestDuplicates = {
                                    viewModel.startAiCleanupSuggestDuplicates(context)
                                }
                            )
                        }

                        AppScreen.AI_CLEANUP_DUPLICATES -> {
                            AiCleanupDuplicatesScreen(
                                uiState = uiState,
                                onBackToFolders = viewModel::navigateToFolderSelection,
                                onToggleDuplicateItem = viewModel::toggleAiDuplicateSelection,
                                onSelectAllSuggestedDuplicates = viewModel::selectAllAiSuggestedDuplicates,
                                onApplyBulkDeleteToQueue = viewModel::applyAiBulkDeleteToReviewQueue,
                                onInspectPhotoFullscreen = viewModel::openFullscreenPhotoInspector
                            )
                        }

                        AppScreen.SWIPE_SESSION -> {
                            BackHandler {
                                dragProgress = 0f
                                programmaticSwipe = null
                                viewModel.navigateToFolderSelection()
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SwipeSessionHeader(
                                        uiState = uiState,
                                        onBackToFolders = {
                                            dragProgress = 0f
                                            programmaticSwipe = null
                                            viewModel.navigateToFolderSelection()
                                        },
                                        onOpenAchievements = {
                                            previousScreenBeforeAchievements = AppScreen.SWIPE_SESSION
                                            viewModel.navigateToAchievements()
                                        },
                                        onOpenReviewQueue = {
                                            dragProgress = 0f
                                            programmaticSwipe = null
                                            viewModel.openReviewScreenEarly()
                                        },
                                        onSelectSmartTagFilter = { tag ->
                                            dragProgress = 0f
                                            programmaticSwipe = null
                                            viewModel.selectSmartTagFilter(tag)
                                        }
                                    )

                                    if (uiState.activeSwipePhotos.isEmpty() && uiState.remainingPhotos.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = ObsidianSurface,
                                                border = BorderStroke(1.dp, ObsidianBorder),
                                                modifier = Modifier
                                                    .widthIn(max = 380.dp)
                                                    .fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(24.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = CyanAccent,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Text(
                                                        text = "Sin fotos en '${uiState.selectedSmartTagFilter.label}'",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "Quedan ${uiState.remainingPhotos.size} archivos en otras categorías.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = TextSecondary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Surface(
                                                        onClick = { viewModel.selectSmartTagFilter(SmartPhotoTag.ALL) },
                                                        shape = RoundedCornerShape(50),
                                                        color = CyanAccent
                                                    ) {
                                                        Text(
                                                            text = "Ver todas las categorías",
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = Color.Black,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        SwipeCardStack(
                                            photos = uiState.activeSwipePhotos,
                                            programmaticSwipeRequest = programmaticSwipe,
                                            onSwiped = { decision ->
                                                programmaticSwipe = null
                                                viewModel.classifyTopPhoto(decision)
                                            },
                                            onDragProgressChanged = { progress ->
                                                dragProgress = progress
                                            },
                                            onInspectPhoto = viewModel::openFullscreenPhotoInspector,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        )
                                    }

                                    Text(
                                        text = stringResource(R.string.swipe_hint),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                    )

                                    FloatingActionDock(
                                        dragProgress = dragProgress,
                                        canUndo = uiState.canUndo,
                                        onDiscardClick = {
                                            val top = uiState.activeSwipePhotos.firstOrNull() ?: return@FloatingActionDock
                                            programmaticSwipe = ProgrammaticSwipeRequest(
                                                photoId = top.id,
                                                decision = SwipeDecision.DISCARD
                                            )
                                        },
                                        onUndoClick = {
                                            dragProgress = 0f
                                            programmaticSwipe = null
                                            viewModel.undoLastDecision()
                                        },
                                        onKeepClick = {
                                            val top = uiState.activeSwipePhotos.firstOrNull() ?: return@FloatingActionDock
                                            programmaticSwipe = ProgrammaticSwipeRequest(
                                                photoId = top.id,
                                                decision = SwipeDecision.KEEP
                                            )
                                        }
                                    )
                                }

                                // Compact lateral streak multiplier (x1, x2, x3...)
                                StreakMultiplierSideBadge(
                                    streakCount = uiState.activeStreakCount,
                                    isDeleteStreak = uiState.isDeleteStreakActive,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(bottom = 44.dp)
                                )

                                // Quick compact slide-in/out achievement pill on the top-right
                                AchievementIncentivePopup(
                                    achievement = uiState.activeAchievementPopup,
                                    queuedMb = uiState.queuedDeleteMb,
                                    onDismiss = viewModel::dismissAchievementPopup,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 60.dp)
                                )
                            }
                        }

                        AppScreen.REVIEW_CONFIRM -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                PreDeletionReviewScreen(
                                    uiState = uiState,
                                    onSelectTab = viewModel::selectSummaryTab,
                                    onRescuePhotoFromDeleteList = viewModel::rescuePhotoToKept,
                                    onMoveKeptToDeleteList = viewModel::moveKeptPhotoToDeleteQueue,
                                    onResumeSwiping = viewModel::resumeSwipingRemaining,
                                    onBackToFolders = viewModel::navigateToFolderSelection,
                                    onRequestConfirmDialog = viewModel::setShowConfirmDeleteDialog,
                                    onConfirmPermanentDeletion = {
                                        val pendingIntent = viewModel.buildOsDeletePendingIntent(context)
                                        if (pendingIntent != null) {
                                            try {
                                                osDeleteIntentLauncher.launch(
                                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                                )
                                            } catch (_: Exception) {
                                                viewModel.finalizePermanentDeletion(context)
                                            }
                                        } else {
                                            viewModel.finalizePermanentDeletion(context)
                                        }
                                    },
                                    onInspectPhotoFullscreen = viewModel::openFullscreenPhotoInspector,
                                    onSuggestDuplicates = {
                                        viewModel.startAiCleanupSuggestDuplicates(context)
                                    },
                                    onSelectSmartTagFilter = viewModel::selectSmartTagFilter
                                )

                                AchievementIncentivePopup(
                                    achievement = uiState.activeAchievementPopup,
                                    queuedMb = uiState.queuedDeleteMb,
                                    onDismiss = viewModel::dismissAchievementPopup,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 12.dp)
                                )
                            }
                        }

                        AppScreen.CELEBRATION_DONE -> {
                            CelebrationDoneScreen(
                                uiState = uiState,
                                onAnalyzeMoreFolders = viewModel::navigateToFolderSelection,
                                onOpenAchievements = {
                                    previousScreenBeforeAchievements = AppScreen.CELEBRATION_DONE
                                    viewModel.navigateToAchievements()
                                },
                                onReturnHome = viewModel::navigateHome
                            )
                        }

                        AppScreen.ACHIEVEMENTS_GALLERY -> {
                            AchievementsGalleryScreen(
                                uiState = uiState,
                                onSelectCategory = viewModel::selectAchievementCategory,
                                onBack = {
                                    when (previousScreenBeforeAchievements) {
                                        AppScreen.FOLDER_SELECTION -> viewModel.navigateToFolderSelection()
                                        AppScreen.SWIPE_SESSION -> viewModel.resumeSwipingRemaining()
                                        AppScreen.REVIEW_CONFIRM -> viewModel.openReviewScreenEarly()
                                        else -> viewModel.navigateHome()
                                    }
                                }
                            )
                        }
                    }
                }

                // Fullscreen Photo/Video Inspector Modal when user taps the center of a photo
                val inspectedPhoto = uiState.fullscreenInspectedPhoto
                if (inspectedPhoto != null) {
                    FullscreenPhotoInspectorModal(
                        photo = inspectedPhoto,
                        onDismiss = viewModel::closeFullscreenPhotoInspector
                    )
                }
            }

            // Dynamic Opening Animation when launching the app
            AppDynamicLaunchAnimation(
                visible = showLaunchIntro,
                onDismiss = { showLaunchIntro = false }
            )
        }
    }
}

@Composable
private fun SwipeSessionHeader(
    uiState: SwipeGalleryUiState,
    onBackToFolders: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenReviewQueue: () -> Unit,
    onSelectSmartTagFilter: (SmartPhotoTag) -> Unit
) {
    val progressFraction by animateFloatAsState(
        targetValue = if (uiState.totalCount == 0) 1f else uiState.currentIndex.toFloat() / uiState.totalCount.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "progress_bar"
    )

    Column(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
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
                    onClick = onBackToFolders,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("swipe_back_to_folders_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Carpetas",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.activeSwipePhotos.size} restantes",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Achievements Pill
                Surface(
                    onClick = onOpenAchievements,
                    shape = RoundedCornerShape(50),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, UndoAmber.copy(alpha = 0.55f)),
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = UndoAmber,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Trash queue badge with live MB to free (strictly single-line so icon/text never stretches vertically)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DiscardCrimsonContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, DiscardCrimson.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .height(30.dp)
                        .clickable(
                            enabled = uiState.discardedPhotos.isNotEmpty() || uiState.keptPhotos.isNotEmpty(),
                            onClick = onOpenReviewQueue
                        )
                        .testTag("counter_discarded")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = DiscardCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%d (%.1fM)",
                                uiState.discardedPhotos.size,
                                uiState.queuedDeleteMb
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Kept counter badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = KeepEmeraldContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, KeepEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("counter_kept")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = KeepEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = uiState.keptPhotos.size.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
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

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = KeepEmerald,
                trackColor = ObsidianCard,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = stringResource(
                    R.string.progress_format,
                    (uiState.currentIndex + 1).coerceAtMost(uiState.totalCount),
                    uiState.totalCount
                ),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun FloatingActionDock(
    dragProgress: Float,
    canUndo: Boolean,
    onDiscardClick: () -> Unit,
    onUndoClick: () -> Unit,
    onKeepClick: () -> Unit
) {
    val discardScale by animateFloatAsState(
        targetValue = 1f + ((-dragProgress).coerceAtLeast(0f) * 0.18f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "discard_btn_scale"
    )
    val keepScale by animateFloatAsState(
        targetValue = 1f + (dragProgress.coerceAtLeast(0f) * 0.18f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "keep_btn_scale"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = ObsidianSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ObsidianBorder),
        tonalElevation = 12.dp,
        shadowElevation = 16.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = onDiscardClick,
                    shape = CircleShape,
                    color = if (dragProgress < -0.15f) DiscardCrimson else DiscardCrimsonContainer,
                    border = BorderStroke(2.dp, DiscardCrimson),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            scaleX = discardScale
                            scaleY = discardScale
                        }
                        .testTag("discard_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_discard),
                            tint = if (dragProgress < -0.15f) Color.White else DiscardCrimson,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.action_discard),
                    style = MaterialTheme.typography.labelMedium,
                    color = DiscardCrimson
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = onUndoClick,
                    enabled = canUndo,
                    shape = CircleShape,
                    color = ObsidianCard,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (canUndo) UndoAmber.copy(alpha = 0.8f) else ObsidianBorder
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            alpha = if (canUndo) 1f else 0.4f
                        }
                        .testTag("undo_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.action_undo),
                            tint = if (canUndo) UndoAmber else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.action_undo),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canUndo) UndoAmber else TextSecondary.copy(alpha = 0.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = onKeepClick,
                    shape = CircleShape,
                    color = if (dragProgress > 0.15f) KeepEmerald else KeepEmeraldContainer,
                    border = BorderStroke(2.dp, KeepEmerald),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            scaleX = keepScale
                            scaleY = keepScale
                        }
                        .testTag("keep_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.action_keep),
                            tint = if (dragProgress > 0.15f) Color.Black else KeepEmerald,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.action_keep),
                    style = MaterialTheme.typography.labelMedium,
                    color = KeepEmerald
                )
            }
        }
    }
}
