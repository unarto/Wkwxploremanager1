// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/repository/DirectoryRepositoryImplTest.kt
// [Penjelasan]: Unit test untuk DirectoryRepositoryImpl dengan LocalFileSystem.
package com.wakwau.xplore.core.storage.repository

import com.wakwau.xplore.core.storage.error.StorageErrorMapper
import com.wakwau.xplore.core.storage.filesystem.local.LocalFileSystem
import com.wakwau.xplore.core.storage.mapper.FileItemMapper
import com.wakwau.xplore.core.storage.metadata.FileMetadataReader
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationError
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.wakwau.xplore.core.storage.filesystem.StorageBackendClassifier
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryRepositoryImplTest {

    companion object {
        private const val TEST_DIR_PREFIX = "dir_repo_test"
        private const val TEST_FILE_NAME = "file.txt"
        private const val TEST_NONEXISTENT_DIR = "none"
        private const val TEST_NEW_DIR_NAME = "newdir"
        private const val TEST_DUP_DIR_NAME = "dupdir"
    }

    private lateinit var repository: DirectoryRepositoryImpl
    private lateinit var tempDir: File
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        val metadataReader = FileMetadataReader()
        val itemMapper = FileItemMapper()
        val errorMapper = StorageErrorMapper()
        val localFileSystem = LocalFileSystem(
            fileMetadataReader = metadataReader,
            fileItemMapper = itemMapper
        )
        
        repository = DirectoryRepositoryImpl(
            localFileSystem = localFileSystem,
            safFileSystem = com.wakwau.xplore.core.storage.testutil.TestSafFileSystem(),
            safShizukuFileSystem = com.wakwau.xplore.core.storage.testutil.TestShizukuFileSystem(),
            rootFileSystem = com.wakwau.xplore.core.storage.testutil.TestRootFileSystem(),
            backendClassifier = StorageBackendClassifier(isSuAvailable = { false }, isShizukuAvailable = { false }, isSafPersisted = { false }),
            storageErrorMapper = errorMapper,
            ioDispatcher = dispatcher
        )
        
        tempDir = Files.createTempDirectory(TEST_DIR_PREFIX).toFile()
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun list_existingDirectory_returnsSuccess() = runTest {
        val file = File(tempDir, TEST_FILE_NAME)
        file.createNewFile()
        
        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/repository/DirectoryRepositoryImplTest.kt
        // [Penjelasan]: Menyesuaikan pemanggilan repository.list dengan menyertakan parameter showHidden sesuai kontrak DirectoryRepository terkini.
        val result = repository.list(StorageLocation(tempDir.absolutePath), showHidden = false)
        
        assertTrue(result is FileOperationResult.Success)
        val data = (result as FileOperationResult.Success).data
        assertEquals(1, data.size)
        assertEquals(TEST_FILE_NAME, data[0].name)
    }

    @Test
    fun list_nonexistentDirectory_returnsFailure() = runTest {
        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/repository/DirectoryRepositoryImplTest.kt
        // [Penjelasan]: Menyesuaikan pemanggilan repository.list dengan menyertakan parameter showHidden sesuai kontrak DirectoryRepository terkini.
        val result = repository.list(StorageLocation("${tempDir.absolutePath}/$TEST_NONEXISTENT_DIR"), showHidden = false)
        
        assertTrue(result is FileOperationResult.Failure)
        assertEquals(FileOperationError.NOT_FOUND, (result as FileOperationResult.Failure).error)
    }

    @Test
    fun create_inExistingParent_returnsSuccess() = runTest {
        val result = repository.create(StorageLocation(tempDir.absolutePath), TEST_NEW_DIR_NAME)
        
        assertTrue(result is FileOperationResult.Success)
        val dir = File(tempDir, TEST_NEW_DIR_NAME)
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun create_duplicateDirectory_returnsFailure() = runTest {
        val dir = File(tempDir, TEST_DUP_DIR_NAME)
        dir.mkdir()
        
        val result = repository.create(StorageLocation(tempDir.absolutePath), TEST_DUP_DIR_NAME)
        
        assertTrue(result is FileOperationResult.Failure)
    }
}

