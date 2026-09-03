// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
// [Penjelasan]: Implementasi FileRepository dengan routing sistem berkas berbasis StorageBackendClassifier dan CrossFilesystemTransferBridge untuk transfer berkas antar-filesystem (Local, SAF, Shizuku, Root) tanpa asumsi path biner dan tanpa menyamarkan error.

package com.wakwau.xplore.core.storage.repository

import com.wakwau.xplore.core.storage.error.StorageErrorMapper
import com.wakwau.xplore.core.storage.filesystem.LocalFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.RootFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.SafFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.ShizukuFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.StorageBackendClassifier
import com.wakwau.xplore.core.storage.filesystem.StorageBackendType
import com.wakwau.xplore.core.storage.filesystem.bridge.CrossFilesystemTransferBridge
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.core.storage.search.FileIndexSynchronizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class FileRepositoryImpl(
    private val localFileSystem: LocalFileSystemContract,
    private val safFileSystem: SafFileSystemContract,
    private val safShizukuFileSystem: ShizukuFileSystemContract,
    private val rootFileSystem: RootFileSystemContract,
    private val crossFilesystemTransferBridge: CrossFilesystemTransferBridge? = null,
    private val backendClassifier: StorageBackendClassifier,
    private val storageErrorMapper: StorageErrorMapper,
    private val fileIndexSynchronizer: FileIndexSynchronizer? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileRepository {

    override suspend fun delete(location: StorageLocation): FileOperationResult<Unit> = withContext(ioDispatcher) {
        try {
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Mendelegasikan penghapusan ke driver filesystem yang sesuai berdasarkan klasifikasi backend.
            when (backendClassifier.classify(location)) {
                StorageBackendType.ROOT -> rootFileSystem.delete(location)
                StorageBackendType.SAF -> safFileSystem.delete(location)
                StorageBackendType.SHIZUKU -> safShizukuFileSystem.delete(location)
                StorageBackendType.LOCAL -> localFileSystem.delete(location)
            }
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Menghapus entitas berkas/direktori dan seluruh hierarkinya dari indeks Room DB secara atomik dalam satu transaksi.
            fileIndexSynchronizer?.removeSingle(location.path)
            FileOperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FileOperationResult.Failure(storageErrorMapper.map(e))
        }
    }

    override suspend fun rename(location: StorageLocation, newName: String): FileOperationResult<FileItem> = withContext(ioDispatcher) {
        try {
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Mendelegasikan penggantian nama ke driver filesystem yang sesuai berdasarkan klasifikasi backend.
            val fileItem = when (backendClassifier.classify(location)) {
                StorageBackendType.ROOT -> rootFileSystem.rename(location, newName)
                StorageBackendType.SAF -> safFileSystem.rename(location, newName)
                StorageBackendType.SHIZUKU -> safShizukuFileSystem.rename(location, newName)
                StorageBackendType.LOCAL -> localFileSystem.rename(location, newName)
            }
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Memperbarui indeks Room DB secara atomik dengan menghapus path lama (beserta sub-path) dan menyimpan entitas baru dalam satu transaksi.
            fileIndexSynchronizer?.syncRename(location.path, fileItem)
            FileOperationResult.Success(fileItem)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FileOperationResult.Failure(storageErrorMapper.map(e))
        }
    }

    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationResult<FileOperationProgress>> = flow {
        try {
            val sourceType = backendClassifier.classify(source)
            val destType = backendClassifier.classify(destination)

            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Memilih operasi salin internal driver jika tipe filesystem sama, atau mengalihkan ke CrossFilesystemTransferBridge jika lintas backend.
            val progressFlow = if (sourceType == destType) {
                when (sourceType) {
                    StorageBackendType.LOCAL -> localFileSystem.copy(source, destination)
                    StorageBackendType.SAF -> safFileSystem.copy(source, destination)
                    StorageBackendType.SHIZUKU -> safShizukuFileSystem.copy(source, destination)
                    StorageBackendType.ROOT -> rootFileSystem.copy(source, destination)
                }
            } else {
                val bridge = crossFilesystemTransferBridge ?: throw IllegalStateException("Cross-filesystem transfer bridge is not available")
                bridge.copyCross(source, destination, sourceType, destType)
            }

            progressFlow.collect { progress ->
                emit(FileOperationResult.Success(progress))
            }
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Menyinkronkan entitas berkas/direktori tujuan di indeks Room DB (FileIndexDao) setelah eksekusi salin selesai.
            syncDestinationIndex(destination)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(FileOperationResult.Failure(storageErrorMapper.map(e)))
        }
    }

    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationResult<FileOperationProgress>> = flow {
        try {
            val sourceType = backendClassifier.classify(source)
            val destType = backendClassifier.classify(destination)

            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Memilih operasi pindah internal driver jika tipe filesystem sama, atau mengalihkan ke CrossFilesystemTransferBridge jika lintas backend.
            val progressFlow = if (sourceType == destType) {
                when (sourceType) {
                    StorageBackendType.LOCAL -> localFileSystem.move(source, destination)
                    StorageBackendType.SAF -> safFileSystem.move(source, destination)
                    StorageBackendType.SHIZUKU -> safShizukuFileSystem.move(source, destination)
                    StorageBackendType.ROOT -> rootFileSystem.move(source, destination)
                }
            } else {
                val bridge = crossFilesystemTransferBridge ?: throw IllegalStateException("Cross-filesystem transfer bridge is not available")
                bridge.moveCross(source, destination, sourceType, destType)
            }

            progressFlow.collect { progress ->
                emit(FileOperationResult.Success(progress))
            }
            // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
            // [Penjelasan]: Menghapus indeks sumber dan menyinkronkan entitas tujuan di indeks Room DB (FileIndexDao) secara atomik (@Transaction) setelah eksekusi pindah selesai.
            val destItem = getDestinationFileItem(destination)
            if (destItem != null) {
                fileIndexSynchronizer?.syncMove(source.path, destItem)
            } else {
                fileIndexSynchronizer?.removeSingle(source.path)
                fileIndexSynchronizer?.removeByPrefix(source.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(FileOperationResult.Failure(storageErrorMapper.map(e)))
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
    // [Penjelasan]: Menyinkronkan entitas berkas/direktori tujuan ke indeks Room DB (FileIndexDao) menggunakan abstraksi filesystem yang sesuai (Local, SAF, Shizuku, Root) tanpa asumsi path biner.
    private suspend fun syncDestinationIndex(destination: StorageLocation) {
        getDestinationFileItem(destination)?.let { fileItem ->
            fileIndexSynchronizer?.syncSingle(fileItem)
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt
    // [Penjelasan]: Mengambil metadata FileItem tujuan berdasarkan klasifikasi driver backend aktif.
    private suspend fun getDestinationFileItem(destination: StorageLocation): FileItem? = when (backendClassifier.classify(destination)) {
        StorageBackendType.ROOT -> rootFileSystem.getFileItem(destination)
        StorageBackendType.SAF -> safFileSystem.getFileItem(destination)
        StorageBackendType.SHIZUKU -> safShizukuFileSystem.getFileItem(destination)
        StorageBackendType.LOCAL -> localFileSystem.getFileItem(destination)
    }
}


