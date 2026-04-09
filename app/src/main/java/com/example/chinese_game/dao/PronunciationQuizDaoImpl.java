package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.PronunciationQuiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PronunciationQuizDaoImpl implements PronunciationQuizDao {
    private MYsqliteopenhelper dbHelper;

    public PronunciationQuizDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long save(PronunciationQuiz data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("word_id", data.getWordId());
        cv.put("audio_path", data.getAudioPath());
        cv.put("difficulty", data.getDifficulty());
        cv.put("hint", data.getHint());

        long result = db.insert("pronunciation_quiz", null, cv);
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result;
    }

    @Override
    public PronunciationQuiz findById(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT pq.id, pq.sentence_id, pq.word_id, pq.audio_path, pq.difficulty, pq.hint, " +
            "s.sentence AS sentence_text, sw.word AS word_text, sw.pinyin AS word_pinyin " +
            "FROM pronunciation_quiz pq " +
            "JOIN sentences s ON pq.sentence_id = s.id " +
            "JOIN sentence_words sw ON pq.word_id = sw.id " +
            "WHERE pq.id = ?",
            new String[]{String.valueOf(dataId)});

        PronunciationQuiz data = null;
        if (cursor.moveToFirst()) {
            data = cursorToPronunciationQuiz(cursor);
        }
        cursor.close();
        return data;
    }

    @Override
    public List<PronunciationQuiz> findByDifficulty(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT pq.id, pq.sentence_id, pq.word_id, pq.audio_path, pq.difficulty, pq.hint, " +
            "s.sentence AS sentence_text, sw.word AS word_text, sw.pinyin AS word_pinyin " +
            "FROM pronunciation_quiz pq " +
            "JOIN sentences s ON pq.sentence_id = s.id " +
            "JOIN sentence_words sw ON pq.word_id = sw.id " +
            "WHERE pq.difficulty = ? " +
            "ORDER BY pq.id ASC",
            new String[]{difficulty});

        List<PronunciationQuiz> dataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            dataList.add(cursorToPronunciationQuiz(cursor));
        }
        cursor.close();
        return dataList;
    }

    @Override
    public List<PronunciationQuiz> findActiveData() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT pq.id, pq.sentence_id, pq.word_id, pq.audio_path, pq.difficulty, pq.hint, " +
            "s.sentence AS sentence_text, sw.word AS word_text, sw.pinyin AS word_pinyin " +
            "FROM pronunciation_quiz pq " +
            "JOIN sentences s ON pq.sentence_id = s.id " +
            "JOIN sentence_words sw ON pq.word_id = sw.id " +
            "ORDER BY pq.difficulty ASC, pq.id ASC",
            null);

        List<PronunciationQuiz> dataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            dataList.add(cursorToPronunciationQuiz(cursor));
        }
        cursor.close();
        return dataList;
    }

    @Override
    public List<PronunciationQuiz> getRandomData(String difficulty, int count) {
        List<PronunciationQuiz> allData = findByDifficulty(difficulty);

        // If not enough data, return all available
        if (allData.size() <= count) {
            return allData;
        }

        // Randomly select specified number of items
        Collections.shuffle(allData);
        return allData.subList(0, count);
    }

    @Override
    public boolean update(PronunciationQuiz data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("word_id", data.getWordId());
        cv.put("audio_path", data.getAudioPath());
        cv.put("difficulty", data.getDifficulty());
        cv.put("hint", data.getHint());

        int result = db.update("pronunciation_quiz", cv, "id=?",
                new String[]{String.valueOf(data.getId())});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public boolean setActive(int dataId, boolean active) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();

        int result = db.update("pronunciation_quiz", cv, "id=?",
                new String[]{String.valueOf(dataId)});
        return result > 0;
    }

    @Override
    public boolean delete(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("pronunciation_quiz", "id=?", new String[]{String.valueOf(dataId)});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public PronunciationQuizStatistics getStatistics() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        PronunciationQuizStatistics stats = new PronunciationQuizStatistics();

        // 获取总数
        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM pronunciation_quiz", null);
        if (totalCursor.moveToFirst()) {
            stats.totalQuestions = totalCursor.getInt(0);
        }
        totalCursor.close();

        // Get active questions
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM pronunciation_quiz", null);
        if (countCursor.moveToFirst()) {
            stats.activeQuestions = countCursor.getInt(0);
        }
        countCursor.close();

        // Group by difficulty
        Cursor difficultyCursor = db.rawQuery(
            "SELECT difficulty, COUNT(*) FROM pronunciation_quiz GROUP BY difficulty", null);
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
    public int batchInsert(List<PronunciationQuiz> dataList) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int successCount = 0;

        db.beginTransaction();
        try {
            for (PronunciationQuiz data : dataList) {
                ContentValues cv = new ContentValues();
                cv.put("sentence_id", data.getSentenceId());
                cv.put("word_id", data.getWordId());
                cv.put("audio_path", data.getAudioPath());
                cv.put("difficulty", data.getDifficulty());
                cv.put("hint", data.getHint());

                long result = db.insert("pronunciation_quiz", null, cv);
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
    public long insertIfNotExists(PronunciationQuiz data) {
        if (data == null) return -1;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor c = db.query("pronunciation_quiz", new String[]{"id"},
                "sentence_id = ? AND word_id = ?",
                new String[]{String.valueOf(data.getSentenceId()), String.valueOf(data.getWordId())},
                null, null, null, "1");
        if (c.moveToFirst()) {
            long existingId = c.getLong(0);
            c.close();
            return existingId;
        }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("sentence_id", data.getSentenceId());
        cv.put("word_id", data.getWordId());
        cv.put("audio_path", data.getAudioPath());
        cv.put("difficulty", data.getDifficulty());
        cv.put("hint", data.getHint());
        return db.insert("pronunciation_quiz", null, cv);
    }

    @Override
    public boolean hasData(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM pronunciation_quiz WHERE difficulty=?",
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
            query = "SELECT COUNT(*) FROM pronunciation_quiz WHERE difficulty=?";
            args = new String[]{difficulty};
        } else {
            query = "SELECT COUNT(*) FROM pronunciation_quiz";
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

    private PronunciationQuiz cursorToPronunciationQuiz(Cursor cursor) {
        PronunciationQuiz data = new PronunciationQuiz();
        data.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        data.setSentenceId(cursor.getInt(cursor.getColumnIndexOrThrow("sentence_id")));
        data.setWordId(cursor.getInt(cursor.getColumnIndexOrThrow("word_id")));
        data.setAudioPath(cursor.getString(cursor.getColumnIndexOrThrow("audio_path")));
        data.setDifficulty(cursor.getString(cursor.getColumnIndexOrThrow("difficulty")));
        data.setHint(cursor.getString(cursor.getColumnIndexOrThrow("hint")));
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

                String word = jsonData.optString("word", "");
                String pinyin = jsonData.optString("pinyin", "");
                String audioPath = jsonData.optString("audio_path", "");
                String difficulty = jsonData.optString("difficulty", "EASY");
                String hint = jsonData.optString("hint", null);

                // 1. sentences 中确保有对应记录（将单词视为一句）
                int sentenceId = -1;
                Cursor sentenceCursor = db.rawQuery(
                    "SELECT id FROM sentences WHERE sentence = ?",
                    new String[]{word});
                if (sentenceCursor.moveToFirst()) {
                    sentenceId = sentenceCursor.getInt(0);
                }
                sentenceCursor.close();

                if (sentenceId == -1) {
                    ContentValues sentenceValues = new ContentValues();
                    sentenceValues.put("sentence", word);
                    sentenceValues.put("pinyin", pinyin);
                    sentenceValues.put("difficulty", difficulty);
                    sentenceValues.put("category", (String) null);
                    sentenceValues.put("word_count", 1);
                    long newSentenceId = db.insert("sentences", null, sentenceValues);
                    if (newSentenceId == -1) {
                        continue;
                    }
                    sentenceId = (int) newSentenceId;
                }

                // 2. sentence_words 中确保有对应记录
                int wordId = -1;
                Cursor wordCursor = db.rawQuery(
                    "SELECT id FROM sentence_words WHERE sentence_id = ? AND word_order = 1",
                    new String[]{String.valueOf(sentenceId)});
                if (wordCursor.moveToFirst()) {
                    wordId = wordCursor.getInt(0);
                }
                wordCursor.close();

                if (wordId == -1) {
                    ContentValues wordValues = new ContentValues();
                    wordValues.put("sentence_id", sentenceId);
                    wordValues.put("word", word);
                    wordValues.put("pinyin", pinyin);
                    wordValues.put("pos_tag", "X");
                    wordValues.put("word_order", 1);
                    wordValues.put("word_position", 1);
                    wordValues.put("word_difficulty", difficulty);
                    wordValues.put("word_frequency", 0);
                    long newWordId = db.insert("sentence_words", null, wordValues);
                    if (newWordId == -1) {
                        continue;
                    }
                    wordId = (int) newWordId;
                }

                // 3. 检查 pronunciation_quiz 是否已有记录
                boolean dataExists = false;
                Cursor existsCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM pronunciation_quiz WHERE sentence_id = ? AND word_id = ? AND difficulty = ?",
                    new String[]{String.valueOf(sentenceId), String.valueOf(wordId), difficulty});
                if (existsCursor.moveToFirst()) {
                    dataExists = existsCursor.getInt(0) > 0;
                }
                existsCursor.close();

                if (!dataExists) {
                    ContentValues pqValues = new ContentValues();
                    pqValues.put("sentence_id", sentenceId);
                    pqValues.put("word_id", wordId);
                    pqValues.put("audio_path", audioPath);
                    pqValues.put("difficulty", difficulty);
                    pqValues.put("hint", hint);
                    long result = db.insert("pronunciation_quiz", null, pqValues);
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
            java.io.InputStream inputStream = context.getAssets().open("json/pronunciation_quiz.json");
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