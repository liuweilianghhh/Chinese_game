package com.example.chinese_game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chinese_game.dao.UserDao;
import com.example.chinese_game.dao.UserDaoImpl;
import com.example.chinese_game.javabean.User;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button login, register, update, reloadData, musicSettingsButton;
    private EditText name, password;
    private ImageView mascotView;
    private TextView titleView, subtitleView;
    private View cardAuth, mainActions;
    private UserDao userDao;
    private DataManager dataManager;
    private BackgroundMusicManager backgroundMusicManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userDao = new UserDaoImpl(this);
        dataManager = DataManager.getInstance(this);
        backgroundMusicManager = BackgroundMusicManager.getInstance(this);
        find();
        refreshMusicButtonState();
        playIntroAnimation();
    }

    private void find() {
        login = findViewById(R.id.login);
        register = findViewById(R.id.register);
        name = findViewById(R.id.edname);
        password = findViewById(R.id.edpassword);
        update = findViewById(R.id.update2);
        reloadData = findViewById(R.id.reload_data);
        musicSettingsButton = findViewById(R.id.btn_music_settings);
        mascotView = findViewById(R.id.iv_mascot);
        titleView = findViewById(R.id.tv_main_title);
        subtitleView = findViewById(R.id.tv_main_subtitle);
        cardAuth = findViewById(R.id.card_auth);
        mainActions = findViewById(R.id.main_actions);

        login.setOnClickListener(this);
        register.setOnClickListener(this);
        update.setOnClickListener(this);
        reloadData.setOnClickListener(this);
        musicSettingsButton.setOnClickListener(this);
    }

    private void playIntroAnimation() {
        if (mascotView == null || titleView == null || subtitleView == null || cardAuth == null || mainActions == null) {
            return;
        }

        View[] animatedViews = {mascotView, titleView, subtitleView, cardAuth, mainActions};
        for (View v : animatedViews) {
            v.setAlpha(0f);
            v.setTranslationY(36f);
        }
        mascotView.setScaleX(0.75f);
        mascotView.setScaleY(0.75f);

        AnimatorSet mascotSet = new AnimatorSet();
        mascotSet.playTogether(
                ObjectAnimator.ofFloat(mascotView, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(mascotView, View.TRANSLATION_Y, 36f, 0f),
                ObjectAnimator.ofFloat(mascotView, View.SCALE_X, 0.75f, 1f),
                ObjectAnimator.ofFloat(mascotView, View.SCALE_Y, 0.75f, 1f)
        );
        mascotSet.setDuration(320);
        mascotSet.start();

        animateEntranceView(titleView, 120);
        animateEntranceView(subtitleView, 180);
        animateEntranceView(cardAuth, 260);
        animateEntranceView(mainActions, 360);
    }

    private void animateEntranceView(View target, long delayMs) {
        target.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(260)
                .start();
    }

    private void reloadDataFromJson() {
        Toast.makeText(this, "Reloading data from JSON files...", Toast.LENGTH_SHORT).show();

        dataManager.forceReloadAllData(new DataManager.DataLoadCallback() {
            @Override
            public void onDataReloaded() {
                runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "Data reloaded successfully!", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onNoChanges() {
                runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "No changes detected", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void toggleMusic() {
        if (backgroundMusicManager == null) {
            return;
        }
        boolean enabled = !backgroundMusicManager.isMusicEnabled();
        backgroundMusicManager.setMusicEnabled(enabled);
        refreshMusicButtonState();
        Toast.makeText(this, enabled ? "Background music on" : "Background music off", Toast.LENGTH_SHORT).show();
    }

    private void refreshMusicButtonState() {
        if (musicSettingsButton == null || backgroundMusicManager == null) {
            return;
        }
        boolean enabled = backgroundMusicManager.isMusicEnabled();
        musicSettingsButton.setText(enabled ? "Music On" : "Music Off");
        musicSettingsButton.setAlpha(enabled ? 1f : 0.7f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMusicButtonState();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.login:
                String username = name.getText().toString().trim();
                String pwd = password.getText().toString().trim();

                if (username.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    break;
                }

                try {
                    User user = userDao.login(username, pwd);
                    if (user != null) {
                        // 更新用户登录信息
                        userDao.updateLoginInfo(user.getId());
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(this, game_choice.class);
                        i.putExtra("USER_ID", user.getId());
                        i.putExtra("USERNAME", user.getName());
                        startActivity(i);
                    } else {
                        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.register:
                Intent i1 = new Intent(this, register.class);
                startActivity(i1);
                break;
            case R.id.update2:
                Intent i2 =new Intent(this,update_test.class);
                startActivity(i2);
                break;
            case R.id.reload_data:
                reloadDataFromJson();
                break;
            case R.id.btn_music_settings:
                toggleMusic();
                break;
        }
    }
}
