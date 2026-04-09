# 科大讯飞SparkChain SDK集成指南

## 📋 概述

本项目已集成科大讯飞SparkChain大模型语音识别SDK，提供更准确的中文语音识别能力。

## 🎯 SparkChain SDK优势

相比传统语音识别SDK，SparkChain具有以下优势：

1. **更高的识别准确率**：基于大模型技术，识别效果更好
2. **自动方言识别**：无需切换方言参数，自动识别普通话和方言
3. **更好的噪音处理**：在嘈杂环境下也能保持较高识别率
4. **智能标点预测**：自动添加标点符号，识别结果更自然
5. **数字规整**：自动将文字数字转换为阿拉伯数字

## 📦 SDK文件结构

```
SparkChain_Android_SDK_2.0.1_rc1/
├── Demo/                          # 官方示例Demo
├── SDK/
│   └── SparkChain.aar            # SDK核心库文件
├── ReleaseNotes.txt              # 版本更新日志
└── SparkChain 大模型识别 Android SDK集成文档.pdf
```

## 🔧 集成步骤

### 1. 复制SDK文件

将`SparkChain.aar`复制到项目的`app/libs/`目录：

```
Chinese_game/
└── app/
    └── libs/
        └── SparkChain.aar  ✅ 放在这里
```

### 2. 配置Gradle依赖

在`app/build.gradle.kts`中添加：

```kotlin
dependencies {
    // 科大讯飞SparkChain语音识别SDK
    implementation(files("libs/SparkChain.aar"))
}
```

### 3. 配置权限

在`AndroidManifest.xml`中添加必需权限：

```xml
<!-- 必需权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Android 10及以上设备（可选） -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

<!-- Application配置 -->
<application
    android:requestLegacyExternalStorage="true"
    ...>
</application>
```

### 4. 配置混淆规则

在`proguard-rules.pro`中添加：

```proguard
-keep class com.iflytek.sparkchain.** {*;}
-keep class com.iflytek.sparkchain.**
```

### 5. 初始化SDK

在`ChineseGameApplication.java`中初始化：

```java
import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;

public class ChineseGameApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initSparkChain();
    }
    
    private void initSparkChain() {
        SparkChainConfig config = SparkChainConfig.builder()
                .appID("your_app_id")
                .apiKey("your_api_key")
                .apiSecret("your_api_secret");
        
        int ret = SparkChain.getInst().init(getApplicationContext(), config);
        
        if (ret == 0) {
            Log.i("App", "SparkChain SDK初始化成功");
        } else {
            Log.e("App", "SparkChain SDK初始化失败，错误码: " + ret);
        }
    }
}
```

## 🔑 配置APPID信息

在`IFlyTekConfig.java`中配置您的应用信息：

```java
public class IFlyTekConfig {
    public static final String APP_ID = "f55d1648";
    public static final String API_KEY = "c37bb52e7c6914dcf549fc154fa68113";
    public static final String API_SECRET = "ZDAyMDFiZjAyOTZlNWIxYzRmNmFjY2Ri";
}
```

## 💻 使用示例

### 基本使用

```java
// 1. 创建语音识别Helper
SpeechRecognitionHelper speechHelper = new SpeechRecognitionHelper(context);

// 2. 配置参数
SpeechRecognitionConfig config = SpeechRecognitionConfig.createChineseConfig();
speechHelper.initialize(config);

// 3. 开始识别
speechHelper.startRecognition(new SpeechRecognitionListener() {
    @Override
    public void onRecognitionSuccess(SpeechRecognitionResult result) {
        String text = result.getRecognizedText();
        Log.d("Speech", "识别结果: " + text);
    }
    
    @Override
    public void onRecognitionError(int errorCode, String errorMessage) {
        Log.e("Speech", "识别错误: " + errorMessage);
    }
    
    @Override
    public void onPartialResults(String partialText) {
        Log.d("Speech", "部分结果: " + partialText);
    }
});

// 4. 停止识别
speechHelper.stopRecognition();
```

### 在发音游戏中使用

