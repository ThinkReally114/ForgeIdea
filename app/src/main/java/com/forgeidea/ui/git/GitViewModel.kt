package com.forgeidea.ui.git

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgeidea.git.CommitInfo
import com.forgeidea.git.GitTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GitViewModel(private val gitTool: GitTool) : ViewModel() {
    private val _commits = MutableStateFlow<List<CommitInfo>>(emptyList())
    val commits: StateFlow<List<CommitInfo>> = _commits.asStateFlow()

    private val _selectedCommit = MutableStateFlow<CommitInfo?>(null)
    val selectedCommit: StateFlow<CommitInfo?> = _selectedCommit.asStateFlow()

    private val _hasRepo = MutableStateFlow(false)
    val hasRepo: StateFlow<Boolean> = _hasRepo.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _hasRepo.value = gitTool.hasRepo()
            _commits.value = gitTool.listCommits()
        }
    }

    fun selectCommit(commit: CommitInfo?) {
        _selectedCommit.value = commit
    }
}
