package com.qz.agent.di

import androidx.room.Room
import com.qz.agent.data.local.AppDatabase
import com.qz.agent.data.local.dao.MessageDao
import com.qz.agent.data.local.dao.SessionDao
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "qz_agent.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    single<SessionDao> { get<AppDatabase>().sessionDao() }
    single<MessageDao> { get<AppDatabase>().messageDao() }
}
