// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizerTest.kt
// [Penjelasan]: Unit test untuk FileIndexSynchronizer dalam menyinkronkan penambahan, pembaruan, dan penghapusan entitas indeks Room DB.
package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.db.entity.FileIndexEntity
import com.wakwau.xplore.core.storage.db.repository.FileIndexRepository
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileIndexSynchronizerTest {

    private lateinit var fakeRepository: FakeFileIndexRepository
    private lateinit var synchronizer: FileIndexSynchronizer

    @Before
    fun setup() {
        fakeRepository = FakeFileIndexRepository()
        synchronizer = FileIndexSynchronizer(fakeRepository)
    }

    @Test
    fun syncSingle_addsEntityToRepository() = runTest {
        val item = FileItem(
            id = "/storage/emulated/0/test.txt",
            name = "test.txt",
            location = StorageLocation("/storage/emulated/0/test.txt"),
            type = FileType.FILE,
            metadata = FileMetadata.EMPTY.copy(size = 100L, modifiedTime = 12345L)
        )

        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizerTest.kt
        // [Penjelasan]: Menguji bahwa syncSingle mengonversi FileItem dan menyimpan entitas ke dalam repository.
        synchronizer.syncSingle(item)

        assertEquals(1, fakeRepository.indexMap.size)
        val entity = fakeRepository.indexMap["/storage/emulated/0/test.txt"]
        assertTrue(entity != null)
        assertEquals("test.txt", entity?.fileName)
        assertEquals("txt", entity?.extension)
        assertEquals(100L, entity?.size)
    }

    @Test
    fun removeSingle_removesEntityFromRepository() = runTest {
        val path = "/storage/emulated/0/doc.pdf"
        fakeRepository.indexMap[path] = FileIndexEntity(
            filePath = path,
            fileName = "doc.pdf",
            size = 200L,
            extension = "pdf",
            category = "DOCUMENT",
            dateModified = 1000L,
            isDirectory = false
        )

        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizerTest.kt
        // [Penjelasan]: Menguji bahwa removeSingle menghapus baris spesifik berdasarkan path dari repository.
        synchronizer.removeSingle(path)

        assertEquals(0, fakeRepository.indexMap.size)
    }

    @Test
    fun removeByPrefix_removesMatchingHierarchy() = runTest {
        val prefix = "/storage/emulated/0/folder"
        fakeRepository.indexMap["$prefix/a.txt"] = FileIndexEntity("$prefix/a.txt", "a.txt", 10L, "txt", "DOCUMENT", 1000L, false)
        fakeRepository.indexMap["$prefix/b.txt"] = FileIndexEntity("$prefix/b.txt", "b.txt", 20L, "txt", "DOCUMENT", 1000L, false)
        fakeRepository.indexMap["/storage/emulated/0/other.png"] = FileIndexEntity("/storage/emulated/0/other.png", "other.png", 30L, "png", "IMAGE", 1000L, false)

        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizerTest.kt
        // [Penjelasan]: Menguji bahwa removeByPrefix menghapus semua entitas indeks yang berawalan dengan direktori terkait.
        synchronizer.removeByPrefix(prefix)

        assertEquals(1, fakeRepository.indexMap.size)
        assertTrue(fakeRepository.indexMap.containsKey("/storage/emulated/0/other.png"))
    }

    @Test
    fun syncBatch_addsMultipleEntitiesInBatch() = runTest {
        val items = listOf(
            FileItem(
                id = "/storage/emulated/0/f1.txt",
                name = "f1.txt",
                location = StorageLocation("/storage/emulated/0/f1.txt"),
                type = FileType.FILE,
                metadata = FileMetadata.EMPTY.copy(size = 10L, modifiedTime = 100L)
            ),
            FileItem(
                id = "/storage/emulated/0/f2.jpg",
                name = "f2.jpg",
                location = StorageLocation("/storage/emulated/0/f2.jpg"),
                type = FileType.FILE,
                metadata = FileMetadata.EMPTY.copy(size = 20L, modifiedTime = 200L)
            )
        )

        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizerTest.kt
        // [Penjelasan]: Menguji bahwa syncBatch menambahkan semua berkas secara batch.
        synchronizer.syncBatch(items)

        assertEquals(2, fakeRepository.indexMap.size)
        assertTrue(fakeRepository.indexMap.containsKey("/storage/emulated/0/f1.txt"))
        assertTrue(fakeRepository.indexMap.containsKey("/storage/emulated/0/f2.jpg"))
    }

    @Test
    fun syncRename_atomicallyUpdatesIndex() = runTest {
        val oldPath = "/storage/emulated/0/old.txt"
        fakeRepository.indexMap[oldPath] = FileIndexEntity(oldPath, "old.txt", 15L, "txt", "DOCUMENT", 100L, false)

        val newItem = FileItem(
            id = "/storage/emulated/0/renamed.txt",
            name = "renamed.txt",
            location = StorageLocation("/storage/emulated/0/renamed.txt"),
            type = FileType.FILE,
            metadata = FileMetadata.EMPTY.copy(size = 15L, modifiedTime = 200L)
        )

        // [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizerTest.kt
        // [Penjelasan]: Menguji bahwa syncRename menghapus path lama dan menambahkan entitas baru secara konsisten.
        synchronizer.syncRename(oldPath, newItem)

        assertEquals(1, fakeRepository.indexMap.size)
        assertTrue(!fakeRepository.indexMap.containsKey(oldPath))
        assertTrue(fakeRepository.indexMap.containsKey("/storage/emulated/0/renamed.txt"))
    }
}

