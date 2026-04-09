package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.PronunciationQuiz;

import java.util.List;

public interface PronunciationQuizDao {
    /**
     * 保存发音测验游戏数据
     * @param data 发音测验数据对象
     * @return 数据ID，保存失败返回-1
     */
    long save(PronunciationQuiz data);

    /**
     * 根据ID查找发音测验数据
     * @param dataId 数据ID
     * @return 发音测验数据对象，不存在返回null
     */
    PronunciationQuiz findById(int dataId);

    /**
     * 根据难度获取发音测验数据
     * @param difficulty 难度等级
     * @return 发音测验数据列表
     */
    List<PronunciationQuiz> findByDifficulty(String difficulty);

    /**
     * 获取所有活跃的发音测验数据
     * @return 发音测验数据列表
     */
    List<PronunciationQuiz> findActiveData();

    /**
     * 获取随机发音测验数据（用于随机出题）
     * @param difficulty 难度等级
     * @param count 获取数量
     * @return 随机发音测验数据列表
     */
    List<PronunciationQuiz> getRandomData(String difficulty, int count);

    /**
     * 更新发音测验数据
     * @param data 发音测验数据对象
     * @return 更新是否成功
     */
    boolean update(PronunciationQuiz data);

    /**
     * 启用/禁用发音测验数据
     * @param dataId 数据ID
     * @param active 是否启用
     * @return 更新是否成功
     */
    boolean setActive(int dataId, boolean active);

    /**
     * 删除发音测验数据
     * @param dataId 数据ID
     * @return 删除是否成功
     */
    boolean delete(int dataId);

    /**
     * 获取发音测验数据的统计信息
     * @return 统计信息
     */
    PronunciationQuizStatistics getStatistics();

    /**
     * 批量插入发音测验数据（用于初始化数据）
     * @param dataList 发音测验数据列表
     * @return 成功插入的数量
     */
    int batchInsert(List<PronunciationQuiz> dataList);

    /**
     * 若不存在 (sentence_id, word_id) 则插入一条，用于游戏结束后自动填充 pronunciation_quiz 表。
     * @param data 含 sentence_id、word_id、difficulty、hint 的题目
     * @return 已存在则返回已有行 id，新插入则返回新行 id；失败返回 -1
     */
    long insertIfNotExists(PronunciationQuiz data);

    /**
     * 检查指定难度的发音测验数据是否存在
     * @param difficulty 难度等级
     * @return 是否存在
     */
    boolean hasData(String difficulty);

    /**
     * 获取发音测验数据的总数
     * @param difficulty 难度等级（可选，为null时统计所有难度）
     * @return 数据总数
     */
    int getDataCount(String difficulty);

    /**
     * 从JSON文件中批量导入发音测验数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的数量，失败返回-1
     */
    int importFromJson(String jsonContent);

    /**
     * 从assets文件夹中的JSON文件导入发音测验数据
     * @param context Android上下文
     * @return 成功导入的数量，失败返回-1
     */
    int importFromAssetsJson(android.content.Context context);

    /**
     * 发音测验数据统计信息类
     */
    class PronunciationQuizStatistics {
        public int totalQuestions;
        public int activeQuestions;
        public int easyQuestions;
        public int mediumQuestions;
        public int hardQuestions;
    }
}