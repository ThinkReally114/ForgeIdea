# 移动端编程 Agent - UI 层 + LLM 客户端 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建安卓项目骨架，实现 QZMusic 风格 UI 层（聊天界面 + 代码查看器 + 划选评论）和 LLM 客户端（Zen API 流式调用），跑通"发消息收到 AI 回复"的最小闭环。

**Architecture:** 原生 Kotlin + Jetpack Compose + Material 3。五层架构中的 UI 层和 LLM 客户端先行，Agent 引擎用最小桩实现（直接转发用户消息给 LLM，无工具调用）。UI 采用 QZ 风格视觉系统（Haze 毛玻璃 + 纯色深色背景 + M3Color 动态取色 + 胶囊组件）。LLM 客户端用 Ktor + SSE 流式调 OpenCode Zen API。

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Haze, M3Color, Capsule, dotlottie-android, Ktor Client, kotlinx.serialization, Room, DataStore, Coil, Koin

**参考设计文档:** `docs/superpowers/specs/2026-07-20-android-agent-design.md`

---

## 文件结构总览

```
app/
├── build.gradle.kts                      # 应用模块 Gradle 配置
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/qz/agent/
│   │   ├── AgentApplication.kt           # Application 类，Koin 初始化
│   │   ├── di/
│   │   │   ├── AppModule.kt             # 数据层 DI
│   │   │   ├── NetworkModule.kt          # LLM 客户端 DI
│   │   │   └── UiModule.kt               # UI 相关 DI
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.kt        # Room 数据库
│   │   │   │   ├── dao/
│   │   │   │   │   ├── MessageDao.kt     # 消息 DAO
│   │   │   │   │   └── SessionDao.kt     # 会话 DAO
│   │   │   │   └── entity/
│   │   │   │       ├── MessageEntity.kt
│   │   │   │       └── SessionEntity.kt
│   │   │   ├── datastore/
│   │   │   │   ├── ApiKeyStore.kt        # 加密存储 API key
│   │   │   │   └── SettingsStore.kt      # 设置存储
│   │   │   └── repository/
│   │   │       ├── ChatRepository.kt    # 聊天仓库
│   │   │       └── SettingsRepository.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Message.kt           # 消息领域模型
│   │   │   │   ├── Session.kt           # 会话领域模型
│   │   │   │   ├── ChatRole.kt          # 角色(system/user/assistant/tool)
│   │   │   │   └── Theme.kt             # 主题模型
│   │   │   └── usecase/
│   │   │       ├── SendMessageUseCase.kt # 发送消息(最小桩)
│   │   │       └── GetSessionsUseCase.kt
│   │   ├── llm/
│   │   │   ├── LlmClient.kt              # LLM 客户端接口
│   │   │   ├── OpenAiCompatibleClient.kt # OpenAI 兼容实现
│   │   │   ├── ZenConfig.kt             # Zen 配置
│   │   │   ├── model/
│   │   │   │   ├── ChatRequest.kt       # 请求 DTO
│   │   │   │   ├── ChatResponse.kt      # 响应 DTO
│   │   │   │   └── StreamChunk.kt       # 流式分片
│   │   │   └── stream/
│   │   │       └── SseParser.kt         # SSE 解析器
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Theme.kt             # QZ 主题入口
│   │   │   │   ├── ColorSchemes.kt      # 预设主题色板
│   │   │   │   ├── Type.kt              # 字体
│   │   │   │   └── Shapes.kt            # 形状(胶囊等)
│   │   │   ├── components/
│   │   │   │   ├── MessageBubble.kt     # 消息气泡(毛玻璃)
│   │   │   │   ├── CodeBlock.kt         # 代码块
│   │   │   │   ├── CodeViewer.kt        # 代码查看器(带选择)
│   │   │   │   ├── CommentSheet.kt      # 划选评论面板
│   │   │   │   ├── ChatInput.kt         # 胶囊输入框
│   │   │   │   ├── ToolTag.kt           # 工具调用胶囊标签
│   │   │   │   └── CapsuleButton.kt     # 胶囊按钮
│   │   │   ├── chat/
│   │   │   │   ├── ChatScreen.kt        # 聊天主屏
│   │   │   │   └── ChatViewModel.kt     # 聊天 VM
│   │   │   ├── settings/
│   │   │   │   ├── SettingsScreen.kt   # 设置页
│   │   │   │   └── SettingsViewModel.kt
│   │   │   ├── sessions/
│   │   │   │   ├── SessionsScreen.kt    # 会话列表
│   │   │   │   └── SessionsViewModel.kt
│   │   │   └── MainActivity.kt         # 入口 + Navigation
│   │   └── util/
│   │       ├── TokenEstimator.kt        # token 估算(留给后续压缩用)
│   │       └── MarkdownRenderer.kt      # Markdown 渲染
│   └── res/
│       ├── values/
│       │   ├── strings.xml
│       │   ├── colors.xml
│       │   └── themes.xml
│       └── assets/
│           └── lottie/                  # Lottie 动画文件
build.gradle.kts                         # 根 Gradle 配置
settings.gradle.kts                      # 项目设置
gradle/libs.versions.toml               # 依赖版本目录
```

