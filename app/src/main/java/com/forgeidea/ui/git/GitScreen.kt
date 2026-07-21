package com.forgeidea.ui.git

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeidea.git.CommitInfo
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitScreen(
    onBack: () -> Unit = {},
    viewModel: GitViewModel = koinViewModel()
) {
    val commits by viewModel.commits.collectAsStateWithLifecycle()
    val selected by viewModel.selectedCommit.collectAsStateWithLifecycle()
    val hasRepo by viewModel.hasRepo.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!hasRepo) {
                Text(
                    text = "workspace 目录暂无 Git 仓库",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(commits, key = { it.id }) { commit ->
                        CommitListItem(commit = commit, onClick = {
                            viewModel.selectCommit(commit)
                            showSheet = true
                        })
                    }
                }
            }
        }
    }

    if (showSheet && selected != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.6f)) {
                Text(selected!!.shortId, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(selected!!.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${selected!!.author} · ${formatTime(selected!!.time)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun CommitListItem(commit: CommitInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Text(
                    commit.shortId,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(commit.message, maxLines = 1)
            }
            Text(
                "${commit.author} · ${formatTime(commit.time)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatTime(time: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(time))
}
