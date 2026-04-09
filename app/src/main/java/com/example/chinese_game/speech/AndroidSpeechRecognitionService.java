package com.example.chinese_game.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Android原生语音识别服务实现
 * 使用Android系统的SpeechRecognizer API
 */
public class AndroidSpeechRecognitionService implements SpeechRecognitionService {
    
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private SpeechRecognitionConfig config;
    private SpeechRecognitionListener listener;
    private boolean isListening;
    private long startTime;
    
    public AndroidSpeechRecognitionService() {
        this.isListening = false;
    }
    
    @Override
    public void initialize(Context context, SpeechRecognitionConfig config) {
        this.context = context.getApplicationContext();
        this.config = config != null ? config : SpeechRecognitionConfig.createDefault();
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.context);
    }
    
    @Override
    public void startListening(SpeechRecognitionListener listener) {
        if (!isAvailable()) {
            if (listener != null) {
                listener.onRecognitionError(-1, "Speech recognition not available");
            }
            return;
        }
        
        if (isListening) {
            stopListening();
        }
        
        this.listener = listener;
        this.isListening = true;
        this.startTime = System.currentTimeMillis();
        
        Intent intent = createRecognitionIntent();
        speechRecognizer.setRecognitionListener(new InternalRecognitionListener());
        speechRecognizer.startListening(intent);
    }
    
    private Intent createRecognitionIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                       RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.getLanguage());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, config.getMaxResults());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, config.isPartialResults());
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 
                       config.getSilenceTimeout());
        
        if (config.isOfflineMode()) {
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        }
        
        return intent;
    }
    
    @Override
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }
    
    @Override
    public void cancel() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            isListening = false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return context != null && SpeechRecognizer.isRecognitionAvailable(context);
    }
    
    @Override
    public boolean isListening() {
        return isListening;
    }
    
    @Override
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        isListening = false;
        listener = null;
    }
    
    @Override
    public void setLanguage(String languageCode) {
        if (config != null) {
            config = new SpeechRecognitionConfig.Builder()
                    .setLanguage(languageCode)
                    .setMaxResults(config.getMaxResults())
                    .setPartialResults(config.isPartialResults())
                    .setOfflineMode(config.isOfflineMode())
                    .setMaxRecordingDuration(config.getMaxRecordingDuration())
                    .setSilenceTimeout(config.getSilenceTimeout())
                    .setShowRecognitionDialog(config.isShowRecognitionDialog())
                    .build();
        }
    }
    
    @Override
    public String getLanguage() {
        return config != null ? config.getLanguage() : SpeechRecognitionConfig.DEFAULT_LANGUAGE;
    }
    
    /**
     * 内部识别监听器，将Android系统回调转换为我们的接口
     */
    private class InternalRecognitionListener implements RecognitionListener {
        
        @Override
        public void onReadyForSpeech(Bundle params) {
            if (listener != null) {
                listener.onReadyForSpeech();
            }
        }
        
        @Override
        public void onBeginningOfSpeech() {
            if (listener != null) {
                listener.onBeginningOfSpeech();
            }
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            if (listener != null) {
                listener.onRmsChanged(rmsdB);
            }
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {
            // 不处理原始音频数据
        }
        
        @Override
        public void onEndOfSpeech() {
            isListening = false;
            if (listener != null) {
                listener.onEndOfSpeech();
            }
        }
        
        @Override
        public void onError(int error) {
            isListening = false;
            if (listener != null) {
                String errorMessage = getErrorMessage(error);
                listener.onRecognitionError(error, errorMessage);
            }
        }
        
        @Override
        public void onResults(Bundle results) {
            isListening = false;
            if (listener != null) {
                SpeechRecognitionResult result = parseResults(results);
                listener.onRecognitionSuccess(result);
            }
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {
            if (listener != null && config.isPartialResults()) {
                ArrayList<String> matches = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    listener.onPartialResults(matches.get(0));
                }
            }
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {
            // 不处理自定义事件
        }
        
        private SpeechRecognitionResult parseResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            float[] confidences = results.getFloatArray(
                    SpeechRecognizer.CONFIDENCE_SCORES);
            
            SpeechRecognitionResult result = new SpeechRecognitionResult();
            result.setSuccess(matches != null && !matches.isEmpty());
            result.setDurationMs(System.currentTimeMillis() - startTime);
            
            if (matches != null && !matches.isEmpty()) {
                result.setRecognizedText(matches.get(0));
                
                float confidence = 0.0f;
                if (confidences != null && confidences.length > 0) {
                    confidence = confidences[0];
                }
                result.setConfidence(confidence);
                
                for (int i = 0; i < matches.size(); i++) {
                    float conf = (confidences != null && i < confidences.length) 
                            ? confidences[i] : 0.0f;
                    result.addCandidate(matches.get(i), conf);
                }
            } else {
                result.setErrorMessage("No recognition results");
            }
            
            return result;
        }
        
        private String getErrorMessage(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    return "Audio recording error";
                case SpeechRecognizer.ERROR_CLIENT:
                    return "Client side error";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    return "Insufficient permissions";
                case SpeechRecognizer.ERROR_NETWORK:
                    return "Network error";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    return "Network timeout";
                case SpeechRecognizer.ERROR_NO_MATCH:
                    return "No recognition result matched";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    return "Recognition service busy";
                case SpeechRecognizer.ERROR_SERVER:
                    return "Server error";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    return "No speech input";
                default:
                    return "Unknown error: " + error;
            }
        }
    }
}
