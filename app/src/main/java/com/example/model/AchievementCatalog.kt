package com.example.model

import kotlin.math.roundToInt

data class UserCleaningStats(
    val totalMarkedOrDeletedCount: Int = 0,
    val totalConfirmedDeletedCount: Int = 0,
    val totalFreedAndQueuedMb: Float = 0f,
    val totalVideosMarkedOrDeleted: Int = 0,
    val totalPhotosMarkedOrDeleted: Int = 0,
    val maxSingleFileMbDeleted: Float = 0f,
    val maxVideoDurationSecDeleted: Int = 0,
    val currentDeleteStreak: Int = 0,
    val maxDeleteStreak: Int = 0,
    val currentKeepStreak: Int = 0,
    val maxKeepStreak: Int = 0,
    val totalKeptCount: Int = 0,
    val totalRescuedInReviewCount: Int = 0,
    val totalUndoUsedCount: Int = 0,
    val foldersScannedCount: Int = 0,
    val maxFoldersSelectedAtOnce: Int = 0,
    val completedSessionsCount: Int = 0,
    val cleanedCameraItem: Boolean = false,
    val cleanedWhatsAppItem: Boolean = false,
    val cleanedScreenshotsItem: Boolean = false,
    val cleanedDownloadsItem: Boolean = false,
    val cleanedMoviesItem: Boolean = false
)

object AchievementCatalog {

    private data class Spec(
        val id: String,
        val category: AchievementCategory,
        val title: String,
        val badgeLabel: String,
        val incentiveMessage: String,
        val requirementText: String,
        val target: Int,
        val extractor: (UserCleaningStats, Int) -> Int
    )

