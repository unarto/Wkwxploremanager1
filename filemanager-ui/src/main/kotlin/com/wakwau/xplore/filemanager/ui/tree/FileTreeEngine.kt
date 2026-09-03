// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
// [Penjelasan]: Engine pohon direktori berkas yang memuat hierarki folder secara asinkron menggunakan ListDirectoryUseCase, TreeState, dan FileTreeItemFactory untuk navigasi pohon berkas aktual termasuk Up Dir bertingkat dengan batas root volume yang aman.
package com.wakwau.xplore.filemanager.ui.tree

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.filemanager.factory.FileTreeItemFactory
import com.wakwau.xplore.filemanager.usecase.ListDirectoryUseCase
import com.wakwau.xplore.treeview.model.TreeNode
import com.wakwau.xplore.treeview.state.TreeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
import com.wakwau.xplore.core.storage.preferences.FileSortOrder
import com.wakwau.xplore.core.storage.preferences.FileSortDirection

class FileTreeEngine(
    private val listDirectoryUseCase: ListDirectoryUseCase,
    private val appPreferencesRepository: AppPreferencesRepository? = null,
    private val fileTreeItemFactory: FileTreeItemFactory = FileTreeItemFactory(),
    val treeState: TreeState<FileItem> = TreeState()
) {
    private val loadingNodes = ConcurrentHashMap.newKeySet<String>()

    private val _selectedPath = MutableStateFlow<String?>(null)
    val selectedPath: StateFlow<String?> = _selectedPath.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    suspend fun refreshExpandedNodes() {
        suspend fun refreshNode(node: TreeNode<FileItem>) {
            if (treeState.isExpanded(node) && node.data.type == FileType.DIRECTORY) {
                val previousExpandedIds = node.children.filter { it.isExpanded }.map { it.id }.toSet()
                loadChildren(node)
                for (child in node.children) {
                    if (previousExpandedIds.contains(child.id)) {
                        treeState.expand(child)
                        refreshNode(child)
                    }
                }
            }
        }
        for (root in treeState.roots) {
            refreshNode(root)
        }
        treeState.forceRefresh()
    }

    suspend fun refreshNodeByPath(path: String) {
        val node = findNodeByPath(path)
        if (node != null && node.data.type == FileType.DIRECTORY && treeState.isExpanded(node)) {
            val previousExpandedIds = node.children.filter { it.isExpanded }.map { it.id }.toSet()
            loadChildren(node)
            for (child in node.children) {
                if (previousExpandedIds.contains(child.id)) {
                    treeState.expand(child)
                }
            }
            treeState.forceRefresh()
        } else {
            refreshExpandedNodes()
        }
    }

    fun setSelectedPath(path: String?) {
        _selectedPath.value = path
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
    // [Penjelasan]: Mencari node pada hierarki pohon berdasarkan path lokasi berkas atau ID node secara rekursif.
    fun findNodeByPath(path: String): TreeNode<FileItem>? {
        fun search(nodes: List<TreeNode<FileItem>>): TreeNode<FileItem>? {
            for (node in nodes) {
                if (node.data.location.path == path || node.id == path) return node
                val found = search(node.children)
                if (found != null) return found
            }
            return null
        }
        return search(treeState.roots)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
    // [Penjelasan]: Menavigasikan fokus pohon ke direktori induk (parent) satu level ke atas, meng-collapse folder yang ditinggalkan beserta seluruh sub-branchnya secara rekursif, memperbarui selectedPath, memastikan node parent expanded, dan merefresh treeState.
    fun navigateUp(): StorageLocation? {
        val currentPath = _selectedPath.value
        if (currentPath == null) {
            val firstRoot = treeState.roots.firstOrNull() ?: return null
            _selectedPath.value = firstRoot.data.location.path
            return firstRoot.data.location
        }

        val currentNode = findNodeByPath(currentPath)
        if (currentNode != null) {
            val parentNode = currentNode.parent
            if (parentNode != null) {
                // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
                // [Penjelasan]: Menutup/collapse folder yang baru ditinggalkan beserta branch di bawahnya secara rekursif agar sesuai perilaku navigasi X-plore.
                treeState.collapseRecursively(currentNode)
                _selectedPath.value = parentNode.data.location.path
                if (!treeState.isExpanded(parentNode)) {
                    treeState.expand(parentNode)
                }
                treeState.forceRefresh()
                return parentNode.data.location
            } else {
                // Sudah berada pada batas volume root (isRoot = true), tetap di root tanpa navigasi keluar
                _selectedPath.value = currentNode.data.location.path
                return currentNode.data.location
            }
        } else {
            val trimmed = currentPath.trimEnd('/')
            val lastSlashIndex = trimmed.lastIndexOf('/')
            if (lastSlashIndex > 0) {
                val parentPath = trimmed.substring(0, lastSlashIndex)
                val parentNode = findNodeByPath(parentPath)
                if (parentNode != null) {
                    _selectedPath.value = parentNode.data.location.path
                    if (!treeState.isExpanded(parentNode)) {
                        treeState.expand(parentNode)
                    }
                    treeState.forceRefresh()
                    return parentNode.data.location
                } else {
                    _selectedPath.value = parentPath
                    val rootId = treeState.roots.firstOrNull()?.data?.location?.rootId ?: StorageConstants.UNKNOWN_ROOT_ID
                    return StorageLocation(parentPath, rootId)
                }
            }
            val firstRoot = treeState.roots.firstOrNull()
            if (firstRoot != null) {
                _selectedPath.value = firstRoot.data.location.path
                return firstRoot.data.location
            }
            return null
        }
    }

    suspend fun loadVolumesAsRoots(volumes: List<com.wakwau.xplore.core.storage.model.StorageVolumeItem>) {
        _errorState.value = null
        val roots = volumes.map { volume ->
            val rootItem = fileTreeItemFactory.createVolumeRoot(volume)
            TreeNode(data = rootItem, id = volume.rootPath)
        }
        treeState.setRoots(roots)
        if (_selectedPath.value == null && roots.isNotEmpty()) {
            _selectedPath.value = roots.first().data.location.path
        }
    }

    suspend fun loadRoot(rootItem: FileItem) {
        _errorState.value = null
        val rootNode = TreeNode(data = rootItem, id = rootItem.location.path)
        treeState.setRoots(listOf(rootNode))
        loadChildren(rootNode)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
    // [Penjelasan]: Membuka (expand) node direktori secara eksplisit dan memuat anak node jika belum dimuat.
    suspend fun expandNode(node: TreeNode<FileItem>) {
        _errorState.value = null
        if (!treeState.isExpanded(node)) {
            if (!node.hasChildren && node.data.type == FileType.DIRECTORY) {
                loadChildren(node)
            } else {
                treeState.expand(node)
            }
        }
    }

    suspend fun toggleNode(node: TreeNode<FileItem>) {
        _errorState.value = null
        if (treeState.isExpanded(node)) {
            treeState.collapse(node)
        } else {
            if (!node.hasChildren && node.data.type == FileType.DIRECTORY) {
                loadChildren(node)
            } else {
                treeState.expand(node)
            }
        }
    }

    private suspend fun loadChildren(node: TreeNode<FileItem>) {
        if (!loadingNodes.add(node.id)) return
        try {
            when (val result = listDirectoryUseCase(node.data.location)) {
                is FileOperationResult.Success -> {
                    node.clearChildren()
                    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
                    // [Penjelasan]: Memuat anak node aktual murni tanpa menyuntikkan placeholder buatan ke dalam struktur data domain tree.
                    val comparator = getComparator()
                    val sortedItems = result.data.map { item ->
                        TreeNode(data = item, id = item.location.path)
                    }.sortedWith(comparator)
                    
                    sortedItems.forEach { node.addChild(it) }
                    treeState.expand(node)
                }
                is FileOperationResult.Failure -> {
                    _errorState.value = result.error.name
                }
                is FileOperationResult.Cancelled -> {
                    // Ignore
                }
                is FileOperationResult.Completed -> {
                    // No-op
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _errorState.value = e.message
        } finally {
            loadingNodes.remove(node.id)
        }
    }

    fun reSortCurrentNodes() {
        val comparator = getComparator()
        treeState.roots.forEach { it.sortChildren(comparator) }
        treeState.forceRefresh()
    }

    private fun getComparator(): java.util.Comparator<TreeNode<FileItem>> {
        val prefs = appPreferencesRepository?.getPreferencesState()
        val order = prefs?.sortOrder ?: FileSortOrder.NAME
        val direction = prefs?.sortDirection ?: FileSortDirection.ASCENDING

        val baseComparator = when (order) {
            FileSortOrder.NAME -> compareBy<TreeNode<FileItem>> { it.data.name.lowercase() }
            FileSortOrder.DATE -> compareBy { it.data.metadata.modifiedTime }
            FileSortOrder.SIZE -> compareBy { it.data.metadata.size }
            FileSortOrder.TYPE -> compareBy { it.data.name.substringAfterLast('.', "") }
        }
        
        val directedComparator = if (direction == FileSortDirection.DESCENDING) {
            baseComparator.reversed()
        } else {
            baseComparator
        }
        
        return compareBy<TreeNode<FileItem>> { it.data.type != FileType.DIRECTORY }.then(directedComparator)
    }

    fun clearError() {
        _errorState.value = null
    }

    fun getFocusRange(): IntRange? {
        val visibleNodes = treeState.visibleNodes.value
        val path = _selectedPath.value
        return com.wakwau.xplore.treeview.model.TreeScopeCalculator.calculateFocusRange(visibleNodes, path) { it.location.path }
    }

    fun getBorderPositionForIndex(index: Int): com.wakwau.xplore.treeview.model.BorderPosition {
        val range = getFocusRange()
        return com.wakwau.xplore.treeview.model.TreeScopeCalculator.getBorderPosition(index, range)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
    // [Penjelasan]: Mengumpulkan seluruh FileItem yang terseleksi berdasarkan selectedIds, mengabaikan node Storage root untuk perlindungan storage.
    fun getSelectedItems(selectedIds: Set<String>): List<FileItem> {
        val selectedItems = mutableListOf<FileItem>()
        fun traverse(nodes: List<TreeNode<FileItem>>) {
            for (node in nodes) {
                if (!node.isRoot && (selectedIds.contains(node.data.location.path))) {
                    selectedItems.add(node.data)
                } else {
                    traverse(node.children)
                }
            }
        }
        traverse(treeState.roots)
        return selectedItems
    }

    fun updateSearchResults(keyword: String, items: List<FileItem>) {
        val searchRootId = StorageConstants.VIRTUAL_SEARCH_ROOT_ID
        
        // Remove existing search results root if any
        val filteredRoots = treeState.roots.filter { it.id != searchRootId }.toMutableList()
        
        val countLabel = "${StorageConstants.SEARCH_RESULTS_PREFIX}(${items.size})"
        val rootItem = fileTreeItemFactory.createSearchResultsRoot(
            keyword = keyword
        ).copy(name = countLabel)
        val searchRootNode = TreeNode(data = rootItem, id = searchRootId)
        
        // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
        // [Penjelasan]: Menambahkan item hasil pencarian aktual dengan prefix ID search result agar UI dapat merender jalur direktori induk sesuai antarmuka X-plore.
        val comparator = getComparator()
        val sortedItems = items.map { item ->
            val searchItem = item.copy(id = "${StorageConstants.SEARCH_RESULT_ID_PREFIX}${item.location.path}")
            TreeNode(data = searchItem, id = searchItem.id)
        }.sortedWith(comparator)
        
        sortedItems.forEach { searchRootNode.addChild(it) }
        
        filteredRoots.add(searchRootNode)
        treeState.setRoots(filteredRoots)
        treeState.expand(searchRootNode)
        treeState.forceRefresh()
    }
}
