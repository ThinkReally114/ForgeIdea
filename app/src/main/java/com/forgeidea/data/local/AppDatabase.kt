package com.forgeidea.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.forgeidea.data.local.dao.MessageDao
import com.forgeidea.data.local.dao.SessionDao
import com.forgeidea.data.local.entity.MessageEntity
import com.forgeidea.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
}
