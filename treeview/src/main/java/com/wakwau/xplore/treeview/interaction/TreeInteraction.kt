package com.wakwau.xplore.treeview.interaction

import com.wakwau.xplore.treeview.model.TreeNode

interface TreeInteraction<T> {
    fun onNodeClick(node: TreeNode<T>) {}
    fun onNodeLongClick(node: TreeNode<T>) {}
    fun onToggle(node: TreeNode<T>) {}
    fun onExpand(node: TreeNode<T>) {}
    fun onCollapse(node: TreeNode<T>) {}
}

