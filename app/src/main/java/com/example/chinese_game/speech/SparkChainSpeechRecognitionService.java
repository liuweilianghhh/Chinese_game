package com.example.chinese_game.speech;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.chinese_game.ChineseGameApplication;
import com.iflytek.sparkchain.core.asr.ASR;
import com.iflytek.sparkchain.core.asr.AsrCallbacks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于科大讯飞SparkChain SDK的语音识别服务实现，参照官方Demo写法。
 */
public class SparkChainSpeechRecognitionService implements SpeechRecognitionService {

    private static final String TAG = "SparkChainSpeech";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private Context context;
    private SpeechRecognitionConfig config;
    private SpeechRecognitionListener listener;
    private volatile boolean isListening;
    private long startTime;

    private ASR asr;
    private AudioRecord audioRecord;
    private final AtomicBoolean isWriting = new AtomicBoolean(false);
    private StringBuilder resultBuilder = new StringBuilder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean useDynamicCorrection = false;
    private String cachedPartialResult = "";

    @Override
    public void initialize(Context context, SpeechRecognitionConfig config) {
        this.context = context.getApplicationContext();
        this.config = config != null ? config : SpeechRecognitionConfig.createDefault();
        Log.d(TAG, "SparkChain语音识别服务已准备");
    }