---

## Task 1: 项目骨架与 Gradle 配置

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/qz/agent/AgentApplication.kt`

- [ ] **Step 1: 创建 `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "qz-agent"
include(":app")
```

- [ ] **Step 2: 创建根 `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: 创建 `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
ksp = "2.0.0-1.0.21"
compose-bom = "2024.06.00"
lifecycle = "2.8.2"
activity-compose = "1.9.0"
navigation = "2.7.7"
ktor = "2.3.11"
serialization = "1.6.3"
room = "2.6.1"
datastore = "1.1.1"
coil = "2.6.0"
koin = "3.5.6"
haze = "0.7.2"
m3color = "1.0.0"
capsule = "1.0.1"
dotlottie = "0.0.9"
security-crypto = "1.1.0-alpha06"

[libraries]
androidx-core-ktx = "androidx.core:core-ktx:1.13.1"
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }

compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }

ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-sse = { module = "io.ktor:ktor-client-sse", version.ref = "ktor" }

kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "security-crypto" }

coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }

koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }

haze = { module = "dev.chrisbanes.haze:haze", version.ref = "haze" }
m3color = { module = "com.github.kyant0:m3color", version.ref = "m3color" }
capsule = { module = "com.github.kyant0:capsule", version.ref = "capsule" }
dotlottie-android = { module = "com.github.lottiefiles:dotlottie-android", version.ref = "dotlottie" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 4: 创建 `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.qz.agent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.qz.agent"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.sse)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    implementation(libs.coil.compose)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.haze)
    implementation(libs.m3color)
    implementation(libs.capsule)
    implementation(libs.dotlottie.android)
}
```

- [ ] **Step 5: 创建 `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <application
        android:name=".AgentApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.QZAgent"
        android:usesCleartextTraffic="false">
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.QZAgent">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: 创建 `AgentApplication.kt`**

```kotlin
package com.qz.agent

import android.app.Application
import com.qz.agent.di.appModule
import com.qz.agent.di.networkModule
import com.qz.agent.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AgentApplication)
            modules(appModule, networkModule, uiModule)
        }
    }
}
```

- [ ] **Step 7: 创建空 DI 模块占位文件（避免编译错误）**

创建 `app/src/main/java/com/qz/agent/di/AppModule.kt`、`NetworkModule.kt`、`UiModule.kt`，内容暂时为空 module：

```kotlin
package com.qz.agent.di
import org.koin.dsl.module
val appModule = module { }
val networkModule = module { }
val uiModule = module { }
```

- [ ] **Step 8: 创建基础资源文件**

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">QZ Agent</string>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.QZAgent" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
    </style>
</resources>
```

- [ ] **Step 9: 验证 Gradle sync 和 build 通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL（如遇依赖拉取失败，检查 jitpack 仓库配置）

- [ ] **Step 10: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle/ app/
git commit -m "feat: 初始化安卓项目骨架

- Gradle Kotlin DSL 配置 + 版本目录
- 引入 Compose/Material3/Ktor/Room/DataStore/Koin 依赖
- 引入 QZ 风格视觉依赖(Haze/M3Color/Capsule/dotlottie)
- AgentApplication + Koin 初始化
- AndroidManifest 声明 INTERNET 权限

Author: ThinkReally114"
```

---

## Task 2: 领域模型定义

**Files:**
- Create: `app/src/main/java/com/qz/agent/domain/model/ChatRole.kt`
- Create: `app/src/main/java/com/qz/agent/domain/model/Message.kt`
- Create: `app/src/main/java/com/qz/agent/domain/model/Session.kt`
- Create: `app/src/main/java/com/qz/agent/domain/model/Theme.kt`

- [ ] **Step 1: 创建 `ChatRole.kt`**

```kotlin
package com.qz.agent.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    SYSTEM, USER, ASSISTANT, TOOL
}
```

- [ ] **Step 2: 创建 `Message.kt`**

```kotlin
package com.qz.agent.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)
```

- [ ] **Step 3: 创建 `Session.kt`**

```kotlin
package com.qz.agent.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String = "opencode/big-pickle",
    val messageCount: Int = 0
)
```

- [ ] **Step 4: 创建 `Theme.kt`**

```kotlin
package com.qz.agent.domain.model

enum class PresetTheme(val displayName: String) {
    QZ_PURPLE("QZ 紫"),
    DEEP_SPACE("深空蓝"),
    AURORA_GREEN("极光绿"),
    WARM_ORANGE("暖橙")
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/qz/agent/domain/
git commit -m "feat: 定义领域模型

- ChatRole(system/user/assistant/tool)
- Message + ToolCall
- Session
- PresetTheme(4 个预设主题)

Author: ThinkReally114"
```

