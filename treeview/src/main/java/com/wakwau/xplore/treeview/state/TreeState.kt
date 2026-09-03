package com.wakwau.xplore.treeview.state

import com.wakwau.xplore.treeview.model.FlattenedTreeNode
import com.wakwau.xplore.treeview.model.TreeNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TreeState<T> {
    private val _roots = mutableListOf<TreeNode<T>>()
    val roots: List<TreeNode<T>> get() = _roots

    private val _visibleNodes = MutableStateFlow<List<FlattenedTreeNode<T>>>(emptyList())
    val visibleNodes: StateFlow<List<FlattenedTreeNode<T>>> = _visibleNodes.asStateFlow()

    fun setRoots(newRoots: List<TreeNode<T>>) {
        _roots.clear()
        _roots.addAll(newRoots)
        updateVisibleNodes()
    }

    fun clear() {
        _roots.clear()
        updateVisibleNodes()
    }

    fun expand(node: TreeNode<T>) {
        node.expand()
        updateVisibleNodes()
    }

    fun collapse(node: TreeNode<T>) {
        node.collapse()
        updateVisibleNodes()
    }

    // [Jalur Class/Modul]: treeview/src/main/java/com/wakwau/xplore/treeview/state/TreeState.kt
    // [Penjelasan]: Meng-collapse node dan seluruh subtreenya secara rekursif serta memperbarui daftar visibleNodes sebagai single source of truth.
    fun collapseRecursively(node: TreeNode<T>) {
        node.collapseRecursively()
        updateVisibleNodes()
    }

    fun toggle(node: TreeNode<T>) {
        node.toggleExpanded()
        updateVisibleNodes()
    }

    fun isExpanded(node: TreeNode<T>): Boolean {
        return node.isExpanded
    }

    fun forceRefresh() {
        updateVisibleNodes()
    }

    private fun updateVisibleNodes() {
        val flatList = mutableListOf<FlattenedTreeNode<T>>()
        for ((index, root) in _roots.withIndex()) {
            val isLast = index == _roots.lastIndex
            flatten(root, flatList, isLastChild = isLast, ancestorsHasNextSibling = emptyList())
        }
        _visibleNodes.value = flatList
    }

    private fun flatten(
        node: TreeNode<T>,
        result: MutableList<FlattenedTreeNode<T>>,
        isLastChild: Boolean,
        ancestorsHasNextSibling: List<Boolean>
    ) {
        result.add(
            FlattenedTreeNode(
                node = node,
                depth = node.depth,
                isLastChild = isLastChild,
                ancestorHasNextSibling = ancestorsHasNextSibling,
                isEmptyPlaceholder = node.isPlaceholder
            )
        )
        if (node.isExpanded) {
            val children = node.children
            val nextAncestors = ancestorsHasNextSibling + (!isLastChild)
            for ((index, child) in children.withIndex()) {
                val childIsLast = index == children.lastIndex
                flatten(
                    node = child,
                    result = result,
                    isLastChild = childIsLast,
                    ancestorsHasNextSibling = nextAncestors
                )
            }
        }
    }
}
