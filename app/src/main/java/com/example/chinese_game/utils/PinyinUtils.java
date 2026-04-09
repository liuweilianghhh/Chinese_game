package com.example.chinese_game.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 汉字转拼音工具，用于 sentence_words 等处的词条标注拼音。
 * 使用 pinyin4j，输出带声调、音节间空格的格式（如 "běi jīng"）。
 */
public final class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    }

    private PinyinUtils() {}

    /**
     * 将中文词转为拼音字符串，多音节用空格分隔，带声调（如 "běi jīng"）。
     * 非汉字字符原样保留；多音字取第一个读音。
     *
     * @param word 词语，如 "北京"、"学习"
     * @return 拼音字符串，如 "běi jīng"、"xué xí"；若输入为 null 或空则返回空字符串
     */
    public static String wordToPinyin(String word) {
        if (word == null || word.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            try {
                String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                if (pinyins != null && pinyins.length > 0) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(pinyins[0]);
                } else {
                    sb.append(c);
                }
            } catch (BadHanyuPinyinOutputFormatCombination e) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}
