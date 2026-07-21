package com.forgeidea

import android.app.Application
import com.forgeidea.di.appModule
import com.forgeidea.di.networkModule
import com.forgeidea.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AgentApplication)
            modules(appModule, networkModule, uiModule)
        }
    }
}
