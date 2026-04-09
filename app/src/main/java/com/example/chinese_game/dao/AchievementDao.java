package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.Achievement;
import com.example.chinese_game.javabean.UserAchievement;

import java.util.List;

public interface AchievementDao {
    /**
     * 保存成就
     * @param achievement 成就对象
     * @return 成就ID，保存失败返回-1
     */
    long save(Achievement achievement);

    /**
     * 根据ID查找成就
     * @param achievementId 成就ID
     * @return 成就对象，不存在返回null
     */
    Achievement findById(int achievementId);

    /**
     * 获取所有成就
     * @return 成就列表
     */
    List<Achievement> findAll();

    /**
     * 根据类型查找成就
     * @param type 成就类型
     * @return 成就列表
     */
    List<Achievement> findByType(Achievement.AchievementType type);

    /**
     * 根据分类查找成就
     * @param category 成就分类
     * @return 成就列表
     */
    List<Achievement> findByCategory(Achievement.AchievementCategory category);

    /**
     * 更新成就信息
     * @param achievement 成就对象
     * @return 更新是否成功
     */
    boolean update(Achievement achievement);

    /**
     * 删除成就
     * @param achievementId 成就ID
     * @return 删除是否成功
     */
    boolean delete(int achievementId);

    // UserAchievement相关方法

    /**
     * 为用户解锁成就
     * @param userAchievement 用户成就对象
     * @return 保存是否成功
     */
    boolean unlockAchievement(UserAchievement userAchievement);

    /**
     * 检查用户是否已解锁某个成就
     * @param userId 用户ID
     * @param achievementId 成就ID
     * @return 是否已解锁
     */
    boolean isAchievementUnlocked(int userId, int achievementId);

    /**
     * 获取用户已解锁的所有成就
     * @param userId 用户ID
     * @return 用户成就列表
     */
    List<UserAchievement> getUserAchievements(int userId);

    /**
     * 获取用户的成就统计信息
     * @param userId 用户ID
     * @return 成就统计信息
     */
    AchievementStatistics getUserAchievementStatistics(int userId);

    /**
     * 检查并自动解锁里程碑成就
     * @param userId 用户ID
     * @return 新解锁的成就列表
     */
    List<Achievement> checkAndUnlockMilestoneAchievements(int userId);

    /**
     * 检查并自动解锁游戏特定成就
     * @param userId 用户ID
     * @param gameType 游戏类型
     * @param streakCount 连续成功次数
     * @return 新解锁的成就列表
     */
    List<Achievement> checkAndUnlockGameAchievements(int userId, String gameType, int streakCount);

    /**
     * 更新用户成就进度
     * @param userId 用户ID
     * @param achievementId 成就ID
     * @param progressValue 进度值
     * @return 更新是否成功
     */
    boolean updateAchievementProgress(int userId, int achievementId, int progressValue);

    /**
     * 删除用户的成就记录
     * @param userId 用户ID
     * @param achievementId 成就ID
     * @return 删除是否成功
     */
    boolean removeUserAchievement(int userId, int achievementId);

    /**
     * 从JSON文件中批量导入成就数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的成就数量，失败返回-1
     */
    int importAchievementsFromJson(String jsonContent);

    /**
     * 从JSON文件中批量导入用户成就数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的用户成就数量，失败返回-1
     */
    int importUserAchievementsFromJson(String jsonContent);

    /**
     * 从assets文件夹中的JSON文件导入成就数据
     * @param context Android上下文
     * @return 成功导入的成就数量，失败返回-1
     */
    int importAchievementsFromAssetsJson(android.content.Context context);

    /**
     * 从assets文件夹中的JSON文件导入用户成就数据
     * @param context Android上下文
     * @return 成功导入的用户成就数量，失败返回-1
     */
    int importUserAchievementsFromAssetsJson(android.content.Context context);

    /**
     * 成就统计信息类
     */
    class AchievementStatistics {
        public int totalAchievements;
        public int unlockedAchievements;
        public int totalRewardPoints;
        public int milestoneAchievements;
        public int gameAchievements;

        public AchievementStatistics() {
        }

        public double getCompletionRate() {
            return totalAchievements == 0 ? 0.0 : (double) unlockedAchievements / totalAchievements;
        }
    }
}