package com.example.chinese_game;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chinese_game.dao.AchievementDao;
import com.example.chinese_game.dao.GameScoreDao;
import com.example.chinese_game.dao.UserDao;
import com.example.chinese_game.javabean.Achievement;
import com.example.chinese_game.javabean.GameScore;
import com.example.chinese_game.javabean.User;
import com.example.chinese_game.javabean.UserAchievement;
import com.example.chinese_game.utils.DaoFactory;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserAchievementsActivity extends AppCompatActivity {
    private static final String TAG = "UserAchievementsActivity";

    private LinearLayout achievementsContainer;
    private TextView tvAchievementSummary;
    private Button btnBack;

    private AchievementDao achievementDao;
    private GameScoreDao gameScoreDao;
    private UserDao userDao;
    private int userId;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_achievements);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        achievementDao = DaoFactory.getAchievementDao(this);
        gameScoreDao = DaoFactory.getGameScoreDao(this);
        userDao = DaoFactory.getUserDao(this);
        initViews();
        loadUserAchievements();
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        achievementsContainer = findViewById(R.id.achievements_container);
        tvAchievementSummary = findViewById(R.id.tv_achievement_summary);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadUserAchievements() {
        if (achievementsContainer == null) {
            Toast.makeText(this, "Achievement view not found", Toast.LENGTH_SHORT).show();
            return;
        }
        achievementsContainer.removeAllViews();

        try {
            List<Achievement> achievements = achievementDao.findAll();
            if (achievements == null || achievements.isEmpty()) {
                addEmptyState("No achievements available.");
                return;
            }

            synchronizeEligibleAchievements(achievements);

            User user = userDao.findById(userId);
            List<GameScore> completedScores = getCompletedScores();
            Map<Integer, UserAchievement> unlockedMap = buildUnlockedMap(achievementDao.getUserAchievements(userId));
            AchievementDao.AchievementStatistics statistics = achievementDao.getUserAchievementStatistics(userId);

            if (tvAchievementSummary != null) {
                tvAchievementSummary.setText(String.format(
                        Locale.getDefault(),
                        "Unlocked %d / %d  •  Reward Points %d",
                        statistics.unlockedAchievements,
                        achievements.size(),
                        statistics.totalRewardPoints
                ));
            }

            for (Achievement achievement : achievements) {
                UserAchievement userAchievement = unlockedMap.get(achievement.getId());
                AchievementProgress progress = calculateAchievementProgress(
                        achievement,
                        userAchievement,
                        user,
                        completedScores
                );
                achievementsContainer.addView(createAchievementCard(achievement, userAchievement, progress));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load achievements for userId=" + userId, e);
            Toast.makeText(this, "Failed to load achievements", Toast.LENGTH_SHORT).show();
            addEmptyState("Failed to load achievements.");
        }
    }

    private void synchronizeEligibleAchievements(List<Achievement> achievements) {
        User user = userDao.findById(userId);
        List<GameScore> completedScores = getCompletedScores();
        Map<Integer, UserAchievement> unlockedMap = buildUnlockedMap(achievementDao.getUserAchievements(userId));

        for (Achievement achievement : achievements) {
            if (unlockedMap.containsKey(achievement.getId())) {
                continue;
            }

            AchievementProgress progress = calculateAchievementProgress(achievement, null, user, completedScores);
            if (progress.requiredValue > 0 && progress.currentValue >= progress.requiredValue) {
                UserAchievement userAchievement = new UserAchievement(userId, achievement.getId());
                userAchievement.setProgressValue(progress.requiredValue);
                achievementDao.unlockAchievement(userAchievement);
            }
        }
    }

    private List<GameScore> getCompletedScores() {
        List<GameScore> allScores = gameScoreDao.findByUserId(userId);
        List<GameScore> completedScores = new ArrayList<>();
        for (GameScore score : allScores) {
            if (score != null && score.isCompleted()) {
                completedScores.add(score);
            }
        }
        return completedScores;
    }

    private Map<Integer, UserAchievement> buildUnlockedMap(List<UserAchievement> userAchievements) {
        Map<Integer, UserAchievement> unlockedMap = new HashMap<>();
        if (userAchievements == null) {
            return unlockedMap;
        }
        for (UserAchievement userAchievement : userAchievements) {
            unlockedMap.put(userAchievement.getAchievementId(), userAchievement);
        }
        return unlockedMap;
    }

    private AchievementProgress calculateAchievementProgress(
            Achievement achievement,
            UserAchievement userAchievement,
            User user,
            List<GameScore> completedScores
    ) {
        int requiredValue = Math.max(achievement.getRequiredValue(), 1);
        int currentValue = 0;
        String detailText;
        boolean unlocked = userAchievement != null;

        if (unlocked) {
            currentValue = requiredValue;
            detailText = userAchievement.getUnlockedDate() != null
                    ? "Unlocked on " + dateFormat.format(userAchievement.getUnlockedDate())
                    : "Unlocked";
            return new AchievementProgress(currentValue, requiredValue, true, detailText);
        }

        switch (achievement.getCategory()) {
            case GENERAL:
                currentValue = Math.max(user != null ? user.getTotalGamesPlayed() : 0, completedScores.size());
                detailText = "Completed games so far: " + currentValue;
                break;
            case LOGIN_STREAK:
                currentValue = user != null ? user.getLoginStreak() : 0;
                detailText = "Current login streak: " + currentValue + " day(s)";
                break;
            case CHARACTER_MATCHING:
                currentValue = getBestGameProgress(GameScore.GameType.CHARACTER_MATCHING, requiredValue, completedScores);
                detailText = buildBestAccuracyText(GameScore.GameType.CHARACTER_MATCHING, completedScores);
                break;
            case PRONUNCIATION_QUIZ:
                currentValue = getBestGameProgress(GameScore.GameType.PRONUNCIATION_QUIZ, requiredValue, completedScores);
                detailText = buildBestAccuracyText(GameScore.GameType.PRONUNCIATION_QUIZ, completedScores);
                break;
            case WORD_PUZZLE:
                currentValue = getBestGameProgress(GameScore.GameType.WORD_PUZZLE, requiredValue, completedScores);
                detailText = buildBestAccuracyText(GameScore.GameType.WORD_PUZZLE, completedScores);
                break;
            default:
                detailText = "No progress yet";
                break;
        }

        currentValue = Math.min(currentValue, requiredValue);
        return new AchievementProgress(currentValue, requiredValue, false, detailText);
    }

    private int getBestGameProgress(
            GameScore.GameType targetType,
            int requiredValue,
            List<GameScore> completedScores
    ) {
        int bestProgress = 0;
        for (GameScore score : completedScores) {
            if (score == null || score.getGameType() != targetType) {
                continue;
            }
            double ratio = score.getAccuracy();
            if (ratio <= 0 && score.getMaxPossibleScore() > 0) {
                ratio = (double) score.getScore() / score.getMaxPossibleScore();
            }
            int progress = (int) Math.round(ratio * requiredValue);
            bestProgress = Math.max(bestProgress, progress);
        }
        return bestProgress;
    }

    private String buildBestAccuracyText(GameScore.GameType targetType, List<GameScore> completedScores) {
        double bestAccuracy = -1.0;
        for (GameScore score : completedScores) {
            if (score == null || score.getGameType() != targetType) {
                continue;
            }
            double ratio = score.getAccuracy();
            if (ratio <= 0 && score.getMaxPossibleScore() > 0) {
                ratio = (double) score.getScore() / score.getMaxPossibleScore();
            }
            bestAccuracy = Math.max(bestAccuracy, ratio);
        }

        if (bestAccuracy < 0) {
            return "No completed records in this mode yet";
        }
        return String.format(
                Locale.getDefault(),
                "Best recorded accuracy: %.0f%%",
                bestAccuracy * 100
        );
    }

    private View createAchievementCard(
            Achievement achievement,
            UserAchievement userAchievement,
            AchievementProgress progress
    ) {
        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setRadius(dpToPx(18));
        cardView.setCardElevation(dpToPx(2));
        cardView.setStrokeWidth(dpToPx(1));
        cardView.setStrokeColor(progress.unlocked
                ? getColor(R.color.duo_green_dark)
                : resolveMaterialColor(cardView, com.google.android.material.R.attr.colorOutline, Color.LTGRAY));
        cardView.setCardBackgroundColor(progress.unlocked
                ? Color.parseColor("#FFFDF6E8")
                : Color.parseColor("#F9FFFFFF"));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        cardView.setLayoutParams(cardParams);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(dpToPx(16), dpToPx(15), dpToPx(16), dpToPx(15));
        cardView.addView(cardLayout);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setBaselineAligned(false);
        cardLayout.addView(titleRow);

        TextView nameText = new TextView(this);
        nameText.setText(achievement.getName() != null ? achievement.getName() : "Unnamed Achievement");
        nameText.setTextSize(18);
        nameText.setTextColor(resolveMaterialColor(
                nameText,
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK));
        nameText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameText.setLayoutParams(nameParams);
        titleRow.addView(nameText);

        TextView statusBadge = buildStatusBadge(progress.unlocked ? "Unlocked" : "In Progress", progress.unlocked);
        titleRow.addView(statusBadge);

        TextView descriptionText = new TextView(this);
        descriptionText.setText(achievement.getDescription() != null ? achievement.getDescription() : "");
        descriptionText.setTextSize(14);
        descriptionText.setTextColor(resolveMaterialColor(
                descriptionText,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Color.DKGRAY));
        descriptionText.setPadding(0, dpToPx(6), 0, dpToPx(8));
        cardLayout.addView(descriptionText);

        TextView metaText = new TextView(this);
        metaText.setText(String.format(
                Locale.getDefault(),
                "%s  •  %s  •  Reward %d",
                achievement.getCategory() != null ? achievement.getCategory().getDisplayName() : "General",
                achievement.getType() != null ? achievement.getType().getDisplayName() : "Milestone",
                achievement.getRewardPoints()
        ));
        metaText.setTextSize(12);
        metaText.setTextColor(getColor(R.color.duo_text_muted));
        cardLayout.addView(metaText);

        TextView progressText = new TextView(this);
        progressText.setText(String.format(
                Locale.getDefault(),
                "Progress %d / %d",
                progress.currentValue,
                progress.requiredValue
        ));
        progressText.setTextSize(13);
        progressText.setTextColor(progress.unlocked ? getColor(R.color.duo_green_dark) : getColor(R.color.duo_text_dark));
        progressText.setTypeface(null, Typeface.BOLD);
        progressText.setPadding(0, dpToPx(10), 0, dpToPx(6));
        cardLayout.addView(progressText);

        if (!progress.unlocked) {
            LinearProgressIndicator progressIndicator = new LinearProgressIndicator(this);
            progressIndicator.setMax(progress.requiredValue);
            progressIndicator.setProgress(progress.currentValue);
            progressIndicator.setTrackThickness(dpToPx(8));
            progressIndicator.setIndicatorColor(getColor(R.color.duo_green));
            progressIndicator.setTrackColor(getColor(R.color.duo_green_soft));
            LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            indicatorParams.bottomMargin = dpToPx(8);
            progressIndicator.setLayoutParams(indicatorParams);
            cardLayout.addView(progressIndicator);
        }

        TextView detailText = new TextView(this);
        detailText.setText(progress.detailText);
        detailText.setTextSize(12);
        detailText.setTextColor(getColor(R.color.duo_text_muted));
        cardLayout.addView(detailText);

        if (progress.unlocked && userAchievement != null && userAchievement.getUnlockedDate() != null) {
            TextView unlockTimeText = new TextView(this);
            unlockTimeText.setText("Unlocked: " + dateFormat.format(userAchievement.getUnlockedDate()));
            unlockTimeText.setTextSize(12);
            unlockTimeText.setTextColor(getColor(R.color.duo_green_dark));
            unlockTimeText.setPadding(0, dpToPx(6), 0, 0);
            cardLayout.addView(unlockTimeText);
        }

        return cardView;
    }

    private TextView buildStatusBadge(String text, boolean unlocked) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(11);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(unlocked ? Color.parseColor("#FF6B4A00") : getColor(R.color.duo_blue));
        badge.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dpToPx(999));
        drawable.setColor(unlocked ? Color.parseColor("#FFFEE3B3") : Color.parseColor("#FFE7F5FF"));
        drawable.setStroke(dpToPx(1), unlocked ? Color.parseColor("#FFE0BA5C") : Color.parseColor("#FF9ED7FF"));
        badge.setBackground(drawable);
        return badge;
    }

    private void addEmptyState(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setTextColor(getColor(R.color.duo_text_muted));
        textView.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        achievementsContainer.addView(textView);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private int resolveMaterialColor(View view, int attr, int fallbackColor) {
        try {
            return MaterialColors.getColor(view, attr);
        } catch (Exception e) {
            Log.w(TAG, "Fallback color used for attr=" + attr, e);
            return fallbackColor;
        }
    }

    private static class AchievementProgress {
        final int currentValue;
        final int requiredValue;
        final boolean unlocked;
        final String detailText;

        AchievementProgress(int currentValue, int requiredValue, boolean unlocked, String detailText) {
            this.currentValue = currentValue;
            this.requiredValue = requiredValue;
            this.unlocked = unlocked;
            this.detailText = detailText;
        }
    }
}
