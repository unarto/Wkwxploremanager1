// [Jalur Class]: com.wakwau.xplore.filemanager.ui.list.FileListItem
// [Penjelasan]: Komponen perender baris berkas individual dengan penanda ikon bundar (indikator fokus/detail berkas) dan integrasi interaksi klik ikon untuk menampilkan dialog detail berkas.
package com.wakwau.xplore.filemanager.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.ui.components.FileIcon
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.core.util.ByteFormatter
import com.wakwau.xplore.core.util.DateFormatter
import com.wakwau.xplore.core.util.FileCategory
import com.wakwau.xplore.core.util.MimeTypeDetector
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.treeview.component.treeScopeBorder
import com.wakwau.xplore.treeview.model.BorderPosition
import com.wakwau.xplore.filemanager.ui.selection.FolderCheckCycleState

object FileListItemDefaults {
    val RowCornerRadius: Dp = 3.dp
    val ArrowBoxSize: Dp = 18.dp
    val ArrowIconSize: Dp = 11.dp
    val FileIconSize: Dp = 24.dp
    val CheckBoxSize: Dp = 28.dp
    val AllSelectedBoxSize: Dp = 19.dp
    val AllSelectedIconSize: Dp = 14.dp
    val SingleCheckIconSize: Dp = 18.dp
    val NameFontSize = 13.sp
    val MetadataFontSize = 10.5.sp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckToggle: () -> Unit,
    onIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    borderPosition: BorderPosition = BorderPosition.NONE,
    isPathSelected: Boolean = borderPosition != BorderPosition.NONE,
    showExpandArrow: Boolean = true,
    selectionState: FolderCheckCycleState = if (isSelected) FolderCheckCycleState.CHECKED else FolderCheckCycleState.UNCHECKED
) {
    val colors = XPloreTheme.colors
    val isDir = item.type == FileType.DIRECTORY
    val ext = item.name.substringAfterLast('.', "")
    val category = MimeTypeDetector.getCategory(item.name, isDir)
    val isArchive = category == FileCategory.ARCHIVE
    val hasExpandArrow = showExpandArrow && (isDir || isArchive)
    val isMarked = isSelected || selectionState != FolderCheckCycleState.UNCHECKED
    val isFocused = borderPosition != BorderPosition.NONE || isPathSelected

    val effectiveBorderPosition = when {
        borderPosition != BorderPosition.NONE -> borderPosition
        isPathSelected -> BorderPosition.SINGLE
        else -> BorderPosition.NONE
    }

    val rowBg = Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(rowBg)
            .treeScopeBorder(
                position = effectiveBorderPosition,
                borderColor = colors.folderSelectionColor,
                strokeWidth = 1.dp,
                cornerRadius = 3.dp
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand arrow or spacing
        if (hasExpandArrow) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = colors.treeExpandArrow,
                    modifier = Modifier.size(11.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onIconClick),
                contentAlignment = Alignment.Center
            ) {
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(1.5.dp, colors.treeExpandArrow, CircleShape)
                            .padding(2.dp)
                            .background(colors.primary, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(colors.textSecondary.copy(alpha = 0.6f), CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(3.dp))

        val isInternalStorage = com.wakwau.xplore.filemanager.ui.icon.StorageIconMapper.isInternalStorage(item)
        val isSearchRoot = item.id == com.wakwau.xplore.core.storage.constant.StorageConstants.VIRTUAL_SEARCH_ROOT_ID
        val isSearchResult = item.id.startsWith(com.wakwau.xplore.core.storage.constant.StorageConstants.SEARCH_RESULT_ID_PREFIX)

        // [Jalur Class]: com.wakwau.xplore.filemanager.ui.list.FileListItem
        // [Penjelasan]: Render ikon pencarian khusus untuk search root node dan ikon berkas biasa untuk item lainnya.
        if (isSearchRoot) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Search,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onIconClick() }
            )
        } else {
            FileIcon(
                category = category,
                isDirectory = isDir,
                isInternalStorage = isInternalStorage,
                extension = ext,
                size = 24.dp,
                modifier = Modifier.clickable { onIconClick() }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Name and Metadata (Date + Size)
        Column(modifier = Modifier.weight(1f)) {
            // [Jalur Class]: com.wakwau.xplore.filemanager.ui.list.FileListItem
            // [Penjelasan]: Lokalisasi judul hasil pencarian dengan format Hasil pencarian (N) dan menampilkan path folder asal berkas hasil pencarian sesuai antarmuka X-plore.
            val displayName = if (isSearchRoot) {
                if (item.name.contains("(") && item.name.contains(")")) {
                    val countStr = item.name.substringAfter("(").substringBefore(")")
                    val count = countStr.toIntOrNull() ?: 0
                    androidx.compose.ui.res.stringResource(com.wakwau.xplore.filemanager.ui.R.string.label_search_results_count, count)
                } else if (item.name.startsWith(com.wakwau.xplore.core.storage.constant.StorageConstants.SEARCH_RESULTS_PREFIX)) {
                    val keyword = item.name.removePrefix(com.wakwau.xplore.core.storage.constant.StorageConstants.SEARCH_RESULTS_PREFIX).removeSurrounding("'")
                    androidx.compose.ui.res.stringResource(com.wakwau.xplore.filemanager.ui.R.string.label_search_results_query, keyword)
                } else {
                    androidx.compose.ui.res.stringResource(com.wakwau.xplore.filemanager.ui.R.string.label_search_results)
                }
            } else {
                item.name
            }
            Text(
                text = displayName,
                color = if (isInternalStorage || isDir || isArchive) colors.treeExpandArrow else colors.textPrimary,
                fontWeight = if (isFocused || isDir || isArchive) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isSearchResult) {
                val parentDir = item.location.path.substringBeforeLast('/', "")
                val displayParent = if (parentDir.isNotEmpty()) "$parentDir/" else "/"
                Text(
                    text = displayParent,
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (!isSearchRoot) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormatter.formatShort(item.metadata.modifiedTime),
                        color = colors.textSecondary,
                        fontSize = 10.5.sp
                    )
                    if (!isDir) {
                        Text(
                            text = ByteFormatter.format(item.metadata.size),
                            color = colors.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Selection Checkmark button (3-State for folder, 2-State for file)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onCheckToggle),
            contentAlignment = Alignment.Center
        ) {
            when (selectionState) {
                FolderCheckCycleState.CHECKED -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.cd_selected),
                        tint = colors.checkmarkColor,
                        modifier = Modifier.size(FileListItemDefaults.SingleCheckIconSize)
                    )
                }
                FolderCheckCycleState.PARTIAL -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.cd_partially_selected),
                        tint = colors.checkmarkColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(FileListItemDefaults.SingleCheckIconSize)
                    )
                }
                FolderCheckCycleState.UNCHECKED -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.cd_unselected),
                        tint = colors.checkMarkUnchecked,
                        modifier = Modifier.size(FileListItemDefaults.SingleCheckIconSize)
                    )
                }
            }
        }
    }
}
