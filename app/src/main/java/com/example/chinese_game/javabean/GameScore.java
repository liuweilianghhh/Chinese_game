package com.example.chinese_game.javabean;

import java.util.Date;

public class GameScore {
    public enum GameType {
        CHARACTER_MATCHING("Character Matching"),
        PRONUNCIATION_QUIZ("Pronunciation Quiz"),
        WORD_PUZZLE("Word Puzzle");

        private final String displayName;

        GameType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Difficulty {
        EASY("Easy"),
        MEDIUM("Medium"),
        HARD("Hard");

        private final String displayName;

        Difficulty(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private int id;
    private int userId;
    private GameType gameType;
    private Difficulty difficulty;
    private int score;
    private int maxPossibleScore;
    private double accuracy; // 准确率（0.0-1.0）
    private long timeSpent; // 耗时（毫秒）
    private Date playDate;
    private boolean completed; // 是否完成游戏

    public GameScore() {
    }

    public GameScore(int userId, GameType gameType, Difficulty difficulty, int score) {
        this.userId = userId;
        this.gameType = gameType;
        this.difficulty = difficulty;
        this.score = score;
        this.playDate = new Date();
        this.completed = true;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxPossibleScore() {
        return maxPossibleScore;
    }

    public void setMaxPossibleScore(int maxPossibleScore) {
        this.maxPossibleScore = maxPossibleScore;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(long timeSpent) {
        this.timeSpent = timeSpent;
    }

    public Date getPlayDate() {
        return playDate;
    }

    public void setPlayDate(Date playDate) {
        this.playDate = playDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }


    @Override
    public String toString() {
        return "GameScore{" +
                "id=" + id +
                ", userId=" + userId +
                ", gameType=" + gameType +
                ", difficulty=" + difficulty +
                ", score=" + score +
                ", accuracy=" + accuracy +
                ", timeSpent=" + timeSpent +
                ", playDate=" + playDate +
                ", completed=" + completed +
                '}';
    }
}