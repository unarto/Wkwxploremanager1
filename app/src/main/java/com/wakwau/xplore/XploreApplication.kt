package com.wakwau.xplore

import android.app.Application
import com.wakwau.xplore.di.AppCompositionRoot
import com.wakwau.xplore.core.storage.operation.FileCopyServiceLocator

class XploreApplication : Application() {
    lateinit var appCompositionRoot: AppCompositionRoot
        private set

    override fun onCreate() {
        super.onCreate()
        appCompositionRoot = AppCompositionRoot(this)
        FileCopyServiceLocator.fileRepository = appCompositionRoot.storageModule.fileRepository
    }
}
