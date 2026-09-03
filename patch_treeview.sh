#!/bin/bash
sed -i 's/interaction: TreeInteraction<T>? = null,/interaction: TreeInteraction<T>? = null,\n    key: ((index: Int, item: com.wakwau.xplore.treeview.model.FlattenedTreeNode<T>) -> Any)? = { _, it -> "${it.node.data.hashCode()}_${it.node.id}" },/' treeview/src/main/java/com/wakwau/xplore/treeview/component/ComposeTreeView.kt
sed -i 's/key = { _, it -> "${it.node.data.hashCode()}_${it.node.id}" }/key = key/' treeview/src/main/java/com/wakwau/xplore/treeview/component/ComposeTreeView.kt