    private boolean ensureASR() {
        try {
            if (!ChineseGameApplication.isSparkChainInitialized()) {
                if (!ChineseGameApplication.initSparkChain()) {
                    String err = ChineseGameApplication.getSparkChainInitError();
                    Log.e(TAG, "SparkChain SDK未初始化: " + err);
                    return false;
                }
            }
            if (asr == null) {
                asr = new ASR();
                asr.registerCallbacks(asrCallbacks);
            }
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "初始化ASR异常", e);
            return false;
        }
    }

    @Override
    public void startListening(SpeechRecognitionListener listener) {
        if (isListening) {
            Log.w(TAG, "正在识别中，请勿重复开启");
            return;
        }

        this.listener = listener;
        this.resultBuilder.setLength(0);
        this.cachedPartialResult = "";
        this.startTime = System.currentTimeMillis();

        if (!ensureASR()) {
            if (listener != null) {
                String err = ChineseGameApplication.getSparkChainInitError();
                String msg = "SDK未初始化";
                if (err != null && err.contains("libSparkChain.so")) {
                    msg = "当前设备架构不支持（需要ARM设备/模拟器）";
                } else if (err != null) {
                    msg = "SDK初始化失败: " + err;
                }
                notifyError(18301, msg);
            }
            return;
        }

        String language = "zh_cn";
        useDynamicCorrection = "zh_cn".equals(language);

        asr.language(language);
        asr.domain("iat");
        asr.accent("mandarin");
        asr.vinfo(true);
        if (useDynamicCorrection) {
            asr.dwa("wpgs");
        }

        int ret = asr.start(String.valueOf(System.currentTimeMillis()));
        if (ret != 0) {
            Log.e(TAG, "启动ASR会话失败，错误码: " + ret);
            if (listener != null) {
                notifyError(ret, "启动识别失败，错误码: " + ret);
            }
            return;
        }

        isListening = true;
        Log.d(TAG, "ASR会话启动成功");

        if (listener != null) {
            notifyOnMainThread(() -> listener.onReadyForSpeech());
        }

        startRecording();
    }

    private void startRecording() {
        try {
            int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            int bufferSize = Math.max(minBuf, SAMPLE_RATE * 2) * 2;

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord初始化失败");
                notifyError(-1, "麦克风初始化失败");
                stopListening();
                return;
            }

            audioRecord.startRecording();
            isWriting.set(true);

            if (listener != null) {
                notifyOnMainThread(() -> listener.onBeginningOfSpeech());
            }

            new Thread(() -> {
                byte[] buffer = new byte[1280];
                while (isWriting.get() && audioRecord != null) {
                    try {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0 && isWriting.get() && asr != null) {
                            asr.write(buffer.clone());

                            float rms = calculateRMS(buffer, read);
                            if (listener != null) {
                                notifyOnMainThread(() -> listener.onRmsChanged(rms));
                            }
                        }
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "录音线程异常", e);
                        break;
                    }
                }
                Log.d(TAG, "录音线程结束");
            }, "ASR-Record").start();

            Log.d(TAG, "开始录音");
        } catch (Exception e) {
            Log.e(TAG, "启动录音失败", e);
            notifyError(-1, "启动录音失败: " + e.getMessage());
            stopListening();
        }
    }

    @Override
    public void stopListening() {
        if (!isListening) return;
        Log.d(TAG, "停止识别");

        isWriting.set(false);
        releaseAudioRecord();

        if (asr != null) {
            try {
                asr.stop(false);
            } catch (Exception e) {
                Log.e(TAG, "停止ASR异常", e);
            }
        }

        isListening = false;
        if (listener != null) {
            notifyOnMainThread(() -> listener.onEndOfSpeech());
        }
    }

    @Override
    public void cancel() {
        Log.d(TAG, "取消识别");
        isWriting.set(false);
        releaseAudioRecord();
        if (asr != null) {
            try { asr.stop(true); } catch (Exception ignored) {}
        }
        isListening = false;
    }

    private void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "释放AudioRecord异常", e);
            }
            audioRecord = null;
        }
    }

    @Override public boolean isAvailable() { return true; }
    @Override public boolean isListening() { return isListening; }

    @Override
    public void destroy() {
        cancel();
        asr = null;
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

    private final AsrCallbacks asrCallbacks = new AsrCallbacks() {
        @Override
        public void onResult(ASR.ASRResult asrResult, Object usrContext) {
            String result = asrResult.getBestMatchText();
            int status = asrResult.getStatus();
            Log.d(TAG, "识别结果: status=" + status + ", text=" + result);

            if (status == 0) {
                if (useDynamicCorrection) {
                    cachedPartialResult = resultBuilder.toString();
                    String partial = cachedPartialResult + result;
                    if (listener != null) {
                        notifyOnMainThread(() -> listener.onPartialResults(partial));
                    }
                }
            } else if (status == 2) {
                if (useDynamicCorrection) {
                    String finalText = (cachedPartialResult + result).trim();
                    deliverFinalResult(finalText);
                } else {
                    deliverFinalResult(result != null ? result.trim() : "");
                }
            } else {
                if (useDynamicCorrection && listener != null) {
                    String partial = cachedPartialResult + result;
                    notifyOnMainThread(() -> listener.onPartialResults(partial));
                }
            }
        }

        @Override
        public void onError(ASR.ASRError asrError, Object usrContext) {
            int code = asrError.getCode();
            String msg = asrError.getErrMsg();
            Log.e(TAG, "识别错误: code=" + code + ", msg=" + msg);

            isListening = false;
            isWriting.set(false);
            releaseAudioRecord();

            if (listener != null) {
                notifyError(code, getErrorMessage(code, msg));
            }
        }

        @Override
        public void onBeginOfSpeech() {
            Log.d(TAG, "检测到语音开始");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "检测到语音结束");
        }
    };

    private void deliverFinalResult(String finalText) {
        Log.d(TAG, "最终识别结果: " + finalText);

        if (listener != null) {
            SpeechRecognitionResult speechResult = new SpeechRecognitionResult();
            speechResult.setRecognizedText(finalText);
            speechResult.setConfidence(finalText.isEmpty() ? 0f : 0.95f);
            speechResult.setSuccess(!finalText.isEmpty());
            speechResult.setDurationMs(System.currentTimeMillis() - startTime);
            if (!finalText.isEmpty()) {
                speechResult.addCandidate(finalText, 0.95f);
            } else {
                speechResult.setErrorMessage("无识别结果");
            }
            notifyOnMainThread(() -> listener.onRecognitionSuccess(speechResult));
        }
        isListening = false;
    }

    private float calculateRMS(byte[] buffer, int readSize) {
        long sum = 0;
        for (int i = 0; i + 1 < readSize; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sum += (long) sample * sample;
        }
        double rms = Math.sqrt((double) sum / (readSize / 2));
        return (float) (20 * Math.log10(rms + 1));
    }

    private String getErrorMessage(int errorCode, String originalMsg) {
        switch (errorCode) {
            case 18301: return "SDK未初始化";
            case 18700: case 18701: return "网络错误，请检查网络连接";
            case 18702: return "授权失败，请检查APPID配置";
            case 10005: return "APPID授权失败，请确认APPID是否正确";
            case 10114: return "会话超时";
            default: return originalMsg != null ? originalMsg : "识别错误: " + errorCode;
        }
    }

    private void notifyError(int code, String msg) {
        notifyOnMainThread(() -> {
            if (listener != null) listener.onRecognitionError(code, msg);
        });
    }

    private void notifyOnMainThread(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else mainHandler.post(r);
    }
}
