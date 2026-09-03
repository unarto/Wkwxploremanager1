// [Jalur Class]: com.wakwau.xplore.filemanager.ui.detail.FileDetailInfoTab
// [Penjelasan]: Tab informasi berkas yang menampilkan metadata mendalam (jalur, nama, mime type, ukuran byte detail, tanggal ubah, aplikasi pembuka bawaan, dan daftar aplikasi yang kompatibel).
package com.wakwau.xplore.filemanager.ui.detail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.wakwau.xplore.core.storage.model.FileDetailedMetadata
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.ui.components.FileIcon
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.core.util.ByteFormatter
import com.wakwau.xplore.core.util.DateFormatter
import com.wakwau.xplore.core.util.MimeTypeDetector
import com.wakwau.xplore.filemanager.ui.R

// [Jalur Class]: com.wakwau.xplore.filemanager.ui.detail.FileDetailInfoTab
// [Penjelasan]: Tab informasi metadata berkas/folder dengan dukungan aksi ganti nama (rename) langsung pada baris nama berkas.
@Composable
fun FileDetailInfoTab(
    item: FileItem,
    metadata: FileDetailedMetadata?,
    modifier: Modifier = Modifier,
    onRenameClick: (FileItem) -> Unit = {},
    onRemoveLinkClick: ((FileItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = XPloreTheme.colors
    val clipboardManager = LocalClipboardManager.current

    val isDir = item.type == FileType.DIRECTORY
    val isSafRoot = isDir && item.location.path.startsWith("content://") && item.location.path == item.id

    val filePath = metadata?.fullPath ?: item.location.path
    val parentPath = metadata?.parentPath ?: if (filePath.contains('/')) filePath.substringBeforeLast('/', "") else ""
    val fileName = metadata?.fileName ?: item.name
    val mimeType = metadata?.mimeType ?: if (isDir) stringResource(R.string.label_directory_folder) else MimeTypeDetector.getMimeType(fileName)
    val sizeBytes = metadata?.sizeBytes ?: item.metadata.size
    val modifiedTime = metadata?.lastModifiedTimestamp ?: item.metadata.modifiedTime

    var isCompatibleAppsExpanded by remember { mutableStateOf(false) }
    var compatibleApps by remember { mutableStateOf<List<CompatibleAppInfo>>(emptyList()) }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath, mimeType) {
        if (!isDir) {
            val apps = AppIntentResolver.queryCompatibleApps(context, filePath, mimeType)
            compatibleApps = apps
            if (apps.isNotEmpty() && selectedAppPackage == null) {
                selectedAppPackage = apps.first().packageName
            }
        }
    }

    val copyToClipboard: (String, String) -> Unit = { text, label ->
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, context.getString(R.string.toast_copied_to_clipboard, label), Toast.LENGTH_SHORT).show()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Jalur
        FileDetailRow(
            label = stringResource(R.string.label_path_colon),
            value = parentPath,
            actionIcon = Icons.Default.ContentCopy,
            actionContentDescription = stringResource(R.string.cd_copy_value, stringResource(R.string.label_path_colon)),
            onActionClick = { copyToClipboard(parentPath, context.getString(R.string.label_path_colon)) }
        )

        // 2. Nama
        FileDetailRow(
            label = stringResource(R.string.label_name_colon),
            value = fileName,
            actionIcon = Icons.Default.Edit,
            actionContentDescription = stringResource(R.string.cd_rename),
            onActionClick = { onRenameClick(item) }
        )

        // 3. Mime type
        FileDetailRow(
            label = stringResource(R.string.label_mime_type_colon),
            value = mimeType,
            actionIcon = Icons.Default.Edit,
            actionContentDescription = stringResource(R.string.cd_edit_value, stringResource(R.string.label_mime_type_colon)),
            onActionClick = { copyToClipboard(mimeType, context.getString(R.string.label_mime_type_colon)) }
        )

        // 4. Ukuran
        val sizeFormatted = if (isDir) {
            ByteFormatter.format(sizeBytes)
        } else {
            ByteFormatter.formatDetailed(sizeBytes, stringResource(R.string.label_byte_unit))
        }
        FileDetailRow(
            label = stringResource(R.string.label_size_colon),
            value = sizeFormatted
        )

        // 5. Ubah tanggal
        FileDetailRow(
            label = stringResource(R.string.label_modify_date_colon),
            value = DateFormatter.format(modifiedTime),
            actionIcon = Icons.Default.Edit,
            actionContentDescription = stringResource(R.string.cd_edit_value, stringResource(R.string.label_modify_date_colon)),
            onActionClick = { copyToClipboard(DateFormatter.format(modifiedTime), context.getString(R.string.label_modify_date_colon)) }
        )

        // Hapus Tautan (SAF Root)
        if (isSafRoot && onRemoveLinkClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = { onRemoveLinkClick(item) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.cd_remove_link))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 6. Dibuka dengan (Non-direktori)
        if (!isDir) {
            val selectedApp = compatibleApps.find { it.packageName == selectedAppPackage } ?: compatibleApps.firstOrNull()
            val openWithLabel = selectedApp?.label ?: stringResource(R.string.label_default_app)

            FileDetailRow(
                label = stringResource(R.string.label_open_with_colon),
                value = openWithLabel,
                actionIcon = Icons.Default.Close,
                actionContentDescription = stringResource(R.string.cd_clear_app),
                onActionClick = { selectedAppPackage = null },
                leadingContent = {
                    val appBitmap = remember(selectedApp?.icon) {
                        try {
                            selectedApp?.icon?.toBitmap(48, 48)?.asImageBitmap()
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    if (appBitmap != null) {
                        Image(
                            bitmap = appBitmap,
                            contentDescription = selectedApp?.label,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        val category = MimeTypeDetector.getCategory(fileName, false)
                        FileIcon(category = category, isDirectory = false, size = 18.dp)
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 7. Aplikasi yang kompatibel Header (Expandable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCompatibleAppsExpanded = !isCompatibleAppsExpanded }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCompatibleAppsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.label_compatible_apps),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            AnimatedVisibility(visible = isCompatibleAppsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    if (compatibleApps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.msg_no_compatible_apps),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        compatibleApps.forEachIndexed { index, appInfo ->
                            val isSelected = appInfo.packageName == selectedAppPackage
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAppPackage = appInfo.packageName
                                        AppIntentResolver.openWithApp(
                                            context = context,
                                            filePath = filePath,
                                            mimeType = mimeType,
                                            packageName = appInfo.packageName,
                                            activityName = appInfo.activityName
                                        )
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_index_format, index + 1),
                                        color = colors.textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    val itemBitmap = remember(appInfo.icon) {
                                        try {
                                            appInfo.icon?.toBitmap(48, 48)?.asImageBitmap()
                                        } catch (_: Throwable) {
                                            null
                                        }
                                    }
                                    if (itemBitmap != null) {
                                        Image(
                                            bitmap = itemBitmap,
                                            contentDescription = appInfo.label,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        val category = MimeTypeDetector.getCategory(fileName, false)
                                        FileIcon(category = category, isDirectory = false, size = 20.dp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = appInfo.label,
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.primary else colors.textTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
