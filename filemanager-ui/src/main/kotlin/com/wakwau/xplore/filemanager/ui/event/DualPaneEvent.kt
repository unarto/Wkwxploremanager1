package com.wakwau.xplore.filemanager.ui.event

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.filemanager.ui.state.PanelId

// [Jalur Class]: com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
// [Penjelasan]: Menambahkan event ShowCreateDirectoryDialog, ShowRenameDialog, ShowDeleteConfirmationDialog, dan DismissInputDialog untuk intent dialog UI.
sealed class DualPaneEvent {
    // Navigation
    data class OpenLocation(val panelId: PanelId, val location: StorageLocation) : DualPaneEvent()
    data class NavigateUp(val panelId: PanelId) : DualPaneEvent()
    
    // Panel Focus
    data class SetActivePanel(val panelId: PanelId) : DualPaneEvent()
    
    // Selection
    data class ToggleSelection(val panelId: PanelId, val itemId: String) : DualPaneEvent()
    data class SetSelectedItems(val panelId: PanelId, val itemIds: Set<String>) : DualPaneEvent()
    data class ClearSelection(val panelId: PanelId) : DualPaneEvent()
    
    // Sorting & Preferences
    
    data class UpdateSortPreferences(
        val fileSortOrder: com.wakwau.xplore.core.storage.preferences.FileSortOrder,
        val fileSortDirection: com.wakwau.xplore.core.storage.preferences.FileSortDirection
    ) : DualPaneEvent()
    
    object ToggleShowHiddenFiles : DualPaneEvent()
    
    // Refresh
    data class Refresh(val panelId: PanelId) : DualPaneEvent()

    // Data Loading Result (From UseCase back to UI state)
    data class DirectoryLoaded(val panelId: PanelId, val location: StorageLocation, val items: List<FileItem>) : DualPaneEvent()
    data class DirectoryLoadFailed(val panelId: PanelId, val error: String) : DualPaneEvent()
    data class LoadingStarted(val panelId: PanelId) : DualPaneEvent()
    
    // Dialog triggers
    data class ShowCreateDirectoryDialog(val parentLocation: StorageLocation) : DualPaneEvent()
    data class ShowRenameDialog(val item: FileItem) : DualPaneEvent()
    data class ShowDeleteConfirmationDialog(val items: List<FileItem>) : DualPaneEvent()
    object DismissInputDialog : DualPaneEvent()

    // Operation intents
    data class ExecuteConfirmedCopy(val items: List<FileItem>, val targetPath: String) : DualPaneEvent()
    data class ExecuteConfirmedMove(val items: List<FileItem>, val targetPath: String) : DualPaneEvent()
    data class DeleteSelected(val items: List<FileItem>) : DualPaneEvent()
    data class RenameItem(val item: FileItem, val newName: String) : DualPaneEvent()
    data class CreateDirectory(val parentLocation: StorageLocation, val name: String) : DualPaneEvent()
    
    // Operation Results
    data class OperationStarted(val operationNameRes: Int) : DualPaneEvent()
    data class ShowOperationConfirmation(
        val isMove: Boolean,
        val items: List<FileItem>,
        val targetPath: String
    ) : DualPaneEvent()
    data class ShowConflictResolution(
        val isMove: Boolean,
        val conflicts: List<com.wakwau.xplore.core.storage.conflict.FileConflict>,
        val destinationDir: StorageLocation,
        val allSources: List<StorageLocation>
    ) : DualPaneEvent()
    data class ResolveConflictDecision(
        val choice: com.wakwau.xplore.core.storage.conflict.ConflictChoice,
        val applyToAll: Boolean
    ) : DualPaneEvent()
    object CancelOperationRequested : DualPaneEvent()
    data class OperationProgress(val progress: com.wakwau.xplore.core.storage.operation.FileOperationProgress) : DualPaneEvent()
    data class OperationSuccess(val messageRes: Int) : DualPaneEvent()
    data class OperationFailed(val error: String) : DualPaneEvent()
    object OperationCancelled : DualPaneEvent()
    object ClearOperationState : DualPaneEvent()

    // File Details & Checksum
    data class ShowFileDetails(val item: FileItem) : DualPaneEvent()
    object DismissFileDetails : DualPaneEvent()
    data class CalculateChecksum(val item: FileItem) : DualPaneEvent()
    object FileDetailsLoadingStarted : DualPaneEvent()
    data class FileDetailsLoaded(val metadata: com.wakwau.xplore.core.storage.model.FileDetailedMetadata) : DualPaneEvent()
    data class FileDetailsFailed(val errorRes: Int, val errorMessage: String? = null) : DualPaneEvent()
    object ChecksumCalculationStarted : DualPaneEvent()
    data class ChecksumCalculated(val checksum: com.wakwau.xplore.core.storage.model.FileChecksum) : DualPaneEvent()
    data class ChecksumCalculationFailed(val errorRes: Int, val errorMessage: String? = null) : DualPaneEvent()
    
    // Search
    object SearchIconClicked : DualPaneEvent()
    object DismissSearchDialog : DualPaneEvent()
    data class ExecuteSearch(val query: com.wakwau.xplore.core.storage.search.FileSearchQuery) : DualPaneEvent()
    data class SearchStarted(
        val keyword: String,
        val searchType: com.wakwau.xplore.core.storage.search.SearchTargetType = com.wakwau.xplore.core.storage.search.SearchTargetType.ALL,
        val searchInArchives: Boolean = false
    ) : DualPaneEvent()
    data class SearchResultsUpdated(val keyword: String, val results: List<FileItem>) : DualPaneEvent()
    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/event/DualPaneEvent.kt
    // [Penjelasan]: Event untuk memperbarui daftar riwayat pencarian dan intent untuk menghapus seluruh riwayat.
    data class SearchHistoryUpdated(val history: List<String>) : DualPaneEvent()
    data class ClearSearchHistory(val dummy: Boolean = true) : DualPaneEvent()
    object SearchCompleted : DualPaneEvent()
    data class SearchFailed(val error: String) : DualPaneEvent()
    object SearchCancelled : DualPaneEvent()

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/event/DualPaneEvent.kt
    // [Penjelasan]: Event untuk memperbarui status perizinan penyimpanan dan daftar volume penyimpanan nyata.
    data class PermissionStatusUpdated(
        val hasPermission: Boolean,
        val requiredPermissionType: com.wakwau.xplore.core.storage.permission.StoragePermissionType
    ) : DualPaneEvent()
    data class StorageVolumesUpdated(
        val volumes: List<com.wakwau.xplore.core.storage.model.StorageVolumeItem>
    ) : DualPaneEvent()
    data class StorageVolumesLoading(val isLoading: Boolean) : DualPaneEvent()
}
