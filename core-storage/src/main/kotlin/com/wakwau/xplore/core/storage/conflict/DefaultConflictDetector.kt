// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/conflict/DefaultConflictDetector.kt
// [Penjelasan]: Implementasi ConflictDetector untuk mendeteksi potensi benturan nama berkas dan direktori di tujuan lintas sistem berkas (Local, SAF, Shizuku, Root) dengan proteksi rekursi ke dalam diri sendiri.
package com.wakwau.xplore.core.storage.conflict

import com.wakwau.xplore.core.storage.filesystem.LocalFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.RootFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.SafFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.ShizukuFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.StorageBackendClassifier
import com.wakwau.xplore.core.storage.filesystem.StorageBackendType
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import java.io.File

class DefaultConflictDetector(
    private val localFileSystem: LocalFileSystemContract,
    private val safFileSystem: SafFileSystemContract,
    private val safShizukuFileSystem: ShizukuFileSystemContract,
    private val rootFileSystem: RootFileSystemContract,
    private val backendClassifier: StorageBackendClassifier
) : ConflictDetector {

    override suspend fun getExistingNames(destinationDir: StorageLocation): Set<String> {
        val items = when (backendClassifier.classify(destinationDir)) {
            StorageBackendType.LOCAL -> localFileSystem.listFiles(destinationDir, showHidden = true)
            StorageBackendType.SAF -> safFileSystem.listFiles(destinationDir, showHidden = true)
            StorageBackendType.SHIZUKU -> safShizukuFileSystem.listFiles(destinationDir, showHidden = true)
            StorageBackendType.ROOT -> rootFileSystem.listFiles(destinationDir, showHidden = true)
        }
        return items.map { it.name }.toSet()
    }

    override suspend fun detectConflicts(
        sources: List<StorageLocation>,
        destinationDir: StorageLocation
    ): List<FileConflict> {
        val existingNames = getExistingNames(destinationDir)
        val conflicts = mutableListOf<FileConflict>()

        for (source in sources) {
            val sourceName = extractItemName(source)
            val isDir = isSourceDirectory(source)

            // Proteksi: jangan memproses kontainer ke dalam dirinya sendiri atau anak-anaknya
            if (isDir && isSelfOrInside(source, destinationDir)) {
                continue
            }

            val hasConflict = existingNames.any { it.equals(sourceName, ignoreCase = true) }
            if (hasConflict) {
                conflicts.add(
                    FileConflict(
                        source = source,
                        sourceName = sourceName,
                        targetName = sourceName,
                        isDirectory = isDir,
                        destinationDir = destinationDir
                    )
                )
            }
        }

        return conflicts
    }

    private suspend fun isSourceDirectory(source: StorageLocation): Boolean {
        return when (backendClassifier.classify(source)) {
            StorageBackendType.LOCAL -> localFileSystem.getFileItem(source)?.type == FileType.DIRECTORY
            StorageBackendType.SAF -> safFileSystem.getFileItem(source)?.type == FileType.DIRECTORY
            StorageBackendType.SHIZUKU -> safShizukuFileSystem.getFileItem(source)?.type == FileType.DIRECTORY
            StorageBackendType.ROOT -> rootFileSystem.getFileItem(source)?.type == FileType.DIRECTORY
        }
    }

    private fun isSelfOrInside(source: StorageLocation, destination: StorageLocation): Boolean {
        if (source.path.startsWith("content://") || destination.path.startsWith("content://")) {
            return source.path == destination.path
        }
        val sourceFile = File(source.path)
        val destFile = File(destination.path)
        val sourceCanonical = runCatching { sourceFile.canonicalPath }.getOrDefault(sourceFile.absolutePath)
        val destCanonical = runCatching { destFile.canonicalPath }.getOrDefault(destFile.absolutePath)
        return destCanonical == sourceCanonical || destCanonical.startsWith(sourceCanonical + File.separator)
    }

    private fun extractItemName(location: StorageLocation): String {
        return location.path.trimEnd('/').substringAfterLast('/')
    }
}