---

## Task 3: QZ 风格主题系统

**Files:**
- Create: `app/src/main/java/com/qz/agent/ui/theme/ColorSchemes.kt`
- Create: `app/src/main/java/com/qz/agent/ui/theme/Shapes.kt`
- Create: `app/src/main/java/com/qz/agent/ui/theme/Type.kt`
- Create: `app/src/main/java/com/qz/agent/ui/theme/Theme.kt`

- [ ] **Step 1: 创建 `ColorSchemes.kt` - 预设主题色板（含纯色背景）**

```kotlin
package com.qz.agent.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.qz.agent.domain.model.PresetTheme

object ColorSchemes {
    val QzPurpleDark = darkColorScheme(
        primary = Color(0xFFC4A3FF),
        onPrimary = Color(0xFF1A1B2E),
        primaryContainer = Color(0xFF2D1B4E),
        onPrimaryContainer = Color(0xFFEDE9FE),
        secondary = Color(0xFF7FD4FF),
        onSecondary = Color(0xFF0F1A2E),
        secondaryContainer = Color(0xFF1B3A4E),
        background = Color(0xFF0F0F1E),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF1A1B2E),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF27272A),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF3F3F46)
    )

    val DeepSpaceDark = darkColorScheme(
        primary = Color(0xFF7FD4FF),
        onPrimary = Color(0xFF0F1A2E),
        primaryContainer = Color(0xFF0C2740),
        onPrimaryContainer = Color(0xFFE0F2FE),
        secondary = Color(0xFFC4A3FF),
        onSecondary = Color(0xFF1A1B2E),
        background = Color(0xFF050A14),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF0A1428),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF162238),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF2E4057)
    )

    val AuroraGreenDark = darkColorScheme(
        primary = Color(0xFF7FFFD4),
        onPrimary = Color(0xFF0A1F1A),
        primaryContainer = Color(0xFF0F2E26),
        onPrimaryContainer = Color(0xFFD1FAE5),
        secondary = Color(0xFFC4A3FF),
        onSecondary = Color(0xFF1A1B2E),
        background = Color(0xFF0A1410),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF0F1A14),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF1A2A20),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF2E4035)
    )

    val WarmOrangeDark = darkColorScheme(
        primary = Color(0xFFFFB877),
        onPrimary = Color(0xFF2A1400),
        primaryContainer = Color(0xFF3D1F00),
        onPrimaryContainer = Color(0xFFFFE0C2),
        secondary = Color(0xFFFF7F7F),
        onSecondary = Color(0xFF2A0000),
        background = Color(0xFF1A0F0A),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF241410),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF33201A),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF4D322A)
    )

    fun forTheme(theme: PresetTheme): ColorScheme = when (theme) {
        PresetTheme.QZ_PURPLE -> QzPurpleDark
        PresetTheme.DEEP_SPACE -> DeepSpaceDark
        PresetTheme.AURORA_GREEN -> AuroraGreenDark
        PresetTheme.WARM_ORANGE -> WarmOrangeDark
    }
}
```

- [ ] **Step 2: 创建 `Shapes.kt` - 胶囊形状定义**

```kotlin
package com.qz.agent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val QzShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraSmall = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val CapsuleShape = RoundedCornerShape(50)
```

- [ ] **Step 3: 创建 `Type.kt` - 字体**

```kotlin
package com.qz.agent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val QzTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

val CodeTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 20.sp
)
```

- [ ] **Step 4: 创建 `Theme.kt` - 主题入口（纯色背景）**

```kotlin
package com.qz.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.qz.agent.domain.model.PresetTheme

@Composable
fun QZAgentTheme(
    theme: PresetTheme = PresetTheme.QZ_PURPLE,
    content: @Composable () -> Unit
) {
    val colorScheme = ColorSchemes.forTheme(theme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QzTypography,
        shapes = QzShapes,
        content = {
            Surface(
                modifier = Modifier,
                color = colorScheme.background,
                content = content
            )
        }
    )
}
```

- [ ] **Step 5: 验证编译通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/qz/agent/ui/theme/
git commit -m "feat: 实现 QZ 风格主题系统

- 4 个预设主题色板(紫/蓝/绿/橙)深色优先
- 每主题预设纯色背景(无渐变)
- 胶囊形状 + 字体配置
- QZAgentTheme 主题入口,Surface 用 colorScheme.background

