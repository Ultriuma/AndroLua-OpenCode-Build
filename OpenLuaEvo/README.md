# OpenLuaEvo

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
OpenLuaEvo/
├── README.md               # 本文件
├── build-1/                # v1.6.4 初始签名版
│   ├── OpenLuaEvo_1.6.4_sign.APK
│   ├── OpenLuaEvo_1.6.4_sign_arm64-v8a.APK
│   ├── OpenLuaEvo_1.6.4_sign_armeabi-v7a.APK
│   ├── OpenLuaEvo_1.6.4_sign_armeabi.APK
│   └── *.APK.idsig
├── build-2/                # v1.6.4-build2 修正版
│   ├── OpenLuaEvo_1.6.4-build2_sign.APK
│   ├── OpenLuaEvo_1.6.4-build2_arm64-v8a.APK
│   ├── OpenLuaEvo_1.6.4-build2_armeabi-v7a.APK
│   ├── OpenLuaEvo_1.6.4-build2_armeabi.APK
│   └── *.APK.idsig
├── lua/                    # Lua 脚本
│   ├── bin.lua             # APK 打包脚本
│   └── README.md
├── utils/                  # 工具脚本
│   ├── LuaIcon.lua         # 图标工具
│   └── README.md
└── html/                   # 文档
    ├── document/
    └── README.md
```

## 版本说明 / Versions

| 版本 | 说明 | 下载 |
| --- | --- | --- |
| v1.6.4 (build-1) | 初始签名版，多 ABI 分发 | [Release](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build/releases/tag/androlua-ide-bin-v1.6.4) |
| v1.6.4-build2 | 修正 res/drawable 缺失、替换测试图标、bin.lua 自动打包 slot 图标 | [Release](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build/releases/tag/openluaevo-v1.6.4-build2) |

## Build-2 更新内容 / Build-2 Changelog

1. 修正 `res/drawable` 内部缺失的内容
2. 替换 `res/drawable/slot1.png` ~ `slot8.png` 测试图标为 Android 机器人 LOGO 图标
3. 修正 `bin.lua` 使得打包时自动把项目 `/icon/slots/` 文件夹内的 `slot1-slot8.png` 打包进安装包 `res/drawable`

## 来源与维护 / Provenance

| 来源 | 说明 |
| --- | --- |
| AndroLua+ 官方 | 基础打包框架、console 编译器接口 |
| OpenLuaEvo 社区 | Teal（`.tl`）支持、打包流程优化、签名 APK 分发 |
| ULTRIUMA | 集成整理、版本封包、文档维护 |

> 本文件夹作为 AndroLua 生态的参考实现保留，不保证与最新版 AndroLua+ / OpenLuaEvo 的 API 变化同步。如遇问题请参考对应社区的最新文档。
