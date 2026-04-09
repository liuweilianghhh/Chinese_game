package com.example.chinese_game.javabean;

import java.util.Date;

public class User {
    private int id;
    private String name;
    private String password;
    private String email;
    private Date registrationDate;
    private Date lastLoginDate;
    private int loginStreak; // 连续登录天数
    private int totalGamesPlayed; // 总游戏次数
    private int totalScore; // 总分数
    private String avatarPath; // 头像路径（可选）

    public User() {
    }

    public User(String name, String password) {
        this.name = name;
        this.password = password;
        this.registrationDate = new Date();
        this.lastLoginDate = new Date();
        this.loginStreak = 1;
        this.totalGamesPlayed = 0;
        this.totalScore = 0;
    }

    public User(String name, String password, String email) {
        this(name, password);
        this.email = email;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public int getLoginStreak() {
        return loginStreak;
    }

    public void setLoginStreak(int loginStreak) {
        this.loginStreak = loginStreak;
    }

    public int getTotalGamesPlayed() {
        return totalGamesPlayed;
    }

    public void setTotalGamesPlayed(int totalGamesPlayed) {
        this.totalGamesPlayed = totalGamesPlayed;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", registrationDate=" + registrationDate +
                ", lastLoginDate=" + lastLoginDate +
                ", loginStreak=" + loginStreak +
                ", totalGamesPlayed=" + totalGamesPlayed +
                ", totalScore=" + totalScore +
                '}';
    }
}

