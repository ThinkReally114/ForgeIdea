package com.forgeidea.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val model by viewModel.model.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    var keyInput by remember { mutableStateOf(apiKey) }
    var baseUrlInput by remember { mutableStateOf(baseUrl) }
    var modelInput by remember { mutableStateOf(model) }

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
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("模型 ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            CapsuleButton(
                text = "保存",
                onClick = {
                    viewModel.setApiKey(keyInput)
                    viewModel.setBaseUrl(baseUrlInput)
                    viewModel.setModel(modelInput)
                },
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
