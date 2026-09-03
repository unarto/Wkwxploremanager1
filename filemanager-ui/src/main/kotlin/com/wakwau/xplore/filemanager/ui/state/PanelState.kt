package com.wakwau.xplore.filemanager.ui.state

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation

data class PanelState(
    val id: PanelId,
    val currentLocation: StorageLocation? = null,
    val items: List<FileItem> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)
