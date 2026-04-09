package com.example.chinese_game;

import android.content.Intent;
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

public class register extends AppCompatActivity {
    private Button register1;
    private EditText name1,password1;
    private UserDao userDao;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        userDao = new UserDaoImpl(this);
        find();
    }

    private void find() {

        register1 = findViewById(R.id.register1);
        name1 = findViewById(R.id.edname);
        password1 = findViewById(R.id.edpassword1);
    }

    public void zhuche(View view) {
        String username = name1.getText().toString().trim();
        String password = password1.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 检查用户名是否已存在
            User existingUser = userDao.findByUsername(username);
            if (existingUser != null) {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            User newUser = new User(username, password);
            long result = userDao.register(newUser);
            if (result != -1) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                Intent i3 = new Intent(this, MainActivity.class);
                startActivity(i3);
                finish();
            } else {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}