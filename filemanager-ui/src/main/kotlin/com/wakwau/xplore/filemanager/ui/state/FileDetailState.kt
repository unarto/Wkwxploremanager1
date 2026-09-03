package com.wakwau.xplore.filemanager.ui.state

import com.wakwau.xplore.core.storage.model.FileDetailedMetadata
import com.wakwau.xplore.core.storage.model.FileItem

// [Jalur Class]: com.wakwau.xplore.filemanager.ui.state.FileDetailState
// [Penjelasan]: State UI immutable yang menyimpan data tampilan bottom sheet metadata berkas, termasuk status kalkulasi checksum.
data class FileDetailState(
    val isVisible: Boolean = false,
    val selectedItem: FileItem? = null,
    val metadata: FileDetailedMetadata? = null,
    val isLoadingMetadata: Boolean = false,
    val errorRes: Int? = null,
    val errorMessage: String? = null,
    val checksumState: ChecksumState = ChecksumState.Idle
)
