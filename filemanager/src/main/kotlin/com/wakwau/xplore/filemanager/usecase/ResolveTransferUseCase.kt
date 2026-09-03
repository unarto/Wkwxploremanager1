// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/usecase/ResolveTransferUseCase.kt
// [Penjelasan]: UseCase untuk menyelesaikan daftar item transfer berkas/direktori dengan mengaplikasikan resolusi benturan (SKIP, OVERWRITE, RENAME) atau nama asli jika tidak bentrok.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.conflict.ConflictChoice
import com.wakwau.xplore.core.storage.conflict.ConflictDetector
import com.wakwau.xplore.core.storage.conflict.ConflictResolver
import com.wakwau.xplore.core.storage.conflict.FileConflict
import com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem
import com.wakwau.xplore.core.storage.model.StorageLocation

class ResolveTransferUseCase(
    private val conflictDetector: ConflictDetector,
    private val conflictResolver: ConflictResolver
) {
    suspend fun invoke(
        sources: List<StorageLocation>,
        destinationDir: StorageLocation,
        conflictDecisions: Map<StorageLocation, ConflictChoice> = emptyMap(),
        defaultDecision: ConflictChoice? = null
    ): List<ResolvedTransferItem> {
        val existingNames = conflictDetector.getExistingNames(destinationDir).toMutableSet()
        val conflicts = conflictDetector.detectConflicts(sources, destinationDir).associateBy { it.source }
        val resolvedList = mutableListOf<ResolvedTransferItem>()

        for (source in sources) {
            val conflict = conflicts[source]
            if (conflict != null) {
                val choice = conflictDecisions[source] ?: defaultDecision ?: ConflictChoice.RENAME
                val resolved = conflictResolver.resolveConflict(conflict, choice, existingNames)
                if (resolved != null) {
                    resolvedList.add(resolved)
                }
            } else {
                val resolved = conflictResolver.resolveNonConflictingItem(
                    source = source,
                    destinationDir = destinationDir,
                    isDirectory = false, // resolveNonConflictingItem menggunakan path nama asli
                    existingNames = existingNames
                )
                resolvedList.add(resolved)
            }
        }

        return resolvedList
    }
}
