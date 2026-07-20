# 移动端编程 Agent 设计文档

**日期：** 2026-07-20
**作者：** ThinkReally114
**状态：** 待主人审查

## 1. 背景与目标

主人希望「基于 opencode 的理念」在安卓端做一个移动端编程辅助 agent。本项目不直接移植 opencode 代码，而是借鉴其**可扩展架构**（rule + 权限 + MCP + skills + LSP + 工具），用原生 Kotlin + Jetpack Compose 实现一个独立 app，UI 风格参考 QZMusic v2（毛玻璃 + 动态取色 + 纯色深色背景 + 胶囊组件 + Lottie 动效），模型接入 OpenCode Zen 免费模型（OpenAI 兼容 API）。

### 核心目标

- 借鉴 opencode / MiMo Code 的可扩展架构，做一个原生安卓编程 agent
- 借鉴 QZMusic 的视觉 DNA 做深色 Material You 风格 UI
- 借鉴 opencode Zen 白嫖免费模型，纯客户端无后端
- 高度可自定义：预设主题切换 + rule.md + agent 权限开关 + MCP/skills/LSP/Exa 联网

### 非目标（MVP 不做）

- JS 插件 SDK（V2 再考虑）
- 自建后端中转 server
- 复杂主题自定义滑块（仅预设主题切换）
- 跨平台（仅安卓）

## 2. 总体架构

五层分层架构（Clean Architecture 风格），每层职责单一、靠接口通信。Agent 引擎为核心，规则系统和权限系统在运行时注入策略，扩展生态层提供可插拔工具。

### 2.1 架构图

```
┌─────────────────────────────────────────────────┐
│  UI 层 (Compose + M3 · QZ 风格 · 预设主题切换)  │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│  Agent 引擎 (Agent Loop · 工具调度 · 权限网关)  │  ← 核心
└──────┬───────────────────────────┬─────────────┘
       │                           │
┌──────▼──────────┐  ┌─────────────▼──────────────┐
│  规则系统        │  │  权限系统                   │
│  rule.md 解析    │  │  工具级开关 + 运行时授权    │
│  系统提示词组装  │  │  (含"联网"统一开关)         │
└─────────────────┘  └─────────────────────────────┘
       │ (运行时注入策略，虚线)            │
┌──────▼───────────────────────────────────▼──────┐
│  扩展生态层 (统一 Tool 接口)                     │
│  MCP Client · Skills 加载器 · LSP Client        │
│  Exa MCP(联网) · 内置工具(搜索/文件/Git/沙箱)    │
└──────┬───────────────────────────┬─────────────┘
       │                           │
┌──────▼──────────┐  ┌─────────────▼──────────────┐
│  LLM 客户端      │  │  数据层                     │
│  Zen API · 流式  │  │  Room(会话) + DataStore(Key) │
└─────────────────┘  └─────────────────────────────┘
```

### 2.2 数据流

1. 用户在 UI 输入消息
2. Agent 引擎启动会话：读 rule.md（全局 + 项目级）拼进 system prompt
3. 查权限表过滤出当前可用工具列表
4. 组装请求发给 LLM 客户端 → 调 Zen API（OpenAI 兼容、流式）
5. 流式回包解析：
   - 纯文本 → 直接渲染
   - 工具调用 → 经权限网关校验 → 命中扩展生态层对应 Tool 执行 → 结果回填
6. 循环 4-5 直到无工具调用
7. 最终回复渲染到 UI，会话全程落 Room

## 3. 模块设计

### 3.1 UI 层

**职责：** 渲染聊天界面、代码查看/高亮、设置页。

**技术：** Jetpack Compose + Material 3

**视觉风格（复刻 QZMusic DNA）：**

| 风格要素 | 实现方式 | 依赖 |
|---|---|---|
| 纯色深色背景 | 每主题预设一个主背景色，无渐变 | M3 ColorScheme.background |
| 毛玻璃卡片 | 消息/卡片半透明 + 实时背景模糊 | Haze |
| 动态取色 | 主色跟系统壁纸生成（安卓 12+） | M3Color (kyant0) |
| 胶囊组件 | 圆角按钮、工具标签、输入框全胶囊化 | Capsule (kyant0) |
| 渐变品牌色 | 紫→蓝渐变做强调色 | 自定义 Brush |
| Lottie 动效 | 加载、发送、工具执行配动效 | dotlottie-android |
| 深色优先 | 默认深色，浅色作备选 | M3 darkColorScheme |
| 代码块 | 深色半透明底 + 等宽字体 + 语法高亮 | 自定义 |

