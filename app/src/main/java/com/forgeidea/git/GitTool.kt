package com.forgeidea.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.ByteArrayOutputStream
import java.io.File

class GitTool(private val workspaceDir: File) {

    private fun openRepo(): Git? {
        val gitDir = workspaceDir.resolve(".git")
        return if (gitDir.exists()) Git.open(workspaceDir) else null
    }

    fun hasRepo(): Boolean = workspaceDir.resolve(".git").exists()

    fun listCommits(maxCount: Int = 100): List<CommitInfo> {
        val git = openRepo() ?: return emptyList()
        return git.log().setMaxCount(maxCount).call().map { commit ->
            CommitInfo(
                id = commit.name,
                shortId = commit.name.take(7),
                author = commit.authorIdent.name,
                email = commit.authorIdent.emailAddress,
                time = commit.authorIdent.`when`.time,
                message = commit.shortMessage
            )
        }
    }

    fun listBranches(): List<BranchInfo> {
        val git = openRepo() ?: return emptyList()
        val current = git.repository.branch
        return git.branchList().call().map { ref ->
            BranchInfo(
                name = ref.name.removePrefix("refs/heads/"),
                isCurrent = ref.name == "refs/heads/$current"
            )
        }
    }

    fun getStatus(): StatusInfo {
        val git = openRepo() ?: return StatusInfo("", true, emptyList(), emptyList(), emptyList(), emptyList())
        val status = git.status().call()
        return StatusInfo(
            branch = git.repository.branch,
            isClean = status.isClean,
            added = status.added.sorted(),
            modified = status.modified.sorted(),
            removed = status.removed.sorted(),
            untracked = status.untracked.sorted()
        )
    }

    fun diff(commitA: String? = null, commitB: String? = null): List<DiffResult> {
        val git = openRepo() ?: return emptyList()
        val diffCommand = git.diff()
        if (commitA != null && commitB != null) {
            val reader = git.repository.newObjectReader()
            val parserA = CanonicalTreeParser().apply {
                reset(reader, resolveCommitTree(git, commitA))
            }
            val parserB = CanonicalTreeParser().apply {
                reset(reader, resolveCommitTree(git, commitB))
            }
            diffCommand.setOldTree(parserA).setNewTree(parserB)
        }
        return diffCommand.call().map { entry ->
            DiffResult(
                oldPath = entry.oldPath,
                newPath = entry.newPath,
                patch = formatDiff(git, entry)
            )
        }
    }

    private fun resolveCommitTree(git: Git, commitId: String): ObjectId {
        val objectId = git.repository.resolve(commitId)
            ?: throw IllegalArgumentException("Bad commit $commitId")
        return RevWalk(git.repository).use { walk ->
            walk.parseCommit(objectId).tree.id
        }
    }

    private fun formatDiff(git: Git, entry: DiffEntry): String {
        val out = ByteArrayOutputStream()
        val formatter = org.eclipse.jgit.diff.DiffFormatter(out)
        formatter.setRepository(git.repository)
        formatter.format(entry)
        formatter.flush()
        return out.toString(Charsets.UTF_8.name())
    }
}
