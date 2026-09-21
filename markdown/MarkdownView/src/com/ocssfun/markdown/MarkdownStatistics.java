package com.ocssfun.markdown;

/** 文档统计容器 — 一次 getStatistics() 全量返回。 */
public class MarkdownStatistics {
    public int chars;        // COUNT_TYPE_CHARS
    public int words;        // COUNT_TYPE_WORDS
    public int marks;        // COUNT_TYPE_MARKS
    public int lines;        // COUNT_TYPE_LINES
    public int paragraphs;   // COUNT_TYPE_PARAGRAPHS
    public int codeBlocks;   // COUNT_TYPE_CODE_BLOCKS
    public int images;       // COUNT_TYPE_IMAGES
    public int links;        // COUNT_TYPE_LINKS
    public int tables;       // COUNT_TYPE_TABLES
    public int headings;     // COUNT_TYPE_HEADINGS

    @Override
    public String toString() {
        return "MarkdownStatistics{chars=" + chars + ", words=" + words + ", marks=" + marks
                + ", lines=" + lines + ", paragraphs=" + paragraphs
                + ", codeBlocks=" + codeBlocks + ", images=" + images
                + ", links=" + links + ", tables=" + tables + ", headings=" + headings + "}";
    }
}
