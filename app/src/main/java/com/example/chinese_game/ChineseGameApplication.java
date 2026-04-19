package com.example.chinese_game;

import android.app.Activity;
import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.example.chinese_game.speech.IFlyTekConfig;
import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;

public class ChineseGameApplication extends Application {
    private static ChineseGameApplication instance;
    private static boolean sparkChainInitialized = false;
    private static String sparkChainInitError = null;

    private MYsqliteopenhelper dbHelper;
    private SQLiteDatabase database;
    private int startedActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        dbHelper = new MYsqliteopenhelper(this);
        database = dbHelper.getPersistentDatabase();
        android.util.Log.i("ChineseGameApplication",
                "Database connection opened and kept alive for App Inspection");

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
                if (startedActivityCount == 1) {
                    BackgroundMusicManager.getInstance(ChineseGameApplication.this).onAppForeground();
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivityCount = Math.max(0, startedActivityCount - 1);
                if (startedActivityCount == 0) {
                    BackgroundMusicManager.getInstance(ChineseGameApplication.this).onAppBackground();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

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
                        "SparkChain SDK initialized successfully. APPID: " + IFlyTekConfig.APP_ID);
                return true;
            }

            android.util.Log.e("ChineseGameApplication",
                    "SparkChain SDK initialization failed. Error code: " + ret);
            return false;
        } catch (Throwable e) {
            android.util.Log.e("ChineseGameApplication",
                    "SparkChain SDK initialization error: " + e.getMessage(), e);
            sparkChainInitError = e.getMessage();
            return false;
        }
    }

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
        BackgroundMusicManager.getInstance(this).release();
        super.onTerminate();
    }
}
