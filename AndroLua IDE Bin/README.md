# AndroLua IDE Bin Library

[![license](https://img.shields.io/github/license/ULTRIUMA/AndroLua-OpenCode-Build.svg)](LICENSE)
[![releases](https://img.shields.io/github/v/tag/ULTRIUMA/AndroLua-OpenCode-Build?color=C71D23&label=releases&logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build/releases)
[![Github repository](https://img.shields.io/badge/Github-repository-0969DA?logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build)

---

> **⚠ 历史遗产声明 / Legacy Notice**
>
> 本文件夹内容源自**多个不同时期、不同开发者**的贡献与迭代，属于 AndroLua 生态的**历史遗留产物**。各文件可能来自不同的项目分支、不同的 AndroLua 版本适配期，代码风格与接口约定不完全统一。使用前请结合对应时期的文档或社区讨论理解其上下文。
>
> The contents of this folder are **legacy artifacts** contributed by **multiple developers across different time periods** within the AndroLua ecosystem. Files may originate from different project branches and different eras of AndroLua version compatibility. Code style and API conventions may vary. Please refer to the corresponding period's documentation or community discussions for context.

---

## 目录 / Contents

```
AndroLua IDE Bin/
├── README.md
├── lua/
│   └── bin.lua                              # APK 打包脚本
├── OpenLuaEvo_1.6.4_sign.APK               # 通用签名版（all ABI）
├── OpenLuaEvo_1.6.4_sign_arm64-v8a.APK     # arm64-v8a 签名版
├── OpenLuaEvo_1.6.4_sign_armeabi-v7a.APK   # armeabi-v7a 签名版
├── OpenLuaEvo_1.6.4_sign_armeabi.APK       # armeabi 签名版
└── *.APK.idsig                              # 对应签名验证文件
```

## APK 说明

OpenLuaEvo v1.6.4 已签名版本，按目标架构分发：

| 文件 | 目标 ABI | 适用设备 |
| --- | --- | --- |
| `OpenLuaEvo_1.6.4_sign.APK` | all | 通用，包含多 ABI native 库 |
| `OpenLuaEvo_1.6.4_sign_arm64-v8a.APK` | arm64-v8a | 64 位 ARM 设备（主流） |
| `OpenLuaEvo_1.6.4_sign_armeabi-v7a.APK` | armeabi-v7a | 32 位 ARM 设备 |
| `OpenLuaEvo_1.6.4_sign_armeabi.APK` | armeabi | 旧版 ARM 设备 |

> `.idsig` 文件为 APK 签名验证附加数据，随 APK 一同分发。

## `lua/bin.lua` — APK 打包脚本

AndroLua 项目打包为独立 APK 的核心脚本，功能包括：

- 读取工程 `init.lua` 配置（包名、版本号、应用名等）
- 递归扫描 Lua 源文件，编译为 `.luac`
- 支持 `.tl`（Teal）类型注解文件转 Lua 后编译
- 依赖分析：自动提取 `require` / `import` 引用的模块并打入 APK
- 修改 `AndroidManifest.xml`：替换包名、版本号、权限声明
- 调用 `apksigner` 签名并触发安装

**用法 / Usage：**

```lua
require "import"
local bin = require "bin"

-- 打包当前工程
bin(activity.getLuaExtDir("project/myapp") .. "/")
```

**依赖 / Dependencies：**

- `console`（AndroLua 内置编译器）
- `mao.res.AXmlDecoder`（Android XML 解析）
- `apksigner.Signer`（APK 签名）
- `androidx.appcompat` / `androidx.core`（FileProvider 兼容）

## 来源与维护 / Provenance

| 来源 | 说明 |
| --- | --- |
| AndroLua+ 官方 | 基础打包框架、console 编译器接口 |
| OpenLuaEvo 社区 | Teal（`.tl`）支持、打包流程优化、签名 APK 分发 |
| ULTRIUMA | 集成整理、版本封包、文档维护 |

> 本文件夹作为 AndroLua 生态的参考实现保留，不保证与最新版 AndroLua+ / OpenLuaEvo 的 API 变化同步。如遇问题请参考对应社区的最新文档。
>
> This folder is preserved as a reference implementation of the AndroLua ecosystem. Compatibility with the latest AndroLua+ / OpenLuaEvo APIs is not guaranteed. Consult the respective community docs for up-to-date information.
