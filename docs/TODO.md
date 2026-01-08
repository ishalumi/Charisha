# Charisha - Android LLM 对话客户端 开发进度追踪

> **项目版本**: 1.3.6
> **创建日期**: 2026-01-07
> **最后更新**: 2026-01-08

---

## 状态说明

| 状态 | 图标 | 说明 |
|------|------|------|
| 未开始 | ⬜ | 任务尚未开始 |
| 进行中 | 🔄 | 正在开发中 |
| 已完成 | ✅ | 开发完成并测试通过 |
| 阻塞 | 🚫 | 因依赖或问题被阻塞 |
| 待审核 | 🔍 | 等待代码审核 |

---

## 1. 项目初始化 (Project Setup)

### 1.1 基础配置
- ✅ 创建 Android 项目 (minSdk 26, targetSdk 34)
- ✅ 配置 Gradle 依赖 (Compose BOM, Hilt, Room, Retrofit 等)
- ✅ 配置 Kotlin Serialization 插件
- ✅ 配置 KSP 编译器
- ✅ 创建项目目录结构

### 1.2 主题与样式
- ✅ 配置 Material Design 3 主题
- ✅ 定义颜色方案 (Color.kt)
- ✅ 定义字体样式 (Type.kt)
- ✅ 配置深色/浅色主题切换

---

## 2. 数据层 (Data Layer)

### 2.1 本地数据库 (Room)
- ✅ 创建 AppDatabase.kt
- ✅ 创建 ChannelEntity 及 ChannelDao
- ✅ 创建 ModelEntity 及 ModelDao (复合主键: id + channelId)
- ✅ 创建 ConversationEntity 及 ConversationDao
- ✅ 创建 MessageEntity 及 MessageDao
- ✅ 配置数据库迁移策略 (exportSchema = true)
- ✅ 创建 TypeConverters (枚举、JSON 序列化)
- ✅ 创建 EntityMapper (Entity <-> Domain 映射)

### 2.2 安全存储
- ✅ 实现 SecurePreferences (EncryptedSharedPreferences)
- ✅ API Key 加密存储与读取

### 2.3 远程 API
- ✅ 创建 OpenAI API 接口 (OpenAIApi.kt)
- ✅ 创建 Gemini API 接口 (GeminiApi.kt)
- ✅ 创建 Claude API 接口 (ClaudeApi.kt)
- ✅ 实现 SSE 流式解析器 (SSEParser.kt)
- ✅ 实现 Gemini NDJSON 解析器

### 2.4 DTO 定义
- ✅ OpenAI 请求/响应 DTO
- ✅ Gemini 请求/响应 DTO
- ✅ Claude 请求/响应 DTO

### 2.5 Repository 实现
- ✅ ChannelRepositoryImpl
- ✅ ModelRepositoryImpl
- ✅ ConversationRepositoryImpl
- ✅ MessageRepositoryImpl
- ✅ ChatRepositoryImpl

---

## 3. 领域层 (Domain Layer)

### 3.1 领域模型
- ✅ ProviderType.kt (提供商类型枚举)
- ✅ ProxyType.kt (代理类型枚举)
- ✅ MessageRole.kt (消息角色枚举)
- ✅ Channel.kt (渠道模型)
- ✅ LlmModel.kt (模型配置)
- ✅ Conversation.kt (对话会话)
- ✅ Message.kt (消息)
- ✅ ContentPart.kt (内容块: 文本/图片/文件)
- ✅ StreamEvent.kt (流式事件)

### 3.2 Repository 接口
- ✅ ChannelRepository 接口
- ✅ ModelRepository 接口
- ✅ ConversationRepository 接口
- ✅ MessageRepository 接口
- ✅ ChatRepository 接口

### 3.3 Use Cases
- ✅ CreateChannelUseCase
- ✅ FetchModelsUseCase
- ✅ TestConnectionUseCase
- ✅ SendMessageUseCase
- ✅ RegenerateResponseUseCase
- ✅ CreateBranchUseCase
- ✅ EditMessageUseCase

---

## 4. 依赖注入 (DI)

