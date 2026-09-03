// [Jalur Class]: com.wakwau.xplore.core.ui.components.StorageDiskBar
// [Penjelasan]: Komponen UI composable untuk menampilkan status volume penyimpanan disk, kapasitas terpakai, dan visualisasi bar persentase.
package com.wakwau.xplore.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.ui.R
import com.wakwau.xplore.core.ui.theme.XPloreTheme

object StorageDiskBarDefaults {
    const val CRITICAL_USAGE_THRESHOLD = 0.9f
    const val HIGH_USAGE_THRESHOLD = 0.75f
    const val ROTATION_COLLAPSED_DEG = 0f
    const val ROTATION_EXPANDED_DEG = 90f
}

@Composable
fun StorageDiskBar(
    name: String,
    path: String,
    subFoldersCount: Int,
    subFilesCount: Int,
    freeSpaceText: String,
    totalSpaceText: String,
    usedPercentage: Float,
    isExternal: Boolean = false,
    isSelected: Boolean = true,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    val borderModifier = if (isSelected) {
        Modifier.border(BorderStroke(1.5.dp, colors.focusBorder), RoundedCornerShape(2.dp))
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .background(colors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand Toggle (Cyan/Secondary Triangle)
        Box(
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onToggleExpand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = if (isExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                tint = colors.treeExpandArrow,
                modifier = Modifier
                    .size(12.dp)
                    .rotate(if (isExpanded) StorageDiskBarDefaults.ROTATION_EXPANDED_DEG else StorageDiskBarDefaults.ROTATION_COLLAPSED_DEG)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Storage Device Icon
        Icon(
            imageVector = if (isExternal) Icons.Default.SdCard else Icons.Default.PhoneAndroid,
            contentDescription = stringResource(R.string.cd_storage),
            tint = if (isExternal) colors.primary else colors.secondary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Info (Name, folder/file count, path)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = colors.iconFolder,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " $subFoldersCount  ",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = colors.iconDoc,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " $subFilesCount  ",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = path,
                    fontSize = 10.5.sp,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Free / Total Space Meter
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_free_space) + " ",
                    fontSize = 9.5.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = stringResource(R.string.storage_ratio_format, freeSpaceText, totalSpaceText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(usedPercentage.coerceIn(0f, 1f))
                        .height(3.5.dp)
                        .background(
                            if (usedPercentage > StorageDiskBarDefaults.CRITICAL_USAGE_THRESHOLD) MaterialTheme.colorScheme.error
                            else if (usedPercentage > StorageDiskBarDefaults.HIGH_USAGE_THRESHOLD) colors.primary
                            else colors.secondary
                        )
                )
            }
        }
    }
}
