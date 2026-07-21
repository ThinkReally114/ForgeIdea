package com.forgeidea.terminal

import android.content.Context
import com.forgeidea.domain.model.CommandRecord
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProotShellExecutor(
    private val context: Context,
    httpClient: HttpClient
) : ShellExecutor {

    private val binaryManager = ProotBinaryManager(context)
    private val rootfsManager = RootfsManager(context, httpClient, binaryManager)

    override fun isEnvironmentReady(): Boolean {
        return binaryManager.getProotPath() != null && rootfsManager.isReady()
    }

    override suspend fun prepareEnvironment(): Flow<ShellExecutor.PrepareProgress> {
        return rootfsManager.prepare()
    }

    override suspend fun execute(command: String, cwd: String?): Result<CommandRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val process = startProcess(command, cwd)
                val stdout = StringBuilder()
                val stderr = StringBuilder()
                val stdoutJob = launch {
                    process.stdout.collect { stdout.appendLine(it) }
                }
                val stderrJob = launch {
                    process.stderr.collect { stderr.appendLine(it) }
                }
                val exit = process.waitFor()
                stdoutJob.join()
                stderrJob.join()
                Result.success(
                    CommandRecord(
                        command = command,
                        stdout = stdout.toString(),
                        stderr = stderr.toString(),
                        exitCode = exit
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun executeStreaming(command: String, cwd: String?): Flow<ShellExecutor.ShellOutputLine> = channelFlow {
        val process = startProcess(command, cwd)
        val stdoutJob = launch {
            process.stdout.collect {
                send(ShellExecutor.ShellOutputLine(ShellExecutor.ShellOutputLine.LineType.STDOUT, it))
            }
        }
        val stderrJob = launch {
            process.stderr.collect {
                send(ShellExecutor.ShellOutputLine(ShellExecutor.ShellOutputLine.LineType.STDERR, it))
            }
        }
        val exit = process.waitFor()
        stdoutJob.join()
        stderrJob.join()
        send(ShellExecutor.ShellOutputLine(ShellExecutor.ShellOutputLine.LineType.EXIT, exit.toString()))
        awaitClose { process.destroy() }
    }.flowOn(Dispatchers.IO)

    private fun startProcess(command: String, cwd: String?): SessionProcess {
        val proot = binaryManager.getProotPath()
            ?: throw IllegalStateException("proot 不可用")
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
            ?: throw IllegalStateException("nativeLibraryDir 不可用")
        val builder = ProotCommandBuilder(
            proot,
            rootfsManager.rootfsPath(),
            rootfsManager.workspaceHostPath().absolutePath,
            nativeLibDir
        )
        val (args, env) = builder.build(command, cwd)
        val pb = ProcessBuilder(args)
        pb.environment().putAll(env)
        val process = pb.start()
        return SessionProcess(process)
    }
}
