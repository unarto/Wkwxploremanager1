package com.wakwau.xplore.treeview.model

object TreeScopeCalculator {

    /**
     * Calculates the index range [startIndex..endIndex] in [visibleNodes]
     * for a focused node matching [focusedIdOrPath].
     *
     * - If the focused node is not found or focusedIdOrPath is null: returns null.
     * - If the focused node is a leaf or collapsed (not expanded): returns startIndex..startIndex.
     * - If the focused node is expanded: returns startIndex..endIndex where endIndex is the last
     *   visible descendant node with depth > parent.depth.
     */
    fun <T> calculateFocusRange(
        visibleNodes: List<FlattenedTreeNode<T>>,
        focusedIdOrPath: String?,
        idExtractor: (T) -> String = { it.toString() }
    ): IntRange? {
        if (focusedIdOrPath == null || visibleNodes.isEmpty()) return null

        val startIndex = visibleNodes.indexOfFirst { flattened ->
            flattened.node.id == focusedIdOrPath || 
            idExtractor(flattened.node.data) == focusedIdOrPath
        }

        if (startIndex == -1) return null

        val startNode = visibleNodes[startIndex]

        // If collapsed or no expanded children
        if (!startNode.node.isExpanded) {
            return startIndex..startIndex
        }

        var endIndex = startIndex
        val parentDepth = startNode.depth

        for (i in (startIndex + 1) until visibleNodes.size) {
            val child = visibleNodes[i]
            if (child.depth > parentDepth) {
                endIndex = i
            } else {
                break
            }
        }

        return startIndex..endIndex
    }

    /**
     * Resolves the [BorderPosition] for a given [currentIndex] based on the [range].
     */
    fun getBorderPosition(currentIndex: Int, range: IntRange?): BorderPosition {
        if (range == null || currentIndex !in range) {
            return BorderPosition.NONE
        }
        if (range.first == range.last) {
            return BorderPosition.SINGLE
        }
        return when (currentIndex) {
            range.first -> BorderPosition.TOP
            range.last -> BorderPosition.BOTTOM
            else -> BorderPosition.MIDDLE
        }
    }
}
