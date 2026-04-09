package com.example.chinese_game;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
import com.example.chinese_game.dao.SentenceWordDao;
import com.example.chinese_game.dao.SentenceWordDaoImpl;
import com.example.chinese_game.dao.WordPuzzleDao;
import com.example.chinese_game.dao.WordPuzzleDaoImpl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据管理器 - 负责从JSON文件实时加载数据
 */
public class DataManager {
    private static final String TAG = "DataManager";
    private static DataManager instance;

    private Context context;
    private Handler mainHandler;
    private UserDao userDao;
    private AchievementDao achievementDao;
    private GameScoreDao gameScoreDao;
    private CharacterMatchingDao characterMatchingDao;
    private SentenceWordDao sentenceWordDao;
    private GameWordDao gameWordDao;
    private PronunciationQuizDao pronunciationQuizDao;
    private WordPuzzleDao wordPuzzleDao;

    // 存储文件的最后修改时间
    private Map<String, Long> fileLastModified;

    private DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.userDao = new UserDaoImpl(context);
        this.achievementDao = new AchievementDaoImpl(context);
        this.gameScoreDao = new GameScoreDaoImpl(context);
        this.characterMatchingDao = new CharacterMatchingDaoImpl(context);
        this.sentenceWordDao = new SentenceWordDaoImpl(context);
        this.gameWordDao = new GameWordDaoImpl(context);
        this.pronunciationQuizDao = new PronunciationQuizDaoImpl(context);
        this.wordPuzzleDao = new WordPuzzleDaoImpl(context);
        this.fileLastModified = new HashMap<>();

        // 初始化文件修改时间
        initializeFileTimestamps();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    private void initializeFileTimestamps() {
        String[] jsonFiles = {"users.json", "achievements.json", "user_achievements.json", "game_scores.json", "game_words.json", "character_matching.json", "pronunciation_quiz.json", "word_puzzle.json"};

        for (String fileName : jsonFiles) {
            try {
                File file = new File(context.getFilesDir(), "json/" + fileName);
                if (file.exists()) {
                    fileLastModified.put(fileName, file.lastModified());
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking file timestamp for " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * 检查JSON文件是否有更新，如果有则重新加载数据
     */
    public void checkAndReloadData() {
        checkAndReloadData(null);
    }

    /**
     * 检查JSON文件是否有更新，如果有则重新加载数据
     * @param callback 完成回调
     */
    public void checkAndReloadData(DataLoadCallback callback) {
        new Thread(() -> {
            boolean hasChanges = false;
            String[] jsonFiles = {"users.json", "achievements.json", "user_achievements.json", "game_scores.json", "game_words.json", "character_matching.json", "pronunciation_quiz.json", "word_puzzle.json"};

            for (String fileName : jsonFiles) {
                try {
                    File file = new File(context.getFilesDir(), "json/" + fileName);
                    if (file.exists()) {
                        long currentModified = file.lastModified();
                        Long lastModified = fileLastModified.get(fileName);

                        if (lastModified == null || currentModified > lastModified) {
                            hasChanges = true;
                            fileLastModified.put(fileName, currentModified);
                            Log.i(TAG, "File " + fileName + " has been modified, reloading data...");
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error checking file " + fileName + ": " + e.getMessage());
                }
            }

            if (hasChanges) {
                reloadAllDataFromJson();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onDataReloaded();
                    }
                });
            } else {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onNoChanges();
                    }
                });
            }
        }).start();
    }

    /**
     * 强制重新加载所有JSON数据
     */
    public void forceReloadAllData() {
        forceReloadAllData(null);
    }

    /**
     * 强制重新加载所有JSON数据
     * @param callback 完成回调
     */
    public void forceReloadAllData(DataLoadCallback callback) {
        new Thread(() -> {
            reloadAllDataFromJson();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onDataReloaded();
                }
            });
        }).start();
    }

    /**
     * 从assets文件夹重新加载所有数据
     */
    private void reloadAllDataFromJson() {
        try {
            Log.i(TAG, "Reloading all data from JSON files...");

            // 先清空所有表并重置自增 id，使本次导入的 id 从 1 开始
            com.example.chinese_game.utils.SeedDataLoader.resetAllTablesAndSequences(context);
            Log.i(TAG, "Reset all tables and sequences");

            // 导入用户数据
            int userCount = userDao.importFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + userCount + " users from JSON");

            // 导入成就数据
            int achievementCount = achievementDao.importAchievementsFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + achievementCount + " achievements from JSON");

            // 导入用户成就数据
            int userAchievementCount = achievementDao.importUserAchievementsFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + userAchievementCount + " user achievements from JSON");

            // 导入游戏分数数据
            int gameScoreCount = gameScoreDao.importFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + gameScoreCount + " game scores from JSON");
            int gameWordCount = gameWordDao.importFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + gameWordCount + " game words from JSON");

            // 先导入 sentences 和 sentence_words，再导入题目
            com.example.chinese_game.utils.SeedDataLoader.loadSentencesAndWordsFromAssets(context);
            Log.i(TAG, "Loaded sentences and sentence_words from JSON");

            sentenceWordDao.updateWordFrequencyAndDifficulty(context);
            Log.i(TAG, "Updated word_frequency and word_difficulty by frequency (excluding game-excluded pos)");

            // 导入字符匹配数据
            int characterMatchingCount = characterMatchingDao.importFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + characterMatchingCount + " character matching data from JSON");

            // 导入发音测验数据
            int pronunciationQuizCount = pronunciationQuizDao.importFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + pronunciationQuizCount + " pronunciation quiz data from JSON");

            // 导入字词谜题数据
            int wordPuzzleCount = wordPuzzleDao.importFromAssetsJson(context);
            Log.i(TAG, "Reloaded " + wordPuzzleCount + " word puzzle data from JSON");

            Log.i(TAG, "Data reload completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error reloading data from JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 复制assets中的JSON文件到内部存储，以便实时修改
     */
    public void copyAssetsJsonToInternalStorage() {
        new Thread(() -> {
            String[] jsonFiles = {"users.json", "achievements.json", "user_achievements.json", "game_scores.json", "game_words.json", "character_matching.json", "pronunciation_quiz.json", "word_puzzle.json"};

            for (String fileName : jsonFiles) {
                try {
                    // 读取assets中的文件
                    java.io.InputStream inputStream = context.getAssets().open("json/" + fileName);
                    int size = inputStream.available();
                    byte[] buffer = new byte[size];
                    inputStream.read(buffer);
                    inputStream.close();

                    // 写入到内部存储
                    File jsonDir = new File(context.getFilesDir(), "json");
                    if (!jsonDir.exists()) {
                        jsonDir.mkdirs();
                    }

                    File outputFile = new File(jsonDir, fileName);
                    java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputFile);
                    outputStream.write(buffer);
                    outputStream.close();

                    Log.i(TAG, "Copied " + fileName + " to internal storage");

                } catch (Exception e) {
                    Log.e(TAG, "Error copying " + fileName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            mainHandler.post(() -> Log.i(TAG, "All JSON files copied to internal storage"));
        }).start();
    }

    /**
     * 数据加载回调接口
     */
    public interface DataLoadCallback {
        void onDataReloaded();
        void onNoChanges();
    }
}
