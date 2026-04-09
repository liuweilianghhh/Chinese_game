package com.example.chinese_game.utils;

import android.content.Context;
import com.example.chinese_game.dao.AchievementDao;
import com.example.chinese_game.dao.AchievementDaoImpl;
import com.example.chinese_game.dao.CharacterMatchingDao;
import com.example.chinese_game.dao.CharacterMatchingDaoImpl;
import com.example.chinese_game.dao.GameScoreDao;
import com.example.chinese_game.dao.GameScoreDaoImpl;
import com.example.chinese_game.dao.GameWordDao;
import com.example.chinese_game.dao.GameWordDaoImpl;
import com.example.chinese_game.dao.PronunciationQuizDao;
import com.example.chinese_game.dao.PronunciationQuizDaoImpl;
import com.example.chinese_game.dao.UserDao;
import com.example.chinese_game.dao.UserDaoImpl;
import com.example.chinese_game.dao.WordPuzzleDao;
import com.example.chinese_game.dao.WordPuzzleDaoImpl;
import com.example.chinese_game.dao.SentenceWordDao;
import com.example.chinese_game.dao.SentenceWordDaoImpl;
import com.example.chinese_game.dao.GameQuestionDetailDao;
import com.example.chinese_game.dao.GameQuestionDetailDaoImpl;

/**
 * DAO工厂类，用于创建和管理所有DAO实例
 */
public class DaoFactory {

    private static UserDao userDaoInstance;
    private static AchievementDao achievementDaoInstance;
    private static GameScoreDao gameScoreDaoInstance;
    private static CharacterMatchingDao characterMatchingDaoInstance;
    private static PronunciationQuizDao pronunciationQuizDaoInstance;
    private static WordPuzzleDao wordPuzzleDaoInstance;
    private static GameWordDao gameWordDaoInstance;
    private static SentenceWordDao sentenceWordDaoInstance;
    private static GameQuestionDetailDao gameQuestionDetailDaoInstance;

    /**
     * 获取UserDao实例
     * @param context Android上下文
     * @return UserDao实例
     */
    public static UserDao getUserDao(Context context) {
        if (userDaoInstance == null) {
            userDaoInstance = new UserDaoImpl(context);
        }
        return userDaoInstance;
    }

    /**
     * 获取AchievementDao实例
     * @param context Android上下文
     * @return AchievementDao实例
     */
    public static AchievementDao getAchievementDao(Context context) {
        if (achievementDaoInstance == null) {
            achievementDaoInstance = new AchievementDaoImpl(context);
        }
        return achievementDaoInstance;
    }

    /**
     * 获取GameScoreDao实例
     * @param context Android上下文
     * @return GameScoreDao实例
     */
    public static GameScoreDao getGameScoreDao(Context context) {
        if (gameScoreDaoInstance == null) {
            gameScoreDaoInstance = new GameScoreDaoImpl(context);
        }
        return gameScoreDaoInstance;
    }

    /**
     * 获取CharacterMatchingDao实例
     * @param context Android上下文
     * @return CharacterMatchingDao实例
     */
    public static CharacterMatchingDao getCharacterMatchingDao(Context context) {
        if (characterMatchingDaoInstance == null) {
            characterMatchingDaoInstance = new CharacterMatchingDaoImpl(context);
        }
        return characterMatchingDaoInstance;
    }

    /**
     * 获取PronunciationQuizDao实例
     * @param context Android上下文
     * @return PronunciationQuizDao实例
     */
    public static PronunciationQuizDao getPronunciationQuizDao(Context context) {
        if (pronunciationQuizDaoInstance == null) {
            pronunciationQuizDaoInstance = new PronunciationQuizDaoImpl(context);
        }
        return pronunciationQuizDaoInstance;
    }

    /**
     * 获取WordPuzzleDao实例
     * @param context Android上下文
     * @return WordPuzzleDao实例
     */
    public static WordPuzzleDao getWordPuzzleDao(Context context) {
        if (wordPuzzleDaoInstance == null) {
            wordPuzzleDaoInstance = new WordPuzzleDaoImpl(context);
        }
        return wordPuzzleDaoInstance;
    }

    public static GameWordDao getGameWordDao(Context context) {
        if (gameWordDaoInstance == null) {
            gameWordDaoInstance = new GameWordDaoImpl(context);
        }
        return gameWordDaoInstance;
    }

    public static SentenceWordDao getSentenceWordDao(Context context) {
        if (sentenceWordDaoInstance == null) {
            sentenceWordDaoInstance = new SentenceWordDaoImpl(context);
        }
        return sentenceWordDaoInstance;
    }

    public static GameQuestionDetailDao getGameQuestionDetailDao(Context context) {
        if (gameQuestionDetailDaoInstance == null) {
            gameQuestionDetailDaoInstance = new GameQuestionDetailDaoImpl(context);
        }
        return gameQuestionDetailDaoInstance;
    }
}
