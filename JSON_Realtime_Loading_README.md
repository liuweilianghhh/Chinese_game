# JSON实时数据加载功能

## 概述
应用现在支持从JSON文件实时加载数据。所有表都有对应的JSON文件，可以通过修改这些文件来实时更新应用数据。

## 文件结构

```
app/src/main/assets/json/
├── users.json              # 用户数据
├── achievements.json       # 成就数据
├── user_achievements.json  # 用户成就关联数据
├── game_scores.json        # 游戏分数数据
└── game_data.json          # 游戏题目数据
```

## 功能特性

### 1. 自动初始化
- 应用启动时自动从assets中的JSON文件加载数据
- JSON文件会被复制到内部存储以便实时修改

### 2. 实时重新加载
- 应用运行时可以手动重新加载JSON数据
- 支持检测文件变更并自动重新加载

### 3. 数据完整性
- 支持跳过重复数据导入
- 保持数据库外键约束
- 提供详细的导入日志

## 使用方法

### 1. 修改JSON文件
编辑 `app/src/main/assets/json/` 目录下的相应JSON文件：

```json
// users.json 示例
[
  {
    "name": "new_user",
    "password": "password123",
    "email": "user@example.com",
    "registration_date": "2024-01-20 10:00:00",
    "last_login_date": "2024-01-20 10:00:00",
    "login_streak": 1,
    "total_games_played": 0,
    "total_score": 0,
    "avatar_path": null
  }
]
```

### 2. 实时重新加载
在应用主界面点击 "Reload JSON Data" 按钮，数据会立即重新加载到数据库中。

### 3. 文件位置
- **Assets目录**: `app/src/main/assets/json/` (应用安装时的初始数据)
- **内部存储**: `app/data/data/com.example.chinese_game/files/json/` (运行时可修改的文件)

## 实时监控功能

### DataManager类
提供了完整的数据管理功能：

```java
// 获取单例实例
DataManager dataManager = DataManager.getInstance(context);

// 强制重新加载所有数据
dataManager.forceReloadAllData(new DataManager.DataLoadCallback() {
    @Override
    public void onDataReloaded() {
        // 数据重新加载完成
    }

    @Override
    public void onNoChanges() {
        // 没有检测到变更
    }
});

// 检查并重新加载变更的数据
dataManager.checkAndReloadData(callback);
```

## 开发调试

### App Inspection集成
- 数据库连接保持打开状态，支持App Inspection实时监控
- 可以实时查看数据变更
- 支持SQL查询和数据修改

### 日志监控
应用会输出详细的数据加载日志：

```
ChineseGameApplication: Database connection opened and kept alive for App Inspection
ChineseGameApplication: Imported X users from JSON
DataManager: File users.json has been modified, reloading data...
```

## 数据格式说明

### 通用字段类型
- **字符串**: 使用双引号包围
- **数字**: 直接书写
- **布尔值**: true/false
- **空值**: null
- **日期**: "YYYY-MM-DD HH:mm:ss" 格式

### 枚举值对应
- **GameType**: "CHARACTER_MATCHING", "PRONUNCIATION_QUIZ", "WORD_PUZZLE"
- **Difficulty**: "EASY", "MEDIUM", "HARD"
- **AchievementType**: "MILESTONE", "GAME_SPECIFIC"
- **AchievementCategory**: "GENERAL", "CHARACTER_MATCHING", "PRONUNCIATION_QUIZ", "WORD_PUZZLE", "LOGIN_STREAK"

## 最佳实践

### 1. 数据验证
- 修改JSON文件前先备份
- 确保JSON格式正确（使用JSON验证工具）
- 检查外键约束（用户ID、成就ID等）

### 2. 性能考虑
- 大量数据导入时会阻塞UI线程
- 考虑在后台线程中执行数据导入
- 定期清理不需要的历史数据

### 3. 版本控制
- 将JSON文件纳入版本控制
- 为数据结构变更创建迁移脚本
- 记录数据版本和变更历史

## 故障排除

### 常见问题
1. **JSON解析错误**: 检查JSON格式是否正确
2. **外键约束失败**: 确保关联数据存在
3. **文件访问错误**: 检查文件权限和路径
4. **编码问题**: 确保使用UTF-8编码

### 调试方法
1. 查看Android Studio的Logcat日志
2. 使用App Inspection检查数据库状态
3. 检查内部存储中的JSON文件
4. 验证assets目录中的原始文件

## 扩展功能

### 自定义数据源
可以扩展DataManager支持其他数据源：
- 网络API数据
- 本地文件系统
- 共享偏好设置
- Content Provider

### 增量更新
实现增量数据更新而不是全量重载：
- 比较新旧数据差异
- 只更新变更的部分
- 支持数据版本控制

这个实时JSON加载系统为开发和测试提供了极大的灵活性，可以快速修改和测试不同的数据集。