Author: ThinkReally114"
```

---

## Task 4: 胶囊按钮与消息气泡组件

**Files:**
- Create: `app/src/main/java/com/qz/agent/ui/components/CapsuleButton.kt`
- Create: `app/src/main/java/com/qz/agent/ui/components/MessageBubble.kt`

- [ ] **Step 1: 创建 `CapsuleButton.kt`**

```kotlin
package com.qz.agent.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qz.agent.ui.theme.CapsuleShape

@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CapsuleShape,
        colors = if (isPrimary) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }
}
```

- [ ] **Step 2: 创建 `MessageBubble.kt` - 毛玻璃消息气泡**

```kotlin
package com.qz.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qz.agent.domain.model.ChatRole
import com.qz.agent.domain.model.Message

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            if (!isUser) {
                Text(
                    text = "Agent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private val Arrangement.End get() = androidx.compose.foundation.layout.Arrangement.End
private val Arrangement.Start get() = androidx.compose.foundation.layout.Arrangement.Start
```

- [ ] **Step 3: 修复 import（上面用了 Arrangement 的简化引用，实际应直接 import）**

修正 `MessageBubble.kt` 的 import：

```kotlin
package com.qz.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.qz.agent.domain.model.ChatRole
import com.qz.agent.domain.model.Message

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            if (!isUser) {
                Text(
                    text = "Agent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

- [ ] **Step 4: 验证编译通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/qz/agent/ui/components/CapsuleButton.kt app/src/main/java/com/qz/agent/ui/components/MessageBubble.kt
git commit -m "feat: 添加胶囊按钮和消息气泡组件

- CapsuleButton: 主/次样式,胶囊形状
- MessageBubble: 用户/Agent 区分,半透明气泡,QZ 风格

Author: ThinkReally114"
```

---

## Task 5: 聊天输入框组件

**Files:**
- Create: `app/src/main/java/com/qz/agent/ui/components/ChatInput.kt`

- [ ] **Step 1: 创建 `ChatInput.kt` - 胶囊输入框 + 发送按钮**

```kotlin
package com.qz.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.qz.agent.ui.theme.CapsuleShape

@Composable
fun ChatInput(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "输入消息...",
    enabled: Boolean = true
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CapsuleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                enabled = enabled
            )
        }
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text.trim())
                    text = ""
                }
            },
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            enabled = enabled && text.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "发送",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/qz/agent/ui/components/ChatInput.kt
git commit -m "feat: 添加胶囊聊天输入框组件

- 胶囊形状输入框 + 圆形发送按钮
- 渐变主色发送按钮
- 空文本时显示 placeholder

Author: ThinkReally114"
```

---

## Task 6: 数据层 - Room 数据库与 DAO

**Files:**
- Create: `app/src/main/java/com/qz/agent/data/local/entity/SessionEntity.kt`
- Create: `app/src/main/java/com/qz/agent/data/local/entity/MessageEntity.kt`
- Create: `app/src/main/java/com/qz/agent/data/local/dao/SessionDao.kt`
- Create: `app/src/main/java/com/qz/agent/data/local/dao/MessageDao.kt`
- Create: `app/src/main/java/com/qz/agent/data/local/AppDatabase.kt`

- [ ] **Step 1: 创建 `SessionEntity.kt`**

```kotlin
package com.qz.agent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String,
    val messageCount: Int
)
```

- [ ] **Step 2: 创建 `MessageEntity.kt`**

```kotlin
package com.qz.agent.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val toolCallId: String?
)
```

- [ ] **Step 3: 创建 `SessionDao.kt`**

```kotlin
package com.qz.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qz.agent.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}
```

- [ ] **Step 4: 创建 `MessageDao.kt`**

```kotlin
package com.qz.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qz.agent.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: String): Int
}
```

- [ ] **Step 5: 创建 `AppDatabase.kt`**

```kotlin
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
```

- [ ] **Step 6: 验证编译通过（KSP 生成 Room 代码）**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/qz/agent/data/local/
git commit -m "feat: 实现 Room 数据库与会话/消息 DAO

- SessionEntity + MessageEntity(外键关联)
- SessionDao: CRUD + observeAll Flow
- MessageDao: 按会话查询 + 计数
- AppDatabase Room 数据库入口

Author: ThinkReally114"
```

---

## Task 7: API Key 加密存储

**Files:**
- Create: `app/src/main/java/com/qz/agent/data/datastore/ApiKeyStore.kt`

- [ ] **Step 1: 创建 `ApiKeyStore.kt` - 使用 Android Keystore 加密**

