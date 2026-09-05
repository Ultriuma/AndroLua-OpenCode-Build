# Markdown 渲染组件 / Markdown Rendering

[![license](https://img.shields.io/github/license/ULTRIUMA/AndroLua-OpenCode-Build.svg)](LICENSE)
[![releases](https://img.shields.io/github/v/tag/ULTRIUMA/AndroLua-OpenCode-Build?color=C71D23&label=releases&logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build/releases)
[![Github repository](https://img.shields.io/badge/Github-repository-0969DA?logo=github)](https://github.com/ULTRIUMA/AndroLua-OpenCode-Build)

---

本模块包含两套独立的 Markdown 渲染方案，均为 AndroLua 可直接 `loadDex` 加载的 `classes.dex`，无需源码编译。
This folder ships two independent Markdown rendering solutions for AndroLua, each as a standalone `classes.dex`.

| 方案 / Approach | 产物 / Artifact | 特点 / Highlights |
| --- | --- | --- |
| [MarkdownView](MarkdownView/) | `MarkdownView_v1.2.4.dex` | WebView 方案，完全离线，带公式（KaTeX），双样式亮暗主题 |
| [MarkwonX](MarkwonX/) | `MarkwonX_v1.0.0.dex` | Markwon + Prism4j 方案，嵌入 TextView，语法高亮，无公式 |

## 选型建议 / Recommendation

| 需求 | 推荐 |
| --- | --- |
| 离线渲染、带 LaTeX 公式、WebView 内显示 | **MarkdownView** |
| 嵌入 TextView、体积更小、可追加 Markwon 插件、语法高亮 | **MarkwonX** |
| 两者都支持深色模式、图片、链接 | — |

## 快速对比 / Quick Comparison

| 项 | MarkdownView | MarkwonX |
| --- | --- | --- |
| 渲染引擎 | WebView + marked/highlight.js/KaTeX | Markwon 4.6.2 + Prism4j |
| LaTeX 公式 | 支持 | 不支持 |
| 语法高亮 | highlight.js | Prism4j（更多语言） |
| 深色模式 | setDarkMode + CSS 双主题 | 内置深色配色 |
| 插件扩展 | 自定义 CSS 注入 | Markwon 插件链 |
| 体积 | ~1.8 MB | ~7.5 MB |
| min-api | 21 | 21 |

> 各方案的完整 API 文档请查看对应子目录的 README。
