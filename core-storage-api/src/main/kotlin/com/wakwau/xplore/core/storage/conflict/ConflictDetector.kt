// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/conflict/ConflictDetector.kt
// [Penjelasan]: Kontrak antarmuka untuk mendeteksi potensi benturan nama berkas dan folder di direktori tujuan sebelum transfer.
package com.wakwau.xplore.core.storage.conflict

import com.wakwau.xplore.core.storage.model.StorageLocation

interface ConflictDetector {
    suspend fun detectConflicts(
        sources: List<StorageLocation>,
        destinationDir: StorageLocation
    ): List<FileConflict>

    suspend fun getExistingNames(destinationDir: StorageLocation): Set<String>
}
