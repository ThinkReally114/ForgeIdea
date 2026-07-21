package com.forgeidea.tools

import android.content.Context
import com.forgeidea.llm.model.ToolDefinition
import com.forgeidea.llm.model.ToolFunction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.IOException

class WorkspaceFileTools(private val context: Context) {

    fun getWorkspaceRoot(sessionId: String): File {
        val root = File(context.filesDir, "workspace/$sessionId").apply { mkdirs() }
        return root
    }

    fun resolveWorkspaceFile(sessionId: String, path: String): File {
        val root = getWorkspaceRoot(sessionId).canonicalFile
        val normalized = path.removePrefix("/")
        val target = File(root, normalized).canonicalFile
        if (!target.path.startsWith(root.path)) {
            throw SecurityException("路径越界: $path")
        }
        return target
    }

    fun writeFile(sessionId: String, path: String, content: String): String {
        val file = resolveWorkspaceFile(sessionId, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return "已写入文件: ${file.path}"
    }

    fun readFile(sessionId: String, path: String): String {
        val file = resolveWorkspaceFile(sessionId, path)
        if (!file.exists()) throw IOException("文件不存在: $path")
        return file.readText()
    }

    fun listFiles(sessionId: String): List<String> {
        val root = getWorkspaceRoot(sessionId)
        return root.walkTopDown()
            .filter { it != root }
            .map { it.relativeTo(root).path }
            .toList()
    }

    fun buildToolDefinitions(): List<ToolDefinition> = listOf(
        ToolDefinition(
            function = ToolFunction(
                name = "write_file",
                description = "在工作区中创建新文件或覆盖已有文件，目录不存在会自动创建。当用户需要代码、文本文件或任何文件操作时优先使用此工具。",
                parameters = buildJsonObject {
                    putJsonObject("properties") {
                        putJsonObject("path") {
                            put("type", "string")
                            put("description", "相对工作区的文件路径，例如 src/main.kt 或 README.md")
                        }
                        putJsonObject("content") {
                            put("type", "string")
                            put("description", "要写入文件的完整内容")
                        }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("path"))
                        add(JsonPrimitive("content"))
                    }
                }
            )
        ),
        ToolDefinition(
            function = ToolFunction(
                name = "read_file",
                description = "读取工作区中已有文件的内容",
                parameters = buildJsonObject {
                    putJsonObject("properties") {
                        putJsonObject("path") {
                            put("type", "string")
                            put("description", "相对工作区的文件路径")
                        }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("path"))
                    }
                }
            )
        ),
        ToolDefinition(
            function = ToolFunction(
                name = "list_files",
                description = "列出当前工作区中的所有文件和目录",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {}
                }
            )
        )
    )

    suspend fun executeTool(sessionId: String, name: String, arguments: String): String {
        return try {
            val args = Json.parseToJsonElement(arguments)
            val obj = args as? JsonObject ?: return "错误：参数必须是 JSON 对象"
            when (name) {
                "write_file" -> {
                    val path = obj["path"]?.jsonPrimitive?.content ?: return "错误：缺少 path 参数"
                    val content = obj["content"]?.jsonPrimitive?.content ?: ""
                    writeFile(sessionId, path, content)
                }
                "read_file" -> {
                    val path = obj["path"]?.jsonPrimitive?.content ?: return "错误：缺少 path 参数"
                    readFile(sessionId, path)
                }
                "list_files" -> listFiles(sessionId).joinToString("\n").ifBlank { "工作区为空" }
                else -> "错误：未知工具 $name"
            }
        } catch (e: Exception) {
            "错误：${e.message ?: "执行工具失败"}"
        }
    }
}
