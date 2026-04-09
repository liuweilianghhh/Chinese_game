# 中文学习游戏数据库设计文档

## 详细表结构说明

<img title="" src="file:///D:/微信/xwechat_files/wxid_b5g8nxuxm0fk12_a2ac/temp/RWTemp/2026-01/65effc89b15a1d702888a2636699ac61.jpg" alt="65effc89b15a1d702888a2636699ac61" style="zoom:25%;" data-align="left">

### 1. users (用户表)

**用途**: 存储所有注册用户的基本信息和游戏统计数据

**字段说明**:

- **id**: 用户唯一标识，自增长主键，用于关联其他表
- **name**: 用户选择的用户名，全局唯一且必填
- **password**: 用户登录凭证，应加密存储
- **email**: 用户邮箱，用于密码找回和通知，可选
- **registration_date**: 用户首次注册的时间戳，自动记录
- **last_login_date**: 用户最近一次成功登录的时间
- **login_streak**: 连续登录天数，用于成就系统计算
- **total_games_played**: 用户玩过的游戏总数，用于活跃度统计
- **total_score**: 用户获得的总分数，用于成就解锁
- **avatar_path**: 用户自定义头像的存储路径

**数据示例**:

```json
{
  "id": 1,
  "name": "chinese_learner",
  "password": "hashed_password_here",
  "email": "user@example.com",
  "registration_date": "2024-01-15 10:00:00",
  "last_login_date": "2024-01-20 15:30:00",
  "login_streak": 5,
  "total_games_played": 25,
  "total_score": 1250,
  "avatar_path": "/avatars/user_1.jpg"
}
```

### 2. achievements (成就表)

**用途**: 定义游戏中所有可解锁的成就类型和条件

**字段说明**:

- **id**: 成就的唯一标识
- **name**: 成就的显示名称
- **description**: 成就的详细说明，告诉用户如何获得
- **icon_path**: 成就图标的文件路径
- **type**: 成就类型（MILESTONE里程碑成就/GAME_SPECIFIC游戏特定成就）
- **category**: 成就所属的分类（GENERAL通用/CHARACTER_MATCHING字符匹配/PRONUNCIATION_QUIZ发音测验/WORD_PUZZLE字词谜题/LOGIN_STREAK登录连续性）
- **required_value**: 解锁成就需要达到的值
- **reward_points**: 解锁成就后获得的积分奖励

**数据示例**:

```json
{
  "id": 1,
  "name": "First Steps",
  "description": "完成10个游戏",
  "icon_path": "/icons/first_steps.png",
  "type": "MILESTONE",
  "category": "GENERAL",
  "required_value": 10,
  "reward_points": 50
}
```

### 3. user_achievements (用户成就关联表)

**用途**: 记录用户已解锁的成就和进度状态

**字段说明**:

- **id**: 关联记录的唯一标识
- **user_id**: 关联到users表，标识哪个用户
- **achievement_id**: 关联到achievements表，标识哪个成就
- **unlocked_date**: 用户解锁该成就的时间戳
- **progress_value**: 当前进度值（对于需要累积的成就）

**数据示例**:

```json
{
  "id": 1,
  "user_id": 1,
  "achievement_id": 1,
  "unlocked_date": "2024-01-15 10:00:00",
  "progress_value": 0
}
```

### 4. game_scores (游戏分数表)

**用途**: 记录用户的每次游戏分数和详细表现数据

**字段说明**:

- **id**: 游戏记录的唯一标识
- **user_id**: 关联用户ID
- **game_type**: 游戏类型（CHARACTER_MATCHING/PRONUNCIATION_QUIZ/WORD_PUZZLE）
- **difficulty**: 难度等级（EASY/MEDIUM/HARD）
- **score**: 用户在本次游戏中获得的分数
- **max_possible_score**: 该游戏的最高可能分数
- **accuracy**: 准确率，范围0.0-1.0
- **time_spent**: 完成游戏所用的时间（秒）
- **play_date**: 游戏进行的时间戳
- **completed**: 游戏是否完成（1=完成，0=未完成）

**数据示例**:

```json
{
  "id": 1,
  "user_id": 1,
  "game_type": "CHARACTER_MATCHING",
  "difficulty": "EASY",
  "score": 85,
  "max_possible_score": 100,
  "accuracy": 0.85,
  "time_spent": 120,
  "play_date": "2024-01-20 14:30:00",
  "completed": 1
}
```

### 5. game_question_details (游戏题目详情表)

**用途**: 记录每局游戏中所有题目的详细信息，包括用户答案、答题结果等

**字段说明**:

