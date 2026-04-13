package com.example.chinese_game.dao;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.CharacterMatching;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CharacterMatchingDaoImpl implements CharacterMatchingDao {
    private final MYsqliteopenhelper dbHelper;

    public CharacterMatchingDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long save(CharacterMatching data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        String difficulty = resolveDifficultyForWord(db, data.getWordId());
        if (difficulty == null) return -1;

        ContentValues cv = new ContentValues();
        cv.put("word_id", data.getWordId());
        cv.put("difficulty", difficulty);
        cv.put("hint", data.getHint());
        return db.insert("character_matching", null, cv);
    }

    @Override
    public long insertIfNotExists(CharacterMatching data) {
        if (data == null) return -1;

        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        String difficulty = resolveDifficultyForWord(db, data.getWordId());
        if (difficulty == null) return -1;

        Cursor cursor = db.query(
                "character_matching",
                new String[]{"id"},
                "word_id = ? AND difficulty = ?",
                new String[]{String.valueOf(data.getWordId()), difficulty},
                null,
                null,
                null,
                "1"
        );
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }

        ContentValues cv = new ContentValues();
        cv.put("word_id", data.getWordId());
        cv.put("difficulty", difficulty);
        cv.put("hint", data.getHint());
        return db.insert("character_matching", null, cv);
    }

    @Override
    public CharacterMatching findById(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT cm.id, cm.word_id, cm.difficulty, cm.hint, " +
                        "gw.word AS word_text, gw.pinyin AS word_pinyin, gw.pos_tag AS pos_tag " +
                        "FROM character_matching cm " +
                        "JOIN game_words gw ON cm.word_id = gw.id " +
                        "WHERE cm.id = ?",
                new String[]{String.valueOf(dataId)}
        );
        try {
            if (cursor.moveToFirst()) {
                return cursorToCharacterMatching(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    @Override
    public List<CharacterMatching> findByDifficulty(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT cm.id, cm.word_id, cm.difficulty, cm.hint, " +
                        "gw.word AS word_text, gw.pinyin AS word_pinyin, gw.pos_tag AS pos_tag " +
                        "FROM character_matching cm " +
                        "JOIN game_words gw ON cm.word_id = gw.id " +
                        "WHERE cm.difficulty = ? " +
                        "ORDER BY cm.id ASC",
                new String[]{normalizeDifficulty(difficulty)}
        );

        List<CharacterMatching> dataList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                dataList.add(cursorToCharacterMatching(cursor));
            }
        } finally {
            cursor.close();
        }
        return dataList;
    }

    @Override
    public List<CharacterMatching> findAll() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT cm.id, cm.word_id, cm.difficulty, cm.hint, " +
                        "gw.word AS word_text, gw.pinyin AS word_pinyin, gw.pos_tag AS pos_tag " +
                        "FROM character_matching cm " +
                        "JOIN game_words gw ON cm.word_id = gw.id " +
                        "ORDER BY cm.difficulty ASC, cm.id ASC",
                null
        );

        List<CharacterMatching> dataList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                dataList.add(cursorToCharacterMatching(cursor));
            }
        } finally {
            cursor.close();
        }
        return dataList;
    }

    @Override
    public List<CharacterMatching> getRandomData(String difficulty, int count) {
        List<CharacterMatching> allData = findByDifficulty(difficulty);
        if (allData.size() <= count) {
            return allData;
        }
        Collections.shuffle(allData);
        return allData.subList(0, count);
    }

    @Override
    public boolean update(CharacterMatching data) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        String difficulty = resolveDifficultyForWord(db, data.getWordId());
        if (difficulty == null) return false;

        ContentValues cv = new ContentValues();
        cv.put("word_id", data.getWordId());
        cv.put("difficulty", difficulty);
        cv.put("hint", data.getHint());

        int result = db.update(
                "character_matching",
                cv,
                "id=?",
                new String[]{String.valueOf(data.getId())}
        );
        return result > 0;
    }

    @Override
    public boolean delete(int dataId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("character_matching", "id=?", new String[]{String.valueOf(dataId)});
        return result > 0;
    }

    @Override
    public CharacterMatchingStatistics getStatistics() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        CharacterMatchingStatistics stats = new CharacterMatchingStatistics();

        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM character_matching", null);
        try {
            if (totalCursor.moveToFirst()) {
                stats.totalQuestions = totalCursor.getInt(0);
            }
        } finally {
            totalCursor.close();
        }

        Cursor activeCursor = db.rawQuery("SELECT COUNT(*) FROM character_matching", null);
        try {
            if (activeCursor.moveToFirst()) {
                stats.activeQuestions = activeCursor.getInt(0);
            }
        } finally {
            activeCursor.close();
        }

        Cursor difficultyCursor = db.rawQuery(
                "SELECT difficulty, COUNT(*) FROM character_matching GROUP BY difficulty",
                null
        );
        try {
            while (difficultyCursor.moveToNext()) {
                String difficulty = difficultyCursor.getString(0);
                int count = difficultyCursor.getInt(1);
                switch (difficulty) {
                    case "EASY":
                        stats.easyQuestions = count;
                        break;
                    case "MEDIUM":
                        stats.mediumQuestions = count;
                        break;
                    case "HARD":
                        stats.hardQuestions = count;
                        break;
                    default:
                        break;
                }
            }
        } finally {
            difficultyCursor.close();
        }

        return stats;
    }

    @Override
    public int batchInsert(List<CharacterMatching> dataList) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int successCount = 0;

        db.beginTransaction();
        try {
            for (CharacterMatching data : dataList) {
                String difficulty = resolveDifficultyForWord(db, data.getWordId());
                if (difficulty == null) continue;

                ContentValues cv = new ContentValues();
                cv.put("word_id", data.getWordId());
                cv.put("difficulty", difficulty);
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
                new String[]{normalizeDifficulty(difficulty)}
        );
        try {
            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        } finally {
            cursor.close();
        }
    }

    @Override
    public int getDataCount(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        String query;
        String[] args;
        if (difficulty != null) {
            query = "SELECT COUNT(*) FROM character_matching WHERE difficulty=?";
            args = new String[]{normalizeDifficulty(difficulty)};
        } else {
            query = "SELECT COUNT(*) FROM character_matching";
            args = null;
        }

        Cursor cursor = db.rawQuery(query, args);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    @Override
    public int importFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return -1;
        }

        int importedCount = 0;
        boolean inTransaction = false;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            db.beginTransaction();
            inTransaction = true;

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                String hint = jsonData.optString("hint", null);

                int wordId = jsonData.optInt("word_id", -1);
                if (wordId <= 0) {
                    String word = jsonData.optString("word", jsonData.optString("character", "")).trim();
                    if (word.isEmpty()) continue;

                    String difficulty = normalizeDifficulty(jsonData.optString("difficulty", "EASY"));
                    String pinyin = emptyToNull(jsonData.optString("pinyin", null));
                    String posTag = emptyToNull(jsonData.optString("pos_tag", "X"));
                    long insertedWordId = ensureGameWord(db, word, pinyin, posTag, difficulty, hint);
                    if (insertedWordId <= 0 || insertedWordId > Integer.MAX_VALUE) continue;
                    wordId = (int) insertedWordId;
                }

                String difficulty = resolveDifficultyForWord(db, wordId);
                if (difficulty == null) continue;

                Cursor existsCursor = db.rawQuery(
                        "SELECT COUNT(*) FROM character_matching WHERE word_id = ? AND difficulty = ?",
                        new String[]{String.valueOf(wordId), difficulty}
                );
                boolean exists;
                try {
                    exists = existsCursor.moveToFirst() && existsCursor.getInt(0) > 0;
                } finally {
                    existsCursor.close();
                }

                if (!exists) {
                    ContentValues values = new ContentValues();
                    values.put("word_id", wordId);
                    values.put("difficulty", difficulty);
                    values.put("hint", hint);
                    if (db.insert("character_matching", null, values) != -1) {
                        importedCount++;
                    }
                }
            }

            db.setTransactionSuccessful();
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (inTransaction) {
                db.endTransaction();
            }
        }

        return importedCount;
    }

    @Override
    public int importFromAssetsJson(Context context) {
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

    private CharacterMatching cursorToCharacterMatching(Cursor cursor) {
        CharacterMatching data = new CharacterMatching();
        data.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        data.setSentenceId(0);
        data.setSentence(null);
        data.setWordId(cursor.getInt(cursor.getColumnIndexOrThrow("word_id")));
        data.setDifficulty(cursor.getString(cursor.getColumnIndexOrThrow("difficulty")));
        data.setHint(cursor.getString(cursor.getColumnIndexOrThrow("hint")));

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

    private long ensureGameWord(SQLiteDatabase db, String word, String pinyin, String posTag, String difficulty, String hint) {
        Cursor existsCursor = db.rawQuery(
                "SELECT id FROM game_words WHERE word = ? AND difficulty = ? LIMIT 1",
                new String[]{word, difficulty}
        );
        try {
            if (existsCursor.moveToFirst()) {
                return existsCursor.getLong(0);
            }
        } finally {
            existsCursor.close();
        }

        ContentValues values = new ContentValues();
        values.put("word", word);
        values.put("pinyin", pinyin);
        values.put("pos_tag", posTag == null ? "X" : posTag);
        values.put("difficulty", difficulty);
        values.put("hint", hint);
        return db.insert("game_words", null, values);
    }

    private String resolveDifficultyForWord(SQLiteDatabase db, int wordId) {
        Cursor cursor = db.rawQuery(
                "SELECT difficulty FROM game_words WHERE id = ? LIMIT 1",
                new String[]{String.valueOf(wordId)}
        );
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    private static String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return "EASY";
        String value = difficulty.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            case "EASY":
            case "MEDIUM":
            case "HARD":
                return value;
            default:
                return "EASY";
        }
    }

    private static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
