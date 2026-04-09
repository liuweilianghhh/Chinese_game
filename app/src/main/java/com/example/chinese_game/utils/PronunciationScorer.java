package com.example.chinese_game.utils;

/**
 * 发音评分工具类
 * 用于评估用户发音的准确性
 */
public class PronunciationScorer {
    
    /**
     * 评分结果
     */
    public static class ScoreResult {
        private int score;              // 分数 (0-100)
        private double accuracy;        // 准确度 (0.0-1.0)
        private boolean isCorrect;      // 是否正确
        private String feedback;        // 反馈信息
        
        public ScoreResult(int score, double accuracy, boolean isCorrect, String feedback) {
            this.score = score;
            this.accuracy = accuracy;
            this.isCorrect = isCorrect;
            this.feedback = feedback;
        }
        
        public int getScore() {
            return score;
        }
        
        public double getAccuracy() {
            return accuracy;
        }
        
        public boolean isCorrect() {
            return isCorrect;
        }
        
        public String getFeedback() {
            return feedback;
        }
    }
    
    /**
     * 简单匹配评分：检查识别文本是否包含目标词
     * 
     * @param recognizedText 识别的文本
     * @param targetWord 目标词语
     * @param confidence 识别置信度 (0.0-1.0)
     * @return 评分结果
     */
    public static ScoreResult simpleMatch(String recognizedText, String targetWord, float confidence) {
        if (recognizedText == null || targetWord == null) {
            return new ScoreResult(0, 0.0, false, "Invalid input");
        }
        
        recognizedText = recognizedText.trim();
        targetWord = targetWord.trim();
        
        if (recognizedText.isEmpty() || targetWord.isEmpty()) {
            return new ScoreResult(0, 0.0, false, "Empty input");
        }
        
        boolean contains = recognizedText.contains(targetWord);
        boolean exactMatch = recognizedText.equals(targetWord);
        
        int score;
        double accuracy;
        String feedback;
        
        if (exactMatch) {
            score = (int) (confidence * 100);
            accuracy = confidence;
            feedback = "Perfect pronunciation!";
        } else if (contains) {
            score = (int) (confidence * 80);
            accuracy = confidence * 0.8;
            feedback = "Good! Target word detected.";
        } else {
            score = 0;
            accuracy = 0.0;
            feedback = "Try again. Expected: " + targetWord;
        }
        
        return new ScoreResult(score, accuracy, contains, feedback);
    }
    
    /**
     * 编辑距离评分：基于Levenshtein距离计算相似度
     * 
     * @param recognizedText 识别的文本
     * @param targetWord 目标词语
     * @param confidence 识别置信度
     * @return 评分结果
     */
    public static ScoreResult editDistanceScore(String recognizedText, String targetWord, float confidence) {
        if (recognizedText == null || targetWord == null) {
            return new ScoreResult(0, 0.0, false, "Invalid input");
        }
        
        recognizedText = recognizedText.trim();
        targetWord = targetWord.trim();
        
        if (recognizedText.isEmpty() || targetWord.isEmpty()) {
            return new ScoreResult(0, 0.0, false, "Empty input");
        }
        
        int distance = levenshteinDistance(recognizedText, targetWord);
        int maxLen = Math.max(recognizedText.length(), targetWord.length());
        
        double similarity = 1.0 - ((double) distance / maxLen);
        double accuracy = similarity * confidence;
        int score = (int) (accuracy * 100);
        
        boolean isCorrect = similarity >= 0.7;
        
        String feedback;
        if (similarity >= 0.95) {
            feedback = "Excellent pronunciation!";
        } else if (similarity >= 0.8) {
            feedback = "Very good!";
        } else if (similarity >= 0.7) {
            feedback = "Good effort!";
        } else if (similarity >= 0.5) {
            feedback = "Close, but needs improvement.";
        } else {
            feedback = "Try again. Expected: " + targetWord;
        }
        
        return new ScoreResult(score, accuracy, isCorrect, feedback);
    }
    
