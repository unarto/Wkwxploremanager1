// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/TreeNavigationAdapter.kt
// [Penjelasan]: Abstraksi delegasi handler yang mengelola operasi tree (expand, collapse, load child nodes, set roots, re-sort, refresh, dan Up Dir) untuk panel kiri dan kanan tanpa mengekspos FileTreeEngine ke ViewModel atau Screen UI.
package com.wakwau.xplore.filemanager.ui.tree

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.filemanager.factory.FileTreeItemFactory
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.usecase.ListDirectoryUseCase
import com.wakwau.xplore.treeview.model.TreeNode
import kotlinx.coroutines.flow.StateFlow

import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository

class TreeNavigationAdapter(
    listDirectoryUseCase: ListDirectoryUseCase,
    appPreferencesRepository: AppPreferencesRepository? = null,
    fileTreeItemFactory: FileTreeItemFactory = FileTreeItemFactory()
) {
    private val leftEngine = FileTreeEngine(listDirectoryUseCase, appPreferencesRepository, fileTreeItemFactory)
    private val rightEngine = FileTreeEngine(listDirectoryUseCase, appPreferencesRepository, fileTreeItemFactory)

    fun reSortNodes() {
        leftEngine.reSortCurrentNodes()
        rightEngine.reSortCurrentNodes()
    }

    suspend fun refreshAllNodes() {
        leftEngine.refreshExpandedNodes()
        rightEngine.refreshExpandedNodes()
    }

    suspend fun refreshNodeByPath(panelId: PanelId, path: String) {
        getEngine(panelId).refreshNodeByPath(path)
    }

    fun getEngine(panelId: PanelId): FileTreeEngine {
        return if (panelId == PanelId.LEFT) leftEngine else rightEngine
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/TreeNavigationAdapter.kt
    // [Penjelasan]: Mendelegasikan navigasi Up Dir ke FileTreeEngine panel yang relevan secara independen.
    fun navigateUp(panelId: PanelId): StorageLocation? {
        return getEngine(panelId).navigateUp()
    }

    suspend fun loadVolumesAsRoots(panelId: PanelId, volumes: List<StorageVolumeItem>) {
        getEngine(panelId).loadVolumesAsRoots(volumes)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/TreeNavigationAdapter.kt
    // [Penjelasan]: Membuka (expand) node direktori secara eksplisit pada panel tertentu.
    suspend fun expandNode(panelId: PanelId, node: TreeNode<FileItem>) {
        getEngine(panelId).expandNode(node)
    }

    suspend fun toggleNode(panelId: PanelId, node: TreeNode<FileItem>) {
        getEngine(panelId).toggleNode(node)
    }

    fun setSelectedPath(panelId: PanelId, path: String?) {
        getEngine(panelId).setSelectedPath(path)
    }

    fun clearError(panelId: PanelId) {
        getEngine(panelId).clearError()
    }

    fun getSelectedPath(panelId: PanelId): StateFlow<String?> {
        return getEngine(panelId).selectedPath
    }

    fun getErrorState(panelId: PanelId): StateFlow<String?> {
        return getEngine(panelId).errorState
    }

    fun hasRoots(panelId: PanelId): Boolean {
        return getEngine(panelId).treeState.roots.isNotEmpty()
    }

    fun getSelectedItems(panelId: PanelId, selectedIds: Set<String>): List<FileItem> {
        return getEngine(panelId).getSelectedItems(selectedIds)
    }

    fun updateSearchResults(panelId: PanelId, keyword: String, items: List<FileItem>) {
        getEngine(panelId).updateSearchResults(keyword, items)
    }
}