    private val specs: List<Spec> = buildList {
        // 1. CATEGORÍA: CANTIDAD ELIMINADA (25 Logros: #1 al #25)
        val deleteMilestones = listOf(
            Triple(1, "Primer Paso Ligero", "¡Tu primer archivo en cola de limpieza! Cada gran cambio empieza con un swipe."),
            Triple(3, "Trío de Limpieza", "¡3 archivos innecesarios menos! Tu galería empieza a respirar."),
            Triple(5, "Mano Rápida", "¡5 archivos marcados! Estás tomando ritmo de curador experto."),
            Triple(8, "Octeto Depurador", "¡8 archivos detectados! Menos desorden visual en tu carrete."),
            Triple(10, "Decena Impecable", "¡10 archivos fuera! Ya se nota la diferencia en tus carpetas."),
            Triple(15, "Quincena de Espacio", "¡15 archivos marcados! Tu memoria interna te lo agradece."),
            Triple(20, "Veintena Veloz", "¡20 archivos depurados! Cero piedad con las tomas repetidas."),
            Triple(25, "Cuarto de Centenar", "¡25 archivos en limpieza! Un cuarto de centenar de espacio recuperado."),
            Triple(30, "Treinta y Contando", "¡30 archivos innecesarios fuera del camino!"),
            Triple(40, "Cuarentena de Bytes", "¡40 archivos marcados! Tu galería luce cada vez más selecta."),
            Triple(50, "Medio Centenar", "¡50 archivos! Has superado la barrera de los 50 descartes."),
            Triple(60, "Sesenta Sin Dudar", "¡60 archivos clasificados para borrar! Eficiencia pura."),
            Triple(75, "Tres Cuartos de Cien", "¡75 archivos! Estás a un paso del club de los 100."),
            Triple(90, "Noventa Depurados", "¡90 archivos marcados! Tu teléfono se siente como nuevo."),
            Triple(100, "Centurión de la Galería", "¡100 archivos innecesarios eliminados! Un hito legendario."),
            Triple(125, "Ciento Veinticinco", "¡125 archivos fuera! Nada detiene tu limpieza."),
            Triple(150, "Ciento Cincuenta Libres", "¡150 archivos! Has liberado el equivalente a un álbum entero."),
            Triple(175, "Maestro del Descarte", "¡175 archivos depurados con precisión quirúrgica!"),
            Triple(200, "Doble Centenar", "¡200 archivos eliminados! Tu almacenamiento brilla."),
            Triple(250, "Cuarto de Millar", "¡250 archivos! Eres oficialmente enemigo del almacenamiento lleno."),
            Triple(300, "Espartano del Espacio", "¡300 archivos innecesarios vencidos con un dedo!"),
            Triple(350, "Purificador Digital", "¡350 archivos! Solo las mejores fotos sobreviven."),
            Triple(400, "Cuatrocientos al Vacío", "¡400 archivos! Tu teléfono corre más ligero que nunca."),
            Triple(450, "Élite de la Curaduría", "¡450 archivos depurados! A un paso de los 500."),
            Triple(500, "Leyenda de los 500", "¡500 archivos eliminados! Dominio absoluto de tu galería.")
        )
        deleteMilestones.forEach { (count, title, msg) ->
            add(
                Spec(
                    id = "del_count_$count",
                    category = AchievementCategory.CANTIDAD,
                    title = title,
                    badgeLabel = "$count ARCHIVOS",
                    incentiveMessage = msg,
                    requirementText = "Marca o elimina $count archivos multimedia",
                    target = count,
                    extractor = { stats, _ -> stats.totalMarkedOrDeletedCount }
                )
            )
        }

        // 2. CATEGORÍA: ESPACIO LIBERADO EN MB / GB (20 Logros: #26 al #45)
        val mbMilestones = listOf(
            Triple(2, "Primeros Megabytes", "¡2 MB listos para liberar! Espacio para nuevas capturas."),
            Triple(5, "Bolsa de 5 MB", "¡5 MB recuperados! Suficiente para un par de fotos en alta calidad."),
            Triple(10, "Diez Megas Libres", "¡10 MB! Ya equivale a una aplicación ligera entera."),
            Triple(20, "Veinte Megas de Aire", "¡20 MB en cola de liberación! Sigue sumando espacio."),
            Triple(35, "Treinta y Cinco MB", "¡35 MB! Espacio para una canción en calidad estudio."),
            Triple(50, "Medio Centenar de MB", "¡50 MB recuperados! Tu almacenamiento empieza a notarlo."),
            Triple(75, "Setenta y Cinco MB", "¡75 MB listos para liberar! Casi llegas a los 100 MB."),
            Triple(100, "Barrera de los 100 MB", "¡100 MB liberados! Espacio para decenas de fotografías nuevas."),
            Triple(150, "Ciento Cincuenta MB", "¡150 MB! Ya puedes grabar un buen clip de video sin alertas."),
            Triple(200, "Doscientos Megas", "¡200 MB fuera! La memoria de tu teléfono respira profundo."),
            Triple(300, "Trescientos MB Libres", "¡300 MB recuperados! Un impulso enorme para tu dispositivo."),
            Triple(400, "Cuatrocientos MB", "¡400 MB! Casi medio Gigabyte de basura digital eliminada."),
            Triple(500, "Medio Gigabyte (500 MB)", "¡500 MB! Has recuperado medio Gigabyte completo de memoria."),
            Triple(750, "Tres Cuartos de Giga", "¡750 MB! Estás rozando el Gigabyte entero liberado."),
            Triple(1024, "¡Club del Gigabyte (1 GB)!", "¡1 GB completo liberado! Espacio para cientos de recuerdos nuevos."),
            Triple(1536, "Giga y Medio (1.5 GB)", "¡1.5 GB recuperados! Una limpieza digna de aplauso."),
            Triple(2048, "Dos Gigabytes Libres", "¡2 GB de almacenamiento libre! Tu móvil vuela."),
            Triple(3072, "Tres Gigabytes de Oro", "¡3 GB recuperados! Espacio para películas y videos 4K."),
            Triple(5120, "Cinco Gigabytes Titanio", "¡5 GB liberados! Has rescatado una tajada enorme de tu memoria."),
            Triple(10240, "Diez Gigabytes Supremos", "¡10 GB liberados! Récord colosal de ahorro de almacenamiento.")
        )
        mbMilestones.forEach { (mb, title, msg) ->
            val label = if (mb >= 1024) "${mb / 1024f} GB" else "$mb MB"
            add(
                Spec(
                    id = "space_mb_$mb",
                    category = AchievementCategory.ESPACIO,
                    title = title,
                    badgeLabel = "+$label",
                    incentiveMessage = msg,
                    requirementText = "Acumula $label de espacio liberado o en cola",
                    target = mb,
                    extractor = { stats, _ -> stats.totalFreedAndQueuedMb.roundToInt() }
                )
            )
        }

        // 3. CATEGORÍA: VIDEOS Y ARCHIVOS PESADOS (15 Logros: #46 al #60)
        val videoCounts = listOf(
            Triple(1, "Corte de Edición", "¡Primer video marcado para borrar! Los videos son los que más espacio ocupan."),
            Triple(3, "Trilogía de Clips", "¡3 videos en limpieza! Estás liberando megabytes a toda velocidad."),
            Triple(5, "Director de Montaje", "¡5 videos innecesarios detectados! Gran ahorro de espacio."),
            Triple(10, "Diez Clips Menos", "¡10 videos fuera! Eso equivale a cientos de fotos en memoria."),
            Triple(20, "Sala de Proyección Limpia", "¡20 videos depurados! Tu carpeta de videos está impecable."),
            Triple(35, "Productor Minimalista", "¡35 videos eliminados! Solo conservas las tomas maestras."),
            Triple(50, "Rey del Metraje", "¡50 videos depurados! Dominio total del contenido de video.")
        )
        videoCounts.forEach { (vCount, title, msg) ->
            add(
                Spec(
                    id = "video_count_$vCount",
                    category = AchievementCategory.VIDEOS,
                    title = title,
                    badgeLabel = "$vCount VIDEOS",
                    incentiveMessage = msg,
                    requirementText = "Marca o elimina $vCount videos de tu galería",
                    target = vCount,
                    extractor = { stats, _ -> stats.totalVideosMarkedOrDeleted }
                )
            )
        }

        val heavyFileThresholds = listOf(
            Triple(5, "Caza de Peso Medio", "¡Detectaste un archivo de más de 5 MB!"),
            Triple(10, "Cazador de Gigantes (10 MB)", "¡Un solo archivo de más de 10 MB menos en tu memoria!"),
            Triple(25, "Peso Pesado (25 MB)", "¡Eliminaste un archivo de más de 25 MB de un solo golpe!"),
            Triple(50, "Coloso Digital (50 MB)", "¡Increíble! Un solo archivo superaba los 50 MB."),
            Triple(100, "Leviatán de Memoria (100 MB)", "¡Más de 100 MB liberados en un único archivo multimedia!")
        )
        heavyFileThresholds.forEach { (mb, title, msg) ->
            add(
                Spec(
                    id = "heavy_file_$mb",
                    category = AchievementCategory.VIDEOS,
                    title = title,
                    badgeLabel = "ARCHIVO >$mb MB",
                    incentiveMessage = msg,
                    requirementText = "Marca o elimina un archivo individual de al menos $mb MB",
                    target = mb,
                    extractor = { stats, _ -> stats.maxSingleFileMbDeleted.roundToInt() }
                )
            )
        }

        val videoDurations = listOf(
            Triple(15, "Clip Corto Depurado", "¡Eliminaste un video de más de 15 segundos!"),
            Triple(60, "Minuto de Metraje Libre", "¡Un video de más de 1 minuto menos ocupando memoria!"),
            Triple(180, "Cortometraje Eliminado", "¡Liberaste el espacio de un video de más de 3 minutos!")
        )
        videoDurations.forEach { (secs, title, msg) ->
            add(
                Spec(
                    id = "video_dur_$secs",
                    category = AchievementCategory.VIDEOS,
                    title = title,
                    badgeLabel = "VIDEO >${secs}s",
                    incentiveMessage = msg,
                    requirementText = "Elimina un video con duración mayor a $secs segundos",
                    target = secs,
                    extractor = { stats, _ -> stats.maxVideoDurationSecDeleted }
                )
            )
        }

        // 4. CATEGORÍA: RACHAS Y CURADURÍA (15 Logros: #61 al #75)
        val deleteStreaks = listOf(
            Triple(3, "Racha Implacable x3", "¡3 descartes seguidos sin titubear!"),
            Triple(5, "Combo de Limpieza x5", "¡5 archivos seguidos enviados a la papelera!"),
            Triple(8, "Tormenta de Swipes x8", "¡8 descartes consecutivos! Ritmo imparable."),
            Triple(12, "Ráfaga Depuradora x12", "¡12 descartes seguidos! Sabes exactamente qué no necesitas."),
            Triple(20, "Imparable x20", "¡20 descartes en racha! Limpieza ultrasónica.")
        )
        deleteStreaks.forEach { (streak, title, msg) ->
            add(
                Spec(
                    id = "del_streak_$streak",
                    category = AchievementCategory.RACHAS_CURADURIA,
                    title = title,
                    badgeLabel = "RACHA x$streak",
                    incentiveMessage = msg,
                    requirementText = "Envía $streak archivos seguidos a la lista de eliminación",
                    target = streak,
                    extractor = { stats, _ -> stats.maxDeleteStreak }
                )
            )
        }

        val keptMilestones = listOf(
            Triple(1, "Guardián de Recuerdos", "¡Conservaste tu primera foto valiosa!"),
            Triple(5, "Cinco Joyas a Salvo", "¡5 recuerdos protegidos en tu galería!"),
            Triple(15, "Álbum Selecto (15)", "¡15 fotos y videos elegidos para quedarse!"),
            Triple(30, "Curador de Momentos (30)", "¡30 archivos conservados con criterio artístico!"),
            Triple(60, "Colección Dorada (60)", "¡60 recuerdos favoritos seleccionados!"),
            Triple(100, "Museo Personal (100)", "¡100 archivos conservados tras tu curaduría!")
        )
        keptMilestones.forEach { (keptCount, title, msg) ->
            add(
                Spec(
                    id = "kept_count_$keptCount",
                    category = AchievementCategory.RACHAS_CURADURIA,
                    title = title,
                    badgeLabel = "$keptCount CONSERVADOS",
                    incentiveMessage = msg,
                    requirementText = "Desliza a la derecha para conservar $keptCount archivos",
                    target = keptCount,
                    extractor = { stats, _ -> stats.totalKeptCount }
                )
            )
        }

        add(
            Spec(
                id = "rescue_1",
                category = AchievementCategory.RACHAS_CURADURIA,
                title = "Salvavidas de Último Segundo",
                badgeLabel = "RESCATE",
                incentiveMessage = "¡Salvaste un archivo desde la pantalla de verificación antes de borrarlo!",
                requirementText = "Quita 1 archivo de la lista de eliminación antes de confirmar",
                target = 1,
                extractor = { stats, _ -> stats.totalRescuedInReviewCount }
            )
        )
        add(
            Spec(
                id = "rescue_5",
                category = AchievementCategory.RACHAS_CURADURIA,
                title = "Ojo Crítico en Revisión",
                badgeLabel = "5 RESCATES",
                incentiveMessage = "¡Has rescatado 5 archivos durante la revisión previa!",
                requirementText = "Salva 5 archivos en la vista de verificación pre-borrado",
                target = 5,
                extractor = { stats, _ -> stats.totalRescuedInReviewCount }
            )
        )
        add(
            Spec(
                id = "undo_1",
                category = AchievementCategory.RACHAS_CURADURIA,
                title = "Viajero del Tiempo",
                badgeLabel = "DESHACER",
                incentiveMessage = "¡Usaste Deshacer para reconsiderar tu decisión a tiempo!",
                requirementText = "Usa el botón Deshacer durante una sesión de swipe",
                target = 1,
                extractor = { stats, _ -> stats.totalUndoUsedCount }
            )
        )
        add(
            Spec(
                id = "keep_streak_5",
                category = AchievementCategory.RACHAS_CURADURIA,
                title = "Amor por la Fotografía x5",
                badgeLabel = "RACHA LIKE x5",
                incentiveMessage = "¡5 archivos seguidos dignos de conservarse!",
                requirementText = "Conserva 5 archivos consecutivos deslizando a la derecha",
                target = 5,
                extractor = { stats, _ -> stats.maxKeepStreak }
            )
        )

        // 5. CATEGORÍA: CARPETAS MULTIMEDIA (15 Logros: #76 al #90)
        add(
            Spec(
                id = "folder_scan_1",
                category = AchievementCategory.CARPETAS,
                title = "Explorador del Dispositivo",
                badgeLabel = "ESCANEO ACTIVO",
                incentiveMessage = "¡Conectaste tu galería multimedia real del teléfono!",
                requirementText = "Escanea al menos 1 carpeta multimedia de tu teléfono",
                target = 1,
                extractor = { stats, _ -> stats.foldersScannedCount }
            )
        )
        add(
            Spec(
                id = "folder_scan_3",
                category = AchievementCategory.CARPETAS,
                title = "Rastreador de Álbumes",
                badgeLabel = "3 CARPETAS",
                incentiveMessage = "¡3 o más carpetas multimedia detectadas en tu dispositivo!",
                requirementText = "Detecta al menos 3 carpetas multimedia en tu dispositivo",
                target = 3,
                extractor = { stats, _ -> stats.foldersScannedCount }
            )
        )
        add(
            Spec(
                id = "folder_scan_6",
                category = AchievementCategory.CARPETAS,
                title = "Cartógrafo de Memoria",
                badgeLabel = "6 CARPETAS",
                incentiveMessage = "¡6 carpetas multimedia listas para tu análisis!",
                requirementText = "Detecta 6 o más carpetas multimedia en tu teléfono",
                target = 6,
                extractor = { stats, _ -> stats.foldersScannedCount }
            )
        )
        add(
            Spec(
                id = "multi_select_2",
                category = AchievementCategory.CARPETAS,
                title = "Doble Frente",
                badgeLabel = "2 ÁLBUMES",
                incentiveMessage = "¡Seleccionaste 2 o más carpetas para analizarlas juntas!",
                requirementText = "Selecciona al menos 2 carpetas simultáneamente",
                target = 2,
                extractor = { stats, _ -> stats.maxFoldersSelectedAtOnce }
            )
        )
        add(
            Spec(
                id = "multi_select_4",
                category = AchievementCategory.CARPETAS,
                title = "Operación Multicanal",
                badgeLabel = "4 ÁLBUMES",
                incentiveMessage = "¡Analizando 4 o más carpetas en una sola sesión!",
                requirementText = "Selecciona 4 o más carpetas simultáneamente",
                target = 4,
                extractor = { stats, _ -> stats.maxFoldersSelectedAtOnce }
            )
        )
        add(
            Spec(
                id = "clean_camera",
                category = AchievementCategory.CARPETAS,
                title = "Lente Depurado (Cámara)",
                badgeLabel = "DCIM / CÁMARA",
                incentiveMessage = "¡Depuraste archivos de tu carpeta de Cámara!",
                requirementText = "Marca para eliminar un archivo de la carpeta Cámara / DCIM",
                target = 1,
                extractor = { stats, _ -> if (stats.cleanedCameraItem) 1 else 0 }
            )
        )
        add(
            Spec(
                id = "clean_whatsapp",
                category = AchievementCategory.CARPETAS,
                title = "Adiós Memes Repetidos",
                badgeLabel = "WHATSAPP",
                incentiveMessage = "¡Limpiando archivos recibidos por mensajería!",
                requirementText = "Marca para eliminar un archivo de WhatsApp o mensajería",
                target = 1,
                extractor = { stats, _ -> if (stats.cleanedWhatsAppItem) 1 else 0 }
            )
        )
        add(
            Spec(
                id = "clean_screenshots",
                category = AchievementCategory.CARPETAS,
                title = "Cazador de Capturas",
                badgeLabel = "SCREENSHOTS",
                incentiveMessage = "¡Las capturas de pantalla olvidadas ya no ocuparán lugar!",
                requirementText = "Marca para eliminar una captura en Screenshots",
                target = 1,
                extractor = { stats, _ -> if (stats.cleanedScreenshotsItem) 1 else 0 }
            )
        )
        add(
            Spec(
                id = "clean_downloads",
                category = AchievementCategory.CARPETAS,
                title = "Descargas en Orden",
                badgeLabel = "DOWNLOADS",
                incentiveMessage = "¡Poniendo orden en la carpeta de Descargas!",
                requirementText = "Marca para eliminar un archivo de Descargas / Download",
                target = 1,
                extractor = { stats, _ -> if (stats.cleanedDownloadsItem) 1 else 0 }
            )
        )
        add(
            Spec(
                id = "clean_movies",
                category = AchievementCategory.CARPETAS,
                title = "Vaciando la Videoteca",
                badgeLabel = "MOVIES / VIDEO",
                incentiveMessage = "¡Limpiando videos pesados de tus álbumes!",
                requirementText = "Marca para eliminar un archivo de carpeta de Videos / Movies",
                target = 1,
                extractor = { stats, _ -> if (stats.cleanedMoviesItem) 1 else 0 }
            )
        )
        val sessionMilestones = listOf(
            Triple(1, "Primera Confirmación Oficial", "¡Completaste y confirmaste tu primera limpieza de galería!"),
            Triple(3, "Hábito de Orden (3 Sesiones)", "¡3 sesiones de limpieza confirmadas con éxito!"),
            Triple(5, "Disciplina Digital (5 Sesiones)", "¡5 confirmaciones de borrado completadas!"),
            Triple(10, "Veterano del Espacio (10 Sesiones)", "¡10 sesiones de limpieza completadas!"),
            Triple(20, "Guardián Permanente (20 Sesiones)", "¡20 sesiones! Tu galería siempre está en su mejor forma.")
        )
        sessionMilestones.forEach { (sessCount, title, msg) ->
            add(
                Spec(
                    id = "session_done_$sessCount",
                    category = AchievementCategory.CARPETAS,
                    title = title,
                    badgeLabel = "$sessCount SESIONES",
                    incentiveMessage = msg,
                    requirementText = "Confirma la eliminación final en $sessCount sesiones",
                    target = sessCount,
                    extractor = { stats, _ -> stats.completedSessionsCount }
                )
            )
        }

        // 6. CATEGORÍA: MAESTRÍA Y COLECCIONISTA (10 Logros: #91 al #100)
        val collectorMilestones = listOf(
            Triple(3, "Coleccionista Novato", "¡Ya desbloqueaste tus primeros 3 logros!"),
            Triple(5, "Cazador de Trofeos (5)", "¡5 logros desbloqueados! Sigue limpiando para ganar más."),
            Triple(10, "Diez Medallas de Honor", "¡10 logros conseguidos en tu vitrina!"),
            Triple(20, "Veinte Logros Brillantes", "¡20 logros! Ya dominas el arte del swipe."),
            Triple(30, "Treinta Insignias", "¡30 logros desbloqueados! Tu teléfono está irreconocible."),
            Triple(45, "Mitad del Camino (45)", "¡45 logros! Estás cerca del ecuador del salón de la fama."),
            Triple(60, "Sesenta Trofeos de Oro", "¡60 logros desbloqueados! Nivel experto alcanzado."),
            Triple(75, "Gran Maestro (75)", "¡75 logros! Tres cuartas partes de los 100 completadas."),
            Triple(90, "Casi Perfecto (90)", "¡90 logros desbloqueados! Solo quedan los más épicos."),
            Triple(99, "Dios del Almacenamiento (#100)", "¡Has completado la colección entera de los 100 logros de SwipeFoto!")
        )
        collectorMilestones.forEach { (reqUnlocked, title, msg) ->
            add(
                Spec(
                    id = "collector_$reqUnlocked",
                    category = AchievementCategory.COLECCIONISTA,
                    title = title,
                    badgeLabel = "$reqUnlocked / 100 LOGROS",
                    incentiveMessage = msg,
                    requirementText = "Desbloquea $reqUnlocked logros en total",
                    target = reqUnlocked,
                    extractor = { _, unlockedNonCollector -> unlockedNonCollector }
                )
            )
        }
    }

