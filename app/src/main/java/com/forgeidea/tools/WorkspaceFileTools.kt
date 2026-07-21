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

    fun createFile(sessionId: String, path: String, content: String): String {
        val file = resolveWorkspaceFile(sessionId, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return "已创建文件: ${file.path}"
    }

    fun readFile(sessionId: String, path: String): String {
        val file = resolveWorkspaceFile(sessionId, path)
        if (!file.exists()) throw IOException("文件不存在: $path")
        return file.readText()
    }

    fun writeFile(sessionId: String, path: String, content: String): String {
        val file = resolveWorkspaceFile(sessionId, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return "已写入文件: ${file.path}"
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
                name = "create_file",
                description = "在工作区中创建新文件，如果目录不存在会自动创建",
                parameters = buildJsonObject {
                    putJsonObject("properties") {
                        putJsonObject("path") {
                            put("type", "string")
                            put("description", "相对工作区的文件路径")
                        }
                        putJsonObject("content") {
                            put("type", "string")
                            put("description", "文件内容")
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
                description = "读取工作区中文件的内容",
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
                name = "write_file",
                description = "覆盖写入工作区中的文件",
                parameters = buildJsonObject {
                    putJsonObject("properties") {
                        putJsonObject("path") {
                            put("type", "string")
                            put("description", "相对工作区的文件路径")
                        }
                        putJsonObject("content") {
                            put("type", "string")
                            put("description", "文件内容")
                        }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("path"))
                        add(JsonPrimitive("content"))
                    }
                }
            )
        )
    )

    suspend fun executeTool(sessionId: String, name: String, arguments: String): String {
        val args = Json.parseToJsonElement(arguments)
        val obj = args as? JsonObject ?: throw IllegalArgumentException("参数必须是 JSON 对象")
        val path = obj["path"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("缺少 path")
        return when (name) {
            "create_file" -> {
                val content = obj["content"]?.jsonPrimitive?.content ?: ""
                createFile(sessionId, path, content)
            }
            "read_file" -> readFile(sessionId, path)
            "write_file" -> {
                val content = obj["content"]?.jsonPrimitive?.content ?: ""
                writeFile(sessionId, path, content)
            }
            else -> throw IllegalArgumentException("未知工具: $name")
        }
    }
}
