// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandler.kt
// [Penjelasan]: Mengelola evaluasi status penandaan ceklis dengan Strict Path Isolation & Multi-Select Anak.
package com.wakwau.xplore.filemanager.ui.selection

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.treeview.model.TreeNode

class TreeSelectionHandler {

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandler.kt
    // [Penjelasan]: Mengumpulkan seluruh path turunan (descendant paths) dari sebuah node secara rekursif.
    fun collectDescendantPaths(node: TreeNode<FileItem>): Set<String> {
        val descendantPaths = mutableSetOf<String>()
        fun traverse(parent: TreeNode<FileItem>) {
            for (child in parent.children) {
                descendantPaths.add(child.data.location.path)
                traverse(child)
            }
        }
        traverse(node)
        return descendantPaths
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandler.kt
    // [Penjelasan]: Menghitung status seleksi ceklis; Storage Node dilarang CHECKED. Evaluasi WAJIB pakai FULL PATH.
    fun getSelectionState(
        node: TreeNode<FileItem>,
        selectedIds: Set<String>
    ): FolderCheckCycleState {
        if ((node.isRoot || node.parent == null) && node.data.type == FileType.DIRECTORY) {
            return FolderCheckCycleState.UNCHECKED
        }

        val path = node.data.location.path
        if (selectedIds.contains(path)) {
            return FolderCheckCycleState.CHECKED
        }

        return FolderCheckCycleState.UNCHECKED
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandler.kt
    // [Penjelasan]: Menghapus semua path dari selectedIds yang berada di luar scope parent aktif.
    private fun enforceStrictScopeIsolation(
        currentSelection: MutableSet<String>,
        activeParentPath: String
    ) {
        val parentPrefix = if (activeParentPath.endsWith("/")) activeParentPath else "$activeParentPath/"
        currentSelection.retainAll { selectedPath ->
            val pathWithSlash = if (selectedPath.endsWith("/")) selectedPath else "$selectedPath/"
            selectedPath.startsWith(parentPrefix) || parentPrefix.startsWith(pathWithSlash)
        }
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandler.kt
    // [Penjelasan]: Menghitung set path seleksi baru dengan isolasi jalur absolut secara ketat.
    fun nextSelection(
        node: TreeNode<FileItem>,
        currentSelection: Set<String>,
        onAutoExpand: () -> Unit = {}
    ): Set<String> {
        val newSelection = currentSelection.toMutableSet()
        val nodePath = node.data.location.path
        val prefix = if (nodePath.endsWith("/")) nodePath else "$nodePath/"
        val parentPath = node.parent?.data?.location?.path ?: ""

        // 1. Level File Node (Bukan Direktori) ATAU Seleksi Tunggal Anak
        if (node.data.type != FileType.DIRECTORY) {
            if (newSelection.contains(nodePath)) {
                newSelection.remove(nodePath)
            } else {
                if (parentPath.isNotEmpty()) {
                    enforceStrictScopeIsolation(newSelection, parentPath)
                }
                newSelection.add(nodePath)
            }
            return newSelection
        }

        // 2. Level Storage Node (Internal Storage / SD Card)
        if (node.isRoot || node.parent == null) {
            newSelection.remove(nodePath)

            // Auto-Expand Storage
            onAutoExpand()

            val directChildren = node.children
            if (directChildren.isEmpty()) {
                return newSelection
            }

            val directChildPaths = directChildren.map { it.data.location.path }.toSet()
            val allDirectChildrenMarked = directChildren.all { child ->
                newSelection.contains(child.data.location.path)
            }

            if (allDirectChildrenMarked) {
                // Klik 2 pada Storage: Hapus semua tercentang di bawah storage
                newSelection.removeAll { path ->
                    path == nodePath || path.startsWith(prefix) || directChildPaths.contains(path)
                }
            } else {
                // Klik 1 pada Storage: Bersihkan tempat lain, centang HANYA direct children storage ini
                newSelection.clear()
                directChildren.forEach { child ->
                    newSelection.add(child.data.location.path)
                }
            }
            return newSelection
        }

        // 3. Level Folder Node (3-Step Cycle)
        val isFolderSelfMarked = newSelection.contains(nodePath)
        val directChildren = node.children
        val descendantPaths = collectDescendantPaths(node)

        val hasAnyDescendantMarked = newSelection.any { path ->
            (path.startsWith(prefix) || descendantPaths.contains(path)) && path != nodePath
        }

        when {
            // SIKLUS KLIK 2: Mark All Children + Auto Expand
            isFolderSelfMarked -> {
                // Unmark folder induk
                newSelection.remove(nodePath)

                // AUTO-EXPAND FOLDER
                onAutoExpand()

                // Bersihkan item di luar scope folder ini
                enforceStrictScopeIsolation(newSelection, nodePath)

                // Centang semua anak langsung
                if (directChildren.isNotEmpty()) {
                    directChildren.forEach { child ->
                        newSelection.add(child.data.location.path)
                    }
                }
            }

            // SIKLUS KLIK 3: Unmark All
            hasAnyDescendantMarked -> {
                newSelection.remove(nodePath)
                newSelection.removeAll { path ->
                    path.startsWith(prefix) || descendantPaths.contains(path)
                }
            }

            // SIKLUS KLIK 1: Single Mark Folder
            else -> {
                // Bersihkan centang di luar jalur folder ini (DCIM dll auto hapus)
                if (parentPath.isNotEmpty()) {
                    enforceStrictScopeIsolation(newSelection, parentPath)
                } else {
                    enforceStrictScopeIsolation(newSelection, nodePath)
                }

                // Centang full path folder ini
                newSelection.add(nodePath)
            }
        }

        return newSelection
    }
}


