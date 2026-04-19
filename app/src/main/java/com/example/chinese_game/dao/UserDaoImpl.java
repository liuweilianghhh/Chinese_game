package com.example.chinese_game.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.chinese_game.MYsqliteopenhelper;
import com.example.chinese_game.javabean.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserDaoImpl implements UserDao {
    private MYsqliteopenhelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public UserDaoImpl(Context context) {
        this.dbHelper = new MYsqliteopenhelper(context);
    }

    @Override
    public long register(User user) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", user.getName());
        cv.put("password", user.getPassword());
        cv.put("email", user.getEmail());
        cv.put("registration_date", dateFormat.format(user.getRegistrationDate()));
        cv.put("last_login_date", dateFormat.format(user.getLastLoginDate()));
        cv.put("login_streak", user.getLoginStreak());
        cv.put("total_games_played", user.getTotalGamesPlayed());
        cv.put("total_score", user.getTotalScore());
        cv.put("avatar_path", user.getAvatarPath());

        long result = db.insert("users", null, cv);
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result;
    }

    @Override
    public User login(String username, String password) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("users", null, "name=? AND password=?",
                new String[]{username, password}, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return user;
    }

    @Override
    public User findByUsername(String username) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("users", null, "name=?",
                new String[]{username}, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return user;
    }

    @Override
    public User findById(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("users", null, "id=?",
                new String[]{String.valueOf(userId)}, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return user;
    }

    @Override
    public boolean updateUser(User user) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", user.getName());
        cv.put("password", user.getPassword());
        cv.put("email", user.getEmail());
        cv.put("last_login_date", dateFormat.format(user.getLastLoginDate()));
        cv.put("login_streak", user.getLoginStreak());
        cv.put("total_games_played", user.getTotalGamesPlayed());
        cv.put("total_score", user.getTotalScore());
        cv.put("avatar_path", user.getAvatarPath());

        int result = db.update("users", cv, "id=?", new String[]{String.valueOf(user.getId())});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public boolean updateLoginInfo(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        ContentValues cv = new ContentValues();
        cv.put("last_login_date", dateFormat.format(new Date()));

        // 这里可以实现连续登录天数的计算逻辑
        // 为了简化，这里只是更新最后登录时间
        int result = db.update("users", cv, "id=?", new String[]{String.valueOf(userId)});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public boolean updateGameStats(int userId, int score) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        db.execSQL(
                "UPDATE users " +
                        "SET total_games_played = total_games_played + 1, " +
                        "total_score = total_score + ? " +
                        "WHERE id = ?",
                new Object[]{score, userId}
        );

        Cursor cursor = db.rawQuery("SELECT changes()", null);
        boolean updated = false;
        if (cursor.moveToFirst()) {
            updated = cursor.getInt(0) > 0;
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return updated;
    }

    @Override
    public List<User> findAllUsers() {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.query("users", null, null, null, null, null, "name ASC");

        List<User> users = new ArrayList<>();
        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor));
        }
        cursor.close();
        // 不关闭数据库连接，保持为App Inspection实时访问
        return users;
    }

    @Override
    public boolean deleteUser(int userId) {
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        int result = db.delete("users", "id=?", new String[]{String.valueOf(userId)});
        // 不关闭数据库连接，保持为App Inspection实时访问
        return result > 0;
    }

    @Override
    public int importFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return -1;
        }

        int importedCount = 0;
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        try {
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonUser = jsonArray.getJSONObject(i);

                User user = new User();
                user.setName(jsonUser.optString("name", ""));
                user.setPassword(jsonUser.optString("password", ""));
                user.setEmail(jsonUser.optString("email", null));

                // 解析日期
                String regDateStr = jsonUser.optString("registration_date", null);
                if (regDateStr != null && !regDateStr.isEmpty()) {
                    try {
                        user.setRegistrationDate(dateFormat.parse(regDateStr));
                    } catch (ParseException e) {
                        user.setRegistrationDate(new Date());
                    }
                } else {
                    user.setRegistrationDate(new Date());
                }

                String lastLoginStr = jsonUser.optString("last_login_date", null);
                if (lastLoginStr != null && !lastLoginStr.isEmpty()) {
                    try {
                        user.setLastLoginDate(dateFormat.parse(lastLoginStr));
                    } catch (ParseException e) {
                        user.setLastLoginDate(new Date());
                    }
                } else {
                    user.setLastLoginDate(new Date());
                }

                user.setLoginStreak(jsonUser.optInt("login_streak", 0));
                user.setTotalGamesPlayed(jsonUser.optInt("total_games_played", 0));
                user.setTotalScore(jsonUser.optInt("total_score", 0));
                user.setAvatarPath(jsonUser.optString("avatar_path", null));

                // 检查用户名是否已存在
                if (findByUsername(user.getName()) == null) {
                    long result = register(user);
                    if (result != -1) {
                        importedCount++;
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }

        return importedCount;
    }

    @Override
    public int importFromAssetsJson(android.content.Context context) {
        try {
            java.io.InputStream inputStream = context.getAssets().open("json/users.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String jsonContent = new String(buffer, "UTF-8");
            return importFromJson(jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        user.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password")));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));

        // 解析日期
        try {
            String regDateStr = cursor.getString(cursor.getColumnIndexOrThrow("registration_date"));
            if (regDateStr != null) {
                user.setRegistrationDate(dateFormat.parse(regDateStr));
            }

            String lastLoginStr = cursor.getString(cursor.getColumnIndexOrThrow("last_login_date"));
            if (lastLoginStr != null) {
                user.setLastLoginDate(dateFormat.parse(lastLoginStr));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        user.setLoginStreak(cursor.getInt(cursor.getColumnIndexOrThrow("login_streak")));
        user.setTotalGamesPlayed(cursor.getInt(cursor.getColumnIndexOrThrow("total_games_played")));
        user.setTotalScore(cursor.getInt(cursor.getColumnIndexOrThrow("total_score")));
        user.setAvatarPath(cursor.getString(cursor.getColumnIndexOrThrow("avatar_path")));

        return user;
    }
}
