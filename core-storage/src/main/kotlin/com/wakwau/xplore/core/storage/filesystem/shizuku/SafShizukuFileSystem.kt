// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/shizuku/SafShizukuFileSystem.kt
// [Penjelasan]: Implementasi filesystem nyata untuk akses Privileged Root/Shizuku menggunakan IPC AIDL IPrivilegedFileService untuk operasi list, create directory, delete, dan rename.
package com.wakwau.xplore.core.storage.filesystem.shizuku

import android.content.Context
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.filesystem.ShizukuFileSystemContract
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.shizuku.ShizukuHelper
import com.wakwau.xplore.core.storage.shizuku.ShizukuIpcConstants
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class SafShizukuFileSystem(private val context: Context) : ShizukuFileSystemContract {
    override suspend fun listFiles(location: StorageLocation, showHidden: Boolean): List<FileItem> {
        val service = ShizukuHelper.getPrivilegedService(context.packageName)
            ?: throw FileNotFoundException("Root/Shizuku service not available")

        if (!service.exists(location.path) || !service.isDirectory(location.path)) {
            throw FileNotFoundException("Directory not found or is not a directory: ${location.path}")
        }

        val bundles = service.listDirectory(location.path)

        return bundles.filter { bundle ->
            if (!showHidden) {
                val name = bundle.getString(ShizukuIpcConstants.KEY_NAME) ?: ""
                val isHidden = bundle.getBoolean(ShizukuIpcConstants.KEY_IS_HIDDEN)
                !isHidden && !name.startsWith(".")
            } else {
                true
            }
        }.map { bundle ->
            val name = bundle.getString(ShizukuIpcConstants.KEY_NAME) ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME
            val path = bundle.getString(ShizukuIpcConstants.KEY_PATH) ?: ""
            val isDirectory = bundle.getBoolean(ShizukuIpcConstants.KEY_IS_DIRECTORY)
            val type = if (isDirectory) FileType.DIRECTORY else FileType.FILE

            val metadata = FileMetadata(
                size = bundle.getLong(ShizukuIpcConstants.KEY_SIZE),
                modifiedTime = bundle.getLong(ShizukuIpcConstants.KEY_LAST_MODIFIED),
                createdTime = null,
                isReadable = true,
                isWritable = true,
                isExecutable = true,
                isHidden = bundle.getBoolean(ShizukuIpcConstants.KEY_IS_HIDDEN) || name.startsWith(".")
            )

            FileItem(
                id = path,
                name = name,
                location = StorageLocation(path, location.rootId),
                type = type,
                metadata = metadata
            )
        }.sortedWith(compareBy({ it.type != FileType.DIRECTORY }, { it.name.lowercase() }))
    }

    override suspend fun createDirectory(location: StorageLocation, name: String): FileItem {
        return createDirectoryInternal(location, name)
    }

    private suspend fun createDirectoryInternal(location: StorageLocation, name: String): FileItem {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName == ".." || trimmedName == ".") {
            throw IllegalArgumentException("Invalid directory name: $name")
        }

        val service = ShizukuHelper.getPrivilegedService(context.packageName)
            ?: throw FileNotFoundException("Root/Shizuku service not available")

        val parentPath = location.path.trimEnd('/')
        val targetPath = if (parentPath.isEmpty()) "/$trimmedName" else "$parentPath/$trimmedName"

        if (service.exists(targetPath)) {
            throw IOException("Directory already exists: $targetPath")
        }

        val created = service.createDirectory(targetPath)
        if (!created) {
            throw IOException("Failed to create privileged directory: $targetPath")
        }

        val metadata = FileMetadata(
            size = 0L,
            modifiedTime = System.currentTimeMillis(),
            createdTime = null,
            isReadable = true,
            isWritable = true,
            isExecutable = true,
            isHidden = name.startsWith(".")
        )

        return FileItem(
            id = targetPath,
            name = name,
            location = StorageLocation(targetPath, location.rootId),
            type = FileType.DIRECTORY,
            metadata = metadata
        )
    }

    override suspend fun delete(location: StorageLocation) {
        val pathClean = location.path.trim().trimEnd('/')
        if (pathClean.isEmpty() || pathClean == "/" || pathClean.equals("/storage", ignoreCase = true) || pathClean.equals("/storage/emulated", ignoreCase = true) || pathClean.equals("/system", ignoreCase = true)) {
            throw SecurityException("Cannot delete root or protected storage path: ${location.path}")
        }
        val service = ShizukuHelper.getPrivilegedService(context.packageName)
            ?: throw FileNotFoundException("Root/Shizuku service not available")

        if (!service.exists(location.path)) {
            throw FileNotFoundException("File not found in root: ${location.path}")
        }

        val deleted = service.delete(location.path)
        if (!deleted) {
            throw IOException("Failed to delete root file: ${location.path}")
        }
    }

    override suspend fun rename(location: StorageLocation, newName: String): FileItem {
        if (newName.contains("/") || newName.contains("\\") || newName == ".." || newName == ".") {
            throw IllegalArgumentException("Invalid name: $newName")
        }

        val service = ShizukuHelper.getPrivilegedService(context.packageName)
            ?: throw FileNotFoundException("Root/Shizuku service not available")

        if (!service.exists(location.path)) {
            throw FileNotFoundException("File not found in root: ${location.path}")
        }

        val parentPath = if (location.path.contains("/")) location.path.substringBeforeLast("/") else ""
        val targetPath = if (parentPath.isEmpty()) "/$newName" else "$parentPath/$newName"

        if (service.exists(targetPath)) {
            throw IOException("Target already exists: $targetPath")
        }

        val renamed = service.rename(location.path, targetPath)
        if (!renamed) {
            throw IOException("Failed to rename root file to: $newName")
        }

        val isDir = service.isDirectory(targetPath)

        val metadata = FileMetadata(
            size = if (isDir) 0L else service.length(targetPath),
            modifiedTime = service.lastModified(targetPath),
            createdTime = null,
            isReadable = true,
            isWritable = true,
            isExecutable = true,
            isHidden = newName.startsWith(".")
        )

        return FileItem(
            id = targetPath,
            name = newName,
            location = StorageLocation(targetPath, location.rootId),
            type = if (isDir) FileType.DIRECTORY else FileType.FILE,
            metadata = metadata
        )
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/shizuku/SafShizukuFileSystem.kt
    // [Penjelasan]: Mengambil metadata dan entitas FileItem dari path sistem berkas Root/Privileged via IPC Shizuku untuk sinkronisasi indeks Room DB.
    override suspend fun getFileItem(location: StorageLocation): FileItem? {
        val service = ShizukuHelper.getPrivilegedService(context.packageName) ?: return null
        if (!service.exists(location.path)) return null
        val isDir = service.isDirectory(location.path)
        val name = if (location.path.contains("/")) location.path.substringAfterLast("/") else location.path
        val metadata = FileMetadata(
            size = if (isDir) 0L else service.length(location.path),
            modifiedTime = service.lastModified(location.path),
            createdTime = null,
            isReadable = true,
            isWritable = true,
            isExecutable = true,
            isHidden = name.startsWith(".")
        )
        return FileItem(
            id = location.path,
            name = name.ifEmpty { StorageConstants.DEFAULT_UNKNOWN_FILE_NAME },
            location = StorageLocation(location.path, location.rootId),
            type = if (isDir) FileType.DIRECTORY else FileType.FILE,
            metadata = metadata
        )
    }

    override suspend fun exists(location: StorageLocation): Boolean {
        val service = ShizukuHelper.getPrivilegedService(context.packageName) ?: return false
        return service.exists(location.path)
    }

    private val transferHandler = ShizukuFileTransferHandler()

    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val service = ShizukuHelper.getPrivilegedService(context.packageName)
            ?: throw FileNotFoundException("Root/Shizuku service not available")

        if (!service.exists(source.path)) {
            throw FileNotFoundException("Source not found in root: ${source.path}")
        }
        
        if (source.path == destination.path) {
            throw IllegalArgumentException("Source and destination are the same")
        }
        
        if (service.isDirectory(source.path) && destination.path.startsWith(source.path + "/")) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        var totalCopied = 0L
        val totalBytes = transferHandler.calculateTotalSize(service, source.path)

        if (totalBytes == 0L) {
            val sourceName = source.path.trimEnd('/').substringAfterLast('/')
            emit(FileOperationProgress(0L, 0L, sourceName.ifEmpty { StorageConstants.DEFAULT_UNKNOWN_FILE_NAME }))
        }

        if (service.isDirectory(source.path)) {
            transferHandler.copyDirectoryRecursively(service, source.path, destination.path, totalBytes) { incrementalBytes, fileName ->
                totalCopied += incrementalBytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        } else {
            transferHandler.copySingleFile(service, source.path, destination.path, totalBytes) { incrementalBytes, fileName ->
                totalCopied += incrementalBytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val service = ShizukuHelper.getPrivilegedService(context.packageName)
            ?: throw FileNotFoundException("Root/Shizuku service not available")

        if (!service.exists(source.path)) {
            throw FileNotFoundException("Source not found in root: ${source.path}")
        }
        
        if (source.path == destination.path) {
            throw IOException("Source and destination are the same")
        }
        
        if (service.isDirectory(source.path) && destination.path.startsWith(source.path + "/")) {
            throw IOException("Cannot move a directory into itself")
        }

        val isSourceDir = service.isDirectory(source.path)
        val sourceSize = if (!isSourceDir) service.length(source.path) else 0L

        // Coba atomic rename
        val renamed = service.rename(source.path, destination.path)
        if (renamed) {
            val length = service.length(destination.path)
            val name = source.path.substringAfterLast("/")
            emit(FileOperationProgress(length, length, name))
            return@flow
        }
        
        // Fallback ke copy lalu delete (jika beda mount point)
        copy(source, destination).collect { progress ->
            emit(progress)
        }
        
        if (currentCoroutineContext().isActive) {
            if (!service.exists(destination.path)) {
                throw IOException("Move failed: destination does not exist after copy (${destination.path})")
            }
            if (!isSourceDir && service.length(destination.path) != sourceSize) {
                try { service.delete(destination.path) } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
                throw IOException("Move failed: partial copy detected (destination size mismatch)")
            }
            service.delete(source.path)
        }
    }.flowOn(Dispatchers.IO)
}
