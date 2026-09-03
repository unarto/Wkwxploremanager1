// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt
// [Penjelasan]: Implementasi filesystem nyata untuk Local Storage (Internal/SDCard/OTG) yang menangani listing, pembuatan direktori, penghapusan, penggantian nama, penyalinan byte streaming, dan pemindahan berkas dengan sanitasi path traversal.

package com.wakwau.xplore.core.storage.filesystem.local

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.filesystem.LocalFileSystemContract
import com.wakwau.xplore.core.storage.mapper.FileItemMapper
import com.wakwau.xplore.core.storage.metadata.FileMetadataReader
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

class LocalFileSystem(
    private val fileMetadataReader: FileMetadataReader = FileMetadataReader(),
    private val fileItemMapper: FileItemMapper = FileItemMapper()
) : LocalFileSystemContract {

    override suspend fun listFiles(location: StorageLocation, showHidden: Boolean): List<FileItem> {
        return listFiles(path = location.path, showHidden = showHidden, rootId = location.rootId)
    }

    override suspend fun createDirectory(location: StorageLocation, name: String): FileItem {
        return createDirectory(parentPath = location.path, name = name, rootId = location.rootId)
    }

    override suspend fun delete(location: StorageLocation) {
        delete(path = location.path)
    }

    override suspend fun rename(location: StorageLocation, newName: String): FileItem {
        return rename(path = location.path, newName = newName, rootId = location.rootId)
    }

    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> {
        return copy(sourcePath = source.path, destPath = destination.path)
    }

    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> {
        return move(sourcePath = source.path, destPath = destination.path)
    }

    override suspend fun getFileItem(location: StorageLocation): FileItem? {
        return getFileItem(path = location.path, rootId = location.rootId)
    }

    override suspend fun exists(location: StorageLocation): Boolean {
        return exists(path = location.path)
    }

    fun listFiles(
        path: String,
        showHidden: Boolean = true,
        rootId: String = StorageConstants.PRIMARY_INTERNAL_VOLUME_ID
    ): List<FileItem> {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) {
            throw FileNotFoundException("Directory not found or is not a directory: $path")
        }

        var files = directory.listFiles()?.toList() ?: emptyList()

        if (!showHidden) {
            files = files.filter { !it.isHidden && !it.name.startsWith(".") }
        }

        val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        return sortedFiles.map { file ->
            val metadata = fileMetadataReader.readMetadata(file)
            val type = if (file.isDirectory) FileType.DIRECTORY else FileType.FILE
            val itemLocation = StorageLocation(path = file.absolutePath, rootId = rootId)
            fileItemMapper.map(
                id = file.absolutePath,
                name = file.name,
                location = itemLocation,
                type = type,
                metadata = metadata
            )
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt
    // [Penjelasan]: Membuat direktori baru di filesystem lokal dengan sanitasi nama, resolusi direktori induk jika berkas terpilih, dan proteksi path traversal.
    fun createDirectory(
        parentPath: String,
        name: String,
        rootId: String = StorageConstants.PRIMARY_INTERNAL_VOLUME_ID
    ): FileItem {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName == ".." || trimmedName == ".") {
            throw IllegalArgumentException("Invalid directory name: $name")
        }
        var parent = File(parentPath)
        if (parent.exists() && parent.isFile) {
            parent = parent.parentFile ?: parent
        }
        if (!parent.exists() || !parent.isDirectory) {
            throw IOException("Parent directory not found: $parentPath")
        }
        val dir = File(parent, trimmedName)

        // Validasi Sanitasi Path Traversal
        val parentCanonical = parent.canonicalPath
        val dirCanonical = dir.canonicalPath
        if (!dirCanonical.startsWith(parentCanonical + File.separator) && dirCanonical != parentCanonical) {
            throw SecurityException("Path traversal attempt detected: $trimmedName")
        }

        if (dir.exists()) {
            throw IOException("Directory already exists: $trimmedName")
        }
        if (!dir.mkdir() && !dir.exists()) {
            throw IOException("Failed to create directory: $trimmedName")
        }

        val metadata = fileMetadataReader.readMetadata(dir)
        val newLocation = StorageLocation(path = dir.absolutePath, rootId = rootId)
        return fileItemMapper.map(
            id = dir.absolutePath,
            name = dir.name,
            location = newLocation,
            type = FileType.DIRECTORY,
            metadata = metadata
        )
    }

    suspend fun delete(path: String) {
        if (isRootOrProtectedPath(path)) {
            throw SecurityException("Cannot delete root or protected storage path: $path")
        }
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        val isSymlink = isSymbolicLink(file)
        if (isSymlink) {
            if (!file.delete()) {
                throw IOException("Failed to delete symlink: $path")
            }
        } else if (file.isDirectory) {
            deleteDirectoryRecursivelySafe(file)
        } else {
            if (!file.delete()) {
                throw IOException("Failed to delete file: $path")
            }
        }
    }

    private suspend fun deleteDirectoryRecursivelySafe(dir: File) {
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        val filesToDelete = ArrayDeque<File>()

        while (stack.isNotEmpty()) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) throw kotlinx.coroutines.CancellationException()
            val current = stack.removeLast()
            filesToDelete.addFirst(current)

            val isSymlink = isSymbolicLink(current)
            if (!isSymlink && current.isDirectory) {
                val children = current.listFiles() ?: continue
                for (child in children) {
                    stack.addLast(child)
                }
            }
        }

        for (file in filesToDelete) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) throw kotlinx.coroutines.CancellationException()
            if (!file.delete() && file.exists()) {
                throw IOException("Failed to delete: ${file.absolutePath}")
            }
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt
    // [Penjelasan]: Deteksi symlink yang aman untuk kompatibilitas JVM unit test dan Android API 24+ dengan fallback canonical path jika java.nio.file tidak tersedia di runtime legacy.
    private fun isSymbolicLink(file: File): Boolean {
        return try {
            java.nio.file.Files.isSymbolicLink(file.toPath())
        } catch (_: Throwable) {
            try {
                val parent = file.parentFile ?: return false
                val canonicalParent = parent.canonicalFile
                val fileInCanonicalParent = File(canonicalParent, file.name)
                fileInCanonicalParent.canonicalPath != fileInCanonicalParent.absolutePath
            } catch (_: Throwable) {
                false
            }
        }
    }

    private fun isRootOrProtectedPath(path: String): Boolean {
        val clean = path.trim().trimEnd('/')
        if (clean.isEmpty() || clean == "/" || clean == StorageConstants.ROOT_PATH) return true
        val primaryStorage = StorageConstants.DEFAULT_PRIMARY_STORAGE_PATH.trimEnd('/')
        if (clean.equals(primaryStorage, ignoreCase = true)) return true

        val protectedPaths = setOf(
            "/storage",
            "/storage/emulated",
            "/system",
            "/vendor",
            "/apex",
            "/proc",
            "/sys",
            "/dev",
            "/etc",
            "/bin",
            "/sbin"
        )
        return protectedPaths.contains(clean.lowercase())
    }

    fun rename(
        path: String,
        newName: String,
        rootId: String = StorageConstants.PRIMARY_INTERNAL_VOLUME_ID
    ): FileItem {
        if (newName.contains("/") || newName.contains("\\") || newName == ".." || newName == ".") {
            throw IllegalArgumentException("Invalid name: $newName")
        }
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        val parent = file.parentFile ?: throw IOException("Parent directory not found for: $path")
        val target = File(parent, newName)

        // Validasi Sanitasi Path Traversal
        val parentCanonical = parent.canonicalPath
        val targetCanonical = target.canonicalPath
        if (!targetCanonical.startsWith(parentCanonical + File.separator) && targetCanonical != parentCanonical) {
            throw SecurityException("Path traversal attempt detected: $newName")
        }

        if (target.exists()) {
            throw IOException("Target already exists: $newName")
        }
        if (!file.renameTo(target)) {
            throw IOException("Failed to rename file to: $newName")
        }

        val metadata = fileMetadataReader.readMetadata(target)
        val type = if (target.isDirectory) FileType.DIRECTORY else FileType.FILE
        val newLocation = StorageLocation(path = target.absolutePath, rootId = rootId)
        return fileItemMapper.map(
            id = target.absolutePath,
            name = target.name,
            location = newLocation,
            type = type,
            metadata = metadata
        )
    }

    fun copy(sourcePath: String, destPath: String): Flow<FileOperationProgress> = flow {
        val sourceFile = File(sourcePath)
        val destFile = File(destPath)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: $sourcePath")
        }

        if (sourceFile.absolutePath == destFile.absolutePath) {
            throw IllegalArgumentException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destFile.absolutePath.startsWith(sourceFile.absolutePath + File.separator)) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        emit(FileOperationProgress(0L, 0L, sourceFile.name))

        var totalCopied = 0L
        val totalBytes = if (sourceFile.isDirectory) calculateTotalSize(sourceFile) else sourceFile.length()

        if (sourceFile.isDirectory) {
            copyDirectoryRecursively(sourceFile, destFile, totalBytes) { incrementalBytes, fileName ->
                totalCopied += incrementalBytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        } else {
            copySingleFile(sourceFile, destFile, totalBytes) { incrementalBytes, fileName ->
                totalCopied += incrementalBytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        }
    }.flowOn(Dispatchers.IO)

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt
    // [Penjelasan]: Memindahkan berkas/folder lokal dengan mencoba atomic rename terlebih dahulu, atau fallback copy-delete dengan verifikasi kelengkapan tujuan sebelum menghapus sumber untuk mencegah data loss.
    fun move(sourcePath: String, destPath: String): Flow<FileOperationProgress> = flow {
        val sourceFile = File(sourcePath)
        val destFile = File(destPath)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: $sourcePath")
        }

        val sourceCanonical = sourceFile.canonicalPath
        val destCanonical = destFile.canonicalPath

        if (sourceCanonical == destCanonical) {
            throw IOException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destCanonical.startsWith(sourceCanonical + File.separator)) {
            throw IOException("Cannot move a directory into itself")
        }

        emit(FileOperationProgress(0L, 0L, sourceFile.name))

        val sourceLength = if (sourceFile.isFile) sourceFile.length() else calculateTotalSize(sourceFile)
        val isSourceDir = sourceFile.isDirectory

        // Coba atomic rename terlebih dahulu
        val renamed = sourceFile.renameTo(destFile)
        if (renamed) {
            emit(FileOperationProgress(destFile.length(), destFile.length(), destFile.name))
            return@flow
        }

        // Fallback ke copy lalu delete jika beda mount point
        copy(sourcePath, destPath).collect { progress ->
            emit(progress)
        }

        if (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (!destFile.exists()) {
                throw IOException("Move failed: destination does not exist after copy ($destPath)")
            }
            if (!isSourceDir && destFile.length() != sourceLength) {
                try { destFile.delete() } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
                throw IOException("Move failed: partial copy detected (destination size mismatch)")
            }
            delete(sourcePath)
        }
    }.flowOn(Dispatchers.IO)

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt
    // [Penjelasan]: Mengambil metadata dan entitas FileItem dari berkas/direktori lokal untuk sinkronisasi indeks atau inspeksi stat.
    fun getFileItem(
        path: String,
        rootId: String = StorageConstants.PRIMARY_INTERNAL_VOLUME_ID
    ): FileItem? {
        val file = File(path)
        if (!file.exists()) return null
        val metadata = fileMetadataReader.readMetadata(file)
        val type = if (file.isDirectory) FileType.DIRECTORY else FileType.FILE
        val itemLocation = StorageLocation(path = file.absolutePath, rootId = rootId)
        return fileItemMapper.map(
            id = file.absolutePath,
            name = file.name,
            location = itemLocation,
            type = type,
            metadata = metadata
        )
    }

    fun exists(path: String): Boolean = File(path).exists()

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt
    // [Penjelasan]: Menyalin satu berkas lokal dengan streaming buffer, proteksi pembatalan coroutine, verifikasi kelengkapan ukuran berkas (mencegah partial copy), dan pembersihan berkas parsial jika terjadi kegagalan.
    private suspend fun copySingleFile(
        source: File,
        dest: File,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output ->
                    val inputChannel = input.channel
                    val outputChannel = output.channel
                    val size = inputChannel.size()
                    var position = 0L
                    val chunkSize = 131072L // 128 KB
                    
                    while (position < size) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            throw CancellationException("Local copy cancelled")
                        }
                        val transferred = inputChannel.transferTo(position, chunkSize, outputChannel)
                        if (transferred > 0L) {
                            position += transferred
                            onProgress(transferred, source.name)
                        } else {
                            // Fallback to stream buffer if transferTo stalls
                            val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } >= 0) {
                                if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                                    throw CancellationException("Local copy cancelled")
                                }
                                output.write(buffer, 0, bytesRead)
                                onProgress(bytesRead.toLong(), source.name)
                            }
                            break
                        }
                    }
                    output.flush()
                }
            }
            if (dest.length() != source.length()) {
                throw IOException("Partial copy detected: destination size (${dest.length()}) does not match source size (${source.length()})")
            }
        } catch (e: Throwable) {
            try {
                if (dest.exists()) {
                    dest.delete()
                }
            } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
            throw e
        }
    }

    private suspend fun copyDirectoryRecursively(
        sourceDir: File,
        destDir: File,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val destDirCanonical = destDir.canonicalPath
        val files = sourceDir.listFiles() ?: return
        for (file in files) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                throw kotlinx.coroutines.CancellationException("Copy cancelled")
            }
            val destFile = File(destDir, file.name)
            val destFileCanonical = destFile.canonicalPath
            if (!destFileCanonical.startsWith(destDirCanonical + File.separator) && destFileCanonical != destDirCanonical) {
                throw SecurityException("Path traversal attempt detected during copy: ${file.name}")
            }
            if (file.isDirectory) {
                copyDirectoryRecursively(file, destFile, totalBytes, onProgress)
            } else {
                copySingleFile(file, destFile, totalBytes, onProgress)
            }
        }
    }

    private fun calculateTotalSize(dir: File): Long {
        var size = 0L
        val queue = ArrayDeque<File>()
        queue.add(dir)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val files = current.listFiles() ?: continue
            for (file in files) {
                if (file.isDirectory) {
                    queue.add(file)
                } else {
                    size += file.length()
                }
            }
        }
        return size
    }
}
