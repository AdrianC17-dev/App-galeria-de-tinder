package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DuplicateClusterType(val title: String) {
    DUPLICATE_OR_BURST("Duplicados y Ráfagas"),
    LOW_QUALITY_OR_BLUR("Baja Calidad / Borrosas"),
    LARGE_REDUNDANT("Capturas y Archivos Pesados")
}

data class DuplicateCluster(
    val id: String,
    val title: String,
    val subtitle: String,
    val aiReasonLabel: String,
    val clusterType: DuplicateClusterType,
    val bestPhotoToKeep: PhotoItem?,
    val suggestedToDelete: List<PhotoItem>
) {
    val allItems: List<PhotoItem>
        get() = listOfNotNull(bestPhotoToKeep) + suggestedToDelete

    val reclaimableMb: Float
        get() = ((suggestedToDelete.sumOf { it.sizeMb.toDouble() } * 100.0).roundToInt() / 100f)
}

data class AiCleanupAnalysisResult(
    val isPlaceholderAi: Boolean,
    val firebaseRequiredNotice: String,
    val aiSummaryBanner: String,
    val clusters: List<DuplicateCluster>,
    val totalAnalyzedCount: Int
) {
    val totalSuggestedCount: Int
        get() = clusters.sumOf { it.suggestedToDelete.size }

    val totalReclaimableMb: Float
        get() = ((clusters.sumOf { it.reclaimableMb.toDouble() } * 100.0).roundToInt() / 100f)
}

/**
 * Single repository interface through which all AI Cleanup, Auto-Categorization, and Duplicate Suggestion requests are routed.
 */
interface AiCleanupRepository {
    suspend fun analyzeDuplicatesAndLowQuality(
        context: Context,
        items: List<PhotoItem>
    ): AiCleanupAnalysisResult

    fun autoCategorizeItem(
        fileName: String,
        folderName: String,
        relativePath: String,
        width: Int = 0,
        height: Int = 0,
        sizeBytes: Long = 0L,
        mimeType: String = "image/jpeg"
    ): com.example.model.SmartPhotoTag
}

/**
 * Placeholder + on-device perceptual heuristic implementation used when Firebase AI Logic is not enabled.
 * Returns clearly labeled sample AI explanations alongside grouped duplicate/low-quality candidates
 * and auto-categorizes photos into ('Screenshots', 'Travel', 'Food', 'Documents', 'Social y Memes', 'Cámara y Retratos').
 */
class PlaceholderAiCleanupRepository : AiCleanupRepository {

    override fun autoCategorizeItem(
        fileName: String,
        folderName: String,
        relativePath: String,
        width: Int,
        height: Int,
        sizeBytes: Long,
        mimeType: String
    ): com.example.model.SmartPhotoTag {
        return classifySmartPhotoTag(
            fileName = fileName,
            folderName = folderName,
            relativePath = relativePath,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            mimeType = mimeType
        )
    }

    companion object {
        fun classifySmartPhotoTag(
            fileName: String,
            folderName: String,
            relativePath: String,
            width: Int = 0,
            height: Int = 0,
            sizeBytes: Long = 0L,
            mimeType: String = "image/jpeg"
        ): com.example.model.SmartPhotoTag {
            val combined = "$fileName $folderName $relativePath".lowercase(Locale.ROOT)
            val isPortraitScreenAspect = width > 0 && height > 0 &&
                (height.toFloat() / width.toFloat()) in 1.95f..2.35f &&
                mimeType.contains("png", ignoreCase = true)

            return when {
                combined.contains("screenshot") ||
                    combined.contains("captura") ||
                    combined.contains("screen_") ||
                    isPortraitScreenAspect -> com.example.model.SmartPhotoTag.SCREENSHOTS

                combined.contains("doc") ||
                    combined.contains("scan") ||
                    combined.contains("recibo") ||
                    combined.contains("factura") ||
                    combined.contains("pdf") ||
                    combined.contains("ticket") ||
                    combined.contains("id_") ||
                    combined.contains("nota") ||
                    combined.contains("office") ||
                    combined.contains("camscanner") -> com.example.model.SmartPhotoTag.DOCUMENTS

                combined.contains("food") ||
                    combined.contains("comida") ||
                    combined.contains("cena") ||
                    combined.contains("almuerzo") ||
                    combined.contains("restaurante") ||
                    combined.contains("cafe") ||
                    combined.contains("dish") ||
                    combined.contains("recipe") ||
                    combined.contains("menu") -> com.example.model.SmartPhotoTag.FOOD

                combined.contains("travel") ||
                    combined.contains("viaje") ||
                    combined.contains("vacacion") ||
                    combined.contains("trip") ||
                    combined.contains("playa") ||
                    combined.contains("beach") ||
                    combined.contains("montaña") ||
                    combined.contains("pano") ||
                    combined.contains("landscape") ||
                    (width > height && width >= 2500) -> com.example.model.SmartPhotoTag.TRAVEL

                else -> {
                    val seed = kotlin.math.abs(fileName.hashCode() + (sizeBytes % 997L).toInt()) % 10
                    when (seed) {
                        0, 1 -> com.example.model.SmartPhotoTag.TRAVEL
                        2, 3 -> com.example.model.SmartPhotoTag.FOOD
                        4 -> com.example.model.SmartPhotoTag.DOCUMENTS
                        5 -> com.example.model.SmartPhotoTag.SCREENSHOTS
                        else -> com.example.model.SmartPhotoTag.CAMERA_PORTRAITS
                    }
                }
            }
        }
    }

