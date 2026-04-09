package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.GameQuestionDetail;

import java.util.List;

public class GameQuestionDetailDaoImpl implements GameQuestionDetailDao {
    private final MYsqliteopenhelper dbHelper;

    public GameQuestionDetailDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long insert(GameQuestionDetail detail) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("game_score_id", detail.getGameScoreId());
        cv.put("question_order", detail.getQuestionOrder());
        cv.put("question_id", detail.getQuestionId());
        cv.put("user_answer", detail.getUserAnswer());
        cv.put("is_correct", detail.isCorrect() ? 1 : 0);
        cv.put("time_spent", detail.getTimeSpent());
        cv.put("question_score", detail.getQuestionScore());
        cv.put("max_score", detail.getMaxScore());
        return db.insert("game_question_details", null, cv);
    }

    @Override
    public void insertBatch(List<GameQuestionDetail> details) {
        if (details == null || details.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        db.beginTransaction();
        try {
            for (GameQuestionDetail d : details) {
                ContentValues cv = new ContentValues();
                cv.put("game_score_id", d.getGameScoreId());
                cv.put("question_order", d.getQuestionOrder());
                cv.put("question_id", d.getQuestionId());
                cv.put("user_answer", d.getUserAnswer());
                cv.put("is_correct", d.isCorrect() ? 1 : 0);
                cv.put("time_spent", d.getTimeSpent());
                cv.put("question_score", d.getQuestionScore());
                cv.put("max_score", d.getMaxScore());
                db.insert("game_question_details", null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean updateByGameScoreAndOrder(int gameScoreId, int questionOrder, String userAnswer, boolean correct, long timeSpent, int questionScore) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_answer", userAnswer != null ? userAnswer : "");
        cv.put("is_correct", correct ? 1 : 0);
        cv.put("time_spent", timeSpent);
        cv.put("question_score", questionScore);
        int rows = db.update("game_question_details", cv, "game_score_id = ? AND question_order = ?",
                new String[]{String.valueOf(gameScoreId), String.valueOf(questionOrder)});
        return rows > 0;
    }
}
