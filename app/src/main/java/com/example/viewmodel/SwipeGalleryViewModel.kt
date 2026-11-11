package com.example.viewmodel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiCleanupAnalysisResult
import com.example.data.AiCleanupRepository
import com.example.data.DeviceMediaScanner
import com.example.data.PlaceholderAiCleanupRepository
import com.example.model.Achievement
import com.example.model.AchievementCatalog
import com.example.model.AchievementCategory
import com.example.model.AppScreen
import com.example.model.FolderIconType
import com.example.model.MediaFilter
import com.example.model.MediaType
import com.example.model.PhoneFolder
import com.example.model.PhotoItem
import com.example.model.SmartPhotoTag
import com.example.model.SwipeDecision
import com.example.model.UserCleaningStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class DecisionRecord(
    val photo: PhotoItem,
    val decision: SwipeDecision
)

data class SwipeGalleryUiState(
    val currentScreen: AppScreen = AppScreen.ONBOARDING_TUTORIAL,
    val hasCompletedOnboarding: Boolean = false,
    val hasMediaPermission: Boolean = false,
    val isScanningDevice: Boolean = false,
    val folders: List<PhoneFolder> = emptyList(),
    val selectedFolderIds: Set<String> = emptySet(),
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    val totalCount: Int = 0,
    val remainingPhotos: List<PhotoItem> = emptyList(),
    val keptPhotos: List<PhotoItem> = emptyList(),
    val discardedPhotos: List<PhotoItem> = emptyList(),
    val history: List<DecisionRecord> = emptyList(),
    val selectedSummaryTab: SwipeDecision = SwipeDecision.DISCARD,
    val achievements: List<Achievement> = AchievementCatalog.buildInitialAchievements(),
    val selectedAchievementCategory: AchievementCategory = AchievementCategory.ALL,
    val activeAchievementPopup: Achievement? = null,
    val showConfirmDeleteDialog: Boolean = false,
    val stats: UserCleaningStats = UserCleaningStats(),
    val deviceTotalStorageGb: Float = DeviceMediaScanner.getDeviceStorageInfo().totalGb,
    val deviceUsedStorageGb: Float = DeviceMediaScanner.getDeviceStorageInfo().usedGb,
    val lifetimeFreedMb: Float = 0f,
    val lifetimeDeletedCount: Int = 0,
    val lastSessionFreedMb: Float = 0f,
    val lastSessionDeletedCount: Int = 0,
    val lastSessionDeletedPhotosCount: Int = 0,
    val lastSessionDeletedVideosCount: Int = 0,
    val lastSessionKeptCount: Int = 0,
    val lastSessionMaxStreak: Int = 0,
    val isRunningAiCleanup: Boolean = false,
    val aiCleanupResult: AiCleanupAnalysisResult? = null,
    val selectedAiDuplicateIds: Set<String> = emptySet(),
    val fullscreenInspectedPhoto: PhotoItem? = null,
    val selectedSmartTagFilter: SmartPhotoTag = SmartPhotoTag.ALL
) {
    val activeSwipePhotos: List<PhotoItem>
        get() = if (selectedSmartTagFilter == SmartPhotoTag.ALL) {
            remainingPhotos
        } else {
            remainingPhotos.filter { it.aiCategoryTag == selectedSmartTagFilter }
        }

    fun countRemainingByTag(tag: SmartPhotoTag): Int {
        return if (tag == SmartPhotoTag.ALL) {
            remainingPhotos.size
        } else {
            remainingPhotos.count { it.aiCategoryTag == tag }
        }
    }

    val currentIndex: Int
        get() = (totalCount - remainingPhotos.size).coerceAtLeast(0)

    val canUndo: Boolean
        get() = history.isNotEmpty()

    val activeStreakCount: Int
        get() = max(stats.currentDeleteStreak, stats.currentKeepStreak)

    val isDeleteStreakActive: Boolean
        get() = stats.currentDeleteStreak >= stats.currentKeepStreak && stats.currentDeleteStreak > 0

    val queuedDeleteMb: Float
        get() = ((discardedPhotos.sumOf { it.sizeMb.toDouble() } * 100.0).roundToInt() / 100f)

    val filteredFolders: List<PhoneFolder>
        get() = folders.filter { it.matchesMediaFilter(mediaFilter) }

    val selectedFoldersMediaCount: Int
        get() = filteredFolders
            .filter { it.id in selectedFolderIds }
            .flatMap { it.photos }
            .count { matchesFilter(it, mediaFilter) }

    val selectedFoldersTotalMb: Float
        get() = ((filteredFolders
            .filter { it.id in selectedFolderIds }
            .flatMap { it.photos }
            .filter { matchesFilter(it, mediaFilter) }
            .sumOf { it.sizeMb.toDouble() } * 100.0).roundToInt() / 100f)

    val totalDevicePhotosCount: Int
        get() = folders.sumOf { it.imageCount }

    val totalDeviceVideosCount: Int
        get() = folders.sumOf { it.videoCount }

    val totalDeviceSizeMb: Float
        get() = ((folders.sumOf { it.totalSizeMb.toDouble() } * 10.0).roundToInt() / 10f)

    val unlockedAchievementsCount: Int
        get() = achievements.count { it.unlocked }

    private fun matchesFilter(item: PhotoItem, filter: MediaFilter): Boolean {
        return when (filter) {
            MediaFilter.ALL -> true
            MediaFilter.PHOTOS_ONLY -> item.mediaType == MediaType.IMAGE
            MediaFilter.VIDEOS_ONLY -> item.mediaType == MediaType.VIDEO
        }
    }
}

