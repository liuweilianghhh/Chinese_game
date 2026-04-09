package com.example.chinese_game.javabean;

public class Achievement {
    public enum AchievementType {
        MILESTONE("Milestone Badge"), // 里程碑徽章
        GAME_SPECIFIC("Game-Specific Badge"); // 游戏特定徽章

        private final String displayName;

        AchievementType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AchievementCategory {
        GENERAL("General"),
        CHARACTER_MATCHING("Character Matching"),
        PRONUNCIATION_QUIZ("Pronunciation Quiz"),
        WORD_PUZZLE("Word Puzzle"),
        LOGIN_STREAK("Login Streak");

        private final String displayName;

        AchievementCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private int id;
    private String name;
    private String description;
    private String iconPath;
    private AchievementType type;
    private AchievementCategory category;
    private int requiredValue; // 解锁所需的值（如游戏次数、连续正确次数等）
    private int rewardPoints; // 奖励点数

    public Achievement() {
    }

    public Achievement(String name, String description, AchievementType type,
                      AchievementCategory category, int requiredValue, int rewardPoints) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.category = category;
        this.requiredValue = requiredValue;
        this.rewardPoints = rewardPoints;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public AchievementType getType() {
        return type;
    }

    public void setType(AchievementType type) {
        this.type = type;
    }

    public AchievementCategory getCategory() {
        return category;
    }

    public void setCategory(AchievementCategory category) {
        this.category = category;
    }

    public int getRequiredValue() {
        return requiredValue;
    }

    public void setRequiredValue(int requiredValue) {
        this.requiredValue = requiredValue;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }


    @Override
    public String toString() {
        return "Achievement{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", category=" + category +
                ", requiredValue=" + requiredValue +
                ", rewardPoints=" + rewardPoints +
                '}';
    }
}