```kotlin
package com.qz.agent.data.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getZenApiKey(): String? = prefs.getString(KEY_ZEN, null)

    fun setZenApiKey(key: String?) {
        if (key == null) prefs.edit().remove(KEY_ZEN).apply()
        else prefs.edit().putString(KEY_ZEN, key).apply()
    }

    fun getExaApiKey(): String? = prefs.getString(KEY_EXA, null)

    fun setExaApiKey(key: String?) {
        if (key == null) prefs.edit().remove(KEY_EXA).apply()
        else prefs.edit().putString(KEY_EXA, key).apply()
    }

    companion object {
        private const val KEY_ZEN = "zen_api_key"
        private const val KEY_EXA = "exa_api_key"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/qz/agent/data/datastore/ApiKeyStore.kt
git commit -m "feat: 实现 API Key 加密存储

- 基于 EncryptedSharedPreferences + Android Keystore
- 支持 Zen API key 和 Exa API key
- AES256_GCM 加密

Author: ThinkReally114"
```

---

## Task 8: LLM 客户端 - DTO 与 SSE 解析器

**Files:**
- Create: `app/src/main/java/com/qz/agent/llm/model/ChatRequest.kt`
- Create: `app/src/main/java/com/qz/agent/llm/model/ChatResponse.kt`
- Create: `app/src/main/java/com/qz/agent/llm/model/StreamChunk.kt`
- Create: `app/src/main/java/com/qz/agent/llm/stream/SseParser.kt`

- [ ] **Step 1: 创建 `ChatRequest.kt`**

```kotlin
package com.qz.agent.llm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
```

- [ ] **Step 2: 创建 `ChatResponse.kt`**

```kotlin
package com.qz.agent.llm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finishReason: String? = null
)

@Serializable
data class ResponseMessage(
    val role: String,
    val content: String? = null
)
```

- [ ] **Step 3: 创建 `StreamChunk.kt`**

```kotlin
package com.qz.agent.llm.model

import kotlinx.serialization.Serializable

@Serializable
data class StreamChunk(
    val content: String,
    val isDone: Boolean = false
)
```

- [ ] **Step 4: 创建 `SseParser.kt` - 解析 SSE 流**

```kotlin
package com.qz.agent.llm.stream

import com.qz.agent.llm.model.StreamChunk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(line: String): StreamChunk? {
        if (line.isBlank()) return null
        if (!line.startsWith("data:")) return null
        val data = line.removePrefix("data:").trim()
        if (data == "[DONE]") return StreamChunk("", isDone = true)
        return try {
            val obj = json.parseToJsonElement(data).jsonObject
            val choices = obj["choices"]?.jsonArray ?: return null
            if (choices.isEmpty()) return null
            val delta = choices[0].jsonObject["delta"]?.jsonObject
            val content = delta?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
            StreamChunk(content)
        } catch (e: Exception) {
            null
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/qz/agent/llm/model/ app/src/main/java/com/qz/agent/llm/stream/
git commit -m "feat: 实现 LLM DTO 与 SSE 流式解析器

- ChatRequest/ChatMessage(OpenAI 兼容格式)
- ChatResponse/Choice/ResponseMessage
- StreamChunk(分片内容 + 是否结束)
- SseParser 解析 SSE data: 行,支持 [DONE]

Author: ThinkReally114"
```

---

## Task 9: LLM 客户端接口与 Zen 实现

**Files:**
- Create: `app/src/main/java/com/qz/agent/llm/ZenConfig.kt`
- Create: `app/src/main/java/com/qz/agent/llm/LlmClient.kt`
- Create: `app/src/main/java/com/qz/agent/llm/OpenAiCompatibleClient.kt`

- [ ] **Step 1: 创建 `ZenConfig.kt`**

```kotlin
package com.qz.agent.llm

data class ZenConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.opencode.ai/v1",
    val model: String = "opencode/big-pickle",
    val temperature: Double = 0.7
)
```

- [ ] **Step 2: 创建 `LlmClient.kt` - 接口**

```kotlin
package com.qz.agent.llm

import com.qz.agent.llm.model.StreamChunk
import kotlinx.coroutines.flow.Flow

interface LlmClient {
    fun streamChat(messages: List<com.qz.agent.llm.model.ChatMessage>): Flow<StreamChunk>
    suspend fun chat(messages: List<com.qz.agent.llm.model.ChatMessage>): String
}
```

- [ ] **Step 3: 创建 `OpenAiCompatibleClient.kt` - Ktor SSE 流式实现**

