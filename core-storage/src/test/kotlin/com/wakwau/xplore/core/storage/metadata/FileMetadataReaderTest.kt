package com.wakwau.xplore.core.storage.metadata

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileMetadataReaderTest {

    private lateinit var metadataReader: FileMetadataReader
    private lateinit var tempDir: File

    @Before
    fun setup() {
        metadataReader = FileMetadataReader()
        tempDir = Files.createTempDirectory("metadata_test").toFile()
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun readMetadata_forFile_returnsCorrectMetadata() {
        val file = File(tempDir, "test.txt")
        file.writeText("12345") // 5 bytes
        
        val metadata = metadataReader.readMetadata(file)
        
        assertEquals(5L, metadata.size)
        assertTrue(metadata.modifiedTime > 0L)
        // createdTime is null in current implementation
        assertEquals(file.canRead(), metadata.isReadable)
        assertEquals(file.canWrite(), metadata.isWritable)
        assertEquals(file.canExecute(), metadata.isExecutable)
        assertEquals(file.isHidden, metadata.isHidden)
    }

    @Test
    fun readMetadata_forDirectory_returnsZeroSize() {
        val dir = File(tempDir, "test_dir")
        dir.mkdir()
        
        val metadata = metadataReader.readMetadata(dir)
        
        assertEquals(0L, metadata.size)
    }
}
