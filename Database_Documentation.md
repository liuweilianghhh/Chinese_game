# Chinese Game 数据库 README

## 1. 说明

本文档描述当前项目实际使用的数据库结构，基准来源如下：

- `app/src/main/java/com/example/chinese_game/MYsqliteopenhelper.java`
- 数据库名：`ChineseGame.db`
- SQLite 版本：`15`

本 README 以当前运行时建表代码为准，不再沿用旧版设计草稿。

## 2. 数据库概览

当前数据库共 11 张表，分为 4 个模块：

- 用户模块：`users`、`achievements`、`user_achievements`
- 游戏记录模块：`game_scores`、`game_question_details`
- 内容语料模块：`sentences`、`sentence_words`、`game_words`
- 题库模块：`character_matching`、`pronunciation_quiz`、`word_puzzle`

## 3. 表结构

### 3.1 `users`

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

索引与约束：

- `UNIQUE(name)`
- 索引：`idx_users_name`

### 3.2 `achievements`

成就定义表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 成就 ID |
| `name` | VARCHAR(64) | NOT NULL | 成就名 |
| `description` | TEXT | NULL | 成就说明 |
| `icon_path` | VARCHAR(255) | NULL | 图标路径 |
| `type` | VARCHAR(32) | NOT NULL | 成就类型 |
| `category` | VARCHAR(32) | NOT NULL | 成就分类 |
| `required_value` | INTEGER | DEFAULT 0 | 解锁阈值 |
| `reward_points` | INTEGER | DEFAULT 0 | 奖励积分 |

### 3.3 `user_achievements`

用户成就关系表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 行 ID |
| `user_id` | INTEGER | FK, NOT NULL | 用户 ID |
| `achievement_id` | INTEGER | FK, NOT NULL | 成就 ID |
| `unlocked_date` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 解锁时间 |
| `progress_value` | INTEGER | DEFAULT 0 | 当前进度 |

索引与约束：

- FK：`user_id -> users.id`
- FK：`achievement_id -> achievements.id`
- UNIQUE：`(user_id, achievement_id)`
- 索引：`idx_user_achievements_user_id`、`idx_user_achievements_achievement_id`

### 3.4 `game_scores`

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

索引与约束：

- FK：`user_id -> users.id`
- 索引：
  - `idx_game_scores_user_id`
  - `idx_game_scores_game_type`
  - `idx_game_scores_difficulty`
  - `idx_game_scores_play_date`

### 3.5 `game_question_details`

每一局里的逐题作答记录。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 行 ID |
| `game_score_id` | INTEGER | FK, NOT NULL | 所属局 ID |
| `question_order` | INTEGER | NOT NULL | 题目顺序 |
| `question_id` | INTEGER | NOT NULL | 业务题目引用 ID |
| `user_answer` | TEXT | NULL | 用户答案 |
| `is_correct` | INTEGER | DEFAULT 0 | 是否正确 |
| `time_spent` | INTEGER | DEFAULT 0 | 该题用时 |
| `question_score` | INTEGER | DEFAULT 0 | 该题得分 |
| `max_score` | INTEGER | DEFAULT 10 | 该题满分 |

索引与约束：

- FK：`game_score_id -> game_scores.id`
- 索引：
  - `idx_game_question_details_game_score_id`
  - `idx_game_question_details_question_id`

注意：

- `question_id` 不是稳定的物理外键，而是按游戏流程复用的业务引用字段。
- 当前代码中的含义会随玩法变化：
- Character Matching：当前 `GameActivity` 实现里写入的是 `game_words.id`
- Pronunciation Quiz：当前 `GameActivity` 实现里写入的是 `game_words.id`
- Word Puzzle：当前 `GameActivity` 实现里写入的是 `sentences.id`
- 因此在 MySQL ER 图脚本中，不应给 `question_id` 强行加外键。

### 3.6 `sentences`

句子语料表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 句子 ID |
| `sentence` | TEXT | NOT NULL, UNIQUE | 原句 |
| `sentence_hash` | CHAR(64) | MySQL 生成列，用于唯一约束 | 句子哈希 |
| `pinyin` | TEXT | NULL | 整句拼音 |
| `difficulty` | VARCHAR(16) | NOT NULL, DEFAULT `'EASY'` | 难度 |
| `category` | VARCHAR(32) | NULL | 分类 |
| `word_count` | INTEGER | DEFAULT 0 | 分词数量 |

索引与约束：

- SQLite 运行库中为 `UNIQUE(sentence)`
- MySQL ER 脚本中改为 `UNIQUE(sentence_hash)`，避免 `utf8mb4` 下长字符串唯一索引超出 3072 字节限制
- 索引：
  - `idx_sentences_difficulty`
  - `idx_sentences_category`
  - `idx_sentences_word_count`

### 3.7 `sentence_words`

句子分词结果表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 词元 ID |
| `sentence_id` | INTEGER | FK, NOT NULL | 所属句子 |
| `word` | TEXT | NOT NULL | 词语 |
| `pinyin` | TEXT | NULL | 词语拼音 |
| `pos_tag` | VARCHAR(8) | NOT NULL | 词性 |
| `word_order` | INTEGER | NOT NULL | 句内顺序 |
| `word_position` | INTEGER | DEFAULT 0 | 在原句中的位置 |
| `word_difficulty` | VARCHAR(16) | DEFAULT `'EASY'` | 词级难度 |
| `word_frequency` | INTEGER | DEFAULT 0 | 词频 |

索引与约束：

