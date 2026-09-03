// [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/action/PanelNavigationHandlerTest.kt
// [Penjelasan]: Unit test untuk PanelNavigationHandler yang memverifikasi alur Up Dir dari subfolder ke parent, batas root volume, dan independensi panel kiri-kanan.
package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.filemanager.ui.FakeAppPreferencesRepository
import com.wakwau.xplore.filemanager.ui.FakeDirectoryRepository
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.state.PanelState
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter
import com.wakwau.xplore.filemanager.usecase.GetParentLocationUseCase
import com.wakwau.xplore.filemanager.usecase.ListDirectoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PanelNavigationHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDirectoryRepository: FakeDirectoryRepository
    private lateinit var fakeAppPreferencesRepository: FakeAppPreferencesRepository
    private lateinit var listDirectoryUseCase: ListDirectoryUseCase
    private lateinit var treeNavigationAdapter: TreeNavigationAdapter
    private lateinit var getParentLocationUseCase: GetParentLocationUseCase
    private val dispatchedEvents = mutableListOf<DualPaneEvent>()

    private lateinit var handler: PanelNavigationHandler

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDirectoryRepository = FakeDirectoryRepository()
        fakeAppPreferencesRepository = FakeAppPreferencesRepository()
        listDirectoryUseCase = ListDirectoryUseCase(fakeDirectoryRepository, fakeAppPreferencesRepository)
        treeNavigationAdapter = TreeNavigationAdapter(listDirectoryUseCase, fakeAppPreferencesRepository)
        getParentLocationUseCase = GetParentLocationUseCase()
        dispatchedEvents.clear()

        handler = PanelNavigationHandler(
            treeNavigationAdapter = treeNavigationAdapter,
            getParentLocationUseCase = getParentLocationUseCase,
            dispatch = { dispatchedEvents.add(it) }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleNavigateUp navigates from subfolder to parent folder and dispatches OpenLocation`() = runTest {
        val rootPath = "/storage/emulated/0"
        val volume = StorageVolumeItem("primary", "Internal Storage", rootPath, StorageVolumeType.PRIMARY_INTERNAL, false, null)
        val docPath = "/storage/emulated/0/Documents"
        val docItem = FileItem(docPath, "Documents", StorageLocation(docPath, "primary"), FileType.DIRECTORY, FileMetadata.EMPTY)

        fakeDirectoryRepository.setFiles(StorageLocation(rootPath, "primary"), listOf(docItem))

        treeNavigationAdapter.loadVolumesAsRoots(PanelId.LEFT, listOf(volume))
        val leftEngine = treeNavigationAdapter.getEngine(PanelId.LEFT)
        val rootNode = leftEngine.treeState.roots[0]
        leftEngine.toggleNode(rootNode) // expand root

        // Select subfolder Documents
        treeNavigationAdapter.setSelectedPath(PanelId.LEFT, docPath)

        val state = DualPaneState(
            leftPanel = PanelState(id = PanelId.LEFT, currentLocation = StorageLocation(docPath, "primary")),
            rightPanel = PanelState(id = PanelId.RIGHT, currentLocation = StorageLocation(rootPath, "primary")),
            activePanelId = PanelId.LEFT
        )

        handler.handleNavigateUp(state, PanelId.LEFT)

        assertEquals(1, dispatchedEvents.size)
        val event = dispatchedEvents[0] as DualPaneEvent.OpenLocation
        assertEquals(PanelId.LEFT, event.panelId)
        assertEquals(rootPath, event.location.path)
        assertEquals(rootPath, treeNavigationAdapter.getSelectedPath(PanelId.LEFT).value)
    }

    @Test
    fun `handleNavigateUp maintains independence between Left and Right panels`() = runTest {
        val rootPath = "/storage/emulated/0"
        val volume = StorageVolumeItem("primary", "Internal Storage", rootPath, StorageVolumeType.PRIMARY_INTERNAL, false, null)
        val leftSubPath = "/storage/emulated/0/Music"
        val rightSubPath = "/storage/emulated/0/Pictures"
        val musicItem = FileItem(leftSubPath, "Music", StorageLocation(leftSubPath, "primary"), FileType.DIRECTORY, FileMetadata.EMPTY)
        val picItem = FileItem(rightSubPath, "Pictures", StorageLocation(rightSubPath, "primary"), FileType.DIRECTORY, FileMetadata.EMPTY)

        fakeDirectoryRepository.setFiles(StorageLocation(rootPath, "primary"), listOf(musicItem, picItem))

        treeNavigationAdapter.loadVolumesAsRoots(PanelId.LEFT, listOf(volume))
        treeNavigationAdapter.loadVolumesAsRoots(PanelId.RIGHT, listOf(volume))

        val leftEngine = treeNavigationAdapter.getEngine(PanelId.LEFT)
        leftEngine.toggleNode(leftEngine.treeState.roots[0])
        val rightEngine = treeNavigationAdapter.getEngine(PanelId.RIGHT)
        rightEngine.toggleNode(rightEngine.treeState.roots[0])

        treeNavigationAdapter.setSelectedPath(PanelId.LEFT, leftSubPath)
        treeNavigationAdapter.setSelectedPath(PanelId.RIGHT, rightSubPath)

        val state = DualPaneState(
            leftPanel = PanelState(id = PanelId.LEFT, currentLocation = StorageLocation(leftSubPath, "primary")),
            rightPanel = PanelState(id = PanelId.RIGHT, currentLocation = StorageLocation(rightSubPath, "primary")),
            activePanelId = PanelId.RIGHT
        )

        // Navigate Up on Right Panel ONLY
        handler.handleNavigateUp(state, PanelId.RIGHT)

        assertEquals(1, dispatchedEvents.size)
        val event = dispatchedEvents[0] as DualPaneEvent.OpenLocation
        assertEquals(PanelId.RIGHT, event.panelId)
        assertEquals(rootPath, event.location.path)

        // Right panel selectedPath changed to root, Left panel selectedPath remains Music
        assertEquals(rootPath, treeNavigationAdapter.getSelectedPath(PanelId.RIGHT).value)
        assertEquals(leftSubPath, treeNavigationAdapter.getSelectedPath(PanelId.LEFT).value)
    }

    @Test
    fun `handleNavigateUp respects root boundary and stays at root`() = runTest {
        val rootPath = "/storage/emulated/0"
        val volume = StorageVolumeItem("primary", "Internal Storage", rootPath, StorageVolumeType.PRIMARY_INTERNAL, false, null)

        treeNavigationAdapter.loadVolumesAsRoots(PanelId.LEFT, listOf(volume))
        treeNavigationAdapter.setSelectedPath(PanelId.LEFT, rootPath)

        val state = DualPaneState(
            leftPanel = PanelState(id = PanelId.LEFT, currentLocation = StorageLocation(rootPath, "primary")),
            rightPanel = PanelState(id = PanelId.RIGHT),
            activePanelId = PanelId.LEFT
        )

        handler.handleNavigateUp(state, PanelId.LEFT)

        assertEquals(1, dispatchedEvents.size)
        val event = dispatchedEvents[0] as DualPaneEvent.OpenLocation
        assertEquals(PanelId.LEFT, event.panelId)
        assertEquals(rootPath, event.location.path)
        assertEquals(rootPath, treeNavigationAdapter.getSelectedPath(PanelId.LEFT).value)
    }
}
