package com.wakwau.xplore.core.storage.mapper

import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class FileItemMapperTest {

    @Test
    fun map_mapsToTargetProperly() {
        val mapper = FileItemMapper()
        val location = StorageLocation(path = "/some/path", rootId = "root")
        val metadata = FileMetadata(100L, 1000L, null, true, true, false, false)
        
        val fileItem = mapper.map(
            id = "id1",
            name = "file.txt",
            location = location,
            type = FileType.FILE,
            metadata = metadata
        )
        
        assertEquals("id1", fileItem.id)
        assertEquals("file.txt", fileItem.name)
        assertEquals(location, fileItem.location)
        assertEquals(FileType.FILE, fileItem.type)
        assertEquals(metadata, fileItem.metadata)
    }
}
