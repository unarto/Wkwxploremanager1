// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/provider/InternalVolumeProviderTest.kt
// [Penjelasan]: Unit test untuk InternalVolumeProvider memverifikasi resolusi volume penyimpanan internal.
package com.wakwau.xplore.core.storage.provider

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.core.storage.provider.volume.InternalVolumeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class InternalVolumeProviderTest {

    @Test
    fun getInternalVolume_returnsExpectedItem() {
        val pathResolver = InternalStoragePathResolver()
        val spaceReader = StorageSpaceReader()
        val provider = InternalVolumeProvider(pathResolver, spaceReader)

        val volume = provider.getInternalVolume()

        assertEquals(StorageConstants.PRIMARY_INTERNAL_VOLUME_ID, volume.id)
        assertEquals(StorageConstants.DEFAULT_PRIMARY_VOLUME_NAME, volume.name)
        assertEquals(StorageVolumeType.PRIMARY_INTERNAL, volume.type)
        assertFalse(volume.isReadOnly)
        assertNotNull(volume.rootPath)
    }
}
