// [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
// [Penjelasan]: Pengujian unit komprehensif untuk evaluasi status seleksi X-plore: Storage node protection, Folder 3-state cycle (Single Mark, Mark All Children, Unmark All), Strict Scope Isolation guard pada sibling, dan 2-state toggle pada berkas.
package com.wakwau.xplore.filemanager.ui.selection

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.treeview.model.TreeNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeSelectionHandlerTest {

    private val handler = TreeSelectionHandler()

    companion object {
        private const val ROOT_ID = "storage_root"
        private const val STORAGE_PATH = "/storage/emulated/0"
        private const val DIR_ANDROID = "/storage/emulated/0/Android"
        private const val DIR_ANDROID_DATA = "/storage/emulated/0/Android/data"
        private const val DIR_ANDROID_MEDIA = "/storage/emulated/0/Android/media"
        private const val DIR_ANDROID_OBB = "/storage/emulated/0/Android/obb"
        private const val DIR_DCIM = "/storage/emulated/0/DCIM"
        private const val DIR_ALARMS = "/storage/emulated/0/Alarms"
        private const val FILE_DOC = "/storage/emulated/0/document.pdf"
        private const val DIR_EMPTY = "/storage/emulated/0/empty_dir"
    }

    private fun createFileItem(path: String, isDir: Boolean): FileItem {
        val name = path.substringAfterLast('/')
        return FileItem(
            id = path,
            name = name,
            location = StorageLocation(path = path, rootId = ROOT_ID),
            type = if (isDir) FileType.DIRECTORY else FileType.FILE,
            metadata = FileMetadata(100L, 1000L, null, true, true, false, false)
        )
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
    // [Penjelasan]: Memverifikasi bahwa node Storage tidak boleh berstatus CHECKED dan aksi klik hanya mengubah direct children.
    @Test
    fun `test storage node protection and direct children selection`() {
        val storageItem = createFileItem(STORAGE_PATH, isDir = true)
        val androidItem = createFileItem(DIR_ANDROID, isDir = true)
        val dcimItem = createFileItem(DIR_DCIM, isDir = true)
        val docItem = createFileItem(FILE_DOC, isDir = false)

        val androidNode = TreeNode(id = androidItem.id, data = androidItem)
        val dcimNode = TreeNode(id = dcimItem.id, data = dcimItem)
        val docNode = TreeNode(id = docItem.id, data = docItem)

        val storageNode = TreeNode(id = storageItem.id, data = storageItem).apply {
            addChild(androidNode)
            addChild(dcimNode)
            addChild(docNode)
        }

        // Storage node must NEVER be checked (orange icon prohibited on storage row)
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(storageNode, emptySet()))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(storageNode, setOf(storageItem.id, androidItem.id)))

        // Klik 1 pada Storage: HANYA memberikan ceklis oranye ke direct children langsung
        val sel1 = handler.nextSelection(storageNode, emptySet())
        assertFalse(sel1.contains(storageItem.id))
        assertTrue(sel1.contains(androidItem.id))
        assertTrue(sel1.contains(dcimItem.id))
        assertTrue(sel1.contains(docItem.id))
        assertEquals(3, sel1.size)

        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(storageNode, sel1))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(androidNode, sel1))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(dcimNode, sel1))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(docNode, sel1))

        // Klik 2 pada Storage: HANYA menghapus ceklis dari direct children
        val sel2 = handler.nextSelection(storageNode, sel1)
        assertFalse(sel2.contains(storageItem.id))
        assertFalse(sel2.contains(androidItem.id))
        assertFalse(sel2.contains(dcimItem.id))
        assertFalse(sel2.contains(docItem.id))
        assertTrue(sel2.isEmpty())

        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(storageNode, sel2))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(androidNode, sel2))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(dcimNode, sel2))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(docNode, sel2))
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
    // [Penjelasan]: Memverifikasi siklus 3-state pada Folder: Single Mark (Klik 1), Mark All Children (Klik 2), dan Unmark All (Klik 3).
    @Test
    fun `test folder 3-state selection cycle (Single Mark, Mark All Children, Unmark All)`() {
        val storageItem = createFileItem(STORAGE_PATH, isDir = true)
        val androidItem = createFileItem(DIR_ANDROID, isDir = true)
        val dataItem = createFileItem(DIR_ANDROID_DATA, isDir = true)
        val mediaItem = createFileItem(DIR_ANDROID_MEDIA, isDir = true)
        val obbItem = createFileItem(DIR_ANDROID_OBB, isDir = true)

        val dataNode = TreeNode(id = dataItem.id, data = dataItem)
        val mediaNode = TreeNode(id = mediaItem.id, data = mediaItem)
        val obbNode = TreeNode(id = obbItem.id, data = obbItem)

        val androidNode = TreeNode(id = androidItem.id, data = androidItem).apply {
            addChild(dataNode)
            addChild(mediaNode)
            addChild(obbNode)
        }

        val storageNode = TreeNode(id = storageItem.id, data = storageItem).apply {
            addChild(androidNode)
        }

        // Initial State: Unmarked
        var selection = emptySet<String>()
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(androidNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(dataNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(mediaNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(obbNode, selection))

        // Klik 1: Single Mark -> Folder Android tercentang, anak-anaknya belum tercentang
        selection = handler.nextSelection(androidNode, selection)
        assertTrue(selection.contains(androidItem.id))
        assertFalse(selection.contains(dataItem.id))
        assertFalse(selection.contains(mediaItem.id))
        assertFalse(selection.contains(obbItem.id))
        assertEquals(1, selection.size)

        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(androidNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(dataNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(mediaNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(obbNode, selection))

        // Klik 2: Mark All Children -> Folder Android TIDAK DICENTANG (kelabu), seluruh anak DIBERI CEKLIS ORANYE
        selection = handler.nextSelection(androidNode, selection)
        assertFalse(selection.contains(androidItem.id))
        assertTrue(selection.contains(dataItem.id))
        assertTrue(selection.contains(mediaItem.id))
        assertTrue(selection.contains(obbItem.id))
        assertEquals(3, selection.size)

        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(androidNode, selection))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(dataNode, selection))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(mediaNode, selection))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(obbNode, selection))

        // Klik 3: Unmark All -> Hapus semua ceklis dari folder dan seluruh anak di dalamnya
        selection = handler.nextSelection(androidNode, selection)
        assertFalse(selection.contains(androidItem.id))
        assertFalse(selection.contains(dataItem.id))
        assertFalse(selection.contains(mediaItem.id))
        assertFalse(selection.contains(obbItem.id))
        assertTrue(selection.isEmpty())

        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(androidNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(dataNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(mediaNode, selection))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(obbNode, selection))

        // Klik 4: Kembali ke Siklus Klik 1 (Single Mark)
        selection = handler.nextSelection(androidNode, selection)
        assertTrue(selection.contains(androidItem.id))
        assertEquals(1, selection.size)
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(androidNode, selection))
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
    // [Penjelasan]: Memverifikasi Strict Scope Isolation Guard di mana operasi ceklis pada satu folder auto-deselect item di luar scope jalurnya.
    @Test
    fun `test strict scope isolation guard for sibling folders and files`() {
        val storageItem = createFileItem(STORAGE_PATH, isDir = true)
        val androidItem = createFileItem(DIR_ANDROID, isDir = true)
        val dataItem = createFileItem(DIR_ANDROID_DATA, isDir = true)
        val dcimItem = createFileItem(DIR_DCIM, isDir = true)
        val alarmsItem = createFileItem(DIR_ALARMS, isDir = true)

        val dataNode = TreeNode(id = dataItem.id, data = dataItem)
        val androidNode = TreeNode(id = androidItem.id, data = androidItem).apply {
            addChild(dataNode)
        }
        val dcimNode = TreeNode(id = dcimItem.id, data = dcimItem)
        val alarmsNode = TreeNode(id = alarmsItem.id, data = alarmsItem)

        TreeNode(id = storageItem.id, data = storageItem).apply {
            addChild(androidNode)
            addChild(dcimNode)
            addChild(alarmsNode)
        }

        // DCIM sebelumnya sudah tercentang
        val initialSelection = setOf(dcimItem.id)

        // Klik 1 pada Android (Single Mark Android) -> Mengizinkan multi-select anak sesama parent (storage)
        val sel1 = handler.nextSelection(androidNode, initialSelection)
        assertTrue(sel1.contains(androidItem.id))
        assertTrue(sel1.contains(dcimItem.id)) // DCIM sesama anak storage tetap diizinkan multi-select
        assertFalse(sel1.contains(alarmsItem.id)) // Alarms sibling tetap tidak tercentang
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(androidNode, sel1))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(dcimNode, sel1))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(alarmsNode, sel1))

        // Klik 2 pada Android (Mark children data + auto-expand) -> Clear outside scope Android, sehingga DCIM dibersihkan
        val sel2 = handler.nextSelection(androidNode, sel1)
        assertFalse(sel2.contains(androidItem.id))
        assertTrue(sel2.contains(dataItem.id))
        assertFalse(sel2.contains(dcimItem.id)) // DCIM dibersihkan karena di luar folder Android
        assertFalse(sel2.contains(alarmsItem.id))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(androidNode, sel2))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(dataNode, sel2))

        // Klik 3 pada Android (Unmark All Android & children)
        val sel3 = handler.nextSelection(androidNode, sel2)
        assertFalse(sel3.contains(androidItem.id))
        assertFalse(sel3.contains(dataItem.id))
        assertFalse(sel3.contains(dcimItem.id))
        assertFalse(sel3.contains(alarmsItem.id))
        assertTrue(sel3.isEmpty())
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(androidNode, sel3))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(dcimNode, sel3))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(alarmsNode, sel3))
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
    // [Penjelasan]: Memverifikasi siklus 2-state pada berkas tunggal (UNCHECKED <-> CHECKED).
    @Test
    fun `test file 2-state toggle between unchecked and checked`() {
        val fileItem = createFileItem(FILE_DOC, isDir = false)
        val fileNode = TreeNode(id = fileItem.id, data = fileItem)

        val initialSelection = emptySet<String>()
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(fileNode, initialSelection))

        val sel1 = handler.nextSelection(fileNode, initialSelection)
        assertTrue(sel1.contains(fileItem.id))
        assertEquals(1, sel1.size)
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(fileNode, sel1))

        val sel2 = handler.nextSelection(fileNode, sel1)
        assertFalse(sel2.contains(fileItem.id))
        assertEquals(0, sel2.size)
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(fileNode, sel2))
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
    // [Penjelasan]: Memverifikasi Multi-Select pada berkas-berkas yang berada di bawah parent direktori yang sama, serta deselect berkas di luar parent.
    @Test
    fun `test multi-select files under same parent and deselect outside parent`() {
        val parentItem = createFileItem("/storage/emulated/0/Download", isDir = true)
        val file1 = createFileItem("/storage/emulated/0/Download/file1.txt", isDir = false)
        val file2 = createFileItem("/storage/emulated/0/Download/file2.txt", isDir = false)
        val outsideFile = createFileItem("/storage/emulated/0/DCIM/photo.jpg", isDir = false)

        val file1Node = TreeNode(id = file1.id, data = file1)
        val file2Node = TreeNode(id = file2.id, data = file2)
        TreeNode(id = parentItem.id, data = parentItem).apply {
            addChild(file1Node)
            addChild(file2Node)
        }

        val outsideParent = createFileItem("/storage/emulated/0/DCIM", isDir = true)
        val outsideNode = TreeNode(id = outsideFile.id, data = outsideFile)
        TreeNode(id = outsideParent.id, data = outsideParent).apply {
            addChild(outsideNode)
        }

        // Tandai file outside terlebih dahulu
        var selection = handler.nextSelection(outsideNode, emptySet())
        assertTrue(selection.contains(outsideFile.id))

        // Tandai file1 di Download -> Karena outsideFile berada di luar Download, outsideFile otomatis dibersihkan
        selection = handler.nextSelection(file1Node, selection)
        assertTrue(selection.contains(file1.id))
        assertFalse(selection.contains(outsideFile.id))

        // Tandai file2 di Download -> Karena sama-sama di bawah Download, file1 dan file2 keduanya tercentang (multi-select)
        selection = handler.nextSelection(file2Node, selection)
        assertTrue(selection.contains(file1.id))
        assertTrue(selection.contains(file2.id))
        assertEquals(2, selection.size)

        // Unmark file1 -> file2 tetap tercentang
        selection = handler.nextSelection(file1Node, selection)
        assertFalse(selection.contains(file1.id))
        assertTrue(selection.contains(file2.id))
        assertEquals(1, selection.size)
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
    // [Penjelasan]: Memverifikasi toggle pada folder kosong (UNCHECKED <-> CHECKED).
    @Test
    fun `test empty folder cycle toggles between check and uncheck`() {
        val storageItem = createFileItem(STORAGE_PATH, isDir = true)
        val emptyDirItem = createFileItem(DIR_EMPTY, isDir = true)
        val emptyDirNode = TreeNode(id = emptyDirItem.id, data = emptyDirItem)

        TreeNode(id = storageItem.id, data = storageItem).apply {
            addChild(emptyDirNode)
        }

        var currentSelection = emptySet<String>()
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(emptyDirNode, currentSelection))

        currentSelection = handler.nextSelection(emptyDirNode, currentSelection)
        assertEquals(setOf(DIR_EMPTY), currentSelection)
        assertEquals(1, currentSelection.size)
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(emptyDirNode, currentSelection))

        currentSelection = handler.nextSelection(emptyDirNode, currentSelection)
        assertTrue(currentSelection.isEmpty())
        assertEquals(0, currentSelection.size)
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(emptyDirNode, currentSelection))
    }

    // [Jalur Class/Modul]: filemanager-ui/src/test/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandlerTest.kt
    // [Penjelasan]: Memverifikasi Strict Scope Isolation Guard antar Storage Node berbeda (misal Internal Storage vs Kartu SD).
    @Test
    fun `test multi-storage scope isolation between internal storage and sdcard`() {
        val internalStorageItem = createFileItem("/storage/emulated/0", isDir = true)
        val internalChild1 = createFileItem("/storage/emulated/0/Download", isDir = true)
        val internalChild2 = createFileItem("/storage/emulated/0/Pictures", isDir = true)

        val sdcardStorageItem = createFileItem("/storage/1234-5678", isDir = true)
        val sdcardChild1 = createFileItem("/storage/1234-5678/Music", isDir = true)
        val sdcardChild2 = createFileItem("/storage/1234-5678/Videos", isDir = true)

        val intChild1Node = TreeNode(id = internalChild1.id, data = internalChild1)
        val intChild2Node = TreeNode(id = internalChild2.id, data = internalChild2)
        val internalStorageNode = TreeNode(id = internalStorageItem.id, data = internalStorageItem).apply {
            addChild(intChild1Node)
            addChild(intChild2Node)
        }

        val sdChild1Node = TreeNode(id = sdcardChild1.id, data = sdcardChild1)
        val sdChild2Node = TreeNode(id = sdcardChild2.id, data = sdcardChild2)
        val sdcardStorageNode = TreeNode(id = sdcardStorageItem.id, data = sdcardStorageItem).apply {
            addChild(sdChild1Node)
            addChild(sdChild2Node)
        }

        // Storage nodes must NEVER be marked
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(internalStorageNode, emptySet()))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(sdcardStorageNode, emptySet()))

        // Klik 1 pada Internal Storage: Hanya anak-anak internal storage yang tercentang
        val sel1 = handler.nextSelection(internalStorageNode, emptySet())
        assertTrue(sel1.contains(internalChild1.id))
        assertTrue(sel1.contains(internalChild2.id))
        assertFalse(sel1.contains(internalStorageItem.id))
        assertFalse(sel1.contains(sdcardChild1.id))
        assertFalse(sel1.contains(sdcardChild2.id))
        assertFalse(sel1.contains(sdcardStorageItem.id))
        assertEquals(2, sel1.size)

        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(intChild1Node, sel1))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(intChild2Node, sel1))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(sdChild1Node, sel1))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(sdChild2Node, sel1))

        // Klik 1 pada SD Card: AUTO-DESELECT SELURUH KONTEN DI LUAR STORAGE INI
        val sel2 = handler.nextSelection(sdcardStorageNode, sel1)
        assertFalse(sel2.contains(internalChild1.id))
        assertFalse(sel2.contains(internalChild2.id))
        assertTrue(sel2.contains(sdcardChild1.id))
        assertTrue(sel2.contains(sdcardChild2.id))
        assertFalse(sel2.contains(internalStorageItem.id))
        assertFalse(sel2.contains(sdcardStorageItem.id))
        assertEquals(2, sel2.size)

        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(intChild1Node, sel2))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(intChild2Node, sel2))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(sdChild1Node, sel2))
        assertEquals(FolderCheckCycleState.CHECKED, handler.getSelectionState(sdChild2Node, sel2))

        // Klik 2 pada SD Card: Hapus centang dari direct children SD Card
        val sel3 = handler.nextSelection(sdcardStorageNode, sel2)
        assertFalse(sel3.contains(internalChild1.id))
        assertFalse(sel3.contains(internalChild2.id))
        assertFalse(sel3.contains(sdcardChild1.id))
        assertFalse(sel3.contains(sdcardChild2.id))
        assertTrue(sel3.isEmpty())

        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(sdChild1Node, sel3))
        assertEquals(FolderCheckCycleState.UNCHECKED, handler.getSelectionState(sdChild2Node, sel3))
    }
}


