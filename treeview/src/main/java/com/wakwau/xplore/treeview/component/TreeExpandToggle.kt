// [Jalur Class]: com.wakwau.xplore.treeview.component.TreeExpandToggle
// [Penjelasan]: Komponen toggle perluas/ciutkan panah pohon hierarki dengan animasi rotasi sudut halus dan aksesibilitas terpusat.
package com.wakwau.xplore.treeview.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakwau.xplore.core.ui.R
import com.wakwau.xplore.core.ui.theme.XPloreTheme

object TreeExpandToggleDefaults {
    val ContainerSize: Dp = 22.dp
    val CornerRadius: Dp = 4.dp
    val RippleRadius: Dp = 11.dp
    val ArrowIconSize: Dp = 13.dp
    const val ROTATION_EXPANDED = 90f
    const val ROTATION_COLLAPSED = 0f
    const val TRANSITION_LABEL = "tree_toggle_rotation"
}

@Composable
fun TreeExpandToggle(
    isExpanded: Boolean,
    isLeaf: Boolean = false,
    onToggle: () -> Unit,
    tint: Color = XPloreTheme.colors.treeExpandArrow,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(TreeExpandToggleDefaults.ContainerSize)
            .clip(RoundedCornerShape(TreeExpandToggleDefaults.CornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (!isLeaf) ripple(bounded = true, radius = TreeExpandToggleDefaults.RippleRadius) else null,
                enabled = !isLeaf,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isLeaf) {
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) TreeExpandToggleDefaults.ROTATION_EXPANDED else TreeExpandToggleDefaults.ROTATION_COLLAPSED,
                label = TreeExpandToggleDefaults.TRANSITION_LABEL
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = if (isExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                tint = tint,
                modifier = Modifier
                    .size(TreeExpandToggleDefaults.ArrowIconSize)
                    .rotate(rotation)
            )
        }
    }
}
