package com.wakwau.xplore.treeview.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakwau.xplore.core.ui.theme.XPloreTheme

@Composable
fun TreeBranchGuide(
    depth: Int,
    isLastChild: Boolean = false,
    ancestorHasNextSibling: List<Boolean> = emptyList(),
    branchColor: Color = XPloreTheme.colors.treeLine,
    indentWidth: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    if (depth <= 0) return

    Row(modifier = modifier.fillMaxHeight()) {
        for (level in 0 until depth) {
            val isCurrentLevel = level == depth - 1
            val shouldDrawAncestorLine = level < ancestorHasNextSibling.size && ancestorHasNextSibling[level]

            Canvas(
                modifier = Modifier
                    .width(indentWidth)
                    .fillMaxHeight()
            ) {
                val midX = size.width / 2
                val midY = size.height / 2
                val strokeWidthPx = 1.5.dp.toPx()

                if (!isCurrentLevel) {
                    if (shouldDrawAncestorLine) {
                        // Continuous vertical tree trunk line for ancestor level
                        drawLine(
                            color = branchColor,
                            start = Offset(midX, 0f),
                            end = Offset(midX, size.height),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Square
                        )
                    }
                } else {
                    // Current level branch lines
                    // Line from top to center
                    drawLine(
                        color = branchColor,
                        start = Offset(midX, 0f),
                        end = Offset(midX, midY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Square
                    )
                    // If not last child, line continues downward to bottom
                    if (!isLastChild) {
                        drawLine(
                            color = branchColor,
                            start = Offset(midX, midY),
                            end = Offset(midX, size.height),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Square
                        )
                    }
                    // Horizontal line to the node / expand toggle
                    drawLine(
                        color = branchColor,
                        start = Offset(midX, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Square
                    )
                }
            }
        }
    }
}
