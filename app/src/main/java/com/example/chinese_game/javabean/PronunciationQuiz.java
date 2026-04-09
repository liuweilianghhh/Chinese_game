package com.example.chinese_game.javabean;

public class PronunciationQuiz {
    private int id;
    // 规范化字段：关联到句子与词语
    private int sentenceId;
    private int wordId;
    // 便于前端展示的冗余信息
    private String sentence;
    private String word;
    private String pinyin;
    private String audioPath;
    private String difficulty;
    private String hint;

    public PronunciationQuiz() {
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

    public int getWordId() {
        return wordId;
    }

    public void setWordId(int wordId) {
        this.wordId = wordId;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
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
        return "PronunciationQuiz{" +
                "id=" + id +
                ", sentenceId=" + sentenceId +
                ", wordId=" + wordId +
                ", sentence='" + sentence + '\'' +
                ", word='" + word + '\'' +
                ", pinyin='" + pinyin + '\'' +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}