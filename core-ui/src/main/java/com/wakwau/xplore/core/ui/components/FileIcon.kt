package com.wakwau.xplore.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
// [Jalur Class/Modul]: core-ui/src/main/java/com/wakwau/xplore/core/ui/components/FileIcon.kt
// [Penjelasan]: Menggunakan Icons.AutoMirrored.Filled.InsertDriveFile untuk kompatibilitas tampilan RTL.
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.ui.R
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.core.util.FileCategory

object FileIconDefaults {
    val DefaultSize: Dp = 28.dp
    val BadgeCornerRadius: Dp = 2.dp
    val BadgeFontSize = 6.sp
}

@Composable
fun FileIcon(
    category: FileCategory,
    isDirectory: Boolean,
    isExpanded: Boolean = false,
    isInternalStorage: Boolean = false,
    extension: String = "",
    size: Dp = FileIconDefaults.DefaultSize,
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when {
            isInternalStorage -> {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = stringResource(R.string.cd_icon_storage),
                    tint = colors.primary,
                    modifier = Modifier.size(size)
                )
            }
            isDirectory -> {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = stringResource(R.string.cd_icon_folder),
                    tint = colors.iconFolder,
                    modifier = Modifier.size(size)
                )
            }
            category == FileCategory.ARCHIVE -> {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = stringResource(R.string.cd_icon_archive),
                        tint = colors.iconArchive,
                        modifier = Modifier.size(size)
                    )
                    // Small ZIP badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(FileIconDefaults.BadgeCornerRadius))
                            .background(colors.iconArchive.copy(alpha = 0.9f))
                    ) {
                        Text(
                            text = stringResource(R.string.badge_zip),
                            color = colors.onPrimary,
                            fontSize = FileIconDefaults.BadgeFontSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            category == FileCategory.APK -> {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = stringResource(R.string.cd_icon_apk),
                    tint = colors.iconApk,
                    modifier = Modifier.size(size)
                )
            }
            category == FileCategory.IMAGE -> {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = stringResource(R.string.cd_icon_image),
                    tint = colors.iconImage,
                    modifier = Modifier.size(size)
                )
            }
            category == FileCategory.AUDIO -> {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = stringResource(R.string.cd_icon_audio),
                    tint = colors.iconAudio,
                    modifier = Modifier.size(size)
                )
            }
            category == FileCategory.VIDEO -> {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = stringResource(R.string.cd_icon_video),
                    tint = colors.iconVideo,
                    modifier = Modifier.size(size)
                )
            }
            category == FileCategory.CODE -> {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = stringResource(R.string.cd_icon_code),
                    tint = colors.iconCode,
                    modifier = Modifier.size(size)
                )
            }
            category == FileCategory.TEXT || category == FileCategory.DOCUMENT -> {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = stringResource(R.string.cd_icon_document),
                    tint = colors.iconDoc,
                    modifier = Modifier.size(size)
                )
            }
            else -> {
                Icon(
                    // [Jalur Class/Modul]: core-ui/src/main/java/com/wakwau/xplore/core/ui/components/FileIcon.kt
                    // [Penjelasan]: Menggunakan AutoMirrored InsertDriveFile untuk render ikon berkas umum.
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = stringResource(R.string.cd_icon_file),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}
