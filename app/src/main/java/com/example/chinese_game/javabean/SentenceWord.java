package com.example.chinese_game.javabean;

/**
 * 分词结果表 sentence_words 对应的实体，用于按词性选取干扰项等。
 */
public class SentenceWord {
    private int id;
    private int sentenceId;
    private String word;
    private String pinyin;
    private String posTag;
    private int wordOrder;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSentenceId() { return sentenceId; }
    public void setSentenceId(int sentenceId) { this.sentenceId = sentenceId; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getPinyin() { return pinyin; }
    public void setPinyin(String pinyin) { this.pinyin = pinyin; }
    public String getPosTag() { return posTag; }
    public void setPosTag(String posTag) { this.posTag = posTag; }
    public int getWordOrder() { return wordOrder; }
    public void setWordOrder(int wordOrder) { this.wordOrder = wordOrder; }
}
