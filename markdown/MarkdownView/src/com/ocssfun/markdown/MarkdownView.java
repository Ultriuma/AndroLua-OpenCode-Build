package com.ocssfun.markdown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 渲染控件（继承 WebView），样式与 me.ocssfun.com 笔记页逐像素对齐。
 * 单 dex 自包含：vendor JS 已 ES5 转译、KaTeX 字体已 data URI 内联，完全离线。
 *
 * 主题 API 对齐 WebView FORCE_DARK_* 常量（语义绑定到内容级 data-theme 引擎，
 * 绝不调用 WebView.setForceDark 算法反色，避免纯黑页面被二次反色）。
 */
public class MarkdownView extends WebView {

    /* ============ 版本探针 ============ */
    /** 当前 dex 版本，真机可用 print(MarkdownView.VERSION) 确认加载的是哪一版 */
    public static final String VERSION = "1.5.4";

    /* ============ 统计常量 ============ */
    public static final int COUNT_TYPE_CHARS = 1;
    public static final int COUNT_TYPE_WORDS = 2;
    public static final int COUNT_TYPE_MARKS = 3;
    public static final int COUNT_TYPE_LINES = 4;
    public static final int COUNT_TYPE_PARAGRAPHS = 5;
    public static final int COUNT_TYPE_CODE_BLOCKS = 6;
    public static final int COUNT_TYPE_IMAGES = 7;
    public static final int COUNT_TYPE_LINKS = 8;
    public static final int COUNT_TYPE_TABLES = 9;
    public static final int COUNT_TYPE_HEADINGS = 10;

    /* ============ 主题常量（对齐 WebView.FORCE_DARK_*，值相同） ============ */
    public static final int FORCE_DARK_OFF = 0;
    public static final int FORCE_DARK_ON = 1;
    public static final int FORCE_DARK_AUTO = 2;

    /* ============ 样式常量（setMarkdownStyle） ============ */
    /** 默认样式：ULTRIUMA ocssfun 风格（亮 #f4f7f9 底 / 暗纯黑体系） */
    public static final int STYLE_ULTRIUMA = 0;
    /** GitHub Primer 风格（标题灰下边框 / #0969da 链接 / 斑马纹表格） */
    public static final int STYLE_GITHUB = 1;

    /* ============ 导出格式常量（export） ============ */
    public static final int EXPORT_FORMAT_MARKDOWN = 1;
    public static final int EXPORT_FORMAT_TEXT = 2;
    public static final int EXPORT_FORMAT_HTML = 3;
    public static final int EXPORT_FORMAT_IMAGE = 4;
    public static final int EXPORT_FORMAT_BITMAP = 5;

    /* ============ 监听器 ============ */
    public interface OnLinkClickListener {
        /** @return true 表示已消费（不交给系统处理） */
        boolean onLinkClick(String url);
    }

    public interface OnImageClickListener {
        void onImageClick(String src, String alt);
    }

    public interface OnRenderCompleteListener {
        void onRenderComplete(MarkdownView view);
    }

    /**
     * 导出回调。两个 `onExport` 为同名重载：回调方向（Java → Lua）由 LuaJ 按方法名绑定，
     * 单个 Lua 函数即可同时接收文本/路径分支与 Bitmap 分支，按第二参类型分流。
     */
    public interface ExportCallback {
        /**
         * 成功：code = EXPORT_FORMAT_*，content = 文本(MARKDOWN/TEXT/HTML) 或缓存文件绝对路径(IMAGE/PDF)
         * 失败：code = -1，content = 错误描述
         */
        void onExport(int code, String content);
        /** BITMAP 成功：直接返回 Bitmap 对象 */
        void onExport(int format, Bitmap bitmap);
    }

    /* ============ 状态 ============ */
    private int mDarkMode = FORCE_DARK_OFF;
    private String mSource;
    private String mBaseDir;
    private boolean mPageReady;                       // 外壳 onPageFinished 后为 true
    private final List<String> mPendingJs = new ArrayList<String>(); // 页面未就绪时挂起的 JS 队列（按序补发）
    private boolean mCountFromRenderedText = true;
    private MarkdownStatistics mStats;
    private String mCachedPlainText;

