package com.example.chinese_game.javabean;

/**
 * 游戏题目详情表 game_question_details 对应实体。
 */
public class GameQuestionDetail {
    private int id;
    private int gameScoreId;
    private int questionOrder;
    private int questionId;       // 题目ID（如 character_matching.id）
    private String userAnswer;
    private boolean correct;      // is_correct: 0/1
    private long timeSpent;       // 毫秒
    private int questionScore;
    private int maxScore;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGameScoreId() { return gameScoreId; }
    public void setGameScoreId(int gameScoreId) { this.gameScoreId = gameScoreId; }
    public int getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(int questionOrder) { this.questionOrder = questionOrder; }
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public long getTimeSpent() { return timeSpent; }
    public void setTimeSpent(long timeSpent) { this.timeSpent = timeSpent; }
    public int getQuestionScore() { return questionScore; }
    public void setQuestionScore(int questionScore) { this.questionScore = questionScore; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
}
