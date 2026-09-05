# MarkdownView

[![license](https://img.shields.io/github/license/ULTRIUMA/AndroLua-OpenCode-Build.svg)](LICENSE)
[![releases](https://img.shields.io/github/v/tag/ULTRIUMA/AndroLua-OpenCode-Build?color=C71D23&label=releases&logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build/releases)
![](https://img.shields.io/github/last-commit/ULTRIUMA/AndroLua-OpenCode-Build.svg)
[![Github repository](https://img.shields.io/badge/Github-repository-0969DA?logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build)
![QQ: 2957148920](https://img.shields.io/badge/QQ-2957148920-0099FF?logo=tencentqq)

---



> 继承自 `WebView` 的 Markdown 渲染控件，编译为独立 `classes.dex`，供 AndroLua `loadDex` 加载。

- **完全离线**：vendor JS（marked 12 / highlight.js 11 / KaTeX 0.16）已 ES5 转译、KaTeX 20 个字体 data URI 内联，单 dex 无任何外部依赖
- **双内置样式**：ULTRIUMA（默认）/ GitHub（Primer 风格），均带完整亮暗双主题
- **产品名**：`MarkdownView` 
- **包名**：`com.ocssfun.markdown` 
- **产物**：`markdownview.dex`（~1.83 MB，min-api 21）
- 第三方 Java 依赖：**无**

## AndroLua 快速上手

```lua
require "import"
import "com.ocssfun.markdown.*"
import "com.ocssfun.markdown.MarkdownView"

local mdv = MarkdownView(activity)
frame.addView(mdv)

-- 加载
mdv.setMarkdown("# 标题\n正文 **加粗** `code`")
-- mdv.setMarkdown(File("/sdcard/note.md"))     -- File 重载
-- mdv.loadFromPath("/sdcard/note.md")          -- 同上

-- 样式
mdv.setMarkdownStyle(MarkdownView.STYLE_GITHUB) -- 默认 STYLE_ULTRIUMA
mdv.setDarkMode(MarkdownView.FORCE_DARK_AUTO)   -- OFF(0)/ON(1)/AUTO(2)

-- 事件
mdv.setOnRenderCompleteListener(MarkdownView.OnRenderCompleteListener{
  onRenderComplete = function(v) print("render done") end
})
```

## API

### 加载

| 方法 | 说明 |
| --- | --- |
| `loadFromFile(File)` / `setMarkdown(File)` | 读文件渲染；文件父目录作为相对图片的解析基准 |
| `loadFromPath(String)` / `setMarkdownPath(String)` | 等价 `loadFromFile(new File(path))` |
| `loadFromText(String)` / `setMarkdown(String)` | 直接渲染文本 |
| `getMarkdown()` | 取回当前源码 |
| `reloadMarkdown()` / `clear()` | 重渲染 / 清空 |

### 样式 `setMarkdownStyle`

**内置样式（int 重载）**

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `MarkdownView.STYLE_ULTRIUMA` | 0 | 默认。ocssfun 笔记页风格：亮 `#f4f7f9` 底 / 暗纯黑体系、蓝色胶囊行内码、圆角卡片代码块 |
| `MarkdownView.STYLE_GITHUB` | 1 | GitHub Primer 风格：标题灰色下边框、`#0969da` 链接、斑马纹表格、系统字体栈 |

非法值抛 `IllegalArgumentException`。两种样式均自动跟随 `setDarkMode` 切换亮暗 token。

**自定义 CSS（String 重载）**

```java
mdv.setMarkdownStyle(".md-content blockquote { border-left: 4px solid #e5a50a; }");
mdv.setMarkdownStyle(null); // 清除自定义层
```

注入为独立 `<style id="mdCustom">` 覆盖层，位于所有内置样式之后（**同优先级下后者胜出**，加 `!important` 可覆盖任意内置规则）。传 null 或空串清除。

**`styleCss` 书写规范**：

1. **选择器必须以 `.md-content` 开头**（正文容器类名），推荐写 `.md-content 后代选择器`：
   - ✅ `.md-content blockquote { ... }`、`.md-content pre code { ... }`
   - ❌ `blockquote { ... }`（会污染 body 下其他元素，且优先级低于内置 `.md-content blockquote`）
2. **暗色适配**用 `[data-theme="dark"]` 前缀：
   ```css
   .md-content blockquote { border-left-color: #e5a50a; }
   [data-theme="dark"] .md-content blockquote { border-left-color: #f0b429; }
   ```
3. **可用 token**（亮/暗自动切换）：`var(--primary)` 强调色、`var(--text-main)` 主文字、`var(--text-sub)` 次文字、`var(--bg)` 背景、`var(--card)` 卡片、`var(--line)` 边框、`var(--input-bg)` 代码底、`var(--radius-md)` 圆角
4. ** KaTeX/hljs 类名**（`.katex`、`.hljs-*`）同样支持覆盖，写在 `.md-content` 前缀内即可
5. 图片路径如需 `url()` 引用本地文件，注意 WebView 禁止 file 访问——请用 data URI 或 http(s)

### 主题（深色模式）

| 方法 | 说明 |
| --- | --- |
| `setDarkMode(int)` | `FORCE_DARK_OFF(0)` / `FORCE_DARK_ON(1)` / `FORCE_DARK_AUTO(2)`，默认 OFF |
| `setDarkModeEnabled(boolean)` / `getDarkMode()` / `isDarkModeEnabled()` / `toggleDarkMode()` | 便捷封装 |
| `syncSystemDarkMode()` | 按系统夜间态重算（AUTO 模式在宿主 `onResume`/`onConfigurationChanged` 中调用） |

> AUTO 依赖宿主 Activity 声明 `android:configChanges` 含 `uiMode` 才能收到系统派发，未声明时请手动调 `syncSystemDarkMode()`。本控件**不使用** `WebView.setForceDark` 算法反色，暗色由内容引擎（`data-theme` + hljs 双表互斥切换）实现，暗色下不会被二次反色。

### 统计

`countMarkdown(int type)`：

| type | 常量 | 说明 |
| --- | --- | --- |
| 1 | `COUNT_TYPE_CHARS` | 字符数 |
| 2 | `COUNT_TYPE_WORDS` | 词数（CJK 按字 + 拉丁按词） |
| 3 | `COUNT_TYPE_MARKS` | 符号数 |
| 4 | `COUNT_TYPE_LINES` | 行数 |
| 5 | `COUNT_TYPE_PARAGRAPHS` | 段落数 |
| 6 | `COUNT_TYPE_CODE_BLOCKS` | 围栏代码块数 |
| 7 | `COUNT_TYPE_IMAGES` | 图片数 |
| 8 | `COUNT_TYPE_LINKS` | 链接数（不含图片） |
| 9 | `COUNT_TYPE_TABLES` | 表格数 |
| 10 | `COUNT_TYPE_HEADINGS` | 标题数 |

未知 type 抛 `IllegalArgumentException`。`getStatistics()` 一次返回全部（`MarkdownStatistics`，含 toString）。`setCountSource(false)` 把文本类统计切换为按原始源码（默认按渲染后纯文本）。

### 渲染开关

| 方法 | 默认 | 说明 |
| --- | --- | --- |
| `setMathEnabled(boolean)` | true | 关闭跳过 KaTeX，首屏更快 |
| `setHighlightEnabled(boolean)` | true | 关闭跳过 hljs |
| `setAllowInlineHtml(boolean)` | **false** | 默认转义内联 HTML（XSS 安全） |
| `setCodeWrapEnabled(boolean)` | false | false=代码块横向滑动，true=软换行 |
| `setContentPadding(int dp)` | 16 | 正文内边距 |
| `setMaxInlineImageBytes(long)` | 5 MB | 单图内联上限，超限跳过 |
| `getRenderedHtml(ValueCallback<String>)` | — | 异步取渲染后 HTML |

### 事件与默认行为

| 接口 | 默认行为（不设置监听器时） |
| --- | --- |
| `setOnLinkClickListener` | 控件直接发起系统跳转（`ACTION_VIEW`）；监听器返回 true 表示已消费、不跳转 |
| `setOnImageClickListener` | **Toast 显示图片 URL/base64**（>120 字符截断）；设置监听器后完全由宿主接管 |
| `setOnRenderCompleteListener` | 无默认行为 |

点击/hover 均无系统蓝色高亮（`-webkit-tap-highlight-color: transparent`）。

### 图片

本地图片（`![alt](相对/绝对路径)`）自动转 Base64 内联，相对路径基于 `loadFromFile` 的文件父目录解析；支持 png/jpg/jpeg/gif/webp/bmp/svg/ico/avif，未知扩展名兜底 `application/octet-stream`。`img.shields.io` 徽章自动识别为内联小图（3px 圆角、紧凑边距、不独立成行）。


## 已知限制

1. 源站 Parsedown 与本项目 marked（GFM）存在细微语法差异（裸 URL 自动链接等）。
2. 旧 WebView（Chrome < 52）不支持 `contain: layout`，极端宽代码可能溢出。
3. dex 不携带 `res/`，XML 属性定制不可用，请走 `setXxx` 方法。
4. 超大单图（>5MB）默认跳过内联（`setMaxInlineImageBytes` 调整）。