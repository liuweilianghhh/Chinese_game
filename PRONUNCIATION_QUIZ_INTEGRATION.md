# 发音测验游戏集成文档

## 概述

发音测验游戏已成功集成到项目中，使用语音识别框架实现中文发音练习和评估功能。

## 已完成的功能

### 1. 语音识别框架
- ✅ 完整的语音识别服务接口和实现
- ✅ 支持中文和英文识别
- ✅ 实时音量监测
- ✅ 部分识别结果支持
- ✅ 完善的错误处理

### 2. 发音评分系统
- ✅ 简单匹配评分
- ✅ 编辑距离评分（Levenshtein距离）
- ✅ 拼音匹配评分
- ✅ 综合评分算法
- ✅ 评分等级系统（A+到F）

### 3. 游戏逻辑
- ✅ 题目加载和管理
- ✅ 语音录制和识别
- ✅ 答案评判
- ✅ 分数计算
- ✅ 游戏进度保存
- ✅ 结果统计

### 4. 用户界面
- ✅ 发音测验专用UI布局
- ✅ 录音按钮（开始/停止）
- ✅ 识别状态显示
- ✅ 实时反馈
- ✅ 结果展示

### 5. 数据管理
- ✅ 发音测验数据表
- ✅ DAO实现
- ✅ JSON数据导入
- ✅ 30个示例词汇（简单/中等/困难）

## 使用方法

### 启动发音测验游戏

从游戏选择界面点击"Pronunciation Quiz"按钮，选择难度后即可开始。

### 游戏流程

1. **查看题目**
   - 屏幕显示中文词语
   - 显示拼音辅助
   - 可选提示信息

2. **录制发音**
   - 点击"🎤 Start Recording"按钮
   - 对着麦克风朗读词语
   - 点击"⏹ Stop"停止录音（可选，会自动停止）

3. **查看结果**
   - 系统自动识别并评分
   - 显示识别的文本
   - 显示正确/错误反馈
   - 自动进入下一题

4. **完成游戏**
   - 完成10道题目后显示总分
   - 显示正确率
   - 保存成绩到数据库

## 权限要求

应用需要以下权限（已在AndroidManifest.xml中配置）：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

首次使用时，应用会请求录音权限。

## 数据导入

### 方法1：使用现有数据

已提供30个示例词汇，分为三个难度等级：
- **EASY**: 10个基础词汇（你好、谢谢等）
- **MEDIUM**: 10个中级词汇（学习、工作等）
- **HARD**: 10个高级词汇（环境、发展等）

### 方法2：导入自定义数据

在`app/src/main/assets/json/pronunciation_quiz.json`中添加数据：

```json
{
  "word": "词语",
  "pinyin": "cí yǔ",
  "audio_path": "",
  "difficulty": "EASY",
  "hint": "提示信息"
}
```

然后在代码中调用导入方法：

```java
PronunciationQuizDao dao = DaoFactory.getPronunciationQuizDao(context);
int count = dao.importFromAssetsJson(context);
```

## 评分算法

### 当前实现

GameActivity中使用简单匹配算法：

```java
boolean correct = recognizedText.contains(targetWord);
```

### 高级评分（可选）

使用`PronunciationScorer`类实现更精确的评分：

```java
PronunciationScorer.ScoreResult result = 
    PronunciationScorer.comprehensiveScore(
        recognizedText, 
        targetWord, 
        targetPinyin, 
        confidence
    );

int score = result.getScore();
boolean isCorrect = result.isCorrect();
String feedback = result.getFeedback();
```

支持的评分方法：
1. **simpleMatch**: 简单包含匹配
2. **editDistanceScore**: 基于编辑距离
3. **pinyinMatchScore**: 基于拼音相似度
4. **comprehensiveScore**: 综合多种算法

## 自定义配置

### 修改识别语言

```java
SpeechRecognitionConfig config = new SpeechRecognitionConfig.Builder()
    .setLanguage(SpeechRecognitionConfig.LANGUAGE_ZH_CN)
    .setMaxResults(5)
    .setPartialResults(true)
    .build();

speechHelper.initialize(config);
```

### 调整题目数量

在`GameActivity.java`中修改常量：

```java
private static final int QUESTIONS_PER_GAME = 10;  // 改为其他数量
```

### 修改评分标准

在`GameActivity.handleRecognitionResult()`方法中修改判断逻辑：

```java
// 当前：简单包含匹配
boolean correct = recognizedText.contains(targetWord);

// 改为：使用高级评分
PronunciationScorer.ScoreResult scoreResult = 
    PronunciationScorer.comprehensiveScore(
        recognizedText, targetWord, targetPinyin, result.getConfidence()
    );
boolean correct = scoreResult.isCorrect();
```

## 故障排除

### 1. 语音识别不可用

**问题**: 点击录音按钮没有反应

**解决方案**:
- 检查设备是否安装Google语音识别服务
- 确认已授予录音权限
- 检查网络连接（在线识别需要网络）

