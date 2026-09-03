package com.wakwau.xplore.treeview.model

data class FlattenedTreeNode<T>(
    val node: TreeNode<T>,
    val depth: Int,
    val isLastChild: Boolean = false,
    val ancestorHasNextSibling: List<Boolean> = emptyList(),
    val isEmptyPlaceholder: Boolean = false
)

