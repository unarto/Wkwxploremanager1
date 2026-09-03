#!/bin/bash
sed -i 's/interaction = interaction,/interaction = interaction,\n        key = key,/' treeview/src/main/java/com/wakwau/xplore/treeview/component/ComposeTreeView.kt
