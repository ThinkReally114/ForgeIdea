package com.qz.agent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.qz.agent.data.local.dao.MessageDao
import com.qz.agent.data.local.dao.SessionDao
import com.qz.agent.data.local.entity.MessageEntity
import com.qz.agent.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
}
