-- Chinese Game MySQL schema
-- Source of truth:
--   app/src/main/java/com/example/chinese_game/MYsqliteopenhelper.java
-- SQLite DB version: 15
--
-- Usage in MySQL Workbench:
-- 1. Execute this script
-- 2. Database -> Reverse Engineer
-- 3. Select schema: chinese_game
--
-- Note:
-- game_question_details.question_id is a business reference only.
-- It is intentionally not declared as a foreign key because its meaning
-- varies by game type in the current application logic.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS chinese_game;
CREATE DATABASE chinese_game
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE chinese_game;

CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    password VARCHAR(32) NOT NULL,
    email VARCHAR(64) NULL,
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_date DATETIME NULL,
    login_streak INT NOT NULL DEFAULT 0,
    total_games_played INT NOT NULL DEFAULT 0,
    total_score INT NOT NULL DEFAULT 0,
    avatar_path VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_name (name)
) ENGINE=InnoDB COMMENT='Registered users';

CREATE TABLE achievements (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    description TEXT NULL,
    icon_path VARCHAR(255) NULL,
    type VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    required_value INT NOT NULL DEFAULT 0,
    reward_points INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Achievement definitions';

CREATE TABLE sentences (
    id INT NOT NULL AUTO_INCREMENT,
    sentence VARCHAR(1024) NOT NULL,
    sentence_hash CHAR(64) AS (SHA2(sentence, 256)) STORED,
    pinyin VARCHAR(2048) NULL,
    difficulty VARCHAR(16) NOT NULL DEFAULT 'EASY',
    category VARCHAR(32) NULL,
    word_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sentences_sentence_hash (sentence_hash)
) ENGINE=InnoDB COMMENT='Sentence corpus';

CREATE TABLE game_words (
    id INT NOT NULL AUTO_INCREMENT,
    word VARCHAR(128) NOT NULL,
    pinyin VARCHAR(255) NULL,
    pos_tag VARCHAR(8) NOT NULL DEFAULT 'X',
    difficulty VARCHAR(16) NOT NULL DEFAULT 'EASY',
    hint TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_words_word_difficulty (word, difficulty)
) ENGINE=InnoDB COMMENT='Standalone vocabulary pool used by game runtime';

CREATE TABLE user_achievements (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    achievement_id INT NOT NULL,
    unlocked_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    progress_value INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_achievement (user_id, achievement_id),
    CONSTRAINT fk_user_achievements_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_achievements_achievement
        FOREIGN KEY (achievement_id) REFERENCES achievements(id)
) ENGINE=InnoDB COMMENT='Achievement progress per user';

CREATE TABLE game_scores (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    game_type VARCHAR(32) NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    score INT NOT NULL,
    max_possible_score INT NOT NULL DEFAULT 0,
    accuracy DOUBLE NOT NULL DEFAULT 0.0,
    time_spent INT NOT NULL DEFAULT 0,
    play_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_game_scores_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='Game session summary';

CREATE TABLE sentence_words (
    id INT NOT NULL AUTO_INCREMENT,
    sentence_id INT NOT NULL,
    word VARCHAR(128) NOT NULL,
    pinyin VARCHAR(255) NULL,
    pos_tag VARCHAR(8) NOT NULL,
    word_order INT NOT NULL,
    word_position INT NOT NULL DEFAULT 0,
    word_difficulty VARCHAR(16) NOT NULL DEFAULT 'EASY',
    word_frequency INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sentence_order (sentence_id, word_order),
    CONSTRAINT fk_sentence_words_sentence
        FOREIGN KEY (sentence_id) REFERENCES sentences(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Tokenized words for each sentence';

CREATE TABLE word_puzzle (
    id INT NOT NULL AUTO_INCREMENT,
    sentence_id INT NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    hint TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_word_puzzle_sentence
        FOREIGN KEY (sentence_id) REFERENCES sentences(id)
) ENGINE=InnoDB COMMENT='Word puzzle question bank';

CREATE TABLE character_matching (
    id INT NOT NULL AUTO_INCREMENT,
    word_id INT NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    hint TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_character_matching_word
        FOREIGN KEY (word_id) REFERENCES game_words(id)
) ENGINE=InnoDB COMMENT='Character matching question bank';

CREATE TABLE pronunciation_quiz (
    id INT NOT NULL AUTO_INCREMENT,
    word_id INT NOT NULL,
    audio_path VARCHAR(255) NULL,
    difficulty VARCHAR(16) NOT NULL,
    hint TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pronunciation_quiz_word
        FOREIGN KEY (word_id) REFERENCES game_words(id)
) ENGINE=InnoDB COMMENT='Pronunciation quiz question bank';

CREATE TABLE game_question_details (
    id INT NOT NULL AUTO_INCREMENT,
    game_score_id INT NOT NULL,
    question_order INT NOT NULL,
    question_id INT NOT NULL COMMENT 'Logical reference only, no physical FK',
    user_answer TEXT NULL,
    is_correct TINYINT(1) NOT NULL DEFAULT 0,
    time_spent INT NOT NULL DEFAULT 0,
    question_score INT NOT NULL DEFAULT 0,
    max_score INT NOT NULL DEFAULT 10,
    PRIMARY KEY (id),
    CONSTRAINT fk_game_question_details_score
        FOREIGN KEY (game_score_id) REFERENCES game_scores(id)
) ENGINE=InnoDB COMMENT='Per-question result rows within a game session';

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
CREATE INDEX idx_sentence_words_word ON sentence_words(word);

CREATE INDEX idx_character_matching_word_id ON character_matching(word_id);
CREATE INDEX idx_character_matching_difficulty ON character_matching(difficulty);

CREATE INDEX idx_pronunciation_quiz_word_id ON pronunciation_quiz(word_id);
CREATE INDEX idx_pronunciation_quiz_difficulty ON pronunciation_quiz(difficulty);

CREATE INDEX idx_word_puzzle_sentence_id ON word_puzzle(sentence_id);
CREATE INDEX idx_word_puzzle_difficulty ON word_puzzle(difficulty);

CREATE INDEX idx_game_words_difficulty ON game_words(difficulty);
CREATE INDEX idx_game_words_pos_tag ON game_words(pos_tag);
CREATE INDEX idx_game_words_word ON game_words(word);

CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_achievement_id ON user_achievements(achievement_id);

SET FOREIGN_KEY_CHECKS = 1;
