package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.GameQuestionDetail;
import java.util.List;

/**
 * 游戏题目详情表 DAO，用于记录每局游戏中每道题的答题结果。
 */
public interface GameQuestionDetailDao {
    /**
     * 插入一条题目详情
     * @param detail 题目详情
     * @return 插入后的行 id，失败返回 -1
     */
    long insert(GameQuestionDetail detail);

    /**
     * 批量插入本局每道题的结果（按 question_order 顺序）
     * @param details 题目详情列表
     */
    void insertBatch(List<GameQuestionDetail> details);

    /**
     * 按本局 id 与题序更新一道题的答题结果（避免每题多插一行导致表无限增长）
     * @return true 若更新了至少一行
     */
    boolean updateByGameScoreAndOrder(int gameScoreId, int questionOrder, String userAnswer, boolean correct, long timeSpent, int questionScore);
}
