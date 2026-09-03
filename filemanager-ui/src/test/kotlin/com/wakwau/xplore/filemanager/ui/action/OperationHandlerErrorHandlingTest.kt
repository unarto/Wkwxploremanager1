// [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/action/OperationHandlerErrorHandlingTest.kt
// [Penjelasan]: Unit test untuk memverifikasi pemisahan CancellationException (OperationCancelled) dan pemetaan exception operasi I/O nyata ke OperationFailed pada CopyOperationHandler, MoveOperationHandler, dan DeleteOperationHandler.
package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.error.StorageErrorMapper
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.BackgroundOperationManager
import com.wakwau.xplore.core.storage.operation.BackgroundOperationType
import com.wakwau.xplore.core.storage.operation.FileOperationError
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.state.PanelState
import com.wakwau.xplore.filemanager.usecase.CopyFilesUseCase
import com.wakwau.xplore.filemanager.usecase.DeleteFilesUseCase
import com.wakwau.xplore.filemanager.usecase.MoveFilesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class OperationHandlerErrorHandlingTest {

    private class TestBackgroundOperationManager : BackgroundOperationManager {
        var errorToThrow: Throwable? = null
        val enqueued = mutableListOf<Triple<BackgroundOperationType, List<StorageLocation>, StorageLocation?>>()

        override fun enqueueOperation(
            type: BackgroundOperationType,
            sources: List<StorageLocation>,
            destination: StorageLocation?
        ) {
            errorToThrow?.let { throw it }
            enqueued.add(Triple(type, sources, destination))
        }

        override fun enqueueResolvedOperation(
            type: BackgroundOperationType,
            resolvedItems: List<com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem>
        ) {
            errorToThrow?.let { throw it }
        }

        override fun cancelOperation() {
            errorToThrow?.let { throw it }
        }

        override fun observeProgress(): Flow<FileOperationResult<FileOperationProgress>> = emptyFlow()
    }

    private lateinit var bgManager: TestBackgroundOperationManager
    private lateinit var copyUseCase: CopyFilesUseCase
    private lateinit var moveUseCase: MoveFilesUseCase
    private lateinit var deleteUseCase: DeleteFilesUseCase
    private lateinit var events: MutableList<DualPaneEvent>
    private lateinit var testState: DualPaneState

    private val testFileItem = FileItem(
        id = "1",
        name = "test.txt",
        location = StorageLocation(path = "/storage/emulated/0/test.txt", rootId = "primary_internal"),
        type = com.wakwau.xplore.core.storage.model.FileType.FILE,
        metadata = com.wakwau.xplore.core.storage.model.FileMetadata(
            size = 100L,
            modifiedTime = 0L,
            createdTime = null,
            isReadable = true,
            isWritable = true,
            isExecutable = false,
            isHidden = false
        )
    )

    private lateinit var detectConflictsUseCase: com.wakwau.xplore.filemanager.usecase.DetectConflictsUseCase
    private lateinit var resolveTransferUseCase: com.wakwau.xplore.filemanager.usecase.ResolveTransferUseCase

    @Before
    fun setUp() {
        bgManager = TestBackgroundOperationManager()
        val mockDetector = object : com.wakwau.xplore.core.storage.conflict.ConflictDetector {
            override suspend fun detectConflicts(sources: List<StorageLocation>, destinationDir: StorageLocation): List<com.wakwau.xplore.core.storage.conflict.FileConflict> = emptyList()
            override suspend fun getExistingNames(destinationDir: StorageLocation): Set<String> = emptySet()
        }
        val mockResolver = object : com.wakwau.xplore.core.storage.conflict.ConflictResolver {
            override fun resolveConflict(
                conflict: com.wakwau.xplore.core.storage.conflict.FileConflict,
                choice: com.wakwau.xplore.core.storage.conflict.ConflictChoice,
                existingNames: MutableSet<String>
            ): com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem? = null

            override fun generateUniqueName(
                originalName: String,
                isDirectory: Boolean,
                existingNames: Set<String>
            ): String = originalName

            override fun resolveNonConflictingItem(
                source: StorageLocation,
                destinationDir: StorageLocation,
                isDirectory: Boolean,
                existingNames: MutableSet<String>
            ): com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem {
                val name = source.path.trimEnd('/').substringAfterLast('/')
                val targetLoc = StorageLocation(destinationDir.path.trimEnd('/') + "/" + name, destinationDir.rootId)
                return com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem(
                    source = source,
                    destinationDir = destinationDir,
                    targetLocation = targetLoc,
                    originalName = name,
                    targetName = name,
                    isDirectory = isDirectory,
                    choice = com.wakwau.xplore.core.storage.conflict.ConflictChoice.RENAME
                )
            }
        }
        detectConflictsUseCase = com.wakwau.xplore.filemanager.usecase.DetectConflictsUseCase(mockDetector)
        resolveTransferUseCase = com.wakwau.xplore.filemanager.usecase.ResolveTransferUseCase(mockDetector, mockResolver)

        copyUseCase = CopyFilesUseCase(bgManager)
        moveUseCase = MoveFilesUseCase(bgManager)
        deleteUseCase = DeleteFilesUseCase(bgManager)
        events = mutableListOf()

        testState = DualPaneState(
            activePanelId = PanelId.LEFT,
            leftPanel = PanelState(
                id = PanelId.LEFT,
                currentLocation = StorageLocation(path = "/storage/emulated/0", rootId = "primary_internal")
            ),
            rightPanel = PanelState(
                id = PanelId.RIGHT,
                currentLocation = StorageLocation(path = "/storage/emulated/0/target", rootId = "primary_internal")
            )
        )
    }

    // --- CopyOperationHandler Tests ---

    @Test
    fun copyHandler_cancellationException_dispatchesOperationCancelled() = runTest {
        bgManager.errorToThrow = CancellationException("Copy user cancelled")
        val handler = CopyOperationHandler(copyUseCase, detectConflictsUseCase, resolveTransferUseCase, StorageErrorMapper()) { events.add(it) }

        try {
            handler.execute(testState, listOf(testFileItem), "/storage/emulated/0/target")
        } catch (e: CancellationException) {}

        assertTrue(events.any { it is DualPaneEvent.OperationCancelled })
        assertTrue(events.none { it is DualPaneEvent.OperationFailed })
    }

    @Test
    fun copyHandler_ioException_dispatchesOperationFailedWithIoError() = runTest {
        bgManager.errorToThrow = IOException("ENOSPC - no space left on device")
        val handler = CopyOperationHandler(copyUseCase, detectConflictsUseCase, resolveTransferUseCase, StorageErrorMapper()) { events.add(it) }

        handler.execute(testState, listOf(testFileItem), "/storage/emulated/0/target")

        val failedEvent = events.filterIsInstance<DualPaneEvent.OperationFailed>().firstOrNull()
        assertEquals(FileOperationError.IO_ERROR.name, failedEvent?.error)
        assertTrue(events.none { it is DualPaneEvent.OperationCancelled })
    }

    @Test
    fun copyHandler_securityException_dispatchesOperationFailedWithAccessDenied() = runTest {
        bgManager.errorToThrow = SecurityException("Permission denied")
        val handler = CopyOperationHandler(copyUseCase, detectConflictsUseCase, resolveTransferUseCase, StorageErrorMapper()) { events.add(it) }

        handler.execute(testState, listOf(testFileItem), "/storage/emulated/0/target")

        val failedEvent = events.filterIsInstance<DualPaneEvent.OperationFailed>().firstOrNull()
        assertEquals(FileOperationError.ACCESS_DENIED.name, failedEvent?.error)
        assertTrue(events.none { it is DualPaneEvent.OperationCancelled })
    }

    // --- MoveOperationHandler Tests ---

    @Test
    fun moveHandler_cancellationException_dispatchesOperationCancelled() = runTest {
        bgManager.errorToThrow = CancellationException("Move job cancelled")
        val handler = MoveOperationHandler(moveUseCase, detectConflictsUseCase, resolveTransferUseCase, StorageErrorMapper()) { events.add(it) }

        try {
            handler.execute(testState, listOf(testFileItem), "/storage/emulated/0/target")
        } catch (e: CancellationException) {}

        assertTrue(events.any { it is DualPaneEvent.OperationCancelled })
        assertTrue(events.none { it is DualPaneEvent.OperationFailed })
    }

    @Test
    fun moveHandler_fileNotFoundException_dispatchesOperationFailedWithNotFound() = runTest {
        bgManager.errorToThrow = FileNotFoundException("Source file disappeared")
        val handler = MoveOperationHandler(moveUseCase, detectConflictsUseCase, resolveTransferUseCase, StorageErrorMapper()) { events.add(it) }

        handler.execute(testState, listOf(testFileItem), "/storage/emulated/0/target")

        val failedEvent = events.filterIsInstance<DualPaneEvent.OperationFailed>().firstOrNull()
        assertEquals(FileOperationError.NOT_FOUND.name, failedEvent?.error)
        assertTrue(events.none { it is DualPaneEvent.OperationCancelled })
    }

    // --- DeleteOperationHandler Tests ---

    @Test
    fun deleteHandler_cancellationException_dispatchesOperationCancelled() = runTest {
        bgManager.errorToThrow = CancellationException("Delete job cancelled")
        val handler = DeleteOperationHandler(deleteUseCase, StorageErrorMapper()) { events.add(it) }

        try {
            handler.execute(testState, listOf(testFileItem))
        } catch (e: CancellationException) {}

        assertTrue(events.any { it is DualPaneEvent.OperationCancelled })
        assertTrue(events.none { it is DualPaneEvent.OperationFailed })
    }

    @Test
    fun deleteHandler_accessDeniedException_dispatchesOperationFailedWithAccessDenied() = runTest {
        bgManager.errorToThrow = IOException("EACCES - permission denied")
        val handler = DeleteOperationHandler(deleteUseCase, StorageErrorMapper()) { events.add(it) }

        handler.execute(testState, listOf(testFileItem))

        val failedEvent = events.filterIsInstance<DualPaneEvent.OperationFailed>().firstOrNull()
        assertEquals(FileOperationError.ACCESS_DENIED.name, failedEvent?.error)
        assertTrue(events.none { it is DualPaneEvent.OperationCancelled })
    }
}
