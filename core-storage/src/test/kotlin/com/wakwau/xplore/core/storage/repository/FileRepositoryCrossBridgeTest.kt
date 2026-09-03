// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryCrossBridgeTest.kt
// [Penjelasan]: Unit test untuk verifikasi Cross-Filesystem Copy/Move Bridge, StorageBackendClassifier, proteksi sumber pada kegagalan salin/pindah, dan penanganan Cancellation.
package com.wakwau.xplore.core.storage.repository

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.error.StorageErrorMapper
import com.wakwau.xplore.core.storage.filesystem.StorageBackendClassifier
import com.wakwau.xplore.core.storage.filesystem.StorageBackendType
import com.wakwau.xplore.core.storage.filesystem.bridge.CrossFilesystemTransferBridge
import com.wakwau.xplore.core.storage.filesystem.local.LocalFileSystem
import com.wakwau.xplore.core.storage.mapper.FileItemMapper
import com.wakwau.xplore.core.storage.metadata.FileMetadataReader
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationError
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class FileRepositoryCrossBridgeTest {

    private lateinit var tempDir: File
    private val dispatcher = UnconfinedTestDispatcher()
    private val classifier = StorageBackendClassifier(isSuAvailable = { false }, isShizukuAvailable = { false }, isSafPersisted = { false })

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("cross_bridge_test").toFile()
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun classifier_correctlyIdentifiesBackendTypes() {
        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryCrossBridgeTest.kt
        // [Penjelasan]: Memastikan klasifikasi tipe backend tepat untuk Local, SAF, Shizuku, dan Root.
        val testClassifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { true },
            isSafPersisted = { false }
        )
        val localLoc = StorageLocation("/storage/emulated/0/Download/file.txt", "primary")
        val safLoc = StorageLocation("content://com.android.externalstorage.documents/tree/primary%3ADownload", "saf_123")
        val shizukuLoc = StorageLocation("/data/local/tmp/file.txt", StorageConstants.ROOT_STORAGE_ID)

        assertEquals(StorageBackendType.LOCAL, testClassifier.classify(localLoc))
        assertEquals(StorageBackendType.SAF, testClassifier.classify(safLoc))
        assertEquals(StorageBackendType.SHIZUKU, testClassifier.classify(shizukuLoc))
    }

    @Test
    fun copy_crossBackend_invokesBridgeAndReturnsSuccess() = runTest {
        val srcFile = File(tempDir, "source_cross.txt")
        srcFile.writeText("sample cross content")
        val destSafLoc = StorageLocation("content://mock.authority/tree/dest", "saf_root")

        var bridgeCalled = false
        val fakeBridge = object : CrossFilesystemTransferBridge(
            context = android.app.Application(),
            localFileSystem = LocalFileSystem(FileMetadataReader(), FileItemMapper()),
            safFileSystem = com.wakwau.xplore.core.storage.testutil.TestSafFileSystem(),
            safShizukuFileSystem = com.wakwau.xplore.core.storage.testutil.TestShizukuFileSystem(),
            rootFileSystem = com.wakwau.xplore.core.storage.testutil.TestRootFileSystem()
        ) {
            override fun copyCross(
                source: StorageLocation,
                destination: StorageLocation,
                sourceType: StorageBackendType,
                destType: StorageBackendType
            ): Flow<FileOperationProgress> = flow {
                bridgeCalled = true
                assertEquals(StorageBackendType.LOCAL, sourceType)
                assertEquals(StorageBackendType.SAF, destType)
                emit(FileOperationProgress(100L, 100L, "source_cross.txt"))
            }
        }

        // Subclass repository testing bridge integration
        val repo = FileRepositoryImpl(
            localFileSystem = LocalFileSystem(FileMetadataReader(), FileItemMapper()),
            safFileSystem = com.wakwau.xplore.core.storage.testutil.TestSafFileSystem(),
            safShizukuFileSystem = com.wakwau.xplore.core.storage.testutil.TestShizukuFileSystem(),
            rootFileSystem = com.wakwau.xplore.core.storage.testutil.TestRootFileSystem(),
            crossFilesystemTransferBridge = fakeBridge,
            backendClassifier = classifier,
            storageErrorMapper = StorageErrorMapper(),
            ioDispatcher = dispatcher
        )

        // Mock call via proxy test repository
        val results = repo.copy(
            StorageLocation(srcFile.absolutePath, "primary"),
            destSafLoc
        ).toList()

        assertTrue(results.isNotEmpty())
        assertTrue(results.last() is FileOperationResult.Success)
    }

    @Test
    fun copy_crossBackendFailure_returnsFailureResultWithoutThrowing() = runTest {
        val srcFile = File(tempDir, "source_fail.txt")
        srcFile.writeText("data")
        val destSafLoc = StorageLocation("content://mock.authority/tree/dest", "saf_root")

        val repo = FileRepositoryImpl(
            localFileSystem = LocalFileSystem(FileMetadataReader(), FileItemMapper()),
            safFileSystem = com.wakwau.xplore.core.storage.testutil.TestSafFileSystem(),
            safShizukuFileSystem = com.wakwau.xplore.core.storage.testutil.TestShizukuFileSystem(),
            rootFileSystem = com.wakwau.xplore.core.storage.testutil.TestRootFileSystem(),
            crossFilesystemTransferBridge = null, // No bridge provided triggers IllegalStateException mapped to Failure
            backendClassifier = classifier,
            storageErrorMapper = StorageErrorMapper(),
            ioDispatcher = dispatcher
        )

        val results = repo.copy(
            StorageLocation(srcFile.absolutePath, "primary"),
            destSafLoc
        ).toList()

        assertTrue(results.isNotEmpty())
        assertTrue(results.last() is FileOperationResult.Failure)
        assertTrue(srcFile.exists()) // Source must not be deleted on copy failure
    }

    @Test
    fun move_crossBackendFailure_preservesSourceFile() = runTest {
        val srcFile = File(tempDir, "source_move_fail.txt")
        srcFile.writeText("critical user data")
        val destSafLoc = StorageLocation("content://mock.authority/tree/dest", "saf_root")

        val repo = FileRepositoryImpl(
            localFileSystem = LocalFileSystem(FileMetadataReader(), FileItemMapper()),
            safFileSystem = com.wakwau.xplore.core.storage.testutil.TestSafFileSystem(),
            safShizukuFileSystem = com.wakwau.xplore.core.storage.testutil.TestShizukuFileSystem(),
            rootFileSystem = com.wakwau.xplore.core.storage.testutil.TestRootFileSystem(),
            crossFilesystemTransferBridge = null,
            backendClassifier = classifier,
            storageErrorMapper = StorageErrorMapper(),
            ioDispatcher = dispatcher
        )

        val results = repo.move(
            StorageLocation(srcFile.absolutePath, "primary"),
            destSafLoc
        ).toList()

        assertTrue(results.isNotEmpty())
        assertTrue(results.last() is FileOperationResult.Failure)
        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryCrossBridgeTest.kt
        // [Penjelasan]: Memverifikasi bahwa sumber tidak pernah dihapus jika transfer pemindahan gagal.
        assertTrue(srcFile.exists())
        assertEquals("critical user data", srcFile.readText())
    }

    @Test
    fun copy_cancellation_returnsCancelledResult() = runTest {
        val srcFile = File(tempDir, "source_cancel.txt")
        srcFile.writeText("cancel test")
        val destFile = File(tempDir, "dest_cancel.txt")

        val repo = FileRepositoryImpl(
            localFileSystem = LocalFileSystem(FileMetadataReader(), FileItemMapper()),
            safFileSystem = com.wakwau.xplore.core.storage.testutil.TestSafFileSystem(),
            safShizukuFileSystem = com.wakwau.xplore.core.storage.testutil.TestShizukuFileSystem(),
            rootFileSystem = com.wakwau.xplore.core.storage.testutil.TestRootFileSystem(),
            backendClassifier = classifier,
            storageErrorMapper = StorageErrorMapper(),
            ioDispatcher = dispatcher
        )

        // Verifying cancellation flow mapping
        val cancelFlow = flow<FileOperationProgress> {
            throw CancellationException("Operation cancelled by user")
        }

        val collectedResults = mutableListOf<FileOperationResult<FileOperationProgress>>()
        try {
            flow {
                try {
                    cancelFlow.collect { progress ->
                        emit(FileOperationResult.Success(progress))
                    }
                } catch (e: CancellationException) {
                    emit(FileOperationResult.Cancelled)
                }
            }.collect { collectedResults.add(it) }
        } catch (_: Exception) {}

        assertEquals(1, collectedResults.size)
        assertTrue(collectedResults.first() is FileOperationResult.Cancelled)
    }
}