- FK：`sentence_id -> sentences.id ON DELETE CASCADE`
- UNIQUE：`(sentence_id, word_order)`
- 索引：
  - `idx_sentence_words_sentence_id`
  - `idx_sentence_words_pos_tag`
  - `idx_sentence_words_word_difficulty`
  - `idx_sentence_words_word`

### 3.8 `character_matching`

Character Matching 题库表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 题目 ID |
| `word_id` | INTEGER | FK, NOT NULL | 正确目标词 |
| `difficulty` | VARCHAR(16) | NOT NULL | 难度 |
| `hint` | TEXT | NULL | 提示 |

索引与约束：

- FK：`word_id -> game_words.id`
- 索引：
  - `idx_character_matching_word_id`
  - `idx_character_matching_difficulty`

### 3.9 `pronunciation_quiz`

Pronunciation Quiz 题库表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 题目 ID |
| `word_id` | INTEGER | FK, NOT NULL | 目标词 |
| `audio_path` | VARCHAR(255) | NULL | 音频路径 |
| `difficulty` | VARCHAR(16) | NOT NULL | 难度 |
| `hint` | TEXT | NULL | 提示 |

索引与约束：

- FK：`word_id -> game_words.id`
- 索引：
  - `idx_pronunciation_quiz_word_id`
  - `idx_pronunciation_quiz_difficulty`

### 3.10 `word_puzzle`

Word Puzzle 题库表。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 题目 ID |
| `sentence_id` | INTEGER | FK, NOT NULL | 来源句子 |
| `difficulty` | VARCHAR(16) | NOT NULL | 难度 |
| `hint` | TEXT | NULL | 提示 |

索引与约束：

- FK：`sentence_id -> sentences.id`
- 索引：
  - `idx_word_puzzle_sentence_id`
  - `idx_word_puzzle_difficulty`

### 3.11 `game_words`

独立词汇池表，供游戏运行时和种子数据导入流程使用。

这张表存在于当前实际运行数据库中，但旧版 MySQL 脚本没有覆盖它。

| 字段 | 类型 | 约束 | 含义 |
| --- | --- | --- | --- |
| `id` | INTEGER | PK, AUTOINCREMENT | 词条 ID |
| `word` | TEXT | NOT NULL | 词语 |
| `pinyin` | TEXT | NULL | 拼音 |
| `pos_tag` | VARCHAR(8) | NOT NULL, DEFAULT `'X'` | 词性 |
| `difficulty` | VARCHAR(16) | NOT NULL, DEFAULT `'EASY'` | 难度 |
| `hint` | TEXT | NULL | 提示 |

索引与约束：

- UNIQUE：`(word, difficulty)`
- 索引：
  - `idx_game_words_difficulty`
  - `idx_game_words_pos_tag`
  - `idx_game_words_word`

## 4. 核心关系

真实物理外键：

- `user_achievements.user_id -> users.id`
- `user_achievements.achievement_id -> achievements.id`
- `game_scores.user_id -> users.id`
- `game_question_details.game_score_id -> game_scores.id`
- `sentence_words.sentence_id -> sentences.id`
- `character_matching.word_id -> game_words.id`
- `pronunciation_quiz.word_id -> game_words.id`
- `word_puzzle.sentence_id -> sentences.id`

逻辑关系但不是物理外键：

- `game_question_details.question_id`
- `game_words` 与题目生成流程之间的业务关系

## 5. 设计说明

### 5.1 为什么有 `game_words`

`game_words` 是一个独立轻量词汇池，主要用于运行时导入和快速出题。
它和 `sentences`、`sentence_words` 的定位不同：

- `sentences` + `sentence_words`：规范化语料
- `character_matching`、`pronunciation_quiz`：基于 `game_words` 的题库
- `word_puzzle`：基于 `sentences` 的题库
- `game_words`：独立词汇池

### 5.2 为什么 `question_id` 不能建外键

当前项目把一个字段复用成不同玩法下的不同题目引用，所以它不是稳定地指向某一张表。
应用逻辑能处理这种设计，但 ER 图不应该把它错误画成单一外键。

### 5.3 ER 图建模建议

在 MySQL Workbench 里生成 ER 图时：

- 保留所有真实外键
- 不要给 `game_question_details.question_id` 增加外键
- 将 `game_words` 保持为独立表

## 6. 推荐阅读顺序

如果要快速看懂数据库，建议按下面顺序看：

1. `users`
2. `game_scores`
3. `game_question_details`
4. `sentences`
5. `sentence_words`
6. `character_matching`
7. `pronunciation_quiz`
8. `word_puzzle`
9. `game_words`
10. `achievements`
11. `user_achievements`

## 7. MySQL Workbench 使用方式

请使用以下脚本生成 MySQL 结构：

- `sql/chinese_game_mysql_schema.sql`

建议步骤：

1. 打开 MySQL Workbench。
2. 连接你的 MySQL 实例。
3. 新建 SQL 标签页，把 `sql/chinese_game_mysql_schema.sql` 全部粘贴进去。
4. 执行脚本，创建 `chinese_game` schema。
5. 打开 `Database -> Reverse Engineer`。
6. 选择 `chinese_game`。
7. 完成向导后生成 ER 图。

## 8. 后续维护建议

如果后面又调整了数据库结构，这两个文件要一起更新：

- `app/src/main/java/com/example/chinese_game/MYsqliteopenhelper.java`
- `sql/chinese_game_mysql_schema.sql`

否则 README、ER 脚本和应用实际库结构会再次不一致。
