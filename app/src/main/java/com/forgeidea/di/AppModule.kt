package com.forgeidea.di

import androidx.room.Room
import com.forgeidea.data.local.AppDatabase
import com.forgeidea.data.local.dao.MessageDao
import com.forgeidea.data.local.dao.SessionDao
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "forgeidea.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    single<SessionDao> { get<AppDatabase>().sessionDao() }
    single<MessageDao> { get<AppDatabase>().messageDao() }
}