class SwipeGalleryViewModel(
    private val aiCleanupRepository: AiCleanupRepository = PlaceholderAiCleanupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeGalleryUiState())
    val uiState: StateFlow<SwipeGalleryUiState> = _uiState.asStateFlow()

    fun openFullscreenPhotoInspector(photo: PhotoItem) {
        _uiState.update { it.copy(fullscreenInspectedPhoto = photo) }
    }

    fun closeFullscreenPhotoInspector() {
        _uiState.update { it.copy(fullscreenInspectedPhoto = null) }
    }

    fun checkAndLoadMediaFolders(context: Context) {
        val storage = DeviceMediaScanner.getDeviceStorageInfo()
        val granted = DeviceMediaScanner.hasMediaPermissions(context)
        _uiState.update {
            it.copy(
                hasMediaPermission = granted,
                deviceTotalStorageGb = storage.totalGb,
                deviceUsedStorageGb = storage.usedGb
            )
        }
        if (granted) {
            scanDeviceMultimedia(context)
        }
    }

    fun onPermissionResult(context: Context, granted: Boolean) {
        val hasAccess = granted || DeviceMediaScanner.hasMediaPermissions(context)
        _uiState.update { it.copy(hasMediaPermission = hasAccess) }
        if (hasAccess) {
            scanDeviceMultimedia(context, navigateToFoldersAfterScan = true)
        }
    }

    fun scanDeviceMultimedia(
        context: Context,
        navigateToFoldersAfterScan: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningDevice = true) }
            val scannedFolders = DeviceMediaScanner.scanAllMultimediaFolders(context)
            _uiState.update { state ->
                val customFolders = state.folders.filter { it.id.startsWith("saf_") || it.id.startsWith("picker_") }
                val mergedFolders = customFolders + scannedFolders
                val defaultSelected = if (state.selectedFolderIds.isEmpty()) {
                    mergedFolders.map { it.id }.toSet()
                } else {
                    state.selectedFolderIds.intersect(mergedFolders.map { it.id }.toSet())
                        .ifEmpty { mergedFolders.map { it.id }.toSet() }
                }

                val updatedStats = state.stats.copy(
                    foldersScannedCount = max(state.stats.foldersScannedCount, mergedFolders.size),
                    maxFoldersSelectedAtOnce = max(state.stats.maxFoldersSelectedAtOnce, defaultSelected.size)
                )
                val (evaluatedAchievements, newlyUnlocked) = AchievementCatalog.evaluate(
                    stats = updatedStats,
                    previous = state.achievements
                )

                state.copy(
                    hasMediaPermission = true,
                    isScanningDevice = false,
                    folders = mergedFolders,
                    selectedFolderIds = defaultSelected,
                    stats = updatedStats,
                    achievements = evaluatedAchievements,
                    activeAchievementPopup = newlyUnlocked.firstOrNull() ?: state.activeAchievementPopup,
                    currentScreen = if (navigateToFoldersAfterScan) AppScreen.FOLDER_SELECTION else state.currentScreen
                )
            }
        }
    }

    fun completeOnboarding() {
        _uiState.update {
            it.copy(
                hasCompletedOnboarding = true,
                currentScreen = AppScreen.HOME_WELCOME
            )
        }
    }

    fun openOnboardingTutorial() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.ONBOARDING_TUTORIAL,
                activeAchievementPopup = null
            )
        }
    }

    fun navigateToFolderSelection() {
        _uiState.update { it.copy(currentScreen = AppScreen.FOLDER_SELECTION) }
    }

    fun navigateToAchievements() {
        _uiState.update { it.copy(currentScreen = AppScreen.ACHIEVEMENTS_GALLERY) }
    }

    fun selectAchievementCategory(category: AchievementCategory) {
        _uiState.update { it.copy(selectedAchievementCategory = category) }
    }

    fun navigateHome() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.HOME_WELCOME,
                activeAchievementPopup = null,
                showConfirmDeleteDialog = false
            )
        }
    }

    fun setMediaFilter(filter: MediaFilter) {
        _uiState.update { it.copy(mediaFilter = filter) }
    }

    fun selectSmartTagFilter(tag: SmartPhotoTag) {
        _uiState.update { it.copy(selectedSmartTagFilter = tag) }
    }

    fun toggleFolderSelection(folderId: String) {
        _uiState.update { state ->
            val updated = if (folderId in state.selectedFolderIds) {
                state.selectedFolderIds - folderId
            } else {
                state.selectedFolderIds + folderId
            }
            val updatedStats = state.stats.copy(
                maxFoldersSelectedAtOnce = max(state.stats.maxFoldersSelectedAtOnce, updated.size)
            )
            val (evaluated, newlyUnlocked) = AchievementCatalog.evaluate(updatedStats, state.achievements)
            state.copy(
                selectedFolderIds = updated,
                stats = updatedStats,
                achievements = evaluated,
                activeAchievementPopup = newlyUnlocked.firstOrNull() ?: state.activeAchievementPopup
            )
        }
    }

    fun selectAllFolders(selectAll: Boolean) {
        _uiState.update { state ->
            val visibleIds = state.filteredFolders.map { it.id }.toSet()
            val updated = if (selectAll) {
                state.selectedFolderIds + visibleIds
            } else {
                state.selectedFolderIds - visibleIds
            }
            val updatedStats = state.stats.copy(
                maxFoldersSelectedAtOnce = max(state.stats.maxFoldersSelectedAtOnce, updated.size)
            )
            val (evaluated, _) = AchievementCatalog.evaluate(updatedStats, state.achievements)
            state.copy(
                selectedFolderIds = updated,
                stats = updatedStats,
                achievements = evaluated
            )
        }
    }

    fun startSelectedFoldersAnalysis() {
        _uiState.update { state ->
            val itemsToAnalyze = state.folders
                .filter { it.id in state.selectedFolderIds }
                .flatMap { it.photos }
                .filter { item ->
                    when (state.mediaFilter) {
                        MediaFilter.ALL -> true
                        MediaFilter.PHOTOS_ONLY -> item.mediaType == MediaType.IMAGE
                        MediaFilter.VIDEOS_ONLY -> item.mediaType == MediaType.VIDEO
                    }
                }
            if (itemsToAnalyze.isEmpty()) return@update state
            state.copy(
                currentScreen = AppScreen.SWIPE_SESSION,
                totalCount = itemsToAnalyze.size,
                remainingPhotos = itemsToAnalyze,
                keptPhotos = emptyList(),
                discardedPhotos = emptyList(),
                history = emptyList(),
                activeAchievementPopup = null,
                showConfirmDeleteDialog = false,
                fullscreenInspectedPhoto = null,
                selectedSmartTagFilter = SmartPhotoTag.ALL
            )
        }
    }

    /**
     * Runs AI Cleanup / Duplicate & Low-Quality suggestion mode on the selected folders.
     */
    fun startAiCleanupSuggestDuplicates(context: Context) {
        val stateSnap = _uiState.value
        val itemsToAnalyze = stateSnap.folders
            .filter { it.id in stateSnap.selectedFolderIds }
            .flatMap { it.photos }
            .filter { item ->
                when (stateSnap.mediaFilter) {
                    MediaFilter.ALL -> true
                    MediaFilter.PHOTOS_ONLY -> item.mediaType == MediaType.IMAGE
                    MediaFilter.VIDEOS_ONLY -> item.mediaType == MediaType.VIDEO
                }
            }
        if (itemsToAnalyze.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.AI_CLEANUP_DUPLICATES,
                    isRunningAiCleanup = true,
                    aiCleanupResult = null,
                    selectedAiDuplicateIds = emptySet(),
                    fullscreenInspectedPhoto = null
                )
            }
            val result = aiCleanupRepository.analyzeDuplicatesAndLowQuality(context, itemsToAnalyze)
            val preSelectedIds = result.clusters
                .flatMap { it.suggestedToDelete }
                .map { it.id }
                .toSet()
            _uiState.update {
                it.copy(
                    isRunningAiCleanup = false,
                    aiCleanupResult = result,
                    selectedAiDuplicateIds = preSelectedIds
                )
            }
        }
    }

    fun toggleAiDuplicateSelection(photoId: String) {
        _uiState.update { state ->
            val updated = if (photoId in state.selectedAiDuplicateIds) {
                state.selectedAiDuplicateIds - photoId
            } else {
                state.selectedAiDuplicateIds + photoId
            }
            state.copy(selectedAiDuplicateIds = updated)
        }
    }

    fun selectAllAiSuggestedDuplicates(selectAll: Boolean) {
        _uiState.update { state ->
            val allSuggestedIds = state.aiCleanupResult?.clusters
                ?.flatMap { it.suggestedToDelete }
                ?.map { it.id }
                ?.toSet() ?: emptySet()
            state.copy(
                selectedAiDuplicateIds = if (selectAll) allSuggestedIds else emptySet()
            )
        }
    }

    fun applyAiBulkDeleteToReviewQueue() {
        _uiState.update { state ->
            val analysis = state.aiCleanupResult ?: return@update state
            val allSuggested = analysis.clusters.flatMap { it.suggestedToDelete }
            val chosenToDelete = allSuggested.filter { it.id in state.selectedAiDuplicateIds }
            if (chosenToDelete.isEmpty()) return@update state

            val bestKept = analysis.clusters.mapNotNull { it.bestPhotoToKeep }
            val mergedDiscarded = (state.discardedPhotos + chosenToDelete).distinctBy { it.id }
            val mergedKept = (state.keptPhotos + bestKept)
                .distinctBy { it.id }
                .filterNot { kept -> mergedDiscarded.any { it.id == kept.id } }

            val totalQueuedMb = mergedDiscarded.sumOf { it.sizeMb.toDouble() }.toFloat()
            val updatedStats = state.stats.copy(
                totalMarkedOrDeletedCount = state.stats.totalMarkedOrDeletedCount + chosenToDelete.size,
                totalFreedAndQueuedMb = state.lifetimeFreedMb + totalQueuedMb
            )
            val (evaluatedAchievements, newlyUnlocked) = AchievementCatalog.evaluate(
                stats = updatedStats,
                previous = state.achievements
            )
            val nonStreakUnlocked = newlyUnlocked.filter {
                it.category != AchievementCategory.RACHAS_CURADURIA
            }

            state.copy(
                currentScreen = AppScreen.REVIEW_CONFIRM,
                discardedPhotos = mergedDiscarded,
                keptPhotos = mergedKept,
                selectedSummaryTab = SwipeDecision.DISCARD,
                stats = updatedStats,
                achievements = evaluatedAchievements,
                activeAchievementPopup = nonStreakUnlocked.lastOrNull(),
                fullscreenInspectedPhoto = null
            )
        }
    }

    fun classifyTopPhoto(decision: SwipeDecision) {
        _uiState.update { state ->
            val currentTop = state.activeSwipePhotos.firstOrNull()
                ?: state.remainingPhotos.firstOrNull()
                ?: return@update state
            val updatedRemaining = state.remainingPhotos.filterNot { it.id == currentTop.id }
            val updatedKept = if (decision == SwipeDecision.KEEP) {
                state.keptPhotos + currentTop
            } else {
                state.keptPhotos
            }
            val updatedDiscarded = if (decision == SwipeDecision.DISCARD) {
                state.discardedPhotos + currentTop
            } else {
                state.discardedPhotos
            }

            val folderLower = "${currentTop.folderName} ${currentTop.relativePath}".lowercase(Locale.ROOT)
            val newStats = if (decision == SwipeDecision.DISCARD) {
                val newDelStreak = state.stats.currentDeleteStreak + 1
                val newMarkedCount = state.stats.totalMarkedOrDeletedCount + 1
                val newVideosCount = state.stats.totalVideosMarkedOrDeleted + if (currentTop.isVideo) 1 else 0
                val newPhotosCount = state.stats.totalPhotosMarkedOrDeleted + if (!currentTop.isVideo) 1 else 0
                val durationSec = (currentTop.durationMillis / 1000L).toInt()
                val totalQueuedMb = updatedDiscarded.sumOf { it.sizeMb.toDouble() }.toFloat()

                state.stats.copy(
                    totalMarkedOrDeletedCount = newMarkedCount,
                    totalFreedAndQueuedMb = state.lifetimeFreedMb + totalQueuedMb,
                    totalVideosMarkedOrDeleted = newVideosCount,
                    totalPhotosMarkedOrDeleted = newPhotosCount,
                    maxSingleFileMbDeleted = max(state.stats.maxSingleFileMbDeleted, currentTop.sizeMb),
                    maxVideoDurationSecDeleted = max(state.stats.maxVideoDurationSecDeleted, durationSec),
                    currentDeleteStreak = newDelStreak,
                    maxDeleteStreak = max(state.stats.maxDeleteStreak, newDelStreak),
                    currentKeepStreak = 0,
                    cleanedCameraItem = state.stats.cleanedCameraItem || folderLower.contains("camera") || folderLower.contains("dcim") || folderLower.contains("cámara"),
                    cleanedWhatsAppItem = state.stats.cleanedWhatsAppItem || folderLower.contains("whatsapp") || folderLower.contains("telegram"),
                    cleanedScreenshotsItem = state.stats.cleanedScreenshotsItem || folderLower.contains("screenshot") || folderLower.contains("captura"),
                    cleanedDownloadsItem = state.stats.cleanedDownloadsItem || folderLower.contains("download") || folderLower.contains("descarga"),
                    cleanedMoviesItem = state.stats.cleanedMoviesItem || folderLower.contains("movie") || folderLower.contains("video") || currentTop.isVideo
                )
            } else {
                val newKeepStreak = state.stats.currentKeepStreak + 1
                state.stats.copy(
                    totalKeptCount = state.stats.totalKeptCount + 1,
                    currentKeepStreak = newKeepStreak,
                    maxKeepStreak = max(state.stats.maxKeepStreak, newKeepStreak),
                    currentDeleteStreak = 0
                )
            }

            val (evaluatedAchievements, newlyUnlocked) = AchievementCatalog.evaluate(
                stats = newStats,
                previous = state.achievements
            )

            // Streak achievements are shown via the compact lateral x1, x2, x3 multiplier only.
            // Only non-streak achievements trigger the quick compact slide pill.
            val nonStreakUnlocked = newlyUnlocked.filter {
                it.category != AchievementCategory.RACHAS_CURADURIA
            }
            val popupToShow: Achievement? = nonStreakUnlocked.lastOrNull() ?: state.activeAchievementPopup

            val nextScreen = if (updatedRemaining.isEmpty()) {
                AppScreen.REVIEW_CONFIRM
            } else {
                AppScreen.SWIPE_SESSION
            }

            val nextTagFilter = if (
                state.selectedSmartTagFilter != SmartPhotoTag.ALL &&
                updatedRemaining.none { it.aiCategoryTag == state.selectedSmartTagFilter }
            ) {
                SmartPhotoTag.ALL
            } else {
                state.selectedSmartTagFilter
            }

            state.copy(
                currentScreen = nextScreen,
                remainingPhotos = updatedRemaining,
                keptPhotos = updatedKept,
                discardedPhotos = updatedDiscarded,
                history = state.history + DecisionRecord(currentTop, decision),
                stats = newStats,
                achievements = evaluatedAchievements,
                activeAchievementPopup = popupToShow,
                selectedSummaryTab = if (updatedDiscarded.isNotEmpty()) SwipeDecision.DISCARD else SwipeDecision.KEEP,
                selectedSmartTagFilter = nextTagFilter
            )
        }
    }

    fun dismissAchievementPopup() {
        _uiState.update { it.copy(activeAchievementPopup = null) }
    }

    fun undoLastDecision() {
        _uiState.update { state ->
            val lastRecord = state.history.lastOrNull() ?: return@update state
            val updatedHistory = state.history.dropLast(1)
            val updatedKept = if (lastRecord.decision == SwipeDecision.KEEP) {
                state.keptPhotos.filterNot { it.id == lastRecord.photo.id }
            } else {
                state.keptPhotos
            }
            val updatedDiscarded = if (lastRecord.decision == SwipeDecision.DISCARD) {
                state.discardedPhotos.filterNot { it.id == lastRecord.photo.id }
            } else {
                state.discardedPhotos
            }
            val updatedStats = state.stats.copy(totalUndoUsedCount = state.stats.totalUndoUsedCount + 1)
            val (evaluated, newlyUnlocked) = AchievementCatalog.evaluate(updatedStats, state.achievements)

            state.copy(
                currentScreen = AppScreen.SWIPE_SESSION,
                remainingPhotos = listOf(lastRecord.photo) + state.remainingPhotos,
                keptPhotos = updatedKept,
                discardedPhotos = updatedDiscarded,
                history = updatedHistory,
                stats = updatedStats,
                achievements = evaluated,
                activeAchievementPopup = newlyUnlocked.firstOrNull()
            )
        }
    }

    fun openReviewScreenEarly() {
        _uiState.update { state ->
            state.copy(
                currentScreen = AppScreen.REVIEW_CONFIRM,
                selectedSummaryTab = SwipeDecision.DISCARD,
                activeAchievementPopup = null
            )
        }
    }

    fun resumeSwipingRemaining() {
        _uiState.update { state ->
            if (state.remainingPhotos.isEmpty()) return@update state
            state.copy(
                currentScreen = AppScreen.SWIPE_SESSION,
                showConfirmDeleteDialog = false
            )
        }
    }

    fun selectSummaryTab(tab: SwipeDecision) {
        _uiState.update { it.copy(selectedSummaryTab = tab) }
    }

    /**
     * Removes a photo/video from the deletion list before final confirmation so it is kept safe.
     */
    fun rescuePhotoToKept(photo: PhotoItem) {
        _uiState.update { state ->
            if (state.discardedPhotos.none { it.id == photo.id }) return@update state
            val updatedDiscarded = state.discardedPhotos.filterNot { it.id == photo.id }
            val updatedStats = state.stats.copy(
                totalRescuedInReviewCount = state.stats.totalRescuedInReviewCount + 1
            )
            val (evaluated, newlyUnlocked) = AchievementCatalog.evaluate(updatedStats, state.achievements)

            state.copy(
                discardedPhotos = updatedDiscarded,
                keptPhotos = state.keptPhotos + photo,
                stats = updatedStats,
                achievements = evaluated,
                activeAchievementPopup = newlyUnlocked.firstOrNull()
            )
        }
    }

    fun moveKeptPhotoToDeleteQueue(photo: PhotoItem) {
        _uiState.update { state ->
            if (state.keptPhotos.none { it.id == photo.id }) return@update state
            state.copy(
                keptPhotos = state.keptPhotos.filterNot { it.id == photo.id },
                discardedPhotos = state.discardedPhotos + photo
            )
        }
    }

    fun setShowConfirmDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showConfirmDeleteDialog = show) }
    }

    /**
     * Creates an Android 11+ MediaStore.createDeleteRequest PendingIntent for real device MediaStore URIs
     * if available, so the OS can also delete the underlying files cleanly.
     */
    fun buildOsDeletePendingIntent(context: Context): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val mediaStoreUris = _uiState.value.discardedPhotos
            .filter { it.mediaStoreId > 0L }
            .mapNotNull {
                try {
                    Uri.parse(it.contentUri)
                } catch (_: Exception) {
                    null
                }
            }
        if (mediaStoreUris.isEmpty()) return null
        return try {
            MediaStore.createDeleteRequest(context.contentResolver, mediaStoreUris)
        } catch (_: Exception) {
            null
        }
    }

    fun finalizePermanentDeletion(context: Context) {
        _uiState.update { state ->
            val toDelete = state.discardedPhotos
            val deletedIds = toDelete.map { it.id }.toSet()
            val freedMb = ((toDelete.sumOf { it.sizeMb.toDouble() } * 100.0).roundToInt() / 100f)

            // Best-effort direct delete for SAF documents or legacy MediaStore items
            toDelete.forEach { item ->
                try {
                    val uri = Uri.parse(item.contentUri)
                    if (DocumentsContract.isDocumentUri(context, uri)) {
                        DocumentsContract.deleteDocument(context.contentResolver, uri)
                    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        context.contentResolver.delete(uri, null, null)
                    }
                } catch (_: Exception) {}
            }

            // Update folders removing the deleted files and dropping empty folders
            val updatedFolders = state.folders.mapNotNull { folder ->
                val remainingInFolder = folder.photos.filterNot { it.id in deletedIds }
                if (remainingInFolder.isEmpty()) null else folder.copy(photos = remainingInFolder)
            }

            val newLifetimeMb = ((state.lifetimeFreedMb + freedMb) * 100f).roundToInt() / 100f
            val newLifetimeCount = state.lifetimeDeletedCount + toDelete.size
            val updatedStats = state.stats.copy(
                totalConfirmedDeletedCount = newLifetimeCount,
                totalFreedAndQueuedMb = newLifetimeMb,
                completedSessionsCount = state.stats.completedSessionsCount + 1
            )
            val (evaluatedAchievements, newlyUnlocked) = AchievementCatalog.evaluate(
                stats = updatedStats,
                previous = state.achievements
            )

            val deletedPhotosCount = toDelete.count { !it.isVideo }
            val deletedVideosCount = toDelete.count { it.isVideo }
            val sessionKeptCount = state.keptPhotos.size
            val sessionMaxStreak = max(updatedStats.maxDeleteStreak, updatedStats.maxKeepStreak)
            val nonStreakUnlocked = newlyUnlocked.filter {
                it.category != AchievementCategory.RACHAS_CURADURIA
            }

            state.copy(
                currentScreen = AppScreen.CELEBRATION_DONE,
                folders = updatedFolders,
                selectedFolderIds = state.selectedFolderIds.intersect(updatedFolders.map { it.id }.toSet()),
                discardedPhotos = emptyList(),
                history = emptyList(),
                showConfirmDeleteDialog = false,
                stats = updatedStats,
                achievements = evaluatedAchievements,
                activeAchievementPopup = nonStreakUnlocked.lastOrNull(),
                lifetimeFreedMb = newLifetimeMb,
                lifetimeDeletedCount = newLifetimeCount,
                lastSessionFreedMb = freedMb,
                lastSessionDeletedCount = toDelete.size,
                lastSessionDeletedPhotosCount = deletedPhotosCount,
                lastSessionDeletedVideosCount = deletedVideosCount,
                lastSessionKeptCount = sessionKeptCount,
                lastSessionMaxStreak = sessionMaxStreak
            )
        }
    }

    fun importFromDeviceFolder(context: Context, treeUri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            } catch (_: Exception) {}

            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val importedItems = mutableListOf<PhotoItem>()
            val folderId = "saf_folder_${System.currentTimeMillis()}"
            val folderDisplayName = docId.substringAfterLast(':').substringAfterLast('/').ifBlank {
                "Carpeta Seleccionada"
            }

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                var idx = 1
                while (cursor.moveToNext() && idx <= 80) {
                    val mime = if (mimeCol >= 0 && !cursor.isNull(mimeCol)) cursor.getString(mimeCol) ?: "" else ""
                    val name = if (nameCol >= 0 && !cursor.isNull(nameCol)) cursor.getString(nameCol) ?: "Archivo_$idx" else "Archivo_$idx"
                    val isImg = mime.startsWith("image/") || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) || name.endsWith(".webp", true)
                    val isVid = mime.startsWith("video/") || name.endsWith(".mp4", true) || name.endsWith(".mov", true) || name.endsWith(".mkv", true) || name.endsWith(".webm", true)

                    if (isImg || isVid) {
                        val childDocId = cursor.getString(idCol)
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        val sizeBytes = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 3_500_000L
                        val sizeMb = ((sizeBytes.toDouble() / (1024.0 * 1024.0)) * 100.0).roundToInt().coerceAtLeast(1) / 100f

                        val resolvedMime = mime.ifBlank { if (isVid) "video/mp4" else "image/jpeg" }
                        val smartTag = aiCleanupRepository.autoCategorizeItem(
                            fileName = name,
                            folderName = folderDisplayName,
                            relativePath = folderDisplayName,
                            sizeBytes = sizeBytes,
                            mimeType = resolvedMime
                        )

                        importedItems.add(
                            PhotoItem(
                                id = "${folderId}_$idx",
                                title = name.substringBeforeLast('.'),
                                fileName = name,
                                folderId = folderId,
                                folderName = folderDisplayName,
                                relativePath = folderDisplayName,
                                sizeMb = sizeMb,
                                sizeBytes = sizeBytes,
                                dateTaken = if (isVid) "Video · Carpeta vinculada" else "Foto · Carpeta vinculada",
                                resolution = if (isVid) "Video" else "Imagen",
                                mimeType = resolvedMime,
                                mediaType = if (isVid) MediaType.VIDEO else MediaType.IMAGE,
                                contentUri = childUri.toString(),
                                aiCategoryTag = smartTag
                            )
                        )
                        idx++
                    }
                }
            }

            if (importedItems.isNotEmpty()) {
                val newFolder = PhoneFolder(
                    id = folderId,
                    name = folderDisplayName,
                    pathLabel = "Carpeta vinculada del teléfono",
                    iconType = if (importedItems.all { it.isVideo }) FolderIconType.VIDEOS else FolderIconType.DEVICE_FOLDER,
                    photos = importedItems
                )
                _uiState.update { state ->
                    val merged = listOf(newFolder) + state.folders
                    val updatedStats = state.stats.copy(
                        foldersScannedCount = max(state.stats.foldersScannedCount, merged.size)
                    )
                    val (evaluated, newlyUnlocked) = AchievementCatalog.evaluate(updatedStats, state.achievements)
                    state.copy(
                        folders = merged,
                        selectedFolderIds = state.selectedFolderIds + folderId,
                        stats = updatedStats,
                        achievements = evaluated,
                        activeAchievementPopup = newlyUnlocked.firstOrNull(),
                        currentScreen = AppScreen.FOLDER_SELECTION
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun importFromPhotoPicker(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val contentResolver = context.contentResolver
        val folderId = "picker_folder_${System.currentTimeMillis()}"
        val folderName = "Selección Multimedia"

        val imported = uris.mapIndexed { index, uri ->
            var displayName = "MEDIA_${index + 1}"
            var sizeBytes = 4_000_000L
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val isVid = mimeType.startsWith("video/")
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                            displayName = cursor.getString(nameIdx)
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            sizeBytes = cursor.getLong(sizeIdx)
                        }
                    }
                }
            } catch (_: Exception) {}

            val sizeMb = ((sizeBytes.toDouble() / (1024.0 * 1024.0)) * 100.0).roundToInt().coerceAtLeast(1) / 100f
            val smartTag = aiCleanupRepository.autoCategorizeItem(
                fileName = displayName,
                folderName = folderName,
                relativePath = "Galería del teléfono",
                sizeBytes = sizeBytes,
                mimeType = mimeType
            )
            PhotoItem(
                id = "${folderId}_$index",
                title = displayName.substringBeforeLast('.'),
                fileName = displayName,
                folderId = folderId,
                folderName = folderName,
                relativePath = "Galería del teléfono",
                sizeMb = sizeMb,
                sizeBytes = sizeBytes,
                dateTaken = if (isVid) "Video seleccionado" else "Foto seleccionada",
                resolution = if (isVid) "Video" else "Imagen",
                mimeType = mimeType,
                mediaType = if (isVid) MediaType.VIDEO else MediaType.IMAGE,
                contentUri = uri.toString(),
                aiCategoryTag = smartTag
            )
        }

        val newFolder = PhoneFolder(
            id = folderId,
            name = folderName,
            pathLabel = "${imported.size} archivos seleccionados",
            iconType = if (imported.any { it.isVideo }) FolderIconType.VIDEOS else FolderIconType.DEVICE_FOLDER,
            photos = imported
        )

        _uiState.update { state ->
            val merged = listOf(newFolder) + state.folders
            state.copy(
                folders = merged,
                selectedFolderIds = setOf(folderId),
                currentScreen = AppScreen.FOLDER_SELECTION
            )
        }
    }
}
