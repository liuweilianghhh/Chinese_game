package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.GameScore;

import java.util.List;

public interface GameScoreDao {
    /**
     * 保存游戏分数记录
     * @param gameScore 游戏分数对象
     * @return 记录ID，保存失败返回-1
     */
    long save(GameScore gameScore);

    /**
     * 更新游戏分数记录（按 id）
     * @param gameScore 须已设置 id
     * @return 是否更新成功
     */
    boolean update(GameScore gameScore);

    /**
     * 根据ID查找游戏分数记录
     * @param scoreId 记录ID
     * @return 游戏分数对象，不存在返回null
     */
    GameScore findById(int scoreId);

    /**
     * 根据用户ID查找游戏分数记录
     * @param userId 用户ID
     * @return 游戏分数记录列表
     */
    List<GameScore> findByUserId(int userId);

    /**
     * 根据用户ID和游戏类型查找游戏分数记录
     * @param userId 用户ID
     * @param gameType 游戏类型
     * @return 游戏分数记录列表
     */
    List<GameScore> findByUserIdAndGameType(int userId, GameScore.GameType gameType);

    /**
     * 获取用户在特定游戏类型中的最高分
     * @param userId 用户ID
     * @param gameType 游戏类型
     * @return 最高分，没有记录返回0
     */
    int getHighestScore(int userId, GameScore.GameType gameType);

    /**
     * 获取用户在特定游戏类型和难度中的平均分
     * @param userId 用户ID
     * @param gameType 游戏类型
     * @param difficulty 难度等级
     * @return 平均分，没有记录返回0.0
     */
    double getAverageScore(int userId, GameScore.GameType gameType, GameScore.Difficulty difficulty);

    /**
     * 获取用户游戏统计信息
     * @param userId 用户ID
     * @return 包含总游戏次数、总分数、平均准确率等的统计信息
     */
    GameStatistics getUserGameStatistics(int userId);

    /**
     * 获取最近的游戏记录
     * @param userId 用户ID
     * @param limit 记录数量限制
     * @return 最近的游戏记录列表
     */
    List<GameScore> getRecentGames(int userId, int limit);

    /**
     * 删除游戏分数记录
     * @param scoreId 记录ID
     * @return 删除是否成功
     */
    boolean delete(int scoreId);

    /**
     * 删除用户的所有游戏分数记录
     * @param userId 用户ID
     * @return 删除的记录数量
     */
    int deleteByUserId(int userId);

    /**
     * 游戏统计信息类
     */
    class GameStatistics {
        public int totalGames;
        public int totalScore;
        public double averageAccuracy;
        public long totalTimeSpent;
        public int gamesByType[]; // 按游戏类型统计

        public GameStatistics() {
            gamesByType = new int[GameScore.GameType.values().length];
        }
    }

    /**
     * 从JSON文件中批量导入游戏分数数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的游戏分数数量，失败返回-1
     */
    int importFromJson(String jsonContent);

    /**
     * 从assets文件夹中的JSON文件导入游戏分数数据
     * @param context Android上下文
     * @return 成功导入的游戏分数数量，失败返回-1
     */
    int importFromAssetsJson(android.content.Context context);
}