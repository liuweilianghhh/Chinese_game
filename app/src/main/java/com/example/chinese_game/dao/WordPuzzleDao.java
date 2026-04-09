package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.WordPuzzle;

import java.util.List;

public interface WordPuzzleDao {
    /**
     * 保存字词谜题游戏数据
     * @param data 字词谜题数据对象
     * @return 数据ID，保存失败返回-1
     */
    long save(WordPuzzle data);

    /**
     * 根据ID查找字词谜题数据
     * @param dataId 数据ID
     * @return 字词谜题数据对象，不存在返回null
     */
    WordPuzzle findById(int dataId);

    /**
     * 根据难度获取字词谜题数据
     * @param difficulty 难度等级
     * @return 字词谜题数据列表
     */
    List<WordPuzzle> findByDifficulty(String difficulty);

    /**
     * 获取所有活跃的字词谜题数据
     * @return 字词谜题数据列表
     */
    List<WordPuzzle> findActiveData();

    /**
     * 获取随机字词谜题数据（用于随机出题）
     * @param difficulty 难度等级
     * @param count 获取数量
     * @return 随机字词谜题数据列表
     */
    List<WordPuzzle> getRandomData(String difficulty, int count);

    /**
     * 更新字词谜题数据
     * @param data 字词谜题数据对象
     * @return 更新是否成功
     */
    boolean update(WordPuzzle data);

    /**
     * 启用/禁用字词谜题数据
     * @param dataId 数据ID
     * @param active 是否启用
     * @return 更新是否成功
     */
    boolean setActive(int dataId, boolean active);

    /**
     * 删除字词谜题数据
     * @param dataId 数据ID
     * @return 删除是否成功
     */
    boolean delete(int dataId);

    /**
     * 获取字词谜题数据的统计信息
     * @return 统计信息
     */
    WordPuzzleStatistics getStatistics();

    /**
     * 批量插入字词谜题数据（用于初始化数据）
     * @param dataList 字词谜题数据列表
     * @return 成功插入的数量
     */
    int batchInsert(List<WordPuzzle> dataList);

    /**
     * 检查指定难度的字词谜题数据是否存在
     * @param difficulty 难度等级
     * @return 是否存在
     */
    boolean hasData(String difficulty);

    /**
     * 获取字词谜题数据的总数
     * @param difficulty 难度等级（可选，为null时统计所有难度）
     * @return 数据总数
     */
    int getDataCount(String difficulty);

    /**
     * 从JSON文件中批量导入字词谜题数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的数量，失败返回-1
     */
    int importFromJson(String jsonContent);

    /**
     * 从assets文件夹中的JSON文件导入字词谜题数据
     * @param context Android上下文
     * @return 成功导入的数量，失败返回-1
     */
    int importFromAssetsJson(android.content.Context context);

    /**
     * 字词谜题数据统计信息类
     */
    class WordPuzzleStatistics {
        public int totalQuestions;
        public int activeQuestions;
        public int easyQuestions;
        public int mediumQuestions;
        public int hardQuestions;
    }
}