// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt
// [Penjelasan]: Bridge transfer streaming I/O lintas sistem berkas (Local <-> SAF <-> Shizuku <-> Root) yang menghubungkan InputStream sumber ke OutputStream tujuan dengan pelacakan progres, proteksi coroutine cancellation, dan integritas pemindahan data.
package com.wakwau.xplore.core.storage.filesystem.bridge

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.filesystem.LocalFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.RootFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.SafFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.ShizukuFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.StorageBackendType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.shizuku.IPrivilegedFileService
import com.wakwau.xplore.core.storage.shizuku.ShizukuHelper
import com.wakwau.xplore.core.storage.shizuku.ShizukuIpcConstants
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

open class CrossFilesystemTransferBridge(
    private val context: Context,
    private val localFileSystem: LocalFileSystemContract,
    private val safFileSystem: SafFileSystemContract,
    private val safShizukuFileSystem: ShizukuFileSystemContract,
    private val rootFileSystem: RootFileSystemContract
) {

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt
    // [Penjelasan]: Mengeksekusi penyalinan berkas/direktori dari sistem berkas sumber ke sistem berkas tujuan dengan streaming byte terisolasi.
    open fun copyCross(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType
    ): Flow<FileOperationProgress> = flow {
        val totalBytes = calculateTotalSourceSize(source, sourceType)
        var totalCopied = 0L

        if (totalBytes == 0L) {
            val sourceName = getSourceName(source, sourceType)
            emit(FileOperationProgress(0L, 0L, sourceName))
        }

        val isSourceDir = isSourceDirectory(source, sourceType)
        if (isSourceDir) {
            copyDirectoryCrossRecursively(
                source = source,
                destination = destination,
                sourceType = sourceType,
                destType = destType,
                totalBytes = totalBytes
            ) { bytes, fileName ->
                totalCopied += bytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        } else {
            copySingleFileCross(
                source = source,
                destination = destination,
                sourceType = sourceType,
                destType = destType,
                totalBytes = totalBytes
            ) { bytes, fileName ->
                totalCopied += bytes
                emit(FileOperationProgress(totalCopied, totalBytes, fileName))
            }
        }
    }.flowOn(Dispatchers.IO)

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt
    // [Penjelasan]: Integritas rollback & commit: validasi transfer selesai 100% dan coroutine aktif sebelum menghapus sumber; rollback jika transfer gagal/batal.
    open fun moveCross(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType
    ): Flow<FileOperationProgress> = flow {
        val isSourceDir = isSourceDirectory(source, sourceType)
        val sourceSize = if (!isSourceDir) calculateTotalSourceSize(source, sourceType) else 0L

        try {
            copyCross(source, destination, sourceType, destType).collect { progress ->
                emit(progress)
            }
            if (kotlinx.coroutines.currentCoroutineContext().isActive) {
                validateTransferComplete(source, destination, sourceType, destType, isSourceDir, sourceSize)
                deleteSource(source, sourceType)
            }
        } catch (e: Throwable) {
            rollbackDestination(source, destination, sourceType, destType)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun copySingleFileCross(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        val sourceName = getSourceName(source, sourceType)
        val inStream = openSourceInputStream(source, sourceType)
        var outHandle: OutputHandle? = null

        val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)
        try {
            outHandle = openDestOutputStream(sourceName, destination, destType)
            inStream.use { input ->
                outHandle.outputStream.use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            outHandle.cleanup()
                            throw CancellationException("Cross-filesystem copy cancelled")
                        }
                        output.write(buffer, 0, bytesRead)
                        onProgress(bytesRead.toLong(), sourceName)
                    }
                }
            }
        } catch (e: CancellationException) {
            outHandle?.cleanup()
            inStream.close()
            throw e
        } catch (e: Exception) {
            outHandle?.cleanup()
            inStream.close()
            throw e
        }
    }

    private suspend fun copyDirectoryCrossRecursively(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        val sourceDirName = getSourceName(source, sourceType)
        val targetDestLocation = createDestDirectory(sourceDirName, destination, destType)
        val children = listSourceChildren(source, sourceType)

        for (child in children) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                throw CancellationException("Cross-filesystem directory copy cancelled")
            }
            val isChildDir = isSourceDirectory(child, sourceType)
            if (isChildDir) {
                copyDirectoryCrossRecursively(
                    source = child,
                    destination = targetDestLocation,
                    sourceType = sourceType,
                    destType = destType,
                    totalBytes = totalBytes,
                    onProgress = onProgress
                )
            } else {
                copySingleFileCross(
                    source = child,
                    destination = targetDestLocation,
                    sourceType = sourceType,
                    destType = destType,
                    totalBytes = totalBytes,
                    onProgress = onProgress
                )
            }
        }
    }

    private suspend fun openSourceInputStream(source: StorageLocation, sourceType: StorageBackendType): InputStream = when (sourceType) {
        StorageBackendType.LOCAL -> {
            val file = File(source.path)
            if (!file.exists()) throw FileNotFoundException("Source local file not found: ${source.path}")
            FileInputStream(file)
        }
        StorageBackendType.SAF -> {
            val doc = resolveSafDocument(Uri.parse(source.path)) ?: throw FileNotFoundException("Source SAF file not found: ${source.path}")
            context.contentResolver.openInputStream(doc.uri) ?: throw FileNotFoundException("Cannot open SAF input stream: ${source.path}")
        }
        StorageBackendType.SHIZUKU -> {
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt
            // [Penjelasan]: Menggunakan AutoCloseInputStream agar ParcelFileDescriptor IPC tertutup otomatis saat stream selesai dibaca.
            val pfd = getShizukuService().openFileForRead(source.path) ?: throw IOException("Cannot open Shizuku input stream: ${source.path}")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)
        }
        StorageBackendType.ROOT -> SuFileInputStream.open(SuFile(source.path))
    }

    private data class OutputHandle(val outputStream: OutputStream, val cleanup: () -> Unit)

    private suspend fun openDestOutputStream(
        sourceFileName: String,
        destination: StorageLocation,
        destType: StorageBackendType
    ): OutputHandle = when (destType) {
        StorageBackendType.LOCAL -> {
            val destFile = resolveLocalDestFile(sourceFileName, destination.path)
            destFile.parentFile?.mkdirs()
            val outStream = FileOutputStream(destFile)
            OutputHandle(outStream) { try { destFile.delete() } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) } }
        }
        StorageBackendType.SAF -> {
            val destDoc = resolveSafDocument(Uri.parse(destination.path))
                ?: throw FileNotFoundException("Destination SAF folder not found: ${destination.path}")
            val targetFileDoc = if (destDoc.isDirectory) {
                destDoc.createFile(StorageConstants.DEFAULT_MIME_TYPE_ALL, sourceFileName)
                    ?: throw IOException("Failed to create SAF destination file: $sourceFileName")
            } else {
                destDoc
            }
            val outStream = context.contentResolver.openOutputStream(targetFileDoc.uri)
                ?: throw IOException("Cannot open SAF output stream: ${targetFileDoc.uri}")
            OutputHandle(outStream) { try { targetFileDoc.delete() } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) } }
        }
        StorageBackendType.SHIZUKU -> {
            val service = getShizukuService()
            val destFilePath = resolveShizukuDestFilePath(sourceFileName, destination.path)
            val pfd = service.openFileForWrite(destFilePath)
                ?: throw IOException("Cannot open Shizuku output stream: $destFilePath")
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt
            // [Penjelasan]: Menggunakan AutoCloseOutputStream agar ParcelFileDescriptor tujuan tertutup otomatis saat streaming selesai/gagal.
            val outStream = android.os.ParcelFileDescriptor.AutoCloseOutputStream(pfd)
            OutputHandle(outStream) { try { service.delete(destFilePath) } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) } }
        }
        StorageBackendType.ROOT -> {
            val destFilePath = resolveRootDestFilePath(sourceFileName, destination.path)
            val destFile = SuFile(destFilePath)
            destFile.parentFile?.let { SuFile(it.absolutePath).mkdirs() }
            val outStream = SuFileOutputStream.open(destFile)
            OutputHandle(outStream) { try { destFile.delete() } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) } }
        }
    }

    private suspend fun createDestDirectory(
        dirName: String,
        destination: StorageLocation,
        destType: StorageBackendType
    ): StorageLocation {
        return when (destType) {
            StorageBackendType.LOCAL -> {
                val destParent = File(destination.path)
                val targetDir = if (destParent.isDirectory) File(destParent, dirName) else destParent
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                StorageLocation(targetDir.absolutePath, destination.rootId)
            }
            StorageBackendType.SAF -> {
                val parentDoc = resolveSafDocument(Uri.parse(destination.path))
                    ?: throw FileNotFoundException("Destination SAF parent folder not found: ${destination.path}")
                val targetDoc = if (parentDoc.isDirectory) {
                    parentDoc.findFile(dirName)?.takeIf { it.isDirectory }
                        ?: parentDoc.createDirectory(dirName)
                        ?: throw IOException("Failed to create SAF subfolder: $dirName")
                } else {
                    parentDoc
                }
                StorageLocation(targetDoc.uri.toString(), destination.rootId)
            }
            StorageBackendType.SHIZUKU -> {
                val service = getShizukuService()
                val targetPath = resolveShizukuDestFilePath(dirName, destination.path)
                if (!service.exists(targetPath)) {
                    service.createDirectory(targetPath)
                }
                StorageLocation(targetPath, destination.rootId)
            }
            StorageBackendType.ROOT -> {
                val targetPath = resolveRootDestFilePath(dirName, destination.path)
                val dir = SuFile(targetPath)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                StorageLocation(targetPath, destination.rootId)
            }
        }
    }

    private suspend fun isSourceDirectory(source: StorageLocation, sourceType: StorageBackendType): Boolean = when (sourceType) {
        StorageBackendType.LOCAL -> File(source.path).isDirectory
        StorageBackendType.SAF -> resolveSafDocument(Uri.parse(source.path))?.let { try { it.isDirectory } catch (_: Exception) { false } } ?: false
        StorageBackendType.SHIZUKU -> getShizukuService().isDirectory(source.path)
        StorageBackendType.ROOT -> SuFile(source.path).isDirectory
    }

    private fun getSourceName(source: StorageLocation, sourceType: StorageBackendType): String = when (sourceType) {
        StorageBackendType.LOCAL -> File(source.path).name
        StorageBackendType.SAF -> resolveSafDocument(Uri.parse(source.path))?.name ?: StorageConstants.DEFAULT_UNKNOWN_FILE_NAME
        StorageBackendType.SHIZUKU -> source.path.trimEnd('/').substringAfterLast('/')
        StorageBackendType.ROOT -> SuFile(source.path).name.ifEmpty { source.path.trimEnd('/').substringAfterLast('/') }
    }

    private suspend fun listSourceChildren(source: StorageLocation, sourceType: StorageBackendType): List<StorageLocation> = when (sourceType) {
        StorageBackendType.LOCAL -> (File(source.path).listFiles() ?: emptyArray()).map { StorageLocation(it.absolutePath, source.rootId) }
        StorageBackendType.SAF -> {
            val doc = resolveSafDocument(Uri.parse(source.path))
            val children = if (doc != null) (try { doc.listFiles() } catch (_: Exception) { emptyArray() }) else emptyArray()
            children.map { StorageLocation(it.uri.toString(), source.rootId) }
        }
        StorageBackendType.SHIZUKU -> getShizukuService().listDirectory(source.path).mapNotNull { bundle ->
            val path = bundle.getString(ShizukuIpcConstants.KEY_PATH)
            val name = bundle.getString(ShizukuIpcConstants.KEY_NAME)
            if (path != null && name != "." && name != "..") StorageLocation(path, source.rootId) else null
        }
        StorageBackendType.ROOT -> (SuFile(source.path).listFiles() ?: emptyArray()).map { StorageLocation(it.absolutePath, source.rootId) }
    }

    private suspend fun calculateTotalSourceSize(source: StorageLocation, sourceType: StorageBackendType): Long = when (sourceType) {
        StorageBackendType.LOCAL -> calculateLocalSize(File(source.path))
        StorageBackendType.SAF -> resolveSafDocument(Uri.parse(source.path))?.let { calculateSafSize(it) } ?: 0L
        StorageBackendType.SHIZUKU -> calculateShizukuSize(getShizukuService(), source.path)
        StorageBackendType.ROOT -> calculateSuSize(SuFile(source.path))
    }

    private fun calculateLocalSize(file: File): Long {
        if (!file.exists()) return 0L
        if (!file.isDirectory) return file.length()
        return file.listFiles()?.sumOf { calculateLocalSize(it) } ?: 0L
    }

    private fun calculateSafSize(doc: DocumentFile): Long {
        if (!try { doc.isDirectory } catch (_: Exception) { false }) return doc.length()
        return (try { doc.listFiles() } catch (_: Exception) { emptyArray() }).sumOf { calculateSafSize(it) }
    }

    private fun calculateShizukuSize(service: IPrivilegedFileService, path: String): Long {
        if (!service.exists(path)) return 0L
        if (!service.isDirectory(path)) return service.length(path)
        var size = 0L
        val children = service.listDirectory(path)
        for (bundle in children) {
            val childPath = bundle.getString(ShizukuIpcConstants.KEY_PATH) ?: continue
            val isDir = bundle.getBoolean(ShizukuIpcConstants.KEY_IS_DIRECTORY)
            size += if (isDir) calculateShizukuSize(service, childPath) else bundle.getLong(ShizukuIpcConstants.KEY_SIZE)
        }
        return size
    }

    private fun calculateSuSize(file: SuFile): Long {
        if (!file.exists()) return 0L
        if (!file.isDirectory) return file.length()
        return file.listFiles()?.sumOf { calculateSuSize(it) } ?: 0L
    }

    private suspend fun validateTransferComplete(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType,
        isSourceDir: Boolean,
        expectedSize: Long
    ) {
        val sourceName = getSourceName(source, sourceType)
        val destSize = getDestFileSize(sourceName, destination, destType)
        if (!isSourceDir) {
            if (destSize < 0 || (expectedSize > 0 && destSize != expectedSize)) {
                rollbackDestination(source, destination, sourceType, destType)
                throw IOException("Cross-filesystem move validation failed: destination file incomplete or size mismatch")
            }
        } else if (destSize < 0) {
            rollbackDestination(source, destination, sourceType, destType)
            throw IOException("Cross-filesystem move validation failed: destination directory not found")
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt
    // [Penjelasan]: Rollback pemindahan data lintas filesystem jika transfer gagal di tengah jalan untuk mencegah berkas yatim/korup.
    private suspend fun rollbackDestination(
        source: StorageLocation,
        destination: StorageLocation,
        sourceType: StorageBackendType,
        destType: StorageBackendType
    ) {
        try {
            val sourceName = getSourceName(source, sourceType)
            val targetLocation = when (destType) {
                StorageBackendType.LOCAL -> StorageLocation(resolveLocalDestFile(sourceName, destination.path).absolutePath, destination.rootId)
                StorageBackendType.SAF -> {
                    val doc = resolveSafDocument(Uri.parse(destination.path))
                    val target = if (doc?.isDirectory == true) doc.findFile(sourceName) else doc
                    target?.let { StorageLocation(it.uri.toString(), destination.rootId) }
                }
                StorageBackendType.SHIZUKU -> StorageLocation(resolveShizukuDestFilePath(sourceName, destination.path), destination.rootId)
                StorageBackendType.ROOT -> StorageLocation(resolveRootDestFilePath(sourceName, destination.path), destination.rootId)
            }
            if (targetLocation != null) {
                deleteSource(targetLocation, destType)
            }
        } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
    }

    private suspend fun getDestFileSize(sourceFileName: String, destination: StorageLocation, destType: StorageBackendType): Long = when (destType) {
        StorageBackendType.LOCAL -> {
            val file = resolveLocalDestFile(sourceFileName, destination.path)
            if (file.exists()) (if (file.isDirectory) 0L else file.length()) else -1L
        }
        StorageBackendType.SAF -> {
            val doc = resolveSafDocument(Uri.parse(destination.path))
            if (doc != null && doc.exists()) {
                val target = if (doc.isDirectory) doc.findFile(sourceFileName) else doc
                if (target != null && target.exists()) (if (target.isDirectory) 0L else target.length()) else -1L
            } else -1L
        }
        StorageBackendType.SHIZUKU -> {
            val service = getShizukuService()
            val path = resolveShizukuDestFilePath(sourceFileName, destination.path)
            if (service.exists(path)) (if (service.isDirectory(path)) 0L else service.length(path)) else -1L
        }
        StorageBackendType.ROOT -> {
            val file = SuFile(resolveRootDestFilePath(sourceFileName, destination.path))
            if (file.exists()) (if (file.isDirectory) 0L else file.length()) else -1L
        }
    }

    private suspend fun deleteSource(source: StorageLocation, sourceType: StorageBackendType) = when (sourceType) {
        StorageBackendType.LOCAL -> localFileSystem.delete(source)
        StorageBackendType.SAF -> safFileSystem.delete(source)
        StorageBackendType.SHIZUKU -> safShizukuFileSystem.delete(source)
        StorageBackendType.ROOT -> rootFileSystem.delete(source)
    }

    private fun resolveLocalDestFile(sourceName: String, destPath: String): File =
        File(destPath).let { if (it.isDirectory) File(it, sourceName) else it }

    private fun resolveShizukuDestFilePath(sourceName: String, destPath: String): String =
        if (destPath.endsWith("/")) "$destPath$sourceName" else "$destPath/$sourceName"

    private fun resolveRootDestFilePath(sourceName: String, destPath: String): String =
        if (destPath.endsWith("/")) "$destPath$sourceName" else "$destPath/$sourceName"

    private fun resolveSafDocument(uri: Uri): DocumentFile? = try {
        DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
    } catch (_: Exception) { null }

    private suspend fun getShizukuService(): IPrivilegedFileService =
        ShizukuHelper.getPrivilegedService(context.packageName) ?: throw FileNotFoundException("Root/Shizuku service not available")
}

