// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
// [Penjelasan]: Implementasi filesystem nyata untuk Storage Access Framework (SAF) URI menggunakan Android DocumentFile dan ContentResolver dengan penanganan eksplisit IllegalArgumentException untuk tree URI dan single-document URI.

package com.wakwau.xplore.core.storage.filesystem.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.filesystem.SafFileSystemContract
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

class SafFileSystem(private val context: Context) : SafFileSystemContract {

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Mendaftar berkas di direktori SAF dengan resolusi aman tree URI dan proteksi dari IllegalArgumentException.
    override suspend fun listFiles(location: StorageLocation, showHidden: Boolean): List<FileItem> {
        val uri = Uri.parse(location.path)
        val documentFile = resolveTreeDocumentFile(uri)
            ?: resolveDocumentFile(uri)
            ?: throw FileNotFoundException("Invalid SAF URI or not a tree URI: ${location.path}")

        val exists = documentFile.exists()
        val isDirectory = documentFile.isDirectory

        if (!exists || !isDirectory) {
            throw FileNotFoundException("Directory not found or is not a directory: ${location.path}")
        }

        val files = documentFile.listFiles() ?: emptyArray()

        return files.filter { file ->
            if (!showHidden) {
                file.name?.startsWith(".") != true
            } else {
                true
            }
        }.map { file ->
            val type = if (file.isDirectory) FileType.DIRECTORY else FileType.FILE
            val itemUri = file.uri.toString()
            val metadata = FileMetadata(
                size = if (file.isFile) file.length() else 0L,
                modifiedTime = file.lastModified(),
                createdTime = null,
                isReadable = file.canRead(),
                isWritable = file.canWrite(),
                isExecutable = false,
                isHidden = file.name?.startsWith(".") == true
            )
            FileItem(
                id = itemUri,
                name = file.name ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME,
                location = StorageLocation(itemUri, location.rootId),
                type = type,
                metadata = metadata
            )
        }.sortedWith(compareBy({ it.type != FileType.DIRECTORY }, { it.name.lowercase() }))
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Membuat direktori baru melalui SAF dengan dukungan resolusi aman tree/single URI dan penanganan eksplisit IllegalArgumentException.
    override suspend fun createDirectory(location: StorageLocation, name: String): FileItem {
        val trimmedName = name.trim()
        val uri = Uri.parse(location.path)
        val parentDoc = resolveTreeDocumentFile(uri)
            ?: resolveDocumentFile(uri)
            ?: throw FileNotFoundException("Invalid SAF parent URI: ${location.path}")

        val actualParentDoc = if (parentDoc.exists() && !parentDoc.isDirectory) {
            parentDoc.parentFile ?: parentDoc
        } else {
            parentDoc
        }

        if (!actualParentDoc.exists() || !actualParentDoc.isDirectory) {
            throw FileNotFoundException("Parent directory not found in SAF: ${location.path}")
        }

        val createdDir = try {
            actualParentDoc.createDirectory(trimmedName)
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid argument when creating directory via SAF: $trimmedName", e)
        } ?: throw IOException("Failed to create directory via SAF: $trimmedName")

        val createdUri = createdDir.uri.toString()
        val metadata = FileMetadata(
            size = 0L,
            modifiedTime = createdDir.lastModified(),
            createdTime = null,
            isReadable = createdDir.canRead(),
            isWritable = createdDir.canWrite(),
            isExecutable = false,
            isHidden = trimmedName.startsWith(".")
        )

        return FileItem(
            id = createdUri,
            name = createdDir.name ?: trimmedName,
            location = StorageLocation(path = createdUri, rootId = location.rootId),
            type = FileType.DIRECTORY,
            metadata = metadata
        )
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Menghapus berkas/direktori SAF dengan resolusi aman tree/single URI dan proteksi IllegalArgumentException.
    override suspend fun delete(location: StorageLocation) {
        val pathClean = location.path.trim().trimEnd('/')
        if (pathClean.isEmpty() || pathClean == "/" || pathClean.equals("/storage", ignoreCase = true) || pathClean.equals("/storage/emulated", ignoreCase = true)) {
            throw SecurityException("Cannot delete root or protected storage path: ${location.path}")
        }
        val uri = Uri.parse(location.path)
        val documentFile = resolveDocumentFile(uri)
            ?: throw FileNotFoundException("Invalid SAF URI: ${location.path}")

        val exists = documentFile.exists()
        if (!exists) {
            throw FileNotFoundException("SAF file not found: ${location.path}")
        }

        val isDeleted = try {
            documentFile.delete()
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid argument when deleting SAF file: ${location.path}", e)
        }
        if (!isDeleted) {
            throw IOException("Failed to delete SAF file: ${location.path}")
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Mengubah nama berkas/direktori SAF dengan resolusi aman tree/single URI dan proteksi IllegalArgumentException.
    override suspend fun rename(location: StorageLocation, newName: String): FileItem {
        val uri = Uri.parse(location.path)
        val documentFile = resolveDocumentFile(uri)
            ?: throw FileNotFoundException("Invalid SAF URI: ${location.path}")

        val exists = documentFile.exists()
        if (!exists) {
            throw FileNotFoundException("SAF file not found: ${location.path}")
        }

        val isRenamed = try {
            documentFile.renameTo(newName)
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid argument when renaming SAF file to: $newName", e)
        }

        if (!isRenamed) {
            throw IOException("Failed to rename SAF file to: $newName")
        }

        val updatedUri = documentFile.uri.toString()
        val isDir = documentFile.isDirectory
        val metadata = FileMetadata(
            size = if (isDir) 0L else documentFile.length(),
            modifiedTime = documentFile.lastModified(),
            createdTime = null,
            isReadable = documentFile.canRead(),
            isWritable = documentFile.canWrite(),
            isExecutable = false,
            isHidden = newName.startsWith(".")
        )

        return FileItem(
            id = updatedUri,
            name = documentFile.name ?: newName,
            location = StorageLocation(path = updatedUri, rootId = location.rootId),
            type = if (isDir) FileType.DIRECTORY else FileType.FILE,
            metadata = metadata
        )
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Menyalin berkas tunggal atau folder secara rekursif melalui Storage Access Framework (SAF) dengan pelacakan progres berkala dan dukungan pembatalan coroutine.
    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceDoc = resolveDocumentFile(Uri.parse(source.path))
            ?: throw FileNotFoundException("Source SAF file not found: ${source.path}")
        val destDoc = resolveTreeDocumentFile(Uri.parse(destination.path))
            ?: resolveDocumentFile(Uri.parse(destination.path))
            ?: throw FileNotFoundException("Destination SAF folder not found: ${destination.path}")

        if (source.path == destination.path || sourceDoc.uri == destDoc.uri) {
            throw IllegalArgumentException("Source and destination are the same")
        }

        val isSourceDir = sourceDoc.isDirectory

        if (isSourceDir && destination.path.startsWith(source.path)) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        var totalCopied = 0L
        val totalBytes = if (isSourceDir) calculateTotalSize(sourceDoc) else sourceDoc.length()

        if (totalBytes == 0L) {
            emit(FileOperationProgress(0L, 0L, sourceDoc.name ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME))
        }

        if (isSourceDir) {
            copyDirectoryRecursively(sourceDoc, destDoc, totalBytes) { incrementalBytes, fileName ->
                totalCopied += incrementalBytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        } else {
            copySingleFile(sourceDoc, destDoc, totalBytes) { incrementalBytes, fileName ->
                totalCopied += incrementalBytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        }
    }.flowOn(Dispatchers.IO)

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Memindahkan berkas stream SAF dengan operasi copy stream dan verifikasi integritas berkas tujuan sebelum menghapus berkas sumber untuk mencegah data loss.
    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = flow {
        val sourceDoc = resolveDocumentFile(Uri.parse(source.path))
            ?: throw FileNotFoundException("Source not found: ${source.path}")
        val sourceSize = if (sourceDoc.isFile) sourceDoc.length() else 0L
        val isSourceDir = sourceDoc.isDirectory

        copy(source, destination).collect { progress ->
            emit(progress)
        }

        if (kotlinx.coroutines.currentCoroutineContext().isActive) {
            val destDoc = resolveDocumentFile(Uri.parse(destination.path))
            if (destDoc == null || !destDoc.exists()) {
                throw IOException("Move failed: destination does not exist after copy (${destination.path})")
            }
            if (!isSourceDir && destDoc.isFile && destDoc.length() != sourceSize) {
                try { destDoc.delete() } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
                throw IOException("Move failed: partial copy detected (destination size mismatch)")
            }
            delete(source)
        }
    }.flowOn(Dispatchers.IO)

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Mengambil metadata dan entitas FileItem dari DocumentFile SAF untuk sinkronisasi indeks Room DB atau inspeksi berkas dengan proteksi IllegalArgumentException.
    override suspend fun getFileItem(location: StorageLocation): FileItem? {
        val uri = Uri.parse(location.path)
        val documentFile = resolveDocumentFile(uri) ?: return null
        val exists = documentFile.exists()
        if (!exists) return null
        val isDir = documentFile.isDirectory
        val metadata = FileMetadata(
            size = if (isDir) 0L else documentFile.length(),
            modifiedTime = documentFile.lastModified(),
            createdTime = null,
            isReadable = documentFile.canRead(),
            isWritable = documentFile.canWrite(),
            isExecutable = false,
            isHidden = documentFile.name?.startsWith(".") == true
        )
        val itemUri = documentFile.uri.toString()
        return FileItem(
            id = itemUri,
            name = documentFile.name ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME,
            location = StorageLocation(itemUri, location.rootId),
            type = if (isDir) FileType.DIRECTORY else FileType.FILE,
            metadata = metadata
        )
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Memeriksa keberadaan berkas/folder SAF dengan proteksi IllegalArgumentException.
    override suspend fun exists(location: StorageLocation): Boolean {
        val uri = Uri.parse(location.path)
        val documentFile = resolveDocumentFile(uri) ?: return false
        return documentFile.exists()
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Resolusi DocumentFile secara aman dari URI dengan penanganan eksplisit IllegalArgumentException untuk tree URI maupun single URI.
    private fun resolveDocumentFile(uri: Uri): DocumentFile? {
        val treeDoc = resolveTreeDocumentFile(uri)
        if (treeDoc != null) {
            val exists = treeDoc.exists()
            if (exists) return treeDoc
        }

        val singleDoc = resolveSingleDocumentFile(uri)
        if (singleDoc != null) {
            val exists = singleDoc.exists()
            if (exists) return singleDoc
        }

        return treeDoc ?: singleDoc
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Resolusi DocumentFile tree URI dengan menangkap IllegalArgumentException secara eksplisit saat format bukan tree URI.
    private fun resolveTreeDocumentFile(uri: Uri): DocumentFile? {
        return try {
            DocumentFile.fromTreeUri(context, uri)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Resolusi DocumentFile single URI dengan menangkap IllegalArgumentException secara eksplisit saat format bukan single URI.
    private fun resolveSingleDocumentFile(uri: Uri): DocumentFile? {
        return try {
            DocumentFile.fromSingleUri(context, uri)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Menyalin satu berkas SAF ke target DocumentFile dengan streaming buffer, pengecekan isActive coroutine, verifikasi ukuran hasil salin untuk mencegah partial copy, dan pembersihan berkas parsial jika terjadi kegagalan atau pembatalan.
    private suspend fun copySingleFile(
        sourceDoc: DocumentFile,
        destDoc: DocumentFile,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        val isDestDir = destDoc.isDirectory

        val targetFile = if (isDestDir) {
            val mimeType = sourceDoc.type ?: StorageConstants.DEFAULT_MIME_TYPE_ALL
            val displayName = sourceDoc.name ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME
            try {
                destDoc.createFile(mimeType, displayName)
            } catch (e: IllegalArgumentException) {
                throw IOException("Invalid argument when creating destination SAF file: $displayName", e)
            } ?: throw IOException("Failed to create destination SAF file: $displayName")
        } else {
            destDoc
        }

        val fileName = sourceDoc.name ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME
        val input = context.contentResolver.openInputStream(sourceDoc.uri)
            ?: throw FileNotFoundException("Cannot open input stream: ${sourceDoc.uri}")
        val output = context.contentResolver.openOutputStream(targetFile.uri)
            ?: throw FileNotFoundException("Cannot open output stream: ${targetFile.uri}")

        val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)
        try {
            input.use { inStream ->
                output.use { outStream ->
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } >= 0) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            throw kotlinx.coroutines.CancellationException("Copy cancelled")
                        }
                        outStream.write(buffer, 0, bytesRead)
                        onProgress(bytesRead.toLong(), fileName)
                    }
                    outStream.flush()
                }
            }
            if (sourceDoc.isFile && targetFile.length() != sourceDoc.length()) {
                throw IOException("Partial copy detected: destination size (${targetFile.length()}) does not match source size (${sourceDoc.length()})")
            }
        } catch (e: Throwable) {
            if (isDestDir) {
                try {
                    targetFile.delete()
                } catch (_: Exception) {
                }
            }
            throw e
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Menyalin pohon direktori SAF secara rekursif termasuk seluruh subfolder dan berkas anak dengan verifikasi pembatalan coroutine.
    private suspend fun copyDirectoryRecursively(
        sourceDir: DocumentFile,
        destParentDir: DocumentFile,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        val dirName = sourceDir.name ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME
        val targetDir = try {
            destParentDir.findFile(dirName)?.takeIf { it.isDirectory }
                ?: destParentDir.createDirectory(dirName)
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid argument when creating SAF directory: $dirName", e)
        } ?: throw IOException("Failed to create SAF directory: $dirName")

        val children = sourceDir.listFiles() ?: emptyArray()

        for (child in children) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                throw kotlinx.coroutines.CancellationException("Copy cancelled")
            }
            val isChildDir = child.isDirectory
            if (isChildDir) {
                copyDirectoryRecursively(child, targetDir, totalBytes, onProgress)
            } else {
                copySingleFile(child, targetDir, totalBytes, onProgress)
            }
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt
    // [Penjelasan]: Menghitung total ukuran bytes secara rekursif dari direktori SAF untuk estimasi pelacakan progres.
    private fun calculateTotalSize(doc: DocumentFile): Long {
        var size = 0L
        val queue = ArrayDeque<DocumentFile>()
        queue.add(doc)
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
}
