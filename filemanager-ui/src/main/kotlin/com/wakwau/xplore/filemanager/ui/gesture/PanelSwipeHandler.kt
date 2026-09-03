package com.wakwau.xplore.filemanager.ui.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

object PanelSwipeDefaults {
    const val DEFAULT_SWIPE_THRESHOLD_PX = 70f
    const val MIN_TOUCH_SLOP_PX = 15f
    const val HORIZONTAL_RATIO_MULTIPLIER = 1.3f
}

// [Jalur Class]: com.wakwau.xplore.filemanager.ui.gesture.PanelSwipeHandler
// [Penjelasan]: Modifier gesture deteksi swipe kiri/kanan antar panel dengan konstanta threshold terpusat.
fun Modifier.onPanelSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    threshold: Float = PanelSwipeDefaults.DEFAULT_SWIPE_THRESHOLD_PX
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalDragX = 0f
        var totalDragY = 0f
        var isHorizontal: Boolean? = null

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break

            if (!change.pressed) {
                if (isHorizontal == true) {
                    if (totalDragX < -threshold) {
                        onSwipeLeft()
                    } else if (totalDragX > threshold) {
                        onSwipeRight()
                    }
                }
                break
            }

            val dragX = change.position.x - change.previousPosition.x
            val dragY = change.position.y - change.previousPosition.y

            totalDragX += dragX
            totalDragY += dragY

            if (isHorizontal == null) {
                if (abs(totalDragX) > PanelSwipeDefaults.MIN_TOUCH_SLOP_PX || abs(totalDragY) > PanelSwipeDefaults.MIN_TOUCH_SLOP_PX) {
                    if (abs(totalDragX) > abs(totalDragY) * PanelSwipeDefaults.HORIZONTAL_RATIO_MULTIPLIER) {
                        isHorizontal = true
                        change.consume()
                    } else {
                        isHorizontal = false
                    }
                }
            } else if (isHorizontal == true) {
                change.consume()
            }
        }
    }
}

