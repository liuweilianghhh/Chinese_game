package com.example.chinese_game.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 从 assets 的 sentences.json / sentence_words.json 导入基础数据，
 * 供 character_matching、pronunciation_quiz、word_puzzle 使用。
 * 导入前会清空相关表，保证 sentence_id / word_id 与 JSON 中一致。
 */
public class SeedDataLoader {

    private static final String[] ALL_TABLE_NAMES = {
        "game_question_details", "game_scores", "user_achievements",
        "character_matching", "pronunciation_quiz", "word_puzzle", "game_words",
        "sentence_words", "sentences", "users", "achievements"
    };
    /**
     * 清空所有业务表并重置自增 id（sqlite_sequence），使下次插入从 1 开始。
     * 按外键依赖顺序删除，避免约束错误。
     */
    public static void resetAllTablesAndSequences(Context context) {
        MYsqliteopenhelper helper = new MYsqliteopenhelper(context);
        SQLiteDatabase db = helper.getPersistentDatabase();
        try {
            db.beginTransaction();
            for (String table : ALL_TABLE_NAMES) {
                db.execSQL("DELETE FROM " + table);
            }
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < ALL_TABLE_NAMES.length; i++) {
                if (i > 0) inClause.append(",");
                inClause.append("'").append(ALL_TABLE_NAMES[i]).append("'");
            }
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN (" + inClause + ")");
            db.setTransactionSuccessful();
            android.util.Log.i("SeedDataLoader", "resetAllTablesAndSequences completed");
        } catch (Exception e) {
            android.util.Log.e("SeedDataLoader", "resetAllTablesAndSequences failed", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 清空游戏依赖的句子与题目表（保持外键顺序），然后从 JSON 导入 sentences 和 sentence_words。
     * 应在导入 character_matching / pronunciation_quiz / word_puzzle 之前调用。
     */
    public static void loadSentencesAndWordsFromAssets(Context context) {
        MYsqliteopenhelper helper = new MYsqliteopenhelper(context);
        SQLiteDatabase db = helper.getPersistentDatabase();
        try {
            db.beginTransaction();
            // 先删子表再删父表（外键）
            db.execSQL("DELETE FROM character_matching");
            db.execSQL("DELETE FROM pronunciation_quiz");
            db.execSQL("DELETE FROM word_puzzle");
            db.execSQL("DELETE FROM sentence_words");
            db.execSQL("DELETE FROM sentences");

            loadSentencesFromAssets(context, db);
            loadSentenceWordsFromAssets(context, db);
            segmentUnsegmentedSentencesInDb(db);
            updateWordPositionsForSentenceWords(db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            android.util.Log.e("SeedDataLoader", "loadSentencesAndWordsFromAssets failed", e);
        } finally {
            db.endTransaction();
        }
    }
    /**
     * 检测 sentence_words 中未包含分词的句子，并用 HanLP 在数据库中自动分词写入 sentence_words，
     * 同时更新 sentences.word_count。
     */
    public static void segmentUnsegmentedSentencesInDb(SQLiteDatabase db) {
        if (db == null || !db.isOpen()) return;
        String sql = "SELECT s.id, s.sentence FROM sentences s " +
                "LEFT JOIN sentence_words sw ON s.id = sw.sentence_id " +
                "WHERE sw.sentence_id IS NULL";
        int segmentedCount = 0;
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                int sentenceId = c.getInt(c.getColumnIndexOrThrow("id"));
                String sentence = c.getString(c.getColumnIndexOrThrow("sentence"));
                if (sentence == null || sentence.trim().isEmpty()) continue;
                java.util.List<HanLPSegmenter.SegmentResult> results = HanLPSegmenter.segmentWithPos(sentence.trim());
                if (results.isEmpty()) continue;
                int wordPosition = 1; // 从 1 开始，表示词在句子中的起始字符位置
                for (int i = 0; i < results.size(); i++) {
                    HanLPSegmenter.SegmentResult r = results.get(i);
                    String word = r.getWord();
                    String pinyin = PinyinUtils.wordToPinyin(word);
                    ContentValues cv = new ContentValues();
                    cv.put("sentence_id", sentenceId);
                    cv.put("word", word);
                    cv.put("pinyin", pinyin != null && !pinyin.isEmpty() ? pinyin : null);
                    cv.put("pos_tag", (r.getPosTag() == null || r.getPosTag().isEmpty()) ? "X" : r.getPosTag());
                    cv.put("word_order", i + 1);
                    cv.put("word_position", wordPosition);
                    cv.put("word_difficulty", "EASY");
                    cv.put("word_frequency", 0);
                    db.insert("sentence_words", null, cv);
                    wordPosition += (word != null ? word.length() : 0);
                }
                ContentValues updateCv = new ContentValues();
                updateCv.put("word_count", results.size());
                db.update("sentences", updateCv, "id = ?", new String[]{String.valueOf(sentenceId)});
                segmentedCount++;
                android.util.Log.i("SeedDataLoader", "HanLP 分词并写入: sentence_id=" + sentenceId + ", \"" + sentence + "\" -> " + results.size() + " 词");
            }
            if (segmentedCount == 0) {
                android.util.Log.i("SeedDataLoader", "检测分词: 所有句子在 sentence_words 中均有分词记录");
            } else {
                android.util.Log.i("SeedDataLoader", "检测分词: 对 " + segmentedCount + " 个未分词句子进行了 HanLP 分词并写入 sentence_words");
            }
        } catch (Exception e) {
            android.util.Log.e("SeedDataLoader", "segmentUnsegmentedSentencesInDb failed", e);
        }
    }

    /**
     * 为 sentence_words 表中所有词重新计算 word_position（从 1 开始的起始字符位置）。
     * 在 reload 时对从 JSON 导入或 HanLP 新填入的词统一补全/校正。
     */
    public static void updateWordPositionsForSentenceWords(SQLiteDatabase db) {
        if (db == null || !db.isOpen()) return;
        try (Cursor sidCursor = db.rawQuery("SELECT DISTINCT sentence_id FROM sentence_words ORDER BY sentence_id", null)) {
            while (sidCursor.moveToNext()) {
                int sentenceId = sidCursor.getInt(0);
                String sentence = null;
                try (Cursor sCursor = db.query("sentences", new String[]{"sentence"}, "id = ?", new String[]{String.valueOf(sentenceId)}, null, null, null)) {
                    if (sCursor.moveToFirst()) sentence = sCursor.getString(0);
                }
                if (sentence == null) continue;
                int position = 1;
                try (Cursor wCursor = db.query("sentence_words", new String[]{"id", "word"}, "sentence_id = ?", new String[]{String.valueOf(sentenceId)}, null, null, "word_order ASC")) {
                    while (wCursor.moveToNext()) {
                        int rowId = wCursor.getInt(wCursor.getColumnIndexOrThrow("id"));
                        String word = wCursor.getString(wCursor.getColumnIndexOrThrow("word"));
                        ContentValues cv = new ContentValues();
                        cv.put("word_position", position);
                        db.update("sentence_words", cv, "id = ?", new String[]{String.valueOf(rowId)});
                        position += (word != null ? word.length() : 0);
                    }
                }
            }
            android.util.Log.i("SeedDataLoader", "updateWordPositionsForSentenceWords completed");
        } catch (Exception e) {
            android.util.Log.e("SeedDataLoader", "updateWordPositionsForSentenceWords failed", e);
        }
    }

    private static void loadSentencesFromAssets(Context context, SQLiteDatabase db) throws Exception {
        String json = readAssetFile(context, "json/sentences.json");
        if (json == null || json.trim().isEmpty()) return;
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String sentence = o.optString("sentence", "").trim();
            if (sentence.isEmpty()) continue; // 跳过空白句子，避免多出一行
            ContentValues cv = new ContentValues();
            cv.put("sentence", sentence);
            cv.put("pinyin", o.optString("pinyin", null));
            cv.put("difficulty", o.optString("difficulty", "EASY"));
            cv.put("category", o.optString("category", null));
            cv.put("word_count", o.optInt("word_count", 0));
            db.insert("sentences", null, cv);
        }
    }

    private static void loadSentenceWordsFromAssets(Context context, SQLiteDatabase db) throws Exception {
        String json = readAssetFile(context, "json/sentence_words.json");
        if (json == null || json.trim().isEmpty()) return;
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            ContentValues cv = new ContentValues();
            cv.put("sentence_id", o.optInt("sentence_id", 0));
            cv.put("word", o.optString("word", ""));
            cv.put("pinyin", o.optString("pinyin", null));
            cv.put("pos_tag", o.optString("pos_tag", "X"));
            cv.put("word_order", o.optInt("word_order", 1));
            cv.put("word_position", o.optInt("word_position", 0));
            cv.put("word_difficulty", o.optString("word_difficulty", "EASY"));
            cv.put("word_frequency", o.optInt("word_frequency", 0));
            db.insert("sentence_words", null, cv);
        }
    }

    private static String readAssetFile(Context context, String path) {
        try {
            java.io.InputStream is = context.getAssets().open(path);
            int size = is.available();
            byte[] buf = new byte[size];
            is.read(buf);
            is.close();
            return new String(buf, "UTF-8");
        } catch (Exception e) {
            android.util.Log.w("SeedDataLoader", "readAssetFile " + path + ": " + e.getMessage());
            return null;
        }
    }
}
