package com.example.chinese_game.javabean;

public class GameData {
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
    private GameType gameType;
    private Difficulty difficulty;
    private String questionData; // JSON格式存储题目数据
    private String correctAnswer; // 正确答案
    private String hint; // 提示信息
    private boolean isActive; // 是否启用
    private int orderIndex; // 题目顺序

    public GameData() {
    }

    public GameData(GameType gameType, Difficulty difficulty, String questionData, String correctAnswer) {
        this.gameType = gameType;
        this.difficulty = difficulty;
        this.questionData = questionData;
        this.correctAnswer = correctAnswer;
        this.isActive = true;
        this.orderIndex = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getQuestionData() {
        return questionData;
    }

    public void setQuestionData(String questionData) {
        this.questionData = questionData;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    @Override
    public String toString() {
        return "GameData{" +
                "id=" + id +
                ", gameType=" + gameType +
                ", difficulty=" + difficulty +
                ", questionData='" + questionData + '\'' +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", hint='" + hint + '\'' +
                ", isActive=" + isActive +
                ", orderIndex=" + orderIndex +
                '}';
    }

    // 便捷方法：创建Character Matching游戏数据
    public static GameData createCharacterMatchingData(String character, String pinyin,
                                                      String[] distractors, Difficulty difficulty) {
        String questionData = String.format("{\"character\":\"%s\",\"pinyin\":\"%s\",\"distractors\":%s}",
                character, pinyin, java.util.Arrays.toString(distractors));
        return new GameData(GameType.CHARACTER_MATCHING, difficulty, questionData, pinyin);
    }

    // 便捷方法：创建Pronunciation Quiz游戏数据
    public static GameData createPronunciationData(String word, String pinyin,
                                                 String audioPath, Difficulty difficulty) {
        String questionData = String.format("{\"word\":\"%s\",\"pinyin\":\"%s\",\"audioPath\":\"%s\"}",
                word, pinyin, audioPath);
        return new GameData(GameType.PRONUNCIATION_QUIZ, difficulty, questionData, pinyin);
    }

    // 便捷方法：创建Word Puzzle游戏数据
    public static GameData createWordPuzzleData(String sentence, String[] words,
                                              Difficulty difficulty) {
        String questionData = String.format("{\"sentence\":\"%s\",\"words\":%s}",
                sentence, java.util.Arrays.toString(words));
        return new GameData(GameType.WORD_PUZZLE, difficulty, questionData, sentence);
    }
}