    private int mExportFormat = EXPORT_FORMAT_MARKDOWN;

    private OnLinkClickListener mLinkListener;
    private OnImageClickListener mImageListener;
    private OnRenderCompleteListener mRenderListener;

    /* ============ 构造 ============ */
    public MarkdownView(Context context) {
        super(context);
        init();
    }

    public MarkdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarkdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        WebSettings s = getSettings();
        s.setJavaScriptEnabled(true);
        s.setDefaultTextEncodingName("UTF-8");
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setDisplayZoomControls(false);
        setVerticalScrollBarEnabled(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setBackgroundColor(0xFFF4F7F9);
        addJavascriptInterface(new NativeBridge(), "__mdNative");
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mPageReady = true;
                applyCurrentTheme(); // 外壳默认 light，这里同步用户已设置的主题
                // 按调用顺序补发挂起的 JS（渲染 / option / theme）
                if (!mPendingJs.isEmpty()) {
                    List<String> pending = new ArrayList<String>(mPendingJs);
                    mPendingJs.clear();
                    for (String js : pending) {
                        evaluateJavascript(js, null);
                    }
                }
            }
        });
        loadShell();
    }

    /* ============ 外壳加载 ============ */
    private void loadShell() {
        String html = MarkdownAssets.SHELL_HTML;
        String baseUrl = mBaseDir != null ? "file://" + mBaseDir + "/" : "about:blank";
        mPageReady = false; // 重载期间 JS 未就绪
        loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

    /**
     * 统一 JS 执行入口：页面未就绪时入队，onPageFinished 后按序补发。
     * 修复白屏：loadDataWithBaseURL 返回时页面尚未加载完，
     * 此刻 evaluateJavascript 会被 WebView 静默丢弃。
     */
    private void evalJs(String js) {
        if (mPageReady) {
            evaluateJavascript(js, null);
        } else {
            mPendingJs.add(js);
        }
    }

    /* ============ 需求指定：加载方法 ============ */
    public void loadFromFile(File file) {
        if (file == null) {
            showToast("MarkdownView: file is null");
            return;
        }
        if (!file.isFile()) {
            showToast("MarkdownView: not a file: " + file.getPath());
            return;
        }
        String content = readFileUtf8(file);
        if (content == null) {
            // 读取失败（IO 异常）：保持现有渲染状态不变，不清空 mSource
            showToast("MarkdownView: read failed: " + file.getPath());
            return;
        }
        mSource = content;
        mBaseDir = file.getParentFile() != null ? file.getParentFile().getAbsolutePath() : null;
        renderInternal();
    }

    public void loadFromPath(String path) {
        if (path == null) {
            showToast("MarkdownView: path is null");
            return;
        }
        if (path.isEmpty()) {
            showToast("MarkdownView: path is empty");
            return;
        }
        loadFromFile(new File(path));
    }

    public void loadFromText(String markdown) {
        mSource = markdown;
        mBaseDir = null;
        renderInternal();
    }

    /*
     * 注意：setMarkdown 存在 String / File 两个同名重载。
     * AndroLua 反射按方法名分派，若调用时传参类型与被选中重载不符，
     * 可能静默失败（白屏无异常）。出现此类现象时请改用唯一命名的
     * setMarkdownText / setMarkdownFile。
     */

    /* D11：属性式别名，与 loadFromText 完全等价 */
    public void setMarkdown(String markdown) {
        loadFromText(markdown);
    }

    /* 属性式别名，与 loadFromFile 完全等价 */
    public void setMarkdown(File file) {
        loadFromFile(file);
    }

    /* 属性式别名，与 loadFromText 完全等价（唯一 String 入口，无同名重载） */
    public void setMarkdownText(String markdown) {
        loadFromText(markdown);
    }

    /* D11：属性式别名，与 loadFromPath 完全等价 */
    public void setMarkdownPath(String path) {
        loadFromPath(path);
    }

    public String getMarkdown() {
        return mSource;
    }

    /* ============ 主题（见 PLAN 3.3） ============ */
    public void setDarkMode(int mode) {
        if (mode != FORCE_DARK_OFF && mode != FORCE_DARK_ON && mode != FORCE_DARK_AUTO) {
            throw new IllegalArgumentException("mode must be FORCE_DARK_OFF/ON/AUTO");
        }
        mDarkMode = mode;
        applyCurrentTheme();
    }

    public int getDarkMode() {
        return mDarkMode;
    }

    public void setDarkModeEnabled(boolean on) {
        setDarkMode(on ? FORCE_DARK_ON : FORCE_DARK_OFF);
    }

    public boolean isDarkModeEnabled() {
        return resolveEffectiveTheme() == FORCE_DARK_ON;
    }

    public void toggleDarkMode() {
        setDarkMode(resolveEffectiveTheme() == FORCE_DARK_ON ? FORCE_DARK_OFF : FORCE_DARK_ON);
    }

    /** 宿主在 onConfigurationChanged / onResume 中调用，强制按系统态重算。 */
    public void syncSystemDarkMode() {
        if (mDarkMode == FORCE_DARK_AUTO) {
            applyCurrentTheme();
        }
    }

    private int resolveEffectiveTheme() {
        if (mDarkMode == FORCE_DARK_AUTO) {
            return isSystemNightMode() ? FORCE_DARK_ON : FORCE_DARK_OFF;
        }
        return mDarkMode;
    }

    private boolean isSystemNightMode() {
        int mask = getContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return mask == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyCurrentTheme() {
        int eff = resolveEffectiveTheme();
        boolean dark = eff == FORCE_DARK_ON;
        setBackgroundColor(dark ? 0xFF000000 : 0xFFF4F7F9); // 防白屏闪烁
        evalJs("__mdApplyTheme('" + (dark ? "dark" : "light") + "');");
    }

    /**
     * 平台标准做法：Override View.onConfigurationChanged（API 1+，无 AndroidX 依赖）。
     * 基类实现为空，调用 super 保有父类行为；不与 Activity / 其他 View 的回调冲突。
     * 注意：仅当宿主 Activity 声明 configChanges 含 uiMode 时系统才派发；
     * 否则由宿主主动调 syncSystemDarkMode() 驱动（PLAN 3.3）。
     */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDarkMode == FORCE_DARK_AUTO) {
            syncSystemDarkMode();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyCurrentTheme();
    }

    /* ============ 渲染 ============ */
    private void renderInternal() {
        String md = mSource == null ? "" : mSource;
        md = MarkdownImageInliner.inlineImages(md, mBaseDir);

        // 统计缓存失效
        mStats = null;
        mCachedPlainText = null;

        evalJs("__mdRender(" + jsString(md) + ");");
    }

    public void reloadMarkdown() {
        renderInternal();
    }

    public void clear() {
        mSource = null;
        mBaseDir = null;
        mStats = null;
        mCachedPlainText = null;
        evalJs("__mdRender('');");
    }

    public void getRenderedHtml(ValueCallback<String> callback) {
        if (callback == null) return;
        if (!mPageReady) {
            // 页面未就绪：没有任何已渲染 HTML，直接返回 null
            callback.onReceiveValue(null);
            return;
        }
        evaluateJavascript("window.__mdGetHtml && __mdGetHtml()", html -> {
            if (callback != null && html != null && html.length() >= 2) {
                callback.onReceiveValue(jsonUnescape(html));
            } else if (callback != null) {
                callback.onReceiveValue(null);
            }
        });
    }

    /* ============ 导出 ============ */
    /**
     * 设置导出格式（默认 EXPORT_FORMAT_MARKDOWN）。配合 {@link #doExportAndCallback(Object)} 使用。
     */
    public void setExportFormat(int format) {
        mExportFormat = format;
    }

    /** 读取当前导出格式（默认 EXPORT_FORMAT_MARKDOWN）。 */
    public int getExportFormat() {
        return mExportFormat;
    }

    /**
     * 统一导出入口（AndroLua 安全形态）。
     * <p>
     * callback 以 {@code Object} 接收：LuaJava 对 Object 参数不做接口类型匹配，可正常接收 Lua 创建的
     * ExportCallback 代理；内部 instanceof 转型后调用。这样无需单独的 setExportCallback，也绕开了
     * 「接口参数内联导致 export 反射失败（not a field or method）」的坑。
     * 格式取自 {@link #setExportFormat(int)}（默认 MARKDOWN）。全部主线程，不新开线程。
     */
    public void doExportAndCallback(Object callback) {
        if (!(callback instanceof ExportCallback)) {
            showToast("MarkdownView: callback is not ExportCallback");
            return;
        }
        exportInternal(mExportFormat, (ExportCallback) callback);
    }

    /** 导出核心逻辑（强类型 ExportCallback）。原 export(int, ExportCallback) 的 switch 抽到此处复用。 */
    private void exportInternal(int format, ExportCallback callback) {
        if (callback == null) return;
        switch (format) {
            case EXPORT_FORMAT_MARKDOWN:
                if (mSource == null) {
                    callback.onExport(-1, "no content");
                } else {
                    callback.onExport(EXPORT_FORMAT_MARKDOWN, mSource);
                }
                return;
            case EXPORT_FORMAT_TEXT:
                if (mSource == null) {
                    callback.onExport(-1, "no content");
                } else {
                    callback.onExport(EXPORT_FORMAT_TEXT, MarkdownCounter.stripMarkdown(mSource));
                }
                return;
            case EXPORT_FORMAT_HTML:
                if (!mPageReady) {
                    callback.onExport(-1, "page not ready");
                    return;
                }
                evaluateJavascript("__mdGetStandaloneHtml()",
                        result -> callback.onExport(EXPORT_FORMAT_HTML, jsonUnescape(result)));
                return;
            case EXPORT_FORMAT_BITMAP:
                exportBitmap(callback);
                return;
            case EXPORT_FORMAT_IMAGE:
                exportImage(callback);
                return;
            default:
                callback.onExport(-1, "unknown EXPORT_FORMAT: " + format);
        }
    }

    /** 整篇大图像素预算（ARGB_8888 约 4B/px，约 128MB）：超出时整篇降采样而非截断。 */
    private static final int MAX_CAPTURE_PIXELS = 32_000_000;

    /** 离屏量高轮询：最长等待时长。data URI 图片通常秒级解码完成，超时按当前量值兜底。 */
    private static final long OFFSCREEN_WAIT_TIMEOUT_MS = 8000;

    /**
     * 截图 WebView：**离屏整页渲染后一次性截取**（含屏幕外内容）。结果经 sink 回传（可能为 null）。
     * <p>
     * 路线演进（真机 Android 17 / API 37 验证）：
     * v1.5.0/v1.5.1 滚动逐屏拼接已判死 —— 总高、滚动都正确，但 scrollTo(y&gt;0) 后
     * draw() 回读到的仍是首屏内容：Chromium 软件绘制只对「当前可见视口的已光栅化帧」
     * 做绘制回读，滚走的部分拿不到，第 2 屏起全空白。
     * v1.5.2 改走离屏旁路：用已验证的 __mdGetStandaloneHtml() 取完整自包含 HTML
     * （样式/主题/图片全内联），装进**不 attach 到窗口**的离屏 WebView 重渲染成整页
     * 高度后一次性 draw —— 离屏视图无硬件合成、无可见视口裁剪，整页都进绘制路径。
     */
    private void captureBitmap(int w, int h, ValueCallback<Bitmap> sink) {
        if (!mPageReady) { // 页面未就绪：退回单屏
            captureViewport(w, h, bmp -> {
                restoreAfterCapture(getScrollY());
                sink.onReceiveValue(bmp);
            });
            return;
        }
        evaluateJavascript("__mdGetStandaloneHtml()", result -> {
            if (result == null || "null".equals(result) || result.length() < 2) {
                sink.onReceiveValue(null);
                return;
            }
            captureOffscreen(jsonUnescape(result), w, h, sink);
        });
    }

    /**
     * 离屏整页渲染 + 截图（全部主线程）。
     * 流程：建离屏 WebView → 加载自包含 HTML → onPageFinished 后固定宽度 layout
     * （避免 device-width 初次 layout 宽度不定导致重排竞态）→ **轮询**等全部图片
     * 解码完成（complete）再 JS 量整页高 —— v1.5.2 固定等 200ms 的教训：
     * 图片未加载时占位高度为 0，scrollHeight 量出来偏小，layout 成的"整页"
     * 比真实内容矮，导出"更长但仍非全图"（真机已验证）→ layout 成整页 →
     * 软件渲染三连 + 延时（v1.3.8 原语）→ draw 一次性截整页 → 回收。
     */
    private void captureOffscreen(String html, int w, int h, ValueCallback<Bitmap> sink) {
        final WebView off = new WebView(getContext());
        WebSettings s = off.getSettings();
        s.setJavaScriptEnabled(true);
        off.setBackgroundColor(resolveEffectiveTheme() == FORCE_DARK_ON ? 0xFF000000 : 0xFFF4F7F9);
        off.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // 先按可视高固定宽度 layout，宽度定了 JS 量的高度才稳定
                view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
                view.layout(0, 0, w, h);
                pollOffscreenHeight(view, System.currentTimeMillis(), w, h, sink);
            }
        });
        off.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    /**
     * 轮询等离屏页内容稳定（图片全部 complete）后量高并截图。
     * 延时走已 attach 的主视图 postDelayed（未 attach 的离屏 WebView 自己的
     * postDelayed 进挂起队列，永不执行）。JS 返回 "图片是否全部complete:整页高(px)"；
     * 图片齐且连续两次量高一致才认为高度可信；超时（OFFSCREEN_WAIT_TIMEOUT_MS）按当前量值兜底。
     * 参数 h 兼作「上一轮量值」在递归间传递（初值为可视高），lambda 捕获要求不可重赋值。
     */
    private void pollOffscreenHeight(final WebView off, final long startAt, final int w, final int h,
                                     final ValueCallback<Bitmap> sink) {
        off.evaluateJavascript(
                "(function(){var i,all=true;for(i=0;i<document.images.length;i++){"
                        + "if(!document.images[i].complete){all=false;break;}}"
                        + "return all+':'+Math.round((document.documentElement.scrollHeight||document.body.scrollHeight)"
                        + "*(window.devicePixelRatio||1));})()",
                r -> {
                    boolean ready = r != null && r.contains("true");
                    int now = parseColonInt(r, h);
                    if (ready && now == h) { // 图片齐且高度与上轮一致：可信
                        layoutAndCapture(off, now, w, h, sink);
                        return;
                    }
                    if (System.currentTimeMillis() - startAt > OFFSCREEN_WAIT_TIMEOUT_MS) {
                        layoutAndCapture(off, Math.max(now, h), w, h, sink); // 超时兜底
                        return;
                    }
                    postDelayed(() -> pollOffscreenHeight(off, startAt, w, now, sink), 100);
                });
    }

    /** 轮询返回 "ready:height" → height（解析失败兜底 def）。 */
    private static int parseColonInt(String json, int def) {
        if (json == null) return def;
        String s = json.replace("\"", "").trim();
        int i = s.indexOf(':');
        if (i < 0) return def;
        try {
            return Integer.parseInt(s.substring(i + 1).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 离屏 WebView 按整页高 layout 后软件渲染截图，然后回收。
     * v1.5.3 真机反馈"尾部缺失、跨内容类型"→ 超预算砍尾正是各类型内容齐缺的模式：
     * v1.5.4 改为**整篇降采样**——超预算时把整页 layout 在预算高度内，用 Canvas.scale
     * 等比缩画，内容永远完整（代价：整图等比变糊），绝不裁尾。
     * 另：超预算路径跳过 drawingCache 三连——它会先分配全尺寸缓存位图（比正图还大），
     * 自己先 OOM；离屏 WebView 本就走软件绘制路径，直接 draw 已可出图。
     */
    @SuppressWarnings("deprecation")
    private void layoutAndCapture(WebView off, int measured, int w, int h, ValueCallback<Bitmap> sink) {
        int total = Math.max(measured, h);
        int budgetH = MAX_CAPTURE_PIXELS / Math.max(1, w);
        final int captureH = Math.min(total, budgetH); // lambda 引用须 effectively final
        final float scale = total <= budgetH ? 1f : (float) budgetH / total; // 超预算：等比缩
        off.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(captureH, MeasureSpec.EXACTLY));
        off.layout(0, 0, w, captureH);
        // 同 captureOffscreen：延时统一走主视图 postDelayed（离屏视图的 postDelayed 永不执行）
        postDelayed(() -> {
            if (scale == 1f) { // 预算内：v1.3.8 原语三连强制软件渲染，缺一不可
                off.buildDrawingCache(true);
                off.setDrawingCacheEnabled(true);
                off.buildDrawingCache();
            }
            postDelayed(() -> {
                Bitmap bmp = null;
                try {
                    bmp = Bitmap.createBitmap(w, captureH, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    if (scale != 1f) c.scale(scale, scale); // 等比缩画整页，不裁尾
                    off.draw(c);
                } catch (Throwable t) {
                    bmp = null;
                }
                sink.onReceiveValue(bmp);
                postDelayed(off::destroy, 100); // 截完回收离屏实例，防泄露
            }, 10); // 不延时会截到部分白屏（v1.3.8 教训）
        }, 50); // 等 layout 后重排/光栅化
    }

    /** evaluateJavascript 返回的 JSON 数字 → int（带引号/小数/非法输入均兜底 def）。 */
    private static int parseJsInt(String json, int def) {
        if (json == null) return def;
        String s = json.replace("\"", "").trim();
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 截当前可视区域（v1.3.8 验证可出图的原语）：buildDrawingCache 强制软件渲染 + 延时 10ms 后 draw。
     * 关键点缺一不可 —— 只切 LAYER_TYPE_SOFTWARE 或只调 enableSlowWholeDocumentDraw 都不触发
     * 软件渲染（全白）；不延时会截到部分白屏。
     */
    @SuppressWarnings("deprecation")
    private void captureViewport(int w, int h, ValueCallback<Bitmap> sink) {
        buildDrawingCache(true);
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        postDelayed(() -> {
            Bitmap bmp = null;
            try {
                bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                draw(new Canvas(bmp));
            } catch (Exception e) {
                bmp = null;
            }
            sink.onReceiveValue(bmp);
        }, 10);
    }

    /** 截图收尾（仅单屏 fallback 路径用）：拆掉 drawingCache、恢复滚动条。 */
    @SuppressWarnings("deprecation")
    private void restoreAfterCapture(int startY) {
        destroyDrawingCache();
        setDrawingCacheEnabled(false);
        setVerticalScrollBarEnabled(true);
        scrollTo(0, startY);
    }

    /** BITMAP：直接回调 Bitmap 对象。 */
    private void exportBitmap(ExportCallback callback) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            callback.onExport(-1, "view not laid out");
            return;
        }
        captureBitmap(w, h, bmp -> {
            if (bmp == null) {
                callback.onExport(-1, "screenshot failed");
            } else {
                callback.onExport(EXPORT_FORMAT_BITMAP, bmp);
            }
        });
    }

    /** IMAGE：截图后压缩 PNG 写入缓存目录，回调返回文件绝对路径。 */
    private void exportImage(ExportCallback callback) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            callback.onExport(-1, "view not laid out");
            return;
        }
        captureBitmap(w, h, bmp -> {
            if (bmp == null) {
                callback.onExport(-1, "screenshot failed");
                return;
            }
            writeBitmapPng(bmp, callback);
        });
    }

    /**
     * 导出落盘文件：优先 ExternalCacheDir（应用专属外置缓存，可被 FileProvider 分享给其它 app）。
     * 内部 cacheDir 下的文件其它 app 读不到，shareFile 会拿到无效分享，故默认走外置；不可用再回退。
     */
    private File makeExportFile(String ext) {
        File dir = getContext().getExternalCacheDir();
        if (dir == null) {
            dir = getContext().getCacheDir();
        } else if (!dir.exists() && !dir.mkdirs()) {
            dir = getContext().getCacheDir();
        }
        return new File(dir, "markdownview_export_" + System.currentTimeMillis() + ext);
    }

    /** 把 Bitmap 压缩为 PNG 落盘；成功回调路径，失败回调 -1。 */
    private void writeBitmapPng(Bitmap bmp, ExportCallback callback) {
        FileOutputStream fos = null;
        try {
            File out = makeExportFile(".png");
            fos = new FileOutputStream(out);
            if (bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                fos.flush();
                callback.onExport(EXPORT_FORMAT_IMAGE, out.getAbsolutePath());
            } else {
                callback.onExport(-1, "png compress failed");
            }
        } catch (Exception e) {
            callback.onExport(-1, "write png failed: " + e.getMessage());
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception ignored) {}
            }
            bmp.recycle();
        }
    }

    /* ============ 统计 ============ */
    public void setCountSource(boolean renderedText) {
        mCountFromRenderedText = renderedText;
        mStats = null;
        mCachedPlainText = null;
    }

    public int countMarkdown(int type) {
        MarkdownStatistics st = computeStatsIfNeeded();
        switch (type) {
            case COUNT_TYPE_CHARS: return st.chars;
            case COUNT_TYPE_WORDS: return st.words;
            case COUNT_TYPE_MARKS: return st.marks;
            case COUNT_TYPE_LINES: return st.lines;
            case COUNT_TYPE_PARAGRAPHS: return st.paragraphs;
            case COUNT_TYPE_CODE_BLOCKS: return st.codeBlocks;
            case COUNT_TYPE_IMAGES: return st.images;
            case COUNT_TYPE_LINKS: return st.links;
            case COUNT_TYPE_TABLES: return st.tables;
            case COUNT_TYPE_HEADINGS: return st.headings;
            default: throw new IllegalArgumentException("unknown COUNT_TYPE: " + type);
        }
    }

    public MarkdownStatistics getStatistics() {
        return computeStatsIfNeeded();
    }

    private MarkdownStatistics computeStatsIfNeeded() {
        if (mStats == null) {
            String src = mSource == null ? "" : mSource;
            String text = mCountFromRenderedText
                    ? MarkdownCounter.stripMarkdown(src) : src;
            mCachedPlainText = text; // 供调试；统计本身即时计算（纯内存，快）
            mStats = MarkdownCounter.compute(src, text);
        }
        return mStats;
    }

    /* ============ 4.2 补充开关 ============ */
    /* ============ 样式 ============ */
    /**
     * 切换内置样式。可选 {@link #STYLE_ULTRIUMA}（默认）或 {@link #STYLE_GITHUB}。
     */
    public void setMarkdownStyle(int style) {
        if (style != STYLE_ULTRIUMA && style != STYLE_GITHUB) {
            throw new IllegalArgumentException("style must be STYLE_ULTRIUMA or STYLE_GITHUB");
        }
        evalJs("__mdSetGithubStyle(" + (style == STYLE_GITHUB) + ");");
    }

    /**
     * 注入自定义 CSS 覆盖层（追加在内置样式之后，优先级最高，可覆盖任意规则）。
     * 传 null 或空串清除自定义层。
     *
     * 选择器规范：必须以 <code>.md-content</code> 或 <code>.md-content 后代</code> 为前缀
     * （正文容器类名），如 <code>.md-content blockquote { ... }</code>。
     * 暗色适配：<code>[data-theme="dark"] .md-content ...</code>。
     */
    public void setMarkdownStyle(String styleCss) {
        evalJs("__mdSetCustomCss(" + jsString(styleCss == null ? "" : styleCss) + ");");
    }

    public void setCodeWrapEnabled(boolean wrap) {
        evalJs("__mdSetOption('codeWrap'," + wrap + ");");
    }

    public void setMathEnabled(boolean enabled) {
        evalJs("__mdSetOption('mathEnabled'," + enabled + ");");
    }

    public void setHighlightEnabled(boolean enabled) {
        evalJs("__mdSetOption('highlightEnabled'," + enabled + ");");
    }

    public void setContentPadding(int dp) {
        float px = dp * getResources().getDisplayMetrics().density;
        evalJs("__mdSetOption('contentPadding'," + (int) px + ");");
    }

    public void setMaxInlineImageBytes(long bytes) {
        MarkdownImageInliner.maxInlineImageBytes = bytes;
    }

    public void setAllowInlineHtml(boolean allow) {
        evalJs("__mdSetOption('allowInlineHtml'," + allow + ");");
    }

    /** GitHub (Primer) 风格开关；默认关闭（使用默认 ocssfun 风格）。 */
    public void setGithubStyle(boolean on) {
        evalJs("__mdSetGithubStyle(" + on + ");");
    }

    public void setOnLinkClickListener(OnLinkClickListener l) {
        mLinkListener = l;
    }

    public void setOnImageClickListener(OnImageClickListener l) {
        mImageListener = l;
    }

    public void setOnRenderCompleteListener(OnRenderCompleteListener l) {
        mRenderListener = l;
    }

    /* ============ JS 桥 ============ */
    private class NativeBridge {
        @JavascriptInterface
        public void onRenderComplete() {
            post(() -> {
                if (mRenderListener != null) {
                    mRenderListener.onRenderComplete(MarkdownView.this);
                }
            });
        }

        @JavascriptInterface
        public boolean onLinkClick(final String url) {
            post(() -> {
                boolean consumed = false;
                if (mLinkListener != null) {
                    consumed = mLinkListener.onLinkClick(url);
                }
                if (!consumed && url != null) {
                    try {
                        getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) {} // 无可处理 app 时静默
                }
            });
            return true;
        }

        @JavascriptInterface
        public void onImageClick(final String src, final String alt) {
            post(() -> {
                if (mImageListener != null) {
                    mImageListener.onImageClick(src, alt);
                } else {
                    // 默认行为：Toast 提示图片 URL / base64（超长截断）
                    String msg = src == null ? "" : src;
                    if (msg.length() > 120) {
                        msg = msg.substring(0, 120) + "…";
                    }
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /* ============ 工具 ============ */
    /** 极简失败诊断：失败分支直接 Toast 提示，便于真机定位（无诊断框架）。 */
    private void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private static String readFileUtf8(File f) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8), 16384);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            return sb.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (r != null) {
                try { r.close(); } catch (IOException ignored) {}
            }
        }
    }

    /** Java String → JS 字符串字面量参数（单引号包裹 + 转义）。 */
    private static String jsString(String s) {
        if (s == null) return "''";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('\'');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '<': sb.append("\\x3c"); break;
                case '>': sb.append("\\x3e"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    /** evaluateJavascript 返回 JSON 字符串 → 原始 String（首尾引号 + 基本转义）。 */
    private static String jsonUnescape(String s) {
        String body = s.substring(1, s.length() - 1);
        StringBuilder sb = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char n = body.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (i + 4 < body.length()) {
                            sb.append((char) Integer.parseInt(body.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