```kotlin
package com.qz.agent.llm

import com.qz.agent.llm.model.ChatMessage
import com.qz.agent.llm.model.ChatRequest
import com.qz.agent.llm.model.StreamChunk
import com.qz.agent.llm.stream.SseParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class OpenAiCompatibleClient(
    private val httpClient: HttpClient,
    private val config: ZenConfig,
    private val sseParser: SseParser = SseParser()
) : LlmClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChat(messages: List<ChatMessage>): Flow<StreamChunk> = flow {
        val request = ChatRequest(
            model = config.model,
            messages = messages,
            stream = true,
            temperature = config.temperature
        )
        val response: HttpResponse = httpClient.post("${config.baseUrl}/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), request))
        }

        val channel = response.bodyAsChannel()
        val buffer = StringBuilder()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            val chunk = sseParser.parse(line)
            if (chunk != null) {
                if (chunk.isDone) break
                if (chunk.content.isNotEmpty()) emit(chunk)
            }
        }
    }

    override suspend fun chat(messages: List<ChatMessage>): String {
        val builder = StringBuilder()
        streamChat(messages).collect { chunk ->
            builder.append(chunk.content)
        }
        return builder.toString()
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/qz/agent/llm/
git commit -m "feat: 实现 LLM 客户端 - Zen API OpenAI 兼容实现

- ZenConfig: Zen API 配置(baseUrl/key/model)
- LlmClient 接口: streamChat + chat
- OpenAiCompatibleClient: Ktor SSE 流式调用
  - Authorization Bearer 认证
  - bodyAsChannel 逐行读取 SSE
  - Flow<StreamChunk> 流式返回

Author: ThinkReally114"
```

---

## Task 10: 依赖注入模块

**Files:**
- Modify: `app/src/main/java/com/qz/agent/di/AppModule.kt`
- Modify: `app/src/main/java/com/qz/agent/di/NetworkModule.kt`
- Modify: `app/src/main/java/com/qz/agent/di/UiModule.kt`

- [ ] **Step 1: 实现 `NetworkModule.kt`**

```kotlin
package com.qz.agent.di

import com.qz.agent.data.datastore.ApiKeyStore
import com.qz.agent.llm.LlmClient
import com.qz.agent.llm.OpenAiCompatibleClient
import com.qz.agent.llm.ZenConfig
import com.qz.agent.llm.stream.SseParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) { level = LogLevel.NONE }
        }
    }
    single { SseParser() }
    single { ApiKeyStore(get()) }
    single<LlmClient> {
        val keyStore = get<ApiKeyStore>()
        val apiKey = keyStore.getZenApiKey() ?: ""
        OpenAiCompatibleClient(
            httpClient = get(),
            config = ZenConfig(apiKey = apiKey)
        )
    }
}
```

- [ ] **Step 2: 实现 `AppModule.kt`**

```kotlin
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
```

- [ ] **Step 3: `UiModule.kt` 保持空（后续 VM 注入）**

```kotlin
package com.qz.agent.di
import org.koin.dsl.module
val uiModule = module { }
```

- [ ] **Step 4: 验证编译通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/qz/agent/di/
git commit -m "feat: 实现 Koin 依赖注入模块

- NetworkModule: HttpClient + SseParser + ApiKeyStore + LlmClient
- AppModule: Room 数据库 + DAO
- UiModule: 预留(后续 ViewModel)

Author: ThinkReally114"
```

---

## Task 11: 聊天 ViewModel 与发送消息用例

**Files:**
- Create: `app/src/main/java/com/qz/agent/domain/usecase/SendMessageUseCase.kt`
- Create: `app/src/main/java/com/qz/agent/ui/chat/ChatViewModel.kt`

- [ ] **Step 1: 创建 `SendMessageUseCase.kt` - 最小桩实现**

```kotlin
package com.qz.agent.domain.usecase

import com.qz.agent.llm.LlmClient
import com.qz.agent.llm.model.ChatMessage
import com.qz.agent.llm.model.StreamChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SendMessageUseCase(
    private val llmClient: LlmClient
) {
    operator fun invoke(history: List<com.qz.agent.domain.model.Message>, userInput: String): Flow<StreamChunk> {
        val chatMessages = history.map { msg ->
            ChatMessage(role = msg.role.name.lowercase(), content = msg.content)
        } + ChatMessage(role = "user", content = userInput)
        return llmClient.streamChat(chatMessages)
    }
}
```

- [ ] **Step 2: 创建 `ChatViewModel.kt`**

```kotlin
package com.qz.agent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qz.agent.domain.model.ChatRole
import com.qz.agent.domain.model.Message
import com.qz.agent.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    fun sendUserMessage(text: String) {
        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = "default",
            role = ChatRole.USER,
            content = text,
            timestamp = System.currentTimeMillis()
        )
        _messages.update { it + userMsg }

        val assistantMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = "default",
            role = ChatRole.ASSISTANT,
            content = "",
            timestamp = System.currentTimeMillis()
        )
        _messages.update { it + assistantMsg }
        _isStreaming.value = true

        viewModelScope.launch {
            try {
                sendMessageUseCase(_messages.value.filter { it.id != assistantMsg.id }, text)
                    .collect { chunk ->
                        _messages.update { msgs ->
                            msgs.map { if (it.id == assistantMsg.id) it.copy(content = it.content + chunk.content) else it }
                        }
                    }
            } finally {
                _isStreaming.value = false
            }
        }
    }
}
```

- [ ] **Step 3: 更新 `UiModule.kt` 注入 VM**

```kotlin
package com.qz.agent.di