### 2. 识别准确率低

**问题**: 识别结果不准确

**解决方案**:
- 在安静环境中录音
- 靠近麦克风清晰朗读
- 调整识别语言设置
- 考虑使用离线识别模型

### 3. 没有题目数据

**问题**: 提示"Not enough questions"

**解决方案**:
```java
// 在Application或MainActivity中导入数据
PronunciationQuizDao dao = DaoFactory.getPronunciationQuizDao(this);
int count = dao.importFromAssetsJson(this);
Log.i("DataImport", "Imported " + count + " pronunciation quiz questions");
```

### 4. 权限被拒绝

**问题**: 用户拒绝录音权限

**解决方案**:
- 在`onRequestPermissionsResult`中提示用户
- 引导用户到设置中手动开启权限
- 提供无需录音的替代游戏模式

## 性能优化建议

### 1. 预加载数据

在游戏选择界面预加载题目数据：

```java
// 在game_choice.java中
new Thread(() -> {
    List<PronunciationQuiz> questions = 
        pronunciationQuizDao.getRandomData(difficulty, 10);
    // 缓存到内存
}).start();
```

### 2. 优化识别超时

调整静音超时时间：

```java
SpeechRecognitionConfig config = new SpeechRecognitionConfig.Builder()
    .setSilenceTimeout(2000)  // 2秒静音后自动停止
    .build();
```

### 3. 减少UI更新

使用Handler延迟更新UI，避免频繁刷新：

```java
private Handler uiHandler = new Handler(Looper.getMainLooper());
uiHandler.postDelayed(() -> {
    // 更新UI
}, 100);
```

## 扩展功能建议

### 1. 发音评分可视化

显示详细的发音评分：
- 声调准确度
- 韵母准确度
- 声母准确度
- 整体流畅度

### 2. 录音回放

保存用户录音，允许回放对比：

```java
// 保存录音文件
String audioPath = saveRecording(audioData);
pronunciationQuestion.setAudioPath(audioPath);

// 回放录音
MediaPlayer player = new MediaPlayer();
player.setDataSource(audioPath);
player.start();
```

### 3. 进度追踪

记录用户在每个词语上的练习次数和进步：

```java
// 添加练习历史表
CREATE TABLE pronunciation_history (
    id INTEGER PRIMARY KEY,
    user_id INTEGER,
    word_id INTEGER,
    attempt_count INTEGER,
    best_score INTEGER,
    last_practice_date DATETIME
);
```

### 4. 社交功能

- 排行榜
- 好友对战
- 分享成绩

### 5. 离线模式

集成离线语音识别引擎：
- 百度语音识别SDK
- 讯飞语音识别SDK
- Google离线语音包

## 文件清单

### 核心文件

```
speech/
├── SpeechRecognitionService.java          # 服务接口
├── AndroidSpeechRecognitionService.java   # Android实现
├── SpeechRecognitionListener.java         # 监听器接口
├── SpeechRecognitionResult.java           # 结果模型
├── SpeechRecognitionConfig.java           # 配置类
├── SpeechRecognitionManager.java          # 管理器
└── SpeechRecognitionHelper.java           # 辅助类

utils/
└── PronunciationScorer.java               # 评分算法

dao/
├── PronunciationQuizDao.java              # DAO接口
└── PronunciationQuizDaoImpl.java          # DAO实现

javabean/
└── PronunciationQuiz.java                 # 数据模型

GameActivity.java                          # 游戏主Activity

res/layout/
└── activity_game.xml                      # UI布局

assets/json/
└── pronunciation_quiz.json                # 示例数据
```

## 技术栈

- **语音识别**: Android SpeechRecognizer API
- **数据库**: SQLite
- **UI**: Android XML布局
- **评分算法**: Levenshtein距离、拼音匹配
- **拼音转换**: Pinyin4j库

## 测试建议

### 单元测试

```java
@Test
public void testPronunciationScorer() {
    ScoreResult result = PronunciationScorer.simpleMatch(
        "你好世界", "你好", 0.9f
    );
    assertTrue(result.isCorrect());
    assertTrue(result.getScore() >= 70);
}
```

### 集成测试

1. 测试数据导入
2. 测试语音识别权限
3. 测试游戏流程
4. 测试分数保存

### 用户测试

1. 在不同环境中测试识别准确率
2. 测试不同口音的识别效果
3. 收集用户反馈

## 后续开发计划

- [ ] 添加更多词汇和句子
- [ ] 实现高级评分算法
- [ ] 添加发音教学视频
- [ ] 支持方言识别
- [ ] 添加语音波形可视化
- [ ] 实现离线识别
- [ ] 添加成就系统
- [ ] 支持自定义词库

## 联系与支持

如有问题或建议，请查看：
- 语音识别框架文档: `SPEECH_RECOGNITION_README.md`
- 项目主文档: `README.md`
