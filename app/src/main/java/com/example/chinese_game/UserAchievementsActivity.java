package com.example.chinese_game;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chinese_game.dao.AchievementDao;
import com.example.chinese_game.javabean.Achievement;
import com.example.chinese_game.javabean.UserAchievement;
import com.example.chinese_game.utils.DaoFactory;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class UserAchievementsActivity extends AppCompatActivity {

    private LinearLayout achievementsContainer;
    private Button btnBack;

    private AchievementDao achievementDao;
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
        initViews();
        loadUserAchievements();
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        achievementsContainer = findViewById(R.id.achievements_container);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadUserAchievements() {
        List<UserAchievement> userAchievements = achievementDao.getUserAchievements(userId);

        if (userAchievements.isEmpty()) {
            TextView noAchievementsText = new TextView(this);
            noAchievementsText.setText("You haven't unlocked any achievements yet!\n\nKeep playing to earn achievements!");
            noAchievementsText.setTextSize(16);
            noAchievementsText.setTextColor(MaterialColors.getColor(noAchievementsText, com.google.android.material.R.attr.colorOnSurfaceVariant));
            noAchievementsText.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24));
            noAchievementsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            achievementsContainer.addView(noAchievementsText);
            return;
        }

        for (UserAchievement userAchievement : userAchievements) {
            Achievement achievement = achievementDao.findById(userAchievement.getAchievementId());
            if (achievement != null) {
                View achievementCard = createAchievementCard(achievement, userAchievement);
                achievementsContainer.addView(achievementCard);
            }
        }
    }

    private View createAchievementCard(Achievement achievement, UserAchievement userAchievement) {
        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setRadius(dpToPx(16));
        cardView.setCardElevation(dpToPx(2));
        cardView.setStrokeWidth(dpToPx(1));
        cardView.setStrokeColor(MaterialColors.getColor(cardView, com.google.android.material.R.attr.colorOutline));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        cardView.setLayoutParams(cardParams);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        cardView.addView(cardLayout);

        TextView nameText = new TextView(this);
        nameText.setText(achievement.getName());
        nameText.setTextSize(18);
        nameText.setTextColor(MaterialColors.getColor(nameText, com.google.android.material.R.attr.colorOnSurface));
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        cardLayout.addView(nameText);

        TextView descriptionText = new TextView(this);
        descriptionText.setText(achievement.getDescription());
        descriptionText.setTextSize(14);
        descriptionText.setTextColor(MaterialColors.getColor(descriptionText, com.google.android.material.R.attr.colorOnSurfaceVariant));
        descriptionText.setPadding(0, dpToPx(4), 0, dpToPx(8));
        cardLayout.addView(descriptionText);

        LinearLayout categoryLayout = new LinearLayout(this);
        categoryLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView categoryText = new TextView(this);
        categoryText.setText(String.format("Category: %s", achievement.getCategory().getDisplayName()));
        categoryText.setTextSize(12);
        categoryText.setTextColor(MaterialColors.getColor(categoryText, com.google.android.material.R.attr.colorPrimary));
        categoryLayout.addView(categoryText);

        TextView typeText = new TextView(this);
        typeText.setText(String.format(" | Type: %s", achievement.getType().getDisplayName()));
        typeText.setTextSize(12);
        typeText.setTextColor(MaterialColors.getColor(typeText, com.google.android.material.R.attr.colorSecondary));
        categoryLayout.addView(typeText);

        cardLayout.addView(categoryLayout);

        TextView unlockTimeText = new TextView(this);
        unlockTimeText.setText(String.format("Unlocked: %s",
                userAchievement.getUnlockedDate() != null
                        ? dateFormat.format(userAchievement.getUnlockedDate())
                        : "Unknown"));
        unlockTimeText.setTextSize(12);
        unlockTimeText.setTextColor(MaterialColors.getColor(unlockTimeText, com.google.android.material.R.attr.colorPrimary));
        unlockTimeText.setPadding(0, dpToPx(8), 0, 0);
        cardLayout.addView(unlockTimeText);

        if (achievement.getRewardPoints() > 0) {
            TextView rewardText = new TextView(this);
            rewardText.setText(String.format("Reward Points: %d", achievement.getRewardPoints()));
            rewardText.setTextSize(12);
            rewardText.setTextColor(MaterialColors.getColor(rewardText, com.google.android.material.R.attr.colorError));
            rewardText.setPadding(0, dpToPx(4), 0, 0);
            cardLayout.addView(rewardText);
        }

        return cardView;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}

