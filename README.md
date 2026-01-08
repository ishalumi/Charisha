# Charisha

Android LLM 对话客户端 - 基于 Jetpack Compose 的多模型智能对话应用

## 功能特性

- **多模型支持**: OpenAI、Claude、Gemini 三大主流 LLM 提供商
- **流式响应**: SSE 实时流式输出，支持思维链展示
- **多模态输入**: 支持图片、PDF、文件等多种附件类型
- **AI 图片生成**: 支持 DALL-E、Imagen 等图片生成模型
- **消息分支**: 支持消息编辑和对话分支管理
- **Markdown 渲染**: 完整的 Markdown 支持，代码块语法高亮

## 技术栈

| 类别 | 技术选型 |
|------|---------|
| 语言 | Kotlin 100% |
| UI 框架 | Jetpack Compose |
| 设计规范 | Material Design 3 |
| 架构模式 | Clean Architecture + MVVM |
| 依赖注入 | Hilt |
| 异步处理 | Coroutines + Flow |
| 网络层 | Retrofit + OkHttp |
| 数据库 | Room |

## 构建要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.5

## 快速开始

```bash
# 克隆仓库
git clone https://github.com/ishalumi/Charisha.git

# 进入项目目录
cd Charisha

# 构建 Debug APK
./gradlew assembleDebug
```

## 项目结构

```
app/src/main/java/com/example/charisha/
├── data/           # 数据层 (Repository 实现、API、数据库)
├── domain/         # 领域层 (Use Cases、Repository 接口、模型)
├── presentation/   # UI 层 (Compose Screens、ViewModels)
└── di/             # 依赖注入模块
```

## 许可证

MIT License
