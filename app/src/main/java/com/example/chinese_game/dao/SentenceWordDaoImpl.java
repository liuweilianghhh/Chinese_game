package com.example.chinese_game.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.CharacterMatching;
import com.example.chinese_game.javabean.SentenceWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SentenceWordDaoImpl implements SentenceWordDao {
    private final MYsqliteopenhelper dbHelper;

    public SentenceWordDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public List<SentenceWord> getRandomWordsByPosTag(String posTag, int excludeWordId, int count) {
        if (posTag == null || count <= 0) return new ArrayList<>();

        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        String[] args = new String[]{posTag, String.valueOf(excludeWordId)};
        Cursor cursor = db.rawQuery(
            "SELECT id, sentence_id, word, pinyin, pos_tag, word_order " +
            "FROM sentence_words WHERE pos_tag = ? AND id != ?",
            args);

        List<SentenceWord> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(cursorToSentenceWord(cursor));
        }
        cursor.close();

        if (list.size() <= count) return list;
        Collections.shuffle(list);
        return list.subList(0, count);
    }

    /** 排除的 CTB 词性：感叹词(IJ)、标点(PU)、代词(PN)、数词量词(CD,M,OD)、语气词(SP)、助词(DEG,AS,DEV)、介词(P)、连词(CC,CS)、把/被(BA,LB,SB)，避免过于简单的题 */
    private static final String EXCLUDED_POS_FOR_GAME = "('IJ','PU','PN','CD','M','OD','SP','DEG','AS','DEV','P','CC','CS','BA','LB','SB')";
    private static final Pattern ONLY_DIGITS_PUNCT = Pattern.compile("^[\\d\\s.,，．、％%\\-]*$");

    @Override
    public List<CharacterMatching> getRandomSentenceWordsForGame(int count, String difficulty) {
        if (count <= 0) return new ArrayList<>();
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        String sql = "SELECT sw.id AS word_id, sw.sentence_id, sw.word, sw.pinyin, sw.pos_tag, s.sentence, s.difficulty " +
                "FROM sentence_words sw JOIN sentences s ON sw.sentence_id = s.id " +
                "WHERE sw.pos_tag NOT IN " + EXCLUDED_POS_FOR_GAME;
        List<String> argsList = new ArrayList<>();
        if (difficulty != null && !difficulty.isEmpty()) {
            sql += " AND sw.word_difficulty = ? ";
            argsList.add(difficulty);
        }
        sql += " ORDER BY RANDOM() LIMIT ?";
        argsList.add(String.valueOf(Math.max(count * 3, 30)));
        Cursor cursor = db.rawQuery(sql, argsList.toArray(new String[0]));
        List<CharacterMatching> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            CharacterMatching q = cursorToCharacterMatching(cursor);
            String w = q.getWord();
            if (w != null && !w.trim().isEmpty() && !ONLY_DIGITS_PUNCT.matcher(w.trim()).matches()) {
                list.add(q);
                if (list.size() >= count) break;
            }
        }
        cursor.close();
        return list;
    }

    @Override
    public void updateWordFrequencyAndDifficulty(Context context) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        if (db == null || !db.isOpen()) return;
        Cursor cursor = null;
        try {
            String sql = "SELECT word, COUNT(*) AS cnt FROM sentence_words WHERE pos_tag NOT IN " + EXCLUDED_POS_FOR_GAME + " GROUP BY word";
            cursor = db.rawQuery(sql, null);
            List<Map.Entry<String, Integer>> wordFreqs = new ArrayList<>();
            while (cursor.moveToNext()) {
                String word = cursor.getString(0);
                int cnt = cursor.getInt(1);
                if (word != null) wordFreqs.add(new HashMap.SimpleEntry<>(word, cnt));
            }
            cursor.close();
            cursor = null;
            if (wordFreqs.isEmpty()) return;
            Collections.sort(wordFreqs, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
            int n = wordFreqs.size();
            int easyEnd = Math.max(1, n / 3);
            int mediumEnd = Math.max(easyEnd + 1, 2 * n / 3);
            Map<String, Integer> wordToFreq = new HashMap<>();
            Map<String, String> wordToDiff = new HashMap<>();
            for (int i = 0; i < n; i++) {
                String word = wordFreqs.get(i).getKey();
                int freq = wordFreqs.get(i).getValue();
                wordToFreq.put(word, freq);
                String diff = (i < easyEnd) ? "EASY" : (i < mediumEnd) ? "MEDIUM" : "HARD";
                wordToDiff.put(word, diff);
            }
            for (Map.Entry<String, Integer> e : wordToFreq.entrySet()) {
                String word = e.getKey();
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("word_frequency", e.getValue());
                cv.put("word_difficulty", wordToDiff.get(word));
                db.update("sentence_words", cv, "word = ?", new String[]{word});
            }
            android.content.ContentValues cvZero = new android.content.ContentValues();
            cvZero.put("word_difficulty", "HARD");
            db.update("sentence_words", cvZero, "word_frequency = 0 OR word_frequency IS NULL", null);
            android.util.Log.i("SentenceWordDao", "updateWordFrequencyAndDifficulty: " + n + " words, freq percentiles -> EASY/MEDIUM/HARD");
        } catch (Exception e) {
            android.util.Log.e("SentenceWordDao", "updateWordFrequencyAndDifficulty failed", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }
    }

    private static CharacterMatching cursorToCharacterMatching(Cursor c) {
        CharacterMatching q = new CharacterMatching();
        q.setId(c.getInt(c.getColumnIndexOrThrow("word_id")));
        q.setWordId(c.getInt(c.getColumnIndexOrThrow("word_id")));
        q.setSentenceId(c.getInt(c.getColumnIndexOrThrow("sentence_id")));
        q.setWord(c.getString(c.getColumnIndexOrThrow("word")));
        int pinyinIdx = c.getColumnIndex("pinyin");
        q.setPinyin(pinyinIdx >= 0 ? c.getString(pinyinIdx) : null);
        q.setPosTag(c.getString(c.getColumnIndexOrThrow("pos_tag")));
        q.setSentence(c.getString(c.getColumnIndexOrThrow("sentence")));
        int diffIdx = c.getColumnIndex("difficulty");
        q.setDifficulty(diffIdx >= 0 ? c.getString(diffIdx) : "EASY");
        q.setHint("");
        return q;
    }

    private static SentenceWord cursorToSentenceWord(Cursor c) {
        SentenceWord w = new SentenceWord();
        w.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        w.setSentenceId(c.getInt(c.getColumnIndexOrThrow("sentence_id")));
        w.setWord(c.getString(c.getColumnIndexOrThrow("word")));
        int pinyinIdx = c.getColumnIndex("pinyin");
        w.setPinyin(pinyinIdx >= 0 ? c.getString(pinyinIdx) : null);
        w.setPosTag(c.getString(c.getColumnIndexOrThrow("pos_tag")));
        w.setWordOrder(c.getInt(c.getColumnIndexOrThrow("word_order")));
        return w;
    }
}
