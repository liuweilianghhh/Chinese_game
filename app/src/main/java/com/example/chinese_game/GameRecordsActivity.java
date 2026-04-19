package com.example.chinese_game;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chinese_game.dao.GameScoreDao;
import com.example.chinese_game.javabean.GameScore;
import com.example.chinese_game.utils.DaoFactory;
import com.example.chinese_game.view.AccuracyChartView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GameRecordsActivity extends AppCompatActivity {

    private LinearLayout recordsContainer;
    private TextView tvRecordsSummary;
    private TextView tvRecordsSectionTitle;
    private Button btnBack;
    private AccuracyChartView accuracyChartView;
    private MaterialButtonToggleGroup toggleGameType;
    private MaterialButtonToggleGroup toggleTimeRange;

    private GameScoreDao gameScoreDao;
    private int userId;
    private GameScore.GameType selectedGameType = GameScore.GameType.CHARACTER_MATCHING;
    private int selectedRangeDays = 30;

    private final SimpleDateFormat recordDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final SimpleDateFormat chartLabelFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_records);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        gameScoreDao = DaoFactory.getGameScoreDao(this);
        initViews();
        setupFilters();
        refreshContent();
    }

    private void initViews() {
        recordsContainer = findViewById(R.id.records_container);
        tvRecordsSummary = findViewById(R.id.tv_records_summary);
        tvRecordsSectionTitle = findViewById(R.id.tv_records_section_title);
        btnBack = findViewById(R.id.btn_back_records);
        accuracyChartView = findViewById(R.id.accuracy_chart_view);
        toggleGameType = findViewById(R.id.toggle_game_type);
        toggleTimeRange = findViewById(R.id.toggle_time_range);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupFilters() {
        toggleGameType.check(R.id.btn_filter_character_matching);
        toggleTimeRange.check(R.id.btn_range_30);

        toggleGameType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btn_filter_character_matching) {
                selectedGameType = GameScore.GameType.CHARACTER_MATCHING;
            } else if (checkedId == R.id.btn_filter_pronunciation) {
                selectedGameType = GameScore.GameType.PRONUNCIATION_QUIZ;
            } else if (checkedId == R.id.btn_filter_word_puzzle) {
                selectedGameType = GameScore.GameType.WORD_PUZZLE;
            }
            refreshContent();
        });

        toggleTimeRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btn_range_7) {
                selectedRangeDays = 7;
            } else if (checkedId == R.id.btn_range_30) {
                selectedRangeDays = 30;
            } else if (checkedId == R.id.btn_range_90) {
                selectedRangeDays = 90;
            } else if (checkedId == R.id.btn_range_all) {
                selectedRangeDays = -1;
            }
            refreshContent();
        });
    }

    private void refreshContent() {
        List<GameScore> scores = gameScoreDao.findByUserIdAndGameType(userId, selectedGameType);
        List<GameScore> filteredScores = filterCompletedScores(scores);
        filteredScores = filterByTimeRange(filteredScores);

        List<GameScore> ascendingScores = new ArrayList<>(filteredScores);
        ascendingScores.sort(Comparator.comparing(GameScore::getPlayDate, Comparator.nullsLast(Date::compareTo)));
        accuracyChartView.setPoints(toChartPoints(ascendingScores));

        List<GameScore> descendingScores = new ArrayList<>(filteredScores);
        descendingScores.sort((left, right) -> {
            Date leftDate = left.getPlayDate();
            Date rightDate = right.getPlayDate();
            if (leftDate == null && rightDate == null) {
                return 0;
            }
            if (leftDate == null) {
                return 1;
            }
            if (rightDate == null) {
                return -1;
            }
            return rightDate.compareTo(leftDate);
        });

        updateSummary(filteredScores);
        renderRecordList(descendingScores);
    }

    private List<GameScore> filterCompletedScores(List<GameScore> scores) {
        List<GameScore> completedScores = new ArrayList<>();
        if (scores == null) {
            return completedScores;
        }
        for (GameScore score : scores) {
            if (score != null && score.isCompleted()) {
                completedScores.add(score);
            }
        }
        return completedScores;
    }

    private List<GameScore> filterByTimeRange(List<GameScore> scores) {
        if (selectedRangeDays <= 0) {
            return scores;
        }

        long threshold = System.currentTimeMillis() - selectedRangeDays * 24L * 60L * 60L * 1000L;
        List<GameScore> filtered = new ArrayList<>();
        for (GameScore score : scores) {
            if (score.getPlayDate() != null && score.getPlayDate().getTime() >= threshold) {
                filtered.add(score);
            }
        }
        return filtered;
    }

    private List<AccuracyChartView.ChartPoint> toChartPoints(List<GameScore> scores) {
        List<AccuracyChartView.ChartPoint> points = new ArrayList<>();
        for (GameScore score : scores) {
            points.add(new AccuracyChartView.ChartPoint(
                    score.getPlayDate() != null ? chartLabelFormat.format(score.getPlayDate()) : "--/--",
                    (float) (resolveAccuracy(score) * 100f)
            ));
        }
        return points;
    }

    private void updateSummary(List<GameScore> scores) {
        String rangeLabel = selectedRangeDays > 0 ? selectedRangeDays + "D" : "All Time";
        tvRecordsSectionTitle.setText(selectedGameType.getDisplayName() + " Records");

        if (scores.isEmpty()) {
            tvRecordsSummary.setText(selectedGameType.getDisplayName() + " · " + rangeLabel + " · No completed records");
            return;
        }

        double totalAccuracy = 0.0;
        int bestScore = 0;
        int bestMaxScore = 0;
        for (GameScore score : scores) {
            totalAccuracy += resolveAccuracy(score);
            if (score.getScore() > bestScore) {
                bestScore = score.getScore();
                bestMaxScore = score.getMaxPossibleScore();
            }
        }

        double averageAccuracy = totalAccuracy / scores.size();
        tvRecordsSummary.setText(String.format(
                Locale.getDefault(),
                "%s · %s · %d record(s) · Avg accuracy %.0f%% · Best score %d/%d",
                selectedGameType.getDisplayName(),
                rangeLabel,
                scores.size(),
                averageAccuracy * 100,
                bestScore,
                bestMaxScore
        ));
    }

    private void renderRecordList(List<GameScore> scores) {
        recordsContainer.removeAllViews();

        if (scores.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No completed records in the selected range.");
            emptyText.setTextSize(15);
            emptyText.setTextColor(getColor(R.color.duo_text_muted));
            emptyText.setPadding(0, dpToPx(8), 0, dpToPx(8));
            recordsContainer.addView(emptyText);
            return;
        }

        for (GameScore score : scores) {
            recordsContainer.addView(createRecordCard(score));
        }
    }

    private MaterialCardView createRecordCard(GameScore score) {
        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setRadius(dpToPx(16));
        cardView.setCardElevation(0f);
        cardView.setStrokeWidth(dpToPx(1));
        cardView.setStrokeColor(Color.parseColor("#33213A12"));
        cardView.setCardBackgroundColor(Color.parseColor("#FCFFFFFF"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dpToPx(10);
        cardView.setLayoutParams(params);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        cardView.addView(layout);

        TextView title = new TextView(this);
        title.setText(recordDateFormat.format(score.getPlayDate() != null ? score.getPlayDate() : new Date()));
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getColor(R.color.duo_text_dark));
        layout.addView(title);

        TextView meta = new TextView(this);
        meta.setText(String.format(
                Locale.getDefault(),
                "Difficulty %s  •  Score %d/%d  •  Accuracy %.0f%%",
                score.getDifficulty() != null ? score.getDifficulty().getDisplayName() : "Unknown",
                score.getScore(),
                score.getMaxPossibleScore(),
                resolveAccuracy(score) * 100
        ));
        meta.setTextSize(13);
        meta.setTextColor(getColor(R.color.duo_text_dark));
        meta.setPadding(0, dpToPx(6), 0, 0);
        layout.addView(meta);

        TextView timeText = new TextView(this);
        timeText.setText("Time spent " + formatDuration(score.getTimeSpent()));
        timeText.setTextSize(12);
        timeText.setTextColor(getColor(R.color.duo_text_muted));
        timeText.setPadding(0, dpToPx(6), 0, 0);
        layout.addView(timeText);

        return cardView;
    }

    private double resolveAccuracy(GameScore score) {
        if (score.getAccuracy() > 0) {
            return score.getAccuracy();
        }
        if (score.getMaxPossibleScore() > 0) {
            return (double) score.getScore() / score.getMaxPossibleScore();
        }
        return 0.0;
    }

    private String formatDuration(long timeSpentMs) {
        long totalSeconds = Math.max(0L, timeSpentMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
