# JSON导入功能使用示例

## 概述
所有数据表现在都支持从JSON文件中批量导入数据。以下是各个表的JSON格式示例和使用方法。

## 1. 用户表 (users) - importFromJson()

**JSON格式示例:**
```json
[
  {
    "name": "john_doe",
    "password": "password123",
    "email": "john@example.com",
    "registration_date": "2024-01-15 10:30:00",
    "last_login_date": "2024-01-20 14:20:00",
    "login_streak": 5,
    "total_games_played": 25,
    "total_score": 1250,
    "avatar_path": "/avatars/john.png"
  },
  {
    "name": "jane_smith",
    "password": "secure456",
    "email": "jane@example.com",
    "registration_date": "2024-01-10 09:15:00",
    "last_login_date": "2024-01-19 16:45:00",
    "login_streak": 3,
    "total_games_played": 18,
    "total_score": 980,
    "avatar_path": null
  }
]
```

**使用方法:**
```java
UserDao userDao = new UserDaoImpl(context);
int importedCount = userDao.importFromJson(jsonString);
if (importedCount > 0) {
    Log.i("Import", "成功导入 " + importedCount + " 个用户");
} else {
    Log.e("Import", "导入失败");
}
```

## 2. 成就表 (achievements) - importAchievementsFromJson()

**JSON格式示例:**
```json
[
  {
    "name": "First Steps",
    "description": "完成10个游戏",
    "icon_path": "/icons/first_steps.png",
    "type": "MILESTONE",
    "category": "GENERAL",
    "required_value": 10,
    "reward_points": 50,
    "is_active": true
  },
  {
    "name": "Perfect Match",
    "description": "在Character Matching中连续10次正确",
    "icon_path": "/icons/perfect_match.png",
    "type": "GAME_SPECIFIC",
    "category": "CHARACTER_MATCHING",
    "required_value": 10,
    "reward_points": 75,
    "is_active": true
  }
]
```

## 3. 用户成就表 (user_achievements) - importUserAchievementsFromJson()

**JSON格式示例:**
```json
[
  {
    "user_id": 1,
    "achievement_id": 1,
    "unlocked_date": "2024-01-20 10:00:00",
    "progress_value": 0
  },
  {
    "user_id": 2,
    "achievement_id": 2,
    "unlocked_date": "2024-01-19 15:30:00",
    "progress_value": 5
  }
]
```

## 4. 游戏分数表 (game_scores) - importFromJson()

**JSON格式示例:**
```json
[
  {
    "user_id": 1,
    "game_type": "CHARACTER_MATCHING",
    "difficulty": "EASY",
    "score": 95,
    "max_possible_score": 100,
    "accuracy": 0.95,
    "time_spent": 45000,
    "play_date": "2024-01-20 14:30:00",
    "completed": true,
  },
  {
    "user_id": 2,
    "game_type": "PRONUNCIATION_QUIZ",
    "difficulty": "MEDIUM",
    "score": 85,
    "max_possible_score": 100,
    "accuracy": 0.85,
    "time_spent": 60000,
    "play_date": "2024-01-19 16:20:00",
    "completed": true,
  }
]
```

## 5. 游戏数据表 (game_data) - importFromJson()

**JSON格式示例:**
```json
[
  {
    "game_type": "CHARACTER_MATCHING",
    "difficulty": "EASY",
    "question_data": "{\"character\":\"你\",\"pinyin\":\"nǐ\",\"distractors\":[\"我\",\"他\",\"她\"]}",
    "correct_answer": "nǐ",
    "hint": "第一人称代词",
    "is_active": true,
    "order_index": 1
  },
  {
    "game_type": "PRONUNCIATION_QUIZ",
    "difficulty": "EASY",
    "question_data": "{\"word\":\"你好\",\"pinyin\":\"nǐ hǎo\",\"audioPath\":\"\"}",
    "correct_answer": "nǐ hǎo",
    "hint": "最常见的问候语",
    "is_active": true,
    "order_index": 1
  },
  {
    "game_type": "WORD_PUZZLE",
    "difficulty": "EASY",
    "question_data": "{\"sentence\":\"我喜欢学习中文\",\"words\":[\"我\",\"喜欢\",\"学习\",\"中文\"]}",
    "correct_answer": "我喜欢学习中文",
    "hint": "表达兴趣的句子",
    "is_active": true,
    "order_index": 1
  }
]
```

## 枚举值说明

### GameType (游戏类型)
- `CHARACTER_MATCHING`: 字符匹配游戏
- `PRONUNCIATION_QUIZ`: 发音测验
- `WORD_PUZZLE`: 单词拼图

### Difficulty (难度等级)
- `EASY`: 简单
- `MEDIUM`: 中等
- `HARD`: 困难

### AchievementType (成就类型)
- `MILESTONE`: 里程碑成就
- `GAME_SPECIFIC`: 游戏特定成就

### AchievementCategory (成就分类)
- `GENERAL`: 通用
- `CHARACTER_MATCHING`: 字符匹配
- `PRONUNCIATION_QUIZ`: 发音测验
- `WORD_PUZZLE`: 单词拼图
- `LOGIN_STREAK`: 登录连续

## 使用建议

1. **数据验证**: 导入前确保JSON数据的完整性和正确性
2. **外键约束**: 导入用户成就时，确保user_id和achievement_id存在
3. **重复数据**: 系统会自动跳过重复的数据（如同名用户）
4. **错误处理**: 返回值-1表示JSON解析失败，正数表示成功导入的数量

## 完整使用示例

```java
// 在Activity中导入数据
public void importDataFromJson(String jsonContent, String dataType) {
    try {
        int result = -1;

        switch (dataType) {
            case "users":
                UserDao userDao = new UserDaoImpl(this);
                result = userDao.importFromJson(jsonContent);
                break;
            case "achievements":
                AchievementDao achievementDao = new AchievementDaoImpl(this);
                result = achievementDao.importAchievementsFromJson(jsonContent);
                break;
            case "user_achievements":
                AchievementDao uaDao = new AchievementDaoImpl(this);
                result = uaDao.importUserAchievementsFromJson(jsonContent);
                break;
            case "game_scores":
                GameScoreDao scoreDao = new GameScoreDaoImpl(this);
                result = scoreDao.importFromJson(jsonContent);
                break;
            case "game_data":
                GameDataDao dataDao = new GameDataDaoImpl(this);
                result = dataDao.importFromJson(jsonContent);
                break;
        }

        if (result > 0) {
            Toast.makeText(this, "成功导入 " + result + " 条数据", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show();
        }

    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "导入过程中发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
```