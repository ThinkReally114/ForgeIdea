package com.qz.agent

import android.app.Application
import com.qz.agent.di.appModule
import com.qz.agent.di.networkModule
import com.qz.agent.di.uiModule
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
