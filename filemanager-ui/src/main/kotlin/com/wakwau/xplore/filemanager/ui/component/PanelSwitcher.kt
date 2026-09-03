package com.wakwau.xplore.filemanager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.state.PanelId

@Composable
fun PanelSwitcher(
    activePanelId: PanelId,
    onSelectPanel: (PanelId) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(colors.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PanelTab(
            title = stringResource(R.string.label_left_panel),
            isActive = activePanelId == PanelId.LEFT,
            activeColor = colors.secondary,
            onClick = { onSelectPanel(PanelId.LEFT) },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        PanelTab(
            title = stringResource(R.string.label_right_panel),
            isActive = activePanelId == PanelId.RIGHT,
            activeColor = colors.primary,
            onClick = { onSelectPanel(PanelId.RIGHT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PanelTab(
    title: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors
    val bg = if (isActive) activeColor.copy(alpha = 0.22f) else colors.surfaceElevated
    val textColor = if (isActive) activeColor else colors.textSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}
