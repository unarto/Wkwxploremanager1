#!/bin/bash
sed -i 's/isSelected = panelState.selectedItemIds.contains(node.data.id),/isSelected = panelState.selectedItemIds.contains(node.data.location.path),/g' filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/component/DirectoryTreeView.kt
