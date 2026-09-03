package com.wakwau.xplore.treeview.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakwau.xplore.treeview.model.BorderPosition

fun Modifier.treeScopeBorder(
    position: BorderPosition,
    borderColor: Color,
    strokeWidth: Dp = 1.dp,
    cornerRadius: Dp = 3.dp
): Modifier = if (position == BorderPosition.NONE) this else this.drawBehind {
    val stroke = strokeWidth.toPx()
    val halfStroke = stroke / 2f
    val radius = cornerRadius.toPx()
    val w = size.width
    val h = size.height

    when (position) {
        BorderPosition.SINGLE -> {
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(halfStroke, halfStroke),
                size = Size(w - stroke, h - stroke),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = stroke)
            )
        }
        BorderPosition.TOP -> {
            val path = Path().apply {
                moveTo(halfStroke, h)
                lineTo(halfStroke, halfStroke + radius)
                arcTo(
                    rect = Rect(halfStroke, halfStroke, halfStroke + radius * 2, halfStroke + radius * 2),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(w - halfStroke - radius, halfStroke)
                arcTo(
                    rect = Rect(w - halfStroke - radius * 2, halfStroke, w - halfStroke, halfStroke + radius * 2),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(w - halfStroke, h)
            }
            drawPath(path = path, color = borderColor, style = Stroke(width = stroke))
        }
        BorderPosition.MIDDLE -> {
            drawLine(
                color = borderColor,
                start = Offset(halfStroke, 0f),
                end = Offset(halfStroke, h),
                strokeWidth = stroke
            )
            drawLine(
                color = borderColor,
                start = Offset(w - halfStroke, 0f),
                end = Offset(w - halfStroke, h),
                strokeWidth = stroke
            )
        }
        BorderPosition.BOTTOM -> {
            val path = Path().apply {
                moveTo(halfStroke, 0f)
                lineTo(halfStroke, h - halfStroke - radius)
                arcTo(
                    rect = Rect(halfStroke, h - halfStroke - radius * 2, halfStroke + radius * 2, h - halfStroke),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(w - halfStroke - radius, h - halfStroke)
                arcTo(
                    rect = Rect(w - halfStroke - radius * 2, h - halfStroke - radius * 2, w - halfStroke, h - halfStroke),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(w - halfStroke, 0f)
            }
            drawPath(path = path, color = borderColor, style = Stroke(width = stroke))
        }
        BorderPosition.NONE -> {}
    }
}