- **id**: 题目记录的唯一标识
- **game_score_id**: 关联到game_scores表，标识该题目属于哪局游戏。通过game_scores.game_type可以确定question_id关联到哪个题目配置表
- **question_order**: 题目在游戏中的顺序位置（通常每局游戏有10道题）
- **question_id**: 题目配置ID，根据game_scores.game_type不同，关联到：
  - CHARACTER_MATCHING → character_matching.id
  - PRONUNCIATION_QUIZ → pronunciation_quiz.id
  - WORD_PUZZLE → word_puzzle.id
- **user_answer**: 用户的答案，格式根据游戏类型不同：
  - CHARACTER_MATCHING: 用户选择的汉字
  - PRONUNCIATION_QUIZ: 用户发音的拼音（通过语音识别获得）
  - WORD_PUZZLE: 用户重组的句子
- **is_correct**: 是否答对
- **time_spent**: 该题目花费的时间
- **question_score**: 该题目获得的分数（通常答对10分，答错0分，在pronunciation_quiz中，分数不固定）
- **max_score**: 该题目的最高分数（通常为10分）

**外键约束**:

* `game_score_id` → `game_scores.id`

**数据示例**:

```json
[
  {
    "id": 1,
    "game_score_id": 1,
    "question_order": 1,
    "question_id": 5,
    "user_answer": "学习",
    "is_correct": 1,
    "time_spent": 8,
    "question_score": 10,
    "max_score": 10
  },
  {
    "id": 2,
    "game_score_id": 1,
    "question_order": 2,
    "question_id": 12,
    "user_answer": "上海",
    "is_correct": 0,
    "time_spent": 15,
    "question_score": 0,
    "max_score": 10
  },
  {
    "id": 3,
    "game_score_id": 1,
    "question_order": 3,
    "question_id": 8,
    "user_answer": "中文",
    "is_correct": 1,
    "time_spent": 6,
    "question_score": 10,
    "max_score": 10
  }
]
```

---

### 6. character_matching (字符匹配游戏表)

**用途**: 存储字符匹配游戏的题目配置，支持基于sentence_words表自动生成题目。用户看到拼音，需要从4个汉字选项中选择正确的对应词语。

**字段说明**:

- **id**: 题目配置的唯一标识
- **sentence_id**: 关联到sentences表，用于获取句子上下文信息
- **word_id**: 关联到sentence_words表，标识正确答案的词语
- **difficulty**: 题目难度等级，继承自sentence_words.word_difficulty
- **hint**: 基于第三方SDK动态生成，为词语作相关解释

**外键约束**:

* `sentence_id` → `sentences.id`
* `word_id` → `sentence_words.id`

**题目生成逻辑**:

1. 从sentence_words表获取正确答案（word_id对应的word和pinyin）
2. 根据word_id的词性（pos_tag）和难度（word_difficulty），从sentence_words表中筛选相同词性和相似难度的其他词语作为干扰项
3. 随机选择3个干扰项，与正确答案一起组成4个选项

**数据示例**:

```json
{
  "id": 1,
  "sentence_id": 1,
  "word_id": 5,
  "difficulty": "MEDIUM",
  "hint": null
}
```

**动态生成示例**:

- 正确答案：从word_id=5获取 → word="学习", pinyin="xué xí", pos_tag="VV"
- 干扰项：从sentence_words中筛选pos_tag="VV"且word_difficulty="MEDIUM"的其他词
- 最终题目：显示拼音"xué xí"，4个选项为["学习", "工作", "休息", "吃饭"]（随机顺序）

---

### 7. pronunciation_quiz (发音测验游戏表)

**用途**: 存储发音测验游戏的题目配置，支持基于sentence_words表自动生成题目。用户看到词语和句子上下文，进行发音练习。

**字段说明**:

- **id**: 题目配置的唯一标识
- **sentence_id**: 关联到sentences表，用于提供句子上下文，帮助用户理解词语含义
- **word_id**: 关联到sentence_words表，标识要发音练习的词语
- **audio_path**: 标准发音的音频文件路径，可以动态生成或预存
- **difficulty**: 题目难度等级，继承自sentence_words.word_difficulty
- **hint**: 基于第三方SDK动态生成，为词语作相关解释

**外键约束**:

* `sentence_id` → `sentences.id`
* `word_id` → `sentence_words.id`

**题目生成逻辑**:

1. 从sentence_words表获取要发音的词语（word_id对应的word和pinyin）
2. 从sentences表获取完整句子作为上下文
3. 在句子中将目标词语替换为空白，形成填空题形式
4. 提供标准拼音和可选的标准发音音频

**数据示例**:

```json
{
  "id": 1,
  "sentence_id": 1,
  "word_id": 4,
  "audio_path": "/audio/beijing.mp3",
  "difficulty": "MEDIUM",
  "hint": null
}
```

**动态生成示例**:

