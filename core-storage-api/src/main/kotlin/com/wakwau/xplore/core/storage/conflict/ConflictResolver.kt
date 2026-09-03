// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/conflict/ConflictResolver.kt
// [Penjelasan]: Kontrak antarmuka untuk menyelesaikan benturan nama (SKIP, OVERWRITE, RENAME) dan menghasilkan nama unik tanpa bentrok.
package com.wakwau.xplore.core.storage.conflict

import com.wakwau.xplore.core.storage.model.StorageLocation

interface ConflictResolver {
    fun resolveConflict(
        conflict: FileConflict,
        choice: ConflictChoice,
        existingNames: MutableSet<String>
    ): ResolvedTransferItem?

    fun generateUniqueName(
        originalName: String,
        isDirectory: Boolean,
        existingNames: Set<String>
    ): String

    fun resolveNonConflictingItem(
        source: StorageLocation,
        destinationDir: StorageLocation,
        isDirectory: Boolean,
        existingNames: MutableSet<String>
    ): ResolvedTransferItem
}
