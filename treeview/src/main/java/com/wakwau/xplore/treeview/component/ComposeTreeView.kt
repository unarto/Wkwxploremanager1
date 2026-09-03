package com.wakwau.xplore.treeview.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wakwau.xplore.treeview.interaction.TreeInteraction
import com.wakwau.xplore.treeview.model.BorderPosition
import com.wakwau.xplore.treeview.model.TreeNode
import com.wakwau.xplore.treeview.model.TreeScopeCalculator
import com.wakwau.xplore.treeview.state.TreeState

@Composable
fun <T> ComposeTreeView(
    treeState: TreeState<T>,
    modifier: Modifier = Modifier,
    focusedId: String? = null,
    interaction: TreeInteraction<T>? = null,
    key: ((index: Int, item: com.wakwau.xplore.treeview.model.FlattenedTreeNode<T>) -> Any)? = { _, it -> "${it.node.data.hashCode()}_${it.node.id}" },
    emptyPlaceholderContent: (@Composable (node: TreeNode<T>) -> Unit)? = null,
    nodeContent: @Composable (node: TreeNode<T>, borderPosition: BorderPosition) -> Unit
) {
    val visibleNodes by treeState.visibleNodes.collectAsStateWithLifecycle()

    val focusRange = remember(visibleNodes, focusedId) {
        TreeScopeCalculator.calculateFocusRange(visibleNodes, focusedId)
    }

    LazyColumn(
        modifier = modifier
    ) {
        itemsIndexed(
            items = visibleNodes,
            key = key
        ) { index, flattenedNode ->
            val isPlaceholder = flattenedNode.isEmptyPlaceholder || flattenedNode.node.isPlaceholder
            val borderPosition = TreeScopeCalculator.getBorderPosition(index, focusRange)

            TreeNodeRow(
                flattenedNode = flattenedNode,
                borderPosition = borderPosition,
                onToggle = {
                    if (interaction != null) {
                        interaction.onToggle(flattenedNode.node)
                    } else {
                        treeState.toggle(flattenedNode.node)
                    }
                },
                onClick = {
                    if (!isPlaceholder) {
                        interaction?.onNodeClick(flattenedNode.node)
                    }
                },
                onLongClick = {
                    if (!isPlaceholder) {
                        interaction?.onNodeLongClick(flattenedNode.node)
                    }
                }
            ) { node ->
                if (isPlaceholder && emptyPlaceholderContent != null) {
                    emptyPlaceholderContent(node)
                } else {
                    nodeContent(node, borderPosition)
                }
            }
        }
    }
}

@Composable
fun <T> ComposeTreeView(
    treeState: TreeState<T>,
    modifier: Modifier = Modifier,
    interaction: TreeInteraction<T>? = null,
    key: ((index: Int, item: com.wakwau.xplore.treeview.model.FlattenedTreeNode<T>) -> Any)? = { _, it -> "${it.node.data.hashCode()}_${it.node.id}" },
    emptyPlaceholderContent: (@Composable (node: TreeNode<T>) -> Unit)? = null,
    nodeContent: @Composable (node: TreeNode<T>) -> Unit
) {
    ComposeTreeView(
        treeState = treeState,
        modifier = modifier,
        focusedId = null,
        interaction = interaction,
        key = key,
        emptyPlaceholderContent = emptyPlaceholderContent,
        nodeContent = { node, _ -> nodeContent(node) }
    )
}
