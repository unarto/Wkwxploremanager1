// [Jalur Class]: com.wakwau.xplore.filemanager.ui.screen.DualPaneFileManagerScreen
// [Penjelasan]: Menggunakan TreeNavigationAdapter untuk menginisialisasi storage volume roots dan menyinkronkan lokasi awal panel ke ViewModel.
package com.wakwau.xplore.filemanager.ui.screen

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.presentation.DualPaneViewModel
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter

@Composable
fun DualPaneFileManagerScreen(
    viewModel: DualPaneViewModel,
    treeAdapter: TreeNavigationAdapter,
    storageVolumes: List<StorageVolumeItem>,
    onSettingsClick: () -> Unit = {},
    onLinkStorageClick: () -> Unit = {},
    onRemoveLinkClick: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // [Jalur Class]: com.wakwau.xplore.filemanager.ui.screen.DualPaneFileManagerScreen
    // [Penjelasan]: Menginisialisasi atau memperbarui volume root pada TreeNavigationAdapter saat daftar storageVolumes berubah dan menyinkronkan currentLocation awal ke ViewModel jika belum diset.
    LaunchedEffect(storageVolumes) {
        if (storageVolumes.isNotEmpty()) {
            val hadLeftRoots = treeAdapter.hasRoots(PanelId.LEFT)
            val hadRightRoots = treeAdapter.hasRoots(PanelId.RIGHT)

            treeAdapter.loadVolumesAsRoots(PanelId.LEFT, storageVolumes)
            treeAdapter.loadVolumesAsRoots(PanelId.RIGHT, storageVolumes)

            if (!hadLeftRoots) {
                storageVolumes.firstOrNull()?.let { volume ->
                    val loc = StorageLocation(volume.rootPath, volume.id)
                    viewModel.dispatch(DualPaneEvent.OpenLocation(PanelId.LEFT, loc))
                }
            }
            if (!hadRightRoots) {
                storageVolumes.firstOrNull()?.let { volume ->
                    val loc = StorageLocation(volume.rootPath, volume.id)
                    viewModel.dispatch(DualPaneEvent.OpenLocation(PanelId.RIGHT, loc))
                }
            }
        }
    }

    FileManagerScreen(
        viewModel = viewModel,
        treeAdapter = treeAdapter,
        onSettingsClick = onSettingsClick,
        onLinkStorageClick = onLinkStorageClick,
        onRemoveLinkClick = onRemoveLinkClick,
        modifier = modifier
    )
}
