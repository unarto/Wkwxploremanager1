// [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngineUpDirCollapseTest.kt
// [Penjelasan]: Regression tests untuk memverifikasi bahwa navigasi Up Dir secara otomatis meng-collapse folder yang ditinggalkan sesuai perilaku X-plore, menjaga konsistensi state ekspansi pohon, tidak merusak ekspansi branch lain, dan mempertahankan independensi panel kiri dan kanan.
package com.wakwau.xplore.filemanager.ui.tree

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.filemanager.ui.FakeAppPreferencesRepository
import com.wakwau.xplore.filemanager.ui.FakeDirectoryRepository
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.usecase.ListDirectoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileTreeEngineUpDirCollapseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDirectoryRepository: FakeDirectoryRepository
    private lateinit var fakeAppPreferencesRepository: FakeAppPreferencesRepository
    private lateinit var listDirectoryUseCase: ListDirectoryUseCase
    private lateinit var treeAdapter: TreeNavigationAdapter
    private lateinit var engine: FileTreeEngine

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDirectoryRepository = FakeDirectoryRepository()
        fakeAppPreferencesRepository = FakeAppPreferencesRepository()
        listDirectoryUseCase = ListDirectoryUseCase(fakeDirectoryRepository, fakeAppPreferencesRepository)
        treeAdapter = TreeNavigationAdapter(listDirectoryUseCase, fakeAppPreferencesRepository)
        engine = FileTreeEngine(listDirectoryUseCase, fakeAppPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createFileItem(path: String, type: FileType): FileItem {
        return FileItem(
            id = path,
            location = StorageLocation(path, "primary"),
            name = path.substringAfterLast("/").ifEmpty { "/" },
            type = type,
            metadata = FileMetadata.EMPTY
        )
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngineUpDirCollapseTest.kt
    // [Penjelasan]: Skenario 1 - Root -> Folder A -> Folder B -> Up Dir -> Folder B di-collapse, lokasi aktif Folder A, dan fokus node pada Folder A.
    @Test
    fun `test Up Dir collapses abandoned folder B when navigating back to folder A`() = runTest {
        val rootItem = createFileItem("/storage/emulated/0", FileType.DIRECTORY)
        val folderA = createFileItem("/storage/emulated/0/FolderA", FileType.DIRECTORY)
        val folderB = createFileItem("/storage/emulated/0/FolderA/FolderB", FileType.DIRECTORY)
        val fileB = createFileItem("/storage/emulated/0/FolderA/FolderB/fileB.txt", FileType.FILE)

        fakeDirectoryRepository.setFiles(rootItem.location, listOf(folderA))
        fakeDirectoryRepository.setFiles(folderA.location, listOf(folderB))
        fakeDirectoryRepository.setFiles(folderB.location, listOf(fileB))

        engine.loadRoot(rootItem)

        // Expand Root -> Folder A -> Folder B
        val rootNode = engine.treeState.roots[0]
        val nodeA = rootNode.children[0]
        engine.toggleNode(nodeA) // expands Folder A
        val nodeB = nodeA.children[0]
        engine.toggleNode(nodeB) // expands Folder B

        // Verify initial state: root, folderA, folderB are all expanded
        assertTrue(rootNode.isExpanded)
        assertTrue(nodeA.isExpanded)
        assertTrue(nodeB.isExpanded)
        assertEquals(4, engine.treeState.visibleNodes.value.size) // root, A, B, fileB

        // Focus / select Folder B
        engine.setSelectedPath(folderB.location.path)
        assertEquals(folderB.location.path, engine.selectedPath.value)

        // Action: Navigate Up
        val parentLocation = engine.navigateUp()

        // Assertions:
        assertNotNull(parentLocation)
        assertEquals(folderA.location.path, parentLocation?.path)
        assertEquals(folderA.location.path, engine.selectedPath.value) // Focused on Folder A

        // Folder B MUST be collapsed
        assertFalse(nodeB.isExpanded)
        // Folder A MUST be expanded
        assertTrue(nodeA.isExpanded)

        // Visible nodes must now only contain Root, Folder A, Folder B (fileB is hidden)
        val visiblePaths = engine.treeState.visibleNodes.value.map { it.node.data.location.path }
        assertEquals(listOf(rootItem.location.path, folderA.location.path, folderB.location.path), visiblePaths)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngineUpDirCollapseTest.kt
    // [Penjelasan]: Skenario 2 - Navigasi bertingkat beberapa level: Root -> A -> B -> C -> D -> Up Dir bertahap.
    @Test
    fun `test nested multiple levels Up Dir collapses each level step by step`() = runTest {
        val root = createFileItem("/storage/emulated/0", FileType.DIRECTORY)
        val dirA = createFileItem("/storage/emulated/0/A", FileType.DIRECTORY)
        val dirB = createFileItem("/storage/emulated/0/A/B", FileType.DIRECTORY)
        val dirC = createFileItem("/storage/emulated/0/A/B/C", FileType.DIRECTORY)
        val dirD = createFileItem("/storage/emulated/0/A/B/C/D", FileType.DIRECTORY)

        fakeDirectoryRepository.setFiles(root.location, listOf(dirA))
        fakeDirectoryRepository.setFiles(dirA.location, listOf(dirB))
        fakeDirectoryRepository.setFiles(dirB.location, listOf(dirC))
        fakeDirectoryRepository.setFiles(dirC.location, listOf(dirD))
        fakeDirectoryRepository.setFiles(dirD.location, emptyList())

        engine.loadRoot(root)
        val nodeA = engine.treeState.roots[0].children[0]
        engine.toggleNode(nodeA)
        val nodeB = nodeA.children[0]
        engine.toggleNode(nodeB)
        val nodeC = nodeB.children[0]
        engine.toggleNode(nodeC)
        val nodeD = nodeC.children[0]
        engine.toggleNode(nodeD)

        // All nodes A, B, C, D are expanded
        assertTrue(nodeA.isExpanded)
        assertTrue(nodeB.isExpanded)
        assertTrue(nodeC.isExpanded)
        assertTrue(nodeD.isExpanded)

        // Start at D
        engine.setSelectedPath(dirD.location.path)

        // Step 1: Up Dir from D to C -> D collapsed, C expanded & selected
        val loc1 = engine.navigateUp()
        assertEquals(dirC.location.path, loc1?.path)
        assertEquals(dirC.location.path, engine.selectedPath.value)
        assertFalse(nodeD.isExpanded)
        assertTrue(nodeC.isExpanded)

        // Step 2: Up Dir from C to B -> C collapsed, B expanded & selected
        val loc2 = engine.navigateUp()
        assertEquals(dirB.location.path, loc2?.path)
        assertEquals(dirB.location.path, engine.selectedPath.value)
        assertFalse(nodeC.isExpanded)
        assertTrue(nodeB.isExpanded)

        // Step 3: Up Dir from B to A -> B collapsed, A expanded & selected
        val loc3 = engine.navigateUp()
        assertEquals(dirA.location.path, loc3?.path)
        assertEquals(dirA.location.path, engine.selectedPath.value)
        assertFalse(nodeB.isExpanded)
        assertTrue(nodeA.isExpanded)

        // Step 4: Up Dir from A to Root -> A collapsed, Root expanded & selected
        val loc4 = engine.navigateUp()
        assertEquals(root.location.path, loc4?.path)
        assertEquals(root.location.path, engine.selectedPath.value)
        assertFalse(nodeA.isExpanded)
        assertTrue(engine.treeState.roots[0].isExpanded)

        // Step 5: Up Dir at Root -> remains at Root
        val loc5 = engine.navigateUp()
        assertEquals(root.location.path, loc5?.path)
        assertEquals(root.location.path, engine.selectedPath.value)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngineUpDirCollapseTest.kt
    // [Penjelasan]: Skenario 3 - Up Dir tidak merusak status ekspansi folder lain yang berdampingan (sibling).
    @Test
    fun `test Up Dir does not damage expansion of sibling folders`() = runTest {
        val root = createFileItem("/storage/emulated/0", FileType.DIRECTORY)
        val dirA = createFileItem("/storage/emulated/0/FolderA", FileType.DIRECTORY)
        val dirB = createFileItem("/storage/emulated/0/FolderA/FolderB", FileType.DIRECTORY)
        val dirOther = createFileItem("/storage/emulated/0/FolderOther", FileType.DIRECTORY)
        val dirOtherChild = createFileItem("/storage/emulated/0/FolderOther/Child", FileType.DIRECTORY)

        fakeDirectoryRepository.setFiles(root.location, listOf(dirA, dirOther))
        fakeDirectoryRepository.setFiles(dirA.location, listOf(dirB))
        fakeDirectoryRepository.setFiles(dirB.location, emptyList())
        fakeDirectoryRepository.setFiles(dirOther.location, listOf(dirOtherChild))
        fakeDirectoryRepository.setFiles(dirOtherChild.location, emptyList())

        engine.loadRoot(root)
        val nodeA = engine.treeState.roots[0].children[0]
        val nodeOther = engine.treeState.roots[0].children[1]

        engine.toggleNode(nodeA)
        val nodeB = nodeA.children[0]
        engine.toggleNode(nodeB)

        engine.toggleNode(nodeOther)
        val nodeOtherChild = nodeOther.children[0]
        engine.toggleNode(nodeOtherChild)

        // Both branch A/B and branch Other/Child are expanded
        assertTrue(nodeA.isExpanded)
        assertTrue(nodeB.isExpanded)
        assertTrue(nodeOther.isExpanded)
        assertTrue(nodeOtherChild.isExpanded)

        // Select Folder B
        engine.setSelectedPath(dirB.location.path)

        // Navigate Up from Folder B to Folder A
        val parentLoc = engine.navigateUp()
        assertEquals(dirA.location.path, parentLoc?.path)

        // Folder B is collapsed
        assertFalse(nodeB.isExpanded)
        assertTrue(nodeA.isExpanded)

        // Sibling branch Other and OtherChild MUST remain expanded!
        assertTrue(nodeOther.isExpanded)
        assertTrue(nodeOtherChild.isExpanded)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngineUpDirCollapseTest.kt
    // [Penjelasan]: Skenario 4 - Panel kiri dan kanan tetap independen saat Up Dir di-trigger pada salah satu panel.
    @Test
    fun `test left and right panel tree engines are independent during Up Dir collapse`() = runTest {
        val rootPath = "/storage/emulated/0"
        val volume = StorageVolumeItem("primary", "Internal Storage", rootPath, StorageVolumeType.PRIMARY_INTERNAL, false, null)
        val leftFolderA = createFileItem("/storage/emulated/0/LeftA", FileType.DIRECTORY)
        val leftFolderB = createFileItem("/storage/emulated/0/LeftA/LeftB", FileType.DIRECTORY)
        val rightFolderX = createFileItem("/storage/emulated/0/RightX", FileType.DIRECTORY)
        val rightFolderY = createFileItem("/storage/emulated/0/RightX/RightY", FileType.DIRECTORY)

        fakeDirectoryRepository.setFiles(StorageLocation(rootPath, "primary"), listOf(leftFolderA, rightFolderX))
        fakeDirectoryRepository.setFiles(leftFolderA.location, listOf(leftFolderB))
        fakeDirectoryRepository.setFiles(leftFolderB.location, emptyList())
        fakeDirectoryRepository.setFiles(rightFolderX.location, listOf(rightFolderY))
        fakeDirectoryRepository.setFiles(rightFolderY.location, emptyList())

        treeAdapter.loadVolumesAsRoots(PanelId.LEFT, listOf(volume))
        treeAdapter.loadVolumesAsRoots(PanelId.RIGHT, listOf(volume))

        val leftEngine = treeAdapter.getEngine(PanelId.LEFT)
        val rightEngine = treeAdapter.getEngine(PanelId.RIGHT)

        // Expand left panel
        val leftRoot = leftEngine.treeState.roots[0]
        leftEngine.toggleNode(leftRoot)
        val leftNodeA = leftRoot.children.first { it.data.location.path == leftFolderA.location.path }
        leftEngine.toggleNode(leftNodeA)
        val leftNodeB = leftNodeA.children[0]
        leftEngine.toggleNode(leftNodeB)
        treeAdapter.setSelectedPath(PanelId.LEFT, leftFolderB.location.path)

        // Expand right panel
        val rightRoot = rightEngine.treeState.roots[0]
        rightEngine.toggleNode(rightRoot)
        val rightNodeX = rightRoot.children.first { it.data.location.path == rightFolderX.location.path }
        rightEngine.toggleNode(rightNodeX)
        val rightNodeY = rightNodeX.children[0]
        rightEngine.toggleNode(rightNodeY)
        treeAdapter.setSelectedPath(PanelId.RIGHT, rightFolderY.location.path)

        // Both are expanded in their respective panels
        assertTrue(leftNodeB.isExpanded)
        assertTrue(rightNodeY.isExpanded)

        // Up Dir on Left Panel ONLY
        val leftParent = treeAdapter.navigateUp(PanelId.LEFT)
        assertEquals(leftFolderA.location.path, leftParent?.path)
        assertEquals(leftFolderA.location.path, treeAdapter.getSelectedPath(PanelId.LEFT).value)
        assertFalse(leftNodeB.isExpanded)

        // Right Panel MUST be completely unaffected
        assertEquals(rightFolderY.location.path, treeAdapter.getSelectedPath(PanelId.RIGHT).value)
        assertTrue(rightNodeY.isExpanded)
        assertTrue(rightNodeX.isExpanded)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngineUpDirCollapseTest.kt
    // [Penjelasan]: Skenario 5 & 6 - Memverifikasi konsistensi visibleNodes dan selectedPath setelah navigasi Up Dir.
    @Test
    fun `test selected and focused node remains parent node and visibleNodes is consistent after Up Dir`() = runTest {
        val root = createFileItem("/storage/emulated/0", FileType.DIRECTORY)
        val dir1 = createFileItem("/storage/emulated/0/Dir1", FileType.DIRECTORY)
        val dir2 = createFileItem("/storage/emulated/0/Dir1/Dir2", FileType.DIRECTORY)
        val file2 = createFileItem("/storage/emulated/0/Dir1/Dir2/file.txt", FileType.FILE)

        fakeDirectoryRepository.setFiles(root.location, listOf(dir1))
        fakeDirectoryRepository.setFiles(dir1.location, listOf(dir2))
        fakeDirectoryRepository.setFiles(dir2.location, listOf(file2))

        engine.loadRoot(root)
        val node1 = engine.treeState.roots[0].children[0]
        engine.toggleNode(node1)
        val node2 = node1.children[0]
        engine.toggleNode(node2)

        engine.setSelectedPath(dir2.location.path)

        // Visible before Up Dir: root, dir1, dir2, file2
        assertEquals(4, engine.treeState.visibleNodes.value.size)

        // Navigate Up
        val resultLoc = engine.navigateUp()
        assertEquals(dir1.location.path, resultLoc?.path)
        assertEquals(dir1.location.path, engine.selectedPath.value)

        // Focus range calculation based on parent
        val focusRange = engine.getFocusRange()
        assertNotNull(focusRange)

        // Visible after Up Dir: root, dir1, dir2 (file2 is no longer in visibleNodes)
        assertEquals(3, engine.treeState.visibleNodes.value.size)
        val visibleIds = engine.treeState.visibleNodes.value.map { it.node.data.id }
        assertTrue(visibleIds.contains(root.id))
        assertTrue(visibleIds.contains(dir1.id))
        assertTrue(visibleIds.contains(dir2.id))
        assertFalse(visibleIds.contains(file2.id))
    }
}
