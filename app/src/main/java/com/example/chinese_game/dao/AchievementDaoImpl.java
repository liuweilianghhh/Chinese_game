package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.ChineseGameApplication;
import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.Achievement;
import com.example.chinese_game.javabean.GameScore;
import com.example.chinese_game.javabean.User;
import com.example.chinese_game.javabean.UserAchievement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AchievementDaoImpl implements AchievementDao {
    private MYsqliteopenhelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private UserDao userDao;
    private GameScoreDao gameScoreDao;

    public AchievementDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
        this.userDao = new UserDaoImpl(context);
        this.gameScoreDao = new GameScoreDaoImpl(context);
    }

    @Override
    public long save(Achievement achievement) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", achievement.getName());
        cv.put("description", achievement.getDescription());
        cv.put("icon_path", achievement.getIconPath());
        cv.put("type", achievement.getType().name());
        cv.put("category", achievement.getCategory().name());
        cv.put("required_value", achievement.getRequiredValue());
        cv.put("reward_points", achievement.getRewardPoints());

        long result = db.insert("achievements", null, cv);
        // Keep database connection open for App Inspection real-time access
        return result;
    }

    @Override
    public Achievement findById(int achievementId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("achievements", null, "id=?",
                new String[]{String.valueOf(achievementId)}, null, null, null);

        Achievement achievement = null;
        if (cursor.moveToFirst()) {
            achievement = cursorToAchievement(cursor);
        }
        cursor.close();
        // Keep database connection open for App Inspection real-time access
        return achievement;
    }

    @Override
    public List<Achievement> findAll() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("achievements", null, null,
                null, null, null, "type ASC, name ASC");

        List<Achievement> achievements = new ArrayList<>();
        while (cursor.moveToNext()) {
            achievements.add(cursorToAchievement(cursor));
        }
        cursor.close();
        return achievements;
    }

    @Override
    public List<Achievement> findByType(Achievement.AchievementType type) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("achievements", null, "type=?",
                new String[]{type.name()}, null, null, "name ASC");

        List<Achievement> achievements = new ArrayList<>();
        while (cursor.moveToNext()) {
            achievements.add(cursorToAchievement(cursor));
        }
        cursor.close();
        return achievements;
    }

    @Override
    public List<Achievement> findByCategory(Achievement.AchievementCategory category) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("achievements", null, "category=?",
                new String[]{category.name()}, null, null, "name ASC");

        List<Achievement> achievements = new ArrayList<>();
        while (cursor.moveToNext()) {
            achievements.add(cursorToAchievement(cursor));
        }
        cursor.close();
        return achievements;
    }

    @Override
    public boolean update(Achievement achievement) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", achievement.getName());
        cv.put("description", achievement.getDescription());
        cv.put("icon_path", achievement.getIconPath());
        cv.put("type", achievement.getType().name());
        cv.put("category", achievement.getCategory().name());
        cv.put("required_value", achievement.getRequiredValue());
        cv.put("reward_points", achievement.getRewardPoints());

        int result = db.update("achievements", cv, "id=?",
                new String[]{String.valueOf(achievement.getId())});
        return result > 0;
    }

    @Override
    public boolean delete(int achievementId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("achievements", "id=?", new String[]{String.valueOf(achievementId)});
        return result > 0;
    }

    @Override
    public boolean unlockAchievement(UserAchievement userAchievement) {
        if (isAchievementUnlocked(userAchievement.getUserId(), userAchievement.getAchievementId())) {
            return false; // Already unlocked
        }

        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", userAchievement.getUserId());
        cv.put("achievement_id", userAchievement.getAchievementId());
        cv.put("unlocked_date", dateFormat.format(userAchievement.getUnlockedDate()));
        cv.put("progress_value", userAchievement.getProgressValue());

        long result = db.insert("user_achievements", null, cv);
        return result != -1;
    }

    @Override
    public boolean isAchievementUnlocked(int userId, int achievementId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("user_achievements", null,
                "user_id=? AND achievement_id=?",
                new String[]{String.valueOf(userId), String.valueOf(achievementId)},
                null, null, null);

        boolean unlocked = cursor.getCount() > 0;
        cursor.close();
        return unlocked;
    }

    @Override
    public List<UserAchievement> getUserAchievements(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT ua.*, a.name, a.description, a.reward_points " +
            "FROM user_achievements ua " +
            "JOIN achievements a ON ua.achievement_id = a.id " +
            "WHERE ua.user_id=? " +
            "ORDER BY ua.unlocked_date DESC",
            new String[]{String.valueOf(userId)});

        List<UserAchievement> userAchievements = new ArrayList<>();
        while (cursor.moveToNext()) {
            UserAchievement ua = new UserAchievement();
            ua.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            ua.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
            ua.setAchievementId(cursor.getInt(cursor.getColumnIndexOrThrow("achievement_id")));

            try {
                String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("unlocked_date"));
                if (dateStr != null) {
                    ua.setUnlockedDate(dateFormat.parse(dateStr));
                }
            } catch (ParseException e) {
                ua.setUnlockedDate(new Date());
            }

            ua.setProgressValue(cursor.getInt(cursor.getColumnIndexOrThrow("progress_value")));
            userAchievements.add(ua);
        }
        cursor.close();
        return userAchievements;
    }

    @Override
    public AchievementStatistics getUserAchievementStatistics(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        AchievementStatistics stats = new AchievementStatistics();

        // Get total achievements count
        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM achievements", null);
        if (totalCursor.moveToFirst()) {
            stats.totalAchievements = totalCursor.getInt(0);
        }
        totalCursor.close();

        // Get user's unlocked achievements count and reward points
        Cursor userCursor = db.rawQuery(
            "SELECT COUNT(*), SUM(a.reward_points) " +
            "FROM user_achievements ua " +
            "JOIN achievements a ON ua.achievement_id = a.id " +
            "WHERE ua.user_id=?",
            new String[]{String.valueOf(userId)});

        if (userCursor.moveToFirst()) {
            stats.unlockedAchievements = userCursor.getInt(0);
            stats.totalRewardPoints = userCursor.isNull(1) ? 0 : userCursor.getInt(1);
        }
        userCursor.close();

        // Get milestone achievements count
        Cursor milestoneCursor = db.rawQuery(
            "SELECT COUNT(*) FROM user_achievements ua " +
            "JOIN achievements a ON ua.achievement_id = a.id " +
            "WHERE ua.user_id=? AND a.type='MILESTONE'",
            new String[]{String.valueOf(userId)});

        if (milestoneCursor.moveToFirst()) {
            stats.milestoneAchievements = milestoneCursor.getInt(0);
        }
        milestoneCursor.close();

        // Get game achievements count
        Cursor gameCursor = db.rawQuery(
            "SELECT COUNT(*) FROM user_achievements ua " +
            "JOIN achievements a ON ua.achievement_id = a.id " +
            "WHERE ua.user_id=? AND a.type='GAME_SPECIFIC'",
            new String[]{String.valueOf(userId)});

        if (gameCursor.moveToFirst()) {
            stats.gameAchievements = gameCursor.getInt(0);
        }
        gameCursor.close();

        return stats;
    }

    @Override
    public List<Achievement> checkAndUnlockMilestoneAchievements(int userId) {
        List<Achievement> newlyUnlocked = new ArrayList<>();
        User user = userDao.findById(userId);
        if (user == null) return newlyUnlocked;

        List<Achievement> milestones = findByType(Achievement.AchievementType.MILESTONE);

        for (Achievement achievement : milestones) {
            if (isAchievementUnlocked(userId, achievement.getId())) {
                continue; // 已经解锁
            }

            boolean shouldUnlock = false;

            switch (achievement.getCategory()) {
                case GENERAL:
                    if (achievement.getName().equals("First Steps")) {
                        shouldUnlock = user.getTotalGamesPlayed() >= achievement.getRequiredValue();
                    }
                    break;
                case LOGIN_STREAK:
                    if (achievement.getName().equals("Login Streak")) {
                        shouldUnlock = user.getLoginStreak() >= achievement.getRequiredValue();
                    }
                    break;
            }

            if (shouldUnlock) {
                UserAchievement ua = new UserAchievement(userId, achievement.getId());
                if (unlockAchievement(ua)) {
                    newlyUnlocked.add(achievement);
                }
            }
        }

        return newlyUnlocked;
    }

    @Override
    public List<Achievement> checkAndUnlockGameAchievements(int userId, String gameType, int streakCount) {
        List<Achievement> newlyUnlocked = new ArrayList<>();

        List<Achievement> gameAchievements = findByType(Achievement.AchievementType.GAME_SPECIFIC);

        for (Achievement achievement : gameAchievements) {
            if (isAchievementUnlocked(userId, achievement.getId())) {
                continue; // 已经解锁
            }

            boolean shouldUnlock = false;

            if (achievement.getCategory().name().equals(gameType)) {
                if (achievement.getName().equals("Perfect Match") ||
                    achievement.getName().equals("Excellent Pronunciation")) {
                    shouldUnlock = streakCount >= achievement.getRequiredValue();
                }
            }

            if (shouldUnlock) {
                UserAchievement ua = new UserAchievement(userId, achievement.getId());
                if (unlockAchievement(ua)) {
                    newlyUnlocked.add(achievement);
                }
            }
        }

        return newlyUnlocked;
    }

    @Override
    public boolean updateAchievementProgress(int userId, int achievementId, int progressValue) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("progress_value", progressValue);

        int result = db.update("user_achievements", cv,
                "user_id=? AND achievement_id=?",
                new String[]{String.valueOf(userId), String.valueOf(achievementId)});
        return result > 0;
    }

    @Override
    public boolean removeUserAchievement(int userId, int achievementId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("user_achievements",
                "user_id=? AND achievement_id=?",
                new String[]{String.valueOf(userId), String.valueOf(achievementId)});
        return result > 0;
    }

    private Achievement cursorToAchievement(Cursor cursor) {
        Achievement achievement = new Achievement();
        achievement.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        achievement.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        achievement.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        achievement.setIconPath(cursor.getString(cursor.getColumnIndexOrThrow("icon_path")));

        // Parse achievement type
        String typeStr = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        try {
            achievement.setType(Achievement.AchievementType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            achievement.setType(Achievement.AchievementType.MILESTONE);
        }

        // Parse achievement category
        String categoryStr = cursor.getString(cursor.getColumnIndexOrThrow("category"));
        try {
            achievement.setCategory(Achievement.AchievementCategory.valueOf(categoryStr));
        } catch (IllegalArgumentException e) {
            achievement.setCategory(Achievement.AchievementCategory.GENERAL);
        }

        achievement.setRequiredValue(cursor.getInt(cursor.getColumnIndexOrThrow("required_value")));
        achievement.setRewardPoints(cursor.getInt(cursor.getColumnIndexOrThrow("reward_points")));

        return achievement;
    }

    @Override
    public int importAchievementsFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return -1;
        }

        int importedCount = 0;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        try {
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonAchievement = jsonArray.getJSONObject(i);

                Achievement achievement = new Achievement();
                achievement.setName(jsonAchievement.optString("name", ""));
                achievement.setDescription(jsonAchievement.optString("description", ""));
                achievement.setIconPath(jsonAchievement.optString("icon_path", null));
                achievement.setRequiredValue(jsonAchievement.optInt("required_value", 0));
                achievement.setRewardPoints(jsonAchievement.optInt("reward_points", 0));

                // 解析成就类型
                String typeStr = jsonAchievement.optString("type", "MILESTONE");
                try {
                    achievement.setType(Achievement.AchievementType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    achievement.setType(Achievement.AchievementType.MILESTONE);
                }

                // 解析成就分类
                String categoryStr = jsonAchievement.optString("category", "GENERAL");
                try {
                    achievement.setCategory(Achievement.AchievementCategory.valueOf(categoryStr));
                } catch (IllegalArgumentException e) {
                    achievement.setCategory(Achievement.AchievementCategory.GENERAL);
                }

                // 检查成就名称是否已存在
                boolean achievementExists = false;
                for (Achievement existing : findAll()) {
                    if (existing.getName().equals(achievement.getName())) {
                        achievementExists = true;
                        break;
                    }
                }

                if (!achievementExists) {
                    long result = save(achievement);
                    if (result != -1) {
                        importedCount++;
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }

        return importedCount;
    }

    @Override
    public int importUserAchievementsFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return -1;
        }

        int importedCount = 0;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        try {
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonUserAchievement = jsonArray.getJSONObject(i);

                int userId = jsonUserAchievement.optInt("user_id", -1);
                int achievementId = jsonUserAchievement.optInt("achievement_id", -1);

                if (userId == -1 || achievementId == -1) {
                    continue; // 跳过无效数据
                }

                UserAchievement userAchievement = new UserAchievement(userId, achievementId);

                // 解析日期
                String unlockedDateStr = jsonUserAchievement.optString("unlocked_date", null);
                if (unlockedDateStr != null && !unlockedDateStr.isEmpty()) {
                    try {
                        userAchievement.setUnlockedDate(dateFormat.parse(unlockedDateStr));
                    } catch (ParseException e) {
                        userAchievement.setUnlockedDate(new Date());
                    }
                } else {
                    userAchievement.setUnlockedDate(new Date());
                }

                userAchievement.setProgressValue(jsonUserAchievement.optInt("progress_value", 0));

                boolean result = unlockAchievement(userAchievement);
                if (result) {
                    importedCount++;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }

        return importedCount;
    }

    @Override
    public int importAchievementsFromAssetsJson(android.content.Context context) {
        try {
            java.io.InputStream inputStream = context.getAssets().open("json/achievements.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String jsonContent = new String(buffer, "UTF-8");
            return importAchievementsFromJson(jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int importUserAchievementsFromAssetsJson(android.content.Context context) {
        try {
            java.io.InputStream inputStream = context.getAssets().open("json/user_achievements.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String jsonContent = new String(buffer, "UTF-8");
            return importUserAchievementsFromJson(jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
