// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/conflict/DefaultConflictDetectorTest.kt
// [Penjelasan]: Unit test untuk memverifikasi deteksi benturan nama berkas/folder ganda pada lokasi tujuan menggunakan DefaultConflictDetector.
package com.wakwau.xplore.core.storage.conflict

import com.wakwau.xplore.core.storage.filesystem.StorageBackendClassifier
import com.wakwau.xplore.core.storage.filesystem.local.LocalFileSystem
import com.wakwau.xplore.core.storage.mapper.FileItemMapper
import com.wakwau.xplore.core.storage.metadata.FileMetadataReader
import com.wakwau.xplore.core.storage.model.StorageLocation
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DefaultConflictDetectorTest {

    private lateinit var detector: DefaultConflictDetector
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("conflict_detector_test").toFile()
        val localFs = LocalFileSystem(FileMetadataReader(), FileItemMapper())
        val classifier = StorageBackendClassifier(isSuAvailable = { false }, isShizukuAvailable = { false }, isSafPersisted = { false })

        detector = DefaultConflictDetector(
            localFileSystem = localFs,
            safFileSystem = com.wakwau.xplore.core.storage.testutil.TestSafFileSystem(),
            safShizukuFileSystem = com.wakwau.xplore.core.storage.testutil.TestShizukuFileSystem(),
            rootFileSystem = com.wakwau.xplore.core.storage.testutil.TestRootFileSystem(),
            backendClassifier = classifier
        )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun detectConflicts_whenTargetExists_detectsConflict() = runTest {
        val existingFile = File(tempDir, "document.pdf")
        existingFile.createNewFile()

        val source = StorageLocation("/source/document.pdf", "root1")
        val destDir = StorageLocation(tempDir.absolutePath, "root1")

        val conflicts = detector.detectConflicts(listOf(source), destDir)

        assertEquals(1, conflicts.size)
        assertEquals("document.pdf", conflicts.first().targetName)
        assertEquals(false, conflicts.first().isDirectory)
    }

    @Test
    fun detectConflicts_whenFolderExists_detectsFolderConflict() = runTest {
        val existingFolder = File(tempDir, "dest/Projects")
        existingFolder.mkdirs()

        val sourceFolder = File(tempDir, "source/Projects").apply { mkdirs() }

        val sourceDir = StorageLocation(sourceFolder.absolutePath, "root1")
        val destDir = StorageLocation(existingFolder.parentFile?.absolutePath ?: tempDir.absolutePath, "root1")

        val conflicts = detector.detectConflicts(listOf(sourceDir), destDir)

        assertEquals(1, conflicts.size)
        assertEquals("Projects", conflicts.first().targetName)
        assertEquals(true, conflicts.first().isDirectory)
    }

    @Test
    fun detectConflicts_whenMultipleItemsExist_detectsMultipleConflicts() = runTest {
        File(tempDir, "a.txt").createNewFile()
        File(tempDir, "b.txt").createNewFile()

        val sources = listOf(
            StorageLocation("/source/a.txt", "root1"),
            StorageLocation("/source/b.txt", "root1"),
            StorageLocation("/source/c.txt", "root1") // unique, non-conflicting
        )
        val destDir = StorageLocation(tempDir.absolutePath, "root1")

        val conflicts = detector.detectConflicts(sources, destDir)

        assertEquals(2, conflicts.size)
        val conflictNames = conflicts.map { it.targetName }.toSet()
        assertTrue(conflictNames.contains("a.txt"))
        assertTrue(conflictNames.contains("b.txt"))
    }
}
