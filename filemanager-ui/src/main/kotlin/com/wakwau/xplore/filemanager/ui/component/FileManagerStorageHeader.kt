// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/component/FileManagerStorageHeader.kt
// [Penjelasan]: Menampilkan header informasi storage dan kapasitas disk menggunakan StorageConstants untuk pengecekan rootId tanpa hardcoded strings.
package com.wakwau.xplore.filemanager.ui.component

import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.ui.components.StorageDiskBar
import com.wakwau.xplore.core.util.ByteFormatter
import com.wakwau.xplore.filemanager.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FileManagerStorageHeader(
    location: StorageLocation?,
    subFoldersCount: Int = 0,
    subFilesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val isSdCard = location?.rootId == StorageConstants.SDCARD_STORAGE_ID
    val isRoot = location?.rootId == StorageConstants.ROOT_STORAGE_ID || location?.rootId == StorageConstants.ROOT_LEGACY_ID

    val diskName = when {
        isSdCard -> stringResource(R.string.label_sd_card)
        isRoot -> stringResource(R.string.label_root)
        else -> stringResource(R.string.label_internal_shared_storage)
    }

    val pathDisplay = location?.path?.ifEmpty { Environment.getExternalStorageDirectory().absolutePath } ?: Environment.getExternalStorageDirectory().absolutePath

    var freeSpaceText by remember { mutableStateOf("...") }
    var totalSpaceText by remember { mutableStateOf("...") }
    var usedPercentage by remember { mutableStateOf(0f) }
    
    LaunchedEffect(pathDisplay) {
        withContext(Dispatchers.IO) {
            try {
                val statFs = StatFs(pathDisplay)
                val blockSize = statFs.blockSizeLong
                val totalBlocks = statFs.blockCountLong
                val availableBlocks = statFs.availableBlocksLong
                
                val totalBytes = totalBlocks * blockSize
                val freeBytes = availableBlocks * blockSize
                val usedBytes = totalBytes - freeBytes
                
                freeSpaceText = ByteFormatter.format(freeBytes)
                totalSpaceText = ByteFormatter.format(totalBytes)
                usedPercentage = if (totalBytes > 0) {
                    (usedBytes.toFloat() / totalBytes.toFloat())
                } else {
                    0f
                }
            } catch (e: IllegalArgumentException) {
                // Ignore or handle invalid path
            }
        }
    }

    StorageDiskBar(
        name = diskName,
        path = pathDisplay,
        subFoldersCount = subFoldersCount,
        subFilesCount = subFilesCount,
        freeSpaceText = freeSpaceText,
        totalSpaceText = totalSpaceText,
        usedPercentage = usedPercentage,
        isExternal = isSdCard,
        isSelected = true,
        isExpanded = true,
        modifier = modifier
    )
}




