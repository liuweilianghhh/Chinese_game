package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.WordPuzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WordPuzzleDaoImpl implements WordPuzzleDao {
    private MYsqliteopenhelper dbHelper;

    public WordPuzzleDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long save(WordPuzzle data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("difficulty", data.getDifficulty());
        cv.put("hint", data.getHint());

        long result = db.insert("word_puzzle", null, cv);
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result;
    }

    @Override
    public WordPuzzle findById(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT wp.id, wp.sentence_id, wp.difficulty, wp.hint, " +
            "s.sentence AS sentence_text " +
            "FROM word_puzzle wp " +
            "JOIN sentences s ON wp.sentence_id = s.id " +
            "WHERE wp.id = ?",
            new String[]{String.valueOf(dataId)});

        WordPuzzle data = null;
        if (cursor.moveToFirst()) {
            data = cursorToWordPuzzle(cursor);
        }
        cursor.close();
        return data;
    }

    @Override
    public List<WordPuzzle> findByDifficulty(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT wp.id, wp.sentence_id, wp.difficulty, wp.hint, " +
            "s.sentence AS sentence_text " +
            "FROM word_puzzle wp " +
            "JOIN sentences s ON wp.sentence_id = s.id " +
            "WHERE wp.difficulty = ? " +
            "ORDER BY wp.id ASC",
            new String[]{difficulty});

        List<WordPuzzle> dataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            dataList.add(cursorToWordPuzzle(cursor));
        }
        cursor.close();
        return dataList;
    }

    @Override
    public List<WordPuzzle> findActiveData() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT wp.id, wp.sentence_id, wp.difficulty, wp.hint, " +
            "s.sentence AS sentence_text " +
            "FROM word_puzzle wp " +
            "JOIN sentences s ON wp.sentence_id = s.id " +
            "ORDER BY wp.difficulty ASC, wp.id ASC",
            null);

        List<WordPuzzle> dataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            dataList.add(cursorToWordPuzzle(cursor));
        }
        cursor.close();
        return dataList;
    }

    @Override
    public List<WordPuzzle> getRandomData(String difficulty, int count) {
        List<WordPuzzle> allData = findByDifficulty(difficulty);

        // If not enough data, return all available
        if (allData.size() <= count) {
            return allData;
        }

        // Randomly select specified number of items
        Collections.shuffle(allData);
        return allData.subList(0, count);
    }

    @Override
    public boolean update(WordPuzzle data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("difficulty", data.getDifficulty());
        cv.put("hint", data.getHint());

        int result = db.update("word_puzzle", cv, "id=?",
                new String[]{String.valueOf(data.getId())});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public boolean setActive(int dataId, boolean active) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();

        int result = db.update("word_puzzle", cv, "id=?",
                new String[]{String.valueOf(dataId)});
        return result > 0;
    }

    @Override
    public boolean delete(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("word_puzzle", "id=?", new String[]{String.valueOf(dataId)});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public WordPuzzleStatistics getStatistics() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        WordPuzzleStatistics stats = new WordPuzzleStatistics();

        // 获取总数
        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM word_puzzle", null);
        if (totalCursor.moveToFirst()) {
            stats.totalQuestions = totalCursor.getInt(0);
        }
        totalCursor.close();

        // Get active questions
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM word_puzzle", null);
        if (countCursor.moveToFirst()) {
            stats.activeQuestions = countCursor.getInt(0);
        }
        countCursor.close();

        // Group by difficulty
        Cursor difficultyCursor = db.rawQuery(
            "SELECT difficulty, COUNT(*) FROM word_puzzle GROUP BY difficulty", null);
        while (difficultyCursor.moveToNext()) {
            String difficultyStr = difficultyCursor.getString(0);
            int count = difficultyCursor.getInt(1);

            switch (difficultyStr) {
                case "EASY":
                    stats.easyQuestions = count;
                    break;
                case "MEDIUM":
                    stats.mediumQuestions = count;
                    break;
                case "HARD":
                    stats.hardQuestions = count;
                    break;
            }
        }
        difficultyCursor.close();
        return stats;
    }

    @Override
    public int batchInsert(List<WordPuzzle> dataList) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int successCount = 0;

        db.beginTransaction();
        try {
            for (WordPuzzle data : dataList) {
                ContentValues cv = new ContentValues();
                cv.put("sentence_id", data.getSentenceId());
                cv.put("difficulty", data.getDifficulty());
                cv.put("hint", data.getHint());

                long result = db.insert("word_puzzle", null, cv);
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
    public boolean hasData(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM word_puzzle WHERE difficulty=?",
            new String[]{difficulty});

        boolean hasData = false;
        if (cursor.moveToFirst()) {
            hasData = cursor.getInt(0) > 0;
        }
        cursor.close();
        return hasData;
    }

    @Override
    public int getDataCount(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        String query;
        String[] args;

        if (difficulty != null) {
            query = "SELECT COUNT(*) FROM word_puzzle WHERE difficulty=?";
            args = new String[]{difficulty};
        } else {
            query = "SELECT COUNT(*) FROM word_puzzle";
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

    private WordPuzzle cursorToWordPuzzle(Cursor cursor) {
        WordPuzzle data = new WordPuzzle();
        data.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        data.setSentenceId(cursor.getInt(cursor.getColumnIndexOrThrow("sentence_id")));
        data.setDifficulty(cursor.getString(cursor.getColumnIndexOrThrow("difficulty")));
        data.setHint(cursor.getString(cursor.getColumnIndexOrThrow("hint")));
        int sentenceTextIndex = cursor.getColumnIndex("sentence_text");
        if (sentenceTextIndex != -1) {
            data.setSentence(cursor.getString(sentenceTextIndex));
        }

        return data;
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

            db.beginTransaction();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);

                String sentence = jsonData.optString("sentence", "");
                String difficulty = jsonData.optString("difficulty", "EASY");
                String hint = jsonData.optString("hint", null);

                // 1. sentences 中确保有对应记录
                int sentenceId = -1;
                Cursor sentenceCursor = db.rawQuery(
                    "SELECT id FROM sentences WHERE sentence = ?",
                    new String[]{sentence});
                if (sentenceCursor.moveToFirst()) {
                    sentenceId = sentenceCursor.getInt(0);
                }
                sentenceCursor.close();

                if (sentenceId == -1) {
                    ContentValues sentenceValues = new ContentValues();
                    sentenceValues.put("sentence", sentence);
                    sentenceValues.put("pinyin", (String) null);
                    sentenceValues.put("difficulty", difficulty);
                    sentenceValues.put("category", (String) null);
                    // 统计非空的word1..word6作为word_count
                    int wordCount = 0;
                    for (int w = 1; w <= 6; w++) {
                        String key = "word" + w;
                        if (!jsonData.isNull(key) && !jsonData.optString(key, "").isEmpty()) {
                            wordCount++;
                        }
                    }
                    sentenceValues.put("word_count", wordCount);
                    long newSentenceId = db.insert("sentences", null, sentenceValues);
                    if (newSentenceId == -1) {
                        continue;
                    }
                    sentenceId = (int) newSentenceId;
                }

                // 2. 在 sentence_words 中插入分词（基于 word1..word6）
                int order = 1;
                for (int w = 1; w <= 6; w++) {
                    String key = "word" + w;
                    String word = jsonData.optString(key, null);
                    if (word == null || word.isEmpty()) {
                        continue;
                    }
                    // 检查是否已存在该序号的词
                    Cursor wordCursor = db.rawQuery(
                        "SELECT id FROM sentence_words WHERE sentence_id = ? AND word_order = ?",
                        new String[]{String.valueOf(sentenceId), String.valueOf(order)});
                    boolean exists = wordCursor.moveToFirst();
                    wordCursor.close();
                    if (!exists) {
                        ContentValues wordValues = new ContentValues();
                        wordValues.put("sentence_id", sentenceId);
                        wordValues.put("word", word);
                        wordValues.put("pinyin", (String) null);
                        wordValues.put("pos_tag", "X");
                        wordValues.put("word_order", order);
                        wordValues.put("word_position", order);
                        wordValues.put("word_difficulty", difficulty);
                        wordValues.put("word_frequency", 0);
                        db.insert("sentence_words", null, wordValues);
                    }
                    order++;
                }

                // 3. 检查 word_puzzle 是否已有记录
                boolean dataExists = false;
                Cursor existsCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM word_puzzle WHERE sentence_id = ? AND difficulty = ?",
                    new String[]{String.valueOf(sentenceId), difficulty});
                if (existsCursor.moveToFirst()) {
                    dataExists = existsCursor.getInt(0) > 0;
                }
                existsCursor.close();

                if (!dataExists) {
                    ContentValues puzzleValues = new ContentValues();
                    puzzleValues.put("sentence_id", sentenceId);
                    puzzleValues.put("difficulty", difficulty);
                    puzzleValues.put("hint", hint);
                    long result = db.insert("word_puzzle", null, puzzleValues);
                    if (result != -1) {
                        importedCount++;
                    }
                }
            }

            db.setTransactionSuccessful();
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        } finally {
            db.endTransaction();
        }

        return importedCount;
    }

    @Override
    public int importFromAssetsJson(android.content.Context context) {
        try {
            java.io.InputStream inputStream = context.getAssets().open("json/word_puzzle.json");
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