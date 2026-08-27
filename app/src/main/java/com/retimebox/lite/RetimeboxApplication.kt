package com.retimebox.lite

import android.app.Application
import com.retimebox.lite.data.local.AppDatabase
import com.retimebox.lite.data.repository.FolderRepository
import com.retimebox.lite.data.repository.MediaRepository
import com.retimebox.lite.data.repository.RecordRepository
import com.retimebox.lite.data.repository.SpaceFileRepository
import com.retimebox.lite.data.repository.SpaceLinkRepository
import com.tencent.smtt.sdk.QbSdk

class RetimeboxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        QbSdk.initX5Environment(this, object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {}
            override fun onViewInitFinished(arg0: Boolean) {}
        })
    }

    val database by lazy { AppDatabase.getInstance(this) }
    val folderRepository by lazy {
        FolderRepository(
            database.folderDao(),
            database.mediaItemDao(),
            database.spaceLinkItemDao(),
            database.recordDao()
        )
    }
    val recordRepository by lazy { RecordRepository(database.recordDao(), database.mediaItemDao(), database.spaceLinkItemDao(), database.spaceFileItemDao()) }
    val mediaRepository by lazy { MediaRepository(database.mediaItemDao(), database.recordDao()) }
    val spaceLinkRepository by lazy { SpaceLinkRepository(database.spaceLinkItemDao(), database.recordDao()) }
    val spaceFileRepository by lazy { SpaceFileRepository(database.spaceFileItemDao()) }
}
