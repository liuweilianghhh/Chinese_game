-- MySQL 兼容建表脚本（由项目 SQLite 结构转换，用于在 MySQL Workbench 中生成 ER 图）
-- 使用：在 MySQL 中执行此脚本创建 schema，然后在 Workbench 中 Database -> Reverse Engineer 选择该 schema

SET NAMES utf8mb4;
CREATE SCHEMA IF NOT EXISTS chinese_game DEFAULT CHARACTER SET utf8mb4;
USE chinese_game;

-- 用户表
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(32) UNIQUE NOT NULL,
    password VARCHAR(32) NOT NULL,
    email VARCHAR(64),
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_date DATETIME,
    login_streak INT DEFAULT 0,
    total_games_played INT DEFAULT 0,
    total_score INT DEFAULT 0,
    avatar_path VARCHAR(255)
);

-- 成就表
CREATE TABLE achievements (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    icon_path VARCHAR(255),
    type VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    required_value INT DEFAULT 0,
    reward_points INT DEFAULT 0
);

-- 用户成就关联表
CREATE TABLE user_achievements (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    achievement_id INT NOT NULL,
    unlocked_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    progress_value INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (achievement_id) REFERENCES achievements(id),
    UNIQUE KEY uk_user_achievement (user_id, achievement_id)
);

-- 游戏分数表
CREATE TABLE game_scores (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    game_type VARCHAR(32) NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    score INT NOT NULL,
    max_possible_score INT DEFAULT 0,
    accuracy DOUBLE DEFAULT 0.0,
    time_spent INT DEFAULT 0,
    play_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed INT DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 句子资源表
CREATE TABLE sentences (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sentence TEXT NOT NULL,
    pinyin TEXT,
    difficulty VARCHAR(16) NOT NULL DEFAULT 'EASY',
    category VARCHAR(32),
    word_count INT DEFAULT 0,
    UNIQUE KEY uk_sentence (sentence(255))
);

-- 分词结果表
CREATE TABLE sentence_words (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sentence_id INT NOT NULL,
    word TEXT NOT NULL,
    pinyin TEXT,
    pos_tag VARCHAR(8) NOT NULL,
    word_order INT NOT NULL,
    word_position INT DEFAULT 0,
    word_difficulty VARCHAR(16) DEFAULT 'EASY',
    word_frequency INT DEFAULT 0,
    FOREIGN KEY (sentence_id) REFERENCES sentences(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sentence_order (sentence_id, word_order)
);

-- 游戏题目详情表
CREATE TABLE game_question_details (
    id INT PRIMARY KEY AUTO_INCREMENT,
    game_score_id INT NOT NULL,
    question_order INT NOT NULL,
    question_id INT NOT NULL,
    user_answer TEXT,
    is_correct INT DEFAULT 0,
    time_spent INT DEFAULT 0,
    question_score INT DEFAULT 0,
    max_score INT DEFAULT 10,
    FOREIGN KEY (game_score_id) REFERENCES game_scores(id)
);

-- 字符匹配游戏表
CREATE TABLE character_matching (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sentence_id INT NOT NULL,
    word_id INT NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    hint TEXT,
    FOREIGN KEY (sentence_id) REFERENCES sentences(id),
    FOREIGN KEY (word_id) REFERENCES sentence_words(id)
);

-- 发音测验游戏表
CREATE TABLE pronunciation_quiz (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sentence_id INT NOT NULL,
    word_id INT NOT NULL,
    audio_path VARCHAR(255),
    difficulty VARCHAR(16) NOT NULL,
    hint TEXT,
    FOREIGN KEY (sentence_id) REFERENCES sentences(id),
    FOREIGN KEY (word_id) REFERENCES sentence_words(id)
);

-- 字词谜题游戏表
CREATE TABLE word_puzzle (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sentence_id INT NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    hint TEXT,
    FOREIGN KEY (sentence_id) REFERENCES sentences(id)
);

-- 索引（与项目一致）
CREATE INDEX idx_users_name ON users(name);
CREATE INDEX idx_game_scores_user_id ON game_scores(user_id);
CREATE INDEX idx_game_scores_game_type ON game_scores(game_type);
CREATE INDEX idx_game_scores_difficulty ON game_scores(difficulty);
CREATE INDEX idx_game_scores_play_date ON game_scores(play_date);
CREATE INDEX idx_game_question_details_game_score_id ON game_question_details(game_score_id);
CREATE INDEX idx_game_question_details_question_id ON game_question_details(question_id);
CREATE INDEX idx_sentences_difficulty ON sentences(difficulty);
CREATE INDEX idx_sentences_category ON sentences(category);
CREATE INDEX idx_sentences_word_count ON sentences(word_count);
CREATE INDEX idx_sentence_words_sentence_id ON sentence_words(sentence_id);
CREATE INDEX idx_sentence_words_pos_tag ON sentence_words(pos_tag);
CREATE INDEX idx_sentence_words_word_difficulty ON sentence_words(word_difficulty);
CREATE INDEX idx_sentence_words_word ON sentence_words(word(64));
CREATE INDEX idx_character_matching_sentence_id ON character_matching(sentence_id);
CREATE INDEX idx_character_matching_word_id ON character_matching(word_id);
CREATE INDEX idx_character_matching_difficulty ON character_matching(difficulty);
CREATE INDEX idx_pronunciation_quiz_sentence_id ON pronunciation_quiz(sentence_id);
CREATE INDEX idx_pronunciation_quiz_word_id ON pronunciation_quiz(word_id);
CREATE INDEX idx_pronunciation_quiz_difficulty ON pronunciation_quiz(difficulty);
CREATE INDEX idx_word_puzzle_sentence_id ON word_puzzle(sentence_id);
CREATE INDEX idx_word_puzzle_difficulty ON word_puzzle(difficulty);
CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_achievement_id ON user_achievements(achievement_id);
