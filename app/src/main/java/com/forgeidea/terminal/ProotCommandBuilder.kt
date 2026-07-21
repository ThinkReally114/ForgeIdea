package com.forgeidea.terminal

class ProotCommandBuilder(
    private val prootPath: String,
    private val rootfsPath: String,
    private val workspaceHostPath: String
) {
    fun build(command: String, cwd: String? = null): List<String> {
        val workDir = cwd ?: "/workspace"
        return listOf(
            prootPath,
            "-r", rootfsPath,
            "-b", "$workspaceHostPath:/workspace",
            "-b", "/dev",
            "-b", "/proc",
            "-w", workDir,
            "/bin/bash", "-c", command
        )
    }
}
