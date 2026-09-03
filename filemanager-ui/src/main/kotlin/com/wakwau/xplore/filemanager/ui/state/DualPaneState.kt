package com.wakwau.xplore.filemanager.ui.state

import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.permission.StoragePermissionType

// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/state/DualPaneState.kt
// [Penjelasan]: State immutable panel ganda dengan integrasi state rincian metadata berkas, dialog input berkas, perizinan penyimpanan, dan storage volumes.
data class DualPaneState(
    val leftPanel: PanelState = PanelState(id = PanelId.LEFT),
    val rightPanel: PanelState = PanelState(id = PanelId.RIGHT),
    val activePanelId: PanelId = PanelId.LEFT,
    val operationState: OperationUiState = OperationUiState.Idle,
    val fileDetailState: FileDetailState = FileDetailState(),
    val searchUiState: SearchUiState = SearchUiState(),
    val dialogState: FileDialogUiState = FileDialogUiState.None,
    val hasPermission: Boolean = false,
    val requiredPermissionType: StoragePermissionType = StoragePermissionType.READ_WRITE_STORAGE,
    val storageVolumes: List<StorageVolumeItem> = emptyList(),
    val isVolumesLoading: Boolean = false
) {
    val activePanel: PanelState
        get() = if (activePanelId == PanelId.LEFT) leftPanel else rightPanel

    val inactivePanel: PanelState
        get() = if (activePanelId == PanelId.LEFT) rightPanel else leftPanel
}