```java
public class GameActivity extends AppCompatActivity {
    private SpeechRecognitionHelper speechHelper;
    
    private void initializeSpeechRecognition() {
        speechHelper = new SpeechRecognitionHelper(this);
        SpeechRecognitionConfig config = SpeechRecognitionConfig.createChineseConfig();
        speechHelper.initialize(config);
    }
    
    private void startRecording() {
        speechHelper.startRecognition(new SpeechRecognitionListener() {
            @Override
            public void onRecognitionSuccess(SpeechRecognitionResult result) {
                String recognizedText = result.getRecognizedText();
                handleRecognitionResult(recognizedText);
            }
            
            @Override
            public void onRecognitionError(int errorCode, String errorMessage) {
                Toast.makeText(GameActivity.this, 
                    "识别失败: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```

## 🎨 SparkChain特性配置

### 中文语音大模型

```java
ASR asr = new ASR("zh_cn", "slm", "mandarin");
asr.language("zh_cn");      // 中文
asr.domain("slm");          // 大模型识别
asr.accent("mandarin");     // 普通话
asr.ptt(true);              // 开启标点符号
asr.nunum(true);            // 数字规整
asr.vadEos(600);            // 尾静音截断时间（毫秒）
```

### 多语种大模型（可选）

```java
ASR asr = new ASR("mul_cn", "slm", "mandarin");
asr.ln("none");  // 免切模式，自动识别语种
// 或指定语种：asr.ln("zh"); asr.ln("en"); 等
```

## 📊 识别流程

```
1. 初始化SDK (Application.onCreate)
   ↓
2. 创建ASR实例
   ↓
3. 配置识别参数
   ↓
4. 注册回调监听
   ↓
5. 启动会话 (asr.start)
   ↓
6. 送入音频数据 (asr.write)
   ↓
7. 接收识别结果 (onResult回调)
   ↓
8. 停止会话 (asr.stop)
```

## ⚠️ 注意事项

### 1. 音频格式要求

- **采样率**：16000Hz（推荐）或8000Hz
- **编码格式**：PCM（raw）、speex、speex-wb、lame（mp3）
- **声道**：单声道（推荐）
- **位深**：16bit

### 2. 数据发送建议

- 每次发送间隔：40ms
- PCM格式：每次发送1280字节
- Speex格式：根据压缩等级发送对应字节数

### 3. 会话限制

- 单次会话最长60秒
- 超过10秒未发送数据会自动断开
- 建议及时停止会话释放资源

### 4. 权限处理

在Android 6.0+设备上，需要动态申请录音权限：

```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
}
```

### 5. 网络要求

SparkChain SDK需要网络连接才能工作，请确保：
- 设备已连接网络
- 应用具有网络访问权限
- 防火墙未阻止SDK的网络请求

## 🐛 常见错误码

| 错误码 | 说明 | 解决方案 |
|-------|------|---------|
| 18301 | SDK未初始化 | 检查Application中是否正确初始化 |
| 18702 | 授权失败 | 检查APPID、APIKey、APISecret是否正确 |
| 18700/18701 | 网络错误 | 检查网络连接 |
| 10114 | 应用未授权 | 确认APPID是否正确，是否开通服务 |
| 10160 | 网络超时 | 检查网络状况，重试 |

完整错误码列表请参考：[讯飞开放平台文档](https://www.xfyun.cn/doc/spark/%E5%A4%A7%E6%A8%A1%E5%9E%8B%E8%AF%86%E5%88%AB.html#_15-%E9%94%99%E8%AF%AF%E7%A0%81)

## 📚 相关文档

- [SparkChain官方文档](https://www.xfyun.cn/doc/spark/%E5%A4%A7%E6%A8%A1%E5%9E%8B%E8%AF%86%E5%88%AB.html)
- [语音识别框架文档](./SPEECH_RECOGNITION_README.md)
- [发音游戏集成文档](./PRONUNCIATION_QUIZ_INTEGRATION.md)

## 🔄 从旧版本迁移

如果您之前使用的是`Msc.jar`（旧版SDK），迁移到SparkChain非常简单：

1. 删除`libs/Msc.jar`和`jniLibs/`下的SO文件
2. 添加`libs/SparkChain.aar`
3. 更新`build.gradle.kts`依赖配置
4. 更新`ChineseGameApplication`初始化代码
5. 代码中的其他部分无需修改（已通过`SpeechRecognitionService`接口封装）

## 🎉 完成

集成完成后，您的应用将拥有强大的中文语音识别能力！

如有问题，请参考：
- SDK包中的官方Demo
- 讯飞开放平台文档
- 或提交工单到讯飞开放平台
