package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.CharacterMatching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CharacterMatchingDaoImpl implements CharacterMatchingDao {
    private MYsqliteopenhelper dbHelper;

    public CharacterMatchingDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long save(CharacterMatching data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("word_id", data.getWordId());
        cv.put("difficulty", data.getDifficulty());
        cv.put("hint", data.getHint());

        long result = db.insert("character_matching", null, cv);
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result;
    }

    @Override
    public long insertIfNotExists(CharacterMatching data) {
        if (data == null) return -1;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor c = db.query("character_matching", new String[]{"id"},
                "sentence_id = ? AND word_id = ?",
                new String[]{String.valueOf(data.getSentenceId()), String.valueOf(data.getWordId())},
                null, null, null);
        if (c.moveToFirst()) {
            long id = c.getLong(0);
            c.close();
            return id;
        }
        c.close();
        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("word_id", data.getWordId());
        cv.put("difficulty", data.getDifficulty() != null ? data.getDifficulty() : "EASY");
        cv.put("hint", data.getHint());
        return db.insert("character_matching", null, cv);
    }

    @Override
    public CharacterMatching findById(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT cm.id, cm.sentence_id, cm.word_id, cm.difficulty, cm.hint, sw.pos_tag AS pos_tag, " +
            "s.sentence AS sentence_text, sw.word AS word_text, sw.pinyin AS word_pinyin " +
            "FROM character_matching cm " +
            "JOIN sentences s ON cm.sentence_id = s.id " +
            "JOIN sentence_words sw ON cm.word_id = sw.id " +
            "WHERE cm.id = ?",
            new String[]{String.valueOf(dataId)});

        CharacterMatching data = null;
        if (cursor.moveToFirst()) {
            data = cursorToCharacterMatching(cursor);
        }
        cursor.close();
        return data;
    }

    @Override
    public List<CharacterMatching> findByDifficulty(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT cm.id, cm.sentence_id, cm.word_id, cm.difficulty, cm.hint, sw.pos_tag AS pos_tag, " +
            "s.sentence AS sentence_text, sw.word AS word_text, sw.pinyin AS word_pinyin " +
            "FROM character_matching cm " +
            "JOIN sentences s ON cm.sentence_id = s.id " +
            "JOIN sentence_words sw ON cm.word_id = sw.id " +
            "WHERE cm.difficulty = ? " +
            "ORDER BY cm.id ASC",
            new String[]{difficulty});

        List<CharacterMatching> dataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            dataList.add(cursorToCharacterMatching(cursor));
        }
        cursor.close();
        return dataList;
    }

    @Override
    public List<CharacterMatching> findAll() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT cm.id, cm.sentence_id, cm.word_id, cm.difficulty, cm.hint, sw.pos_tag AS pos_tag, " +
            "s.sentence AS sentence_text, sw.word AS word_text, sw.pinyin AS word_pinyin " +
            "FROM character_matching cm " +
            "JOIN sentences s ON cm.sentence_id = s.id " +
            "JOIN sentence_words sw ON cm.word_id = sw.id " +
            "ORDER BY cm.difficulty ASC, cm.id ASC",
            null);

        List<CharacterMatching> dataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            dataList.add(cursorToCharacterMatching(cursor));
        }
        cursor.close();
        return dataList;
    }

    @Override
    public List<CharacterMatching> getRandomData(String difficulty, int count) {
        List<CharacterMatching> allData = findByDifficulty(difficulty);

        // If not enough data, return all available
        if (allData.size() <= count) {
            return allData;
        }

        // Randomly select specified number of items
        Collections.shuffle(allData);
        return allData.subList(0, count);
    }

    @Override
    public boolean update(CharacterMatching data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("word_id", data.getWordId());
        cv.put("difficulty", data.getDifficulty());
        cv.put("hint", data.getHint());

        int result = db.update("character_matching", cv, "id=?",
                new String[]{String.valueOf(data.getId())});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }


    @Override
    public boolean delete(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("character_matching", "id=?", new String[]{String.valueOf(dataId)});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public CharacterMatchingStatistics getStatistics() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        CharacterMatchingStatistics stats = new CharacterMatchingStatistics();

        // 获取总数
        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM character_matching", null);
        if (totalCursor.moveToFirst()) {
            stats.totalQuestions = totalCursor.getInt(0);
        }
        totalCursor.close();

        // Get total questions
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM character_matching", null);
        if (countCursor.moveToFirst()) {
            stats.activeQuestions = countCursor.getInt(0);
        }
        countCursor.close();

        // Group by difficulty
        Cursor difficultyCursor = db.rawQuery(
            "SELECT difficulty, COUNT(*) FROM character_matching GROUP BY difficulty", null);
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
    public int batchInsert(List<CharacterMatching> dataList) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int successCount = 0;

        db.beginTransaction();
        try {
            for (CharacterMatching data : dataList) {
                ContentValues cv = new ContentValues();
                cv.put("sentence_id", data.getSentenceId());
                cv.put("word_id", data.getWordId());
                cv.put("difficulty", data.getDifficulty());
                cv.put("hint", data.getHint());

                long result = db.insert("character_matching", null, cv);
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
            "SELECT COUNT(*) FROM character_matching WHERE difficulty=?",
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
            query = "SELECT COUNT(*) FROM character_matching WHERE difficulty=?";
            args = new String[]{difficulty};
        } else {
            query = "SELECT COUNT(*) FROM character_matching";
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

    private CharacterMatching cursorToCharacterMatching(Cursor cursor) {
        CharacterMatching data = new CharacterMatching();
        data.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        data.setSentenceId(cursor.getInt(cursor.getColumnIndexOrThrow("sentence_id")));
        data.setWordId(cursor.getInt(cursor.getColumnIndexOrThrow("word_id")));
        data.setDifficulty(cursor.getString(cursor.getColumnIndexOrThrow("difficulty")));
        data.setHint(cursor.getString(cursor.getColumnIndexOrThrow("hint")));
        // 冗余展示字段（可能不存在于所有查询中，使用opt方式获取）
        int sentenceTextIndex = cursor.getColumnIndex("sentence_text");
        if (sentenceTextIndex != -1) {
            data.setSentence(cursor.getString(sentenceTextIndex));
        }
        int wordTextIndex = cursor.getColumnIndex("word_text");
        if (wordTextIndex != -1) {
            data.setWord(cursor.getString(wordTextIndex));
        }
        int wordPinyinIndex = cursor.getColumnIndex("word_pinyin");
        if (wordPinyinIndex != -1) {
            data.setPinyin(cursor.getString(wordPinyinIndex));
        }
        int posTagIndex = cursor.getColumnIndex("pos_tag");
        if (posTagIndex != -1) {
            data.setPosTag(cursor.getString(posTagIndex));
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
                String difficulty = jsonData.optString("difficulty", "EASY");
                String hint = jsonData.optString("hint", null);

                int sentenceId;
                int wordId;

                // 支持两种格式：1) sentence_id + word_id（需先导入 sentences / sentence_words）
                if (jsonData.has("sentence_id") && jsonData.has("word_id")) {
                    sentenceId = jsonData.optInt("sentence_id", -1);
                    wordId = jsonData.optInt("word_id", -1);
                    if (sentenceId <= 0 || wordId <= 0) continue;
                } else {
                    // 2) 旧格式：character + pinyin，自动创建/查找 sentences 与 sentence_words
                    String character = jsonData.optString("character", "");
                    String pinyin = jsonData.optString("pinyin", "");
                    if (character.isEmpty()) continue;

                    Cursor sentenceCursor = db.rawQuery(
                        "SELECT id FROM sentences WHERE sentence = ?",
                        new String[]{character});
                    sentenceId = -1;
                    if (sentenceCursor.moveToFirst()) {
                        sentenceId = sentenceCursor.getInt(0);
                    }
                    sentenceCursor.close();

                    if (sentenceId == -1) {
                        ContentValues sentenceValues = new ContentValues();
                        sentenceValues.put("sentence", character);
                        sentenceValues.put("pinyin", pinyin);
                        sentenceValues.put("difficulty", difficulty);
                        sentenceValues.put("category", (String) null);
                        sentenceValues.put("word_count", 1);
                        long newSentenceId = db.insert("sentences", null, sentenceValues);
                        if (newSentenceId == -1) continue;
                        sentenceId = (int) newSentenceId;
                    }

                    Cursor wordCursor = db.rawQuery(
                        "SELECT id FROM sentence_words WHERE sentence_id = ? AND word_order = 1",
                        new String[]{String.valueOf(sentenceId)});
                    wordId = -1;
                    if (wordCursor.moveToFirst()) {
                        wordId = wordCursor.getInt(0);
                    }
                    wordCursor.close();

                    if (wordId == -1) {
                        ContentValues wordValues = new ContentValues();
                        wordValues.put("sentence_id", sentenceId);
                        wordValues.put("word", character);
                        wordValues.put("pinyin", pinyin);
                        wordValues.put("pos_tag", "X");
                        wordValues.put("word_order", 1);
                        wordValues.put("word_position", 1);
                        wordValues.put("word_difficulty", difficulty);
                        wordValues.put("word_frequency", 0);
                        long newWordId = db.insert("sentence_words", null, wordValues);
                        if (newWordId == -1) continue;
                        wordId = (int) newWordId;
                    }
                }

                Cursor existsCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM character_matching WHERE sentence_id = ? AND word_id = ? AND difficulty = ?",
                    new String[]{String.valueOf(sentenceId), String.valueOf(wordId), difficulty});
                boolean dataExists = existsCursor.moveToFirst() && existsCursor.getInt(0) > 0;
                existsCursor.close();

                if (!dataExists) {
                    ContentValues cmValues = new ContentValues();
                    cmValues.put("sentence_id", sentenceId);
                    cmValues.put("word_id", wordId);
                    cmValues.put("difficulty", difficulty);
                    cmValues.put("hint", hint);
                    if (db.insert("character_matching", null, cmValues) != -1) {
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
            java.io.InputStream inputStream = context.getAssets().open("json/character_matching.json");
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