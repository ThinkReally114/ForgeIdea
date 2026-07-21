package com.forgeidea.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.ui.components.ChatInput
import com.forgeidea.ui.components.MessageBubble
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    val models by viewModel.models.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val workspaceFiles by viewModel.workspaceFiles.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshModels()
        viewModel.refreshWorkspaceFiles()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshModels()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentSessionId) {
        viewModel.refreshWorkspaceFiles()
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
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
                    workspaceFiles = workspaceFiles,
                    onSessionSelected = { id ->
                        viewModel.loadSession(id)
                        scope.launch { drawerState.close() }
                    },
                    onNewSession = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onRenameSession = { id, title ->
                        viewModel.renameSession(id, title)
                    },
                    onDeleteSession = { id ->
                        viewModel.deleteSession(id)
                    },
                    onRefreshWorkspace = { viewModel.refreshWorkspaceFiles() },
                    onCreateWorkspaceFile = { path, content ->
                        viewModel.createWorkspaceFile(path, content)
                    },
                    onDeleteWorkspaceFile = { path ->
                        viewModel.deleteWorkspaceFile(path)
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            sessions.find { it.id == currentSessionId }?.title ?: "新对话",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "打开侧边栏")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val lastMessage = messages.lastOrNull()
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    reverseLayout = false,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val loading = msg.id == lastMessage?.id && isStreaming && msg.role != ChatRole.USER
                        MessageBubble(
                            message = msg,
                            isLoading = loading,
                            onAnimated = { viewModel.markMessageAnimated(it) }
                        )
                    }
                }

                ChatInput(
                    onSend = { viewModel.sendUserMessage(it) },
                    models = models,
                    providers = providers,
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
    workspaceFiles: List<String>,
    onSessionSelected: (String) -> Unit,
    onNewSession: () -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRefreshWorkspace: () -> Unit,
    onCreateWorkspaceFile: (String, String) -> Unit,
    onDeleteWorkspaceFile: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var showWorkspace by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNewSession() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Text("新对话", style = MaterialTheme.typography.titleMedium)
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionDrawerItem(
                    session = session,
                    selected = session.id == currentSessionId,
                    onClick = { onSessionSelected(session.id) },
                    onRename = { onRenameSession(session.id, it) },
                    onDelete = { onDeleteSession(session.id) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showWorkspace = !showWorkspace }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Text("工作区", style = MaterialTheme.typography.titleMedium)
            }
            Icon(
                imageVector = if (showWorkspace) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }

        AnimatedVisibility(
            visible = showWorkspace,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("文件", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { showNewFileDialog = true }) { Text("新建") }
                }
                if (workspaceFiles.isEmpty()) {
                    Text("暂无文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    workspaceFiles.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = file,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { fileToDelete = file },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSettings() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Text("设置", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showNewFileDialog) {
        NewWorkspaceFileDialog(
            onDismiss = { showNewFileDialog = false },
            onConfirm = { path, content ->
                onCreateWorkspaceFile(path, content)
                showNewFileDialog = false
            }
        )
    }

    fileToDelete?.let { path ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("删除文件") },
            text = { Text("确定删除 $path 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteWorkspaceFile(path)
                    fileToDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SessionDrawerItem(
    session: com.forgeidea.data.local.entity.SessionEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Box {
        NavigationDrawerItem(
            label = {
                Text(
                    session.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            selected = selected,
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            badge = {
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        )
    }

    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(session.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名会话") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRename(newTitle.trim())
                            showRenameDialog = false
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun NewWorkspaceFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var path by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("路径") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容") },
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (path.isNotBlank()) {
                        onConfirm(path.trim(), content)
                    }
                }
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
