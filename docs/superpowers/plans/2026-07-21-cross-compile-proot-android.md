# ForgeIdea Android 真机 proot 交叉编译实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans for inline execution.

**Goal:** 在仓库内建立可复现的交叉编译流程，生成 Android arm64-v8a 可用的 `libproot.so` 并打包进 APK。

**Architecture:** 脚本下载 proot 与 talloc 源码到 `app/proot-build/`，使用 Android NDK LLVM 工具链静态编译 talloc 后再编译 proot，输出到 `app/src/main/jniLibs/arm64-v8a/libproot.so`。Android 端 `ProotBinaryManager` 优先从 `nativeLibraryDir` 加载该产物。

**Tech Stack:** Bash, Android NDK r26d/r27c, Clang/LLVM, proot, talloc.

---

## 文件结构

```
app/
├── proot-build/                    # 构建目录（加入 .gitignore）
│   ├── proot/                      # proot 源码
│   ├── talloc/                     # talloc 源码
│   └── build.sh                    # 交叉编译脚本
├── src/main/jniLibs/
│   └── arm64-v8a/libproot.so       # 编译产物（提交到仓库）
└── src/main/java/com/forgeidea/terminal/
    └── ProotBinaryManager.kt       # 优先加载 libproot.so
scripts/
└── build-proot.sh                  # 一键入口脚本
```

---

## Task 1: 创建构建目录与 .gitignore

**Files:**
- Create: `app/proot-build/.gitkeep`
- Modify: `.gitignore`

- [ ] **Step 1: 创建构建目录**

Run:
```bash
mkdir -p /workspace/ForgeIdea/app/proot-build
```

- [ ] **Step 2: 修改 .gitignore 忽略构建中间文件**

Search for existing `*.so` or build ignore rules and append:

```gitignore
# proot build intermediates
app/proot-build/proot/
app/proot-build/talloc/
app/proot-build/talloc-install/
app/proot-build/*.tar.gz
app/proot-build/*.tar.xz
app/proot-build/ndk/
```

保留 `app/src/main/jniLibs/` 目录下的产物不被忽略。

---

## Task 2: 创建 build-proot.sh 一键入口

**Files:**
- Create: `scripts/build-proot.sh`

- [ ] **Step 1: 创建入口脚本**

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$REPO_ROOT/app/proot-build"
OUTPUT_DIR="$REPO_ROOT/app/src/main/jniLibs/arm64-v8a"
NDK_VERSION="r27c"

mkdir -p "$BUILD_DIR" "$OUTPUT_DIR"

bash "$BUILD_DIR/build.sh" "$BUILD_DIR" "$OUTPUT_DIR" "$NDK_VERSION"

echo "Build complete. Output: $OUTPUT_DIR/libproot.so"
file "$OUTPUT_DIR/libproot.so"
```

- [ ] **Step 2: 添加执行权限**

Run:
```bash
chmod +x /workspace/ForgeIdea/scripts/build-proot.sh
```

---

## Task 3: 创建核心交叉编译脚本

**Files:**
- Create: `app/proot-build/build.sh`

- [ ] **Step 1: 创建脚本并下载 NDK、proot、talloc**

```bash
#!/usr/bin/env bash
set -euo pipefail

BUILD_DIR="${1:-$(pwd)}"
OUTPUT_DIR="${2:-$BUILD_DIR/output}"
NDK_VERSION="${3:-r27c}"

PROOT_VERSION="5.3.1"
TALLOC_VERSION="2.4.2"

NDK_DIR="$BUILD_DIR/ndk/android-ndk-$NDK_VERSION"
TOOLCHAIN="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64"
AARCH64_CC="$TOOLCHAIN/bin/aarch64-linux-android21-clang"
AARCH64_AR="$TOOLCHAIN/bin/llvm-ar"
AARCH64_STRIP="$TOOLCHAIN/bin/llvm-strip"

mkdir -p "$BUILD_DIR" "$OUTPUT_DIR"

cd "$BUILD_DIR"

if [ ! -d "$NDK_DIR" ]; then
    echo "Downloading NDK $NDK_VERSION..."
    NDK_ZIP="android-ndk-$NDK_VERSION-linux.zip"
    wget -q "https://dl.google.com/android/repository/$NDK_ZIP" -O "$NDK_ZIP"
    unzip -q "$NDK_ZIP" -d "$BUILD_DIR/ndk/"
    rm "$NDK_ZIP"
fi

if [ ! -d "proot" ]; then
    echo "Downloading proot $PROOT_VERSION..."
    wget -q "https://github.com/proot-me/proot/archive/refs/tags/v$PROOT_VERSION.tar.gz" -O proot.tar.gz
    tar -xzf proot.tar.gz
    mv "proot-$PROOT_VERSION" proot
    rm proot.tar.gz
