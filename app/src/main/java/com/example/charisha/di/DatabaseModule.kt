package com.example.charisha.di

import android.content.Context
import androidx.room.Room
import com.example.charisha.data.local.converter.Converters
import com.example.charisha.data.local.dao.ChannelDao
import com.example.charisha.data.local.dao.ConversationDao
import com.example.charisha.data.local.dao.MessageDao
import com.example.charisha.data.local.dao.ModelDao
import com.example.charisha.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * 数据库 DI 模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideConverters(json: Json): Converters = Converters(json)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        converters: Converters
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    )
        .addTypeConverter(converters)
        .build()

    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()

    @Provides
    fun provideModelDao(database: AppDatabase): ModelDao = database.modelDao()

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()
}
