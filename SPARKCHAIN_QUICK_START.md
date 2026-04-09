# SparkChain SDK 快速开始指南 🚀

## 📋 3步完成集成

### 步骤1️⃣: 复制SDK文件

将`SparkChain.aar`复制到项目的`app/libs/`目录：

```bash
# 从您的SparkChain SDK包复制
SparkChain_Android_SDK_2.0.1_rc1/SDK/SparkChain.aar
    ↓
app/libs/SparkChain.aar  ✅
```

**验证**: 确认文件已存在
```bash
# Windows
dir "app\libs\SparkChain.aar"

# Mac/Linux
ls -l app/libs/SparkChain.aar
```

### 步骤2️⃣: 同步Gradle

在Android Studio中：
1. 点击顶部工具栏的 **"Sync Project with Gradle Files"** 图标 🔄
2. 等待同步完成（通常需要几秒到几分钟）
3. 确认没有错误提示

### 步骤3️⃣: 运行应用

1. 连接Android设备或启动模拟器
2. 点击 **Run** 按钮 ▶️
3. 等待应用安装并启动

## ✅ 验证集成

### 检查初始化日志

运行应用后，在Logcat中搜索 `ChineseGameApplication`，应该看到：

```
I/ChineseGameApplication: 科大讯飞SparkChain SDK初始化成功, APPID: f55d1648
```

如果看到此日志，说明SDK已成功初始化！✅

### 测试语音识别

1. 打开应用
2. 选择"发音游戏"
3. 点击"开始录音"按钮
4. 对着麦克风说话
5. 点击"停止录音"
6. 查看识别结果

## 🎯 已完成的配置

以下配置已经在代码中完成，您无需手动修改：

### ✅ Gradle依赖配置
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(files("libs/SparkChain.aar"))  ✅
}
```

### ✅ 权限配置
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />  ✅
<uses-permission android:name="android.permission.RECORD_AUDIO" />  ✅
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />  ✅
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />  ✅
```

### ✅ SDK初始化
```java
// ChineseGameApplication.java
private void initSparkChain() {
    SparkChainConfig config = SparkChainConfig.builder()
            .appID("f55d1648")
            .apiKey("c37bb52e7c6914dcf549fc154fa68113")
            .apiSecret("ZDAyMDFiZjAyOTZlNWIxYzRmNmFjY2Ri");
    
    int ret = SparkChain.getInst().init(getApplicationContext(), config);
    // ...
}
```

### ✅ 语音识别服务
```java
// SparkChainSpeechRecognitionService.java
// 已实现完整的语音识别功能  ✅
```

## 🎮 使用发音游戏

### 进入游戏
1. 启动应用
2. 点击"游戏选择"
3. 选择"发音测验"

### 开始游戏
1. 查看屏幕上显示的汉字
2. 点击"开始录音"按钮 🎤
3. 大声朗读汉字
4. 点击"停止录音"按钮 ⏹️
5. 查看识别结果和得分

### 游戏特点
- ✅ 实时语音识别
- ✅ 发音准确度评分
- ✅ 拼音对比显示
- ✅ 三个难度等级（简单/中等/困难）
- ✅ 进度追踪

## 🐛 常见问题

### 问题1: 找不到SparkChain.aar
**解决方案**:
- 确认文件在 `app/libs/` 目录
- 文件名必须是 `SparkChain.aar`（区分大小写）
- 重新同步Gradle

### 问题2: 编译错误 "Cannot find symbol: SparkChain"
**解决方案**:
```bash
# 1. 清理项目
Build → Clean Project

# 2. 重新构建
Build → Rebuild Project

# 3. 同步Gradle
File → Sync Project with Gradle Files
```

### 问题3: SDK初始化失败
**检查清单**:
- ✅ 确认APPID、APIKey、APISecret正确
- ✅ 确认设备已连接网络
- ✅ 查看Logcat中的错误码
- ✅ 参考错误码文档

### 问题4: 无法录音
**解决方案**:
- 确认已授予录音权限
- 检查设备麦克风是否正常
- 在设置中手动授予权限

### 问题5: 识别结果为空
**可能原因**:
- 说话声音太小
- 环境噪音太大
- 网络连接不稳定
- 说话时间太短

## 📱 权限管理

### 运行时权限
首次使用时，应用会请求以下权限：
- 🎤 **录音权限**: 用于语音识别
- 📁 **存储权限**: 用于SDK日志（可选）

请点击"允许"以使用语音识别功能。

### 手动授予权限
如果误点了"拒绝"，可以在设置中手动授予：
```
设置 → 应用 → Chinese Game → 权限 → 麦克风 → 允许
```

## 🔍 调试技巧

### 查看日志
在Android Studio的Logcat中过滤：
```
# 查看SparkChain相关日志
Tag: SparkChainSpeech

# 查看应用初始化日志
Tag: ChineseGameApplication

# 查看游戏活动日志
Tag: GameActivity
```

### 常用日志标签
- `SparkChainSpeech`: SDK操作日志
- `SpeechRecognition`: 识别流程日志
- `GameActivity`: 游戏逻辑日志
- `PronunciationScorer`: 发音评分日志

## 📚 下一步

### 深入了解
- 📖 [SparkChain集成指南](./SPARKCHAIN_INTEGRATION_GUIDE.md) - 详细的SDK配置说明
- 📖 [语音识别框架文档](./SPEECH_RECOGNITION_README.md) - 框架设计和API文档
- 📖 [发音游戏集成文档](./PRONUNCIATION_QUIZ_INTEGRATION.md) - 游戏功能说明

### 自定义开发
- 修改识别参数（VAD、标点、数字规整等）
- 添加新的游戏类型
- 自定义发音评分算法
- 扩展多语种支持

### 官方资源
- 🌐 [讯飞开放平台](https://www.xfyun.cn/)
- 📄 [SparkChain官方文档](https://www.xfyun.cn/doc/spark/大模型识别.html)
- 💬 [技术支持工单](https://console.xfyun.cn/workorder/commit)

## 🎉 完成！

恭喜！您已经成功集成了科大讯飞SparkChain SDK。

现在您可以：
- ✅ 使用高精度的中文语音识别
- ✅ 玩发音测验游戏
- ✅ 基于语音识别开发新功能

祝您使用愉快！🚀

---

**遇到问题？** 请查看 [SPARKCHAIN_INTEGRATION_GUIDE.md](./SPARKCHAIN_INTEGRATION_GUIDE.md) 中的详细说明和错误码参考。
