package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.CharacterMatching;
import com.example.chinese_game.javabean.SentenceWord;
import java.util.List;

/**
 * 分词表 DAO：按词性随机取词，用于字形匹配等游戏的干扰项；并支持从 sentence_words 动态生成题目。
 */
public interface SentenceWordDao {
    /**
     * 从 sentence_words 中按词性随机选取若干词，排除指定 wordId（正确答案）。
     * @param posTag 词性，如 PN, VA, NN
     * @param excludeWordId 要排除的词 id（正确答案）
     * @param count 需要数量（如 3 个干扰项）
     * @return 随机词列表，不足时返回实际数量
     */
    List<SentenceWord> getRandomWordsByPosTag(String posTag, int excludeWordId, int count);

    /**
     * 从 sentence_words 动态生成题目：随机取若干条不重复的 (sentence_id, word_id)，JOIN 句子表，返回可作字形匹配题目的列表。
     * 每题对应一个 sentence_word（一个词在一句中的出现），保证不重复。
     * @param count 题目数量（如 10）
     * @param difficulty 难度，传 null 表示不限难度
     * @return 不重复的题目列表，不足时返回实际数量（可能小于 count）
     */
    List<CharacterMatching> getRandomSentenceWordsForGame(int count, String difficulty);

    /**
     * 统计词频（仅统计未排除的 CTB 词性），并按词频分档写入 word_frequency、word_difficulty。
     * 高词频→EASY，中→MEDIUM，低→HARD；仅在排除词性中出现的词视为 HARD。
     */
    void updateWordFrequencyAndDifficulty(android.content.Context context);
}
