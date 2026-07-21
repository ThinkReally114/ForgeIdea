package com.forgeidea.di

import androidx.room.Room
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.data.local.AppDatabase
import com.forgeidea.data.repository.ChatRepository
import com.forgeidea.domain.usecase.SendMessageUseCase
import com.forgeidea.tools.ExecuteCommandTool
import com.forgeidea.tools.WorkspaceFileTools
import com.forgeidea.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "forgeidea.db")
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().sessionDao() }
    single { get<AppDatabase>().messageDao() }
    single { ApiKeyStore(androidContext()) }
    single { ChatRepository(get(), get(), get()) }
    single { ExecuteCommandTool(androidContext()) }
    single { WorkspaceFileTools(androidContext()) }
    single { SendMessageUseCase(get(), get()) }
}
