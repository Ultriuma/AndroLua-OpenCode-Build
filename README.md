# ULTRIUMA AndroLua Native Enhancement Libraries

[![license](https://img.shields.io/github/license/ULTRIUMA/AndroLua-OpenCode-Build.svg)](LICENSE)
[![releases](https://img.shields.io/github/v/tag/ULTRIUMA/AndroLua-OpenCode-Build?color=C71D23&label=releases&logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build/releases)
![](https://img.shields.io/github/last-commit/ULTRIUMA/AndroLua-OpenCode-Build.svg)
[![Github repository](https://img.shields.io/badge/Github-repository-0969DA?logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build)
![QQ: 2957148920](https://img.shields.io/badge/QQ-2957148920-0099FF?logo=tencentqq)

---

> 一套面向 **AndroLua / Lua** 的 Android 原生增强资源，由 **ULTRIUMA** 编译维护，用于为 AndroLua 项目补充 Markdown 渲染、高性能 KV 存储与一组预编译的原生 `.so` 库。
> A curated set of Android native extensions for **AndroLua / Lua**, compiled and maintained by **ULTRIUMA**, adding Markdown rendering, high-performance KV storage, and a collection of prebuilt native `.so` libraries to AndroLua projects.

---

## 目录 / Table of Contents

- [项目概览 / Overview](#项目概览--overview)
- [目录结构 / Directory Structure](#目录结构--directory-structure)
- [1. Markdown 渲染 / Markdown Rendering](#1-markdown-渲染--markdown-rendering)
  - [1.1 MarkdownView（`markdown.dex`）](#11-markdownviewmarkdowndex)
  - [1.2 markwonx（`markwonx.dex`）](#12-markwonxmarkwonxdex)
- [2. MMKV Lua 绑定 / MMKV Lua Binding](#2-mmkv-lua-绑定--mmkv-lua-binding)
- [3. 原生库集合 / Native Library Collection](#3-原生库集合--native-library-collection)
- [4. 快速开始 / Quick Start](#4-快速开始--quick-start)
- [5. 许可证 / License](#5-许可证--license)

---

## 项目概览 / Overview

本仓库（文件夹）整合了三类可直接用于 AndroLua 的原生资源，全部以 `.dex` 或 `.so` 形式分发，无需源码即可集成。
This repository bundles three kinds of drop-in AndroLua resources, shipped as `.dex` or `.so` — no source build required to integrate.

| 模块 / Module | 用途 / Purpose | 产物 / Artifact |
| --- | --- | --- |
| `markdown/` | 在 Android 上渲染 Markdown（WebView 方案 + Markwon 方案） | `MarkdownView_v1.2.0.dex`、`MarkwonX_v1.0.0.dex` |
| `mmkv/` | 把腾讯 MMKV 编译成单个 `libmmkv.so`，供 Lua `require("mmkv")` 直接调用 | `libmmkv.so` + `LuaMMKV.lua` |
| `Nativelib/` | 28 个预编译原生库（base64 / lfs / lpeg / sqlite / zip / opencc …）+ `libc++_shared.so` | 3 个 ABI × 29 个 `.so` |

编译信息 / Build info：MMKV 构建作者 **ULTRIUMA**，上游版本 **MMKV 2.4.2**，构建时间 **2026-08-30T07:24:59Z**；原生库使用 NDK 26.1.10909125 编译。
Build info: MMKV author **ULTRIUMA**, upstream **MMKV 2.4.2**, built **2026-08-30T07:24:59Z**; native libs built with NDK 26.1.10909125.

---

## 目录结构 / Directory Structure

```
新建文件夹/
├── License                 # Apache-2.0（顶层）
├── markdown/               # Markdown 渲染组件
│   ├── License             # Apache-2.0
│   ├── MARKDOWN.md              # MarkdownView 文档
│   ├── MARKDWONX.md             # MarkwonX 文档（文件名 typo，内容为 markwonx）
│   ├── MarkdownView_v1.0.0.dex  # ≈1.8 MB，WebView 方案（com.ocssfun.markdown）
│   ├── MarkdownView_v1.2.0.dex  # ≈1.8 MB，v1.2.0 修复同名重载问题
│   └── MarkwonX_v1.0.0.dex     # ≈7.6 MB，Markwon 方案（com.ocssfun.markwon）
├── mmkv/                   # MMKV Lua 绑定
│   ├── License             # Apache-2.0
│   ├── LuaMMKV.md          # 完整文档
│   ├── LuaMMKV.lua         # 链式调用封装（require "mmkv_wrap"）
│   └── lib/
│       ├── arm64-v8a/      # libmmkv.so + libc++_shared.so
│       ├── armeabi/
│       └── armeabi-v7a/
└── Nativelib/              # 预编译原生库集合
    ├── arm64-v8a/          # 29 个 .so
    ├── armeabi/
    └── armeabi-v7a/
```

> 注：`markdown/` 下的 `MARKDWONX.md` 文件名拼写有误（应为 `MARKWONX`），但其内容描述的是 `markwonx.dex`，请以内容为据。
> Note: the file `MARKDWONX.md` has a typo in its name; its content documents `markwonx.dex`. Trust the content, not the filename.

---

## 1. Markdown 渲染 / Markdown Rendering

### 1.1 MarkdownView（`MarkdownView_v1.2.0.dex`）

**中文** — 继承自 `WebView` 的离线 Markdown 渲染控件，编译为独立 `classes.dex`，供 AndroLua `loadDex` 加载。完全离线（marked 12 / highlight.js 11 / KaTeX 0.16 已内联，无任何外部依赖），内置 ULTRIUMA / GitHub 双样式并均带完整亮暗双主题。包名 `com.ocssfun.markdown`，产物约 1.8 MB（min-api 21），第三方 Java 依赖为无。支持图片自动 Base64 内联、统计、渲染开关与自定义 CSS 注入。

**English** — An offline Markdown rendering widget extending `WebView`, compiled into a standalone `classes.dex` for AndroLua `loadDex`. Fully offline (marked 12 / highlight.js 11 / KaTeX 0.16 inlined, zero external deps), with two built-in styles (ULTRIUMA / GitHub) each offering complete light & dark themes. Package `com.ocssfun.markdown`, ~1.8 MB (min-api 21), no third-party Java dependencies. Supports automatic Base64 image inlining, statistics, render toggles, and custom CSS injection.

**v1.2.0 变更 / Changelog:**

v1.2.0 修复了 `setMarkdown(File)` 与 `setMarkdown(String)` 同名重载导致 AndroLua 反射分派静默失败（白屏）的问题。已移除 File 重载，新增唯一命名的 `setMarkdownFile(File)` / `setMarkdownText(String)`。
v1.2.0 fixes the silent failure (white screen) caused by `setMarkdown(File)` and `setMarkdown(String)` having the same method name, which confused AndroLua's reflection dispatcher. The File overload has been removed; `setMarkdownFile(File)` and `setMarkdownText(String)` are now the unique entry points.

最小用法 / Minimal usage：

```lua
require "import"
import "com.ocssfun.markdown.*"
import "com.ocssfun.markdown.MarkdownView"

local mdv = MarkdownView(activity)
frame.addView(mdv)
mdv.setMarkdownText("# 标题\n正文 **加粗** `code`")
-- 或从文件加载 / or load from file:
-- mdv.setMarkdownFile("/sdcard/README.md")
mdv.setMarkdownStyle(MarkdownView.STYLE_GITHUB)
mdv.setDarkMode(MarkdownView.FORCE_DARK_AUTO)
```

### 1.2 MarkwonX（`MarkwonX_v1.0.0.dex`）

**中文** — 基于 **Markwon 4.6.2 + Prism4j** 的 Markdown 渲染 dex，已自带深色主题、圆角引用块/行内代码、可点链接、图片与 shields.io SVG 徽章。版本 v7.4.4，约 7.6 MB，5844 个类，单 `classes.dex`（dex 035，min-api 21）。**不含 LaTeX 公式**（v7.4.x 起移除）。注意：加载网络图片需宿主 APK 声明 `android.permission.INTERNET`。

**English** — A Markdown rendering dex built on **Markwon 4.6.2 + Prism4j**, with built-in dark theme, rounded blockquotes / inline code, clickable links, images, and shields.io SVG badges. Version v7.4.4, ~7.6 MB, 5844 classes, single `classes.dex` (dex 035, min-api 21). **No LaTeX math** (removed since v7.4.x). Note: loading remote images requires `android.permission.INTERNET` in the host APK.

最小用法 / Minimal usage：

```lua
import("com.ocssfun.markwon.MarkwonSetup")
local mk = MarkwonSetup.create(activity)
mk:setMarkdown(textView, "# 标题\n\n`inline code`\n\n```lua\nprint('hi')\n```")
```

> 选型建议 / Recommendation: 需要**离线、带公式、WebView 内显示**用 `MarkdownView`；需要**嵌入 TextView、体积小依赖少、可追加 Markwon 插件**用 `markwonx`。
> Use `MarkdownView` when you need **offline rendering, math support, and a WebView surface**; use `markwonx` when you want **TextView embedding, fewer deps, and extensible Markwon plugins**.

---

## 2. MMKV Lua 绑定 / MMKV Lua Binding

**中文** — 把腾讯 [MMKV](https://github.com/Tencent/MMKV)（v2.4.2）的 C++ Core 与一层 Lua 5.3 绑定（`lua_mmkv.cpp`）编译成**单个** `libmmkv.so`，作为 AndroLua 的原生库直接 `require("mmkv")` 使用。关键点：官方 nativelib 未导出 `luaopen_*`，故把 Core + wrapper 用同一 NDK、静态链 `c++_static`（`-static-libstdc++`）编进一个 so，使依赖链回到 `libluajava.so` + 系统库，从而能被 AndroLua 的 `package.cpath` + `dlopen` 加载。

**English** — Compiles Tencent [MMKV](https://github.com/Tencent/MMKV) (v2.4.2) C++ Core plus a Lua 5.3 binding (`lua_mmkv.cpp`) into a **single** `libmmkv.so`, usable directly from AndroLua via `require("mmkv")`. Key detail: the official nativelib only exports C++ mangled symbols (no `luaopen_*`), so Core + wrapper are compiled with the same NDK and statically linked `c++_static` (`-static-libstdc++`) into one `.so`, collapsing the dependency chain back to `libluajava.so` + system libs so that AndroLua's `package.cpath` + `dlopen` can load it.

Lua API 概览 / Lua API overview：

| 函数 / Function | 说明 / Purpose |
| --- | --- |
| `mmkv.init(rootDir [, logLevel])` | 初始化根目录，须先于 `default`/`withID` 调用 / init root dir, call before `default`/`withID` |
| `mmkv.default()` / `defaultMMKV()` | 默认实例（后者为编辑器高亮安全别名）/ default instance |
| `mmkv.withID(id [, mode])` | 具名实例 / named instance |
| `kv:set(key, value)` | 写入，按 Lua 类型自动分派 / set, auto-dispatched by Lua type |
| `kv:getString/getInt/getBool/getDouble(key [, def])` | 类型化读取 / typed getters |
| `kv:remove/clear/count/allKeys/sync/close/totalSize` | 管理与生命周期 / management & lifecycle |

`LuaMMKV.lua` 在原始模块上额外提供链式调用封装：

```lua
local mmkv = require("mmkv_wrap")
local kv = mmkv.open("mydata", mmkv.MULTI_PROCESS)
kv:set("name", "团子"):set("age", 3):setTable("config", { theme = "dark" }):sync()
print(kv:getString("name"))        --> 团子
print(kv:getTable("config").theme) --> dark
```

**重要坑点 / Important gotchas：**
- `close()` 后该 `kv` 变为悬垂指针，绝不可再调用其方法 / after `close()` the instance is a dangling pointer — never call its methods again.
- 无 `__gc` 终结器；`withID` 临时实例用毕应 `close()`，否则累积 mmap/fd / no `__gc`; close `withID` temp instances to avoid fd/mmap leaks.
- `LOG_DEBUG` 在 release 构建中无效 / `LOG_DEBUG` is compiled out in release builds.
- 二进制 blob 接口（`setData`/`getData`）尚未暴露，存 LuaTable 请走 `cjson` / binary blob API not yet exposed; serialize LuaTable via `cjson`.

部署方式 / Deployment：方案 A 推到 project 目录运行时用 `io` 复制 so 到 `app_lib`；方案 B 将 so 放入 APK 对应 `lib/<abi>/` 后 zipalign + apksigner 重签。
Deployment: Plan A pushes the `.so` to the project dir and copies it into `app_lib` at runtime via `io`; Plan B places the `.so` into the APK's `lib/<abi>/` then re-signs with zipalign + apksigner.

---

## 3. 原生库集合 / Native Library Collection

**中文** — `Nativelib/` 内含 28 个预编译原生库 + `libc++_shared.so` 运行库，按 `arm64-v8a`、`armeabi`、`armeabi-v7a` 三个 ABI 各 29 个 `.so` 分发。可直接被 AndroLua `require` 或 `loadDex` 引用。

**English** — `Nativelib/` ships 28 prebuilt native libraries plus the `libc++_shared.so` runtime, distributed as 29 `.so` files per ABI across `arm64-v8a`, `armeabi`, and `armeabi-v7a`. They can be `require`d or `loadDex`'d directly from AndroLua.

| 库 / Library | 功能 / Function |
| --- | --- |
| `libbase64.so` | Base64 编解码 / Base64 codec |
| `libbson.so` | BSON 序列化 / BSON serialization |
| `libcjson.so` | JSON 编解码（MMKV 文档依赖）/ JSON codec (used by MMKV docs) |
| `libcanvas.so` | 画布绘制 / canvas drawing |
| `libcrypt.so` | 加密相关 / crypto helpers |
| `libgl.so` | OpenGL 绑定 / OpenGL binding |
| `liblfs.so` | LuaFileSystem（文件/目录操作）/ LuaFileSystem |
| `liblpeg.so` | LPeg 模式匹配 / LPeg parsing |
| `liblsqlite3.so` | LuaSQLite3（链式 SQLite）/ LuaSQLite3 |
| `libLuaBoost.so` | Lua 增强工具集 / Lua boost utilities |
| `libluajava.so` | Lua ↔ Java 桥接（关键依赖）/ Lua↔Java bridge (key dep) |
| `libluv.so` | libuv 异步 I/O / libuv async I/O |
| `libmd5.so` | MD5 摘要 / MD5 digest |
| `libmime.so` | MIME 类型处理 / MIME handling |
| `libmmkv.so` | MMKV 绑定（同 mmkv/）/ MMKV binding (same as mmkv/) |
| `libndkbitmap.so` | NDK Bitmap 操作 / NDK bitmap ops |
| `libopencc.so` | 简繁中文转换 / OpenCC Chinese conversion |
| `librawio.so` | 原始 I/O / raw I/O |
| `libregex.so` | 正则匹配 / regex |
| `libsensor.so` | 传感器访问 / sensor access |
| `libsocket.so` | 网络套接字 / network sockets |
| `libsqlite3.so` | SQLite 原生引擎 / SQLite engine |
| `libstruct.so` | 二进制结构体打包 / binary struct packing |
| `libtcc.so` | TinyCC 内嵌编译器 / TinyCC in-process compiler |
| `libthreads.so` | 多线程支持 / multithreading |
| `libxml.so` | XML 解析 / XML parsing |
| `libyaml.so` | YAML 解析 / YAML parsing |
| `libzip.so` | ZIP 压缩/解压 / ZIP compress/extract |
| `libzlib.so` | zlib 压缩 / zlib compression |
| `libc++_shared.so` | C++ 运行时（共享 STL）/ C++ runtime (shared STL) |

> 注意：若你的工程已静态链 `c++_static`（如 `libmmkv.so`），应避免同时加载 `libc++_shared.so` 造成 STL 重复；两者取其一即可。
> Note: if your project statically links `c++_static` (e.g. `libmmkv.so`), avoid also loading `libc++_shared.so` to prevent duplicate STL; pick one.

---

## 4. 快速开始 / Quick Start

**中文** — 将所需文件放入 AndroLua 工程的 `library/`（或对应 `lib/<abi>/`）目录，运行时按需 `loadDex` / `require`：

**English** — Place the needed files into your AndroLua project's `library/` (or the matching `lib/<abi>/`) directory, then `loadDex` / `require` at runtime:

```lua
-- Markdown 渲染 / Markdown rendering
require "import"
import("com.ocssfun.markwon.MarkwonSetup")
local mk = MarkwonSetup.create(activity)
mk:setMarkdown(tv, "# Hello ocssfun")

-- KV 存储（需先把 libmmkv.so 推到 app_lib）/ KV store (push libmmkv.so to app_lib first)
local mmkv = require("mmkv")
mmkv.init(activity.getFilesDir().getAbsolutePath() .. "/mmkv", mmkv.LOG_NONE)
local kv = mmkv.defaultMMKV()
kv:set("k", "v"); print(kv:getString("k"))
```

通用提示 / General notes：
- 换 dex / so 后务必清掉旧缓存（含 `optimizedDirectory` 旧 odex），否则可能一直显示旧版 / after swapping a dex/so, clear old caches (incl. stale odex in `optimizedDirectory`) or the old version may persist.
- markwonx 加载网络图片需 `INTERNET` 权限 / markwonx needs `INTERNET` permission for remote images.

---

## 5. 许可证 / License

本仓库全部内容（含顶层与 `markdown/`、`mmkv/` 下的 `License` 文件）均采用 **Apache License 2.0**。
All contents in this repository (including the top-level and the `License` files under `markdown/` and `mmkv/`) are licensed under the **Apache License 2.0**.

- 文本全文：<http://www.apache.org/licenses/LICENSE-2.0>
- 第三方组件各自的许可证（marked / highlight.js / KaTeX / Markwon / Prism4j / MMKV 等）以其上游声明为准，本项目仅做集成与编译分发。
  Third-party components (marked / highlight.js / KaTeX / Markwon / Prism4j / MMKV, etc.) retain their own upstream licenses; this repo only integrates and redistributes the compiled artifacts.

> 编译与维护 / Compiled & maintained by **ULTRIUMA**. 衍生修改请遵守 Apache-2.0 的署名与变更声明要求。
> Derivative modifications must comply with Apache-2.0 attribution and "changed files" notice requirements.
