#!/bin/bash
sed -i 's/selectedIds.contains(node.data.id) || selectedIds.contains(node.data.location.path)/selectedIds.contains(node.data.location.path)/g' filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt
