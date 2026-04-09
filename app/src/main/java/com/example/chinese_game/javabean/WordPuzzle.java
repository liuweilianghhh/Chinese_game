package com.example.chinese_game.javabean;

public class WordPuzzle {
    private int id;
    // 规范化字段：关联到句子
    private int sentenceId;
    // 便于展示的冗余信息
    private String sentence;
    private String difficulty;
    private String hint;

    public WordPuzzle() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(int sentenceId) {
        this.sentenceId = sentenceId;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    @Override
    public String toString() {
        return "WordPuzzle{" +
                "id=" + id +
                ", sentenceId=" + sentenceId +
                ", sentence='" + sentence + '\'' +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}