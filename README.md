# CatLingo 软件说明

CatLingo 是一个 Android 中文学习游戏应用，包含用户登录、三类中文练习游戏、发音测验、成绩记录、成就系统和本地 SQLite 数据管理。

## 主要功能

| 功能 | 说明 |
| --- | --- |
| 用户系统 | 支持注册、登录、资料查看与修改 |
| 游戏选择 | 支持 `EASY`, `MEDIUM`, `HARD` 三个难度 |
| 字形匹配 | 从词汇池中出题，记录答题结果和得分 |
| 发音测验 | 使用讯飞 SparkChain 相关 SDK 和语音评测逻辑进行中文发音练习 |
| 句子拼图 | 基于句子和分词数据进行拖拽式组句练习 |
| 游戏记录 | 保存每局汇总成绩和逐题明细 |
| 成就系统 | 根据用户进度和游戏表现解锁成就 |
| 数据重载 | 可从 `assets/json` 重新导入初始数据 |
| 背景音乐 | 应用前后台自动控制播放状态，支持开关 |

## 技术栈

| 项目 | 当前配置 |
| --- | --- |
| 语言 | Java |
| 构建系统 | Gradle / Android Gradle Plugin |
| `minSdk` | 24 |
| `compileSdk` | 36 |
| `targetSdk` | 36 |
| Java 版本 | 11 |
| 本地数据库 | SQLite |
| 中文分词 | HanLP portable 1.8.6 |
| 拼音转换 | pinyin4j 2.5.0 |
| 语音能力 | SparkChain AAR, Codec AAR, OkHttp, Gson |

## 项目结构

```text
Chinese_game/
├─ app/
│  ├─ libs/                         # SparkChain.aar 和 Codec.aar
│  └─ src/main/
│     ├─ assets/json/               # 初始 JSON 数据
│     ├─ java/com/example/chinese_game/
│     │  ├─ dao/                    # SQLite 数据访问层
│     │  ├─ javabean/               # 数据模型
│     │  ├─ speech/                 # 语音识别与评测相关代码
│     │  ├─ utils/                  # 分词、拼音、数据导入等工具
│     │  └─ view/                   # 自定义视图
│     └─ res/                       # 页面布局、图标、音频等资源
├─ sql/
│  └─ chinese_game_mysql_schema.sql # MySQL ER 图辅助脚本
├─ Database_Documentation.md        # 数据库说明
└─ README.md                        # 软件说明
```

## 运行方式

1. 使用 Android Studio 打开项目根目录。
2. 确认 `app/libs/` 下存在 `SparkChain.aar` 和 `Codec.aar`。
3. 在 `app/src/main/java/com/example/chinese_game/speech/IFlyTekConfig.java` 中配置讯飞开放平台应用信息。
4. 同步 Gradle。
5. 连接 Android 设备或启动模拟器后运行 `app`。

也可以在命令行构建调试包：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 使用流程

1. 打开应用后注册或登录用户。
2. 如需重置测试数据，在登录页点击 Reload Data。
3. 进入游戏选择页，选择字形匹配、发音测验或句子拼图。
4. 选择难度并开始游戏。
5. 游戏结束后，可在菜单中查看用户资料、成就和历史记录。

## 数据库

本项目的数据库说明集中保留在 [Database_Documentation.md](./Database_Documentation.md)。数据库结构以 `MYsqliteopenhelper.java` 为准，MySQL Workbench 建模使用 `sql/chinese_game_mysql_schema.sql`。

维护数据库时需要同步更新代码、SQL 脚本和数据库说明文档。
