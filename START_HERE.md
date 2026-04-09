# 🚀 快速开始

## ⚡ 3步完成集成

### 步骤 1️⃣: 添加SDK文件

将 `SparkChain.aar` 复制到 `app/libs/` 目录：

```bash
SparkChain_Android_SDK_2.0.1_rc1/SDK/SparkChain.aar
    ↓ 复制到 ↓
app/libs/SparkChain.aar
```

### 步骤 2️⃣: 同步Gradle

在Android Studio中点击 **"Sync Project with Gradle Files"** 🔄

### 步骤 3️⃣: 运行应用

点击 **Run** 按钮 ▶️

**完成！** 🎉

---

## 📚 文档导航

| 文档 | 说明 |
|------|------|
| [README.md](./README.md) | 项目介绍和功能说明 |
| [SPARKCHAIN_QUICK_START.md](./SPARKCHAIN_QUICK_START.md) | SparkChain SDK快速开始 |
| [SPARKCHAIN_INTEGRATION_GUIDE.md](./SPARKCHAIN_INTEGRATION_GUIDE.md) | SparkChain SDK详细集成指南 |
| [SPEECH_RECOGNITION_README.md](./SPEECH_RECOGNITION_README.md) | 语音识别框架文档 |
| [PRONUNCIATION_QUIZ_INTEGRATION.md](./PRONUNCIATION_QUIZ_INTEGRATION.md) | 发音游戏集成文档 |

---

## ✅ 验证集成

运行应用后，在Logcat中搜索 `ChineseGameApplication`，应该看到：

```
I/ChineseGameApplication: 科大讯飞SparkChain SDK初始化成功, APPID: f55d1648
```

---

## ❓ 常见问题

### Q: 找不到SparkChain.aar？
**A**: 在SDK包的 `SDK/` 目录下

### Q: 编译错误？
**A**: 确认文件在 `app/libs/` 目录，然后同步Gradle

### Q: SDK初始化失败？
**A**: 检查网络连接和APPID配置

---

**详细文档请查看 [README.md](./README.md)**