**预设主题切换：** 提供 3-4 个预设主题（如「QZ 紫」「深空蓝」「极光绿」「暖橙」），每个主题预设好背景色、品牌色、强调色组合。用户在设置页一键切换，不提供细粒度滑块。

**代码查看器 + 划选评论交互：**

代码查看器是手机端编程的核心组件，支持"划选代码 → 弹出评论面板 → 针对选中片段向 AI 提问/要建议"的多轮交互流程。

交互流程：
1. 用户在代码查看器长按拖动选中一段代码
2. 选中区高亮 + 顶部浮出操作条（毛玻璃胶囊样式）：`评论` / `解释` / `找问题` / `自定义提问`
3. 点操作 → 底部弹出评论面板（半透明毛玻璃 Sheet）：
   - 上半：选中代码预览（只读，带行号高亮）
   - 下半：AI 回复区 + 输入框（支持多轮追问选中片段）
4. AI 回复流式渲染，工具调用（如 read_file 读上下文）显示为胶囊标签
5. 追问可切换不同 skill（如先"解释"再"找问题"），保持选中上下文

技术要点：
- 代码渲染：自定义 Compose 代码查看器，支持语法高亮 + 行号 + 选择高亮
- 选择交互：`SelectableText` 或自定义手势识别，返回选中文本 + 起止行号
- 上下文传递：选中片段连同**文件路径 + 起止行号**作为上下文喂给 skill，AI 能定位代码位置
- 评论面板：`ModalBottomSheet` + Haze 毛玻璃效果，QZ 风格胶囊输入框
- 多轮会话：评论面板内维护独立会话，关闭后可从消息历史重新打开

### 3.2 Agent 引擎（核心）

**职责：** Agent Loop（模型↔工具循环）、工具注册与调度、消息组装、权限网关。

**技术：** Kotlin Coroutines + Flow

**Agent Loop 伪流程：**

```
1. 组装 system prompt = base_prompt + rule.md 内容
2. messages = [system, ...history, user_input]
3. loop:
   a. available_tools = filter_by_permission(registered_tools)
   b. response = llm_client.chat(messages, available_tools)
   c. if response.has_tool_calls:
      - for each tool_call:
        - if permission_gateway.check(tool_call) == DENY:
          - 拒绝并回填"权限不足"
        - else:
          - result = tool_registry.execute(tool_call)
          - messages.append(tool_result)
      - continue loop
   d. else:
      - 渲染最终回复，break
```

### 3.3 规则系统

**职责：** 解析 rule.md，组装系统提示词。

**机制：**
- 用户在 app 内或项目根目录放 `rule.md`，用 Markdown 写 agent 行为规则（如"回复用中文"、"代码必须加注释"、"扮演 Rust 专家"）
- Agent 引擎启动会话时解析 rule.md，拼进 system prompt
- 支持两层：
  - **全局 rule**（存 app 内，所有会话生效）
  - **项目级 rule**（项目根目录 `rule.md`，覆盖全局）

### 3.4 权限系统

**职责：** 工具级开关 + 运行时授权。

**机制：**
- 每个工具能力独立开关，权限配置存 DataStore，可导入导出
- 敏感操作（写文件、执行代码、调外部 MCP）首次触发时弹窗授权：本次允许 / 始终允许 / 拒绝
- **联网统一开关：** Exa MCP 搜索和抓取合并为一个 `联网` 权限开关，不拆分

**权限清单（MVP）：**

| 权限名 | 说明 | 默认 |
|---|---|---|
| `read_file` | 读手机文件 | ask |
| `write_file` | 写/改文件 | ask |
| `联网` | Exa MCP 搜索 + 抓取 | ask |
| `execute_code` | 代码沙箱执行 | ask |
| `read_git` | 读 Git 仓库 | ask |
| `mcp_external` | 调用外部 MCP server | ask |
| `lsp` | LSP 代码智能 | allow |

### 3.5 扩展生态层

**核心：** 所有扩展实现同一个 `Tool` 接口，Agent 引擎不关心工具来源。

```kotlin
interface Tool {
    val name: String
    val description: String
    val schema: JsonSchema  // 参数 schema
    suspend fun execute(args: JsonObject): ToolResult
}
```

**扩展点：**

