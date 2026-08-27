package com.retimebox.lite.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.retimebox.lite.data.local.converter.Converters
import com.retimebox.lite.data.local.dao.FolderDao
import com.retimebox.lite.data.local.dao.MediaItemDao
import com.retimebox.lite.data.local.dao.RecordDao
import com.retimebox.lite.data.local.dao.SpaceFileItemDao
import com.retimebox.lite.data.local.dao.SpaceLinkItemDao
import com.retimebox.lite.data.local.entity.Folder
import com.retimebox.lite.data.local.entity.MediaItem
import com.retimebox.lite.data.local.entity.Record
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.retimebox.lite.util.FileHelper
import java.io.File

@Database(
    entities = [
        Folder::class,
        Record::class,
        MediaItem::class,
        SpaceLinkItem::class,
        SpaceFileItem::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun recordDao(): RecordDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun spaceLinkItemDao(): SpaceLinkItemDao
    abstract fun spaceFileItemDao(): SpaceFileItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }

        fun closeAndClearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val dbFile = File(FileHelper.getDbDir(context), "retimebox_lite.db")
            return Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
