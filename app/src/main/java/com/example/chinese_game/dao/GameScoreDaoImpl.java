package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.GameScore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GameScoreDaoImpl implements GameScoreDao {
    private MYsqliteopenhelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public GameScoreDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long save(GameScore gameScore) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", gameScore.getUserId());
        cv.put("game_type", gameScore.getGameType().name());
        cv.put("difficulty", gameScore.getDifficulty().name());
        cv.put("score", gameScore.getScore());
        cv.put("max_possible_score", gameScore.getMaxPossibleScore());
        cv.put("accuracy", gameScore.getAccuracy());
        cv.put("time_spent", gameScore.getTimeSpent());
        cv.put("play_date", dateFormat.format(gameScore.getPlayDate()));
        cv.put("completed", gameScore.isCompleted() ? 1 : 0);

        long result = db.insert("game_scores", null, cv);
        return result;
    }

    @Override
    public boolean update(GameScore gameScore) {
        if (gameScore.getId() <= 0) return false;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("score", gameScore.getScore());
        cv.put("max_possible_score", gameScore.getMaxPossibleScore());
        cv.put("accuracy", gameScore.getAccuracy());
        cv.put("time_spent", gameScore.getTimeSpent());
        cv.put("completed", gameScore.isCompleted() ? 1 : 0);
        int rows = db.update("game_scores", cv, "id = ?", new String[]{String.valueOf(gameScore.getId())});
        return rows > 0;
    }

    @Override
    public GameScore findById(int scoreId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("game_scores", null, "id=?",
                new String[]{String.valueOf(scoreId)}, null, null, null);

        GameScore gameScore = null;
        if (cursor.moveToFirst()) {
            gameScore = cursorToGameScore(cursor);
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return gameScore;
    }

    @Override
    public List<GameScore> findByUserId(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("game_scores", null, "user_id=?",
                new String[]{String.valueOf(userId)}, null, null, "play_date DESC");

        List<GameScore> scores = new ArrayList<>();
        while (cursor.moveToNext()) {
            scores.add(cursorToGameScore(cursor));
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return scores;
    }

    @Override
    public List<GameScore> findByUserIdAndGameType(int userId, GameScore.GameType gameType) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("game_scores", null, "user_id=? AND game_type=?",
                new String[]{String.valueOf(userId), gameType.name()}, null, null, "play_date DESC");

        List<GameScore> scores = new ArrayList<>();
        while (cursor.moveToNext()) {
            scores.add(cursorToGameScore(cursor));
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return scores;
    }

    @Override
    public int getHighestScore(int userId, GameScore.GameType gameType) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT MAX(score) FROM game_scores WHERE user_id=? AND game_type=?",
            new String[]{String.valueOf(userId), gameType.name()});

        int highestScore = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            highestScore = cursor.getInt(0);
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return highestScore;
    }

    @Override
    public double getAverageScore(int userId, GameScore.GameType gameType, GameScore.Difficulty difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT AVG(score) FROM game_scores WHERE user_id=? AND game_type=? AND difficulty=?",
            new String[]{String.valueOf(userId), gameType.name(), difficulty.name()});

        double averageScore = 0.0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            averageScore = cursor.getDouble(0);
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return averageScore;
    }

    @Override
    public GameStatistics getUserGameStatistics(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        GameStatistics stats = new GameStatistics();

        // 获取基本统计信息
        Cursor basicCursor = db.rawQuery(
            "SELECT COUNT(*), SUM(score), AVG(accuracy), SUM(time_spent) " +
            "FROM game_scores WHERE user_id=? AND completed=1",
            new String[]{String.valueOf(userId)});

        if (basicCursor.moveToFirst()) {
            stats.totalGames = basicCursor.getInt(0);
            stats.totalScore = basicCursor.isNull(1) ? 0 : basicCursor.getInt(1);
            stats.averageAccuracy = basicCursor.isNull(2) ? 0.0 : basicCursor.getDouble(2);
            stats.totalTimeSpent = basicCursor.isNull(3) ? 0 : basicCursor.getLong(3);
        }
        basicCursor.close();

        // Group by game type
        Cursor typeCursor = db.rawQuery(
            "SELECT game_type, COUNT(*) FROM game_scores " +
            "WHERE user_id=? AND completed=1 GROUP BY game_type",
            new String[]{String.valueOf(userId)});

        while (typeCursor.moveToNext()) {
            String gameTypeStr = typeCursor.getString(0);
            int count = typeCursor.getInt(1);

            try {
                GameScore.GameType gameType = GameScore.GameType.valueOf(gameTypeStr);
                stats.gamesByType[gameType.ordinal()] = count;
            } catch (IllegalArgumentException e) {
                // Ignore invalid game types
            }
        }
        typeCursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问

        return stats;
    }

    @Override
    public List<GameScore> getRecentGames(int userId, int limit) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("game_scores", null, "user_id=?",
                new String[]{String.valueOf(userId)}, null, null, "play_date DESC", String.valueOf(limit));

        List<GameScore> scores = new ArrayList<>();
        while (cursor.moveToNext()) {
            scores.add(cursorToGameScore(cursor));
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return scores;
    }

    @Override
    public boolean delete(int scoreId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("game_scores", "id=?", new String[]{String.valueOf(scoreId)});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public int deleteByUserId(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("game_scores", "user_id=?", new String[]{String.valueOf(userId)});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result;
    }

    private GameScore cursorToGameScore(Cursor cursor) {
        GameScore gameScore = new GameScore();
        gameScore.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        gameScore.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));

        // 解析游戏类型
        String gameTypeStr = cursor.getString(cursor.getColumnIndexOrThrow("game_type"));
        try {
            gameScore.setGameType(GameScore.GameType.valueOf(gameTypeStr));
        } catch (IllegalArgumentException e) {
            gameScore.setGameType(GameScore.GameType.CHARACTER_MATCHING); // Default value
        }

        // 解析难度等级
        String difficultyStr = cursor.getString(cursor.getColumnIndexOrThrow("difficulty"));
        try {
            gameScore.setDifficulty(GameScore.Difficulty.valueOf(difficultyStr));
        } catch (IllegalArgumentException e) {
            gameScore.setDifficulty(GameScore.Difficulty.EASY); // Default value
        }

        gameScore.setScore(cursor.getInt(cursor.getColumnIndexOrThrow("score")));
        gameScore.setMaxPossibleScore(cursor.getInt(cursor.getColumnIndexOrThrow("max_possible_score")));
        gameScore.setAccuracy(cursor.getDouble(cursor.getColumnIndexOrThrow("accuracy")));
        gameScore.setTimeSpent(cursor.getLong(cursor.getColumnIndexOrThrow("time_spent")));

        // 解析日期
        try {
            String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("play_date"));
            if (dateStr != null) {
                gameScore.setPlayDate(dateFormat.parse(dateStr));
            }
        } catch (ParseException e) {
            gameScore.setPlayDate(new Date());
        }

        gameScore.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1);

        return gameScore;
    }

    @Override
    public int importFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return -1;
        }

        int importedCount = 0;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        try {
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonGameScore = jsonArray.getJSONObject(i);

                GameScore gameScore = new GameScore();
                gameScore.setUserId(jsonGameScore.optInt("user_id", -1));
                gameScore.setScore(jsonGameScore.optInt("score", 0));
                gameScore.setMaxPossibleScore(jsonGameScore.optInt("max_possible_score", 0));
                gameScore.setAccuracy(jsonGameScore.optDouble("accuracy", 0.0));
                gameScore.setTimeSpent(jsonGameScore.optLong("time_spent", 0));
                gameScore.setCompleted(jsonGameScore.optBoolean("completed", true));

                // 解析游戏类型
                String gameTypeStr = jsonGameScore.optString("game_type", "CHARACTER_MATCHING");
                try {
                    gameScore.setGameType(GameScore.GameType.valueOf(gameTypeStr));
                } catch (IllegalArgumentException e) {
                    gameScore.setGameType(GameScore.GameType.CHARACTER_MATCHING);
                }

                // 解析难度等级
                String difficultyStr = jsonGameScore.optString("difficulty", "EASY");
                try {
                    gameScore.setDifficulty(GameScore.Difficulty.valueOf(difficultyStr));
                } catch (IllegalArgumentException e) {
                    gameScore.setDifficulty(GameScore.Difficulty.EASY);
                }

                // 解析日期
                String playDateStr = jsonGameScore.optString("play_date", null);
                if (playDateStr != null && !playDateStr.isEmpty()) {
                    try {
                        gameScore.setPlayDate(dateFormat.parse(playDateStr));
                    } catch (ParseException e) {
                        gameScore.setPlayDate(new Date());
                    }
                } else {
                    gameScore.setPlayDate(new Date());
                }

                // 避免 reload 时重复插入：同 user_id + game_type + play_date 已存在则跳过
                Cursor existCursor = db.rawQuery(
                    "SELECT id FROM game_scores WHERE user_id=? AND game_type=? AND play_date=?",
                    new String[]{
                        String.valueOf(gameScore.getUserId()),
                        gameScore.getGameType().name(),
                        dateFormat.format(gameScore.getPlayDate())
                    });
                boolean exists = existCursor.moveToFirst();
                existCursor.close();
                if (exists) continue;

                long result = save(gameScore);
                if (result != -1) {
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
    public int importFromAssetsJson(android.content.Context context) {
        try {
            java.io.InputStream inputStream = context.getAssets().open("json/game_scores.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String jsonContent = new String(buffer, "UTF-8");
            return importFromJson(jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
