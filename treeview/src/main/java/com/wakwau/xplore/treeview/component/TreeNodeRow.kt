// [Jalur Class]: com.wakwau.xplore.treeview.component.TreeNodeRow
// [Penjelasan]: Komponen UI composable untuk me-render satu baris node dalam hierarki pohon berkas beserta garis panduan cabang dan indentasi terstruktur.
package com.wakwau.xplore.treeview.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.treeview.model.BorderPosition
import com.wakwau.xplore.treeview.model.FlattenedTreeNode
import com.wakwau.xplore.treeview.model.TreeNode

object TreeDefaults {
    val IndentWidth: Dp = 18.dp
    val MinRowHeight: Dp = 44.dp
    val PlaceholderMinRowHeight: Dp = 32.dp
    val BorderStrokeWidth: Dp = 1.dp
    val BorderCornerRadius: Dp = 3.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> TreeNodeRow(
    flattenedNode: FlattenedTreeNode<T>,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    borderPosition: BorderPosition = BorderPosition.NONE,
    borderColor: Color = XPloreTheme.colors.folderSelectionColor,
    indentWidth: Dp = TreeDefaults.IndentWidth,
    minHeight: Dp = TreeDefaults.MinRowHeight,
    content: @Composable (node: TreeNode<T>) -> Unit
) {
    val node = flattenedNode.node
    val isPlaceholder = flattenedNode.isEmptyPlaceholder || node.isPlaceholder
    val colors = XPloreTheme.colors

    val rowBg = Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .defaultMinSize(minHeight = if (isPlaceholder) TreeDefaults.PlaceholderMinRowHeight else minHeight)
            .background(rowBg)
            .treeScopeBorder(
                position = borderPosition,
                borderColor = borderColor,
                strokeWidth = TreeDefaults.BorderStrokeWidth,
                cornerRadius = TreeDefaults.BorderCornerRadius
            )
            .then(
                if (isPlaceholder) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            )
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TreeBranchGuide(
            depth = flattenedNode.depth,
            isLastChild = flattenedNode.isLastChild,
            ancestorHasNextSibling = flattenedNode.ancestorHasNextSibling,
            branchColor = colors.treeLine,
            indentWidth = indentWidth
        )
        if (!isPlaceholder) {
            TreeExpandToggle(
                isExpanded = node.isExpanded,
                isLeaf = !node.hasChildren,
                tint = colors.treeExpandArrow,
                onToggle = onToggle
            )
        } else {
            Spacer(modifier = Modifier.width(TreeDefaults.IndentWidth))
        }
        content(node)
    }
}
