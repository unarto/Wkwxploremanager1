// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
// [Penjelasan]: Implementasi sistem berkas berbasis Root Superuser (SU) menggunakan API resmi libsu (Topjohnwu) untuk menyediakan operasi list, exists, createDirectory, delete, rename, copy, move, dan inspeksi berkas tanpa ketergantungan Shizuku.

package com.wakwau.xplore.core.storage.filesystem.root

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.filesystem.RootFileSystemContract
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.permission.SuPermissionChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class RootFileSystem(
    private val suPermissionChecker: SuPermissionChecker = SuPermissionChecker(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RootFileSystemContract {

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Memeriksa ketersediaan akses SU melalui permission checker.
    override fun isAvailable(): Boolean = suPermissionChecker.isSuAvailable()

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Menampilkan daftar berkas dan direktori pada path root dengan izin superuser melalui SuFile libsu.
    override suspend fun listFiles(
        location: StorageLocation,
        showHidden: Boolean
    ): List<FileItem> = withContext(ioDispatcher) {
        ensureRootAccess()

        val suDirectory = SuFile(location.path)
        if (!suDirectory.exists() || !suDirectory.isDirectory) {
            throw FileNotFoundException("Directory not found or is not a directory in root: ${location.path}")
        }

        val rawFiles = suDirectory.listFiles() ?: emptyArray()
        val filtered = if (!showHidden) {
            rawFiles.filter { !it.isHidden && !it.name.startsWith(".") }
        } else {
            rawFiles.toList()
        }

        filtered.map { file ->
            val isDir = file.isDirectory
            val metadata = FileMetadata(
                size = if (isDir) 0L else file.length(),
                modifiedTime = file.lastModified(),
                createdTime = null,
                isReadable = file.canRead(),
                isWritable = file.canWrite(),
                isExecutable = file.canExecute(),
                isHidden = file.isHidden || file.name.startsWith(".")
            )
            val type = if (isDir) FileType.DIRECTORY else FileType.FILE
            FileItem(
                id = file.absolutePath,
                name = file.name.ifEmpty { file.absolutePath },
                location = StorageLocation(path = file.absolutePath, rootId = location.rootId),
                type = type,
                metadata = metadata
            )
        }.sortedWith(compareBy({ it.type != FileType.DIRECTORY }, { it.name.lowercase() }))
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Membuat direktori baru di path root dengan validasi nama dan eksekusi su mkdir.
    override suspend fun createDirectory(
        location: StorageLocation,
        name: String
    ): FileItem = withContext(ioDispatcher) {
        ensureRootAccess()

        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName == ".." || trimmedName == ".") {
            throw IllegalArgumentException("Invalid directory name: $name")
        }

        val parentPath = location.path.trimEnd('/')
        val targetPath = if (parentPath.isEmpty()) "/$trimmedName" else "$parentPath/$trimmedName"
        val targetFile = SuFile(targetPath)

        if (targetFile.exists()) {
            throw IOException("Directory already exists in root: $targetPath")
        }

        targetFile.mkdirs() || targetFile.mkdir() || Shell.cmd("mkdir -p ${escapeShellArg(targetPath)}").exec().isSuccess
        if (!targetFile.exists()) {
            throw IOException("Failed to create root directory: $targetPath")
        }

        val metadata = FileMetadata(
            size = 0L,
            modifiedTime = System.currentTimeMillis(),
            createdTime = null,
            isReadable = true,
            isWritable = true,
            isExecutable = true,
            isHidden = trimmedName.startsWith(".")
        )

        FileItem(
            id = targetPath,
            name = trimmedName,
            location = StorageLocation(path = targetPath, rootId = location.rootId),
            type = FileType.DIRECTORY,
            metadata = metadata
        )
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Memeriksa keberadaan file atau direktori pada path root.
    override suspend fun exists(location: StorageLocation): Boolean = withContext(ioDispatcher) {
        if (!isAvailable()) return@withContext false
        SuFile(location.path).exists()
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Menghapus berkas atau direktori root secara rekursif dengan proteksi direktori sistem penting.
    override suspend fun delete(location: StorageLocation) = withContext(ioDispatcher) {
        ensureRootAccess()

        if (isProtectedRootPath(location.path)) {
            throw SecurityException("Cannot delete root or protected storage path: ${location.path}")
        }

        val suFile = SuFile(location.path)
        if (!suFile.exists()) {
            throw FileNotFoundException("File not found in root: ${location.path}")
        }

        if (suFile.isDirectory) {
            deleteDirectoryRecursively(suFile)
        } else {
            if (!suFile.delete() && suFile.exists()) {
                throw IOException("Failed to delete root file: ${location.path}")
            }
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Mengubah nama file atau folder di sistem berkas root.
    override suspend fun rename(
        location: StorageLocation,
        newName: String
    ): FileItem = withContext(ioDispatcher) {
        ensureRootAccess()

        val trimmedName = newName.trim()
        if (trimmedName.isEmpty() || trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName == ".." || trimmedName == ".") {
            throw IllegalArgumentException("Invalid name: $newName")
        }

        val sourceFile = SuFile(location.path)
        if (!sourceFile.exists()) {
            throw FileNotFoundException("File not found in root: ${location.path}")
        }

        val parentPath = if (location.path.contains("/")) location.path.substringBeforeLast("/") else ""
        val targetPath = if (parentPath.isEmpty()) "/$trimmedName" else "$parentPath/$trimmedName"
        val targetFile = SuFile(targetPath)

        if (targetFile.exists()) {
            throw IOException("Target already exists in root: $targetPath")
        }

        sourceFile.renameTo(targetFile) || Shell.cmd("mv ${escapeShellArg(location.path)} ${escapeShellArg(targetPath)}").exec().isSuccess

        if (!targetFile.exists()) {
            throw IOException("Failed to rename root file: ${location.path}")
        }

        val isDir = targetFile.isDirectory
        val metadata = FileMetadata(
            size = if (isDir) 0L else targetFile.length(),
            modifiedTime = targetFile.lastModified(),
            createdTime = null,
            isReadable = true,
            isWritable = true,
            isExecutable = true,
            isHidden = trimmedName.startsWith(".")
        )

        FileItem(
            id = targetPath,
            name = trimmedName,
            location = StorageLocation(path = targetPath, rootId = location.rootId),
            type = if (isDir) FileType.DIRECTORY else FileType.FILE,
            metadata = metadata
        )
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Mengambil detail FileItem dari berkas root untuk keperluan indexing dan stat.
    override suspend fun getFileItem(location: StorageLocation): FileItem? = withContext(ioDispatcher) {
        if (!isAvailable()) return@withContext null
        val suFile = SuFile(location.path)
        if (!suFile.exists()) return@withContext null

        val isDir = suFile.isDirectory
        val name = suFile.name.ifEmpty { location.path.substringAfterLast("/") }
        val metadata = FileMetadata(
            size = if (isDir) 0L else suFile.length(),
            modifiedTime = suFile.lastModified(),
            createdTime = null,
            isReadable = suFile.canRead(),
            isWritable = suFile.canWrite(),
            isExecutable = suFile.canExecute(),
            isHidden = suFile.isHidden || name.startsWith(".")
        )

        FileItem(
            id = location.path,
            name = name.ifEmpty { StorageConstants.DEFAULT_UNKNOWN_FILE_NAME },
            location = StorageLocation(path = location.path, rootId = location.rootId),
            type = if (isDir) FileType.DIRECTORY else FileType.FILE,
            metadata = metadata
        )
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Menyalin berkas/folder root ke tujuan root dengan streaming I/O dan pelaporan progres berulang.
    override fun copy(
        source: StorageLocation,
        destination: StorageLocation
    ): Flow<FileOperationProgress> = flow {
        ensureRootAccess()

        val sourceFile = SuFile(source.path)
        val destFile = SuFile(destination.path)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found in root: ${source.path}")
        }
        if (sourceFile.absolutePath == destFile.absolutePath) {
            throw IllegalArgumentException("Source and destination are the same")
        }
        if (sourceFile.isDirectory && destFile.absolutePath.startsWith(sourceFile.absolutePath + File.separator)) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        var totalCopied = 0L
        val totalBytes = if (sourceFile.isDirectory) calculateTotalSize(sourceFile) else sourceFile.length()

        if (totalBytes == 0L) {
            emit(FileOperationProgress(0L, 0L, sourceFile.name))
        }

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
    }.flowOn(ioDispatcher)

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Memindahkan berkas/folder root secara atomic rename atau fallback copy-delete dengan verifikasi integritas berkas tujuan sebelum menghapus sumber untuk mencegah kehilangan data.
    override fun move(
        source: StorageLocation,
        destination: StorageLocation
    ): Flow<FileOperationProgress> = flow {
        ensureRootAccess()

        val sourceFile = SuFile(source.path)
        val destFile = SuFile(destination.path)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found in root: ${source.path}")
        }
        if (sourceFile.absolutePath == destFile.absolutePath) {
            throw IOException("Source and destination are the same")
        }
        if (sourceFile.isDirectory && destFile.absolutePath.startsWith(sourceFile.absolutePath + File.separator)) {
            throw IOException("Cannot move a directory into itself")
        }

        val sourceLength = if (sourceFile.isFile) sourceFile.length() else 0L
        val isSourceDir = sourceFile.isDirectory

        val renamed = sourceFile.renameTo(destFile) || Shell.cmd("mv ${escapeShellArg(source.path)} ${escapeShellArg(destination.path)}").exec().isSuccess

        if (destFile.exists() && !sourceFile.exists()) {
            emit(FileOperationProgress(destFile.length(), destFile.length(), destFile.name))
            return@flow
        }

        copy(source, destination).collect { progress ->
            emit(progress)
        }

        if (currentCoroutineContext().isActive) {
            if (!destFile.exists()) {
                throw IOException("Move failed: destination does not exist after copy (${destination.path})")
            }
            if (!isSourceDir && destFile.length() != sourceLength) {
                try { destFile.delete() } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
                throw IOException("Move failed: partial copy detected (destination size mismatch)")
            }
            delete(source)
        }
    }.flowOn(ioDispatcher)

    private fun ensureRootAccess() {
        if (!isAvailable()) {
            throw SecurityException("Root access is not available or permission denied")
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt
    // [Penjelasan]: Menyalin satu berkas root dengan SuFileInputStream/SuFileOutputStream, verifikasi pembatalan, pembersihan berkas parsial, dan proteksi partial copy.
    private suspend fun copySingleFile(
        source: SuFile,
        dest: SuFile,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        dest.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }

        val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)
        try {
            SuFileInputStream.open(source).use { input ->
                SuFileOutputStream.open(dest).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        if (!currentCoroutineContext().isActive) {
                            throw CancellationException("Root copy operation cancelled")
                        }
                        output.write(buffer, 0, bytesRead)
                        onProgress(bytesRead.toLong(), source.name)
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
            } catch (_: Exception) {
            }
            throw e
        }
    }

    private suspend fun copyDirectoryRecursively(
        sourceDir: SuFile,
        destDir: SuFile,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val children = sourceDir.listFiles() ?: return
        for (child in children) {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Root copy operation cancelled")
            }
            val targetChild = SuFile(destDir, child.name)
            if (child.isDirectory) {
                copyDirectoryRecursively(child, targetChild, totalBytes, onProgress)
            } else {
                copySingleFile(child, targetChild, totalBytes, onProgress)
            }
        }
    }

    private suspend fun deleteDirectoryRecursively(dir: SuFile) {
        val stack = ArrayDeque<SuFile>()
        stack.addLast(dir)
        val filesToDelete = ArrayDeque<SuFile>()

        while (stack.isNotEmpty()) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) throw kotlinx.coroutines.CancellationException()
            val current = stack.removeLast()
            filesToDelete.addFirst(current)

            if (current.isDirectory) {
                val children = current.listFiles() ?: continue
                for (child in children) {
                    stack.addLast(child)
                }
            }
        }

        for (file in filesToDelete) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) throw kotlinx.coroutines.CancellationException()
            if (!file.delete() && file.exists()) {
                throw IOException("Failed to delete root file: ${file.absolutePath}")
            }
        }
    }

    private fun calculateTotalSize(dir: SuFile): Long {
        var size = 0L
        val queue = ArrayDeque<SuFile>()
        queue.add(dir)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = current.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    queue.add(child)
                } else {
                    size += child.length()
                }
            }
        }
        return size
    }

    private fun isProtectedRootPath(path: String): Boolean {
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

    private fun escapeShellArg(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }
}
