package com.forgeidea.tools

import com.forgeidea.terminal.ShellExecutor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

class ExecuteCommandTool(private val shellExecutor: ShellExecutor) : Tool {

    override val name = "execute_command"
    override val description = "在本地 Ubuntu 22.04 proot 环境中执行 shell 命令，返回 stdout/stderr/exitCode。"

    override val schema = buildJsonObject {
        put("type", JsonPrimitive("object"))
        putJsonObject("properties") {
            putJsonObject("command") {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive("要执行的 shell 命令"))
            }
            putJsonObject("cwd") {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive("工作目录，默认 /workspace"))
            }
        }
        put("required", JsonArray(listOf(JsonPrimitive("command"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult {
        val command = (args["command"] as? JsonPrimitive)?.content
            ?: return ToolResult.error("缺少 command 参数")
        val cwd = (args["cwd"] as? JsonPrimitive)?.content
        val result = shellExecutor.execute(command, cwd)
        return result.fold(
            onSuccess = { record ->
                ToolResult.success(
                    buildJsonObject {
                        put("stdout", JsonPrimitive(record.stdout))
                        put("stderr", JsonPrimitive(record.stderr))
                        put("exitCode", JsonPrimitive(record.exitCode))
                    }
                )
            },
            onFailure = { error ->
                ToolResult.error(error.message ?: "执行失败")
            }
        )
    }
}
