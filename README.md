# Chinese Learning Game 🎮

一个基于Android的中文学习游戏应用，集成了科大讯飞SparkChain大模型语音识别技术。

## ✨ 功能特性

- 🎤 **高精度语音识别**: 基于SparkChain大模型，识别准确率~95%
- 🎯 **发音测验游戏**: 实时评估中文发音准确度
- 📊 **智能评分**: 多维度发音评分算法
- 🎨 **三个难度等级**: 简单、中等、困难

## 🚀 快速开始

### 1. 添加SDK文件

将 `SparkChain.aar` 复制到 `app/libs/` 目录：

```bash
SparkChain_Android_SDK_2.0.1_rc1/SDK/SparkChain.aar → app/libs/SparkChain.aar
```

### 2. 同步Gradle

在Android Studio中点击 "Sync Project with Gradle Files"

### 3. 运行应用

连接设备或启动模拟器，点击 Run 按钮

**详细步骤**: [START_HERE.md](./START_HERE.md)

## 📚 文档

| 文档 | 说明 |
|------|------|
| [START_HERE.md](./START_HERE.md) | 快速开始（3步完成） |
| [SPARKCHAIN_QUICK_START.md](./SPARKCHAIN_QUICK_START.md) | SparkChain SDK快速开始 |
| [SPARKCHAIN_INTEGRATION_GUIDE.md](./SPARKCHAIN_INTEGRATION_GUIDE.md) | SparkChain SDK详细集成指南 |
| [SPEECH_RECOGNITION_README.md](./SPEECH_RECOGNITION_README.md) | 语音识别框架文档 |
| [PRONUNCIATION_QUIZ_INTEGRATION.md](./PRONUNCIATION_QUIZ_INTEGRATION.md) | 发音游戏集成文档 |
| [Database_Documentation.md](./Database_Documentation.md) | 数据库文档 |
| [JSON_Import_Examples.md](./JSON_Import_Examples.md) | JSON数据导入示例 |

## 🏗️ 项目结构

```
Chinese_game/
├── app/
│   ├── src/main/java/com/example/chinese_game/
│   │   ├── speech/              # 语音识别框架
│   │   ├── utils/               # 工具类
│   │   ├── dao/                 # 数据访问层
│   │   ├── javabean/            # 数据模型
│   │   └── GameActivity.java   # 游戏主Activity
│   ├── libs/
│   │   └── SparkChain.aar      # 讯飞SDK（需手动添加）
│   └── build.gradle.kts
├── START_HERE.md               # 快速开始
├── README.md                   # 本文件
└── ...
```

## 🎮 使用说明

### 发音游戏

1. 启动应用 → 选择"游戏选择" → 点击"发音测验"
2. 查看屏幕上的汉字和拼音
3. 点击"开始录音" 🎤 → 大声朗读 → 点击"停止录音" ⏹️
4. 查看识别结果和评分

## 🔧 技术栈

- **语言**: Java
- **SDK**: SparkChain Android SDK 2.0.1 RC1
- **依赖**: HanLP (中文分词)、Pinyin4j (拼音转换)
- **最低版本**: Android 5.0 (API 21)
- **目标版本**: Android 14 (API 34)

## 🔑 配置APPID

在 `IFlyTekConfig.java` 中配置您的讯飞应用信息：

```java
public class IFlyTekConfig {
    public static final String APP_ID = "your_app_id";
    public static final String API_KEY = "your_api_key";
    public static final String API_SECRET = "your_api_secret";
}
```

获取APPID: [讯飞开放平台](https://www.xfyun.cn/)

## ❓ 常见问题

| 问题 | 解决方案 |
|------|---------|
| 找不到SparkChain.aar | 确认文件在 `app/libs/` 目录，同步Gradle |
| SDK初始化失败 | 检查APPID配置和网络连接 |
| 无法录音 | 授予录音权限 |
| 识别结果为空 | 检查网络、音量、噪音 |

详细问题排查: [SPARKCHAIN_INTEGRATION_GUIDE.md](./SPARKCHAIN_INTEGRATION_GUIDE.md)

## 📊 性能指标

| 指标 | 数值 |
|------|------|
| 识别准确率 | ~95% |
| 识别延迟 | ~400ms |
| 内存占用 | ~20MB |

## 🔒 权限说明

- ✅ **RECORD_AUDIO**: 录音识别（必需）
- ✅ **INTERNET**: 网络请求（必需）
- ✅ **READ/WRITE_EXTERNAL_STORAGE**: SDK日志（必需）

## 📞 获取帮助

- 📖 [SparkChain官方文档](https://www.xfyun.cn/doc/spark/大模型识别.html)
- 🏠 [讯飞开放平台](https://www.xfyun.cn/)
- 🎫 [工单系统](https://console.xfyun.cn/workorder/commit)

## 🙏 致谢

- [科大讯飞](https://www.xfyun.cn/) - SparkChain语音识别SDK
- [HanLP](https://github.com/hankcs/HanLP) - 中文自然语言处理
- [Pinyin4j](https://github.com/belerweb/pinyin4j) - 汉字转拼音

---

**Made with ❤️ for Chinese learners**