    override suspend fun analyzeDuplicatesAndLowQuality(
        context: Context,
        items: List<PhotoItem>
    ): AiCleanupAnalysisResult = withContext(Dispatchers.Default) {
        delay(420) // Brief feedback transition for analysis UI

        val clusters = mutableListOf<DuplicateCluster>()
        val usedIds = mutableSetOf<String>()

        // 1. Visual Perceptual Hash (8x8 dHash) + Timestamp/Size similarity grouping for Duplicates & Bursts
        val imageItems = items.filter { !it.isVideo }
        val hashes = mutableMapOf<String, Long>()
        val sharpnessScores = mutableMapOf<String, Float>()

        imageItems.take(60).forEach { photo ->
            val bmp = DeviceMediaScanner.loadThumbnailBitmap(
                context = context,
                contentUriString = photo.contentUri,
                widthPx = 64,
                heightPx = 64
            )
            if (bmp != null) {
                hashes[photo.id] = computeDifferenceHash(bmp)
                sharpnessScores[photo.id] = computeSharpnessScore(bmp)
            }
        }

        // Group visually similar or near-identical timestamp/size photos
        val sortedImages = imageItems.sortedByDescending { it.dateAddedSeconds }
        for (i in sortedImages.indices) {
            val anchor = sortedImages[i]
            if (anchor.id in usedIds) continue

            val similarGroup = mutableListOf(anchor)
            val anchorHash = hashes[anchor.id]

            for (j in (i + 1) until sortedImages.size) {
                val candidate = sortedImages[j]
                if (candidate.id in usedIds) continue

                val candidateHash = hashes[candidate.id]
                val hammingDistance = if (anchorHash != null && candidateHash != null) {
                    java.lang.Long.bitCount(anchorHash xor candidateHash)
                } else 64

                val timeDiffSec = abs(anchor.dateAddedSeconds - candidate.dateAddedSeconds)
                val sizeDiffRatio = if (anchor.sizeBytes > 0L) {
                    abs(anchor.sizeBytes - candidate.sizeBytes).toFloat() / anchor.sizeBytes.toFloat()
                } else 1f
                val samePrefix = anchor.title.take(8).equals(candidate.title.take(8), ignoreCase = true)

                val isDuplicateOrBurst = hammingDistance <= 12 ||
                    (timeDiffSec in 0..45 && anchor.folderId == candidate.folderId) ||
                    (sizeDiffRatio < 0.04f && anchor.folderId == candidate.folderId) ||
                    (samePrefix && anchor.folderId == candidate.folderId && sizeDiffRatio < 0.18f)

                if (isDuplicateOrBurst) {
                    similarGroup.add(candidate)
                }
            }

            if (similarGroup.size >= 2) {
                // Pick the sharpest or highest-resolution/largest file as "Mejor toma" to keep
                val ranked = similarGroup.sortedWith(
                    compareByDescending<PhotoItem> { sharpnessScores[it.id] ?: 50f }
                        .thenByDescending { it.sizeBytes }
                )
                val bestToKeep = ranked.first()
                val duplicatesToDelete = ranked.drop(1)

                similarGroup.forEach { usedIds.add(it.id) }
                clusters.add(
                    DuplicateCluster(
                        id = "dup_cluster_${clusters.size + 1}",
                        title = "Grupo duplicado en ${bestToKeep.folderName}",
                        subtitle = "${similarGroup.size} fotos similares · Se conserva la más nítida",
                        aiReasonLabel = "[Respuesta de ejemplo IA] Ráfaga o encuadre repetido detectado",
                        clusterType = DuplicateClusterType.DUPLICATE_OR_BURST,
                        bestPhotoToKeep = bestToKeep,
                        suggestedToDelete = duplicatesToDelete
                    )
                )
            }
        }

        // 2. Low-Quality / Blurry or Low-Resolution Photos Group
        val remainingImages = imageItems.filterNot { it.id in usedIds }
        val lowQualityCandidates = remainingImages.filter { photo ->
            val sharpness = sharpnessScores[photo.id] ?: 30f
            val isLowRes = photo.resolution.let { res ->
                val parts = res.split("×", "x")
                val w = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 1920
                val h = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1080
                (w * h) < (960 * 720)
            }
            sharpness < 14f || isLowRes
        }.take(12)

        if (lowQualityCandidates.isNotEmpty()) {
            lowQualityCandidates.forEach { usedIds.add(it.id) }
            clusters.add(
                DuplicateCluster(
                    id = "low_quality_cluster",
                    title = "Fotos de Baja Calidad o Borrosas",
                    subtitle = "${lowQualityCandidates.size} archivos con baja nitidez o baja resolución",
                    aiReasonLabel = "[Respuesta de ejemplo IA] Nitidez inferior al umbral recomendado",
                    clusterType = DuplicateClusterType.LOW_QUALITY_OR_BLUR,
                    bestPhotoToKeep = null,
                    suggestedToDelete = lowQualityCandidates
                )
            )
        }

        // 3. Redundant Screenshots / Heavy Files fallback so the user always gets actionable groups when folders have items
        val remainingAll = items.filterNot { it.id in usedIds }
        val screenshotOrHeavy = remainingAll
            .filter {
                val lower = "${it.folderName} ${it.fileName}".lowercase(Locale.ROOT)
                lower.contains("screenshot") || lower.contains("captura") || lower.contains("whatsapp") || it.sizeMb >= 4.0f
            }
            .sortedByDescending { it.sizeMb }
            .take(8)

        if (screenshotOrHeavy.isNotEmpty()) {
            screenshotOrHeavy.forEach { usedIds.add(it.id) }
            clusters.add(
                DuplicateCluster(
                    id = "screenshots_heavy_cluster",
                    title = "Capturas y Archivos de Alto Peso",
                    subtitle = "${screenshotOrHeavy.size} archivos sugeridos para liberar espacio rápido",
                    aiReasonLabel = "[Respuesta de ejemplo IA] Capturas temporales y archivos pesados",
                    clusterType = DuplicateClusterType.LARGE_REDUNDANT,
                    bestPhotoToKeep = null,
                    suggestedToDelete = screenshotOrHeavy
                )
            )
        }

        // If folders had very few files and none matched above, group the secondary items if at least 2 exist
        if (clusters.isEmpty() && items.size >= 2) {
            val sortedBySize = items.sortedByDescending { it.sizeBytes }
            val keepBest = sortedBySize.first()
            val suggestRest = sortedBySize.drop(1).take(6)
            clusters.add(
                DuplicateCluster(
                    id = "fallback_sample_cluster",
                    title = "Sugerencia Rápida de Limpieza",
                    subtitle = "${suggestRest.size + 1} archivos analizados en tus carpetas seleccionadas",
                    aiReasonLabel = "[Respuesta de ejemplo IA] Agrupación de muestra para limpieza rápida",
                    clusterType = DuplicateClusterType.DUPLICATE_OR_BURST,
                    bestPhotoToKeep = keepBest,
                    suggestedToDelete = suggestRest
                )
            )
        }

        val totalSug = clusters.sumOf { it.suggestedToDelete.size }
        val totalMb = String.format(Locale.US, "%.2f", clusters.sumOf { it.reclaimableMb.toDouble() })

        AiCleanupAnalysisResult(
            isPlaceholderAi = true,
            firebaseRequiredNotice = "Aviso: Las funciones de IA en la nube (Gemini) requieren habilitar Firebase AI Logic. Mostrando resultados de ejemplo con agrupación local.",
            aiSummaryBanner = "[Modo Ejemplo IA] Se analizaron ${items.size} archivos y se detectaron $totalSug candidatos ($totalMb MB recuperables) entre duplicados y baja calidad.",
            clusters = clusters,
            totalAnalyzedCount = items.size
        )
    }

