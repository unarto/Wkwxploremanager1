// [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/FileTreeEngineTest.kt
// [Penjelasan]: Unit test untuk FileTreeEngine yang disesuaikan dengan kontrak DirectoryRepository, AppPreferencesRepository, dan model FileItem terkini.
package com.wakwau.xplore.filemanager.ui

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationError
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.core.storage.preferences.AppLanguage
import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
import com.wakwau.xplore.core.storage.preferences.AppThemeMode
import com.wakwau.xplore.core.storage.preferences.FileLayoutMode
import com.wakwau.xplore.core.storage.preferences.FilePreferencesState
import com.wakwau.xplore.core.storage.preferences.FileSortDirection
import com.wakwau.xplore.core.storage.preferences.FileSortOrder
import com.wakwau.xplore.core.storage.preferences.SettingsState
import com.wakwau.xplore.core.storage.repository.DirectoryRepository
import com.wakwau.xplore.filemanager.ui.tree.FileTreeEngine
import com.wakwau.xplore.filemanager.usecase.ListDirectoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileTreeEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDirectoryRepository: FakeDirectoryRepository
    private lateinit var fakeAppPreferencesRepository: FakeAppPreferencesRepository
    private lateinit var listDirectoryUseCase: ListDirectoryUseCase
    private lateinit var engine: FileTreeEngine

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDirectoryRepository = FakeDirectoryRepository()
        fakeAppPreferencesRepository = FakeAppPreferencesRepository()
        listDirectoryUseCase = ListDirectoryUseCase(fakeDirectoryRepository, fakeAppPreferencesRepository)
        engine = FileTreeEngine(listDirectoryUseCase, fakeAppPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRoot populates tree roots and loads children`() = runTest {
        val rootLocation = StorageLocation("/", "local")
        val rootItem = createFileItem("/", FileType.DIRECTORY)
        
        // Root has one directory and one file
        fakeDirectoryRepository.setFiles(rootLocation, listOf(
            createFileItem("/dir1", FileType.DIRECTORY),
            createFileItem("/file1.txt", FileType.FILE)
        ))

        engine.loadRoot(rootItem)

        val roots = engine.treeState.roots
        assertEquals(1, roots.size)
        assertEquals("/", roots[0].data.location.path)

        val children = roots[0].children
        assertEquals(2, children.size)
        assertEquals("/dir1", children[0].data.location.path)
        assertNull(engine.errorState.value)
    }

    @Test
    fun `loadRoot with empty directory has empty children and expands without placeholder`() = runTest {
        // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/FileTreeEngineTest.kt
        // [Penjelasan]: Verifikasi bahwa direktori kosong tidak menyuntikkan node placeholder tiruan ke dalam struktur data domain tree.
        val rootLocation = StorageLocation("/emptyDir", "local")
        val rootItem = createFileItem("/emptyDir", FileType.DIRECTORY)
        
        fakeDirectoryRepository.setFiles(rootLocation, emptyList())

        engine.loadRoot(rootItem)

        val roots = engine.treeState.roots
        assertEquals(1, roots.size)
        assertTrue(roots[0].children.isEmpty())
        assertTrue(engine.treeState.isExpanded(roots[0]))
        assertNull(engine.errorState.value)
    }

    @Test
    fun `setSelectedPath updates selectedPath StateFlow`() = runTest {
        assertNull(engine.selectedPath.value)
        engine.setSelectedPath("/storage/emulated/0/Download")
        assertEquals("/storage/emulated/0/Download", engine.selectedPath.value)
        engine.setSelectedPath(null)
        assertNull(engine.selectedPath.value)
    }

    @Test
    fun `loadRoot handles repository failure`() = runTest {
        val rootItem = createFileItem("/", FileType.DIRECTORY)
        fakeDirectoryRepository.shouldFail = true

        engine.loadRoot(rootItem)

        val roots = engine.treeState.roots
        assertEquals(1, roots.size)
        assertTrue(roots[0].children.isEmpty())
        
        assertEquals("UNKNOWN", engine.errorState.value)
    }

    @Test
    fun `toggleNode expands and loads children if not expanded`() = runTest {
        val rootLocation = StorageLocation("/", "local")
        val rootItem = createFileItem("/", FileType.DIRECTORY)
        
        fakeDirectoryRepository.setFiles(rootLocation, listOf(
            createFileItem("/dir1", FileType.DIRECTORY)
        ))

        engine.loadRoot(rootItem)
        
        val rootNode = engine.treeState.roots[0]
        assertTrue(engine.treeState.isExpanded(rootNode))
        
        val dirNode = rootNode.children[0]
        fakeDirectoryRepository.setFiles(dirNode.data.location, listOf(
            createFileItem("/dir1/subfile.txt", FileType.FILE)
        ))

        assertFalse(engine.treeState.isExpanded(dirNode))
        
        engine.toggleNode(dirNode)
        
        assertTrue(engine.treeState.isExpanded(dirNode))
        assertEquals(1, dirNode.children.size)
    }

    @Test
    fun `toggleNode collapses node if already expanded`() = runTest {
        val rootItem = createFileItem("/", FileType.DIRECTORY)
        engine.loadRoot(rootItem)
        
        val rootNode = engine.treeState.roots[0]
        assertTrue(engine.treeState.isExpanded(rootNode))
        
        engine.toggleNode(rootNode) // collapse
        assertFalse(engine.treeState.isExpanded(rootNode))
    }

    @Test
    fun `getFocusRange and getBorderPositionForIndex calculate dynamic scope`() = runTest {
        val rootLocation = StorageLocation("/", "local")
        val rootItem = createFileItem("/", FileType.DIRECTORY)
        fakeDirectoryRepository.setFiles(rootLocation, listOf(
            createFileItem("/dir1", FileType.DIRECTORY),
            createFileItem("/file1.txt", FileType.FILE)
        ))

        engine.loadRoot(rootItem)

        // With no selected path
        assertNull(engine.getFocusRange())
        assertEquals(com.wakwau.xplore.treeview.model.BorderPosition.NONE, engine.getBorderPositionForIndex(0))

        // Focus on root "/" which has 2 children and is expanded -> range 0..2
        engine.setSelectedPath("/")
        assertEquals(0..2, engine.getFocusRange())
        assertEquals(com.wakwau.xplore.treeview.model.BorderPosition.TOP, engine.getBorderPositionForIndex(0))
        assertEquals(com.wakwau.xplore.treeview.model.BorderPosition.MIDDLE, engine.getBorderPositionForIndex(1))
        assertEquals(com.wakwau.xplore.treeview.model.BorderPosition.BOTTOM, engine.getBorderPositionForIndex(2))

        // Focus on leaf child "/file1.txt" -> range 2..2 (SINGLE)
        engine.setSelectedPath("/file1.txt")
        assertEquals(2..2, engine.getFocusRange())
        assertEquals(com.wakwau.xplore.treeview.model.BorderPosition.NONE, engine.getBorderPositionForIndex(0))
        assertEquals(com.wakwau.xplore.treeview.model.BorderPosition.NONE, engine.getBorderPositionForIndex(1))
        assertEquals(com.wakwau.xplore.treeview.model.BorderPosition.SINGLE, engine.getBorderPositionForIndex(2))
    }

    @Test
    fun `navigateUp from child directory navigates to parent directory and updates selectedPath`() = runTest {
        val rootItem = createFileItem("/storage/emulated/0", FileType.DIRECTORY)
        val dir1Item = createFileItem("/storage/emulated/0/Documents", FileType.DIRECTORY)

        fakeDirectoryRepository.setFiles(rootItem.location, listOf(dir1Item))
        engine.loadRoot(rootItem)

        // Select child directory
        engine.setSelectedPath("/storage/emulated/0/Documents")
        assertEquals("/storage/emulated/0/Documents", engine.selectedPath.value)

        // Navigate Up
        val parentLocation = engine.navigateUp()
        assertEquals("/storage/emulated/0", parentLocation?.path)
        assertEquals("/storage/emulated/0", engine.selectedPath.value)
    }

    @Test
    fun `navigateUp from leaf file navigates to parent directory`() = runTest {
        val rootItem = createFileItem("/storage/emulated/0", FileType.DIRECTORY)
        val file1Item = createFileItem("/storage/emulated/0/file.txt", FileType.FILE)

        fakeDirectoryRepository.setFiles(rootItem.location, listOf(file1Item))
        engine.loadRoot(rootItem)

        // Select leaf file
        engine.setSelectedPath("/storage/emulated/0/file.txt")
        assertEquals("/storage/emulated/0/file.txt", engine.selectedPath.value)

        // Navigate Up
        val parentLocation = engine.navigateUp()
        assertEquals("/storage/emulated/0", parentLocation?.path)
        assertEquals("/storage/emulated/0", engine.selectedPath.value)
    }

    @Test
    fun `navigateUp from nested subdirectories navigates up step by step`() = runTest {
        val rootItem = createFileItem("/storage/emulated/0", FileType.DIRECTORY)
        val sub1Item = createFileItem("/storage/emulated/0/Documents", FileType.DIRECTORY)
        val sub2Item = createFileItem("/storage/emulated/0/Documents/Work", FileType.DIRECTORY)
        val sub3Item = createFileItem("/storage/emulated/0/Documents/Work/Project", FileType.DIRECTORY)

        fakeDirectoryRepository.setFiles(rootItem.location, listOf(sub1Item))
        fakeDirectoryRepository.setFiles(sub1Item.location, listOf(sub2Item))
        fakeDirectoryRepository.setFiles(sub2Item.location, listOf(sub3Item))

        engine.loadRoot(rootItem)
        val sub1Node = engine.treeState.roots[0].children[0]
        engine.toggleNode(sub1Node) // expand Documents
        val sub2Node = sub1Node.children[0]
        engine.toggleNode(sub2Node) // expand Work

        // Start at deepest level: Project
        engine.setSelectedPath("/storage/emulated/0/Documents/Work/Project")

        // Step 1: Up to Work
        val step1 = engine.navigateUp()
        assertEquals("/storage/emulated/0/Documents/Work", step1?.path)
        assertEquals("/storage/emulated/0/Documents/Work", engine.selectedPath.value)

        // Step 2: Up to Documents
        val step2 = engine.navigateUp()
        assertEquals("/storage/emulated/0/Documents", step2?.path)
        assertEquals("/storage/emulated/0/Documents", engine.selectedPath.value)

        // Step 3: Up to Root
        val step3 = engine.navigateUp()
        assertEquals("/storage/emulated/0", step3?.path)
        assertEquals("/storage/emulated/0", engine.selectedPath.value)
    }

    @Test
    fun `navigateUp from root node stays at root boundary`() = runTest {
        val rootItem = createFileItem("/storage/emulated/0", FileType.DIRECTORY)

        fakeDirectoryRepository.setFiles(rootItem.location, emptyList())
        engine.loadRoot(rootItem)

        // Select root
        engine.setSelectedPath("/storage/emulated/0")
        assertEquals("/storage/emulated/0", engine.selectedPath.value)

        // Navigate Up when already at root
        val parentLocation = engine.navigateUp()
        assertEquals("/storage/emulated/0", parentLocation?.path)
        assertEquals("/storage/emulated/0", engine.selectedPath.value)
    }

    private fun createFileItem(path: String, type: FileType): FileItem {
        return FileItem(
            id = path,
            location = StorageLocation(path, "local"),
            name = path.substringAfterLast("/").ifEmpty { "/" },
            type = type,
            metadata = FileMetadata.EMPTY
        )
    }
}

// [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/FileTreeEngineTest.kt
// [Penjelasan]: Fake repository untuk simulasi DirectoryRepository dalam unit test FileTreeEngine.
class FakeDirectoryRepository : DirectoryRepository {
    var shouldFail = false
    private val files = mutableMapOf<StorageLocation, List<FileItem>>()

    fun setFiles(location: StorageLocation, newFiles: List<FileItem>) {
        files[location] = newFiles
    }

    override suspend fun list(location: StorageLocation, showHidden: Boolean): FileOperationResult<List<FileItem>> {
        if (shouldFail) return FileOperationResult.Failure(FileOperationError.UNKNOWN)
        val list = files[location] ?: emptyList()
        val filtered = if (showHidden) list else list.filter { !it.metadata.isHidden }
        return FileOperationResult.Success(filtered)
    }

    override suspend fun create(location: StorageLocation, name: String): FileOperationResult<FileItem> {
        throw UnsupportedOperationException()
    }
}

// [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/FileTreeEngineTest.kt
// [Penjelasan]: Fake repository untuk simulasi AppPreferencesRepository dalam unit test FileTreeEngine.
class FakeAppPreferencesRepository(
    initialPreferences: FilePreferencesState = FilePreferencesState()
) : AppPreferencesRepository {
    private val _prefs = MutableStateFlow(initialPreferences)
    override val preferencesState: StateFlow<FilePreferencesState> = _prefs.asStateFlow()
    override fun getPreferencesState(): FilePreferencesState = _prefs.value

    private val _settings = MutableStateFlow(SettingsState())
    override val settingsState: StateFlow<SettingsState> = _settings.asStateFlow()

    override suspend fun setSortOrder(sortOrder: FileSortOrder) {
        _prefs.value = _prefs.value.copy(sortOrder = sortOrder)
    }

    override suspend fun setSortDirection(sortDirection: FileSortDirection) {
        _prefs.value = _prefs.value.copy(sortDirection = sortDirection)
    }

    override suspend fun setLayoutMode(layoutMode: FileLayoutMode) {
        _prefs.value = _prefs.value.copy(layoutMode = layoutMode)
    }

    override suspend fun setShowHiddenFiles(showHiddenFiles: Boolean) {
        _prefs.value = _prefs.value.copy(showHiddenFiles = showHiddenFiles)
    }

    override suspend fun setLastVisitedPath(lastVisitedPath: String) {
        _prefs.value = _prefs.value.copy(lastVisitedPath = lastVisitedPath)
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        _settings.value = _settings.value.copy(language = language)
    }

    override suspend fun setFileSystemAccessMode(mode: com.wakwau.xplore.core.storage.preferences.FileSystemAccessMode) {
        _settings.value = _settings.value.copy(fileSystemAccessMode = mode)
    }

    override suspend fun setRootReadOnly(isReadOnly: Boolean) {
        _settings.value = _settings.value.copy(isRootReadOnly = isReadOnly)
    }
}
