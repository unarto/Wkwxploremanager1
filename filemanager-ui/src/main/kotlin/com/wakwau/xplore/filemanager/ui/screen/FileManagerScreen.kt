// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/FileManagerScreen.kt
// [Penjelasan]: Menangani navigasi kembali dengan mendispatch DualPaneEvent.NavigateUp tanpa manipulasi java.io.File langsung di UI layer.
package com.wakwau.xplore.filemanager.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.wakwau.xplore.core.ui.theme.LocalXPloreColors
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.filemanager.ui.component.FileManagerTopBar
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.presentation.DualPaneViewModel
import com.wakwau.xplore.filemanager.ui.state.FileOperationPanelPosition
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wakwau.xplore.filemanager.ui.component.SortDialog

@Composable
fun FileManagerScreen(
    viewModel: DualPaneViewModel,
    treeAdapter: TreeNavigationAdapter,
    onSettingsClick: () -> Unit = {},
    onLinkStorageClick: () -> Unit = {},
    onRemoveLinkClick: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // [Jalur Class]: com.wakwau.xplore.filemanager.ui.screen.FileManagerScreen
    // [Penjelasan]: Mengamati MVI state dari DualPaneViewModel dan meneruskan treeAdapter ke FileManagerContent.
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferencesState by viewModel.preferencesState.collectAsStateWithLifecycle()

    val activePanel = if (state.activePanelId == PanelId.LEFT) state.leftPanel else state.rightPanel
    
    var showSortDialog by remember { mutableStateOf(false) }

    val operationPanelPosition = if (state.activePanelId == PanelId.LEFT) {
        FileOperationPanelPosition.RIGHT
    } else {
        FileOperationPanelPosition.LEFT
    }
    
    if (showSortDialog) {
        SortDialog(
            currentOrder = preferencesState.sortOrder,
            currentDirection = preferencesState.sortDirection,
            onDismiss = { showSortDialog = false },
            onSortApplied = { order, direction ->
                viewModel.dispatch(DualPaneEvent.UpdateSortPreferences(order, direction))
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(LocalXPloreColors.current.backgroundBrush),
        topBar = {
            FileManagerTopBar(
                title = stringResource(R.string.app_title),
                selectedCount = activePanel.selectedItemIds.size,
                onBackClick = {
                    viewModel.dispatch(DualPaneEvent.NavigateUp(activePanel.id))
                },
                onSettingsClick = onSettingsClick,
                onLinkStorageClick = onLinkStorageClick
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        FileManagerContent(
            state = state,
            treeAdapter = treeAdapter,
            operationPanelPosition = operationPanelPosition,
            panelStateController = viewModel.panelStateController,
            onEvent = viewModel::dispatch,
            showHiddenFiles = preferencesState.showHiddenFiles,
            onSortClick = { showSortDialog = true },
            onRemoveLinkClick = onRemoveLinkClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