- 目标词语：从word_id=4获取 → word="北京", pinyin="běi jīng"
- 句子上下文：从sentence_id=1获取 → "我喜欢在___学习中文"
- 显示给用户：词语"北京" + 拼音"běi jīng" + 句子上下文

---

### 8. word_puzzle (字词谜题游戏表)

**用途**: 存储字词谜题游戏的题目配置，支持基于sentences和sentence_words表自动生成题目。用户看到打乱顺序的词语，需要重新排列成正确的句子。

**字段说明**:

- **id**: 题目配置的唯一标识
- **sentence_id**: 关联到sentences表，标识要重组的句子
- **difficulty**: 句子难度等级，继承自sentences.difficulty
- **hint**: 基于第三方SDK动态生成，为句子作相关解释 

**外键约束**:

* `sentence_id` → `sentences.id`

**题目生成逻辑**:

1. 从sentences表获取完整句子（sentence_id对应的sentence）
2. 从sentence_words表获取该句子的所有分词，按word_order排序
3. 将词语顺序随机打乱，显示给用户
4. 用户需要重新排列成正确的句子顺序

**数据示例**:

```json
{
  "id": 1,
  "sentence_id": 1,
  "difficulty": "MEDIUM",
  "hint": null
}
```

**动态生成示例**:

- 完整句子：从sentence_id=1获取 → "我喜欢在北京学习中文"
- 分词列表：从sentence_words表获取 → ["我", "喜欢", "在", "北京", "学习", "中文"]（按word_order排序）
- 打乱后显示：["学习", "我", "中文", "在", "北京", "喜欢"]（随机顺序）
- 用户任务：重新排列成正确顺序

### 9. sentences (句子资源表)

**用途**: 存储基于NLP处理的中文句子资源库，为智能游戏生成提供数据基础

**字段说明**

- **id**: 系统自动生成的句子唯一标识，用于关联其他表
- **sentence**: 完整的中文句子文本，用于NLP处理和游戏生成
- **pinyin**: 完整句子的拼音标注，支持发音学习
- **difficulty**: 句子整体难度等级（EASY/MEDIUM/HARD）
- **category**: 句子分类，便于按主题组织内容。预设分类的前提下，可以通过NLP技术自动识别
- **word_count**: 句子中的词数量，是游戏难度控制决定条件之一

**数据示例**:

```json
{
  "id": 1,
  "sentence": "我喜欢在北京学习中文",
  "pinyin": "wǒ xǐ huan zài běi jīng xué xí zhōng wén",
  "difficulty": "MEDIUM",
  "category": "日常学习",
  "word_count": 6
}
```

**NLP自动识别说明**:

以下字段可以通过NLP技术自动识别和生成：

- **category**: 可以通过文本分类模型（如BERT）、关键词匹配或主题建模自动识别句子类别，如"教育学习"、"商务交流"、"旅游出行"等
- **pinyin**: 可以通过拼音标注工具自动生成完整句子的拼音
- **word_count**: 可以通过分词工具自动统计句子中的词数量

---

### 10. sentence_words (分词结果表)

**用途**: 存储句子经过NLP分词和词性标注后的详细信息

**字段说明**

- **id**: 分词记录的唯一标识
- **sentence_id**: 关联到sentences表，标识词所属的句子
- **word**: 分词后的单个词语，是游戏生成的基本单位
- **pinyin**: 该词的拼音标注，支持发音练习
- **pos_tag**: 词性标签（如NN名词、VV动词、JJ形容词等）
- **word_order**: 词在句子中的顺序位置，用于句子重组
- **word_position**: 词在句子文本中的字符起始位置
- **word_difficulty**: 词的难度等级，便于个性化学习
- **word_frequency**: 词在语料库中的出现频率，反映词的常见程度

**外键约束**:

* `sentence_id` → `sentences.id`

**唯一约束**: `(sentence_id, word_order)` - 确保同一句子中位置唯一

**词性标签说明**:

- **NN**: 普通名词 (book, school)
- **NR**: 专有名词 (China, Zhang San)
- **NT**: 时间名词 (today, morning)
- **VV**: 动词 (learn, eat)
- **JJ**: 形容词 (beautiful, good)
- **AD**: 副词 (very, also)
- **PN**: 代词 (I, you)
- **P**: 介词 (in, at)
- **DEC**: 的 (genitive marker)
- **DEG**: 的 (associative marker)
- **DER**: 得 (potential marker)
- **DEV**: 地 (adverbial marker)
- **AS**: 了/着/过 (aspect particles)

**数据示例**:

基于sentences表中id=1的句子"我喜欢在北京学习中文"，完整的sentence_words记录如下：

