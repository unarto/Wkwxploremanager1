// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/StorageBackendClassifier.kt
// [Penjelasan]: Mengklasifikasikan tipe filesystem backend (LOCAL, SAF, SHIZUKU, ROOT) secara dinamis berdasarkan availability aktual su, shizuku, SAF URI permission, dan karakteristik lokasi berkas tanpa ketergantungan framework platform di core-storage-api.
package com.wakwau.xplore.core.storage.filesystem

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageLocation

class StorageBackendClassifier(
    private val isSuAvailable: () -> Boolean,
    private val isShizukuAvailable: () -> Boolean,
    private val isSafPersisted: (String) -> Boolean
) {
    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/StorageBackendClassifier.kt
    // [Penjelasan]: Menentukan backend penyimpanan yang tepat sesuai skema path, restriksi akses Android, permission yang tersedia, dan root/shizuku status.
    fun classify(location: StorageLocation): StorageBackendType {
        if (location.path.startsWith(StorageConstants.CONTENT_SCHEME_PREFIX)) {
            return StorageBackendType.SAF
        }

        val isRestrictedAndroidPath = isAndroidDataOrObb(location.path)

        if (isRestrictedAndroidPath) {
            return when {
                isSuAvailable() -> StorageBackendType.ROOT
                isShizukuAvailable() -> StorageBackendType.SHIZUKU
                isSafPersisted(location.path) -> StorageBackendType.SAF
                else -> StorageBackendType.LOCAL
            }
        }

        val isExplicitRootId = location.rootId == StorageConstants.ROOT_STORAGE_ID ||
                location.rootId == StorageConstants.ROOT_LEGACY_ID

        if (isExplicitRootId) {
            return when {
                isSuAvailable() -> StorageBackendType.ROOT
                isShizukuAvailable() -> StorageBackendType.SHIZUKU
                else -> StorageBackendType.LOCAL
            }
        }

        val cleanPath = location.path.trim()
        val isRootPath = cleanPath == "/" || cleanPath == StorageConstants.ROOT_PATH
        
        if (isRootPath) {
             return when {
                isSuAvailable() -> StorageBackendType.ROOT
                isShizukuAvailable() -> StorageBackendType.SHIZUKU
                else -> StorageBackendType.LOCAL
            }
        }

        if (isSafPersisted(location.path)) {
            return when {
                isSuAvailable() -> StorageBackendType.ROOT
                isShizukuAvailable() -> StorageBackendType.SHIZUKU
                else -> StorageBackendType.SAF
            }
        }
        
        if (isSuAvailable() || isShizukuAvailable()) {
            val isStandardStorage = cleanPath.startsWith("/storage/") || 
                                    cleanPath.startsWith("/sdcard/") || 
                                    cleanPath.startsWith("/mnt/") ||
                                    cleanPath == "/storage" ||
                                    cleanPath == "/sdcard" ||
                                    cleanPath == "/mnt"
            if (!isStandardStorage && cleanPath.startsWith("/")) {
                return if (isSuAvailable()) StorageBackendType.ROOT else StorageBackendType.SHIZUKU
            }
        }

        return StorageBackendType.LOCAL
    }

    private fun isAndroidDataOrObb(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        val lowerPath = normalized.lowercase()
        return lowerPath.contains("/android/data/") || lowerPath.endsWith("/android/data") ||
               lowerPath.contains("/android/obb/") || lowerPath.endsWith("/android/obb")
    }
}
