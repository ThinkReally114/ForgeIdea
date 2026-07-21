# ForgeIdea Android 真机 proot 交叉编译设计文档

**日期：** 2026-07-21  
**作者：** ThinkReally114  
**状态：** 已批准，进入实现  
**关联需求：** 让 ForgeIdea 内置终端在 Android 真机上无需 Termux/Root 即可运行 proot Ubuntu 22.04

## 1. 背景与目标

### 1.1 背景
上一阶段已实现命令/响应式终端 UI、Git 面板和 Agent `/exec` 调用，但 `jniLibs/` 中没有真实的 `libproot.so`。真机运行时 `ProotBinaryManager` 无法定位 proot，会抛出 `proot 不可用` 错误。

### 1.2 目标
在仓库内建立一套可复现的 proot 交叉编译流程，生成 Android 可用的 `libproot.so`，使 ForgeIdea 在主流 arm64 真机上能独立完成 proot Ubuntu 环境初始化。

## 2. 关键决策

| 决策项 | 选择 | 理由 |
|---|---|---|
| proot 来源 | 仓库内 NDK 交叉编译 | 不依赖 Termux/Root，真机体验最好 |
| 依赖处理 | 静态链接 libtalloc | talloc 是 proot 唯一主要依赖，静态链接减少 so 数量和运行时风险 |
| 首批 ABI | arm64-v8a | 覆盖主流真机，验证流程后再扩展到其他 ABI |
| 产物位置 | `app/src/main/jniLibs/arm64-v8a/libproot.so` | Android 自动打包并可从 nativeLibraryDir 执行 |
| 源码管理 | 脚本自动下载源码到 `app/proot-build/` | 不引入 git submodule，减少仓库体积 |
| NDK 版本 | r26d / r27c | 稳定且支持 LLVM 工具链 |

## 3. 架构

```
仓库根目录/
├── app/
│   ├── src/main/jniLibs/
│   │   └── arm64-v8a/libproot.so      # 编译产物
│   └── proot-build/                   # 构建目录（gitignore）
│       ├── proot/                     # proot 源码
│       ├── talloc/                    # talloc 源码
│       └── build.sh                   # 交叉编译脚本
├── scripts/
│   └── build-proot.sh                 # 一键编译入口
└── docs/superpowers/specs/            # 本设计文档
```

## 4. 编译流程

### 4.1 源码下载
- proot: 从 GitHub release 或固定 tag 下载 tarball
- talloc: 从 Samba 官方下载固定版本 tarball

### 4.2 工具链设置
使用 NDK 的 LLVM 工具链：
- 架构前缀：`aarch64-linux-android21`
- CC/CXX：`$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang`
- AR/LD/STRIP：对应 llvm 工具

### 4.3 talloc 静态库编译
```bash
cd talloc
./configure --cross-compile --cross-execute=false \
  --hostcc=gcc --prefix=$BUILD_DIR/talloc-install \
  --without-gettext --without-python
make
make install
```

### 4.4 proot 编译
修改 proot 的 Makefile 或传入变量：
- `PROOT_UNBUNDLE_LOADER` 视情况而定
- `CFLAGS` 包含 talloc 头文件路径
- `LDFLAGS` 包含静态 talloc 库
- 输出文件重命名为 `libproot.so`

```bash
cd proot/src
make V=1 \
  CC=$AARCH64_CC \
  CFLAGS="-O2 -I$TALLOC_INSTALL/include" \
  LDFLAGS="-static -L$TALLOC_INSTALL/lib -ltalloc" \
  proot
cp proot $OUTPUT_DIR/libproot.so
```

## 5. Android 侧调整

### 5.1 ProotBinaryManager
优先从 `nativeLibraryDir` 查找 `libproot.so`，找不到再 fallback 到系统 proot。

### 5.2 build.gradle.kts
确保 `sourceSets["main"].jniLibs.srcDirs` 包含默认路径，无需额外配置。

### 5.3 首次启动流程
1. 检查 `libproot.so` 是否存在
2. 检查 rootfs 是否就绪
3. 若未就绪，下载 Ubuntu base rootfs 并初始化 apt/git/python3

## 6. Phase 1 范围

### 必做
- [ ] 下载 proot 和 talloc 源码脚本
- [ ] 编写 arm64-v8a 交叉编译脚本
- [ ] 生成真实 `libproot.so` 并放入 jniLibs
- [ ] 调整 `ProotBinaryManager` 加载逻辑
- [ ] 验证产物可被 `Runtime.exec()` 执行

### 不做
- 其他 ABI（Phase 2）
- CI 自动编译提交产物（Phase 2）
- proot loader 动态加载优化（Phase 2）

## 7. 风险与应对

| 风险 | 应对 |
|---|---|
| talloc 配置脚本不识别 Android 工具链 | 手动写 waf configure 参数或打 patch |
| proot Makefile 写死 gcc | 传 CC/LD/AR 覆盖 |
| 静态链接后 so 体积大 | 用 `-O2 -s` 和 strip |
| 真机 seccomp 拦截 proot | 初始化前检测并在 UI 提示重置环境 |
| NDK 未安装 | 脚本自动从 Google 下载并缓存 |

## 8. 验证标准

- `./scripts/build-proot.sh` 在 Ubuntu 22.04 x86_64 上成功运行
- 输出 `app/src/main/jniLibs/arm64-v8a/libproot.so`
- `file libproot.so` 显示 `ARM aarch64, version 1 (SYSV), dynamically linked`
- 安装 APK 后，TerminalScreen 不再报 `proot 不可用`
