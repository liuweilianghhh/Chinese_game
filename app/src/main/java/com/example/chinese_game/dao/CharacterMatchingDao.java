package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.CharacterMatching;

import java.util.List;

public interface CharacterMatchingDao {
    /**
     * 保存字符匹配游戏数据
     * @param data 字符匹配数据对象
     * @return 数据ID，保存失败返回-1
     */
    long save(CharacterMatching data);

    /**
     * 根据ID查找字符匹配数据
     * @param dataId 数据ID
     * @return 字符匹配数据对象，不存在返回null
     */
    CharacterMatching findById(int dataId);

    /**
     * 根据难度获取字符匹配数据
     * @param difficulty 难度等级
     * @return 字符匹配数据列表
     */
    List<CharacterMatching> findByDifficulty(String difficulty);

    /**
     * 获取所有字符匹配数据
     * @return 字符匹配数据列表
     */
    List<CharacterMatching> findAll();

    /**
     * 获取随机字符匹配数据（用于随机出题）
     * @param difficulty 难度等级
     * @param count 获取数量
     * @return 随机字符匹配数据列表
     */
    List<CharacterMatching> getRandomData(String difficulty, int count);

    /**
     * 更新字符匹配数据
     * @param data 字符匹配数据对象
     * @return 更新是否成功
     */
    boolean update(CharacterMatching data);


    /**
     * 删除字符匹配数据
     * @param dataId 数据ID
     * @return 删除是否成功
     */
    boolean delete(int dataId);

    /**
     * 获取字符匹配数据的统计信息
     * @return 统计信息
     */
    CharacterMatchingStatistics getStatistics();

    /**
     * 批量插入字符匹配数据（用于初始化数据）
     * @param dataList 字符匹配数据列表
     * @return 成功插入的数量
     */
    int batchInsert(List<CharacterMatching> dataList);

    /**
     * 若不存在 (sentence_id, word_id) 则插入一条，用于出题时自动填充 character_matching 表。
     * @param data 含 sentence_id、word_id、difficulty、hint 的题目
     * @return 已存在则返回已有行 id，新插入则返回新行 id；失败返回 -1
     */
    long insertIfNotExists(CharacterMatching data);

    /**
     * 检查指定难度的字符匹配数据是否存在
     * @param difficulty 难度等级
     * @return 是否存在
     */
    boolean hasData(String difficulty);

    /**
     * 获取字符匹配数据的总数
     * @param difficulty 难度等级（可选，为null时统计所有难度）
     * @return 数据总数
     */
    int getDataCount(String difficulty);

    /**
     * 从JSON文件中批量导入字符匹配数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的数量，失败返回-1
     */
    int importFromJson(String jsonContent);

    /**
     * 从assets文件夹中的JSON文件导入字符匹配数据
     * @param context Android上下文
     * @return 成功导入的数量，失败返回-1
     */
    int importFromAssetsJson(android.content.Context context);

    /**
     * 字符匹配数据统计信息类
     */
    class CharacterMatchingStatistics {
        public int totalQuestions;
        public int activeQuestions;
        public int easyQuestions;
        public int mediumQuestions;
        public int hardQuestions;
    }
}