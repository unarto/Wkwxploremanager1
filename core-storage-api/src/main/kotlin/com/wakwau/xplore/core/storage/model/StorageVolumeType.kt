package com.wakwau.xplore.core.storage.model

// [Jalur Class]: com.wakwau.xplore.core.storage.model.StorageVolumeType
// [Penjelasan]: Enum untuk merepresentasikan tipe storage volume, ditambahkan ROOT.

enum class StorageVolumeType {
    PRIMARY_INTERNAL,
    SECONDARY_SDCARD,
    USB_OTG,
    ROOT,
    SAF_PROVIDER,
    UNKNOWN
}
