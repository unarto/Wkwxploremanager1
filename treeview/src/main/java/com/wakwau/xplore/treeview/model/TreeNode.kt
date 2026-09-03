package com.wakwau.xplore.treeview.model

import java.util.UUID

class TreeNode<T>(
    val data: T,
    val id: String = UUID.randomUUID().toString(),
    val isPlaceholder: Boolean = false
) {
    var parent: TreeNode<T>? = null
        internal set

    @Volatile
    private var _children: List<TreeNode<T>> = emptyList()
    val children: List<TreeNode<T>> get() = _children

    // [Jalur Class/Modul]: treeview/src/main/java/com/wakwau/xplore/treeview/model/TreeNode.kt
    // [Penjelasan]: Properti status ekspansi node yang dapat dimutasi secara langsung oleh TreeSelectionHandler atau tree controller.
    var isExpanded: Boolean = false

    val depth: Int
        get() {
            var d = 0
            var current = parent
            while (current != null) {
                d++
                current = current.parent
            }
            return d
        }

    val isRoot: Boolean get() = parent == null
    val hasChildren: Boolean get() = _children.isNotEmpty()

    fun addChild(child: TreeNode<T>) {
        var current = this as TreeNode<T>?
        while (current != null) {
            if (current.id == child.id) {
                throw IllegalArgumentException("Cycle detected: Cannot add node as its own descendant.")
            }
            current = current.parent
        }
        child.parent = this
        _children = _children + child
    }

    fun removeChild(child: TreeNode<T>) {
        val newChildren = _children.toMutableList()
        if (newChildren.remove(child)) {
            child.parent = null
            _children = newChildren.toList()
        }
    }

    fun clearChildren() {
        _children.forEach { it.parent = null }
        _children = emptyList()
    }
    
    fun sortChildren(comparator: Comparator<TreeNode<T>>) {
        _children = _children.sortedWith(comparator)
        _children.forEach { it.sortChildren(comparator) }
    }

    fun expand() { isExpanded = true }
    fun collapse() { isExpanded = false }
    fun toggleExpanded() { isExpanded = !isExpanded }

    // [Jalur Class/Modul]: treeview/src/main/java/com/wakwau/xplore/treeview/model/TreeNode.kt
    // [Penjelasan]: Meng-collapse node ini beserta seluruh descendant di bawahnya secara rekursif agar branch yang ditinggalkan bersih dari sisa state ekspansi lama.
    fun collapseRecursively() {
        collapse()
        for (child in _children) {
            child.collapseRecursively()
        }
    }
}
