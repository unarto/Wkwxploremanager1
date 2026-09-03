// [Jalur Class]: com.wakwau.xplore.filemanager.ui.component.ActivePanelContent
// [Penjelasan]: Menampilkan konten panel aktif dengan mendelegasikan perenderan file tree ke DirectoryTreeView terisolasi beserta callback klik ikon berkas.
package com.wakwau.xplore.filemanager.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.filemanager.ui.state.PanelState
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter

@Composable
fun ActivePanelContent(
    panel: PanelState,
    treeAdapter: TreeNavigationAdapter,
    onNavigate: (StorageLocation) -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onSelectionChange: (Set<String>) -> Unit,
    onRetry: () -> Unit,
    onIconClick: (FileItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // [Jalur Class]: com.wakwau.xplore.filemanager.ui.component.ActivePanelContent
    // [Penjelasan]: Merender tampilan pohon berkas terisolasi melalui DirectoryTreeView tanpa rujukan ke FileTreeEngine.
    Column(modifier = modifier.fillMaxSize()) {
        DirectoryTreeView(
            panelState = panel,
            treeAdapter = treeAdapter,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            onSelectionChange = onSelectionChange,
            onRetry = onRetry,
            onNavigate = onNavigate,
            onIconClick = onIconClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