import com.qz.agent.domain.usecase.SendMessageUseCase
import com.qz.agent.ui.chat.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { SendMessageUseCase(get()) }
    viewModel { ChatViewModel(get()) }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/qz/agent/domain/usecase/ app/src/main/java/com/qz/agent/ui/chat/ app/src/main/java/com/qz/agent/di/UiModule.kt
git commit -m "feat: 实现聊天 ViewModel 与发送消息用例

- SendMessageUseCase: 历史消息 + 用户输入转 ChatMessage 调 LLM
- ChatViewModel: 管理 messages/isStreaming 状态
  - 先插入 user + 空 assistant 消息
  - 流式追加 assistant content
- UiModule: 注入 usecase + viewModel

Author: ThinkReally114"
```

---

## Task 12: 聊天主屏与 MainActivity

**Files:**
- Create: `app/src/main/java/com/qz/agent/ui/chat/ChatScreen.kt`
- Create: `app/src/main/java/com/qz/agent/ui/MainActivity.kt`

- [ ] **Step 1: 创建 `ChatScreen.kt`**

```kotlin
package com.qz.agent.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qz.agent.ui.components.ChatInput
import com.qz.agent.ui.components.MessageBubble
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QZ Agent") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(message = msg)
                }
            }
            ChatInput(
                onSend = { viewModel.sendUserMessage(it) },
                enabled = !isStreaming
            )
        }
    }
}
```

- [ ] **Step 2: 创建 `MainActivity.kt`**

```kotlin
package com.qz.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qz.agent.domain.model.PresetTheme
import com.qz.agent.ui.chat.ChatScreen
import com.qz.agent.ui.theme.QZAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(PresetTheme.QZ_PURPLE) }
            QZAgentTheme(theme = theme) {
                ChatScreen()
            }
        }
    }
}
```

- [ ] **Step 3: 验证编译通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/qz/agent/ui/chat/ChatScreen.kt app/src/main/java/com/qz/agent/ui/MainActivity.kt
git commit -m "feat: 实现聊天主屏与 MainActivity 入口

- ChatScreen: LazyColumn 消息列表 + ChatInput
  - 透明 Scaffold 透出主题背景色
  - 新消息自动滚动到底
  - 流式时禁用输入
- MainActivity: 入口,套用 QZAgentTheme

Author: ThinkReally114"
```

---

## Task 13: 设置页 - API Key 输入与主题切换

**Files:**
- Create: `app/src/main/java/com/qz/agent/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/qz/agent/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/qz/agent/ui/MainActivity.kt` (添加导航)
- Modify: `app/src/main/java/com/qz/agent/di/UiModule.kt`

- [ ] **Step 1: 创建 `SettingsViewModel.kt`**

```kotlin
package com.qz.agent.ui.settings

import androidx.lifecycle.ViewModel
import com.qz.agent.data.datastore.ApiKeyStore
import com.qz.agent.domain.model.PresetTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _zenApiKey = MutableStateFlow(apiKeyStore.getZenApiKey() ?: "")
    val zenApiKey: StateFlow<String> = _zenApiKey.asStateFlow()

    private val _selectedTheme = MutableStateFlow(PresetTheme.QZ_PURPLE)
    val selectedTheme: StateFlow<PresetTheme> = _selectedTheme.asStateFlow()

    fun setZenApiKey(key: String) {
        viewModelScope.launch {
            apiKeyStore.setZenApiKey(if (key.isBlank()) null else key)
            _zenApiKey.value = key
        }
    }

    fun setTheme(theme: PresetTheme) {
        _selectedTheme.value = theme
    }
}
```

- [ ] **Step 2: 创建 `SettingsScreen.kt`**

```kotlin
package com.qz.agent.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qz.agent.domain.model.PresetTheme
import com.qz.agent.ui.components.CapsuleButton
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: (PresetTheme) -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val zenKey by viewModel.zenApiKey.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    var keyInput by remember { mutableStateOf(zenKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("OpenCode Zen API Key", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Zen API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            CapsuleButton(
                text = "保存",
                onClick = { viewModel.setZenApiKey(keyInput) },
                isPrimary = true
            )

            Text("主题", style = MaterialTheme.typography.titleLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetTheme.values().forEach { theme ->
                    CapsuleButton(
                        text = theme.displayName,
                        onClick = {
                            viewModel.setTheme(theme)
                            onThemeChanged(theme)
                        },
                        isPrimary = theme == selectedTheme
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: 更新 `UiModule.kt` 添加 SettingsViewModel**

```kotlin
package com.qz.agent.di

