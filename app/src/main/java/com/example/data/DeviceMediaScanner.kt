package com.example.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.content.ContextCompat
import com.example.model.FolderIconType
import com.example.model.MediaType
import com.example.model.PhoneFolder
import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object DeviceMediaScanner {

    fun requiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun hasMediaPermissions(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val fullImages = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                val fullVideos = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
                val partialVisual = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED
                (fullImages && fullVideos) || partialVisual
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val fullImages = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                val fullVideos = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
                fullImages || fullVideos
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    suspend fun scanAllMultimediaFolders(context: Context): List<PhoneFolder> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<PhotoItem>()
        allItems.addAll(queryImages(context))
        allItems.addAll(queryVideos(context))

        // Sort newest first inside each folder
        val grouped = allItems
            .sortedByDescending { it.dateAddedSeconds }
            .groupBy { it.folderId }

        grouped.map { (bucketId, items) ->
            val first = items.first()
            val folderName = first.folderName.ifBlank { "Multimedia" }
            val pathLabel = first.relativePath.ifBlank { "/storage/emulated/0/$folderName" }
            PhoneFolder(
                id = bucketId,
                name = folderName,
                pathLabel = pathLabel,
                iconType = inferFolderIcon(folderName, pathLabel, items),
                photos = items
            )
        }.sortedWith(
            compareByDescending<PhoneFolder> { it.photos.size }
                .thenByDescending { it.totalSizeMb }
        )
    }

    private fun queryImages(context: Context): List<PhotoItem> {
        val items = mutableListOf<PhotoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val fileName = if (nameCol >= 0 && !cursor.isNull(nameCol)) {
                        cursor.getString(nameCol)
                    } else "IMG_$mediaId.jpg"
                    val bucketId = if (bucketIdCol >= 0 && !cursor.isNull(bucketIdCol)) {
                        cursor.getString(bucketIdCol)
                    } else "bucket_default"
                    val bucketName = if (bucketNameCol >= 0 && !cursor.isNull(bucketNameCol)) {
                        cursor.getString(bucketNameCol)
                    } else "Cámara"
                    val sizeBytes = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) {
                        cursor.getLong(sizeCol).coerceAtLeast(0L)
                    } else 0L
                    if (sizeBytes <= 0L) continue

                    val dateSec = if (dateCol >= 0 && !cursor.isNull(dateCol)) {
                        cursor.getLong(dateCol)
                    } else 0L
                    val w = if (widthCol >= 0 && !cursor.isNull(widthCol)) cursor.getInt(widthCol) else 0
                    val h = if (heightCol >= 0 && !cursor.isNull(heightCol)) cursor.getInt(heightCol) else 0
                    val mime = if (mimeCol >= 0 && !cursor.isNull(mimeCol)) {
                        cursor.getString(mimeCol)
                    } else "image/jpeg"
                    val relPath = if (relPathCol >= 0 && !cursor.isNull(relPathCol)) {
                        cursor.getString(relPathCol)
                    } else "DCIM/$bucketName/"

                    val contentUri = ContentUris.withAppendedId(collection, mediaId)
                    val sizeMb = ((sizeBytes.toDouble() / (1024.0 * 1024.0)) * 100.0).roundToInt().coerceAtLeast(1) / 100f
                    val dateLabel = if (dateSec > 0L) {
                        dateFormat.format(Date(dateSec * 1000L))
                    } else "Galería del dispositivo"
                    val resLabel = if (w > 0 && h > 0) "${w}×${h}" else "Foto"

                    val smartTag = PlaceholderAiCleanupRepository.classifySmartPhotoTag(
                        fileName = fileName,
                        folderName = bucketName,
                        relativePath = relPath,
                        width = w,
                        height = h,
                        sizeBytes = sizeBytes,
                        mimeType = mime
                    )

                    items.add(
                        PhotoItem(
                            id = "img_$mediaId",
                            mediaStoreId = mediaId,
                            title = fileName.substringBeforeLast('.'),
                            fileName = fileName,
                            folderId = "bucket_$bucketId",
                            folderName = bucketName,
                            relativePath = relPath,
                            sizeMb = sizeMb,
                            sizeBytes = sizeBytes,
                            dateAddedSeconds = dateSec,
                            dateTaken = dateLabel,
                            resolution = resLabel,
                            mimeType = mime,
                            mediaType = MediaType.IMAGE,
                            durationMillis = 0L,
                            contentUri = contentUri.toString(),
                            aiCategoryTag = smartTag
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission or MediaStore query exception
        }
        return items
    }

    private fun queryVideos(context: Context): List<PhotoItem> {
        val items = mutableListOf<PhotoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val fileName = if (nameCol >= 0 && !cursor.isNull(nameCol)) {
                        cursor.getString(nameCol)
                    } else "VID_$mediaId.mp4"
                    val bucketId = if (bucketIdCol >= 0 && !cursor.isNull(bucketIdCol)) {
                        cursor.getString(bucketIdCol)
                    } else "bucket_video"
                    val bucketName = if (bucketNameCol >= 0 && !cursor.isNull(bucketNameCol)) {
                        cursor.getString(bucketNameCol)
                    } else "Videos"
                    val sizeBytes = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) {
                        cursor.getLong(sizeCol).coerceAtLeast(0L)
                    } else 0L
                    if (sizeBytes <= 0L) continue

                    val dateSec = if (dateCol >= 0 && !cursor.isNull(dateCol)) {
                        cursor.getLong(dateCol)
                    } else 0L
                    val w = if (widthCol >= 0 && !cursor.isNull(widthCol)) cursor.getInt(widthCol) else 0
                    val h = if (heightCol >= 0 && !cursor.isNull(heightCol)) cursor.getInt(heightCol) else 0
                    val durationMs = if (durationCol >= 0 && !cursor.isNull(durationCol)) {
                        cursor.getLong(durationCol).coerceAtLeast(0L)
                    } else 0L
                    val mime = if (mimeCol >= 0 && !cursor.isNull(mimeCol)) {
                        cursor.getString(mimeCol)
                    } else "video/mp4"
                    val relPath = if (relPathCol >= 0 && !cursor.isNull(relPathCol)) {
                        cursor.getString(relPathCol)
                    } else "Movies/$bucketName/"

                    val contentUri = ContentUris.withAppendedId(collection, mediaId)
                    val sizeMb = ((sizeBytes.toDouble() / (1024.0 * 1024.0)) * 100.0).roundToInt().coerceAtLeast(1) / 100f
                    val dateLabel = if (dateSec > 0L) {
                        dateFormat.format(Date(dateSec * 1000L))
                    } else "Video del dispositivo"
                    val resLabel = if (w > 0 && h > 0) "${w}×${h} · Video" else "Video"
                    val smartTag = PlaceholderAiCleanupRepository.classifySmartPhotoTag(
                        fileName = fileName,
                        folderName = bucketName,
                        relativePath = relPath,
                        width = w,
                        height = h,
                        sizeBytes = sizeBytes,
                        mimeType = mime
                    )

                    items.add(
                        PhotoItem(
                            id = "vid_$mediaId",
                            mediaStoreId = mediaId,
                            title = fileName.substringBeforeLast('.'),
                            fileName = fileName,
                            folderId = "bucket_$bucketId",
                            folderName = bucketName,
                            relativePath = relPath,
                            sizeMb = sizeMb,
                            sizeBytes = sizeBytes,
                            dateAddedSeconds = dateSec,
                            dateTaken = dateLabel,
                            resolution = resLabel,
                            mimeType = mime,
                            mediaType = MediaType.VIDEO,
                            durationMillis = durationMs,
                            contentUri = contentUri.toString(),
                            aiCategoryTag = smartTag
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission or MediaStore query exception
        }
        return items
    }

    private fun inferFolderIcon(
        name: String,
        path: String,
        items: List<PhotoItem>
    ): FolderIconType {
        val lower = "$name $path".lowercase(Locale.ROOT)
        return when {
            lower.contains("camera") || lower.contains("dcim") || lower.contains("cámara") -> FolderIconType.CAMERA
            lower.contains("whatsapp") || lower.contains("telegram") -> FolderIconType.WHATSAPP
            lower.contains("screenshot") || lower.contains("captura") -> FolderIconType.SCREENSHOTS
            lower.contains("download") || lower.contains("descarga") -> FolderIconType.DOWNLOADS
            lower.contains("movie") || lower.contains("video") || items.all { it.isVideo } -> FolderIconType.VIDEOS
            lower.contains("instagram") || lower.contains("tiktok") || lower.contains("facebook") -> FolderIconType.SOCIAL
            else -> FolderIconType.DEVICE_FOLDER
        }
    }

    suspend fun loadThumbnailBitmap(
        context: Context,
        contentUriString: String,
        widthPx: Int = 720,
        heightPx: Int = 960
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(contentUriString)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(widthPx, heightPx), null)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    data class DeviceStorageInfo(
        val totalGb: Float,
        val usedGb: Float,
        val freeGb: Float
    )

    fun getDeviceStorageInfo(): DeviceStorageInfo {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes.toDouble()
            val availBytes = stat.availableBytes.toDouble()
            val usedBytes = (totalBytes - availBytes).coerceAtLeast(0.0)
            val gbDivisor = 1024.0 * 1024.0 * 1024.0
            val totalGb = ((totalBytes / gbDivisor) * 10.0).roundToInt() / 10f
            val usedGb = ((usedBytes / gbDivisor) * 10.0).roundToInt() / 10f
            val freeGb = ((availBytes / gbDivisor) * 10.0).roundToInt() / 10f
            DeviceStorageInfo(
                totalGb = totalGb.coerceAtLeast(1f),
                usedGb = usedGb,
                freeGb = freeGb
            )
        } catch (_: Exception) {
            DeviceStorageInfo(totalGb = 64.0f, usedGb = 32.0f, freeGb = 32.0f)
        }
    }
}
