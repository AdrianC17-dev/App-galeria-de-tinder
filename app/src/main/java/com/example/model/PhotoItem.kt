package com.example.model

import java.util.Locale

enum class SwipeDecision {
    KEEP,
    DISCARD
}

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class MediaFilter(val label: String) {
    ALL("Todo"),
    PHOTOS_ONLY("Fotos"),
    VIDEOS_ONLY("Videos")
}

enum class AppScreen {
    ONBOARDING_TUTORIAL,
    HOME_WELCOME,
    FOLDER_SELECTION,
    AI_CLEANUP_DUPLICATES,
    SWIPE_SESSION,
    REVIEW_CONFIRM,
    CELEBRATION_DONE,
    ACHIEVEMENTS_GALLERY
}

enum class FolderIconType {
    CAMERA,
    WHATSAPP,
    SCREENSHOTS,
    DOWNLOADS,
    VIDEOS,
    SOCIAL,
    DEVICE_FOLDER
}

enum class AchievementCategory(val label: String) {
    ALL("Todos"),
    CANTIDAD("Limpieza"),
    ESPACIO("Espacio MB"),
    VIDEOS("Videos"),
    RACHAS_CURADURIA("Rachas y Curaduría"),
    CARPETAS("Carpetas"),
    COLECCIONISTA("Maestría")
}

enum class SmartPhotoTag(val label: String, val badgeName: String) {
    ALL("Todas", "All"),
    SCREENSHOTS("Screenshots", "Screenshots"),
    TRAVEL("Travel", "Travel"),
    FOOD("Food", "Food"),
    DOCUMENTS("Documents", "Documents"),
    CAMERA_PORTRAITS("Cámara", "Camera")
}

data class PhotoItem(
    val id: String,
    val mediaStoreId: Long = -1L,
    val title: String,
    val fileName: String,
    val folderId: String,
    val folderName: String,
    val relativePath: String = "",
    val sizeMb: Float,
    val sizeBytes: Long = 0L,
    val dateAddedSeconds: Long = 0L,
    val dateTaken: String,
    val resolution: String,
    val mimeType: String = "image/jpeg",
    val mediaType: MediaType = MediaType.IMAGE,
    val durationMillis: Long = 0L,
    val contentUri: String,
    val aiCategoryTag: SmartPhotoTag = SmartPhotoTag.CAMERA_PORTRAITS
) {
    val isVideo: Boolean
        get() = mediaType == MediaType.VIDEO

    val formattedDuration: String
        get() {
            if (!isVideo || durationMillis <= 0L) return "VIDEO"
            val totalSeconds = (durationMillis / 1000L).coerceAtLeast(1L)
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
}

data class PhoneFolder(
    val id: String,
    val name: String,
    val pathLabel: String,
    val iconType: FolderIconType,
    val photos: List<PhotoItem>
) {
    val totalSizeMb: Float
        get() = photos.sumOf { it.sizeMb.toDouble() }.toFloat()

    val imageCount: Int
        get() = photos.count { it.mediaType == MediaType.IMAGE }

    val videoCount: Int
        get() = photos.count { it.mediaType == MediaType.VIDEO }

    fun matchesMediaFilter(filter: MediaFilter): Boolean {
        return when (filter) {
            MediaFilter.ALL -> photos.isNotEmpty()
            MediaFilter.PHOTOS_ONLY -> imageCount > 0
            MediaFilter.VIDEOS_ONLY -> videoCount > 0
        }
    }

    fun filteredSizeMb(filter: MediaFilter): Float {
        return photos.filter {
            when (filter) {
                MediaFilter.ALL -> true
                MediaFilter.PHOTOS_ONLY -> it.mediaType == MediaType.IMAGE
                MediaFilter.VIDEOS_ONLY -> it.mediaType == MediaType.VIDEO
            }
        }.sumOf { it.sizeMb.toDouble() }.toFloat()
    }

    fun filteredPreviewItem(filter: MediaFilter): PhotoItem? {
        return when (filter) {
            MediaFilter.ALL -> photos.firstOrNull()
            MediaFilter.PHOTOS_ONLY -> photos.firstOrNull { it.mediaType == MediaType.IMAGE } ?: photos.firstOrNull()
            MediaFilter.VIDEOS_ONLY -> photos.firstOrNull { it.mediaType == MediaType.VIDEO } ?: photos.firstOrNull()
        }
    }
}

data class Achievement(
    val id: String,
    val number: Int,
    val category: AchievementCategory,
    val title: String,
    val badgeLabel: String,
    val incentiveMessage: String,
    val requirementText: String,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1,
    val unlocked: Boolean = false
) {
    val progressFraction: Float
        get() = if (progressTarget <= 0) 1f else (progressCurrent.toFloat() / progressTarget.toFloat()).coerceIn(0f, 1f)
}
