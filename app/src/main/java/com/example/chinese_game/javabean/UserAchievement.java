package com.example.chinese_game.javabean;

import java.util.Date;

public class UserAchievement {
    private int id;
    private int userId;
    private int achievementId;
    private Date unlockedDate;
    private int progressValue; // 当前进度值（对于需要累积的成就）

    public UserAchievement() {
    }

    public UserAchievement(int userId, int achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.unlockedDate = new Date();
        this.progressValue = 0;
    }

    public UserAchievement(int userId, int achievementId, Date unlockedDate) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.unlockedDate = unlockedDate;
        this.progressValue = 0;
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

    public int getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    public Date getUnlockedDate() {
        return unlockedDate;
    }

    public void setUnlockedDate(Date unlockedDate) {
        this.unlockedDate = unlockedDate;
    }

    public int getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(int progressValue) {
        this.progressValue = progressValue;
    }

    @Override
    public String toString() {
        return "UserAchievement{" +
                "id=" + id +
                ", userId=" + userId +
                ", achievementId=" + achievementId +
                ", unlockedDate=" + unlockedDate +
                ", progressValue=" + progressValue +
                '}';
    }
}