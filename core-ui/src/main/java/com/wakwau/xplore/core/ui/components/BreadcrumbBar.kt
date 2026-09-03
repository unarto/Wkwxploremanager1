package com.wakwau.xplore.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.ui.R
import com.wakwau.xplore.core.ui.theme.DarkBorder
import com.wakwau.xplore.core.ui.theme.DarkSurfaceElevated
import com.wakwau.xplore.core.ui.theme.DarkSurfaceHighlight
import com.wakwau.xplore.core.ui.theme.XploreBlue
import com.wakwau.xplore.core.ui.theme.XploreCyan
import com.wakwau.xplore.core.ui.theme.XPloreTheme

object BreadcrumbBarDefaults {
    val BarHeight: Dp = 36.dp
    val RootIconSize: Dp = 14.dp
    val ChevronIconSize: Dp = 15.dp
    val ItemCornerRadius: Dp = 4.dp
    val TextFontSize = 12.sp
}

data class BreadcrumbItem(
    val name: String,
    val path: String,
    val isRoot: Boolean = false
)

@Composable
fun BreadcrumbBar(
    items: List<BreadcrumbItem>,
    onItemClick: (BreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = XploreCyan
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val colors = XPloreTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BreadcrumbBarDefaults.BarHeight)
            .background(colors.topBarContainer)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val itemBackground = if (isLast) colors.primary else colors.surfaceElevated

            Row(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = BreadcrumbBarDefaults.ItemCornerRadius,
                            bottomStart = BreadcrumbBarDefaults.ItemCornerRadius,
                            topEnd = if (isLast) BreadcrumbBarDefaults.ItemCornerRadius else 0.dp,
                            bottomEnd = if (isLast) BreadcrumbBarDefaults.ItemCornerRadius else 0.dp
                        )
                    )
                    .background(itemBackground)
                    .clickable { onItemClick(item) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isRoot) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = stringResource(R.string.cd_root_directory),
                        tint = if (isLast) colors.onPrimary else colors.primary,
                        modifier = Modifier.size(BreadcrumbBarDefaults.RootIconSize)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = item.name,
                    fontSize = BreadcrumbBarDefaults.TextFontSize,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLast) colors.onPrimary else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(BreadcrumbBarDefaults.ChevronIconSize)
                )
            }
        }
    }
}