fi

if [ ! -d "talloc" ]; then
    echo "Downloading talloc $TALLOC_VERSION..."
    wget -q "https://www.samba.org/ftp/talloc/talloc-$TALLOC_VERSION.tar.gz" -O talloc.tar.gz
    tar -xzf talloc.tar.gz
    mv "talloc-$TALLOC_VERSION" talloc
    rm talloc.tar.gz
fi

TALLOC_INSTALL="$BUILD_DIR/talloc-install"
rm -rf "$TALLOC_INSTALL"
mkdir -p "$TALLOC_INSTALL"

echo "Building talloc static library..."
cd "$BUILD_DIR/talloc"
./configure \
    --cross-compile \
    --cross-execute=false \
    --hostcc=gcc \
    --prefix="$TALLOC_INSTALL" \
    --without-gettext \
    --without-python \
    --builtin-libraries=replace \
    --bundled-libraries=NONE \
    --disable-python \
    --disable-rpath
make
make install

echo "Building proot..."
cd "$BUILD_DIR/proot/src"
make clean || true
make V=1 \
    CC="$AARCH64_CC" \
    AR="$AARCH64_AR" \
    STRIP="$AARCH64_STRIP" \
    CFLAGS="-O2 -I$TALLOC_INSTALL/include -D_GNU_SOURCE" \
    LDFLAGS="-static -L$TALLOC_INSTALL/lib -ltalloc" \
    proot

cp proot "$OUTPUT_DIR/libproot.so"
$AARCH64_STRIP "$OUTPUT_DIR/libproot.so"

echo "Done. Output: $OUTPUT_DIR/libproot.so"
```

- [ ] **Step 2: 添加执行权限**

Run:
```bash
chmod +x /workspace/ForgeIdea/app/proot-build/build.sh
```

---

## Task 4: 调整 ProotBinaryManager 加载逻辑

**Files:**
- Modify: `app/src/main/java/com/forgeidea/terminal/ProotBinaryManager.kt`

- [ ] **Step 1: 优先从 nativeLibraryDir 加载 libproot.so**

```kotlin
package com.forgeidea.terminal

import android.content.Context
import android.os.Build
import java.io.File

class ProotBinaryManager(private val context: Context) {

    fun getProotPath(): String? {
        val nativeDir = context.applicationInfo.nativeLibraryDir ?: return findSystemProot()
        val bundled = File(nativeDir, "libproot.so")
        if (bundled.exists() && bundled.canExecute()) return bundled.absolutePath

        val legacy = File(nativeDir, "libproot.so")
        if (legacy.exists()) {
            legacy.setExecutable(true, false)
            if (legacy.canExecute()) return legacy.absolutePath
        }
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
```

---

## Task 5: 运行交叉编译脚本

**Files:**
- Create: `app/src/main/jniLibs/arm64-v8a/libproot.so`

- [ ] **Step 1: 执行编译脚本**

Run:
```bash
cd /workspace/ForgeIdea
./scripts/build-proot.sh
```

Expected output:
```
Downloading NDK r27c...
Downloading proot 5.3.1...
Downloading talloc 2.4.2...
Building talloc static library...
Building proot...
Done. Output: /workspace/ForgeIdea/app/src/main/jniLibs/arm64-v8a/libproot.so
/path/to/libproot.so: ELF 64-bit LSB shared object, ARM aarch64, version 1 (SYSV), statically linked, stripped
```

如果 talloc 配置失败，记录具体错误并调整 configure 参数或打 patch。

---

## Task 6: 验证产物与提交

- [ ] **Step 1: 检查产物存在且格式正确**

Run:
```bash
file /workspace/ForgeIdea/app/src/main/jniLibs/arm64-v8a/libproot.so
ls -lh /workspace/ForgeIdea/app/src/main/jniLibs/arm64-v8a/libproot.so
```

Expected:
- 文件存在
- `ARM aarch64`
- 大小在 100KB - 2MB 之间

- [ ] **Step 2: 提交变更**

Run:
```bash
cd /workspace/ForgeIdea
git add -A
git commit -m "feat: 交叉编译 proot 为 Android arm64-v8a libproot.so"
git push origin main
```

---

## 自检

1. **Spec 覆盖：** 源码下载、talloc 编译、proot 编译、Android 加载、产物提交均对应任务。
2. **Placeholder scan：** 无 TBD/TODO；脚本路径和版本号具体。
3. **类型一致性：** 无 Kotlin 类型变化。
