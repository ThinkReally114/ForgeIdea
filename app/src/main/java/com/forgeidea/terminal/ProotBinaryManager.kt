package com.forgeidea.terminal

import android.content.Context
import android.os.Build
import java.io.File

class ProotBinaryManager(private val context: Context) {

    fun getProotPath(): String? {
        val nativeDir = context.applicationInfo.nativeLibraryDir ?: return findSystemProot()
        val so = File(nativeDir, "libproot.so")
        if (so.exists() && so.canExecute()) return so.absolutePath
        return findSystemProot()
    }

    private fun findSystemProot(): String? {
        val candidates = listOf(
            "/system/bin/proot",
            "/system/xbin/proot",
            "/data/data/com.termux/files/usr/bin/proot"
        )
        return candidates.firstOrNull { File(it).exists() && File(it).canExecute() }
    }

    fun getSupportedAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    fun abiToUbuntuArch(abi: String): String = when (abi) {
        "arm64-v8a" -> "arm64"
        "armeabi-v7a" -> "armhf"
        "x86_64" -> "amd64"
        "x86" -> "i386"
        else -> "arm64"
    }
}
