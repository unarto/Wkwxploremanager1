// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/state/FileDialogUiState.kt
// [Penjelasan]: State representasi UI dialog input berkas (pembuatan folder baru, ganti nama, konfirmasi hapus) yang bersifat immutable dan murni MVI.
package com.wakwau.xplore.filemanager.ui.state

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation

sealed class FileDialogUiState {
    object None : FileDialogUiState()
    data class CreateDirectory(val parentLocation: StorageLocation) : FileDialogUiState()
    data class RenameItem(val item: FileItem) : FileDialogUiState()
    data class DeleteConfirmation(val items: List<FileItem>) : FileDialogUiState()
}
