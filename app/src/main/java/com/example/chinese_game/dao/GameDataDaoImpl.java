package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.GameData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GameDataDaoImpl implements GameDataDao {
    private MYsqliteopenhelper dbHelper;

    public GameDataDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long save(GameData gameData) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("game_type", gameData.getGameType().name());
        cv.put("difficulty", gameData.getDifficulty().name());
        cv.put("question_data", gameData.getQuestionData());
        cv.put("correct_answer", gameData.getCorrectAnswer());
        cv.put("hint", gameData.getHint());
        cv.put("order_index", gameData.getOrderIndex());

        long result = db.insert("game_data", null, cv);
        return result;
    }

    @Override
    public GameData findById(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("game_data", null, "id=?",
                new String[]{String.valueOf(dataId)}, null, null, null);

        GameData gameData = null;
        if (cursor.moveToFirst()) {
            gameData = cursorToGameData(cursor);
        }
        cursor.close();
        return gameData;
    }

    @Override
    public List<GameData> findByGameTypeAndDifficulty(GameData.GameType gameType, GameData.Difficulty difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("game_data", null,
                "game_type=? AND difficulty=?",
                new String[]{gameType.name(), difficulty.name()},
                null, null, "order_index ASC");

        List<GameData> gameDataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            gameDataList.add(cursorToGameData(cursor));
        }
        cursor.close();
        return gameDataList;
    }

    @Override
    public List<GameData> findActiveByGameType(GameData.GameType gameType) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("game_data", null,
                "game_type=?",
                new String[]{gameType.name()},
                null, null, "difficulty ASC, order_index ASC");

        List<GameData> gameDataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            gameDataList.add(cursorToGameData(cursor));
        }
        cursor.close();
        return gameDataList;
    }

    @Override
    public List<GameData> getRandomGameData(GameData.GameType gameType, GameData.Difficulty difficulty, int count) {
        List<GameData> allData = findByGameTypeAndDifficulty(gameType, difficulty);

        // If not enough data, return all available
        if (allData.size() <= count) {
            return allData;
        }

        // Randomly select specified number of items
        Collections.shuffle(allData);
        return allData.subList(0, count);
    }

    @Override
    public boolean update(GameData gameData) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("game_type", gameData.getGameType().name());
        cv.put("difficulty", gameData.getDifficulty().name());
        cv.put("question_data", gameData.getQuestionData());
        cv.put("correct_answer", gameData.getCorrectAnswer());
        cv.put("hint", gameData.getHint());
        cv.put("order_index", gameData.getOrderIndex());

        int result = db.update("game_data", cv, "id=?",
                new String[]{String.valueOf(gameData.getId())});
        return result > 0;
    }

    @Override
    public boolean setActive(int dataId, boolean active) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();

        int result = db.update("game_data", cv, "id=?",
                new String[]{String.valueOf(dataId)});
        return result > 0;
    }

    @Override
    public boolean delete(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("game_data", "id=?", new String[]{String.valueOf(dataId)});
        return result > 0;
    }

    @Override
    public GameDataStatistics getGameDataStatistics() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        GameDataStatistics stats = new GameDataStatistics();

        // 获取总数
        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM game_data", null);
        if (totalCursor.moveToFirst()) {
            stats.totalQuestions = totalCursor.getInt(0);
        }
        totalCursor.close();

        // Get active questions
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM game_data", null);
        if (countCursor.moveToFirst()) {
            stats.activeQuestions = countCursor.getInt(0);
        }
        countCursor.close();

        // Group by game type
        Cursor typeCursor = db.rawQuery(
            "SELECT game_type, COUNT(*) FROM game_data GROUP BY game_type", null);
        while (typeCursor.moveToNext()) {
            String gameTypeStr = typeCursor.getString(0);
            int count = typeCursor.getInt(1);

            try {
                GameData.GameType gameType = GameData.GameType.valueOf(gameTypeStr);
                stats.questionsByType[gameType.ordinal()] = count;
            } catch (IllegalArgumentException e) {
                // Ignore invalid game types
            }
        }
        typeCursor.close();

        // Group by difficulty
        Cursor difficultyCursor = db.rawQuery(
            "SELECT difficulty, COUNT(*) FROM game_data GROUP BY difficulty", null);
        while (difficultyCursor.moveToNext()) {
            String difficultyStr = difficultyCursor.getString(0);
            int count = difficultyCursor.getInt(1);

            try {
                GameData.Difficulty difficulty = GameData.Difficulty.valueOf(difficultyStr);
                stats.questionsByDifficulty[difficulty.ordinal()] = count;
            } catch (IllegalArgumentException e) {
                // Ignore invalid difficulty levels
            }
        }
        difficultyCursor.close();

        return stats;
    }

    @Override
    public int batchInsert(List<GameData> gameDataList) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int successCount = 0;

        db.beginTransaction();
        try {
            for (GameData gameData : gameDataList) {
                ContentValues cv = new ContentValues();
                cv.put("game_type", gameData.getGameType().name());
                cv.put("difficulty", gameData.getDifficulty().name());
                cv.put("question_data", gameData.getQuestionData());
                cv.put("correct_answer", gameData.getCorrectAnswer());
                cv.put("hint", gameData.getHint());
                cv.put("order_index", gameData.getOrderIndex());

                long result = db.insert("game_data", null, cv);
                if (result != -1) {
                    successCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return successCount;
    }

    @Override
    public boolean hasGameData(GameData.GameType gameType, GameData.Difficulty difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM game_data WHERE game_type=? AND difficulty=? AND is_active=1",
            new String[]{gameType.name(), difficulty.name()});

        boolean hasData = false;
        if (cursor.moveToFirst()) {
            hasData = cursor.getInt(0) > 0;
        }
        cursor.close();
        return hasData;
    }

    @Override
    public int getGameDataCount(GameData.GameType gameType, GameData.Difficulty difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        String query;
        String[] args;

        if (gameType != null && difficulty != null) {
            query = "SELECT COUNT(*) FROM game_data WHERE game_type=? AND difficulty=? AND is_active=1";
            args = new String[]{gameType.name(), difficulty.name()};
        } else if (gameType != null) {
            query = "SELECT COUNT(*) FROM game_data WHERE game_type=? AND is_active=1";
            args = new String[]{gameType.name()};
        } else if (difficulty != null) {
            query = "SELECT COUNT(*) FROM game_data WHERE difficulty=? AND is_active=1";
            args = new String[]{difficulty.name()};
        } else {
            query = "SELECT COUNT(*) FROM game_data WHERE is_active=1";
            args = null;
        }

        Cursor cursor = db.rawQuery(query, args);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private GameData cursorToGameData(Cursor cursor) {
        GameData gameData = new GameData();
        gameData.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));

        // 解析游戏类型
        String gameTypeStr = cursor.getString(cursor.getColumnIndexOrThrow("game_type"));
        try {
            gameData.setGameType(GameData.GameType.valueOf(gameTypeStr));
        } catch (IllegalArgumentException e) {
            gameData.setGameType(GameData.GameType.CHARACTER_MATCHING);
        }

        // 解析难度等级
        String difficultyStr = cursor.getString(cursor.getColumnIndexOrThrow("difficulty"));
        try {
            gameData.setDifficulty(GameData.Difficulty.valueOf(difficultyStr));
        } catch (IllegalArgumentException e) {
            gameData.setDifficulty(GameData.Difficulty.EASY);
        }

        gameData.setQuestionData(cursor.getString(cursor.getColumnIndexOrThrow("question_data")));
        gameData.setCorrectAnswer(cursor.getString(cursor.getColumnIndexOrThrow("correct_answer")));
        gameData.setHint(cursor.getString(cursor.getColumnIndexOrThrow("hint")));
        gameData.setActive(cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1);
        gameData.setOrderIndex(cursor.getInt(cursor.getColumnIndexOrThrow("order_index")));

        return gameData;
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
                JSONObject jsonGameData = jsonArray.getJSONObject(i);

                GameData gameData = new GameData();
                gameData.setQuestionData(jsonGameData.optString("question_data", ""));
                gameData.setCorrectAnswer(jsonGameData.optString("correct_answer", ""));
                gameData.setHint(jsonGameData.optString("hint", null));
                gameData.setActive(jsonGameData.optBoolean("is_active", true));
                gameData.setOrderIndex(jsonGameData.optInt("order_index", 0));

                // 解析游戏类型
                String gameTypeStr = jsonGameData.optString("game_type", "CHARACTER_MATCHING");
                try {
                    gameData.setGameType(GameData.GameType.valueOf(gameTypeStr));
                } catch (IllegalArgumentException e) {
                    gameData.setGameType(GameData.GameType.CHARACTER_MATCHING);
                }

                // 解析难度等级
                String difficultyStr = jsonGameData.optString("difficulty", "EASY");
                try {
                    gameData.setDifficulty(GameData.Difficulty.valueOf(difficultyStr));
                } catch (IllegalArgumentException e) {
                    gameData.setDifficulty(GameData.Difficulty.EASY);
                }

                // 检查题目数据是否已存在
                boolean questionExists = false;
                List<GameData> existingData = findActiveByGameType(gameData.getGameType());
                for (GameData existing : existingData) {
                    if (existing.getQuestionData().equals(gameData.getQuestionData())) {
                        questionExists = true;
                        break;
                    }
                }

                if (!questionExists) {
                    long result = save(gameData);
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
    public int importFromAssetsJson(android.content.Context context) {
        try {
            java.io.InputStream inputStream = context.getAssets().open("json/game_data.json");
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
