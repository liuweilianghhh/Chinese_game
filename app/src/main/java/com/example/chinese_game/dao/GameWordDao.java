package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.CharacterMatching;
import com.example.chinese_game.javabean.SentenceWord;

import java.util.List;

/**
 * 前两个游戏（Character Matching / Pronunciation Quiz）专用词表 DAO。
 */
public interface GameWordDao {
    /**
     * 按难度随机获取游戏词条。
     * @param count 题目数
     * @param difficulty 难度（EASY/MEDIUM/HARD），为 null 或空时不限制
     */
    List<CharacterMatching> getRandomGameWordsForGame(int count, String difficulty);

    /**
     * 按词性随机获取干扰项，排除正确答案 id。
     * @param posTag 词性
     * @param excludeWordId 正确答案 id
     * @param count 干扰项数量
     */
    List<SentenceWord> getRandomWordsByPosTag(String posTag, int excludeWordId, int count);

    /**
     * 从 JSON 字符串导入词条。
     */
    int importFromJson(String jsonContent);

    /**
     * 从 assets/json/game_words.json 导入词条。
     */
    int importFromAssetsJson(android.content.Context context);

    /**
     * 指定难度是否有数据。
     */
    boolean hasData(String difficulty);

    /**
     * 获取词条数量。
     */
    int getDataCount(String difficulty);
}
