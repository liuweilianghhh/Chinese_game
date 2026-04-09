package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.GameData;

import java.util.List;

public interface GameDataDao {
    /**
     * 保存游戏数据
     * @param gameData 游戏数据对象
     * @return 数据ID，保存失败返回-1
     */
    long save(GameData gameData);

    /**
     * 根据ID查找游戏数据
     * @param dataId 数据ID
     * @return 游戏数据对象，不存在返回null
     */
    GameData findById(int dataId);

    /**
     * 根据游戏类型和难度获取游戏数据
     * @param gameType 游戏类型
     * @param difficulty 难度等级
     * @return 游戏数据列表
     */
    List<GameData> findByGameTypeAndDifficulty(GameData.GameType gameType, GameData.Difficulty difficulty);

    /**
     * 获取指定游戏类型的所有活跃游戏数据
     * @param gameType 游戏类型
     * @return 游戏数据列表
     */
    List<GameData> findActiveByGameType(GameData.GameType gameType);

    /**
     * 获取随机游戏数据（用于随机出题）
     * @param gameType 游戏类型
     * @param difficulty 难度等级
     * @param count 获取数量
     * @return 随机游戏数据列表
     */
    List<GameData> getRandomGameData(GameData.GameType gameType, GameData.Difficulty difficulty, int count);

    /**
     * 更新游戏数据
     * @param gameData 游戏数据对象
     * @return 更新是否成功
     */
    boolean update(GameData gameData);

    /**
     * 启用/禁用游戏数据
     * @param dataId 数据ID
     * @param active 是否启用
     * @return 更新是否成功
     */
    boolean setActive(int dataId, boolean active);

    /**
     * 删除游戏数据
     * @param dataId 数据ID
     * @return 删除是否成功
     */
    boolean delete(int dataId);

    /**
     * 获取游戏数据的统计信息
     * @return 游戏数据统计信息
     */
    GameDataStatistics getGameDataStatistics();

    /**
     * 批量插入游戏数据（用于初始化数据）
     * @param gameDataList 游戏数据列表
     * @return 成功插入的数量
     */
    int batchInsert(List<GameData> gameDataList);

    /**
     * 检查指定类型和难度的游戏数据是否存在
     * @param gameType 游戏类型
     * @param difficulty 难度等级
     * @return 是否存在
     */
    boolean hasGameData(GameData.GameType gameType, GameData.Difficulty difficulty);

    /**
     * 获取游戏数据的总数
     * @param gameType 游戏类型（可选，为null时统计所有类型）
     * @param difficulty 难度等级（可选，为null时统计所有难度）
     * @return 数据总数
     */
    int getGameDataCount(GameData.GameType gameType, GameData.Difficulty difficulty);

    /**
     * 游戏数据统计信息类
     */
    class GameDataStatistics {
        public int totalQuestions;
        public int activeQuestions;
        public int[] questionsByType; // 按游戏类型统计
        public int[] questionsByDifficulty; // 按难度统计

        public GameDataStatistics() {
            questionsByType = new int[GameData.GameType.values().length];
            questionsByDifficulty = new int[GameData.Difficulty.values().length];
        }
    }

    /**
     * 从JSON文件中批量导入游戏数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的游戏数据数量，失败返回-1
     */
    int importFromJson(String jsonContent);

    /**
     * 从assets文件夹中的JSON文件导入游戏数据
     * @param context Android上下文
     * @return 成功导入的游戏数据数量，失败返回-1
     */
    int importFromAssetsJson(android.content.Context context);
}