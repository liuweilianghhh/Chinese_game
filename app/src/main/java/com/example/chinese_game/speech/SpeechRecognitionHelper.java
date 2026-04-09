package com.example.chinese_game.speech;

import android.app.Activity;
import android.content.Context;

/**
 * 语音识别辅助类
 * 提供便捷的语音识别功能封装
 */
public class SpeechRecognitionHelper {
    
    private SpeechRecognitionManager manager;
    private SpeechRecognitionService service;
    private Activity activity;
    
    public SpeechRecognitionHelper(Activity activity) {
        this.activity = activity;
        this.manager = SpeechRecognitionManager.getInstance();
        this.manager.initialize(activity);
    }
    
    /**
     * 初始化语音识别服务
     * @param config 配置，如果为null则使用默认配置
     */
    public void initialize(SpeechRecognitionConfig config) {
        if (config == null) {
            config = SpeechRecognitionConfig.createChineseConfig();
        }
        service = manager.createRecognitionService(config);
    }
    
    /**
     * 检查并请求权限
     * @return true 如果已有权限
     */
    public boolean checkAndRequestPermission() {
        if (!manager.hasRecordAudioPermission(activity)) {
            manager.requestRecordAudioPermission(activity);
            return false;
        }
        return true;
    }
    
    /**
     * 开始语音识别
     * @param listener 监听器
     * @return true 如果成功开始
     */
    public boolean startRecognition(SpeechRecognitionListener listener) {
        if (service == null) {
            initialize(null);
        }
        
        if (!checkAndRequestPermission()) {
            android.util.Log.w("SpeechRecognition", "权限未授予");
            return false;
        }
        
        if (!service.isAvailable()) {
            android.util.Log.e("SpeechRecognition", "语音识别服务不可用");
            if (listener != null) {
                listener.onRecognitionError(-1, 
                    "语音识别服务不可用。请确保：\n" +
                    "1. 已连接网络\n" +
                    "2. SparkChain SDK已正确初始化");
            }
            return false;
        }
        
        android.util.Log.d("SpeechRecognition", "开始监听");
        service.startListening(listener);
        return true;
    }
    
    /**
     * 停止语音识别
     */
    public void stopRecognition() {
        if (service != null) {
            service.stopListening();
        }
    }
    
    /**
     * 取消语音识别
     */
    public void cancelRecognition() {
        if (service != null) {
            service.cancel();
        }
    }
    
    /**
     * 检查是否正在识别
     */
    public boolean isRecognizing() {
        return service != null && service.isListening();
    }
    
    /**
     * 检查语音识别是否可用
     */
    public boolean isAvailable() {
        return service != null && service.isAvailable();
    }
    
    /**
     * 设置识别语言
     */
    public void setLanguage(String languageCode) {
        if (service != null) {
            service.setLanguage(languageCode);
        }
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        if (service != null) {
            service.destroy();
            service = null;
        }
    }
    
    /**
     * 创建简单的回调接口
     */
    public interface SimpleCallback {
        void onSuccess(String recognizedText, float confidence);
        void onError(String errorMessage);
    }
    
    /**
     * 使用简单回调开始识别
     */
    public boolean startRecognition(SimpleCallback callback) {
        return startRecognition(new SpeechRecognitionManager.SimpleSpeechRecognitionListener() {
            @Override
            public void onRecognitionSuccess(SpeechRecognitionResult result) {
                if (callback != null && result.isSuccess()) {
                    callback.onSuccess(result.getRecognizedText(), result.getConfidence());
                }
            }
            
            @Override
            public void onRecognitionError(int errorCode, String errorMessage) {
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
}
