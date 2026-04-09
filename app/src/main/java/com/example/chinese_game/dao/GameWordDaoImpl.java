package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.CharacterMatching;
import com.example.chinese_game.javabean.SentenceWord;
import com.example.chinese_game.utils.PinyinUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class GameWordDaoImpl implements GameWordDao {
    private final MYsqliteopenhelper dbHelper;
    private static final Pattern FULL_CHINESE_WORD_PATTERN = Pattern.compile("^[\\u4e00-\\u9fff]+$");
    private static final Pattern HAS_CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    public GameWordDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public List<CharacterMatching> getRandomGameWordsForGame(int count, String difficulty) {
        List<CharacterMatching> list = new ArrayList<>();
        if (count <= 0) return list;

        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        StringBuilder sql = new StringBuilder(
                "SELECT id, word, pinyin, pos_tag, difficulty, hint FROM game_words"
        );
        List<String> args = new ArrayList<>();
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            String normalizedDifficulty = normalizeDifficulty(difficulty);
            sql.append(" WHERE difficulty = ?");
            args.add(normalizedDifficulty);
            if ("EASY".equals(normalizedDifficulty)) {
                sql.append(" AND LENGTH(word) BETWEEN 1 AND 2");
            } else {
                sql.append(" AND LENGTH(word) = 2");
            }
        } else {
            sql.append(" WHERE ((difficulty = 'EASY' AND LENGTH(word) BETWEEN 1 AND 2) ")
               .append("OR (difficulty IN ('MEDIUM','HARD') AND LENGTH(word) = 2))");
        }
        sql.append(" ORDER BY RANDOM() LIMIT ?");
        args.add(String.valueOf(count));

        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (cursor.moveToNext()) {
                list.add(cursorToCharacterMatching(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @Override
    public List<SentenceWord> getRandomWordsByPosTag(String posTag, int excludeWordId, int count) {
        List<SentenceWord> list = new ArrayList<>();
        if (posTag == null || posTag.trim().isEmpty() || count <= 0) return list;

        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, word, pinyin, pos_tag FROM game_words " +
                        "WHERE pos_tag = ? AND id != ? " +
                        "AND ((difficulty = 'EASY' AND LENGTH(word) BETWEEN 1 AND 2) " +
                        "OR (difficulty IN ('MEDIUM','HARD') AND LENGTH(word) = 2)) " +
                        "ORDER BY RANDOM() LIMIT ?",
                new String[]{posTag, String.valueOf(excludeWordId), String.valueOf(count)}
        );
        try {
            while (cursor.moveToNext()) {
                list.add(cursorToSentenceWord(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @Override
    public int importFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) return -1;

        int importedCount = 0;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        db.beginTransaction();
        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String difficulty = normalizeDifficulty(obj.optString("difficulty", "EASY"));
                String word = normalizeWordByDifficulty(obj.optString("word", ""), difficulty);
                if (word == null || word.isEmpty()) continue;

                String pinyin = PinyinUtils.wordToPinyin(word);
                if (pinyin == null || pinyin.trim().isEmpty()) {
                    pinyin = obj.optString("pinyin", null);
                }
                String posTag = obj.optString("pos_tag", "X");
                if (posTag == null || posTag.trim().isEmpty()) posTag = "X";
                String hint = normalizeHint(obj.optString("hint", ""), word, difficulty);

                Cursor existsCursor = db.rawQuery(
                        "SELECT id FROM game_words WHERE word = ? AND difficulty = ? LIMIT 1",
                        new String[]{word, difficulty}
                );
                boolean exists;
                try {
                    exists = existsCursor.moveToFirst();
                } finally {
                    existsCursor.close();
                }
                if (exists) continue;

                ContentValues cv = new ContentValues();
                cv.put("word", word);
                cv.put("pinyin", (pinyin == null || pinyin.trim().isEmpty()) ? null : pinyin.trim());
                cv.put("pos_tag", posTag.trim());
                cv.put("difficulty", difficulty);
                cv.put("hint", hint);

                long rowId = db.insert("game_words", null, cv);
                if (rowId != -1) importedCount++;
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
    public int importFromAssetsJson(Context context) {
        try {
            java.io.InputStream inputStream = context.getAssets().open("json/game_words.json");
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

    @Override
    public boolean hasData(String difficulty) {
        return getDataCount(difficulty) > 0;
    }

    @Override
    public int getDataCount(String difficulty) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        String sql;
        String[] args;
        if (difficulty == null || difficulty.trim().isEmpty()) {
            sql = "SELECT COUNT(*) FROM game_words " +
                    "WHERE ((difficulty = 'EASY' AND LENGTH(word) BETWEEN 1 AND 2) " +
                    "OR (difficulty IN ('MEDIUM','HARD') AND LENGTH(word) = 2))";
            args = null;
        } else {
            String normalizedDifficulty = normalizeDifficulty(difficulty);
            if ("EASY".equals(normalizedDifficulty)) {
                sql = "SELECT COUNT(*) FROM game_words WHERE difficulty = ? AND LENGTH(word) BETWEEN 1 AND 2";
            } else {
                sql = "SELECT COUNT(*) FROM game_words WHERE difficulty = ? AND LENGTH(word) = 2";
            }
            args = new String[]{normalizedDifficulty};
        }
        Cursor cursor = db.rawQuery(sql, args);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    private static CharacterMatching cursorToCharacterMatching(Cursor c) {
        CharacterMatching q = new CharacterMatching();
        int id = c.getInt(c.getColumnIndexOrThrow("id"));
        q.setId(id);
        q.setWordId(id);
        q.setSentenceId(0);
        q.setWord(c.getString(c.getColumnIndexOrThrow("word")));
        q.setPinyin(c.getString(c.getColumnIndexOrThrow("pinyin")));
        q.setPosTag(c.getString(c.getColumnIndexOrThrow("pos_tag")));
        q.setDifficulty(c.getString(c.getColumnIndexOrThrow("difficulty")));
        q.setHint(c.getString(c.getColumnIndexOrThrow("hint")));
        q.setSentence(null);
        return q;
    }

    private static SentenceWord cursorToSentenceWord(Cursor c) {
        SentenceWord w = new SentenceWord();
        w.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        w.setSentenceId(0);
        w.setWord(c.getString(c.getColumnIndexOrThrow("word")));
        int pinyinIdx = c.getColumnIndex("pinyin");
        w.setPinyin(pinyinIdx >= 0 ? c.getString(pinyinIdx) : null);
        w.setPosTag(c.getString(c.getColumnIndexOrThrow("pos_tag")));
        w.setWordOrder(1);
        return w;
    }

    private static String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return "EASY";
        String v = difficulty.trim().toUpperCase(Locale.ROOT);
        switch (v) {
            case "EASY":
            case "MEDIUM":
            case "HARD":
                return v;
            default:
                return "EASY";
        }
    }

    /**
     * EASY: allow 1-char or 2-char words.
     * MEDIUM/HARD: must be exactly 2-char phrases.
     * No truncation is allowed: invalid length words are rejected.
     */
    private static String normalizeWordByDifficulty(String rawWord, String difficulty) {
        if (rawWord == null) return null;
        String word = rawWord.trim();
        if (word.isEmpty()) return null;
        if (!FULL_CHINESE_WORD_PATTERN.matcher(word).matches()) return null;

        if ("EASY".equals(difficulty)) {
            return (word.length() == 1 || word.length() == 2) ? word : null;
        }

        return word.length() == 2 ? word : null;
    }

    private static String normalizeHint(String rawHint, String word, String difficulty) {
        if (rawHint == null) return buildEnglishHint(word, difficulty);
        String hint = rawHint.trim();
        if (hint.isEmpty()) return buildEnglishHint(word, difficulty);
        // Keep hint English-only for game display consistency.
        if (HAS_CHINESE_PATTERN.matcher(hint).find()) return buildEnglishHint(word, difficulty);
        return hint;
    }

    private static String buildEnglishHint(String word, String difficulty) {
        switch (difficulty) {
            case "HARD":
                return "An advanced two-character Chinese phrase.";
            case "MEDIUM":
                return "An intermediate two-character Chinese phrase.";
            default:
                if (word.length() == 1) {
                    return "A basic single Chinese character.";
                }
                return "A basic Chinese word or short phrase.";
        }
    }
}
