package com.forgeidea.terminal

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.security.MessageDigest

class RootfsManager(
    private val context: Context,
    private val httpClient: HttpClient,
    private val binaryManager: ProotBinaryManager
) {
    private val baseDir: File get() = File(context.filesDir, "ubuntu-22.04").apply { mkdirs() }
    private val rootfsDir: File get() = File(baseDir, "rootfs")
    private val readyFile: File get() = File(baseDir, ".ready")

    fun isReady(): Boolean = readyFile.exists() && rootfsDir.exists()

    fun rootfsPath(): String = rootfsDir.absolutePath

    fun workspaceHostPath(): File {
        val dir = File(context.filesDir, "workspace")
        dir.mkdirs()
        return dir
    }

    fun prepare(): Flow<ShellExecutor.PrepareProgress> = flow {
        if (isReady()) {
            emit(ShellExecutor.PrepareProgress("环境已就绪", 1f, isDone = true))
            return@flow
        }

        emit(ShellExecutor.PrepareProgress("检查存储空间", 0.05f))
        val usable = baseDir.usableSpace
        if (usable < 2L * 1024 * 1024 * 1024) {
            emit(ShellExecutor.PrepareProgress("存储空间不足", 0f, error = "需要至少 2GB 可用空间"))
            return@flow
        }

        val arch = binaryManager.abiToUbuntuArch(binaryManager.getSupportedAbi())
        val fileName = "ubuntu-base-22.04-base-$arch.tar.gz"
        val url = "http://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/$fileName"
        val tarGz = File(baseDir, fileName)

        if (!tarGz.exists()) {
            emit(ShellExecutor.PrepareProgress("下载 Ubuntu rootfs", 0.1f))
            download(url, tarGz) { progress ->
                emit(ShellExecutor.PrepareProgress("下载 Ubuntu rootfs", 0.1f + progress * 0.5f))
            }
        }

        emit(ShellExecutor.PrepareProgress("校验文件", 0.65f))
        if (!verifyChecksum(tarGz, arch)) {
            tarGz.delete()
            emit(ShellExecutor.PrepareProgress("校验失败", 0f, error = "文件校验失败，请重试"))
            return@flow
        }

        emit(ShellExecutor.PrepareProgress("解压 rootfs", 0.7f))
        extractTarGz(tarGz, rootfsDir)

        emit(ShellExecutor.PrepareProgress("初始化 Ubuntu 环境", 0.85f))
        readyFile.createNewFile()

        emit(ShellExecutor.PrepareProgress("环境准备完成", 1f, isDone = true))
    }.flowOn(Dispatchers.IO)

    private suspend fun download(url: String, out: File, onProgress: suspend (Float) -> Unit) {
        val response: HttpResponse = httpClient.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                if (contentLength > 0) {
                    onProgress(bytesSentTotal.toFloat() / contentLength.toFloat())
                }
            }
        }
        val channel: ByteReadChannel = response.body()
        out.outputStream().use { os ->
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                val bytes = packet.readBytes()
                packet.release()
                if (bytes.isNotEmpty()) os.write(bytes)
            }
        }
    }

    private suspend fun verifyChecksum(file: File, arch: String): Boolean {
        return try {
            val response = httpClient.get(
                "http://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/SHA256SUMS"
            )
            val text: String = response.body()
            val expected = text.lines()
                .firstOrNull { it.contains("ubuntu-base-22.04-base-$arch.tar.gz") }
                ?.split(" ")
                ?.firstOrNull()
                ?: return true
            val actual = file.sha256()
            expected.equals(actual, ignoreCase = true)
        } catch (e: Exception) {
            true
        }
    }

    private fun extractTarGz(tarGz: File, outDir: File) {
        outDir.mkdirs()
        val pb = ProcessBuilder(
            "tar", "-xzf", tarGz.absolutePath, "-C", outDir.absolutePath
        )
        pb.inheritIO()
        val process = pb.start()
        process.waitFor()
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