import com.qz.agent.domain.usecase.SendMessageUseCase
import com.qz.agent.ui.chat.ChatViewModel
import com.qz.agent.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { SendMessageUseCase(get()) }
    viewModel { ChatViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
```

- [ ] **Step 4: 更新 `MainActivity.kt` 添加导航**

```kotlin
package com.qz.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qz.agent.domain.model.PresetTheme
import com.qz.agent.ui.chat.ChatScreen
import com.qz.agent.ui.settings.SettingsScreen
import com.qz.agent.ui.theme.QZAgentTheme

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(PresetTheme.QZ_PURPLE) }
            val navController = rememberNavController()

            QZAgentTheme(theme = theme) {
                NavHost(navController = navController, startDestination = Routes.CHAT) {
                    composable(Routes.CHAT) {
                        ChatScreen(
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onThemeChanged = { theme = it }
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: 更新 `ChatScreen` 添加设置入口**

修改 `ChatScreen.kt`，在 TopAppBar 的 actions 添加设置按钮：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QZ Agent") },
                actions = {
                    androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                        Text("设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg -> MessageBubble(message = msg) }
            }
            ChatInput(
                onSend = { viewModel.sendUserMessage(it) },
                enabled = !isStreaming
            )
        }
    }
}
```

- [ ] **Step 6: 验证编译通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/qz/agent/ui/settings/ app/src/main/java/com/qz/agent/ui/MainActivity.kt app/src/main/java/com/qz/agent/ui/chat/ChatScreen.kt app/src/main/java/com/qz/agent/di/UiModule.kt
git commit -m "feat: 实现设置页与导航

- SettingsViewModel: 管理 Zen API key 和主题选择
- SettingsScreen: Zen key 输入框 + 保存按钮 + 主题切换胶囊
- MainActivity: NavHost 导航(chat <-> settings)
- ChatScreen: 顶栏添加设置入口
- 主题切换实时生效

Author: ThinkReally114"
```

---

## Task 14: 端到端验证

**Files:**
- None (manual verification)

- [ ] **Step 1: 在 Android Studio 打开项目，Gradle sync 成功**

Expected: 无报错，依赖全部解析。

- [ ] **Step 2: 运行到模拟器/真机**

Run: `./gradlew installDebug` 或 Android Studio Run
Expected: app 启动，显示 QZ 紫色背景 + 顶部 "QZ Agent" 标题 + 设置按钮 + 空消息列表 + 底部胶囊输入框。

- [ ] **Step 3: 切换主题**

点设置 → 选「深空蓝」「极光绿」「暖橙」 → 返回聊天。
Expected: 背景色和主色实时变化。

- [ ] **Step 4: 输入 Zen API Key**

设置页输入 Zen API key（从 opencode.ai/auth 获取）→ 保存 → 返回聊天。

- [ ] **Step 5: 发送消息测试流式回复**

输入"你好" → 点发送。
Expected: 用户气泡出现，下方出现 Agent 气泡，内容流式追加，完成后输入框恢复可用。

- [ ] **Step 6: 多轮对话**

继续输入"用 Kotlin 写个 hello world"。
Expected: Agent 返回代码，多轮上下文正确。

- [ ] **Step 7: Commit（如有小修）**

```bash
git add -A
git commit -m "test: 端到端验证通过

- 项目可编译运行
- 4 个预设主题切换正常
- Zen API key 保存生效
- 流式聊天功能正常
- 多轮对话上下文正确

Author: ThinkReally114"
```

---

## 后续计划（本计划未覆盖，下一阶段处理）

以下模块在设计文档中定义，但本计划未实现，留待后续计划：
- 代码查看器 + 划选评论交互（CodeViewer + CommentSheet）
- 代码块渲染 + Markdown 渲染
- 毛玻璃效果（Haze 集成）
- Agent 引擎（Agent Loop + 工具调度 + 权限网关）
- 规则系统（rule.md 解析）
- 权限系统（工具级开关 + 运行时授权）
- 扩展生态层（MCP Client + Skills 加载器 + LSP Client）
- Exa MCP 联网工具
- 内置工具（文件读写 + Git + 代码沙箱）
- 上下文压缩（checkpoint + 重建）
- 会话列表页
- 5 个内置 skills（explain/review/comment/refactor/test-gen）

## 自审

**Spec 覆盖检查：**
- ✅ UI 层（Task 3-5, 12-13）：主题系统、纯色背景、胶囊组件、消息气泡、聊天输入、设置页
- ✅ LLM 客户端（Task 8-9）：DTO、SSE 解析、Zen API 流式调用
- ✅ 数据层基础（Task 6-7）：Room + DAO、API key 加密存储
- ✅ 端到端验证（Task 14）
- ⏸ Agent 引擎/规则/权限/扩展/skills/上下文压缩：留待后续计划

**占位符扫描：** 无 TBD/TODO，所有代码完整。

**类型一致性：** `LlmClient`、`ChatMessage`、`StreamChunk`、`ChatViewModel`、`SettingsViewModel` 在各任务间名称和签名一致。
