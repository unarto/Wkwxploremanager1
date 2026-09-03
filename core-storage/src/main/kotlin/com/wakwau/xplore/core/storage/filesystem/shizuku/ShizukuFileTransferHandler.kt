// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/shizuku/ShizukuFileTransferHandler.kt
// [Penjelasan]: Mengelola operasi streaming byte untuk copy dan move lintas/dalam Shizuku filesystem, mem-bypass limit IPC dengan ParcelFileDescriptor, mencegah TransactionTooLargeException.
package com.wakwau.xplore.core.storage.filesystem.shizuku

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.shizuku.IPrivilegedFileService
import com.wakwau.xplore.core.storage.shizuku.ShizukuIpcConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive

class ShizukuFileTransferHandler {

    suspend fun copySingleFile(
        service: IPrivilegedFileService,
        sourcePath: String,
        destPath: String,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        val readFd = service.openFileForRead(sourcePath) 
            ?: throw IOException("Failed to open source file for reading in root: $sourcePath")
            
        val writeFd = service.openFileForWrite(destPath)
            ?: run {
                readFd.close()
                throw IOException("Failed to open destination file for writing in root: $destPath")
            }

        var sourceName = sourcePath.substringAfterLast("/")
        if (sourceName.isEmpty()) sourceName = StorageConstants.DEFAULT_UNKNOWN_FILE_NAME

        try {
            FileInputStream(readFd.fileDescriptor).use { input ->
                FileOutputStream(writeFd.fileDescriptor).use { output ->
                    val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            throw CancellationException("Copy cancelled")
                        }
                        output.write(buffer, 0, bytesRead)
                        onProgress(bytesRead.toLong(), sourceName)
                    }
                    output.flush()
                }
            }
            val isDir = service.isDirectory(sourcePath)
            if (!isDir) {
                val srcLen = service.length(sourcePath)
                val destLen = service.length(destPath)
                if (destLen != srcLen) {
                    throw IOException("Partial copy detected: destination size ($destLen) does not match source size ($srcLen)")
                }
            }
        } catch (e: Throwable) {
            try { service.delete(destPath) } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
            throw e
        } finally {
            try { readFd.close() } catch (e: Exception) { /* ignore */ }
            try { writeFd.close() } catch (e: Exception) { /* ignore */ }
        }
    }

    suspend fun copyDirectoryRecursively(
        service: IPrivilegedFileService,
        sourcePath: String,
        destPath: String,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        if (!service.exists(destPath)) {
            val created = service.createDirectory(destPath)
            if (!created) {
                throw IOException("Failed to create destination directory: $destPath")
            }
        }

        val children = service.listDirectory(sourcePath)
        for (bundle in children) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                throw CancellationException("Copy cancelled")
            }
            
            val childName = bundle.getString(ShizukuIpcConstants.KEY_NAME) ?: continue
            if (childName == "." || childName == "..") continue
            
            val childSourcePath = bundle.getString(ShizukuIpcConstants.KEY_PATH) ?: continue
            val isDirectory = bundle.getBoolean(ShizukuIpcConstants.KEY_IS_DIRECTORY)
            
            val childDestPath = if (destPath.endsWith("/")) "$destPath$childName" else "$destPath/$childName"

            if (isDirectory) {
                copyDirectoryRecursively(service, childSourcePath, childDestPath, totalBytes, onProgress)
            } else {
                copySingleFile(service, childSourcePath, childDestPath, totalBytes, onProgress)
            }
        }
    }

    fun calculateTotalSize(service: IPrivilegedFileService, path: String): Long {
        if (!service.exists(path)) return 0L
        if (!service.isDirectory(path)) {
            return service.length(path)
        }
        var size = 0L
        val queue = ArrayDeque<String>()
        queue.add(path)
        while (queue.isNotEmpty()) {
            val currentPath = queue.removeFirst()
            val children = service.listDirectory(currentPath)
            for (bundle in children) {
                val childPath = bundle.getString(ShizukuIpcConstants.KEY_PATH) ?: continue
                val isDirectory = bundle.getBoolean(ShizukuIpcConstants.KEY_IS_DIRECTORY)
                if (isDirectory) {
                    queue.add(childPath)
                } else {
                    size += bundle.getLong(ShizukuIpcConstants.KEY_SIZE)
                }
            }
        }
        return size
    }
}
