// [Jalur Class]: com.wakwau.xplore.core.storage.db.AppDatabase
// [Penjelasan]: Room Database utama penyimpan skema FileIndexEntity.
package com.wakwau.xplore.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.db.dao.FileIndexDao
import com.wakwau.xplore.core.storage.db.entity.FileIndexEntity

@Database(
    entities = [
        FileIndexEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileIndexDao(): FileIndexDao
}