    /**
     * 拼音匹配评分：比较拼音的相似度
     * 
     * @param recognizedText 识别的文本
     * @param targetWord 目标词语
     * @param targetPinyin 目标拼音
     * @param confidence 识别置信度
     * @return 评分结果
     */
    public static ScoreResult pinyinMatchScore(String recognizedText, String targetWord, 
                                               String targetPinyin, float confidence) {
        if (recognizedText == null || targetWord == null) {
            return new ScoreResult(0, 0.0, false, "Invalid input");
        }
        
        recognizedText = recognizedText.trim();
        targetWord = targetWord.trim();
        
        try {
            String recognizedPinyin = PinyinUtils.wordToPinyin(recognizedText);
            
            if (targetPinyin == null || targetPinyin.isEmpty()) {
                targetPinyin = PinyinUtils.wordToPinyin(targetWord);
            }
            
            recognizedPinyin = normalizePinyin(recognizedPinyin);
            targetPinyin = normalizePinyin(targetPinyin);
            
            int distance = levenshteinDistance(recognizedPinyin, targetPinyin);
            int maxLen = Math.max(recognizedPinyin.length(), targetPinyin.length());
            
            double similarity = 1.0 - ((double) distance / maxLen);
            double accuracy = similarity * confidence;
            int score = (int) (accuracy * 100);
            
            boolean isCorrect = similarity >= 0.75;
            
            String feedback;
            if (similarity >= 0.95) {
                feedback = "Perfect pronunciation!";
            } else if (similarity >= 0.85) {
                feedback = "Excellent!";
            } else if (similarity >= 0.75) {
                feedback = "Good pronunciation!";
            } else if (similarity >= 0.6) {
                feedback = "Close! Keep practicing.";
            } else {
                feedback = "Try again. Expected: " + targetWord + " (" + targetPinyin + ")";
            }
            
            return new ScoreResult(score, accuracy, isCorrect, feedback);
            
        } catch (Exception e) {
            return simpleMatch(recognizedText, targetWord, confidence);
        }
    }
    
    /**
     * 综合评分：结合多种评分方法
     * 
     * @param recognizedText 识别的文本
     * @param targetWord 目标词语
     * @param targetPinyin 目标拼音
     * @param confidence 识别置信度
     * @return 评分结果
     */
    public static ScoreResult comprehensiveScore(String recognizedText, String targetWord,
                                                 String targetPinyin, float confidence) {
        ScoreResult simpleResult = simpleMatch(recognizedText, targetWord, confidence);
        
        if (simpleResult.isCorrect()) {
            return simpleResult;
        }
        
        ScoreResult editResult = editDistanceScore(recognizedText, targetWord, confidence);
        ScoreResult pinyinResult = pinyinMatchScore(recognizedText, targetWord, targetPinyin, confidence);
        
        int finalScore = (int) ((editResult.getScore() * 0.5) + (pinyinResult.getScore() * 0.5));
        double finalAccuracy = (editResult.getAccuracy() * 0.5) + (pinyinResult.getAccuracy() * 0.5);
        boolean isCorrect = finalAccuracy >= 0.7;
        
        String feedback;
        if (finalAccuracy >= 0.9) {
            feedback = "Excellent pronunciation!";
        } else if (finalAccuracy >= 0.8) {
            feedback = "Very good!";
        } else if (finalAccuracy >= 0.7) {
            feedback = "Good effort!";
        } else if (finalAccuracy >= 0.5) {
            feedback = "Keep practicing!";
        } else {
            feedback = "Try again. Expected: " + targetWord;
        }
        
        return new ScoreResult(finalScore, finalAccuracy, isCorrect, feedback);
    }
    
    /**
     * 计算Levenshtein距离（编辑距离）
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * 规范化拼音：去除声调、空格等
     */
    private static String normalizePinyin(String pinyin) {
        if (pinyin == null) return "";
        
        return pinyin.toLowerCase()
                    .replaceAll("[āáǎà]", "a")
                    .replaceAll("[ēéěè]", "e")
                    .replaceAll("[īíǐì]", "i")
                    .replaceAll("[ōóǒò]", "o")
                    .replaceAll("[ūúǔù]", "u")
                    .replaceAll("[ǖǘǚǜü]", "v")
                    .replaceAll("\\s+", "")
                    .replaceAll("[^a-z]", "");
    }
    
    /**
     * 获取评分等级
     */
    public static String getScoreGrade(int score) {
        if (score >= 90) return "A+";
        if (score >= 85) return "A";
        if (score >= 80) return "B+";
        if (score >= 75) return "B";
        if (score >= 70) return "C+";
        if (score >= 60) return "C";
        if (score >= 50) return "D";
        return "F";
    }
}