    fun buildInitialAchievements(): List<Achievement> {
        return specs.mapIndexed { index, spec ->
            Achievement(
                id = spec.id,
                number = index + 1,
                category = spec.category,
                title = spec.title,
                badgeLabel = spec.badgeLabel,
                incentiveMessage = spec.incentiveMessage,
                requirementText = spec.requirementText,
                progressCurrent = 0,
                progressTarget = spec.target,
                unlocked = false
            )
        }
    }

    /**
     * Evaluates all 100 achievements against the latest user stats and returns
     * the updated 100 achievements along with any newly unlocked achievement to show in the popup.
     */
    fun evaluate(
        stats: UserCleaningStats,
        previous: List<Achievement>
    ): Pair<List<Achievement>, List<Achievement>> {
        val prevUnlockedIds = previous.filter { it.unlocked }.map { it.id }.toSet()

        // First pass: non-collector achievements (#1 to #90)
        val firstPass = specs.mapIndexed { index, spec ->
            if (spec.category == AchievementCategory.COLECCIONISTA) {
                previous.getOrNull(index) ?: Achievement(
                    id = spec.id,
                    number = index + 1,
                    category = spec.category,
                    title = spec.title,
                    badgeLabel = spec.badgeLabel,
                    incentiveMessage = spec.incentiveMessage,
                    requirementText = spec.requirementText,
                    progressCurrent = 0,
                    progressTarget = spec.target,
                    unlocked = false
                )
            } else {
                val currentVal = spec.extractor(stats, 0).coerceAtLeast(0)
                val wasUnlocked = spec.id in prevUnlockedIds
                val isUnlocked = wasUnlocked || currentVal >= spec.target
                Achievement(
                    id = spec.id,
                    number = index + 1,
                    category = spec.category,
                    title = spec.title,
                    badgeLabel = spec.badgeLabel,
                    incentiveMessage = spec.incentiveMessage,
                    requirementText = spec.requirementText,
                    progressCurrent = if (isUnlocked) spec.target else currentVal,
                    progressTarget = spec.target,
                    unlocked = isUnlocked
                )
            }
        }

        val unlockedCountSoFar = firstPass.count { it.unlocked }

        // Second pass: collector achievements (#91 to #100)
        val finalList = firstPass.mapIndexed { index, ach ->
            val spec = specs[index]
            if (spec.category == AchievementCategory.COLECCIONISTA) {
                val wasUnlocked = spec.id in prevUnlockedIds
                val isUnlocked = wasUnlocked || unlockedCountSoFar >= spec.target
                ach.copy(
                    progressCurrent = if (isUnlocked) spec.target else unlockedCountSoFar.coerceAtMost(spec.target),
                    unlocked = isUnlocked
                )
            } else {
                ach
            }
        }

        val newlyUnlocked = finalList.filter { it.unlocked && it.id !in prevUnlockedIds }
        return finalList to newlyUnlocked
    }
}
