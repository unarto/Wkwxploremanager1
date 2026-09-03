// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/conflict/DefaultConflictResolver.kt
// [Penjelasan]: Implementasi murni ConflictResolver untuk memproses strategi SKIP, OVERWRITE, dan pembuatan nama unik RENAME ("file (1).ext" atau "folder (1)") mengikuti pola XFiles tanpa magic strings.
package com.wakwau.xplore.core.storage.conflict

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageLocation
import java.io.File

class DefaultConflictResolver : ConflictResolver {

    override fun resolveConflict(
        conflict: FileConflict,
        choice: ConflictChoice,
        existingNames: MutableSet<String>
    ): ResolvedTransferItem? {
        return when (choice) {
            ConflictChoice.SKIP -> null
            ConflictChoice.OVERWRITE -> {
                existingNames.add(conflict.targetName)
                val targetLoc = buildTargetLocation(conflict.destinationDir, conflict.targetName)
                ResolvedTransferItem(
                    source = conflict.source,
                    destinationDir = conflict.destinationDir,
                    targetLocation = targetLoc,
                    originalName = conflict.sourceName,
                    targetName = conflict.targetName,
                    isDirectory = conflict.isDirectory,
                    choice = ConflictChoice.OVERWRITE
                )
            }
            ConflictChoice.RENAME -> {
                val uniqueName = generateUniqueName(conflict.targetName, conflict.isDirectory, existingNames)
                existingNames.add(uniqueName)
                val targetLoc = buildTargetLocation(conflict.destinationDir, uniqueName)
                ResolvedTransferItem(
                    source = conflict.source,
                    destinationDir = conflict.destinationDir,
                    targetLocation = targetLoc,
                    originalName = conflict.sourceName,
                    targetName = uniqueName,
                    isDirectory = conflict.isDirectory,
                    choice = ConflictChoice.RENAME
                )
            }
        }
    }

    override fun generateUniqueName(
        originalName: String,
        isDirectory: Boolean,
        existingNames: Set<String>
    ): String {
        val dot = if (isDirectory) -1 else originalName.lastIndexOf(StorageConstants.Naming.EXTENSION_SEPARATOR)
        val stem = if (dot > 0) originalName.substring(0, dot) else originalName
        val ext = if (dot > 0) originalName.substring(dot) else ""

        var index = StorageConstants.Naming.FIRST_COPY_INDEX
        while (true) {
            val candidate = if (isDirectory) {
                String.format(StorageConstants.Naming.CONFLICT_DIR_FORMAT, stem, index)
            } else {
                String.format(StorageConstants.Naming.CONFLICT_NAME_FORMAT, stem, index, ext)
            }
            // Pemeriksaan case-insensitive untuk menjaga portabilitas filesystem
            val alreadyTaken = existingNames.any { it.equals(candidate, ignoreCase = true) }
            if (!alreadyTaken) {
                return candidate
            }
            index++
        }
    }

    override fun resolveNonConflictingItem(
        source: StorageLocation,
        destinationDir: StorageLocation,
        isDirectory: Boolean,
        existingNames: MutableSet<String>
    ): ResolvedTransferItem {
        val name = extractItemName(source)
        existingNames.add(name)
        val targetLoc = buildTargetLocation(destinationDir, name)
        return ResolvedTransferItem(
            source = source,
            destinationDir = destinationDir,
            targetLocation = targetLoc,
            originalName = name,
            targetName = name,
            isDirectory = isDirectory,
            choice = ConflictChoice.OVERWRITE
        )
    }

    private fun extractItemName(location: StorageLocation): String {
        return location.path.trimEnd('/').substringAfterLast('/')
    }

    private fun buildTargetLocation(destinationDir: StorageLocation, targetName: String): StorageLocation {
        return if (destinationDir.path.startsWith(StorageConstants.CONTENT_SCHEME_PREFIX)) {
            destinationDir
        } else {
            val cleanDest = destinationDir.path.trimEnd('/')
            val newPath = if (cleanDest.isEmpty()) "/$targetName" else "$cleanDest/$targetName"
            StorageLocation(path = newPath, rootId = destinationDir.rootId)
        }
    }
}
