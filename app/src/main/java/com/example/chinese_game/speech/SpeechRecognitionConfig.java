package com.example.chinese_game.speech;

/**
 * 语音识别配置类
 * 用于配置语音识别的各项参数
 */
public class SpeechRecognitionConfig {
    
    /** 默认语言：中文（简体） */
    public static final String DEFAULT_LANGUAGE = "zh-CN";
    
    /** 英语（美国） */
    public static final String LANGUAGE_EN_US = "en-US";
    
    /** 中文（简体） */
    public static final String LANGUAGE_ZH_CN = "zh-CN";
    
    /** 中文（繁体） */
    public static final String LANGUAGE_ZH_TW = "zh-TW";
    
    /** 识别语言 */
    private String language;
    
    /** 最大识别结果数量 */
    private int maxResults;
    
    /** 是否启用部分识别结果 */
    private boolean partialResults;
    
    /** 是否离线识别 */
    private boolean offlineMode;
    
    /** 最大录音时长（秒） */
    private int maxRecordingDuration;
    
    /** 静音超时时间（毫秒） */
    private int silenceTimeout;
    
    /** 是否显示识别对话框 */
    private boolean showRecognitionDialog;
    
    private SpeechRecognitionConfig(Builder builder) {
        this.language = builder.language;
        this.maxResults = builder.maxResults;
        this.partialResults = builder.partialResults;
        this.offlineMode = builder.offlineMode;
        this.maxRecordingDuration = builder.maxRecordingDuration;
        this.silenceTimeout = builder.silenceTimeout;
        this.showRecognitionDialog = builder.showRecognitionDialog;
    }
    
    /**
     * 创建默认配置
     */
    public static SpeechRecognitionConfig createDefault() {
        return new Builder().build();
    }
    
    /**
     * 创建中文识别配置
     */
    public static SpeechRecognitionConfig createChineseConfig() {
        return new Builder()
                .setLanguage(LANGUAGE_ZH_CN)
                .setMaxResults(5)
                .setPartialResults(true)
                .build();
    }
    
    public String getLanguage() {
        return language;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public boolean isPartialResults() {
        return partialResults;
    }
    
    public boolean isOfflineMode() {
        return offlineMode;
    }
    
    public int getMaxRecordingDuration() {
        return maxRecordingDuration;
    }
    
    public int getSilenceTimeout() {
        return silenceTimeout;
    }
    
    public boolean isShowRecognitionDialog() {
        return showRecognitionDialog;
    }
    
    /**
     * 配置构建器
     */
    public static class Builder {
        private String language = DEFAULT_LANGUAGE;
        private int maxResults = 5;
        private boolean partialResults = true;
        private boolean offlineMode = false;
        private int maxRecordingDuration = 30;
        private int silenceTimeout = 3000;
        private boolean showRecognitionDialog = false;
        
        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }
        
        public Builder setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        
        public Builder setPartialResults(boolean partialResults) {
            this.partialResults = partialResults;
            return this;
        }
        
        public Builder setOfflineMode(boolean offlineMode) {
            this.offlineMode = offlineMode;
            return this;
        }
        
        public Builder setMaxRecordingDuration(int maxRecordingDuration) {
            this.maxRecordingDuration = maxRecordingDuration;
            return this;
        }
        
        public Builder setSilenceTimeout(int silenceTimeout) {
            this.silenceTimeout = silenceTimeout;
            return this;
        }
        
        public Builder setShowRecognitionDialog(boolean showRecognitionDialog) {
            this.showRecognitionDialog = showRecognitionDialog;
            return this;
        }
        
        public SpeechRecognitionConfig build() {
            return new SpeechRecognitionConfig(this);
        }
    }
}
