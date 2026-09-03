// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/testutil/TestFileSystemContracts.kt
// [Penjelasan]: Implementasi stub FileSystemContract untuk pengujian unit repository dan conflict detector.
package com.wakwau.xplore.core.storage.testutil

import com.wakwau.xplore.core.storage.filesystem.RootFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.SafFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.ShizukuFileSystemContract
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class TestSafFileSystem : SafFileSystemContract {
    override suspend fun listFiles(location: StorageLocation, showHidden: Boolean): List<FileItem> = emptyList()
    override suspend fun createDirectory(location: StorageLocation, name: String): FileItem = throw UnsupportedOperationException()
    override suspend fun delete(location: StorageLocation) {}
    override suspend fun rename(location: StorageLocation, newName: String): FileItem = throw UnsupportedOperationException()
    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = emptyFlow()
    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = emptyFlow()
    override suspend fun getFileItem(location: StorageLocation): FileItem? = null
    override suspend fun exists(location: StorageLocation): Boolean = false
}

class TestShizukuFileSystem : ShizukuFileSystemContract {
    override suspend fun listFiles(location: StorageLocation, showHidden: Boolean): List<FileItem> = emptyList()
    override suspend fun createDirectory(location: StorageLocation, name: String): FileItem = throw UnsupportedOperationException()
    override suspend fun delete(location: StorageLocation) {}
    override suspend fun rename(location: StorageLocation, newName: String): FileItem = throw UnsupportedOperationException()
    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = emptyFlow()
    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = emptyFlow()
    override suspend fun getFileItem(location: StorageLocation): FileItem? = null
    override suspend fun exists(location: StorageLocation): Boolean = false
}

class TestRootFileSystem : RootFileSystemContract {
    override fun isAvailable(): Boolean = false
    override suspend fun listFiles(location: StorageLocation, showHidden: Boolean): List<FileItem> = emptyList()
    override suspend fun createDirectory(location: StorageLocation, name: String): FileItem = throw UnsupportedOperationException()
    override suspend fun delete(location: StorageLocation) {}
    override suspend fun rename(location: StorageLocation, newName: String): FileItem = throw UnsupportedOperationException()
    override fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = emptyFlow()
    override fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress> = emptyFlow()
    override suspend fun getFileItem(location: StorageLocation): FileItem? = null
    override suspend fun exists(location: StorageLocation): Boolean = false
}
