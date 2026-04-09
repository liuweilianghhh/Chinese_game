package com.example.chinese_game.speech;

import android.content.Context;

/**
 * 语音识别服务接口
 * 定义语音识别的核心功能
 */
public interface SpeechRecognitionService {
    
    /**
     * 初始化语音识别服务
     * @param context Android上下文
     * @param config 识别配置
     */
    void initialize(Context context, SpeechRecognitionConfig config);
    
    /**
     * 开始语音识别
     * @param listener 识别结果监听器
     */
    void startListening(SpeechRecognitionListener listener);
    
    /**
     * 停止语音识别
     */
    void stopListening();
    
    /**
     * 取消语音识别
     */
    void cancel();
    
    /**
     * 检查语音识别是否可用
     * @return true 如果可用
     */
    boolean isAvailable();
    
    /**
     * 检查是否正在识别
     * @return true 如果正在识别
     */
    boolean isListening();
    
    /**
     * 销毁服务，释放资源
     */
    void destroy();
    
    /**
     * 设置识别语言
     * @param languageCode 语言代码，如 "zh-CN" 或 "en-US"
     */
    void setLanguage(String languageCode);
    
    /**
     * 获取当前语言设置
     * @return 语言代码
     */
    String getLanguage();
}
