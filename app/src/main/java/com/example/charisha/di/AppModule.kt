package com.example.charisha.di

import com.example.charisha.data.attachments.AttachmentRepository
import com.example.charisha.data.attachments.AttachmentRepositoryImpl
import com.example.charisha.data.repository.ChannelRepositoryImpl
import com.example.charisha.data.repository.ChatRepositoryImpl
import com.example.charisha.data.repository.ConversationRepositoryImpl
import com.example.charisha.data.repository.MessageRepositoryImpl
import com.example.charisha.data.repository.ModelRepositoryImpl
import com.example.charisha.domain.repository.ChannelRepository
import com.example.charisha.domain.repository.ChatRepository
import com.example.charisha.domain.repository.ConversationRepository
import com.example.charisha.domain.repository.MessageRepository
import com.example.charisha.domain.repository.ModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用级 DI 模块 - Repository 绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
