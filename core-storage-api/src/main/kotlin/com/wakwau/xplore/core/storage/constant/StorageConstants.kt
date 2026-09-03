// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/constant/StorageConstants.kt
// [Penjelasan]: Menambahkan konstanta virtual node search, ID search prefix, scheme URI, ID storage root, sdcard, unknown error, dan default volume names untuk standarisasi domain node tanpa hardcoding.
package com.wakwau.xplore.core.storage.constant

object StorageConstants {
    const val DATABASE_NAME = "xplore_filemanager.db"
    val DEFAULT_PRIMARY_STORAGE_PATH = System.getenv("EXTERNAL_STORAGE") ?: "/storage/emulated/0"
    const val ROOT_PATH = "/"
    const val PRIMARY_INTERNAL_VOLUME_ID = "primary_internal"
    const val DEFAULT_PRIMARY_VOLUME_NAME = "Internal Storage"
    const val DEFAULT_EXTERNAL_VOLUME_NAME = "External Storage"
    const val DEFAULT_SAF_VOLUME_NAME = "SAF Storage"
    const val DEFAULT_UNKNOWN_FILE_NAME = "Unknown"
    const val DEFAULT_UNKNOWN_ERROR = "Unknown error"
    const val EMPTY_NODE_ID_SUFFIX = "/__empty__"

    const val VIRTUAL_SEARCH_ROOT_ID = "virtual_search_results"
    const val VIRTUAL_SEARCH_LOCATION_ID = "virtual_search"
    const val SEARCH_RESULT_ID_PREFIX = "search_res_"
    const val DEFAULT_SEARCH_RESULTS_NAME = "Search results"
    const val SEARCH_RESULTS_PREFIX = "Search results: "

    const val SDCARD_STORAGE_ID = "sdcard"
    const val ROOT_STORAGE_ID = "root_storage"
    const val ROOT_LEGACY_ID = "root"
    const val ROOT_STORAGE_NAME = "Root"
    const val UNKNOWN_ROOT_ID = "unknown"
    const val CONTENT_SCHEME_PREFIX = "content://"
    const val FILE_SCHEME = "file"
    const val ANDROID_DATA_PATH_SEGMENT = "/Android/data/"
    const val EMULATED_PATH_SEGMENT = "emulated"
    const val DEFAULT_MIME_TYPE_ALL = "*/*"

    object Preferences {
        const val KEY_THEME_MODE = "setting_theme_mode"
        const val KEY_LANGUAGE = "setting_language"
        const val KEY_FILE_SYSTEM_ACCESS_MODE = "setting_file_system_access_mode"
        const val KEY_ROOT_READ_ONLY = "setting_root_read_only"
        const val KEY_SORT_ORDER = "pref_sort_order"
        const val KEY_SORT_DIRECTION = "pref_sort_direction"
        const val KEY_LAYOUT_MODE = "pref_layout_mode"
        const val KEY_SHOW_HIDDEN_FILES = "pref_show_hidden_files"
        const val KEY_LAST_VISITED_PATH = "pref_last_visited_path"
        const val KEY_SEARCH_HISTORY = "pref_search_history"
    }

    object Buffer {
        const val DEFAULT_I_O_BUFFER_SIZE_BYTES = 131072
    }

    object ChecksumAlgorithm {
        const val MD5 = "MD5"
        const val SHA1 = "SHA-1"
        const val SHA256 = "SHA-256"
        const val HEX_CHARS = "0123456789abcdef"
    }

    object Naming {
        const val CONFLICT_NAME_FORMAT = "%s (%d)%s"
        const val CONFLICT_DIR_FORMAT = "%s (%d)"
        const val EXTENSION_SEPARATOR = '.'
        const val FIRST_COPY_INDEX = 1
    }
}

