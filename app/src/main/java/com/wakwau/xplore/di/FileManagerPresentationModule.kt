// [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/di/FileManagerPresentationModule.kt
// [Penjelasan]: Menyediakan TreeNavigationAdapter dan mengonstruksi PanelNavigationHandler dengan GetParentLocationUseCase.
package com.wakwau.xplore.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wakwau.xplore.filemanager.ui.action.CopyOperationHandler
import com.wakwau.xplore.filemanager.ui.action.CreateDirectoryOperationHandler
import com.wakwau.xplore.filemanager.ui.action.DeleteOperationHandler
import com.wakwau.xplore.filemanager.ui.action.FileDetailHandler
import com.wakwau.xplore.filemanager.ui.action.MoveOperationHandler
import com.wakwau.xplore.filemanager.ui.action.PanelNavigationHandler
import com.wakwau.xplore.filemanager.ui.action.PanelRefreshHandler
import com.wakwau.xplore.filemanager.ui.action.RenameOperationHandler
import com.wakwau.xplore.filemanager.ui.action.SearchOperationHandler
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.presentation.DualPaneViewModel
import com.wakwau.xplore.filemanager.ui.presentation.DualPaneViewModelFactory
import com.wakwau.xplore.filemanager.ui.reducer.DualPaneReducer
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter

import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository

import com.wakwau.xplore.core.storage.operation.BackgroundOperationManager

class FileManagerPresentationModule(
    private val useCaseModule: FileManagerUseCaseModule,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val backgroundOperationManager: BackgroundOperationManager
) {
    private var activeDispatcher: ((DualPaneEvent) -> Unit)? = null
    private val dispatchBridge: (DualPaneEvent) -> Unit = { event ->
        activeDispatcher?.invoke(event)
    }

    val dualPaneReducer: DualPaneReducer by lazy { DualPaneReducer() }

    val panelRefreshHandler: PanelRefreshHandler by lazy {
        PanelRefreshHandler(useCaseModule.listDirectoryUseCase, dispatchBridge)
    }

    val panelNavigationHandler: PanelNavigationHandler by lazy {
        PanelNavigationHandler(
            treeNavigationAdapter = treeNavigationAdapter,
            getParentLocationUseCase = useCaseModule.getParentLocationUseCase,
            dispatch = dispatchBridge
        )
    }

    val copyOperationHandler: CopyOperationHandler by lazy {
        CopyOperationHandler(
            copyFilesUseCase = useCaseModule.copyFilesUseCase,
            detectConflictsUseCase = useCaseModule.detectConflictsUseCase,
            resolveTransferUseCase = useCaseModule.resolveTransferUseCase,
            dispatch = dispatchBridge
        )
    }

    val moveOperationHandler: MoveOperationHandler by lazy {
        MoveOperationHandler(
            moveFilesUseCase = useCaseModule.moveFilesUseCase,
            detectConflictsUseCase = useCaseModule.detectConflictsUseCase,
            resolveTransferUseCase = useCaseModule.resolveTransferUseCase,
            dispatch = dispatchBridge
        )
    }

    val deleteOperationHandler: DeleteOperationHandler by lazy {
        DeleteOperationHandler(useCaseModule.deleteFilesUseCase, dispatchBridge)
    }

    val renameOperationHandler: RenameOperationHandler by lazy {
        RenameOperationHandler(useCaseModule.renameFileUseCase, dispatchBridge)
    }

    val createDirectoryOperationHandler: CreateDirectoryOperationHandler by lazy {
        CreateDirectoryOperationHandler(useCaseModule.createDirectoryUseCase, dispatchBridge)
    }

    val fileDetailHandler: FileDetailHandler by lazy {
        FileDetailHandler(
            getFileDetailedMetadataUseCase = useCaseModule.getFileDetailedMetadataUseCase,
            computeFileChecksumUseCase = useCaseModule.computeFileChecksumUseCase,
            dispatch = dispatchBridge
        )
    }

    val searchOperationHandler: SearchOperationHandler by lazy {
        SearchOperationHandler(useCaseModule.searchFilesUseCase, dispatchBridge)
    }

    val treeNavigationAdapter: TreeNavigationAdapter by lazy {
        TreeNavigationAdapter(
            listDirectoryUseCase = useCaseModule.listDirectoryUseCase,
            appPreferencesRepository = appPreferencesRepository,
            fileTreeItemFactory = useCaseModule.fileTreeItemFactory
        )
    }

    fun createViewModelFactory(): ViewModelProvider.Factory {
        val baseFactory = DualPaneViewModelFactory(
            reducer = dualPaneReducer,
            refreshHandler = panelRefreshHandler,
            navigationHandler = panelNavigationHandler,
            copyHandler = copyOperationHandler,
            moveHandler = moveOperationHandler,
            deleteHandler = deleteOperationHandler,
            renameHandler = renameOperationHandler,
            createDirectoryHandler = createDirectoryOperationHandler,
            fileDetailHandler = fileDetailHandler,
            searchOperationHandler = searchOperationHandler,
            appPreferencesRepository = appPreferencesRepository,
            treeNavigationAdapter = treeNavigationAdapter,
            toggleShowHiddenFilesUseCase = useCaseModule.toggleShowHiddenFilesUseCase,
            backgroundOperationManager = backgroundOperationManager,
            checkStoragePermissionUseCase = useCaseModule.checkStoragePermissionUseCase,
            getStorageVolumesUseCase = useCaseModule.getStorageVolumesUseCase,
            linkStorageUseCase = useCaseModule.linkStorageUseCase
        )
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val vm = baseFactory.create(modelClass)
                if (vm is DualPaneViewModel) {
                    activeDispatcher = vm::dispatch
                }
                return vm
            }
        }
    }
}
