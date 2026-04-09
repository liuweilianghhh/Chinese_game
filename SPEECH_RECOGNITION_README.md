# 语音识别框架使用文档

## 概述

本项目实现了一个完整的Android语音识别框架，用于第二个游戏（发音测验）的后端支持。该框架基于Android原生的SpeechRecognizer API，提供了简洁易用的接口。

## 架构设计

### 核心组件

1. **SpeechRecognitionService** - 语音识别服务接口
   - 定义了语音识别的核心功能
   - 支持开始、停止、取消识别
   - 支持语言切换

2. **AndroidSpeechRecognitionService** - Android原生实现
   - 实现了SpeechRecognitionService接口
   - 使用Android系统的SpeechRecognizer API
   - 处理识别回调和结果解析

3. **SpeechRecognitionListener** - 识别监听器接口
   - 监听识别的各个阶段
   - 接收识别结果和错误信息
   - 支持实时部分结果

4. **SpeechRecognitionResult** - 识别结果数据模型
   - 封装识别文本、置信度
   - 支持多个候选结果
   - 包含识别耗时信息

5. **SpeechRecognitionConfig** - 识别配置类
   - 配置识别语言（中文/英文）
   - 设置最大结果数量
   - 配置超时和静音检测

6. **SpeechRecognitionManager** - 识别管理器
   - 单例模式管理识别服务
   - 处理权限检查和请求
   - 提供便捷的管理方法

7. **SpeechRecognitionHelper** - 辅助类
   - 简化语音识别的使用
   - 提供简单的回调接口
   - 自动处理权限和初始化

## 使用方法

### 1. 基本使用（使用Helper类）

```java
public class YourActivity extends AppCompatActivity {
    
    private SpeechRecognitionHelper speechHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your);
        
        // 创建Helper
        speechHelper = new SpeechRecognitionHelper(this);
        
        // 初始化（使用中文配置）
        speechHelper.initialize(SpeechRecognitionConfig.createChineseConfig());
        
        // 开始识别
        Button btnStart = findViewById(R.id.btn_start_recognition);
        btnStart.setOnClickListener(v -> {
            speechHelper.startRecognition(new SpeechRecognitionHelper.SimpleCallback() {
                @Override
                public void onSuccess(String recognizedText, float confidence) {
                    // 识别成功
                    Toast.makeText(YourActivity.this, 
                        "识别结果: " + recognizedText, 
                        Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onError(String errorMessage) {
                    // 识别失败
                    Toast.makeText(YourActivity.this, 
                        "错误: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, 
                                          String[] permissions, 
                                          int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SpeechRecognitionManager.REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，可以开始识别
            } else {
                Toast.makeText(this, "需要录音权限", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
```

### 2. 高级使用（使用Manager和Service）

```java
public class AdvancedActivity extends AppCompatActivity {
    
    private SpeechRecognitionManager manager;
    private SpeechRecognitionService service;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced);
        
        // 获取管理器实例
        manager = SpeechRecognitionManager.getInstance();
        manager.initialize(this);
        
        // 创建自定义配置
        SpeechRecognitionConfig config = new SpeechRecognitionConfig.Builder()
                .setLanguage(SpeechRecognitionConfig.LANGUAGE_ZH_CN)
                .setMaxResults(5)
                .setPartialResults(true)
                .setMaxRecordingDuration(30)
                .setSilenceTimeout(3000)
                .build();
        
        // 创建识别服务
        service = manager.createRecognitionService(config);
        
        // 开始识别
        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> startRecognition());
    }
    
    private void startRecognition() {
        // 检查权限
        if (!manager.hasRecordAudioPermission(this)) {
            manager.requestRecordAudioPermission(this);
            return;
        }
        
        // 开始识别
        service.startListening(new SpeechRecognitionListener() {
            @Override
            public void onReadyForSpeech() {
                // 准备就绪
                runOnUiThread(() -> 
                    Toast.makeText(AdvancedActivity.this, 
                        "请开始说话", Toast.LENGTH_SHORT).show());
            }
            
            @Override
            public void onBeginningOfSpeech() {
                // 开始说话
            }
            
            @Override
            public void onEndOfSpeech() {
                // 说话结束
            }
            
            @Override
            public void onRmsChanged(float rmsdB) {
                // 音量变化，可用于显示音量波形
            }
            
            @Override
            public void onRecognitionSuccess(SpeechRecognitionResult result) {
                // 识别成功
                runOnUiThread(() -> {
                    String text = result.getRecognizedText();
                    float confidence = result.getConfidence();
                    Toast.makeText(AdvancedActivity.this,
                        "识别: " + text + " (置信度: " + confidence + ")",
                        Toast.LENGTH_LONG).show();
                    
                    // 显示所有候选结果
                    for (SpeechRecognitionResult.CandidateResult candidate : 
                         result.getCandidates()) {
                        System.out.println("候选: " + candidate.toString());
                    }
                });
            }
            
            @Override
            public void onRecognitionError(int errorCode, String errorMessage) {
                // 识别失败
                runOnUiThread(() -> 
                    Toast.makeText(AdvancedActivity.this,
                        "错误: " + errorMessage,
                        Toast.LENGTH_SHORT).show());
            }
            
            @Override
            public void onPartialResults(String partialResult) {
                // 实时部分结果
                runOnUiThread(() -> 
                    System.out.println("部分结果: " + partialResult));
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manager != null) {
            manager.destroy();
        }
    }
}
```

