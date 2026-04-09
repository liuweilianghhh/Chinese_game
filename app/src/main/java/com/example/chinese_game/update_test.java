package com.example.chinese_game;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chinese_game.dao.UserDao;
import com.example.chinese_game.dao.UserDaoImpl;
import com.example.chinese_game.javabean.User;

public class update_test extends AppCompatActivity {
    private EditText name,password,repassword;
    private Button update;
    private UserDao userDao;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_test);
        userDao = new UserDaoImpl(this);
        find();
    }

    private void find() {
        name =findViewById(R.id.etname2);
        password =findViewById(R.id.edpassword2);
        repassword =findViewById(R.id.repassword1);
    }

    public void xiugai(View view) {
        String username = name.getText().toString().trim();
        String oldPassword = password.getText().toString().trim();
        String newPassword = repassword.getText().toString().trim();

        if (username.isEmpty() || oldPassword.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 先验证旧密码
            User user = userDao.login(username, oldPassword);
            if (user == null) {
                Toast.makeText(this, "Invalid username or old password", Toast.LENGTH_SHORT).show();
                return;
            }

            // 更新密码
            user.setPassword(newPassword);
            boolean success = userDao.updateUser(user);
            if (success) {
                Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                finish(); // 返回上一页
            } else {
                Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Password change failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}