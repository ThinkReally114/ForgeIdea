package com.forgeidea.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.PresetTheme
import com.forgeidea.ui.components.CapsuleButton
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: (PresetTheme) -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    var keyInput by remember { mutableStateOf(apiKey) }
    var baseUrlInput by remember { mutableStateOf(baseUrl) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<LlmModel?>(null) }

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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("API 配置", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("服务商 Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            CapsuleButton(
                text = "保存 API 配置",
                onClick = {
                    viewModel.setApiKey(keyInput)
                    viewModel.setBaseUrl(baseUrlInput)
                },
                isPrimary = true
            )

            Text("模型列表", style = MaterialTheme.typography.titleLarge)
            models.forEach { model ->
                ModelItem(
                    model = model,
                    onEdit = { editingModel = model },
                    onDelete = { viewModel.removeModel(model.id) }
                )
            }
            CapsuleButton(
                text = "添加模型",
                onClick = { showAddDialog = true }
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
private fun ModelItem(
    model: LlmModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = model.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = model.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onEdit) { Text("编辑") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除")
        }
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
