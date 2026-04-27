# 数据库说明

本文档说明当前 Android 项目实际使用的本地数据库结构。数据库以代码中的建表逻辑为准：

- 运行时数据库文件：`ChineseGame.db`
- SQLite 版本号：`15`
- 建表代码：`app/src/main/java/com/example/chinese_game/MYsqliteopenhelper.java`
- MySQL ER 图辅助脚本：`sql/chinese_game_mysql_schema.sql`
- 初始数据来源：`app/src/main/assets/json/`

## 数据库总览

当前数据库共 11 张表，按职责分为 4 类：

| 模块 | 表 |
| --- | --- |
| 用户与成就 | `users`, `achievements`, `user_achievements` |
| 游戏记录 | `game_scores`, `game_question_details` |
| 内容语料 | `sentences`, `sentence_words`, `game_words` |
| 游戏题库 | `character_matching`, `pronunciation_quiz`, `word_puzzle` |

## 核心关系

实际声明的外键关系如下：

| 从表字段 | 目标表字段 | 说明 |
| --- | --- | --- |
| `user_achievements.user_id` | `users.id` | 用户成就归属用户 |
| `user_achievements.achievement_id` | `achievements.id` | 用户成就对应成就定义 |
| `game_scores.user_id` | `users.id` | 游戏局记录归属用户 |
| `game_question_details.game_score_id` | `game_scores.id` | 单题记录归属一局游戏 |
| `sentence_words.sentence_id` | `sentences.id` | 分词记录归属句子，删除句子时级联删除 |
| `character_matching.word_id` | `game_words.id` | 字形匹配题目引用词汇 |
| `pronunciation_quiz.word_id` | `game_words.id` | 发音测验题目引用词汇 |
| `word_puzzle.sentence_id` | `sentences.id` | 句子拼图题目引用句子 |

`game_question_details.question_id` 是业务引用字段，不是稳定的物理外键。当前代码中它会根据游戏类型指向不同来源：

| 游戏类型 | `question_id` 当前含义 |
| --- | --- |
| `CHARACTER_MATCHING` | `game_words.id` |
| `PRONUNCIATION_QUIZ` | `game_words.id` |
| `WORD_PUZZLE` | `sentences.id` |

因此 ER 图或 MySQL 脚本中不应给 `game_question_details.question_id` 强行添加外键。

## 表结构

### `users`

用户基础信息与累计统计。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 用户 ID |
| `name` | VARCHAR(32) | UNIQUE, NOT NULL | 用户名 |
| `password` | VARCHAR(32) | NOT NULL | 密码 |
| `email` | VARCHAR(64) | NULL | 邮箱 |
| `registration_date` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 注册时间 |
| `last_login_date` | DATETIME | NULL | 最近登录时间 |
| `login_streak` | INTEGER | DEFAULT 0 | 连续登录天数 |
| `total_games_played` | INTEGER | DEFAULT 0 | 累计游戏次数 |
| `total_score` | INTEGER | DEFAULT 0 | 累计得分 |
| `avatar_path` | VARCHAR(255) | NULL | 头像路径 |

### `achievements`

成就定义。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 成就 ID |
| `name` | VARCHAR(64) | NOT NULL | 成就名称 |
| `description` | TEXT | NULL | 成就说明 |
| `icon_path` | VARCHAR(255) | NULL | 图标路径 |
| `type` | VARCHAR(32) | NOT NULL | 成就类型 |
| `category` | VARCHAR(32) | NOT NULL | 成就分类 |
| `required_value` | INTEGER | DEFAULT 0 | 解锁阈值 |
| `reward_points` | INTEGER | DEFAULT 0 | 奖励分数 |

### `user_achievements`

用户与成就的关联和进度。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 记录 ID |
| `user_id` | INTEGER | FK, NOT NULL | 用户 ID |
| `achievement_id` | INTEGER | FK, NOT NULL | 成就 ID |
| `unlocked_date` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 解锁时间 |
| `progress_value` | INTEGER | DEFAULT 0 | 当前进度 |

约束：`UNIQUE(user_id, achievement_id)`。

### `game_scores`

每一局游戏的汇总记录。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 局记录 ID |
| `user_id` | INTEGER | FK, NOT NULL | 用户 ID |
| `game_type` | VARCHAR(32) | NOT NULL | 游戏类型 |
| `difficulty` | VARCHAR(16) | NOT NULL | 难度 |
| `score` | INTEGER | NOT NULL | 本局得分 |
| `max_possible_score` | INTEGER | DEFAULT 0 | 本局满分 |
| `accuracy` | REAL | DEFAULT 0.0 | 正确率 |
| `time_spent` | INTEGER | DEFAULT 0 | 用时 |
| `play_date` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 游戏时间 |
| `completed` | INTEGER | DEFAULT 1 | 是否完成 |

### `game_question_details`

每一局游戏内的逐题作答记录。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 记录 ID |
| `game_score_id` | INTEGER | FK, NOT NULL | 所属局 ID |
| `question_order` | INTEGER | NOT NULL | 题目顺序 |
| `question_id` | INTEGER | NOT NULL | 业务题目引用 |
| `user_answer` | TEXT | NULL | 用户答案 |
| `is_correct` | INTEGER | DEFAULT 0 | 是否正确 |
| `time_spent` | INTEGER | DEFAULT 0 | 单题用时 |
| `question_score` | INTEGER | DEFAULT 0 | 单题得分 |
| `max_score` | INTEGER | DEFAULT 10 | 单题满分 |