| 扩展点 | 作用 | 接入方式 |
|---|---|---|
| MCP Client | 连外部 MCP server（GitHub/数据库/Exa/任意 MCP） | stdio 或 SSE 传输 |
| Skills 加载器 | 加载"能力包"（一组工具 + 配套提示词的打包） | 本地目录扫描 SKILL.md |
| LSP Client | 代码补全/跳转/诊断，跟 opencode 一样自动加载 | 跑 LSP server 进程 |
| Exa MCP（联网） | 网络搜索 + 网页抓取，统一走 Exa 托管 MCP | HTTPS 连 `mcp.exa.ai/mcp` |
| 内置工具 | 文件读写 / Git(JGit) / 代码沙箱 | 原生实现 |

**联网方案（Exa 统一）：**
- 不再单独写内置 webfetch
- 联网能力统一走 Exa MCP：搜索 + 抓取都通过 `mcp.exa.ai/mcp`
- 纯客户端无后端可用：app 直接 HTTPS 连 Exa 托管端点
- MVP 用免费匿名档（无需 key，有限流，约 1000 次/月）
- 用户可选填 Exa API key 解锁更高限流
- 权限上合成一个 `联网` 开关

**内置 Skills 清单（开箱自带）：**

app 内置 4 个最核心的编程辅助 skills，用 SKILL.md 格式打包，用户开箱即用，也可在设置里禁用。覆盖手机端编程辅助的四个高频场景：看代码、查代码、改代码、测代码。

| Skill | 触发场景 | 角色 | 能力 | 绑定工具 |
|---|---|---|---|---|
| `code-explain` | 用户问"这段代码什么意思"、"解释一下" | 资深开发者 | 逐行解释逻辑、关键函数、设计意图，用通俗语言 | read_file |
| `code-review` | 用户问"有没有问题"、"review 一下"、"为啥报错" | 严格审查者 | 找 bug/安全/性能/风格问题，按严重程度列出 + 修复建议（含 debug 场景） | read_file, 联网 |
| `code-comment` | 用户在代码查看器划选片段后点"评论" | 代码导师 | 针对选中代码片段给建议、解释、改进点，可多轮追问 | read_file |
| `refactor-suggest` | 用户问"怎么重构"、"能优化吗" | 重构专家 | 给重构方案、改进点、重构后代码 | read_file |
| `test-gen` | 用户问"帮我写测试"、"生成测试用例" | 测试工程师 | 生成单元测试，覆盖正常 + 边界 + 异常情况 | read_file |

每个 skill 的 SKILL.md 包含字段：`name`、`description`（触发条件）、`persona`（角色提示词）、`prompt` 模板、`tools`（绑定的工具白名单）。内置 skills 打包进 APK 的 `assets/skills/` 目录，启动时加载；用户也可在 app 内 `skills/` 目录放自定义 SKILL.md 扩展。

### 3.6 LLM 客户端

**职责：** 调 Zen API（OpenAI 兼容）、流式解析、限流退避。

**技术：** Ktor Client + kotlinx.serialization

**配置：**
- 默认 provider: OpenCode Zen（`opencode/<model_id>` 格式）
- 端点: OpenAI 兼容
- 认证: 用户在 opencode.ai/auth 登录后拿到的 Zen API key
- 流式: SSE 解析
- 退避: 429/5xx 指数退避重试

### 3.7 数据层

**职责：** 会话历史、API Key 安全存储。

**技术：** Room + EncryptedDataStore

- 会话历史 → Room（消息、工具调用、元数据）
- API Key（Zen / Exa 可选 / 其他 provider）→ EncryptedDataStore（Android Keystore 加密）
- rule.md 全局副本 → DataStore
- 权限配置 → DataStore
- 上下文检查点 → Room（结构化状态快照，见 3.8）

### 3.8 上下文压缩

**职责：** 长程对话的上下文管理，避免 token 爆掉，保持多轮任务的状态连续性。

**设计动机：** 手机端模型上下文窗口有限（尤其免费模型），几十轮工具调用 + 代码片段 + 错误日志很快填满窗口。简单截断会丢失早期关键信息（如用户意图、架构决策），导致 agent "失忆"跑偏。借鉴 MiMo Code 的 checkpoint 机制做简化版：**提前抽取 + 结构化快照 + 重建时复用**。

**机制（三层）：**

1. **Token 预算监控**
   - Agent 引擎每轮请求前估算当前消息总 token（用 tokenizer 或字符数估算）
   - 设阈值：`soft_limit`（70%）、`hard_limit`（90%）
   - 到 `soft_limit` 触发增量 checkpoint，到 `hard_limit` 触发重建

