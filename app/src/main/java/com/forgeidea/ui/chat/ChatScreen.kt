package com.forgeidea.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.forgeidea.ui.components.ChatInput
import com.forgeidea.ui.components.MessageBubble
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    val models by viewModel.models.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content, messages.lastOrNull()?.reasoning) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("对话列表", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(
                    label = { Text("新建对话") },
                    selected = false,
                    onClick = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "新建") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                sessions.forEach { session ->
                    NavigationDrawerItem(
                        label = { Text(session.title, maxLines = 1) },
                        selected = session.id == currentSessionId,
                        onClick = {
                            viewModel.switchToSession(session.id)
                            scope.launch { drawerState.close() }
                        },
                        badge = {
                            IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.width(20.dp))
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
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
                        TextButton(onClick = onOpenSettings) { Text("设置") }
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
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(message = msg)
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