    /**
     * Computes a 64-bit difference hash (dHash) from a small bitmap for fast visual duplicate comparison.
     */
    private fun computeDifferenceHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        var bitIndex = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftPixel = scaled.getPixel(x, y)
                val rightPixel = scaled.getPixel(x + 1, y)
                val leftLum = Color.red(leftPixel) * 0.299f + Color.green(leftPixel) * 0.587f + Color.blue(leftPixel) * 0.114f
                val rightLum = Color.red(rightPixel) * 0.299f + Color.green(rightPixel) * 0.587f + Color.blue(rightPixel) * 0.114f
                if (leftLum > rightLum) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return hash
    }

    /**
     * Estimates perceptual image sharpness using average local gradient magnitude (blur detection).
     */
    private fun computeSharpnessScore(bitmap: Bitmap): Float {
        val w = bitmap.width.coerceAtMost(48)
        val h = bitmap.height.coerceAtMost(48)
        if (w < 4 || h < 4) return 25f
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        var sumGrad = 0f
        var count = 0
        for (y in 1 until (h - 1)) {
            for (x in 1 until (w - 1)) {
                val c = scaled.getPixel(x, y)
                val r = scaled.getPixel(x + 1, y)
                val b = scaled.getPixel(x, y + 1)
                val lumC = Color.red(c) * 0.3f + Color.green(c) * 0.59f + Color.blue(c) * 0.11f
                val lumR = Color.red(r) * 0.3f + Color.green(r) * 0.59f + Color.blue(r) * 0.11f
                val lumB = Color.red(b) * 0.3f + Color.green(b) * 0.59f + Color.blue(b) * 0.11f
                sumGrad += abs(lumC - lumR) + abs(lumC - lumB)
                count++
            }
        }
        return if (count > 0) sumGrad / count.toFloat() else 25f
    }
}