### 3. 使用SimpleSpeechRecognitionListener适配器

```java
// 只需要实现必要的方法
service.startListening(new SpeechRecognitionManager.SimpleSpeechRecognitionListener() {
    @Override
    public void onRecognitionSuccess(SpeechRecognitionResult result) {
        // 处理成功结果
    }
    
    @Override
    public void onRecognitionError(int errorCode, String errorMessage) {
        // 处理错误
    }
    
    // 其他方法有默认空实现，可选择性重写
});
```

## 配置选项

### 语言配置

```java
// 中文（简体）
config.setLanguage(SpeechRecognitionConfig.LANGUAGE_ZH_CN);

// 英语（美国）
config.setLanguage(SpeechRecognitionConfig.LANGUAGE_EN_US);

// 中文（繁体）
config.setLanguage(SpeechRecognitionConfig.LANGUAGE_ZH_TW);
```

### 其他配置

```java
SpeechRecognitionConfig config = new SpeechRecognitionConfig.Builder()
    .setLanguage("zh-CN")           // 识别语言
    .setMaxResults(5)               // 最大结果数量
    .setPartialResults(true)        // 启用实时部分结果
    .setOfflineMode(false)          // 是否离线识别
    .setMaxRecordingDuration(30)    // 最大录音时长（秒）
    .setSilenceTimeout(3000)        // 静音超时（毫秒）
    .setShowRecognitionDialog(false) // 是否显示系统对话框
    .build();
```

## 权限处理

### AndroidManifest.xml

已自动添加以下权限：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />

<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
</queries>
```

### 运行时权限

```java
// 检查权限
if (!manager.hasRecordAudioPermission(this)) {
    // 请求权限
    manager.requestRecordAudioPermission(this);
}

// 处理权限结果
@Override
public void onRequestPermissionsResult(int requestCode, 
                                      String[] permissions, 
                                      int[] grantResults) {
    if (requestCode == SpeechRecognitionManager.REQUEST_RECORD_AUDIO_PERMISSION) {
        if (grantResults.length > 0 && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予
        }
    }
}
```

## 错误处理

常见错误代码：

- `ERROR_AUDIO` - 音频录制错误
- `ERROR_CLIENT` - 客户端错误
- `ERROR_INSUFFICIENT_PERMISSIONS` - 权限不足
- `ERROR_NETWORK` - 网络错误
- `ERROR_NETWORK_TIMEOUT` - 网络超时
- `ERROR_NO_MATCH` - 无匹配结果
- `ERROR_RECOGNIZER_BUSY` - 识别服务忙
- `ERROR_SERVER` - 服务器错误
- `ERROR_SPEECH_TIMEOUT` - 无语音输入

## 集成到发音测验游戏

在GameActivity中使用语音识别：

```java
public class GameActivity extends AppCompatActivity {
    
    private SpeechRecognitionHelper speechHelper;
    
    private void initializeSpeechRecognition() {
        speechHelper = new SpeechRecognitionHelper(this);
        speechHelper.initialize(SpeechRecognitionConfig.createChineseConfig());
    }
    
    private void startPronunciationQuiz() {
        // 显示题目：汉字和拼音
        String targetWord = "你好";
        String targetPinyin = "nǐ hǎo";
        
        // 开始语音识别
        speechHelper.startRecognition(new SpeechRecognitionHelper.SimpleCallback() {
            @Override
            public void onSuccess(String recognizedText, float confidence) {
                // 比较识别结果和目标词
                boolean correct = recognizedText.contains(targetWord);
                
                if (correct) {
                    // 发音正确
                    showCorrectFeedback(confidence);
                } else {
                    // 发音错误
                    showIncorrectFeedback(recognizedText, targetWord);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(GameActivity.this, 
                    "请重试: " + errorMessage, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```

## 注意事项

1. **权限**: 必须在运行时请求RECORD_AUDIO权限
2. **网络**: 默认使用在线识别，需要网络连接
3. **语言**: 确保设备支持所选语言的语音识别
4. **资源**: 使用完毕后调用destroy()释放资源
5. **线程**: 回调可能在后台线程，UI操作需使用runOnUiThread()

## 扩展性

框架设计支持以下扩展：

1. **自定义识别引擎**: 实现SpeechRecognitionService接口
2. **第三方SDK**: 可集成百度、讯飞等第三方语音识别SDK
3. **离线识别**: 配置offlineMode并集成离线模型
4. **自定义评分**: 基于识别结果实现发音评分算法

## 文件结构

```
com.example.chinese_game.speech/
├── SpeechRecognitionService.java          # 服务接口
├── AndroidSpeechRecognitionService.java   # Android实现
├── SpeechRecognitionListener.java         # 监听器接口
├── SpeechRecognitionResult.java           # 结果数据模型
├── SpeechRecognitionConfig.java           # 配置类
├── SpeechRecognitionManager.java          # 管理器
└── SpeechRecognitionHelper.java           # 辅助类
```

## 下一步计划

1. 集成到PRONUNCIATION_QUIZ游戏中
2. 实现发音评分算法
3. 添加发音反馈和提示
4. 支持更多语言和方言
5. 优化识别准确率
