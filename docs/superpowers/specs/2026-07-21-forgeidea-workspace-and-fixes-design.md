# ForgeIdea 工作区与交互修复设计文档

## 1. 目标
修复上一版本引入的多个可用性问题，并为 ForgeIdea 加入轻量级工作区，使 Agent 能够安全地创建、读取和写入文件。

## 2. 需求清单

### 2.1 输出方式回退
- LLM 客户端从 SSE 流式改回 `stream=false` 的非流式请求。
- UI 保留打字机效果，让完整回复以动画方式逐段显示，避免生硬跳变。
- 仍记录并展示请求模型、服务商、耗时。

### 2.2 修复“无法发送消息”
- 排查点：
  - 选中模型是否绑定到有效的服务商。
  - 服务商 API Key 是否为空。
  - 非流式 JSON 响应解析是否健壮。
- 任何发送失败都在 Agent 消息气泡中显示错误内容，而不是卡死输入框。

### 2.3 模型切换同步与模态框
- `SettingsViewModel` 修改模型/服务商后，`ChatViewModel` 必须立即刷新。
- 修复模型选择模态框不弹出的问题。
- 优化模型名字显示不全：输入框 Chip 使用 `maxLines=1` + `overflow=Ellipsis`，弹窗中完整显示。

### 2.4 设置改为独立页面
- 将设置区从侧边栏拆出，恢复为 `SettingsScreen` 独立页面。
- 侧边栏仅保留“设置”入口。
- 设置页移除顶部的 API Key / Base URL 输入框；旧配置自动迁移为“默认服务商”。
- 设置页保留服务商/模型管理。

### 2.5 侧边栏去掉 ForgeIdea 字样
- 侧边栏顶部标题直接删除，或替换为“会话”。

### 2.6 修复会话改名
- 排查 `ChatRepository.renameSession` 与 UI 回调。
- 重命名后刷新会话列表与当前会话标题。

### 2.7 工作区（A 方案）
- 每个会话绑定一个沙箱目录：`/data/data/<package>/files/workspace/<sessionId>/`。
- 侧边栏增加可折叠的“工作区”面板，展示该目录下的文件树。
- Agent 工具：
  - `create_file`：在沙箱内创建文件。
  - `read_file`：读取沙箱内文件内容。
  - `write_file`：覆盖写入沙箱内文件。
- 所有路径必须解析为沙箱目录内的规范路径，禁止 `..` 越界。

## 3. 架构与数据流

### 3.1 非流式 LLM 调用
```
ChatViewModel -> SendMessageUseCase -> OpenAiCompatibleClient.chat(stream=false)
                                      -> JSON 解析完整 content
                                      -> 返回完整字符串
ChatViewModel -> 按字符/段落动画追加到 Agent 消息
```

### 3.2 工作区文件工具
```
Agent 收到 toolCall -> ChatViewModel/工具执行器
                     -> WorkspaceFileTools.create/read/write
                     -> 沙箱路径校验
                     -> File I/O
                     -> 结果写回 Agent 消息
```

## 4. 关键实现点

### 4.1 文件工具沙箱
- 工具接收的 `path` 应为相对路径或绝对路径。
- 统一转换为基于会话工作区根目录的 `File`。
- 使用 `File.canonicalPath.startsWith(workspaceRoot.canonicalPath)` 校验。

### 4.2 打字机效果
- Agent 消息最终内容保存完整字符串。
- UI 层用 `remember(message.id)` 保存当前显示长度，启动 `LaunchedEffect` 按 8-16ms 间隔递增显示长度。
- 新消息到达时重置动画。

### 4.3 模型同步
- `SettingsViewModel` 修改模型/服务商后，`ChatViewModel.refreshModels()` 被调用。
- 使用 Koin 或顶层 ViewModel 共享 `ApiKeyStore` 状态，必要时通过事件通知刷新。

## 5. 边界与错误处理
- 工作区目录不存在时自动创建。
- 文件越界访问返回明确错误。
- LLM 响应为空或解析失败时显示错误消息。
- 服务商缺失时提示用户先到设置页配置。

## 6. 后续可扩展
- 在工作区面板中点击文件查看/编辑内容。
- 支持工作区文件作为附件加入对话上下文。
- 支持工作区导入/导出。
