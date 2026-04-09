package com.example.chinese_game.speech;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 语音识别管理器
 * 提供统一的语音识别管理功能，包括权限检查、服务创建等
 */
public class SpeechRecognitionManager {
    
    public static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    private static SpeechRecognitionManager instance;
    private SpeechRecognitionService recognitionService;
    private Context context;
    
    private SpeechRecognitionManager() {
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized SpeechRecognitionManager getInstance() {
        if (instance == null) {
            instance = new SpeechRecognitionManager();
        }
        return instance;
    }
    
    /**
     * 初始化管理器
     * @param context 应用上下文
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 创建语音识别服务
     * @param config 配置
     * @return 语音识别服务实例
     */
    public SpeechRecognitionService createRecognitionService(SpeechRecognitionConfig config) {
        if (context == null) {
            throw new IllegalStateException("SpeechRecognitionManager not initialized. Call initialize() first.");
        }
        
        // 使用科大讯飞SparkChain大模型语音识别服务
        recognitionService = new SparkChainSpeechRecognitionService();
        recognitionService.initialize(context, config);
        return recognitionService;
    }
    
    /**
     * 获取当前的识别服务
     */
    public SpeechRecognitionService getRecognitionService() {
        return recognitionService;
    }
    
    /**
     * 检查录音权限
     * @param context 上下文
     * @return true 如果有权限
     */
    public boolean hasRecordAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 请求录音权限
     * @param activity Activity实例
     */
    public void requestRecordAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }
    
    /**
     * 检查语音识别是否可用
     * @return true 如果可用
     */
    public boolean isSpeechRecognitionAvailable() {
        if (recognitionService != null) {
            return recognitionService.isAvailable();
        }
        return false;
    }
    
    /**
     * 开始语音识别（便捷方法）
     * @param listener 监听器
     */
    public void startListening(SpeechRecognitionListener listener) {
        if (recognitionService != null) {
            recognitionService.startListening(listener);
        }
    }
    
    /**
     * 停止语音识别
     */
    public void stopListening() {
        if (recognitionService != null) {
            recognitionService.stopListening();
        }
    }
    
    /**
     * 取消语音识别
     */
    public void cancel() {
        if (recognitionService != null) {
            recognitionService.cancel();
        }
    }
    
    /**
     * 销毁管理器
     */
    public void destroy() {
        if (recognitionService != null) {
            recognitionService.destroy();
            recognitionService = null;
        }
    }
    
    /**
     * 创建简单的适配器监听器
     * 可以只重写需要的方法
     */
    public static abstract class SimpleSpeechRecognitionListener implements SpeechRecognitionListener {
        
        @Override
        public void onReadyForSpeech() {
            // 默认空实现
        }
        
        @Override
        public void onBeginningOfSpeech() {
            // 默认空实现
        }
        
        @Override
        public void onEndOfSpeech() {
            // 默认空实现
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            // 默认空实现
        }
        
        @Override
        public void onPartialResults(String partialResult) {
            // 默认空实现
        }
        
        @Override
        public abstract void onRecognitionSuccess(SpeechRecognitionResult result);
        
        @Override
        public abstract void onRecognitionError(int errorCode, String errorMessage);
    }
}
