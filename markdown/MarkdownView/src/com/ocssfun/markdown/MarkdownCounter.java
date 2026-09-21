package com.ocssfun.markdown;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档统计：文本类（chars/words/marks）+ 结构类（lines/codeBlocks/images/links/tables/headings）。
 * 文本类基准由 MarkdownView 通过 setCountSource 切换（渲染后纯文本 或 原始源码）。
 */
final class MarkdownCounter {

    private MarkdownCounter() {}

    private static final Pattern FENCE = Pattern.compile("```|~~~");
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("(?<!\\!)\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern HEADING = Pattern.compile("(?m)^#{1,6}\\s");
    private static final Pattern CODE_FENCE_BLOCK = Pattern.compile("(?ms)^```.*?^```|(?ms)^~~~.*?^~~~");
    private static final Pattern MD_MARKS = Pattern.compile("[#*_~`>\\-+!\\[\\]()|]");

    /** 剥离 markdown 语法 → 纯文本（供文本类统计）。 */
    static String stripMarkdown(String md) {
        String s = md;
        s = CODE_FENCE_BLOCK.matcher(s).replaceAll(" ");
        s = IMAGE.matcher(s).replaceAll(" ");
        s = LINK.matcher(s).replaceAll("$1");
        s = s.replaceAll("(?m)^#{1,6}\\s+", "");
        s = s.replaceAll("(\\*\\*|__)(.*?)\\1", "$2");
        s = s.replaceAll("(\\*|_)(.*?)\\1", "$2");
        s = s.replaceAll("~~(.*?)~~", "$1");
        s = s.replaceAll("`([^`]*)`", "$1");
        s = s.replaceAll("(?m)^>\\s?", "");
        s = s.replaceAll("(?m)^\\s*([-*+]|\\d+\\.)\\s+", "");
        s = s.replaceAll("(?m)^\\s*\\|.*\\|\\s*$", " ");
        s = s.replaceAll("(?m)^\\s*[-*_]{3,}\\s*$", " ");
        s = MD_MARKS.matcher(s).replaceAll("");
        s = s.replaceAll("\n{2,}", "\n");
        return s;
    }

    /** 一次性计算全部统计。 */
    static MarkdownStatistics compute(String source, String textForCounting) {
        MarkdownStatistics st = new MarkdownStatistics();

        // 文本类（基于 textForCounting：纯文本或源码，由调用方决定）
        String t = textForCounting == null ? "" : textForCounting;
        st.chars = t.length();
        st.words = countWords(t);
        st.marks = countMarks(t);

        // 结构类（基于源码）
        String src = source == null ? "" : source;
        st.lines = countLines(src);
        st.paragraphs = countParagraphs(src);
        st.codeBlocks = countFences(src) / 2;
        st.images = countMatches(IMAGE, src);
        st.links = countMatches(LINK, src);
        st.tables = countTables(src);
        st.headings = countMatches(HEADING, src);
        return st;
    }

    /** D4：CJK 按字计 + 拉丁按空格分词计，两者相加。 */
    private static int countWords(String s) {
        if (s.isEmpty()) return 0;
        int cjk = 0;
        int latinRuns = 0;
        boolean inLatin = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isCJK(c)) {
                cjk++;
                inLatin = false;
            } else if (Character.isLetterOrDigit(c)) {
                if (!inLatin) {
                    latinRuns++;
                    inLatin = true;
                }
            } else {
                inLatin = false;
            }
        }
        return cjk + latinRuns;
    }

    private static boolean isCJK(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)      // CJK 统一表意
                || (c >= 0x3400 && c <= 0x4DBF)   // 扩展 A
                || (c >= 0xF900 && c <= 0xFAFF)   // 兼容表意
                || (c >= 0x3040 && c <= 0x30FF);  // 日文假名（顺带覆盖）
    }

    /** 符号：非字母、非数字、非 CJK、非空白。 */
    private static int countMarks(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || isCJK(c)) continue;
            n++;
        }
        return n;
    }

    private static int countLines(String s) {
        if (s.isEmpty()) return 0;
        int n = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') n++;
        }
        return n;
    }

    private static int countParagraphs(String s) {
        String[] blocks = s.split("\\n\\s*\\n");
        int n = 0;
        for (String b : blocks) {
            if (!b.trim().isEmpty()) n++;
        }
        return n;
    }

    private static int countFences(String s) {
        Matcher m = FENCE.matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static int countTables(String s) {
        // 表格 = 含 | 的行后跟分隔行 |---|---|
        Pattern sep = Pattern.compile("(?m)^\\s*\\|?(\\s*:?-{2,}:?\\s*\\|)+\\s*:?-{2,}:?\\s*\\|?\\s*$");
        Matcher m = sep.matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static int countMatches(Pattern p, String s) {
        Matcher m = p.matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }
}
