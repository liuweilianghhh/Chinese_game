package com.example.chinese_game;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chinese_game.dao.UserDao;
import com.example.chinese_game.utils.DaoFactory;

public class game_choice extends AppCompatActivity {

    private Button btnCharacterMatching, btnPronunciationQuiz, btnWordPuzzle;
    private ImageView menuDropdown;
    private View mascotView, subtitleView, cardModes;
    private int userId;
    private UserDao userDao;
    private BackgroundMusicManager backgroundMusicManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_choice);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userDao = DaoFactory.getUserDao(this);
        backgroundMusicManager = BackgroundMusicManager.getInstance(this);
        initViews();
        setupListeners();
        playEntranceAnimation();
    }

    private void initViews() {
        btnCharacterMatching = findViewById(R.id.btn_character_matching);
        btnPronunciationQuiz = findViewById(R.id.btn_pronunciation_quiz);
        btnWordPuzzle = findViewById(R.id.btn_word_puzzle);
        menuDropdown = findViewById(R.id.menu_dropdown);
        mascotView = findViewById(R.id.iv_choice_mascot);
        subtitleView = findViewById(R.id.tv_choice_subtitle);
        cardModes = findViewById(R.id.card_game_modes);
    }

    private void setupListeners() {
        btnCharacterMatching.setOnClickListener(v -> startGame("CHARACTER_MATCHING"));
        btnPronunciationQuiz.setOnClickListener(v -> startGame("PRONUNCIATION_QUIZ"));
        btnWordPuzzle.setOnClickListener(v -> startGame("WORD_PUZZLE"));
        menuDropdown.setOnClickListener(this::showDropdownMenu);
    }

    private void startGame(String gameType) {
        showDifficultySelectionDialog(gameType);
    }

    private void showDifficultySelectionDialog(String gameType) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_select_difficulty, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setCancelable(true)
                .create();

        content.findViewById(R.id.btn_difficulty_easy).setOnClickListener(v -> {
            dialog.dismiss();
            showLoadingAndStartGame(gameType, "EASY");
        });
        content.findViewById(R.id.btn_difficulty_medium).setOnClickListener(v -> {
            dialog.dismiss();
            showLoadingAndStartGame(gameType, "MEDIUM");
        });
        content.findViewById(R.id.btn_difficulty_hard).setOnClickListener(v -> {
            dialog.dismiss();
            showLoadingAndStartGame(gameType, "HARD");
        });
        content.findViewById(R.id.btn_difficulty_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showLoadingAndStartGame(String gameType, String difficulty) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_loading_questions, null);
        ProgressBar progressBar = content.findViewById(R.id.dialog_loading_progress);

        Dialog loadingDialog = new AlertDialog.Builder(this)
                .setView(content)
                .setCancelable(false)
                .create();
        loadingDialog.show();

        final int durationMs = 1200;
        final int stepMs = 50;
        final Handler handler = new Handler(Looper.getMainLooper());
        final int[] progress = {0};

        Runnable updateProgress = new Runnable() {
            @Override
            public void run() {
                progress[0] += (100 * stepMs / durationMs);
                if (progress[0] >= 100) {
                    progress[0] = 100;
                    progressBar.setProgress(100);
                    loadingDialog.dismiss();
                    Intent intent = new Intent(game_choice.this, GameActivity.class);
                    intent.putExtra("GAME_TYPE", gameType);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("DIFFICULTY", difficulty);
                    startActivity(intent);
                    return;
                }
                progressBar.setProgress(progress[0]);
                handler.postDelayed(this, stepMs);
            }
        };
        handler.postDelayed(updateProgress, stepMs);
    }

    private void showDropdownMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.game_choice_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_user_profile) {
                openUserProfile();
                return true;
            } else if (itemId == R.id.menu_user_achievements) {
                openUserAchievements();
                return true;
            } else if (itemId == R.id.menu_game_records) {
                openGameRecords();
                return true;
            } else if (itemId == R.id.menu_music_toggle) {
                toggleMusic();
                return true;
            } else if (itemId == R.id.menu_logout) {
                confirmLogout();
                return true;
            }
            return false;
        });
        popupMenu.getMenu().findItem(R.id.menu_music_toggle)
                .setTitle(backgroundMusicManager != null && backgroundMusicManager.isMusicEnabled()
                        ? "Turn Music Off"
                        : "Turn Music On");
        popupMenu.show();
    }

    private void openUserProfile() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivityForResult(intent, 1001);
    }

    private void openUserAchievements() {
        Intent intent = new Intent(this, UserAchievementsActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private void openGameRecords() {
        Intent intent = new Intent(this, GameRecordsActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleMusic() {
        if (backgroundMusicManager == null) {
            return;
        }
        boolean enabled = !backgroundMusicManager.isMusicEnabled();
        backgroundMusicManager.setMusicEnabled(enabled);
        Toast.makeText(this, enabled ? "Background music on" : "Background music off", Toast.LENGTH_SHORT).show();
    }

    private void playEntranceAnimation() {
        if (mascotView == null || subtitleView == null || cardModes == null) return;
        View[] targets = {mascotView, subtitleView, cardModes};
        for (View target : targets) {
            target.setAlpha(0f);
            target.setTranslationY(26f);
        }
        mascotView.setScaleX(0.85f);
        mascotView.setScaleY(0.85f);
        mascotView.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(260).start();
        subtitleView.animate().alpha(1f).translationY(0f).setStartDelay(90).setDuration(220).start();
        cardModes.animate().alpha(1f).translationY(0f).setStartDelay(150).setDuration(260).start();
        ObjectAnimator.ofFloat(btnCharacterMatching, View.TRANSLATION_X, 0f, 8f, -8f, 0f)
                .setDuration(260)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String updatedUsername = data.getStringExtra("UPDATED_USERNAME");
            if (updatedUsername != null) {
                Toast.makeText(this, "User information updated", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