- ✅ AppModule.kt (全局依赖 - Repository 绑定)
- ✅ DatabaseModule.kt (Room 数据库 + Json 配置)
- ✅ NetworkModule.kt (Retrofit, OkHttp)

---

## 5. UI 层 (Presentation Layer)

### 5.1 导航
- ✅ NavGraph.kt (导航图配置)
- ✅ 定义路由常量

### 5.2 通用组件
- ✅ MessageBubble (消息气泡, 支持多模态)
- ✅ MarkdownText (Markdown 渲染)
- ✅ MessageImage (图片渲染, 支持图床 URL 和 Base64)
- ✅ ThinkingBlock (可折叠思维链, 默认折叠)
- ✅ InputBar (输入框 + 附件 + 发送 + 流式开关)
- ⬜ AttachmentPreview (附件预览)
- ✅ BranchIndicator (分支标识)
- ✅ StreamToggle (流式开关)
- ✅ LoadingIndicator (加载指示器)

### 5.3 聊天模块
- ✅ ChatScreen (主对话界面) - 基础实现
- ✅ ChatViewModel
- ✅ ChatUiState

### 5.4 渠道管理模块
- ✅ ChannelListScreen (渠道列表)
- ✅ ChannelEditScreen (渠道编辑)
- ✅ ChannelViewModel
- ⬜ ModelConfigDialog (模型能力配置)
- ⬜ ImageGenModelSelector (图片生成模型选择)

### 5.5 对话历史模块
- ✅ ConversationListScreen (对话历史列表)
- ✅ ConversationViewModel
- ✅ MessageEditDialog (消息编辑弹窗)

---

## 6. 功能模块 (Feature Modules)

### 6.1 渠道管理
- ✅ 渠道创建流程
- ✅ 模型列表获取 (OpenAI/Gemini/Claude 三种格式)
- ✅ Claude 分页遍历实现
- ✅ 模型能力自动检测
- ⬜ 自定义 API 路径支持

### 6.2 消息编辑与分支
- ✅ 用户消息编辑 (编辑后重新生成)
- ✅ AI 消息编辑 (直接修改)
- ✅ 对话分支创建
- ✅ 分支上下文继承

### 6.3 思维链处理
- ✅ OpenAI o1/o3 reasoning_content 解析
- ✅ Claude thinking 块解析
- ✅ Gemini thought 字段解析
- ✅ 思维链折叠 UI

### 6.4 流式响应控制
- ✅ 渠道级流式开关
- ✅ 模型级流式支持检测
- ✅ 运行时流式切换

### 6.5 多模态输入
- ✅ 图片输入 (相册/拍照/剪贴板/图床 URL)
- ✅ 图片压缩处理
- ✅ PDF 文件输入 (Claude/Gemini 格式适配)
- ✅ 其他文件输入

### 6.6 AI 图片生成
- ✅ OpenAI DALL-E/gpt-image-1 支持
- ✅ Gemini Imagen 支持
- ✅ 用户指定图片生成模型

### 6.7 消息渲染
- ✅ Markdown 完整渲染 (Markwon)
- ✅ 代码块语法高亮 (Prism4j + 明暗双主题)
- ✅ 图床 URL 图片渲染
- ✅ Base64 内联图片渲染

---

## 7. Provider 适配器

### 7.1 统一抽象
- ✅ LLMProvider 接口实现
- ✅ ProviderCapabilities 定义

### 7.2 OpenAI 适配器
- ✅ 请求格式转换
- ✅ SSE 流式解析
- ✅ 图片生成 API

### 7.3 Gemini 适配器
- ✅ 请求格式转换 (contents/parts 结构)
- ✅ NDJSON 流式解析
- ✅ SSE 模式支持 (?alt=sse)
- ✅ Imagen 图片生成

### 7.4 Claude 适配器
- ✅ 请求格式转换
- ✅ SSE 流式解析 (event + data)
- ✅ thinking 块处理
- ✅ PDF document 块支持

---

## 8. 测试 (Testing)

