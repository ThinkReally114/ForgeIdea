package com.forgeidea.git

data class CommitInfo(
    val id: String,
    val shortId: String,
    val author: String,
    val email: String,
    val time: Long,
    val message: String
)

data class BranchInfo(
    val name: String,
    val isCurrent: Boolean
)

data class StatusInfo(
    val branch: String,
    val isClean: Boolean,
    val added: List<String>,
    val modified: List<String>,
    val removed: List<String>,
    val untracked: List<String>
)

data class DiffResult(
    val oldPath: String,
    val newPath: String,
    val patch: String
)
