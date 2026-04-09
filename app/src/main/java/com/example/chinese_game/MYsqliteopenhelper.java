package com.example.chinese_game;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MYsqliteopenhelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "ChineseGame.db";
    private static final int DB_VERSION = 14;

    // 保持数据库连接打开以便App Inspection实时访问
    private static SQLiteDatabase persistentDatabase;

    // 用户表
    private static final String CREATE_USERS =
        "CREATE TABLE users (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name VARCHAR(32) UNIQUE NOT NULL, " +
        "password VARCHAR(32) NOT NULL, " +
        "email VARCHAR(64), " +
        "registration_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "last_login_date DATETIME, " +
        "login_streak INTEGER DEFAULT 0, " +
        "total_games_played INTEGER DEFAULT 0, " +
        "total_score INTEGER DEFAULT 0, " +
        "avatar_path VARCHAR(255)" +
        ")";

    // 成就表
    private static final String CREATE_ACHIEVEMENTS =
        "CREATE TABLE achievements (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name VARCHAR(64) NOT NULL, " +
        "description TEXT, " +
        "icon_path VARCHAR(255), " +
        "type VARCHAR(32) NOT NULL, " +
        "category VARCHAR(32) NOT NULL, " +
        "required_value INTEGER DEFAULT 0, " +
        "reward_points INTEGER DEFAULT 0" +
        ")";

    // 用户成就关联表
    private static final String CREATE_USER_ACHIEVEMENTS =
        "CREATE TABLE user_achievements (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "user_id INTEGER NOT NULL, " +
        "achievement_id INTEGER NOT NULL, " +
        "unlocked_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "progress_value INTEGER DEFAULT 0, " +
        "FOREIGN KEY (user_id) REFERENCES users(id), " +
        "FOREIGN KEY (achievement_id) REFERENCES achievements(id), " +
        "UNIQUE(user_id, achievement_id)" +
        ")";

    // 游戏分数表
    private static final String CREATE_GAME_SCORES =
        "CREATE TABLE game_scores (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "user_id INTEGER NOT NULL, " +
        "game_type VARCHAR(32) NOT NULL, " +
        "difficulty VARCHAR(16) NOT NULL, " +
        "score INTEGER NOT NULL, " +
        "max_possible_score INTEGER DEFAULT 0, " +
        "accuracy REAL DEFAULT 0.0, " +
        "time_spent INTEGER DEFAULT 0, " +
        "play_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "completed INTEGER DEFAULT 1, " +
        "FOREIGN KEY (user_id) REFERENCES users(id)" +
        ")";

    // 句子资源表
    private static final String CREATE_SENTENCES =
        "CREATE TABLE sentences (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "sentence TEXT NOT NULL UNIQUE, " +
        "pinyin TEXT, " +
        "difficulty VARCHAR(16) NOT NULL DEFAULT 'EASY', " +
        "category VARCHAR(32), " +
        "word_count INTEGER DEFAULT 0" +
        ")";

    // 分词结果表
    private static final String CREATE_SENTENCE_WORDS =
        "CREATE TABLE sentence_words (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "sentence_id INTEGER NOT NULL, " +
        "word TEXT NOT NULL, " +
        "pinyin TEXT, " +
        "pos_tag VARCHAR(8) NOT NULL, " +
        "word_order INTEGER NOT NULL, " +
        "word_position INTEGER DEFAULT 0, " +
        "word_difficulty VARCHAR(16) DEFAULT 'EASY', " +
        "word_frequency INTEGER DEFAULT 0, " +
        "FOREIGN KEY (sentence_id) REFERENCES sentences(id) ON DELETE CASCADE, " +
        "UNIQUE(sentence_id, word_order)" +
        ")";

    // 游戏题目详情表
    private static final String CREATE_GAME_QUESTION_DETAILS =
        "CREATE TABLE game_question_details (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "game_score_id INTEGER NOT NULL, " +
        "question_order INTEGER NOT NULL, " +
        "question_id INTEGER NOT NULL, " +
        "user_answer TEXT, " +
        "is_correct INTEGER DEFAULT 0, " +
        "time_spent INTEGER DEFAULT 0, " +
        "question_score INTEGER DEFAULT 0, " +
        "max_score INTEGER DEFAULT 10, " +
        "FOREIGN KEY (game_score_id) REFERENCES game_scores(id)" +
        ")";

    // 字符匹配游戏表（规范化，引用sentences和sentence_words）
    private static final String CREATE_CHARACTER_MATCHING =
        "CREATE TABLE character_matching (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "sentence_id INTEGER NOT NULL, " +
        "word_id INTEGER NOT NULL, " +
        "difficulty VARCHAR(16) NOT NULL, " +
        "hint TEXT, " +
        "FOREIGN KEY (sentence_id) REFERENCES sentences(id), " +
        "FOREIGN KEY (word_id) REFERENCES sentence_words(id)" +
        ")";

    // 发音测验游戏表（规范化，引用sentences和sentence_words）
    private static final String CREATE_PRONUNCIATION_QUIZ =
        "CREATE TABLE pronunciation_quiz (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "sentence_id INTEGER NOT NULL, " +
        "word_id INTEGER NOT NULL, " +
        "audio_path VARCHAR(255), " +
        "difficulty VARCHAR(16) NOT NULL, " +
        "hint TEXT, " +
        "FOREIGN KEY (sentence_id) REFERENCES sentences(id), " +
        "FOREIGN KEY (word_id) REFERENCES sentence_words(id)" +
        ")";

    // 字词谜题游戏表（规范化，仅引用句子）
    private static final String CREATE_WORD_PUZZLE =
        "CREATE TABLE word_puzzle (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "sentence_id INTEGER NOT NULL, " +
        "difficulty VARCHAR(16) NOT NULL, " +
        "hint TEXT, " +
        "FOREIGN KEY (sentence_id) REFERENCES sentences(id)" +
        ")";

    // 前两个游戏专用词表（Character Matching / Pronunciation Quiz）
    private static final String CREATE_GAME_WORDS =
        "CREATE TABLE game_words (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "word TEXT NOT NULL, " +
        "pinyin TEXT, " +
        "pos_tag VARCHAR(8) NOT NULL DEFAULT 'X', " +
        "difficulty VARCHAR(16) NOT NULL DEFAULT 'EASY', " +
        "hint TEXT, " +
        "UNIQUE(word, difficulty)" +
        ")";

    public MYsqliteopenhelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        android.util.Log.i("DatabaseHelper", "Database helper created with name: " + DB_NAME + ", version: " + DB_VERSION);
    }

    /**
     * 获取持久数据库连接。运行期间保持打开，便于 App Inspection 随时访问。
     * 注意：返回的 db 不要调用 close()，否则会关闭全局连接导致 App Inspection 无法访问。
     */
    public synchronized SQLiteDatabase getPersistentDatabase() {
        if (persistentDatabase == null || !persistentDatabase.isOpen()) {
            persistentDatabase = getWritableDatabase();
            android.util.Log.i("DatabaseHelper", "Persistent database connection opened for App Inspection");
        }
        return persistentDatabase;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        android.util.Log.i("DatabaseHelper", "Creating database tables...");
        db.execSQL(CREATE_USERS);
        android.util.Log.i("DatabaseHelper", "Created users table");
        db.execSQL(CREATE_ACHIEVEMENTS);
        android.util.Log.i("DatabaseHelper", "Created achievements table");
        db.execSQL(CREATE_USER_ACHIEVEMENTS);
        android.util.Log.i("DatabaseHelper", "Created user_achievements table");
        db.execSQL(CREATE_GAME_SCORES);
        android.util.Log.i("DatabaseHelper", "Created game_scores table");
        db.execSQL(CREATE_SENTENCES);
        android.util.Log.i("DatabaseHelper", "Created sentences table");
        db.execSQL(CREATE_SENTENCE_WORDS);
        android.util.Log.i("DatabaseHelper", "Created sentence_words table");
        db.execSQL(CREATE_GAME_QUESTION_DETAILS);
        android.util.Log.i("DatabaseHelper", "Created game_question_details table");
        db.execSQL(CREATE_CHARACTER_MATCHING);
        android.util.Log.i("DatabaseHelper", "Created character_matching table");
        db.execSQL(CREATE_PRONUNCIATION_QUIZ);
        android.util.Log.i("DatabaseHelper", "Created pronunciation_quiz table");
        db.execSQL(CREATE_WORD_PUZZLE);
        android.util.Log.i("DatabaseHelper", "Created word_puzzle table");
        db.execSQL(CREATE_GAME_WORDS);
        android.util.Log.i("DatabaseHelper", "Created game_words table");
        // 创建索引（与数据库文档保持一致）
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_name ON users(name)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_scores_user_id ON game_scores(user_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_scores_game_type ON game_scores(game_type)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_scores_difficulty ON game_scores(difficulty)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_scores_play_date ON game_scores(play_date)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_question_details_game_score_id ON game_question_details(game_score_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_question_details_question_id ON game_question_details(question_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sentences_difficulty ON sentences(difficulty)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sentences_category ON sentences(category)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sentences_word_count ON sentences(word_count)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sentence_words_sentence_id ON sentence_words(sentence_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sentence_words_pos_tag ON sentence_words(pos_tag)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sentence_words_word_difficulty ON sentence_words(word_difficulty)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sentence_words_word ON sentence_words(word)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_character_matching_sentence_id ON character_matching(sentence_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_character_matching_word_id ON character_matching(word_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_character_matching_difficulty ON character_matching(difficulty)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pronunciation_quiz_sentence_id ON pronunciation_quiz(sentence_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pronunciation_quiz_word_id ON pronunciation_quiz(word_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pronunciation_quiz_difficulty ON pronunciation_quiz(difficulty)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_word_puzzle_sentence_id ON word_puzzle(sentence_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_word_puzzle_difficulty ON word_puzzle(difficulty)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_words_difficulty ON game_words(difficulty)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_words_pos_tag ON game_words(pos_tag)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_words_word ON game_words(word)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements(user_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_achievements_achievement_id ON user_achievements(achievement_id)");
        android.util.Log.i("DatabaseHelper", "Database creation completed");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        android.util.Log.i("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        // 对于任何版本升级，都重新创建所有表
        // 这样可以确保数据库结构是最新的
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS achievements");
        db.execSQL("DROP TABLE IF EXISTS user_achievements");
        db.execSQL("DROP TABLE IF EXISTS game_scores");
        db.execSQL("DROP TABLE IF EXISTS game_question_details");
        db.execSQL("DROP TABLE IF EXISTS sentence_words");
        db.execSQL("DROP TABLE IF EXISTS sentences");
        db.execSQL("DROP TABLE IF EXISTS character_matching");
        db.execSQL("DROP TABLE IF EXISTS pronunciation_quiz");
        db.execSQL("DROP TABLE IF EXISTS word_puzzle");
        db.execSQL("DROP TABLE IF EXISTS game_words");

        // 重新创建所有表
        onCreate(db);
        android.util.Log.i("DatabaseHelper", "Database upgrade completed");
    }


}
