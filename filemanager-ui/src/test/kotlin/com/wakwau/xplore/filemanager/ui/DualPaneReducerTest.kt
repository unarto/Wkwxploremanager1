// [Jalur Class/Modul]: com.wakwau.xplore.filemanager.ui.DualPaneReducerTest
// [Penjelasan]: Pengujian unit untuk DualPaneReducer mencakup peralihan panel, seleksi berkas/folder independen, navigasi, dan transisi status pencarian berkas.
package com.wakwau.xplore.filemanager.ui

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.reducer.DualPaneReducer
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.ui.state.OperationUiState
import com.wakwau.xplore.filemanager.ui.state.PanelId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DualPaneReducerTest {

    private val reducer = DualPaneReducer()

    @Test
    fun `test active panel switching`() {
        val initial = DualPaneState(activePanelId = PanelId.LEFT)
        val switched = reducer.reduce(initial, DualPaneEvent.SetActivePanel(PanelId.RIGHT))
        assertEquals(PanelId.RIGHT, switched.activePanelId)
    }

    @Test
    fun `test selection LEFT does not affect RIGHT`() {
        val initial = DualPaneState()
        val updated = reducer.reduce(initial, DualPaneEvent.ToggleSelection(PanelId.LEFT, "file1"))
        
        assertTrue(updated.leftPanel.selectedItemIds.contains("file1"))
        assertTrue(updated.rightPanel.selectedItemIds.isEmpty())
    }

    @Test
    fun `test selection RIGHT does not affect LEFT`() {
        val initial = DualPaneState()
        val updated = reducer.reduce(initial, DualPaneEvent.ToggleSelection(PanelId.RIGHT, "file2"))
        
        assertTrue(updated.rightPanel.selectedItemIds.contains("file2"))
        assertTrue(updated.leftPanel.selectedItemIds.isEmpty())
    }

    @Test
    fun `test navigation LEFT does not affect RIGHT location`() {
        val initial = DualPaneState()
        val leftLoc = StorageLocation("/left/dir", "root1")
        val updated = reducer.reduce(initial, DualPaneEvent.OpenLocation(PanelId.LEFT, leftLoc))
        
        assertEquals(leftLoc, updated.leftPanel.currentLocation)
        assertEquals(null, updated.rightPanel.currentLocation)
    }

    @Test
    fun `test directory loaded updates items and resets loading`() {
        val initial = DualPaneState()
        val loc = StorageLocation("/path", "root")
        val items = listOf(
            FileItem(
                id = "/path/test.txt",
                name = "test.txt",
                location = StorageLocation("/path/test.txt", "root"),
                type = FileType.FILE,
                metadata = FileMetadata(
                    size = 0L,
                    modifiedTime = 0L,
                    createdTime = null,
                    isReadable = true,
                    isWritable = true,
                    isExecutable = false,
                    isHidden = false
                )
            )
        )
        val updated = reducer.reduce(initial, DualPaneEvent.DirectoryLoaded(PanelId.LEFT, loc, items))
        
        assertEquals(loc, updated.leftPanel.currentLocation)
        assertEquals(1, updated.leftPanel.items.size)
        assertFalse(updated.leftPanel.isLoading)
    }

    @Test
    fun `test operation cancelled updates operation state to Cancelled`() {
        val initial = DualPaneState(operationState = OperationUiState.Running(R.string.op_copy_started))
        val updated = reducer.reduce(initial, DualPaneEvent.OperationCancelled)
        
        assertTrue(updated.operationState is OperationUiState.Cancelled)
    }

    @Test
    fun `test operation failure does not corrupt panel items or location`() {
        val loc = StorageLocation("/test", "root")
        val initial = DualPaneState(
            operationState = OperationUiState.Running(R.string.op_delete_started)
        )
        val withLoc = reducer.reduce(initial, DualPaneEvent.OpenLocation(PanelId.LEFT, loc))
        val failed = reducer.reduce(withLoc, DualPaneEvent.OperationFailed("IO_ERROR"))
        
        assertEquals(loc, failed.leftPanel.currentLocation)
        assertTrue(failed.operationState is OperationUiState.Failure)
        assertEquals("IO_ERROR", (failed.operationState as OperationUiState.Failure).errorMessage)
    }

    @Test
    fun `test set selected items updates panel selection immutably`() {
        val initial = DualPaneState()
        val items = setOf("/path/folder", "/path/folder/file1.txt", "/path/folder/file2.txt")
        val updated = reducer.reduce(initial, DualPaneEvent.SetSelectedItems(PanelId.LEFT, items))

        assertEquals(items, updated.leftPanel.selectedItemIds)
        assertEquals(3, updated.leftPanel.selectedItemIds.size)
        assertTrue(updated.rightPanel.selectedItemIds.isEmpty())
    }

    @Test
    fun `test search events update searchUiState and preserve search results`() {
        val initial = DualPaneState()

        // 1. Open search dialog
        val openState = reducer.reduce(initial, DualPaneEvent.SearchIconClicked)
        assertTrue(openState.searchUiState.isSearchDialogOpen)
        assertNull(openState.searchUiState.searchError)

        // 2. Search started
        val startedState = reducer.reduce(openState, DualPaneEvent.SearchStarted("found"))
        assertTrue(startedState.searchUiState.isSearching)
        assertTrue(startedState.searchUiState.hasSearched)
        assertTrue(startedState.searchUiState.results.isEmpty())

        // 3. Search results updated
        val testResults = listOf(
            FileItem(
                id = "/path/found.txt",
                name = "found.txt",
                location = StorageLocation("/path/found.txt", "root"),
                type = FileType.FILE,
                metadata = FileMetadata(
                    size = 120L,
                    modifiedTime = 1000L,
                    createdTime = null,
                    isReadable = true,
                    isWritable = true,
                    isExecutable = false,
                    isHidden = false
                )
            )
        )
        val resultsState = reducer.reduce(startedState, DualPaneEvent.SearchResultsUpdated("found", testResults))
        assertTrue(resultsState.searchUiState.isSearching)
        assertEquals(1, resultsState.searchUiState.results.size)
        assertEquals("found.txt", resultsState.searchUiState.results[0].name)
        assertTrue(resultsState.searchUiState.hasSearched)

        val completedState = reducer.reduce(resultsState, DualPaneEvent.SearchCompleted)
        assertFalse(completedState.searchUiState.isSearching)

        // 4. Dismiss search dialog clears state
        val dismissedState = reducer.reduce(completedState, DualPaneEvent.DismissSearchDialog)
        assertFalse(dismissedState.searchUiState.isSearchDialogOpen)
        assertTrue(dismissedState.searchUiState.results.isEmpty())
        assertFalse(dismissedState.searchUiState.hasSearched)
    }
}