```json
[
  {
    "id": 1,
    "sentence_id": 1,
    "word": "我",
    "pinyin": "wǒ",
    "pos_tag": "PN",
    "word_order": 1,
    "word_position": 1,
    "word_difficulty": "EASY",
    "word_frequency": 50000
  },
  {
    "id": 2,
    "sentence_id": 1,
    "word": "喜欢",
    "pinyin": "xǐ huan",
    "pos_tag": "VV",
    "word_order": 2,
    "word_position": 2,
    "word_difficulty": "MEDIUM",
    "word_frequency": 8000
  },
  {
    "id": 3,
    "sentence_id": 1,
    "word": "在",
    "pinyin": "zài",
    "pos_tag": "P",
    "word_order": 3,
    "word_position": 4,
    "word_difficulty": "EASY",
    "word_frequency": 45000
  },
  {
    "id": 4,
    "sentence_id": 1,
    "word": "北京",
    "pinyin": "běi jīng",
    "pos_tag": "NR",
    "word_order": 4,
    "word_position": 5,
    "word_difficulty": "MEDIUM",
    "word_frequency": 12000
  },
  {
    "id": 5,
    "sentence_id": 1,
    "word": "学习",
    "pinyin": "xué xí",
    "pos_tag": "VV",
    "word_order": 5,
    "word_position": 7,
    "word_difficulty": "MEDIUM",
    "word_frequency": 15000
  },
  {
    "id": 6,
    "sentence_id": 1,
    "word": "中文",
    "pinyin": "zhōng wén",
    "pos_tag": "NN",
    "word_order": 6,
    "word_position": 9,
    "word_difficulty": "MEDIUM",
    "word_frequency": 10000
  }
]
```

**示例说明**:

- **句子**: "我喜欢在北京学习中文" (sentence_id = 1)
- **分词结果**: 共6个词，按word_order从1到6排列
- **词性分布**: 
  - PN (代词): 1个 - "我"
  - VV (动词): 2个 - "喜欢", "学习"
  - P (介词): 1个 - "在"
  - NR (专有名词): 1个 - "北京"
  - NN (普通名词): 1个 - "中文"
- **难度分布**: EASY (2个), MEDIUM (4个)
- **word_position说明** (字符索引从1开始): 
  - "我" 在位置1 (第1个字符)
  - "喜欢" 在位置2 (第2-3个字符: 喜、欢)
  - "在" 在位置4 (第4个字符，前面"我喜欢"共3个字符)
  - "北京" 在位置5 (第5-6个字符: 北、京，前面"我喜欢在"共4个字符)
  - "学习" 在位置7 (第7-8个字符: 学、习，前面"我喜欢在北京"共6个字符)
  - "中文" 在位置9 (第9-10个字符: 中、文，前面"我喜欢在北京学习"共8个字符)

---

## 游戏机制与表关系

### Character Matching游戏

- **游戏规则**: 用户看到拼音标注，需要从4个汉字选项中选择正确的对应词语
- **数据来源**: 主要使用character_matching表，基于sentence_words表动态生成
- **关联表**: 通过sentence_id可关联到sentences表获取上下文

### Pronunciation Quiz游戏

- **游戏规则**: 用户看到词语和句子上下文，进行发音练习，系统评分
- **数据来源**: 主要使用pronunciation_quiz表，基于sentence_words表筛选合适词语
- **关联表**: 通过sentence_id可关联到sentences表获取完整句子

### Word Puzzle游戏

- **游戏规则**: 用户看到打乱顺序的词语，需要重新排列成正确的句子
- **数据来源**: 主要使用word_puzzle表，基于sentences和sentence_words表动态生成
- **关联表**: 直接关联sentences表获取原始句子和分词信息

## 词性标注体系

采用CTB (Chinese Penn Treebank) 词性标注标准：

- **NN**: 普通名词 (book, school)
- **NR**: 专有名词 (China, Zhang San)
- **NT**: 时间名词 (today, morning)
- **VV**: 动词 (learn, eat)
- **JJ**: 形容词 (beautiful, good)
- **AD**: 副词 (very, also)
- **PN**: 代词 (I, you)
- **P**: 介词 (in, at)
- **DEC**: 的 (genitive marker)
- **DEG**: 的 (associative marker)
- **DER**: 得 (potential marker)
- **DEV**: 地 (adverbial marker)
- **AS**: 了/着/过 (aspect particles)

## 难度分级体系

### 句子级别难度

- **EASY**: 基础词汇，简单句式，日常话题
- **MEDIUM**: 中等词汇，复合句式，常见话题
- **HARD**: 专业词汇，复杂句式，特定领域话题

### 词语级别难度

- **EASY**: 高频基础词，短词，简单词性
- **MEDIUM**: 中频常用词，中等长度，常见词性
- **HARD**: 低频生僻词，长词，复杂词性
