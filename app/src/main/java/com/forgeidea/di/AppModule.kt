package com.forgeidea.di

import androidx.room.Room
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.data.local.AppDatabase
import com.forgeidea.data.local.dao.MessageDao
import com.forgeidea.data.local.dao.SessionDao
import com.forgeidea.data.repository.ChatRepository
import com.forgeidea.git.GitTool
import com.forgeidea.terminal.ProotShellExecutor
import com.forgeidea.terminal.ShellExecutor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "forgeidea.db")
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
    single<SessionDao> { get<AppDatabase>().sessionDao() }
    single<MessageDao> { get<AppDatabase>().messageDao() }
    single { ChatRepository(get(), get(), get()) }
    single<ShellExecutor> { ProotShellExecutor(androidContext(), get()) }
    single { GitTool(androidContext().filesDir.resolve("workspace")) }
}
