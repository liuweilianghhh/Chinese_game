package com.example.chinese_game;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chinese_game.dao.UserDao;
import com.example.chinese_game.javabean.User;
import com.example.chinese_game.utils.DaoFactory;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private EditText etUsername, etEmail;
    private TextView tvRegistrationDate, tvTotalGames, tvTotalScore, tvLoginStreak;
    private Button btnUpdateProfile, btnBack;

    private UserDao userDao;
    private User currentUser;
    private int userId;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // 获取用户ID
        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化DAO
        userDao = DaoFactory.getUserDao(this);

        // 初始化视图
        initViews();

        // 加载用户信息
        loadUserProfile();

        // 设置按钮监听器
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        tvRegistrationDate = findViewById(R.id.tv_registration_date);
        tvTotalGames = findViewById(R.id.tv_total_games);
        tvTotalScore = findViewById(R.id.tv_total_score);
        tvLoginStreak = findViewById(R.id.tv_login_streak);
        btnUpdateProfile = findViewById(R.id.btn_update_profile);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadUserProfile() {
        currentUser = userDao.findById(userId);
        if (currentUser != null) {
            etUsername.setText(currentUser.getName());
            etEmail.setText(currentUser.getEmail());

            // 显示注册时间
            if (currentUser.getRegistrationDate() != null) {
                tvRegistrationDate.setText(dateFormat.format(currentUser.getRegistrationDate()));
            } else {
                tvRegistrationDate.setText("Unknown");
            }

            // 显示统计信息
            tvTotalGames.setText(String.valueOf(currentUser.getTotalGamesPlayed()));
            tvTotalScore.setText(String.valueOf(currentUser.getTotalScore()));
            tvLoginStreak.setText(String.valueOf(currentUser.getLoginStreak()));
        } else {
            Toast.makeText(this, "Failed to load user information", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnBack.setOnClickListener(v -> finish());
    }

    private void updateProfile() {
        String newUsername = etUsername.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        // 验证用户名
        if (TextUtils.isEmpty(newUsername)) {
            etUsername.setError("Username cannot be empty");
            return;
        }

        if (newUsername.length() < 3 || newUsername.length() > 32) {
            etUsername.setError("Username length must be between 3-32 characters");
            return;
        }

        // 验证邮箱格式
        if (!TextUtils.isEmpty(newEmail) && !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Invalid email format");
            return;
        }

        // 检查用户名是否已被其他用户使用
        User existingUser = userDao.findByUsername(newUsername);
        if (existingUser != null && existingUser.getId() != userId) {
            etUsername.setError("Username already in use");
            return;
        }

        // 更新用户信息
        currentUser.setName(newUsername);
        currentUser.setEmail(newEmail);

        boolean success = userDao.updateUser(currentUser);
        if (success) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            // 返回结果给调用者
            Intent resultIntent = new Intent();
            resultIntent.putExtra("UPDATED_USERNAME", newUsername);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
        }
    }
}