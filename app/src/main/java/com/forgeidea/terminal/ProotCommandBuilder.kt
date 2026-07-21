package com.forgeidea.terminal

import java.io.File

class ProotCommandBuilder(
    private val prootPath: String,
    private val rootfsPath: String,
    private val workspaceHostPath: String,
    private val nativeLibDir: String
) {
    fun build(command: String, cwd: String? = null): Pair<List<String>, Map<String, String>> {
        val workDir = cwd ?: "/workspace"
        val env = mapOf(
            "LD_LIBRARY_PATH" to nativeLibDir,
            "PROOT_UNBUNDLE_LOADER" to File(nativeLibDir, "loader").absolutePath
        )
        val args = listOf(
            prootPath,
            "-r", rootfsPath,
            "-b", "$workspaceHostPath:/workspace",
            "-b", "/dev",
            "-b", "/proc",
            "-w", workDir,
            "/bin/bash", "-c", command
        )
        return args to env
    }
}
