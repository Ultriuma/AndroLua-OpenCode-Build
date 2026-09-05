# MarkwonX

[![license](https://img.shields.io/github/license/ULTRIUMA/AndroLua-OpenCode-Build.svg)](LICENSE)
[![releases](https://img.shields.io/github/v/tag/ULTRIUMA/AndroLua-OpenCode-Build?color=C71D23&label=releases&logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build/releases)
![](https://img.shields.io/github/last-commit/ULTRIUMA/AndroLua-OpenCode-Build.svg)
[![Github repository](https://img.shields.io/badge/Github-repository-0969DA?logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build)
![QQ: 2957148920](https://img.shields.io/badge/QQ-2957148920-0099FF?logo=tencentqq)

---

AndroLua / Lua 可直接 `import` 调用的 **Markdown 渲染 dex**：Markwon 4.6.2 + Prism4j 语法高亮，自带深色主题、圆角引用块与圆角行内代码、可点链接、图片与 shields.io SVG 徽章。

| 项 | 值 |
|---|---|
| 版本 | v7.4.4 |
| 体积 | 7,868,604 字节（约 7.50 MB） |
| 类数 | 5844 |
| 格式 | 单 `classes.dex`，dex 035，`min-api 21` |
| MD5 | `c1574c12758327a389979c26dc3b0866` |
| 公式 | **不含**（v7.4.x 已移除） |

---

## 快速开始

```lua
-- 1. 加载 dex
import("com.ocssfun.markwon.MarkwonSetup")

-- 2. 创建实例并渲染
local mk = MarkwonSetup.create(activity)
mk:setMarkdown(textView, "# 标题\n\n`inline code`\n\n```lua\nprint('hi')\n```")
```

`MarkwonSetup.create(context)` 内部已串好全部插件，无需任何额外配置。

---

## API

### `com.ocssfun.markwon.MarkwonSetup`

| 方法 | 返回 | 说明 |
|---|---|---|
| `create(Context)` | `Markwon` | 开箱即用的实例（语法高亮 + 主题 + 圆角 + 图片） |
| `builder(Context)` | `Markwon.Builder` | 已预置上述插件的 builder，**可继续 `usePlugin` 追加自己的插件** |
| `enableHorizontalScroll(TextView)` | `void` | 整个 TextView 开启横向滚动（代码块/表格长行用） |

### `com.ocssfun.markwon.RoundedStylePlugin`

| 方法 | 说明 |
|---|---|
| `create(float radius)` | 圆角插件，`radius` 单位 px。引用块与行内代码的圆角半径 |

自定义圆角（不用默认的 8f）：

```lua
local RoundedStylePlugin = cl.loadClass("com.ocssfun.markwon.RoundedStylePlugin")
local mk = MarkwonSetup.builder(activity)
  .usePlugin(RoundedStylePlugin.create(12))     -- 覆盖默认圆角
  .build()
```

### 追加自己的插件

`builder()` 返回的是标准 `Markwon.Builder`，可继续链式调用：

```lua
import("io.noties.markwon.ext.strikethrough.StrikethroughPlugin")
import("io.noties.markwon.ext.tables.TablePlugin")
import("io.noties.markwon.ext.tasklist.TaskListPlugin")

local mk = MarkwonSetup.builder(activity)
  .usePlugin(StrikethroughPlugin.create())
  .usePlugin(TablePlugin.create(activity))
  .usePlugin(TaskListPlugin.create(activity))
  .build()
```

> dex 已内置这些扩展模块的类，只是默认未挂载，需要哪个就 `usePlugin` 哪个。

### 其它内置类

| 类 | 说明 |
|---|---|
| `io.noties.markwon.core.MarkwonTheme` | 主题（配色由 `MarkwonSetup` 内部设置） |
| `io.noties.markwon.PrecomputedTextSetterCompat` | 大文本异步排版（见下） |
| `com.ocssfun.markwon.GrammarLocatorDef` | Prism4j 语法定位器（已默认接入） |

---

## 渲染能力

### 语法高亮

Prism4j，支持语言及别名：

```
java  kotlin  javascript/js  json  css  html/xml/markup
python/py  c  c++/cpp  c#/csharp  go  sql  yaml/yml
markdown/md  swift  scala  dart  groovy  latex
git  makefile  clojure  clike  brainfuck  lua/luau
```

> `lua` / `luau` 为自行移植的 `Prism_lua`（Prism4j 生态原生没有 Lua 定义）。

### 行内代码

圆角卡片，**内边距 = 6px + 字形侧边字距**。

- **内边距**（文字 ↔ 卡片边缘）由 `RoundedInlineCodeSpan` 的 `hPadding` 控制，当前 `6f`
- **外边距**（卡片 ↔ 前后文字）恒为 **0**，卡片铺满分配的框

要调整观感只改 `hPadding` 一个常量，外边距不会受影响。

### 引用块

`RoundedBlockQuoteSpan`，圆角竖线，颜色 `0xFF607D8B`。

### 图片与 SVG 徽章

```lua
mk.setMarkdown(tv, "![alt](https://example.com/a.png)\n\n![](https://img.shields.io/badge/build-passing-brightgreen.svg)")
```

- 普通图片（png/jpg/gif）与 shields.io 等 **SVG 徽章都会渲染**（SVG 由 androidsvg 解码，不走 Glide）
- **宿主 APK 必须声明 `android.permission.INTERNET`**，否则加载失败

### 链接

`themePlugin` 在 `afterSetText` 注入 `LinkMovementMethod`，链接可点击。

> **已知限制**：默认 `LinkResolverDef` 用 `ACTION_VIEW` 起 `Activity`，在**非 Activity 上下文**会静默失败（异常被内部吞掉）。要让链接真正打开浏览器，需自行注册 `LinkResolver`。

### 主题配色

| 项 | 颜色 |
|---|---|
| 代码块背景 | `0xFF2B2B2B` |
| 代码块文字 | `0xFFE6E6E6` |
| 行内代码文字 | `0xFFD6336C` |
| 行内代码背景 | `0xFF2F3136` |
| 引用块 | `0xFF607D8B` |
| 代码字体 | `Typeface.MONOSPACE` |

改色需改 `MarkwonSetup.themePlugin()` 后重编 dex。

---

## 大文本：PrecomputedText

```lua
local PrecomputedTextSetterCompat = cl.loadClass("io.noties.markwon.PrecomputedTextSetterCompat")
local setter = PrecomputedTextSetterCompat.create(executor)   -- 也有无参 create()

local mk = MarkwonSetup.builder(activity)
  .textSetter(setter)
  .build()
```

`PrecomputedFutureTextSetterCompat` 仅在 TextView 是 **AppCompatTextView** 时可用，否则抛 `IllegalStateException`，主要面向 RecyclerView 场景。

---

## 注意事项

1. **必须 `INTERNET` 权限** —— 图片与 SVG 徽章是网络加载。
2. **TextView 固定高度** —— 纵向滚动靠 `LinkMovementMethod`，不要 `wrap_content`，也无需在外层套 `ScrollView`。
3. **换 dex 要清缓存** —— 更新 dex 后必须重新 `loadDex`，并清掉宿主/应用对旧 dex 的缓存（含 `optimizedDirectory` 里的旧 odex），否则一直显示旧版。这是"改了没效果"最常见的原因。
4. **链接点击** —— 见上文已知限制，非 Activity 上下文需自定义 `LinkResolver`。
5. **无公式支持** —— v7.4.x 起已移除 LaTeX 公式模块，`$...$` / `$$...$$` 按原始文本渲染，无需任何 TeX 字体 assets。
6. **AndroidX 资源未合并** —— 只打类不打资源，`AppCompatTextView` 的 R 是生成的桩值。普通 `PrecomputedTextSetterCompat`（任意 TextView）不受影响。