2. **增量 Checkpoint（软限制触发）**
   - 不阻塞主循环，后台起一个 writer 协程
   - 读取当前会话历史，提取结构化状态写入 Room：
     - `intent`：当前用户意图
     - `next_action`：下一步计划
     - `working_files`：涉及的文件列表
     - `errors_fixes`：遇到的错误及修复
     - `decisions`：已确定的设计决策
     - `recent_summary`：最近 N 轮的摘要
   - 增量更新前一份 checkpoint，不是一次性全量摘要

3. **上下文重建（硬限制触发）**
   - 切掉旧消息，保留：system prompt + rule.md + 最新 checkpoint + 最近 K 轮原始消息
   - 主 agent 在新窗口里"醒来"，状态已铺好，继续干活
   - 从模型视角看对话从未中断

**触发流程：**

```
每轮请求前:
  tokens = estimate(messages)
  if tokens >= hard_limit:
      # 重建：先确保 checkpoint 是最新的
      await writer.flush()
      messages = rebuild_from(checkpoint, recent_k_messages)
  elif tokens >= soft_limit and not writer.running:
      # 增量 checkpoint（后台，不阻塞）
      writer.trigger_incremental()
```

**简化策略（相比 mimocode）：**
- 不做独立的 writer 子 agent（手机端资源紧，用同一模型的后台协程即可）
- 不做四层记忆（session/project/global/history），只做单层 checkpoint + 原始历史回溯
- 不做 `notes.md` 自由笔记通道（主 agent 直接写 checkpoint）
- checkpoint 存 Room，结构化字段固定 6 项，便于查询和 UI 展示

**UI 支持：**
- 设置页可调 `soft_limit` / `hard_limit` 阈值（默认 70% / 90%）
- 聊天界面显示"已压缩 N 轮历史"提示（毛玻璃胶囊标签）
- 可查看当前 checkpoint 内容（只读，展示 agent 当前状态）

## 4. 技术栈总览

| 层 | 技术 | 说明 |
|---|---|---|
| 语言 | Kotlin | 原生 |
| UI | Jetpack Compose + Material 3 | 声明式 |
| 视觉增强 | Haze / M3Color / Capsule / dotlottie | 复刻 QZMusic DNA（无渐变背景） |
| 异步 | Coroutines + Flow | Agent Loop |
| 网络 | Ktor Client | LLM + MCP 调用 |
| 序列化 | kotlinx.serialization | JSON |
| DI | Koin | 轻量 |
| 数据库 | Room | 会话存储 |
| KV | DataStore + EncryptedDataStore | 配置 + Key |
| 图像 | Coil | 头像等 |
| Git | JGit | 读 git 仓库 |
| LSP | 自定义 Client | 代码智能 |
| 沙箱 | Chaquopy / QuickJS | 代码执行（MVP 用 QuickJS 轻量） |

## 5. 模型与 Provider

- **默认：** OpenCode Zen 免费模型（如 Big Pickle 永久免费，DeepSeek/MiMo 轮换免费）
- **接入：** app 直连 Zen API（OpenAI 兼容），key 存手机
- **扩展：** 支持用户自填任意 OpenAI 兼容 provider（OpenAI/Anthropic/Gemini/Ollama 等）

## 6. MVP 范围

**MVP 必做：**
- [x] Agent Loop + 工具调度
- [x] rule.md 解析（全局 + 项目级）
- [x] 权限系统（工具开关 + 运行时授权）
- [x] LLM 客户端（Zen + 流式）
- [x] Exa MCP 联网（搜索 + 抓取）
- [x] 内置工具：文件读写、Git 读取
- [x] MCP Client 框架（至少能连 Exa）
- [x] 内置 5 个核心 skills（explain/review/comment/refactor/test-gen）
- [x] 代码查看器 + 划选评论交互
- [x] QZ 风格 UI（预设主题切换）
- [x] 会话存储
- [x] 上下文压缩（checkpoint + 重建）

**MVP 可选（时间允许）：**
- [ ] LSP Client（代码补全/跳转）
- [ ] Skills 加载器
- [ ] 代码沙箱执行
- [ ] 外部 MCP server 接入（GitHub 等）

## 7. 开放问题

无。所有关键决策已与主人确认：
- 形态：全新安卓 AI 助手 app（编程辅助型）
- 技术栈：原生 Kotlin + Compose
- 模型：app 直连 Zen 免费模型
- Agent 能力：工具调用 + 代码执行 + 读 git 仓库
- 自定义深度：预设主题 + rule.md + agent 权限开关 + MCP/skills/LSP/Exa
- 风格：QZMusic 深色 Material You
- 联网：Exa MCP 统一，权限合并为单个"联网"开关
