// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/usecase/DetectConflictsUseCase.kt
// [Penjelasan]: UseCase untuk mendeteksi potensi benturan nama berkas dan folder sebelum eksekusi operasi Copy atau Move.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.conflict.ConflictDetector
import com.wakwau.xplore.core.storage.conflict.FileConflict
import com.wakwau.xplore.core.storage.model.StorageLocation

class DetectConflictsUseCase(
    private val conflictDetector: ConflictDetector
) {
    suspend fun invoke(
        sources: List<StorageLocation>,
        destinationDir: StorageLocation
    ): List<FileConflict> {
        return conflictDetector.detectConflicts(sources, destinationDir)
    }
}
