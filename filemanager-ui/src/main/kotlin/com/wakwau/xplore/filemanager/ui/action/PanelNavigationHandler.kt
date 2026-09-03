// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/PanelNavigationHandler.kt
// [Penjelasan]: Menangani aksi navigasi naik satu level direktori (Up Dir) dengan mendelegasikan navigasi ke TreeNavigationAdapter dan menyinkronkan StorageLocation ke ViewModel melalui DualPaneEvent.OpenLocation tanpa manipulasi path java.io langsung di presentation layer.
package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter
import com.wakwau.xplore.filemanager.usecase.GetParentLocationUseCase

class PanelNavigationHandler(
    private val treeNavigationAdapter: TreeNavigationAdapter,
    private val getParentLocationUseCase: GetParentLocationUseCase,
    private val dispatch: (DualPaneEvent) -> Unit
) {
    fun handleNavigateUp(state: DualPaneState, panelId: PanelId) {
        val targetLocation: StorageLocation? = treeNavigationAdapter.navigateUp(panelId)
            ?: run {
                val panel = if (panelId == PanelId.LEFT) state.leftPanel else state.rightPanel
                panel.currentLocation?.let { getParentLocationUseCase(it) }
            }

        if (targetLocation != null) {
            dispatch(DualPaneEvent.OpenLocation(panelId, targetLocation))
        }
    }
}
