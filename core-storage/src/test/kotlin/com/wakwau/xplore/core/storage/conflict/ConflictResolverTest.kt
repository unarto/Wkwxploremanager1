// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/conflict/ConflictResolverTest.kt
// [Penjelasan]: Unit test untuk memverifikasi fungsionalitas ConflictResolver pada resolusi benturan berkas dan folder (SKIP, OVERWRITE, RENAME, multiple conflict).
package com.wakwau.xplore.core.storage.conflict

import com.wakwau.xplore.core.storage.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConflictResolverTest {

    private lateinit var resolver: DefaultConflictResolver

    @Before
    fun setUp() {
        resolver = DefaultConflictResolver()
    }

    @Test
    fun resolveConflict_skipChoice_returnsNull() {
        val conflict = FileConflict(
            source = StorageLocation("/source/test.txt", "root1"),
            sourceName = "test.txt",
            targetName = "test.txt",
            isDirectory = false,
            destinationDir = StorageLocation("/dest", "root1")
        )
        val existing = mutableSetOf("test.txt")

        val result = resolver.resolveConflict(conflict, ConflictChoice.SKIP, existing)

        assertNull(result)
        assertEquals(setOf("test.txt"), existing)
    }

    @Test
    fun resolveConflict_overwriteChoice_returnsTargetLocationWithSameName() {
        val conflict = FileConflict(
            source = StorageLocation("/source/test.txt", "root1"),
            sourceName = "test.txt",
            targetName = "test.txt",
            isDirectory = false,
            destinationDir = StorageLocation("/dest", "root1")
        )
        val existing = mutableSetOf("test.txt")

        val result = resolver.resolveConflict(conflict, ConflictChoice.OVERWRITE, existing)

        assertNotNull(result)
        assertEquals("/source/test.txt", result?.source?.path)
        assertEquals("/dest/test.txt", result?.targetLocation?.path)
        assertEquals(ConflictChoice.OVERWRITE, result?.choice)
    }

    @Test
    fun resolveConflict_renameFileChoice_generatesIndexedFileNameAndUpdatesExistingNames() {
        val conflict = FileConflict(
            source = StorageLocation("/source/report.pdf", "root1"),
            sourceName = "report.pdf",
            targetName = "report.pdf",
            isDirectory = false,
            destinationDir = StorageLocation("/dest", "root1")
        )
        val existing = mutableSetOf("report.pdf", "report (1).pdf")

        val result = resolver.resolveConflict(conflict, ConflictChoice.RENAME, existing)

        assertNotNull(result)
        assertEquals("/dest/report (2).pdf", result?.targetLocation?.path)
        assertEquals(ConflictChoice.RENAME, result?.choice)
        assertTrue(existing.contains("report (2).pdf"))
    }

    @Test
    fun resolveConflict_renameFolderChoice_generatesIndexedFolderName() {
        val conflict = FileConflict(
            source = StorageLocation("/source/photos", "root1"),
            sourceName = "photos",
            targetName = "photos",
            isDirectory = true,
            destinationDir = StorageLocation("/dest", "root1")
        )
        val existing = mutableSetOf("photos")

        val result = resolver.resolveConflict(conflict, ConflictChoice.RENAME, existing)

        assertNotNull(result)
        assertEquals("/dest/photos (1)", result?.targetLocation?.path)
        assertEquals(ConflictChoice.RENAME, result?.choice)
        assertTrue(existing.contains("photos (1)"))
    }

    @Test
    fun resolveMultipleConflicts_sequentialIndexing_allocatesUniqueTargetPaths() {
        val conflict1 = FileConflict(
            source = StorageLocation("/source1/doc.docx", "root1"),
            sourceName = "doc.docx",
            targetName = "doc.docx",
            isDirectory = false,
            destinationDir = StorageLocation("/dest", "root1")
        )
        val conflict2 = FileConflict(
            source = StorageLocation("/source2/doc.docx", "root1"),
            sourceName = "doc.docx",
            targetName = "doc.docx",
            isDirectory = false,
            destinationDir = StorageLocation("/dest", "root1")
        )
        val existing = mutableSetOf("doc.docx")

        val res1 = resolver.resolveConflict(conflict1, ConflictChoice.RENAME, existing)
        val res2 = resolver.resolveConflict(conflict2, ConflictChoice.RENAME, existing)

        assertEquals("/dest/doc (1).docx", res1?.targetLocation?.path)
        assertEquals("/dest/doc (2).docx", res2?.targetLocation?.path)
    }
}
