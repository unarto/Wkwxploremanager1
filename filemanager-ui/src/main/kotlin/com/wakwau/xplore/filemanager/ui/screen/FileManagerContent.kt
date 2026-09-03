// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/FileManagerContent.kt
// [Penjelasan]: Menghubungkan trigger UI SideActionBar dan item long click dengan dialog CreateDirectoryDialog, RenameDialog, DeleteConfirmationDialog, serta FileDetailDialog dan FileSearchDialog secara murni MVI.
package com.wakwau.xplore.filemanager.ui.screen

import android.net.Uri
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.ui.components.dialog.CreateDirectoryDialog
import com.wakwau.xplore.core.ui.components.dialog.DeleteConfirmationDialog
import com.wakwau.xplore.core.ui.components.dialog.RenameDialog
import com.wakwau.xplore.filemanager.ui.component.ActivePanelContent
import com.wakwau.xplore.filemanager.ui.search.FileSearchDialog
import com.wakwau.xplore.filemanager.ui.detail.FileDetailDialog
import com.wakwau.xplore.filemanager.ui.component.ProgressDialog
import com.wakwau.xplore.filemanager.ui.component.SideAction
import com.wakwau.xplore.filemanager.ui.component.SideActionBar
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.gesture.onPanelSwipe
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.ui.state.DualPanelStateController
import com.wakwau.xplore.filemanager.ui.state.FileDialogUiState
import com.wakwau.xplore.filemanager.ui.state.FileOperationPanelPosition
import com.wakwau.xplore.filemanager.ui.state.OperationUiState
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter

