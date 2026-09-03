package com.wakwau.xplore.core.storage.db

import android.content.Context
import androidx.room.Room
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.db.dao.FileIndexDao

object DatabaseProvider {
    @Volatile
    private var database: AppDatabase? = null

    private fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                StorageConstants.DATABASE_NAME
            ).fallbackToDestructiveMigration().build().also { database = it }
        }
    }

    fun provideFileIndexDao(context: Context): FileIndexDao {
        return getDatabase(context).fileIndexDao()
    }
}
