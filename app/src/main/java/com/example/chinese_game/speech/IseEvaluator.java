package com.example.chinese_game.speech;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * 讯飞ISE语音评测服务。
 * 通过WebSocket连接讯飞ISE API，发送音频获取发音评分。
 * <p>
 * 使用方法：
 * <pre>
 *   IseEvaluator evaluator = new IseEvaluator(appId, apiKey, apiSecret);
 *   evaluator.evaluate("你好", callback);   // 开始录音+评测
 *   // ... 用户说话 ...
 *   evaluator.stop();                       // 停止录音，等待结果
 * </pre>
 */
public class IseEvaluator {

    private static final String TAG = "IseEvaluator";
    private static final String ISE_HOST = "ise-api.xfyun.cn";
    private static final String ISE_PATH = "/v2/open-ise";
    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME_SIZE = 1280; // 40ms @ 16kHz 16bit mono

    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WebSocket webSocket;
    private AudioRecord audioRecord;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isSendingComplete = new AtomicBoolean(false);
    private EvaluationCallback callback;
    private RmsCallback rmsCallback;
    private StringBuilder resultXml = new StringBuilder();

    public interface EvaluationCallback {
        void onResult(EvalResult result);
        void onError(String errorMessage);
    }

    public interface RmsCallback {
        void onRmsChanged(float rmsDb);
    }

    /** 评测结果 */
    public static class EvalResult {
        public final String recognizedText;
        public final float totalScore;     // 总分 (0‥100 单词模式)
        public final float accuracyScore;  // 准确度
        public final float fluencyScore;   // 流畅度
        public final float completenessScore; // 完整度
        public final String rawXml;

        public EvalResult(String recognizedText, float totalScore,
                          float accuracyScore, float fluencyScore,
                          float completenessScore, String rawXml) {
            this.recognizedText = recognizedText;
            this.totalScore = totalScore;
            this.accuracyScore = accuracyScore;
            this.fluencyScore = fluencyScore;
            this.completenessScore = completenessScore;
            this.rawXml = rawXml;
        }
    }

    public IseEvaluator(String appId, String apiKey, String apiSecret) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void setRmsCallback(RmsCallback cb) {
        this.rmsCallback = cb;
    }

    /**
     * 开始评测：连接WebSocket → 录音 → 发送音频帧
     *
     * @param text     目标文本（用户应读的内容）
     * @param callback 结果回调
     */
    public void evaluate(String text, EvaluationCallback callback) {
        this.callback = callback;
        this.resultXml.setLength(0);
        this.isSendingComplete.set(false);

        try {
            String url = buildAuthUrl();
            Log.d(TAG, "连接ISE WebSocket...");

            Request request = new Request.Builder().url(url).build();
            webSocket = client.newWebSocket(request, new IseWebSocketListener(text));
        } catch (Exception e) {
            Log.e(TAG, "构建ISE URL失败", e);
            notifyError("ISE连接失败: " + e.getMessage());
        }
    }

    /** 停止录音，发送最后一帧 */
    public void stop() {
        isRecording.set(false);
    }

    /** 取消评测 */
    public void cancel() {
        isRecording.set(false);
        releaseAudioRecord();
        if (webSocket != null) {
            try { webSocket.close(1000, "cancelled"); } catch (Exception ignored) {}
            webSocket = null;
        }
    }

    public boolean isActive() {
        return isRecording.get() || !isSendingComplete.get();
    }

    // ==================== WebSocket Listener ====================

    private class IseWebSocketListener extends WebSocketListener {
        private final String targetText;
        private boolean firstFrameSent = false;

        IseWebSocketListener(String targetText) {
            this.targetText = targetText;
        }

        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
            Log.d(TAG, "ISE WebSocket已连接");
            startAudioCapture(ws);
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            try {
                JSONObject json = new JSONObject(text);
                int code = json.optInt("code", -1);
                if (code != 0) {
                    String msg = json.optString("message", "未知错误");
                    Log.e(TAG, "ISE错误: code=" + code + " msg=" + msg);
                    notifyError("评测错误(" + code + "): " + msg);
                    return;
                }

                JSONObject data = json.optJSONObject("data");
                if (data == null) return;

                int status = data.optInt("status", -1);
                String result = data.optString("data", "");
                if (!result.isEmpty()) {
                    String decoded = new String(Base64.decode(result, Base64.NO_WRAP), StandardCharsets.UTF_8);
                    resultXml.append(decoded);
                }

                if (status == 2) {
                    Log.d(TAG, "ISE评测完成");
                    parseAndNotifyResult(resultXml.toString());
                    ws.close(1000, "done");
                }
            } catch (Exception e) {
                Log.e(TAG, "解析ISE结果异常", e);
                notifyError("结果解析失败: " + e.getMessage());
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response response) {
            Log.e(TAG, "ISE WebSocket失败", t);
            releaseAudioRecord();
            notifyError("连接失败: " + t.getMessage());
        }

