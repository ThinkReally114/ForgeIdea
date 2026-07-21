package com.forgeidea.ui.terminal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeidea.domain.model.CommandRecord
import com.forgeidea.domain.model.TerminalState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit = {},
    viewModel: TerminalViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.prepare()
    }

    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) listState.animateScrollToItem(records.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("终端") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }
            )
        },
        bottomBar = {
            TerminalInputBar(
                value = input,
                onValueChange = viewModel::onInputChange,
                onSend = { viewModel.executeCommand(input) },
                enabled = state is TerminalState.Ready && !isExecuting,
                suggestions = listOf("git status", "git log", "ls -la", "python3 --version", "apt update")
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is TerminalState.Preparing -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(s.message)
                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.width(240.dp).padding(top = 8.dp)
                        )
                    }
                }
                is TerminalState.Error -> {
                    Text(
                        text = "错误: ${s.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                TerminalState.Ready -> {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(records, key = { it.id }) { record ->
                            CommandRecordItem(record = record)
                        }
                    }
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun CommandRecordItem(record: CommandRecord) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isSuccess)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$ ${record.command}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            if (record.stdout.isNotBlank()) {
                Text(
                    text = record.stdout,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (record.stderr.isNotBlank()) {
                Text(
                    text = record.stderr,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = "exit: ${record.exitCode}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
            )
        }
    }
}

@Composable
private fun TerminalInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    suggestions: List<String>
) {
    Column {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onValueChange(suggestion) },
                    label = { Text(suggestion) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入命令") },
                enabled = enabled,
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
        }
    }
}