private class FakeFileIndexRepository : FileIndexRepository {
    val indexMap = mutableMapOf<String, FileIndexEntity>()

    override suspend fun addOrUpdateIndex(entity: FileIndexEntity) {
        indexMap[entity.filePath] = entity
    }

    override suspend fun addOrUpdateIndexBatch(entities: List<FileIndexEntity>) {
        entities.forEach { indexMap[it.filePath] = it }
    }

    override suspend fun removeIndex(filePath: String) {
        indexMap.remove(filePath)
        val prefix = if (filePath.endsWith("/")) filePath else "$filePath/"
        indexMap.keys.removeAll { it.startsWith(prefix) }
    }

    override suspend fun removeIndexBatch(filePaths: List<String>) {
        filePaths.forEach { removeIndex(it) }
    }

    override suspend fun removeIndexByPrefix(locationPrefix: String) {
        indexMap.keys.removeAll { it.startsWith(locationPrefix) }
    }

    override suspend fun removeIndexByPrefixes(locationPrefixes: List<String>) {
        locationPrefixes.forEach { removeIndexByPrefix(it) }
    }

    override suspend fun replacePrefixIndex(locationPrefix: String, entities: List<FileIndexEntity>) {
        removeIndexByPrefix(locationPrefix)
        addOrUpdateIndexBatch(entities)
    }

    override suspend fun syncRename(oldPath: String, newEntity: FileIndexEntity) {
        removeIndex(oldPath)
        addOrUpdateIndex(newEntity)
    }

    override suspend fun syncMove(sourcePath: String, destinationEntity: FileIndexEntity) {
        removeIndex(sourcePath)
        addOrUpdateIndex(destinationEntity)
    }

    override suspend fun clearIndex() {
        indexMap.clear()
    }

    override fun searchFiles(
        locationPrefix: String,
        keyword: String,
        minSize: Long?,
        maxSize: Long?,
        extension: String?
    ): Flow<List<FileIndexEntity>> {
        return flowOf(indexMap.values.filter { 
            it.fileName.contains(keyword, ignoreCase = true) && 
            it.filePath.startsWith(locationPrefix)
        })
    }

    override fun getFilesByCategory(category: String): Flow<List<FileIndexEntity>> {
        return flowOf(indexMap.values.filter { it.category == category })
    }
}
