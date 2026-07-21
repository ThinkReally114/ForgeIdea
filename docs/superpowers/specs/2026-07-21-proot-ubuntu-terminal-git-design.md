# ForgeIdea 内置 Ubuntu 22.04 终端 + Git 集成设计文档

**日期：** 2026-07-21  
**作者：** ThinkReally114  
**状态：** 已批准，进入实现  
**关联需求：** 参考 Operit 内置终端，使用 Ubuntu 22.04，集成 Git

## 1. 背景与目标

### 1.1 背景
ForgeIdea 是一个原生 Android AI 编程助手，已有聊天 UI、设置页、LLM 客户端。当前 spec 规划了文件/Git/沙箱等工具能力，但尚未实现可交互的本地 Linux 环境。

### 1.2 目标
在 ForgeIdea 内集成一个基于 proot 的 Ubuntu 22.04 本地环境，提供：
- 内置命令/响应式终端（手动输入 shell 命令）
- 完整的 Ubuntu 用户态环境（apt、git、python3 等）
- Agent 可调用终端作为 `execute_command` 工具
- Git 可视化面板（commit 历史、分支、工作区 diff）

## 2. 关键决策

| 决策项 | 选择 | 理由 |
|---|---|---|
| Ubuntu 运行方式 | proot 本地容器 | 无需 root，可跑完整 Ubuntu 用户态 |
| 终端形态 | 命令/响应式 | Phase 1 先跑通核心闭环，全交互式 PTY 放 Phase 2 |
| 终端渲染 | 纯 Compose LazyColumn | 与现有 QZ 风格 UI 无缝融合 |
| rootfs 来源 | Ubuntu base 官方镜像 | 轻量、可 apt 装工具 |
| 工作区共享 | bind mount workspace 目录 | JGit 与 proot 都能访问同一仓库 |
| Git UI | JGit | 纯 Java，解析稳定，无需在 proot 内解析 |
| Shell 接口 | 参考 Operit ShellExecutor | 统一接口，未来可替换为 PTY 实现 |

## 3. 架构

```
┌──────────────────────────────────────────┐
│  UI 层 (Jetpack Compose)                 │
│  TerminalScreen │ GitScreen              │
└────────────┬─────────────────────────────┘
             │
┌────────────▼─────────────────────────────┐
│  应用层                                  │
│  ShellExecutor (统一接口)                │
│  ProotShellExecutor                      │
│  ├─ PRootBinaryManager                   │
│  ├─ RootfsManager                        │
│  ├─ ProotCommandBuilder                  │
│  └─ SessionProcess                       │
│  GitTool (JGit)                          │
│  ExecuteCommandTool (Agent 工具)         │
└────────────┬─────────────────────────────┘
             │
┌────────────▼─────────────────────────────┐
│  Android 系统                            │
│  app nativeLibraryDir/libproot.so        │
│  filesDir/ubuntu-22.04/rootfs/...        │
│  filesDir/workspace/  ← bind→ /workspace │
└──────────────────────────────────────────┘
```

## 4. 模块设计

### 4.1 ShellExecutor 接口

```kotlin
interface ShellExecutor {
    suspend fun execute(command: String, cwd: String? = null): Result<CommandRecord>
    fun executeStreaming(command: String, cwd: String? = null): Flow<ShellOutputLine>
    fun isEnvironmentReady(): Boolean
    suspend fun prepareEnvironment(): Flow<PrepareProgress>
}
```

### 4.2 ProotShellExecutor

把命令翻译成 `libproot.so` 子进程调用，收集 stdout/stderr/exitCode。

### 4.3 RootfsManager

- 镜像 URL：`http://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04-base-{arch}.tar.gz`
- arch 映射：arm64 → arm64, armeabi-v7a → armhf, x86_64 → amd64, x86 → i386
- 解压目标：`context.filesDir/ubuntu-22.04/rootfs/`
- 就绪标记：`context.filesDir/ubuntu-22.04/.ready`
- 工作区：`context.filesDir/workspace/`

### 4.4 GitTool

JGit 封装：commits、branches、status、diff。

### 4.5 Agent 工具

`execute_command`：参数 `command`, `cwd`；调用 `ShellExecutor.execute`。

## 5. UI 设计

- 终端页：状态栏 + 命令记录列表 + 底部输入条 + 快捷 chips
- Git 面板：提交历史列表 + 底部 diff sheet

## 6. Phase 1 范围

### 必做
- ProotBinaryManager
- RootfsManager
- ShellExecutor + ProotShellExecutor
- TerminalScreen + TerminalViewModel
- GitTool + GitScreen
- ExecuteCommandTool
- MainActivity 导航入口

### 不做
- 全交互式 PTY
- GUI 程序运行
- 暴露 /sdcard