### `sentences`

句子语料表，用于句子拼图和分词语料。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 句子 ID |
| `sentence` | TEXT | UNIQUE, NOT NULL | 原句 |
| `pinyin` | TEXT | NULL | 整句拼音 |
| `difficulty` | VARCHAR(16) | NOT NULL, DEFAULT `'EASY'` | 难度 |
| `category` | VARCHAR(32) | NULL | 分类 |
| `word_count` | INTEGER | DEFAULT 0 | 分词数量 |

说明：SQLite 运行库中没有 `sentence_hash` 字段。MySQL ER 图脚本中额外使用 `sentence_hash` 生成列，是为了避免在 `utf8mb4` 下直接给长句子字段建立唯一索引时超过索引长度限制。

### `sentence_words`

句子的分词结果表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 分词记录 ID |
| `sentence_id` | INTEGER | FK, NOT NULL | 所属句子 |
| `word` | TEXT | NOT NULL | 词语 |
| `pinyin` | TEXT | NULL | 词语拼音 |
| `pos_tag` | VARCHAR(8) | NOT NULL | 词性 |
| `word_order` | INTEGER | NOT NULL | 句内顺序 |
| `word_position` | INTEGER | DEFAULT 0 | 在原句中的位置 |
| `word_difficulty` | VARCHAR(16) | DEFAULT `'EASY'` | 词级难度 |
| `word_frequency` | INTEGER | DEFAULT 0 | 词频 |

约束：`UNIQUE(sentence_id, word_order)`。

### `game_words`

独立词汇池，主要供字形匹配和发音测验使用。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 词条 ID |
| `word` | TEXT | NOT NULL | 词语 |
| `pinyin` | TEXT | NULL | 拼音 |
| `pos_tag` | VARCHAR(8) | NOT NULL, DEFAULT `'X'` | 词性 |
| `difficulty` | VARCHAR(16) | NOT NULL, DEFAULT `'EASY'` | 难度 |
| `hint` | TEXT | NULL | 提示 |

约束：`UNIQUE(word, difficulty)`。

### `character_matching`

字形匹配题库。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 题目 ID |
| `word_id` | INTEGER | FK, NOT NULL | 正确目标词 |
| `difficulty` | VARCHAR(16) | NOT NULL | 难度 |
| `hint` | TEXT | NULL | 提示 |

### `pronunciation_quiz`

发音测验题库。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 题目 ID |
| `word_id` | INTEGER | FK, NOT NULL | 目标词 |
| `audio_path` | VARCHAR(255) | NULL | 音频路径 |
| `difficulty` | VARCHAR(16) | NOT NULL | 难度 |
| `hint` | TEXT | NULL | 提示 |

### `word_puzzle`

句子拼图题库。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 题目 ID |
| `sentence_id` | INTEGER | FK, NOT NULL | 来源句子 |
| `difficulty` | VARCHAR(16) | NOT NULL | 难度 |
| `hint` | TEXT | NULL | 提示 |

## 索引

建表后代码会创建以下索引：

| 表 | 索引字段 |
| --- | --- |
| `users` | `name` |
| `game_scores` | `user_id`, `game_type`, `difficulty`, `play_date` |
| `game_question_details` | `game_score_id`, `question_id` |
| `sentences` | `difficulty`, `category`, `word_count` |
| `sentence_words` | `sentence_id`, `pos_tag`, `word_difficulty`, `word` |
| `character_matching` | `word_id`, `difficulty` |
| `pronunciation_quiz` | `word_id`, `difficulty` |
| `word_puzzle` | `sentence_id`, `difficulty` |
| `game_words` | `difficulty`, `pos_tag`, `word` |
| `user_achievements` | `user_id`, `achievement_id` |

## 数据导入流程

登录页的 Reload Data 操作会通过 `DataManager.forceReloadAllData()` 从 `assets/json` 重新导入数据。当前流程如下：

1. 清空所有业务表并重置自增 ID。
2. 导入 `users`, `achievements`, `user_achievements`, `game_scores`。
3. 导入 `game_words`。
4. 导入 `sentences` 和 `sentence_words`。
5. 根据词频更新 `sentence_words.word_frequency` 和 `sentence_words.word_difficulty`。
6. 导入 `character_matching`, `pronunciation_quiz`, `word_puzzle`。

## MySQL ER 图脚本

如果需要在 MySQL Workbench 中查看 ER 图，使用：

```sql
sql/chinese_game_mysql_schema.sql
```

该脚本是为建模展示准备的 MySQL 版本，字段类型会与 SQLite 有少量差异，但关系设计应保持一致。维护时需要同步检查：

- `app/src/main/java/com/example/chinese_game/MYsqliteopenhelper.java`
- `sql/chinese_game_mysql_schema.sql`
- `Database_Documentation.md`

修改数据库结构时，应同时更新数据库版本号，并重新验证 JSON 导入流程。
