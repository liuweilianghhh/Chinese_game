package com.example.chinese_game.speech;

import java.util.ArrayList;
import java.util.List;

/**
 * 语音识别结果数据模型
 * 封装识别后的文本、置信度等信息
 */
public class SpeechRecognitionResult {
    
    /** 识别的文本结果 */
    private String recognizedText;
    
    /** 置信度 (0.0 - 1.0) */
    private float confidence;
    
    /** 是否识别成功 */
    private boolean success;
    
    /** 错误信息（如果有） */
    private String errorMessage;
    
    /** 多个候选结果 */
    private List<CandidateResult> candidates;
    
    /** 识别耗时（毫秒） */
    private long durationMs;
    
    public SpeechRecognitionResult() {
        this.candidates = new ArrayList<>();
        this.success = false;
        this.confidence = 0.0f;
    }
    
    public SpeechRecognitionResult(String recognizedText, float confidence, boolean success) {
        this.recognizedText = recognizedText;
        this.confidence = confidence;
        this.success = success;
        this.candidates = new ArrayList<>();
    }
    
    /**
     * 候选识别结果
     */
    public static class CandidateResult {
        private String text;
        private float confidence;
        
        public CandidateResult(String text, float confidence) {
            this.text = text;
            this.confidence = confidence;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }
        
        @Override
        public String toString() {
            return text + " (" + String.format("%.2f", confidence) + ")";
        }
    }
    
    public String getRecognizedText() {
        return recognizedText;
    }
    
    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<CandidateResult> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<CandidateResult> candidates) {
        this.candidates = candidates;
    }
    
    public void addCandidate(String text, float confidence) {
        this.candidates.add(new CandidateResult(text, confidence));
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    @Override
    public String toString() {
        if (!success) {
            return "Recognition failed: " + errorMessage;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Text: ").append(recognizedText)
          .append(", Confidence: ").append(String.format("%.2f", confidence))
          .append(", Duration: ").append(durationMs).append("ms");
        if (!candidates.isEmpty()) {
            sb.append(", Candidates: ").append(candidates.size());
        }
        return sb.toString();
    }
}