### 8.1 单元测试
- ⬜ Repository 测试
- ⬜ UseCase 测试
- ⬜ ViewModel 测试
- ⬜ SSE 解析器测试

### 8.2 集成测试
- ⬜ 数据库操作测试
- ⬜ API 调用测试

### 8.3 UI 测试
- ⬜ Compose UI 测试
- ⬜ 导航测试

---

## 9. 优化与发布

- ⬜ ProGuard/R8 混淆配置
- ⬜ 性能优化
- ⬜ 内存泄漏检测
- ⬜ 发布签名配置
- ⬜ 版本管理

---

## 变更日志

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-01-07 | 1.0.0 | 初始化 TODO 文档，基于 TECHNICAL_DESIGN.md v1.3.0 |
| 2026-01-07 | 1.0.1 | 完成项目初始化：Gradle 配置、主题、导航、基础 UI 结构 |
| 2026-01-07 | 1.0.2 | 完成领域层 + 数据层基础：Domain Models、Repository 接口、Room Entity/DAO/Database、DI Module |
| 2026-01-07 | 1.0.3 | 完成网络层：SecurePreferences、NetworkModule、三方 API DTO、API 接口、SSE 流式解析器 |
| 2026-01-07 | 1.0.4 | 完成 Repository 实现层：5 个 Repository 实现 + AppModule DI 绑定 |
| 2026-01-07 | 1.0.5 | 完成 Use Cases：7 个用例 (channel/chat/message) |
| 2026-01-07 | 1.0.6 | 完成 UI 基础组件：MessageBubble, MarkdownText, InputBar, LoadingIndicator |
| 2026-01-07 | 1.0.7 | 完成 Chat 模块核心：ChatScreen, ChatViewModel, ChatUiState |
| 2026-01-07 | 1.0.8 | 优化 ThinkingBlock：支持流式显示、自定义标签配置 |
| 2026-01-07 | 1.0.9 | 完成 Channel 管理模块：ListScreen, EditScreen, ViewModel |
| 2026-01-07 | 1.0.10 | 完成 Conversation 历史模块：ListScreen, ViewModel |
| 2026-01-07 | 1.1.0 | 完善渠道管理流程：保存后自动获取模型、连接测试、模型列表展示、默认模型选择、流式开关 |
| 2026-01-07 | 1.2.0 | 完善 ChatScreen：流式响应实时显示、思维链展示、渠道/模型选择、新建对话、停止生成 |
| 2026-01-07 | 1.3.0 | 完成代码高亮：Prism4j 集成、CharishaPrism4jTheme 明暗双主题（Catppuccin 配色） |
| 2026-01-07 | 1.3.1 | 完成图片渲染：MessageImage 组件（URL/Base64/本地文件）、全屏查看器、MessageBubble 多模态支持 |
| 2026-01-07 | 1.3.2 | 代码审查修复：MessageImage 内存优化（Base64 解码/预览降采样/10MB 限制）、URL scheme 安全校验、MarkdownText 性能优化（避免重复解析）、禁用外链图片自动加载 |
| 2026-01-07 | 1.3.3 | Minor 修复：文件重命名统一、Text 段落边界保留、FileAttachment 占位组件、fontSize 动态更新 |
| 2026-01-08 | 1.3.4 | 完成消息编辑/重新生成/分支：长按消息操作面板、MessageEditDialog、分支上下文继承、对话历史跳转修复 |
| 2026-01-08 | 1.3.5 | 完成多模态输入 + Provider 适配器：附件选择/预览/压缩/PDF，LLMProvider 抽象与 OpenAI/Gemini/Claude 多模态请求转换（含流式解析与图片生成接口），修正 baseUrl 版本段与 API 路径拼接 |
| 2026-01-08 | 1.3.6 | 完成 AI 图片生成：生图入口、OpenAI/Gemini 生图调用、生成结果落盘为本地附件、支持设置生图模型（channel.imageGenModelId） |

---

## 备注

- 本文档与 `TECHNICAL_DESIGN.md` 保持同步
- 每完成一个模块，请更新对应状态
- 遇到阻塞问题，请在对应任务后添加说明