        @Override
        public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
            Log.d(TAG, "ISE WebSocket关闭: " + reason);
            releaseAudioRecord();
        }

        private void startAudioCapture(WebSocket ws) {
            try {
                int bufSize = Math.max(
                        AudioRecord.getMinBufferSize(SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT),
                        SAMPLE_RATE * 2) * 2;

                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufSize);

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    notifyError("麦克风初始化失败");
                    return;
                }

                audioRecord.startRecording();
                isRecording.set(true);

                new Thread(() -> {
                    byte[] buffer = new byte[FRAME_SIZE];
                    int frameIndex = 0;

                    while (isRecording.get()) {
                        int read = audioRecord.read(buffer, 0, FRAME_SIZE);
                        if (read <= 0) continue;

                        try {
                            int status;
                            if (frameIndex == 0) {
                                status = 0; // first frame
                            } else {
                                status = 1; // middle frame
                            }
                            sendFrame(ws, buffer, read, status, frameIndex == 0 ? targetText : null);
                            frameIndex++;

                            if (rmsCallback != null) {
                                float rms = calcRms(buffer, read);
                                mainHandler.post(() -> rmsCallback.onRmsChanged(rms));
                            }
                            Thread.sleep(40);
                        } catch (Exception e) {
                            Log.e(TAG, "发送帧异常", e);
                            break;
                        }
                    }

                    // 发送最后一帧
                    try {
                        sendFrame(ws, new byte[0], 0, 2, null);
                    } catch (Exception e) {
                        Log.e(TAG, "发送最后帧失败", e);
                    }

                    isSendingComplete.set(true);
                    releaseAudioRecord();
                    Log.d(TAG, "音频发送完毕，共" + frameIndex + "帧");
                }, "ISE-Audio").start();

            } catch (Exception e) {
                Log.e(TAG, "启动录音失败", e);
                notifyError("启动录音失败: " + e.getMessage());
            }
        }

        private void sendFrame(WebSocket ws, byte[] audio, int len, int status, String text) throws Exception {
            JSONObject frame = new JSONObject();

            if (status == 0) {
                // ---- 第一帧: common + business(cmd=ssb) + data ----
                JSONObject common = new JSONObject();
                common.put("app_id", appId);
                frame.put("common", common);

                JSONObject business = new JSONObject();
                business.put("sub", "ise");
                business.put("ent", "cn_vip");
                business.put("category", "read_word");
                business.put("text", "\uFEFF" + text);
                business.put("cmd", "ssb");
                business.put("auf", "audio/L16;rate=16000");
                business.put("aue", "raw");
                business.put("rstcd", "utf8");
                frame.put("business", business);

            } else if (status == 1) {
                // ---- 中间帧: business(cmd=auw, aus=1) + data ----
                JSONObject business = new JSONObject();
                business.put("cmd", "auw");
                business.put("aus", 1);
                business.put("aue", "raw");
                frame.put("business", business);

            } else if (status == 2) {
                // ---- 最后帧: business(cmd=auw, aus=4) + data ----
                JSONObject business = new JSONObject();
                business.put("cmd", "auw");
                business.put("aus", 4);
                business.put("aue", "raw");
                frame.put("business", business);
            }

            // data
            JSONObject data = new JSONObject();
            data.put("status", status);
            if (len > 0) {
                byte[] audioSlice = new byte[len];
                System.arraycopy(audio, 0, audioSlice, 0, len);
                data.put("data", Base64.encodeToString(audioSlice, Base64.NO_WRAP));
            } else {
                data.put("data", "");
            }
            frame.put("data", data);

            ws.send(frame.toString());
        }
    }

    // ==================== 结果解析 ====================

    private void parseAndNotifyResult(String rawResult) {
        // 去掉XML前面可能的乱码（base64解码残留）
        int xmlStart = rawResult.indexOf("<?xml");
        if (xmlStart < 0) xmlStart = rawResult.indexOf("<xml_result");
        String xml = xmlStart > 0 ? rawResult.substring(xmlStart) : rawResult;

        Log.i(TAG, "ISE原始XML: " + xml);

        try {
            // 讯飞ISE read_word 的评分在 <read_word> 元素的 **属性** 中
            // 例: <read_word total_score="85.5" fluency_score="80.2" ...>
            float totalScore = extractAttr(xml, "read_word", "total_score");
            float phonScore  = extractAttr(xml, "read_word", "phone_score");
            float fluency    = extractAttr(xml, "read_word", "fluency_score");
            float integrity  = extractAttr(xml, "read_word", "integrity_score");
            float toneScore  = extractAttr(xml, "read_word", "tone_score");

            // content 也在属性里: <read_word content="你好">
            String content = extractAttrStr(xml, "read_word", "content");
            if (content.isEmpty()) {
                content = extractAttrStr(xml, "sentence", "content");
            }

            // 用 phone_score 作为准确度（read_word 模式无 accuracy_score）
            float accuracy = phonScore;

            Log.d(TAG, "评测解析: total=" + totalScore + " phone=" + phonScore
                    + " fluency=" + fluency + " integrity=" + integrity
                    + " tone=" + toneScore + " content=" + content);

            EvalResult result = new EvalResult(content, totalScore, accuracy, fluency, integrity, xml);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(result);
            });
        } catch (Exception e) {
            Log.e(TAG, "解析XML失败", e);
            EvalResult fallback = new EvalResult("", 0, 0, 0, 0, xml);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(fallback);
            });
        }
    }

    /**
     * 从XML属性中提取float值。
     * 查找 <tagName ... attrName="value" ...> 中的 value。
     * 跳过外层的同名标签（取 rec_paper 下的 read_word）。
     */
    private float extractAttr(String xml, String tagName, String attrName) {
        // 找 rec_paper 内部的 read_word（跳过最外层的 read_word）
        String searchIn = xml;
        if ("read_word".equals(tagName)) {
            int recPaper = xml.indexOf("<rec_paper>");
            if (recPaper >= 0) searchIn = xml.substring(recPaper);
        }

        String pattern = attrName + "=\"";
        int tagStart = searchIn.indexOf("<" + tagName + " ");
        if (tagStart < 0) return 0f;

        int attrStart = searchIn.indexOf(pattern, tagStart);
        if (attrStart < 0) return 0f;

        // 确保这个属性在同一个标签内（在下一个 > 之前）
        int tagEnd = searchIn.indexOf(">", tagStart);
        if (tagEnd >= 0 && attrStart > tagEnd) return 0f;

        int valStart = attrStart + pattern.length();
        int valEnd = searchIn.indexOf("\"", valStart);
        if (valEnd < 0) return 0f;

        try {
            return Float.parseFloat(searchIn.substring(valStart, valEnd).trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private String extractAttrStr(String xml, String tagName, String attrName) {
        String searchIn = xml;
        if ("read_word".equals(tagName)) {
            int recPaper = xml.indexOf("<rec_paper>");
            if (recPaper >= 0) searchIn = xml.substring(recPaper);
        }

        String pattern = attrName + "=\"";
        int tagStart = searchIn.indexOf("<" + tagName + " ");
        if (tagStart < 0) return "";

        int attrStart = searchIn.indexOf(pattern, tagStart);
        if (attrStart < 0) return "";

        int tagEnd = searchIn.indexOf(">", tagStart);
        if (tagEnd >= 0 && attrStart > tagEnd) return "";

        int valStart = attrStart + pattern.length();
        int valEnd = searchIn.indexOf("\"", valStart);
        if (valEnd < 0) return "";

        return searchIn.substring(valStart, valEnd).trim();
    }

    // ==================== 工具方法 ====================

    private String buildAuthUrl() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        String signatureOrigin = "host: " + ISE_HOST + "\n"
                + "date: " + date + "\n"
                + "GET " + ISE_PATH + " HTTP/1.1";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signatureBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP);

        String authorizationOrigin = "api_key=\"" + apiKey
                + "\", algorithm=\"hmac-sha256\""
                + ", headers=\"host date request-line\""
                + ", signature=\"" + signature + "\"";
        String authorization = Base64.encodeToString(
                authorizationOrigin.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        return "wss://" + ISE_HOST + ISE_PATH
                + "?authorization=" + URLEncoder.encode(authorization, "UTF-8")
                + "&date=" + URLEncoder.encode(date, "UTF-8")
                + "&host=" + URLEncoder.encode(ISE_HOST, "UTF-8");
    }

    private void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception ignored) {}
            audioRecord = null;
        }
    }

    private float calcRms(byte[] buf, int len) {
        long sum = 0;
        for (int i = 0; i + 1 < len; i += 2) {
            short s = (short) ((buf[i + 1] << 8) | (buf[i] & 0xFF));
            sum += (long) s * s;
        }
        double rms = Math.sqrt((double) sum / (len / 2));
        return (float) (20 * Math.log10(rms + 1));
    }

    private void notifyError(String msg) {
        mainHandler.post(() -> {
            if (callback != null) callback.onError(msg);
        });
    }
}
