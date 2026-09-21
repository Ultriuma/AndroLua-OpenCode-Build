package com.ocssfun.markdown;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地图片 → Base64 data URI。
 * img2Base64(String path) 与 imgEx2BaseType(String s) 为需求指定方法名。
 */
final class MarkdownImageInliner {

    /** 单图内联上限，默认 5 MB（超限跳过并打 log，见 PLAN 3.5）。 */
    static volatile long maxInlineImageBytes = 5L * 1024 * 1024;

    private MarkdownImageInliner() {}

    private static final Map<String, String> MIME = new HashMap<String, String>();

    static {
        MIME.put("png", "image/png");
        MIME.put("jpg", "image/jpeg");
        MIME.put("jpeg", "image/jpeg");
        MIME.put("gif", "image/gif");
        MIME.put("webp", "image/webp");
        MIME.put("bmp", "image/bmp");
        MIME.put("svg", "image/svg+xml");
        MIME.put("ico", "image/x-icon");
        MIME.put("avif", "image/avif");
    }

    /** 需求指定：扩展名 → MIME；未知扩展名 → application/octet-stream（D12）。 */
    private static String imgEx2BaseType(String s) {
        if (s == null) return "application/octet-stream";
        int dot = s.lastIndexOf('.');
        if (dot < 0 || dot == s.length() - 1) return "application/octet-stream";
        String ext = s.substring(dot + 1).toLowerCase();
        String mime = MIME.get(ext);
        return mime != null ? mime : "application/octet-stream";
    }

    /**
     * 需求指定：单张本地图片 → "data:<mime>;base64,<...>"。
     * 失败（不存在 / 超限 / IO 错误）返回 null。
     */
    private static String img2Base64(String path) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        if (!f.isFile()) return null;
        if (f.length() > maxInlineImageBytes) {
            android.util.Log.w("MarkdownView", "image too large, skip inline: " + path
                    + " (" + f.length() + " bytes > " + maxInlineImageBytes + ")");
            return null;
        }
        byte[] bytes = readFile(f);
        if (bytes == null) return null;
        String mime = imgEx2BaseType(path);
        return "data:" + mime + ";base64,"
                + Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * 扫描 markdown，将本地图片引用 ![alt](path) 就地替换为 data URI。
     * 跳过 http(s)/data 已是 URI 的引用。path 为相对路径时基于 baseDir 解析。
     */
    static String inlineImages(String markdown, String baseDir) {
        if (markdown == null || markdown.indexOf("](") < 0) return markdown;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "!\\[([^\\]]*)\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
        java.util.regex.Matcher m = p.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String alt = m.group(1);
            String src = m.group(2);
            String replacement = m.group(0);
            if (src != null && !src.isEmpty()
                    && !src.startsWith("http://") && !src.startsWith("https://")
                    && !src.startsWith("data:")) {
                String abs = resolvePath(src, baseDir);
                String dataUri = img2Base64(abs);
                if (dataUri != null) {
                    replacement = "![" + alt + "](" + dataUri + ")";
                }
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String resolvePath(String src, String baseDir) {
        if (baseDir == null || src.startsWith("/")) return src;
        // 处理 ./ 与 ../
        java.io.File f = new java.io.File(baseDir, src);
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
    }

    private static byte[] readFile(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) f.length());
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
        }
    }
}
