package com.forgeidea.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.PresetTheme
import com.forgeidea.domain.model.Provider
import com.forgeidea.ui.components.CapsuleButton
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val models by viewModel.models.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    var showAddModelDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<LlmModel?>(null) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<Provider?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("服务商列表", style = MaterialTheme.typography.titleLarge)
            providers.forEach { provider ->
                ProviderItem(
                    provider = provider,
                    onEdit = { editingProvider = provider },
                    onDelete = { viewModel.removeProvider(provider.id) }
                )
            }
            CapsuleButton(
                text = "添加服务商",
                onClick = { showAddProviderDialog = true }
            )

            Text("模型列表", style = MaterialTheme.typography.titleLarge)
            models.forEach { model ->
                ModelItem(
                    model = model,
                    providers = providers,
                    onEdit = { editingModel = model },
                    onDelete = { viewModel.removeModel(model.id) }
                )
            }
            CapsuleButton(
                text = "添加模型",
                onClick = { showAddModelDialog = true }
            )

            Text("主题", style = MaterialTheme.typography.titleLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

    if (showAddModelDialog) {
        ModelDialog(
            providers = providers,
            onDismiss = { showAddModelDialog = false },
            onConfirm = { viewModel.addModel(it) }
        )
    }

    editingModel?.let { model ->
        ModelDialog(
            model = model,
            providers = providers,
            onDismiss = { editingModel = null },
            onConfirm = { viewModel.updateModel(model.id, it) }
        )
    }

    if (showAddProviderDialog) {
        ProviderDialog(
            onDismiss = { showAddProviderDialog = false },
            onConfirm = { viewModel.addProvider(it) }
        )
    }

    editingProvider?.let { provider ->
        ProviderDialog(
            provider = provider,
            onDismiss = { editingProvider = null },
            onConfirm = { viewModel.updateProvider(provider.id, it) }
        )
    }
}

@Composable
private fun ProviderItem(
    provider: Provider,
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
            Text(text = provider.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = provider.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onEdit) { Text("编辑") }
        if (provider.id != "default") {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}

@Composable
private fun ModelItem(
    model: LlmModel,
    providers: List<Provider>,
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
            val providerName = providers.find { it.id == model.providerId }?.name ?: ""
            if (providerName.isNotBlank()) {
                Text(providerName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
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
    providers: List<Provider>,
    onDismiss: () -> Unit,
    onConfirm: (LlmModel) -> Unit
) {
    var name by remember { mutableStateOf(model?.name ?: "") }
    var id by remember { mutableStateOf(model?.id ?: "") }
    var providerId by remember { mutableStateOf(model?.providerId ?: providers.firstOrNull()?.id ?: "") }
    var expanded by remember { mutableStateOf(false) }

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
                Box {
                    OutlinedTextField(
                        value = providers.find { it.id == providerId }?.name ?: "",
                        onValueChange = {},
                        label = { Text("服务商") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { expanded = !expanded }
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.name) },
                                onClick = {
                                    providerId = provider.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank()) {
                        onConfirm(LlmModel(id = id.trim(), name = name.trim(), providerId = providerId))
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

@Composable
private fun ProviderDialog(
    provider: Provider? = null,
    onDismiss: () -> Unit,
    onConfirm: (Provider) -> Unit
) {
    var name by remember { mutableStateOf(provider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }
    val id = remember { provider?.id ?: UUID.randomUUID().toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (provider == null) "添加服务商" else "编辑服务商") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && baseUrl.isNotBlank()) {
                        onConfirm(Provider(id = id, name = name.trim(), baseUrl = baseUrl.trim(), apiKey = apiKey))
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
