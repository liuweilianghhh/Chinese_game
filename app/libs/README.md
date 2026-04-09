# 科大讯飞SparkChain SDK文件放置说明

## 📁 此目录用途

此目录用于存放科大讯飞SparkChain语音识别SDK的AAR文件。

## ⚠️ 重要：必须添加的文件

请将以下文件放到此目录：

```
libs/
└── SparkChain.aar  ← 请将讯飞SparkChain SDK的AAR文件放在这里
```

## 📥 如何获取SDK文件

### 方法1: 从项目中复制

如果您已经下载了`SparkChain_Android_SDK_2.0.1_rc1`文件夹：

```bash
# 从SDK包复制到libs目录
SparkChain_Android_SDK_2.0.1_rc1/SDK/SparkChain.aar
    ↓ 复制到 ↓
app/libs/SparkChain.aar
```

### 方法2: 从官网下载

#### 步骤1: 访问讯飞开放平台
访问: https://www.xfyun.cn/

#### 步骤2: 登录账号
使用您的账号登录

#### 步骤3: 下载SparkChain SDK
1. 进入"控制台"
2. 选择"SparkChain大模型识别"
3. 点击"SDK下载"
4. 选择"Android SDK"
5. 下载SDK包

#### 步骤4: 解压并复制
1. 解压下载的SDK包
2. 进入 `SDK/` 目录
3. 找到 `SparkChain.aar` 文件
4. 复制到此目录（`app/libs/`）

## ✅ 验证文件

完成后，此目录应该包含：
- `SparkChain.aar` - 讯飞SparkChain大模型识别SDK

可以通过以下命令验证：
```bash
# Windows PowerShell
dir SparkChain.aar

# Linux/Mac
ls -l SparkChain.aar
```

## 🔧 同步Gradle

添加文件后，在Android Studio中：
1. 点击 "Sync Project with Gradle Files"
2. 等待同步完成

## 🎯 SparkChain SDK优势

相比旧版Msc.jar，SparkChain具有以下优势：

- ✅ **更高识别率**: 基于大模型技术，识别准确率显著提升
- ✅ **自动方言识别**: 无需切换方言参数，自动识别普通话和方言
- ✅ **更好噪音处理**: 在嘈杂环境下也能保持较高识别率
- ✅ **智能标点预测**: 自动添加标点符号，识别结果更自然
- ✅ **数字规整**: 自动将文字数字转换为阿拉伯数字
- ✅ **简化集成**: 不需要额外的SO库文件（jniLibs）

## 📝 注意事项

1. **文件名**: 必须是 `SparkChain.aar`，不要改名
2. **位置**: 必须在 `app/libs/` 目录
3. **版本**: 当前使用版本 2.0.1_rc1
4. **大小**: AAR文件通常在几MB左右
5. **不需要SO文件**: SparkChain SDK已包含所有必需组件

## 🚫 不要提交到Git

此目录已添加到 `.gitignore`，SDK文件不会被提交到版本控制。

## ❓ 常见问题

### Q: 找不到SparkChain.aar文件？
A: 在下载的SDK包中，通常在 `SDK/` 目录下

### Q: 编译时提示找不到SparkChain类？
A: 
1. 确认文件在正确位置（`app/libs/SparkChain.aar`）
2. 同步Gradle（Sync Project with Gradle Files）
3. 清理并重新构建项目（Build → Clean Project → Rebuild Project）

### Q: 还需要Msc.jar和SO文件吗？
A: **不需要！** SparkChain SDK是新版本，已经包含了所有必需组件。如果之前有Msc.jar和jniLibs目录，可以删除。

### Q: 如何从旧版Msc.jar迁移？
A: 
1. 删除 `libs/Msc.jar`
2. 删除 `src/main/jniLibs/` 目录（如果有）
3. 添加 `libs/SparkChain.aar`
4. 更新 `build.gradle.kts` 依赖配置
5. 代码无需修改（已通过接口封装）

## 📚 相关文档

- 详细集成指南: [SPARKCHAIN_INTEGRATION_GUIDE.md](../../../SPARKCHAIN_INTEGRATION_GUIDE.md)
- 语音识别框架: [SPEECH_RECOGNITION_README.md](../../../SPEECH_RECOGNITION_README.md)
- 快速开始: [START_HERE_IFLYTEK.md](../../../START_HERE_IFLYTEK.md)

## 📖 官方文档

- SparkChain官方文档: https://www.xfyun.cn/doc/spark/大模型识别.html
- 讯飞开放平台: https://www.xfyun.cn/

---

**配置完成后，即可使用科大讯飞SparkChain大模型语音识别功能！**
