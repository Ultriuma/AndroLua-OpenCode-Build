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
├── README.md               # 本文件
├── OpenLuaEvo_v.1.6.4.apk  # OpenLuaEvo IDE 安装包（v1.6.4）
└── lua/
    └── bin.lua              # APK 打包脚本
```

## 文件说明 / File Descriptions

### `OpenLuaEvo_v.1.6.4.apk`

OpenLuaEvo IDE 的 APK 安装包，版本 v1.6.4。OpenLuaEvo 是 AndroLua+ 的社区增强分支，提供改进的编辑器体验与打包功能。

### `lua/bin.lua`

AndroLua 项目 APK 打包脚本，功能包括：

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
| OpenLuaEvo 社区 | Teal（`.tl`）支持、打包流程优化 |
| ULTRIUMA | 集成整理、版本封包、文档维护 |

> 本文件夹作为 AndroLua 生态的参考实现保留，不保证与最新版 AndroLua+ / OpenLuaEvo 的 API 变化同步。如遇问题请参考对应社区的最新文档。
>
> This folder is preserved as a reference implementation of the AndroLua ecosystem. Compatibility with the latest AndroLua+ / OpenLuaEvo APIs is not guaranteed. Consult the respective community docs for up-to-date information.
