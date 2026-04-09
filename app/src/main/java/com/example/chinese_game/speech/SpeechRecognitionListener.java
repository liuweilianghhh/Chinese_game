package com.example.chinese_game.speech;

/**
 * 语音识别监听器接口
 * 用于监听语音识别的各个阶段和结果
 */
public interface SpeechRecognitionListener {
    
    /**
     * 准备开始录音
     */
    void onReadyForSpeech();
    
    /**
     * 开始录音
     */
    void onBeginningOfSpeech();
    
    /**
     * 录音结束
     */
    void onEndOfSpeech();
    
    /**
     * 音量变化
     * @param rmsdB 分贝值
     */
    void onRmsChanged(float rmsdB);
    
    /**
     * 识别成功
     * @param result 识别结果
     */
    void onRecognitionSuccess(SpeechRecognitionResult result);
    
    /**
     * 识别失败
     * @param errorCode 错误代码
     * @param errorMessage 错误信息
     */
    void onRecognitionError(int errorCode, String errorMessage);
    
    /**
     * 部分识别结果（实时识别）
     * @param partialResult 部分结果
     */
    void onPartialResults(String partialResult);
}
