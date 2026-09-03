// [Jalur Class]: com.wakwau.xplore.filemanager.ui.detail.FileDetailDialog
// [Penjelasan]: Dialog popup overlay rincian berkas dengan tab bar switchable (Tab Berkas dan Tab Checksum SHA MD5) serta integrasi aksi ganti nama berkas.
package com.wakwau.xplore.filemanager.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.filemanager.ui.state.FileDetailState

@Composable
fun FileDetailDialog(
    state: FileDetailState,
    onDismiss: () -> Unit,
    onCalculateChecksum: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    onRenameClick: (FileItem) -> Unit = {},
    onRemoveLinkClick: ((FileItem) -> Unit)? = null
) {
    if (!state.isVisible || state.selectedItem == null) return
    val item = state.selectedItem
    val colors = XPloreTheme.colors
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val isDir = item.type == FileType.DIRECTORY

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .heightIn(max = 600.dp)
                .border(1.dp, colors.treeLineColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar: Tabs + Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated)
                        .border(width = 0.5.dp, color = colors.treeLineColor.copy(alpha = 0.3f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: File
                    DetailTabButton(
                        title = stringResource(R.string.tab_file),
                        icon = Icons.Default.Description,
                        isSelected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    // Tab 2: SHA MD5 (Only for files)
                    if (!isDir) {
                        DetailTabButton(
                            title = stringResource(R.string.tab_sha_md5),
                            icon = Icons.Default.Functions,
                            isSelected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Close Action Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Tab Body Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            FileDetailInfoTab(
                                item = item,
                                metadata = state.metadata,
                                onRenameClick = onRenameClick,
                                onRemoveLinkClick = onRemoveLinkClick
                            )
                        }
                        1 -> {
                            FileDetailChecksumTab(
                                item = item,
                                checksumState = state.checksumState,
                                onCalculateChecksum = onCalculateChecksum
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors
    val tabBg = if (isSelected) colors.surface else Color.Transparent
    val tabBorderColor = if (isSelected) colors.treeLineColor.copy(alpha = 0.6f) else Color.Transparent

    Box(
        modifier = modifier
            .padding(top = 4.dp, start = 4.dp, end = 4.dp)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(tabBg)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = tabBorderColor,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) colors.primary else colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = title,
                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