@Composable
fun FileManagerContent(
    state: DualPaneState,
    treeAdapter: TreeNavigationAdapter,
    operationPanelPosition: FileOperationPanelPosition,
    panelStateController: DualPanelStateController,
    onEvent: (DualPaneEvent) -> Unit,
    showHiddenFiles: Boolean = false,
    onSortClick: () -> Unit = {},
    onRemoveLinkClick: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // [Jalur Class]: com.wakwau.xplore.filemanager.ui.screen.FileManagerContent
    // [Penjelasan]: Merender tata letak dua panel dengan meneruskan treeAdapter ke ActivePanelContent serta menangani aksi dan dialog file.
    val isLeftActive = state.activePanelId == PanelId.LEFT
    val activePanel = if (isLeftActive) state.leftPanel else state.rightPanel
    val inactivePanel = if (isLeftActive) state.rightPanel else state.leftPanel
    
    val activeEngine = treeAdapter.getEngine(activePanel.id)
    val visibleNodes by activeEngine.treeState.visibleNodes.collectAsStateWithLifecycle()
    val treeSelectionHandler = remember { com.wakwau.xplore.filemanager.ui.selection.TreeSelectionHandler() }
    val selectedCount = remember(visibleNodes, activePanel.selectedItemIds) {
        visibleNodes.count { 
            treeSelectionHandler.getSelectionState(it.node, activePanel.selectedItemIds) == com.wakwau.xplore.filemanager.ui.selection.FolderCheckCycleState.CHECKED 
        }
    }

    val handleSideAction: (SideAction) -> Unit = { action ->
        val selectedItems = treeAdapter.getSelectedItems(activePanel.id, activePanel.selectedItemIds)
        when (action) {
            SideAction.SWITCH_PANE -> {
                panelStateController.togglePanel()
            }
            SideAction.UP_DIR -> {
                onEvent(DualPaneEvent.NavigateUp(activePanel.id))
            }
            // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/FileManagerContent.kt
            // [Penjelasan]: Handler SideAction.MARK dan SideAction.UNMARK dihapus untuk membersihkan dead code setelah tombol Tandai dan Hapus Tanda dihapus dari SideActionBar.
            SideAction.NEW_FOLDER -> {
                // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/FileManagerContent.kt
                // [Penjelasan]: Menentukan parent StorageLocation untuk folder baru; jika berkas terpilih maka menggunakan direktori induknya, jika folder terpilih menggunakan folder tersebut.
                val targetLocation = if (selectedItems.isNotEmpty()) {
                    val firstItem = selectedItems.first()
                    if (firstItem.type == FileType.DIRECTORY) {
                        firstItem.location
                    } else {
                        val parentPath = java.io.File(firstItem.location.path).parent ?: firstItem.location.path
                        StorageLocation(path = parentPath, rootId = firstItem.location.rootId)
                    }
                } else if (activePanel.currentLocation != null) {
                    val cur = activePanel.currentLocation!!
                    val file = java.io.File(cur.path)
                    val dirPath = if (file.exists() && file.isFile) file.parent ?: cur.path else cur.path
                    StorageLocation(path = dirPath, rootId = cur.rootId)
                } else {
                    val selectedPath = treeAdapter.getSelectedPath(activePanel.id).value
                    if (selectedPath != null) {
                        val file = java.io.File(selectedPath)
                        val dirPath = if (file.exists() && file.isFile) file.parent ?: selectedPath else selectedPath
                        val rootId = treeAdapter.getEngine(activePanel.id).treeState.roots.firstOrNull()?.data?.location?.rootId
                            ?: StorageConstants.PRIMARY_INTERNAL_VOLUME_ID
                        StorageLocation(path = dirPath, rootId = rootId)
                    } else {
                        treeAdapter.getEngine(activePanel.id).treeState.roots.firstOrNull()?.data?.location
                    }
                }
                
                if (targetLocation != null) {
                    onEvent(DualPaneEvent.ShowCreateDirectoryDialog(targetLocation))
                }
            }
            SideAction.RENAME -> {
                if (selectedItems.size == 1) {
                    onEvent(DualPaneEvent.ShowRenameDialog(selectedItems.first()))
                }
            }
            SideAction.COPY -> {
                val targetPath = inactivePanel.currentLocation?.path 
                    ?: treeAdapter.getSelectedPath(inactivePanel.id).value 
                    ?: treeAdapter.getEngine(inactivePanel.id).treeState.roots.firstOrNull()?.data?.location?.path
                    
                if (selectedItems.isNotEmpty() && targetPath != null) {
                    onEvent(DualPaneEvent.ShowOperationConfirmation(isMove = false, items = selectedItems, targetPath = targetPath))
                }
            }
            SideAction.MOVE -> {
                val targetPath = inactivePanel.currentLocation?.path 
                    ?: treeAdapter.getSelectedPath(inactivePanel.id).value 
                    ?: treeAdapter.getEngine(inactivePanel.id).treeState.roots.firstOrNull()?.data?.location?.path

                if (selectedItems.isNotEmpty() && targetPath != null) {
                    onEvent(DualPaneEvent.ShowOperationConfirmation(isMove = true, items = selectedItems, targetPath = targetPath))
                }
            }
            SideAction.DELETE -> {
                if (selectedItems.isNotEmpty()) {
                    onEvent(DualPaneEvent.ShowDeleteConfirmationDialog(selectedItems))
                }
            }
            SideAction.SORT -> {
                onSortClick()
            }
            SideAction.TOGGLE_HIDDEN -> {
                onEvent(DualPaneEvent.ToggleShowHiddenFiles)
            }
            SideAction.SEARCH -> {
                onEvent(DualPaneEvent.SearchIconClicked)
            }
        }
    }

    // Input Dialogs (Create Directory, Rename, Delete Confirmation)
    when (val dialog = state.dialogState) {
        is FileDialogUiState.CreateDirectory -> {
            CreateDirectoryDialog(
                onConfirm = { name ->
                    onEvent(DualPaneEvent.CreateDirectory(dialog.parentLocation, name))
                },
                onDismissRequest = { onEvent(DualPaneEvent.DismissInputDialog) }
            )
        }
        is FileDialogUiState.RenameItem -> {
            RenameDialog(
                initialName = dialog.item.name,
                onConfirm = { newName ->
                    onEvent(DualPaneEvent.RenameItem(dialog.item, newName))
                },
                onDismissRequest = { onEvent(DualPaneEvent.DismissInputDialog) }
            )
        }
        is FileDialogUiState.DeleteConfirmation -> {
            DeleteConfirmationDialog(
                itemCount = dialog.items.size,
                itemName = dialog.items.firstOrNull()?.name,
                onConfirm = {
                    onEvent(DualPaneEvent.DeleteSelected(dialog.items))
                },
                onDismissRequest = { onEvent(DualPaneEvent.DismissInputDialog) }
            )
        }
        FileDialogUiState.None -> Unit
    }

    if (state.operationState is OperationUiState.Confirming) {
        val isMove = state.operationState.isMove
        val opName = if (isMove) androidx.compose.ui.res.stringResource(com.wakwau.xplore.filemanager.ui.R.string.label_move) 
                     else androidx.compose.ui.res.stringResource(com.wakwau.xplore.filemanager.ui.R.string.cd_copy)
        
        com.wakwau.xplore.core.ui.components.AppDialog(
            title = androidx.compose.ui.res.stringResource(com.wakwau.xplore.filemanager.ui.R.string.title_operation_items, opName),
            confirmButtonText = opName,
            onConfirm = {
                if (isMove) onEvent(DualPaneEvent.ExecuteConfirmedMove(state.operationState.items, state.operationState.targetPath))
                else onEvent(DualPaneEvent.ExecuteConfirmedCopy(state.operationState.items, state.operationState.targetPath))
            },
            onDismissRequest = { onEvent(DualPaneEvent.ClearOperationState) }
        ) {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(
                    com.wakwau.xplore.filemanager.ui.R.string.msg_operation_confirmation, 
                    opName, state.operationState.items.size, state.operationState.targetPath
                ),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (state.operationState is OperationUiState.ConflictResolution) {
        com.wakwau.xplore.filemanager.ui.component.ConflictResolutionDialog(
            conflictState = state.operationState,
            onDecision = { choice, applyToAll ->
                onEvent(DualPaneEvent.ResolveConflictDecision(choice, applyToAll))
            },
            onDismiss = { onEvent(DualPaneEvent.ClearOperationState) }
        )
    }

    if (state.operationState is OperationUiState.Running) {
        ProgressDialog(
            operationName = stringResource(state.operationState.operationNameRes),
            progress = state.operationState.progress,
            onCancel = { onEvent(DualPaneEvent.CancelOperationRequested) }
        )
    }

    if (state.operationState is OperationUiState.Failure) {
        // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/FileManagerContent.kt
        // [Penjelasan]: Menampilkan dialog galat ketika operasi berkas atau folder gagal dan mereset status operasi saat ditutup.
        val errorMsg = state.operationState.errorMessage
        com.wakwau.xplore.core.ui.components.AppDialog(
            title = stringResource(com.wakwau.xplore.filemanager.ui.R.string.title_processing_operation),
            confirmButtonText = stringResource(com.wakwau.xplore.filemanager.ui.R.string.cd_close),
            onConfirm = { onEvent(DualPaneEvent.ClearOperationState) },
            onDismissRequest = { onEvent(DualPaneEvent.ClearOperationState) }
        ) {
            androidx.compose.material3.Text(
                text = errorMsg,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        }
    }

    if (state.fileDetailState.isVisible) {
        FileDetailDialog(
            state = state.fileDetailState,
            onDismiss = { onEvent(DualPaneEvent.DismissFileDetails) },
            onCalculateChecksum = { item -> onEvent(DualPaneEvent.CalculateChecksum(item)) },
            onRenameClick = { item ->
                onEvent(DualPaneEvent.DismissFileDetails)
                onEvent(DualPaneEvent.ShowRenameDialog(item))
            },
            onRemoveLinkClick = { item ->
                onEvent(DualPaneEvent.DismissFileDetails)
                onRemoveLinkClick(Uri.parse(item.id))
            }
        )
    }

    if (state.searchUiState.isSearchDialogOpen) {
        FileSearchDialog(
            state = state.searchUiState,
            currentLocation = state.activePanel.currentLocation,
            onDismiss = { onEvent(DualPaneEvent.DismissSearchDialog) },
            onSearch = { query -> 
                val queryWithHidden = query.copy(showHidden = showHiddenFiles)
                onEvent(DualPaneEvent.ExecuteSearch(queryWithHidden)) 
            },
            onCancelSearch = { onEvent(DualPaneEvent.SearchCancelled) },
            onFileClick = { item -> 
                if (item.type == FileType.DIRECTORY) {
                    onEvent(DualPaneEvent.OpenLocation(state.activePanelId, item.location))
                }
                onEvent(DualPaneEvent.DismissSearchDialog)
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .onPanelSwipe(
                onSwipeLeft = { panelStateController.switchToRight() },
                onSwipeRight = { panelStateController.switchToLeft() }
            )
    ) {
        // If RIGHT panel is active: SideActionBar is on the LEFT
        if (!isLeftActive) {
            SideActionBar(
                position = FileOperationPanelPosition.LEFT,
                selectedCount = selectedCount,
                showHiddenFiles = showHiddenFiles,
                onActionClick = handleSideAction
            )
        }

        // Active Panel Content (takes remaining width)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ActivePanelContent(
                panel = activePanel,
                treeAdapter = treeAdapter,
                onNavigate = { onEvent(DualPaneEvent.OpenLocation(activePanel.id, it)) },
                onItemClick = { item ->
                    if (item.type == FileType.DIRECTORY) {
                        onEvent(DualPaneEvent.OpenLocation(activePanel.id, item.location))
                    }
                },
                onItemLongClick = { item ->
                    onEvent(DualPaneEvent.SetSelectedItems(activePanel.id, setOf(item.id)))
                    onEvent(DualPaneEvent.ShowFileDetails(item))
                },
                onIconClick = { item ->
                    onEvent(DualPaneEvent.SetSelectedItems(activePanel.id, setOf(item.id)))
                    onEvent(DualPaneEvent.ShowFileDetails(item))
                },
                onSelectionChange = { selectedIds ->
                    onEvent(DualPaneEvent.SetSelectedItems(activePanel.id, selectedIds))
                },
                onRetry = { onEvent(DualPaneEvent.Refresh(activePanel.id)) }
            )
        }

        // If LEFT panel is active: SideActionBar is on the RIGHT
        if (isLeftActive) {
            SideActionBar(
                position = FileOperationPanelPosition.RIGHT,
                selectedCount = selectedCount,
                showHiddenFiles = showHiddenFiles,
                onActionClick = handleSideAction
            )
        }
    }
}
