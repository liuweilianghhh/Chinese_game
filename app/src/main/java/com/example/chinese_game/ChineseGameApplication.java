package com.example.chinese_game;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.speech.IFlyTekConfig;
import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;

public class ChineseGameApplication extends Application {
    private static ChineseGameApplication instance;
    private MYsqliteopenhelper dbHelper;
    private SQLiteDatabase database;
    private static boolean sparkChainInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化数据库 helper 并保持数据库连接打开，便于 App Inspection 在软件运行中随时访问
        dbHelper = new MYsqliteopenhelper(this);
        database = dbHelper.getPersistentDatabase();
        android.util.Log.i("ChineseGameApplication", "Database connection opened and kept alive for App Inspection");

        // 进入软件时不导入 JSON，仅在用户点击 Reload 时由 DataManager.reloadAllDataFromJson() 导入
    }
    
    /**
     * 初始化科大讯飞SparkChain SDK（延迟初始化，在需要时调用）
     * @return true表示初始化成功或已初始化
     */
    public static boolean initSparkChain() {
        if (sparkChainInitialized) {
            return true;
        }
        try {
            SparkChainConfig config = SparkChainConfig.builder()
                    .appID(IFlyTekConfig.APP_ID)
                    .apiKey(IFlyTekConfig.API_KEY)
                    .apiSecret(IFlyTekConfig.API_SECRET);
            
            int ret = SparkChain.getInst().init(instance.getApplicationContext(), config);
            
            if (ret == 0) {
                sparkChainInitialized = true;
                android.util.Log.i("ChineseGameApplication", 
                    "科大讯飞SparkChain SDK初始化成功, APPID: " + IFlyTekConfig.APP_ID);
                return true;
            } else {
                android.util.Log.e("ChineseGameApplication", 
                    "科大讯飞SparkChain SDK初始化失败, 错误码: " + ret);
                return false;
            }
        } catch (Throwable e) {
            android.util.Log.e("ChineseGameApplication", 
                "科大讯飞SparkChain SDK初始化异常: " + e.getMessage(), e);
            sparkChainInitError = e.getMessage();
            return false;
        }
    }
    
    private static String sparkChainInitError = null;
    
    public static String getSparkChainInitError() {
        return sparkChainInitError;
    }
    
    public static boolean isSparkChainInitialized() {
        return sparkChainInitialized;
    }

    public static ChineseGameApplication getInstance() {
        return instance;
    }

    public SQLiteDatabase getDatabase() {
        if (database == null || !database.isOpen()) {
            database = dbHelper.getPersistentDatabase();
        }
        return database;
    }

    public MYsqliteopenhelper getDbHelper() {
        return dbHelper;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 不关闭数据库连接，保证运行期间 App Inspection 可随时访问；进程结束时由系统回收
    }
}