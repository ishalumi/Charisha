package com.example.charisha.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.charisha.data.local.converter.Converters
import com.example.charisha.data.local.dao.ChannelDao
import com.example.charisha.data.local.dao.ConversationDao
import com.example.charisha.data.local.dao.MessageDao
import com.example.charisha.data.local.dao.ModelDao
import com.example.charisha.data.local.entity.ChannelEntity
import com.example.charisha.data.local.entity.ConversationEntity
import com.example.charisha.data.local.entity.MessageEntity
import com.example.charisha.data.local.entity.ModelEntity

/**
 * Room 数据库
 */
@Database(
    entities = [
        ChannelEntity::class,
        ModelEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun channelDao(): ChannelDao
    abstract fun modelDao(): ModelDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        const val DATABASE_NAME = "charisha.db"
    }
}
