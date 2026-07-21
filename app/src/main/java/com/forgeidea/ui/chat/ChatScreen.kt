package com.forgeidea.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.PresetTheme
import com.forgeidea.ui.components.CapsuleButton
import com.forgeidea.ui.components.ChatInput
import com.forgeidea.ui.components.MessageBubble
import com.forgeidea.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    val models by viewModel.models.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val currentSessionTitle by viewModel.currentSessionTitle.collectAsState()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerOpen = drawerState.currentValue == DrawerValue.Open
    BackHandler(enabled = drawerOpen) {
        scope.launch { drawerState.close() }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content, messages.lastOrNull()?.reasoning) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerContent(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    currentSessionTitle = currentSessionTitle,
                    onNewSession = { viewModel.createNewSession() },
                    onSwitchSession = { viewModel.switchToSession(it) },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onRenameSession = { id, title -> viewModel.renameSession(id, title) },
                    settingsViewModel = settingsViewModel
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ForgeIdea") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "对话列表")
                        }
                    },
                    actions = {
                        Text(
                            text = currentSessionTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = WindowInsets.navigationBars.asPaddingValues()
                ) {
                    val lastMessage = messages.lastOrNull()
                    items(messages, key = { it.id }) { msg ->
                        val loading = msg.id == lastMessage?.id && isStreaming && msg.role != ChatRole.USER
                        MessageBubble(message = msg, isLoading = loading)
                    }
                }
                ChatInput(
                    onSend = { viewModel.sendUserMessage(it) },
                    models = models,
                    selectedModelId = selectedModelId,
                    onModelSelected = { viewModel.selectModel(it) },
                    enabled = !isStreaming
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(
    sessions: List<com.forgeidea.data.local.entity.SessionEntity>,
    currentSessionId: String?,
    currentSessionTitle: String,
    onNewSession: () -> Unit,
    onSwitchSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "ForgeIdea",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        NavigationDrawerItem(
            label = { Text("新建对话") },
            selected = false,
            onClick = onNewSession,
            icon = { Icon(Icons.Default.Add, contentDescription = "新建") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "对话列表",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        sessions.forEach { session ->
            val isCurrent = session.id == currentSessionId
            NavigationDrawerItem(
                label = { Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selected = isCurrent,
                onClick = { onSwitchSession(session.id) },
                badge = {
                    Row {
                        IconButton(onClick = { onRenameSession(session.id, session.title) }) {
                            Icon(Icons.Default.Edit, contentDescription = "重命名", modifier = Modifier.width(18.dp))
                        }
                        IconButton(onClick = { onDeleteSession(session.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.width(18.dp))
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        DrawerSettingsSection(viewModel = settingsViewModel)
    }
}

@Composable
private fun DrawerSettingsSection(
    viewModel: SettingsViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    var keyInput by remember { mutableStateOf(apiKey) }
    var urlInput by remember { mutableStateOf(baseUrl) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<LlmModel?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.padding(end = 12.dp))
            Text("设置", style = MaterialTheme.typography.titleMedium)
        }
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            CapsuleButton(
                text = "保存 API 配置",
                onClick = {
                    viewModel.setApiKey(keyInput)
                    viewModel.setBaseUrl(urlInput)
                },
                isPrimary = true
            )

            Text("模型", style = MaterialTheme.typography.titleSmall)
            models.forEach { model ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(model.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(model.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { editingModel = model }) { Text("编辑") }
                    TextButton(onClick = { viewModel.removeModel(model.id) }) { Text("删除") }
                }
            }
            CapsuleButton(
                text = "添加模型",
                onClick = { showAddDialog = true }
            )

            Text("主题", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetTheme.values().forEach { theme ->
                    CapsuleButton(
                        text = theme.displayName,
                        onClick = { viewModel.setTheme(theme) },
                        isPrimary = theme == selectedTheme
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ModelDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { viewModel.addModel(it) }
        )
    }

    editingModel?.let { model ->
        ModelDialog(
            model = model,
            onDismiss = { editingModel = null },
            onConfirm = { viewModel.updateModel(model.id, it) }
        )
    }
}

@Composable
private fun ModelDialog(
    model: LlmModel? = null,
    onDismiss: () -> Unit,
    onConfirm: (LlmModel) -> Unit
) {
    var name by remember { mutableStateOf(model?.name ?: "") }
    var id by remember { mutableStateOf(model?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (model == null) "添加模型" else "编辑模型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("显示名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("模型 ID") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank()) {
                        onConfirm(LlmModel(id = id.trim(), name = name.trim()))
                        onDismiss()
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
