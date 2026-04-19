package com.example.chinese_game;

import android.annotation.SuppressLint;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.example.chinese_game.dao.UserDao;
import com.example.chinese_game.dao.GameWordDao;
import com.example.chinese_game.dao.GameQuestionDetailDao;
import com.example.chinese_game.dao.GameScoreDao;
import com.example.chinese_game.javabean.CharacterMatching;
import com.example.chinese_game.javabean.GameQuestionDetail;
import com.example.chinese_game.javabean.GameScore;
import com.example.chinese_game.javabean.SentenceWord;
import com.example.chinese_game.speech.IFlyTekConfig;
import com.example.chinese_game.speech.IseEvaluator;
import com.example.chinese_game.speech.SpeechRecognitionConfig;
import com.example.chinese_game.speech.SpeechRecognitionHelper;
import com.example.chinese_game.speech.SpeechRecognitionManager;
import com.example.chinese_game.speech.SpeechRecognitionResult;
import com.example.chinese_game.utils.DaoFactory;
import com.example.chinese_game.view.VoiceWaveView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class GameActivity extends AppCompatActivity {

    private String gameType;
    private int userId;
    private TextView tvGameType, tvMessage, tvProgress, tvPinyinQuestion, tvQuestionHint, tvResult;
    private View panelCharacterMatching, panelWordPuzzle, panelPronunciationQuiz;
    private Button btnOpt1, btnOpt2, btnOpt3, btnOpt4, btnBackToMenu, btnBackPlaceholder, btnMusicSettings;
    private TextView tvWordPuzzleProgress, tvWordPuzzleContext, tvWordPuzzleHint, tvWordPuzzleResult;
    private LinearLayout layoutWordPuzzleSlots, layoutWordPuzzleSource;
    private Button btnWordPuzzleReset, btnWordPuzzleSubmit, btnWordPuzzleBack;
    private Button btnStartRecording, btnNextQuestion;
    private VoiceWaveView voiceWaveView;
    private TextView tvRecognitionStatus, tvPronunciationWord, tvPronunciationPinyin, tvPronunciationProgress;
    private TextView tvCurrentScore, tvPronunciationResult, tvPronunciationInstruction;
    private TextView tvFeedbackBanner;
    private boolean isHoldRecording = false;
    // Question settings
    private static final int QUESTIONS_PER_GAME = 10;
    private static final int MAX_SCORE_PER_QUESTION = 10;
    private static final int LONG_SENTENCE_CHAR_THRESHOLD = 14;
    private static final int PUZZLE_BLOCK_COUNT = 5;
    private static final int LONG_SENTENCE_WORD_THRESHOLD = 9;
    private static final int MAX_SENTENCE_POOL = 60;
    private static final int MAX_WORD_PUZZLE_TOKEN_LENGTH = 5;
    private static final String BLANK_PLACEHOLDER = "___";
    private static final Random RANDOM = new Random();
    private static final Set<Integer> EXCLUDED_WORD_PUZZLE_SENTENCE_IDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    10, 15, 26, 30, 36, 37, 41, 42, 44, 46, 48
            ))
    );
    private List<CharacterMatching> questions;
    private int currentIndex;
    private int correctCount;
    private long gameStartTime;
    private long questionStartTime;
    private List<QuestionResult> questionResults;
    private List<String> currentOptions;
    /** game_scores row id for this session; 0 if not started or no record */
    private long currentGameScoreId;
    /** true when game ends normally (all questions done) */
    private boolean gameCompleted;
    private String correctAnswer;
    private GameWordDao gameWordDao;
    private GameScoreDao gameScoreDao;
    private GameQuestionDetailDao gameQuestionDetailDao;
    private UserDao userDao;
    private MYsqliteopenhelper dbHelper;
    private List<PuzzleQuestion> puzzleQuestions;
    private PuzzleQuestion currentPuzzleQuestion;
    private final List<FrameLayout> wordPuzzleSlots = new ArrayList<>();
    private SpeechRecognitionHelper speechHelper;
    private IseEvaluator iseEvaluator;
    private BackgroundMusicManager backgroundMusicManager;
    private boolean sessionStatsSynced;
    private int[] pronScores;           // 濮ｅ繘顣藉妤€鍨?(0閳?0)
    private int currentQuestionScore;   // 瑜版挸澧犳０妯绘付閸氬簼绔村▎鈥崇繁閸?
    /** 閸楁洟顣界粵鏃堫暯缂佹挻鐏夐敍宀€鏁ゆ禍搴ｇ波閺夌喎鎮楅崘娆忓弳 game_question_details */
    private static class QuestionResult {
        int questionId;   // game_words.id
        String userAnswer;
        boolean correct;
        long timeSpentMs;
        QuestionResult(int questionId, String userAnswer, boolean correct, long timeSpentMs) {
            this.questionId = questionId;
            this.userAnswer = userAnswer;
            this.correct = correct;
            this.timeSpentMs = timeSpentMs;
        }
    }

    private static class SentenceToken {
        String word;
        int wordOrder;
        int wordPosition;

        SentenceToken(String word, int wordOrder, int wordPosition) {
            this.word = word;
            this.wordOrder = wordOrder;
            this.wordPosition = wordPosition;
        }
    }

    private static class PuzzleQuestion {
        int sentenceId;
        String sentence;
        String displayPrompt;
        String correctAnswer;
        List<String> options;
        boolean blankMode;
        String contextPrompt;
        String hint;
        List<String> targetBlocks;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameType = getIntent().getStringExtra("GAME_TYPE");
        userId = getIntent().getIntExtra("USER_ID", -1);

        tvGameType = findViewById(R.id.tv_game_type);
        btnMusicSettings = findViewById(R.id.btn_music_settings);
        tvFeedbackBanner = findViewById(R.id.tv_feedback_banner);
        tvMessage = findViewById(R.id.tv_message);
        tvProgress = findViewById(R.id.tv_progress);
        tvPinyinQuestion = findViewById(R.id.tv_pinyin_question);
        tvQuestionHint = findViewById(R.id.tv_question_hint);
        tvResult = findViewById(R.id.tv_result);
        panelCharacterMatching = findViewById(R.id.panel_character_matching);
        panelWordPuzzle = findViewById(R.id.panel_word_puzzle);
        panelPronunciationQuiz = findViewById(R.id.panel_pronunciation_quiz);
        btnOpt1 = findViewById(R.id.btn_opt1);
        btnOpt2 = findViewById(R.id.btn_opt2);
        btnOpt3 = findViewById(R.id.btn_opt3);
        btnOpt4 = findViewById(R.id.btn_opt4);
        btnBackToMenu = findViewById(R.id.btn_back_to_menu);
        btnBackPlaceholder = findViewById(R.id.btn_back_to_menu_placeholder);

        tvWordPuzzleProgress = findViewById(R.id.tv_word_puzzle_progress);
        tvWordPuzzleContext = findViewById(R.id.tv_word_puzzle_context);
        tvWordPuzzleHint = findViewById(R.id.tv_word_puzzle_hint);
        tvWordPuzzleResult = findViewById(R.id.tv_word_puzzle_result);
        layoutWordPuzzleSlots = findViewById(R.id.layout_word_puzzle_slots);
        layoutWordPuzzleSource = findViewById(R.id.layout_word_puzzle_source);
        btnWordPuzzleReset = findViewById(R.id.btn_word_puzzle_reset);
        btnWordPuzzleSubmit = findViewById(R.id.btn_word_puzzle_submit);
        btnWordPuzzleBack = findViewById(R.id.btn_word_puzzle_back);
        btnStartRecording = findViewById(R.id.btn_start_recording);
        btnNextQuestion = findViewById(R.id.btn_next_question);
        voiceWaveView = findViewById(R.id.voice_wave_view);
        tvRecognitionStatus = findViewById(R.id.tv_recognition_status);
        tvPronunciationWord = findViewById(R.id.tv_pronunciation_word);
        tvPronunciationPinyin = findViewById(R.id.tv_pronunciation_pinyin);
        tvPronunciationProgress = findViewById(R.id.tv_pronunciation_progress);
        tvCurrentScore = findViewById(R.id.tv_current_score);
        tvPronunciationResult = findViewById(R.id.tv_pronunciation_result);
        tvPronunciationInstruction = findViewById(R.id.tv_pronunciation_instruction);
        dbHelper = new MYsqliteopenhelper(this);
        userDao = DaoFactory.getUserDao(this);
        backgroundMusicManager = BackgroundMusicManager.getInstance(this);
        sessionStatsSynced = false;
        refreshMusicButtonState();

        if (btnMusicSettings != null) {
            btnMusicSettings.setOnClickListener(v -> toggleMusic());
        }

        if ("CHARACTER_MATCHING".equals(gameType)) {
            tvGameType.setText("Character Matching");
            tvMessage.setVisibility(View.GONE);
            btnBackPlaceholder.setVisibility(View.GONE);
            panelCharacterMatching.setVisibility(View.VISIBLE);
            if (panelWordPuzzle != null) panelWordPuzzle.setVisibility(View.GONE);
            gameWordDao = DaoFactory.getGameWordDao(this);
            gameScoreDao = DaoFactory.getGameScoreDao(this);
            gameQuestionDetailDao = DaoFactory.getGameQuestionDetailDao(this);
            startCharacterMatchingGame();
        } else if ("PRONUNCIATION_QUIZ".equals(gameType)) {
            tvGameType.setText("Pronunciation Quiz");
            tvMessage.setVisibility(View.GONE);
            btnBackPlaceholder.setVisibility(View.GONE);
            panelCharacterMatching.setVisibility(View.GONE);
            if (panelWordPuzzle != null) panelWordPuzzle.setVisibility(View.GONE);
            if (panelPronunciationQuiz != null) {
                panelPronunciationQuiz.setVisibility(View.VISIBLE);
            }
            gameWordDao = DaoFactory.getGameWordDao(this);
            gameScoreDao = DaoFactory.getGameScoreDao(this);
            gameQuestionDetailDao = DaoFactory.getGameQuestionDetailDao(this);
            initializeSpeechRecognition();
            startPronunciationQuizGame();
        } else if ("WORD_PUZZLE".equals(gameType)) {
            tvGameType.setText("Word Puzzle");
            tvMessage.setVisibility(View.GONE);
            btnBackPlaceholder.setVisibility(View.GONE);
            panelCharacterMatching.setVisibility(View.GONE);
            if (panelWordPuzzle != null) panelWordPuzzle.setVisibility(View.VISIBLE);
            if (panelPronunciationQuiz != null) {
                panelPronunciationQuiz.setVisibility(View.GONE);
            }
            gameScoreDao = DaoFactory.getGameScoreDao(this);
            gameQuestionDetailDao = DaoFactory.getGameQuestionDetailDao(this);
            startWordPuzzleDragGame();
        } else {
            tvGameType.setText("Game: " + gameType);
            tvMessage.setVisibility(View.VISIBLE);
            tvMessage.setText("This game is coming soon.\n\nCheck back later!");
            panelCharacterMatching.setVisibility(View.GONE);
            if (panelWordPuzzle != null) panelWordPuzzle.setVisibility(View.GONE);
            if (panelPronunciationQuiz != null) {
                panelPronunciationQuiz.setVisibility(View.GONE);
            }
            btnBackPlaceholder.setVisibility(View.VISIBLE);
            btnBackPlaceholder.setOnClickListener(v -> finish());
        }
    }

    private void startCharacterMatchingGame() {
        String difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "EASY";

        // 妫版娲伴悽?game_words 閸斻劍鈧胶鏁撻幋鎰剁礉娣囨繆鐦?10 闁挷绗夐柌宥咁槻
        questions = gameWordDao.getRandomGameWordsForGame(QUESTIONS_PER_GAME, difficulty);
        if (questions == null) questions = new ArrayList<>();
        if (questions.size() < QUESTIONS_PER_GAME) {
            tvPinyinQuestion.setText("Not enough questions.");
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setText("Only " + questions.size() + " question(s) available. Please tap Reload to import game_words data (need at least 10 entries).");
            setOptionsVisible(0);
            btnBackToMenu.setOnClickListener(v -> finish());
            return;
        }
        if (questions.size() > QUESTIONS_PER_GAME) {
            questions = new ArrayList<>(questions.subList(0, QUESTIONS_PER_GAME));
        }

        currentIndex = 0;
        correctCount = 0;
        questionResults = new ArrayList<>();
        gameCompleted = false;
        gameStartTime = System.currentTimeMillis();

        GameScore gs = createGameScoreForSession(difficulty, 0, 0, 0.0, 0L, false);
        currentGameScoreId = gameScoreDao.save(gs);
        if (currentGameScoreId <= 0) currentGameScoreId = 0;

        if (currentGameScoreId > 0) {
            List<GameQuestionDetail> placeholders = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                GameQuestionDetail d = new GameQuestionDetail();
                d.setGameScoreId((int) currentGameScoreId);
                d.setQuestionOrder(i + 1);
                d.setQuestionId(questions.get(i).getId());
                d.setUserAnswer("");
                d.setCorrect(false);
                d.setTimeSpent(0L);
                d.setQuestionScore(0);
                d.setMaxScore(MAX_SCORE_PER_QUESTION);
                placeholders.add(d);
            }
            gameQuestionDetailDao.insertBatch(placeholders);
        }

        btnBackToMenu.setOnClickListener(v -> onBackToMenuClicked());
        showCurrentQuestion();
    }

    private GameScore createGameScoreForSession(String difficultyStr, int score, int maxPossibleScore, double accuracy, long timeSpent, boolean completed) {
        GameScore.Difficulty diff = GameScore.Difficulty.EASY;
        try { diff = GameScore.Difficulty.valueOf(difficultyStr); } catch (Exception ignored) { }
        GameScore gs = new GameScore();
        gs.setUserId(userId);
        gs.setGameType(GameScore.GameType.CHARACTER_MATCHING);
        gs.setDifficulty(diff);
        gs.setScore(score);
        gs.setMaxPossibleScore(maxPossibleScore);
        gs.setAccuracy(accuracy);
        gs.setTimeSpent(timeSpent);
        gs.setPlayDate(new java.util.Date());
        gs.setCompleted(completed);
        return gs;
    }

    private void onBackToMenuClicked() {
        if (currentGameScoreId > 0 && !gameCompleted) {
            boolean saved = false;
            if ("WORD_PUZZLE".equals(gameType) && puzzleQuestions != null && currentIndex < puzzleQuestions.size()) {
                saveIncompleteWordPuzzleDragGame();
                saved = true;
            } else if ("CHARACTER_MATCHING".equals(gameType) && questions != null && currentIndex < questions.size()) {
                saveIncompleteGame();
                saved = true;
            }
            if (saved) {
                Toast.makeText(this, "Game incomplete. Progress saved.", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    /** Update game_score as incomplete (e.g. back or exit). */
    private void saveIncompleteGame() {
        if (currentGameScoreId <= 0 || questions == null) return;
        int answered = currentIndex;
        int maxSoFar = answered * MAX_SCORE_PER_QUESTION;
        int scoreSoFar = answered > 0 ? (correctCount * maxSoFar / answered) : 0;
        double accuracySoFar = answered > 0 ? (double) correctCount / answered : 0.0;
        long timeSpentSoFar = System.currentTimeMillis() - gameStartTime;
        String difficultyStr = getIntent().getStringExtra("DIFFICULTY");
        if (difficultyStr == null) difficultyStr = "EASY";
        GameScore gs = createGameScoreForSession(difficultyStr, scoreSoFar, answered * MAX_SCORE_PER_QUESTION, accuracySoFar, timeSpentSoFar, false);
        gs.setId((int) currentGameScoreId);
        gameScoreDao.update(gs);
    }

    private void showCurrentQuestion() {
        if (currentIndex >= questions.size()) {
            endCharacterMatchingGame();
            return;
        }
        questionStartTime = System.currentTimeMillis();
        CharacterMatching q = questions.get(currentIndex);
        String pinyin = q.getPinyin() != null ? q.getPinyin() : "";
        correctAnswer = q.getWord();
        if (correctAnswer == null) correctAnswer = "";

        tvPinyinQuestion.setText(pinyin);
        tvQuestionHint.setText(q.getHint() != null ? q.getHint() : "");
        tvProgress.setText((currentIndex + 1) + " / " + questions.size());
        if (tvFeedbackBanner != null) tvFeedbackBanner.setVisibility(View.GONE);

        String posTag = q.getPosTag();
        if (posTag == null || posTag.isEmpty()) posTag = "X";
        List<SentenceWord> distractors = gameWordDao.getRandomWordsByPosTag(posTag, q.getWordId(), 3);

        currentOptions = new ArrayList<>();
        currentOptions.add(correctAnswer);
        for (SentenceWord w : distractors) {
            if (w.getWord() != null && !currentOptions.contains(w.getWord())) {
                currentOptions.add(w.getWord());
            }
        }
        Collections.shuffle(currentOptions);
        int n = currentOptions.size();
        if (n > 0) btnOpt1.setText(currentOptions.get(0));
        if (n > 1) btnOpt2.setText(currentOptions.get(1));
        if (n > 2) btnOpt3.setText(currentOptions.get(2));
        if (n > 3) btnOpt4.setText(currentOptions.get(3));
        setOptionsVisible(n);
        setOptionsClickable(true);
    }

    private void setOptionsVisible(int count) {
        btnOpt1.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        btnOpt2.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
        btnOpt3.setVisibility(count > 2 ? View.VISIBLE : View.GONE);
        btnOpt4.setVisibility(count > 3 ? View.VISIBLE : View.GONE);
    }

    private void setOptionsClickable(boolean enabled) {
        btnOpt1.setEnabled(enabled);
        btnOpt2.setEnabled(enabled);
        btnOpt3.setEnabled(enabled);
        btnOpt4.setEnabled(enabled);
    }

    private void showAnswerFeedback(boolean correct, String text) {
        if (tvFeedbackBanner == null) return;
        tvFeedbackBanner.animate().cancel();
        tvFeedbackBanner.setVisibility(View.VISIBLE);
        tvFeedbackBanner.setText(text);
        tvFeedbackBanner.setBackgroundResource(correct
                ? R.drawable.ui_bg_feedback_correct
                : R.drawable.ui_bg_feedback_wrong);
        Drawable icon = getDrawable(correct
                ? R.drawable.ui_icon_check_badge
                : R.drawable.ui_icon_wrong_badge);
        tvFeedbackBanner.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        tvFeedbackBanner.setAlpha(0f);
        tvFeedbackBanner.setScaleX(0.9f);
        tvFeedbackBanner.setScaleY(0.9f);
        tvFeedbackBanner.setTranslationY(-14f);
        tvFeedbackBanner.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start();
        tvFeedbackBanner.postDelayed(() -> {
            tvFeedbackBanner.animate()
                    .alpha(0f)
                    .translationY(-10f)
                    .setDuration(220)
                    .withEndAction(() -> {
                        if (tvFeedbackBanner != null) {
                            tvFeedbackBanner.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }, 850);
    }

    private void animateAnswerTarget(View target, boolean correct) {
        if (target == null) return;
        if (correct) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.08f, 1f),
                    ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.08f, 1f)
            );
            set.setDuration(260);
            set.start();
            return;
        }
        target.animate().translationXBy(12f).setDuration(40).withEndAction(() ->
                target.animate().translationXBy(-24f).setDuration(80).withEndAction(() ->
                        target.animate().translationXBy(16f).setDuration(80).withEndAction(() ->
                                target.animate().translationX(0f).setDuration(60).start()
                        ).start()
                ).start()
        ).start();
    }

    private void setMicRecordingVisual(boolean recording) {
        if (btnStartRecording == null) return;
        btnStartRecording.setBackgroundResource(recording
                ? R.drawable.ui_bg_mic_recording
                : R.drawable.ui_bg_mic_idle);
        if (recording) {
            btnStartRecording.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start();
        } else {
            btnStartRecording.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
        }
    }

    public void onOptionClick(View view) {
        if (!"CHARACTER_MATCHING".equals(gameType)) return;
        if (currentIndex >= questions.size() || currentOptions == null) return;
        setOptionsClickable(false);

        // 閺堫剟顣介懓妤佹閿涘牊顕犵粔鎺炵礆
        long timeSpentMs = System.currentTimeMillis() - questionStartTime;
        int idx = view.getId() == R.id.btn_opt1 ? 0 : view.getId() == R.id.btn_opt2 ? 1 : view.getId() == R.id.btn_opt3 ? 2 : 3;
        String chosen = (idx >= 0 && idx < currentOptions.size()) ? currentOptions.get(idx) : null;
        boolean correct = correctAnswer.equals(chosen);
        if (correct) correctCount++;

        CharacterMatching q = questions.get(currentIndex);
        QuestionResult qr = new QuestionResult(q.getId(), chosen != null ? chosen : "", correct, timeSpentMs);
        questionResults.add(qr);

        if (currentGameScoreId > 0) {
            boolean updated = gameQuestionDetailDao.updateByGameScoreAndOrder((int) currentGameScoreId, currentIndex + 1, qr.userAnswer, qr.correct, qr.timeSpentMs, qr.correct ? MAX_SCORE_PER_QUESTION : 0);
            if (!updated) {
                GameQuestionDetail d = new GameQuestionDetail();
                d.setGameScoreId((int) currentGameScoreId);
                d.setQuestionOrder(currentIndex + 1);
                d.setQuestionId(q.getId());
                d.setUserAnswer(qr.userAnswer);
                d.setCorrect(qr.correct);
                d.setTimeSpent(qr.timeSpentMs);
                d.setQuestionScore(qr.correct ? MAX_SCORE_PER_QUESTION : 0);
                d.setMaxScore(MAX_SCORE_PER_QUESTION);
                gameQuestionDetailDao.insert(d);
            }
        }

        animateAnswerTarget(view, correct);
        showAnswerFeedback(correct, correct ? "Great! Correct answer." : "Oops! Correct: " + correctAnswer);

        view.postDelayed(() -> {
            currentIndex++;
            showCurrentQuestion();
        }, 700);
    }

    private void endCharacterMatchingGame() {
        gameCompleted = true;
        int total = questions.size();
        int maxScoreTotal = total * MAX_SCORE_PER_QUESTION;
        int score = total > 0 ? (correctCount * maxScoreTotal / total) : 0;
        double accuracy = total > 0 ? (double) correctCount / total : 0;
        long timeSpent = System.currentTimeMillis() - gameStartTime;

        String difficultyStr = getIntent().getStringExtra("DIFFICULTY");
        if (difficultyStr == null && !questions.isEmpty()) difficultyStr = questions.get(0).getDifficulty();
        if (difficultyStr == null) difficultyStr = "EASY";

        if (currentGameScoreId > 0) {
            GameScore gs = createGameScoreForSession(difficultyStr, score, maxScoreTotal, accuracy, timeSpent, true);
            gs.setId((int) currentGameScoreId);
            gameScoreDao.update(gs);
        }
        syncCompletedSessionStats(score);

        tvPinyinQuestion.setVisibility(View.GONE);
        tvQuestionHint.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);
        setOptionsVisible(0);
        renderResultCard(
                tvResult,
                "Character Matching",
                "Correct",
                correctCount + " / " + total,
                score,
                maxScoreTotal,
                accuracy,
                timeSpent
        );
        btnBackToMenu.setVisibility(View.VISIBLE);
    }

    // ==================== 閸欐垿鐓跺ù瀣崣濞撳憡鍨?====================
    
    private void startWordPuzzleGame() {
        String difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "EASY";

        puzzleQuestions = loadWordPuzzleQuestions(difficulty, QUESTIONS_PER_GAME);
        if (puzzleQuestions == null) puzzleQuestions = new ArrayList<>();

        if (puzzleQuestions.size() < QUESTIONS_PER_GAME) {
            tvPinyinQuestion.setText("Not enough sentence questions.");
            tvQuestionHint.setText("Please check sentences table data.");
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setText("Only " + puzzleQuestions.size() + " question(s) available. Need at least 10 sentences for this difficulty.");
            setOptionsVisible(0);
            btnBackToMenu.setOnClickListener(v -> finish());
            return;
        }
        if (puzzleQuestions.size() > QUESTIONS_PER_GAME) {
            puzzleQuestions = new ArrayList<>(puzzleQuestions.subList(0, QUESTIONS_PER_GAME));
        }

        currentIndex = 0;
        correctCount = 0;
        questionResults = new ArrayList<>();
        gameCompleted = false;
        gameStartTime = System.currentTimeMillis();

        GameScore gs = createGameScoreForWordPuzzle(difficulty, 0, 0, 0.0, 0L, false);
        currentGameScoreId = gameScoreDao.save(gs);
        if (currentGameScoreId <= 0) currentGameScoreId = 0;

        if (currentGameScoreId > 0) {
            List<GameQuestionDetail> placeholders = new ArrayList<>();
            for (int i = 0; i < puzzleQuestions.size(); i++) {
                GameQuestionDetail d = new GameQuestionDetail();
                d.setGameScoreId((int) currentGameScoreId);
                d.setQuestionOrder(i + 1);
                d.setQuestionId(puzzleQuestions.get(i).sentenceId);
                d.setUserAnswer("");
                d.setCorrect(false);
                d.setTimeSpent(0L);
                d.setQuestionScore(0);
                d.setMaxScore(MAX_SCORE_PER_QUESTION);
                placeholders.add(d);
            }
            gameQuestionDetailDao.insertBatch(placeholders);
        }

        btnBackToMenu.setOnClickListener(v -> onBackToMenuClicked());
        showCurrentWordPuzzleQuestion();
    }

    private GameScore createGameScoreForWordPuzzle(String difficultyStr, int score, int maxPossibleScore, double accuracy, long timeSpent, boolean completed) {
        GameScore.Difficulty diff = GameScore.Difficulty.EASY;
        try { diff = GameScore.Difficulty.valueOf(difficultyStr); } catch (Exception ignored) { }
        GameScore gs = new GameScore();
        gs.setUserId(userId);
        gs.setGameType(GameScore.GameType.WORD_PUZZLE);
        gs.setDifficulty(diff);
        gs.setScore(score);
        gs.setMaxPossibleScore(maxPossibleScore);
        gs.setAccuracy(accuracy);
        gs.setTimeSpent(timeSpent);
        gs.setPlayDate(new java.util.Date());
        gs.setCompleted(completed);
        return gs;
    }

    private List<PuzzleQuestion> loadWordPuzzleQuestions(String difficulty, int count) {
        List<PuzzleQuestion> result = new ArrayList<>();
        Set<Integer> usedSentenceIds = new HashSet<>();
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT id, sentence, difficulty FROM sentences WHERE difficulty = ? ORDER BY RANDOM() LIMIT ?",
                    new String[]{difficulty, String.valueOf(MAX_SENTENCE_POOL)});
            while (cursor.moveToNext() && result.size() < count) {
                int sentenceId = cursor.getInt(0);
                String sentence = cursor.getString(1);
                String sentenceDifficulty = cursor.getString(2);
                PuzzleQuestion q = buildWordPuzzleQuestion(sentenceId, sentence, sentenceDifficulty);
                if (q != null) {
                    result.add(q);
                    usedSentenceIds.add(sentenceId);
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (result.size() >= count) return result;

        try {
            cursor = db.rawQuery(
                    "SELECT id, sentence, difficulty FROM sentences ORDER BY RANDOM() LIMIT ?",
                    new String[]{String.valueOf(MAX_SENTENCE_POOL)});
            while (cursor.moveToNext() && result.size() < count) {
                int sentenceId = cursor.getInt(0);
                if (usedSentenceIds.contains(sentenceId)) continue;
                String sentence = cursor.getString(1);
                String sentenceDifficulty = cursor.getString(2);
                PuzzleQuestion q = buildWordPuzzleQuestion(sentenceId, sentence, sentenceDifficulty);
                if (q != null) {
                    result.add(q);
                    usedSentenceIds.add(sentenceId);
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return result;
    }

    private PuzzleQuestion buildWordPuzzleQuestion(int sentenceId, String sentence, String difficulty) {
        if (sentence == null) return null;
        String sentenceText = sentence.trim();
        if (sentenceText.isEmpty()) return null;

        List<SentenceToken> tokens = getSentenceTokens(sentenceId, sentenceText);
        if (tokens.size() < 2) return null;

        boolean useBlankMode = sentenceText.length() >= LONG_SENTENCE_CHAR_THRESHOLD || tokens.size() >= LONG_SENTENCE_WORD_THRESHOLD;
        if (useBlankMode) {
            return buildLongSentenceBlankQuestion(sentenceId, sentenceText, difficulty, tokens);
        }
        return buildShortSentenceReorderQuestion(sentenceId, sentenceText, difficulty, tokens);
    }

    private List<SentenceToken> getSentenceTokens(int sentenceId, String sentenceText) {
        List<SentenceToken> tokens = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT word, word_order, word_position FROM sentence_words WHERE sentence_id = ? ORDER BY word_order ASC",
                    new String[]{String.valueOf(sentenceId)});
            while (cursor.moveToNext()) {
                String word = cursor.getString(0);
                if (word == null || word.trim().isEmpty()) continue;
                int order = cursor.getInt(1);
                int position = cursor.getInt(2);
                tokens.add(new SentenceToken(word, order, position));
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (!tokens.isEmpty()) return tokens;

        int order = 1;
        for (int i = 0; i < sentenceText.length(); i++) {
            char ch = sentenceText.charAt(i);
            if (Character.isWhitespace(ch)) continue;
            tokens.add(new SentenceToken(String.valueOf(ch), order++, i + 1));
        }
        return tokens;
    }

    private PuzzleQuestion buildShortSentenceReorderQuestion(int sentenceId, String sentenceText, String difficulty, List<SentenceToken> tokens) {
        List<String> words = new ArrayList<>();
        for (SentenceToken token : tokens) {
            words.add(token.word);
        }
        if (words.size() < 2) return null;

        PuzzleQuestion q = new PuzzleQuestion();
        q.sentenceId = sentenceId;
        q.sentence = sentenceText;
        q.blankMode = false;
        q.correctAnswer = sentenceText;
        q.displayPrompt = "娑斿崬绨拠宥堫嚔: " + buildShuffledWordsPrompt(words);
        q.hint = "鐠囩兘鈧瀚ㄩ懗鐣岀矋閹存劖顒滅涵顔煎綖鐎涙劗娈戦崣銉ョ摍";
        q.options = buildSentenceOptions(words, sentenceText, difficulty, sentenceId);
        if (q.options == null || q.options.isEmpty()) return null;
        return q;
    }

    private String buildShuffledWordsPrompt(List<String> orderedWords) {
        List<String> shuffled = new ArrayList<>(orderedWords);
        Collections.shuffle(shuffled, RANDOM);
        if (shuffled.equals(orderedWords) && shuffled.size() > 1) {
            Collections.rotate(shuffled, 1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shuffled.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(shuffled.get(i));
        }
        return sb.toString();
    }

    private List<String> buildSentenceOptions(List<String> orderedWords, String correctSentence, String difficulty, int sentenceId) {
        Set<String> optionSet = new HashSet<>();
        optionSet.add(correctSentence);

        int attempts = 0;
        while (optionSet.size() < 4 && attempts < 80) {
            attempts++;
            List<String> shuffled = new ArrayList<>(orderedWords);
            Collections.shuffle(shuffled, RANDOM);
            String candidate = joinWords(shuffled);
            if (!candidate.equals(correctSentence) && !candidate.trim().isEmpty()) {
                optionSet.add(candidate);
            }
        }

        if (optionSet.size() < 4 && orderedWords.size() > 1) {
            List<String> reversed = new ArrayList<>(orderedWords);
            Collections.reverse(reversed);
            String reverseCandidate = joinWords(reversed);
            if (!reverseCandidate.equals(correctSentence)) {
                optionSet.add(reverseCandidate);
            }

            List<String> rotated = new ArrayList<>(orderedWords);
            Collections.rotate(rotated, 1);
            String rotateCandidate = joinWords(rotated);
            if (!rotateCandidate.equals(correctSentence)) {
                optionSet.add(rotateCandidate);
            }
        }

        if (optionSet.size() < 4) {
            List<String> sentenceDistractors = getRandomSentenceDistractors(difficulty, sentenceId, 8);
            for (String distractor : sentenceDistractors) {
                if (optionSet.size() >= 4) break;
                if (distractor == null) continue;
                String candidate = distractor.trim();
                if (!candidate.isEmpty() && !candidate.equals(correctSentence)) {
                    optionSet.add(candidate);
                }
            }
        }

        List<String> options = new ArrayList<>(optionSet);
        Collections.shuffle(options, RANDOM);
        if (options.size() <= 4) return options;

        List<String> capped = new ArrayList<>();
        capped.add(correctSentence);
        for (String option : options) {
            if (capped.size() >= 4) break;
            if (!correctSentence.equals(option)) {
                capped.add(option);
            }
        }
        Collections.shuffle(capped, RANDOM);
        return capped;
    }

    private String joinWords(List<String> words) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w != null) sb.append(w);
        }
        return sb.toString();
    }

    private List<String> getRandomSentenceDistractors(String difficulty, int sentenceId, int limit) {
        List<String> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT sentence FROM sentences WHERE difficulty = ? AND id != ? ORDER BY RANDOM() LIMIT ?",
                    new String[]{difficulty != null ? difficulty : "EASY", String.valueOf(sentenceId), String.valueOf(limit)});
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        if (!result.isEmpty()) return result;

        try {
            cursor = db.rawQuery(
                    "SELECT sentence FROM sentences WHERE id != ? ORDER BY RANDOM() LIMIT ?",
                    new String[]{String.valueOf(sentenceId), String.valueOf(limit)});
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    private PuzzleQuestion buildLongSentenceBlankQuestion(int sentenceId, String sentenceText, String difficulty, List<SentenceToken> tokens) {
        SentenceToken blankToken = pickBlankToken(tokens);
        if (blankToken == null || blankToken.word == null || blankToken.word.trim().isEmpty()) {
            return null;
        }

        PuzzleQuestion q = new PuzzleQuestion();
        q.sentenceId = sentenceId;
        q.sentence = sentenceText;
        q.blankMode = true;
        q.correctAnswer = blankToken.word;
        q.displayPrompt = maskSentence(sentenceText, blankToken);
        q.hint = "鐠囪渹璐熼崣銉ョ摍缁岃櫣娅ф径鍕偓澶嬪濮濓絿鈥樼拠宥堫嚔";
        q.options = buildBlankOptions(tokens, difficulty, blankToken.word);
        if (q.options == null || q.options.isEmpty()) return null;
        return q;
    }

    private SentenceToken pickBlankToken(List<SentenceToken> tokens) {
        List<SentenceToken> candidates = new ArrayList<>();
        for (SentenceToken token : tokens) {
            if (isContentWord(token.word)) {
                candidates.add(token);
            }
        }
        if (candidates.isEmpty()) candidates = tokens;
        if (candidates.isEmpty()) return null;
        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    private boolean isContentWord(String word) {
        if (word == null) return false;
        String trimmed = word.trim();
        if (trimmed.isEmpty()) return false;
        return trimmed.matches(".*[\\p{L}\\p{N}\\u4e00-\\u9fa5].*");
    }

    private String maskSentence(String sentenceText, SentenceToken token) {
        if (sentenceText == null || token == null || token.word == null) return sentenceText;

        int start = token.wordPosition - 1;
        if (start >= 0 && start + token.word.length() <= sentenceText.length()) {
            return sentenceText.substring(0, start) + BLANK_PLACEHOLDER + sentenceText.substring(start + token.word.length());
        }

        int index = sentenceText.indexOf(token.word);
        if (index >= 0) {
            return sentenceText.substring(0, index) + BLANK_PLACEHOLDER + sentenceText.substring(index + token.word.length());
        }
        return sentenceText + " " + BLANK_PLACEHOLDER;
    }

    private List<String> buildBlankOptions(List<SentenceToken> tokens, String difficulty, String correctWord) {
        Set<String> optionSet = new HashSet<>();
        optionSet.add(correctWord);

        for (SentenceToken token : tokens) {
            if (optionSet.size() >= 4) break;
            if (token.word == null) continue;
            if (token.word.equals(correctWord)) continue;
            if (!isContentWord(token.word)) continue;
            optionSet.add(token.word);
        }

        if (optionSet.size() < 4) {
            List<String> wordDistractors = getRandomWordDistractors(difficulty, correctWord, 16);
            for (String distractor : wordDistractors) {
                if (optionSet.size() >= 4) break;
                if (distractor == null) continue;
                String candidate = distractor.trim();
                if (!candidate.isEmpty() && !candidate.equals(correctWord) && isContentWord(candidate)) {
                    optionSet.add(candidate);
                }
            }
        }

        List<String> options = new ArrayList<>(optionSet);
        Collections.shuffle(options, RANDOM);
        return options;
    }

    private List<String> getRandomWordDistractors(String difficulty, String correctWord, int limit) {
        List<String> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT sw.word FROM sentence_words sw " +
                            "JOIN sentences s ON sw.sentence_id = s.id " +
                            "WHERE s.difficulty = ? AND sw.word != ? " +
                            "ORDER BY RANDOM() LIMIT ?",
                    new String[]{difficulty != null ? difficulty : "EASY", correctWord, String.valueOf(limit)});
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        if (!result.isEmpty()) return result;

        try {
            cursor = db.rawQuery(
                    "SELECT word FROM sentence_words WHERE word != ? ORDER BY RANDOM() LIMIT ?",
                    new String[]{correctWord, String.valueOf(limit)});
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    private void showCurrentWordPuzzleQuestion() {
        if (puzzleQuestions == null || currentIndex >= puzzleQuestions.size()) {
            endWordPuzzleGame();
            return;
        }

        questionStartTime = System.currentTimeMillis();
        currentPuzzleQuestion = puzzleQuestions.get(currentIndex);
        correctAnswer = currentPuzzleQuestion.correctAnswer != null ? currentPuzzleQuestion.correctAnswer : "";
        currentOptions = new ArrayList<>(currentPuzzleQuestion.options);

        tvProgress.setVisibility(View.VISIBLE);
        tvPinyinQuestion.setVisibility(View.VISIBLE);
        tvQuestionHint.setVisibility(View.VISIBLE);
        tvResult.setVisibility(View.GONE);
        btnBackToMenu.setVisibility(View.VISIBLE);

        tvProgress.setText((currentIndex + 1) + " / " + puzzleQuestions.size());
        tvPinyinQuestion.setText(currentPuzzleQuestion.displayPrompt != null ? currentPuzzleQuestion.displayPrompt : "");
        tvQuestionHint.setText(currentPuzzleQuestion.hint != null ? currentPuzzleQuestion.hint : "");
        if (tvFeedbackBanner != null) tvFeedbackBanner.setVisibility(View.GONE);

        int n = currentOptions.size();
        if (n > 0) btnOpt1.setText(currentOptions.get(0));
        if (n > 1) btnOpt2.setText(currentOptions.get(1));
        if (n > 2) btnOpt3.setText(currentOptions.get(2));
        if (n > 3) btnOpt4.setText(currentOptions.get(3));
        setOptionsVisible(n);
        setOptionsClickable(true);
    }

    private void onWordPuzzleOptionClick(View view) {
        if (puzzleQuestions == null || currentIndex >= puzzleQuestions.size() || currentOptions == null) return;
        setOptionsClickable(false);

        long timeSpentMs = System.currentTimeMillis() - questionStartTime;
        int idx = view.getId() == R.id.btn_opt1 ? 0 : view.getId() == R.id.btn_opt2 ? 1 : view.getId() == R.id.btn_opt3 ? 2 : 3;
        String chosen = (idx >= 0 && idx < currentOptions.size()) ? currentOptions.get(idx) : "";
        boolean correct = correctAnswer.equals(chosen);
        if (correct) correctCount++;

        PuzzleQuestion q = puzzleQuestions.get(currentIndex);
        QuestionResult qr = new QuestionResult(q.sentenceId, chosen, correct, timeSpentMs);
        questionResults.add(qr);

        if (currentGameScoreId > 0) {
            boolean updated = gameQuestionDetailDao.updateByGameScoreAndOrder(
                    (int) currentGameScoreId,
                    currentIndex + 1,
                    qr.userAnswer,
                    qr.correct,
                    qr.timeSpentMs,
                    qr.correct ? MAX_SCORE_PER_QUESTION : 0
            );
            if (!updated) {
                GameQuestionDetail d = new GameQuestionDetail();
                d.setGameScoreId((int) currentGameScoreId);
                d.setQuestionOrder(currentIndex + 1);
                d.setQuestionId(q.sentenceId);
                d.setUserAnswer(qr.userAnswer);
                d.setCorrect(qr.correct);
                d.setTimeSpent(qr.timeSpentMs);
                d.setQuestionScore(qr.correct ? MAX_SCORE_PER_QUESTION : 0);
                d.setMaxScore(MAX_SCORE_PER_QUESTION);
                gameQuestionDetailDao.insert(d);
            }
        }

        animateAnswerTarget(view, correct);
        showAnswerFeedback(correct, correct ? "Great! Correct answer." : "Oops! Correct: " + correctAnswer);

        view.postDelayed(() -> {
            currentIndex++;
            showCurrentWordPuzzleQuestion();
        }, 700);
    }

    private void saveIncompleteWordPuzzleGame() {
        if (currentGameScoreId <= 0 || puzzleQuestions == null) return;
        int answered = currentIndex;
        int scoreSoFar = answered > 0 ? (correctCount * MAX_SCORE_PER_QUESTION) : 0;
        double accuracySoFar = answered > 0 ? (double) correctCount / answered : 0.0;
        long timeSpentSoFar = System.currentTimeMillis() - gameStartTime;
        String difficultyStr = getIntent().getStringExtra("DIFFICULTY");
        if (difficultyStr == null) difficultyStr = "EASY";
        GameScore gs = createGameScoreForWordPuzzle(
                difficultyStr,
                scoreSoFar,
                answered * MAX_SCORE_PER_QUESTION,
                accuracySoFar,
                timeSpentSoFar,
                false
        );
        gs.setId((int) currentGameScoreId);
        gameScoreDao.update(gs);
    }

    private void endWordPuzzleGame() {
        gameCompleted = true;
        int total = puzzleQuestions != null ? puzzleQuestions.size() : 0;
        int maxScoreTotal = total * MAX_SCORE_PER_QUESTION;
        int score = correctCount * MAX_SCORE_PER_QUESTION;
        double accuracy = total > 0 ? (double) correctCount / total : 0.0;
        long timeSpent = System.currentTimeMillis() - gameStartTime;

        String difficultyStr = getIntent().getStringExtra("DIFFICULTY");
        if (difficultyStr == null) difficultyStr = "EASY";

        if (currentGameScoreId > 0) {
            GameScore gs = createGameScoreForWordPuzzle(difficultyStr, score, maxScoreTotal, accuracy, timeSpent, true);
            gs.setId((int) currentGameScoreId);
            gameScoreDao.update(gs);
        }

        tvPinyinQuestion.setVisibility(View.GONE);
        tvQuestionHint.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);
        setOptionsVisible(0);
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText("Done!\nCorrect " + correctCount + " / " + total + "\nScore " + score + " / " + maxScoreTotal);
        btnBackToMenu.setVisibility(View.VISIBLE);
    }

    private void initializeSpeechRecognition() {
        iseEvaluator = new IseEvaluator(
                IFlyTekConfig.APP_ID, IFlyTekConfig.API_KEY, IFlyTekConfig.API_SECRET);
        iseEvaluator.setRmsCallback(rmsDb -> {
            if (voiceWaveView != null) voiceWaveView.updateVolume(rmsDb);
        });

        speechHelper = new SpeechRecognitionHelper(this);
        SpeechRecognitionConfig config = SpeechRecognitionConfig.createChineseConfig();
        speechHelper.initialize(config);

        android.util.Log.d("SpeechRecognition", "Speech evaluation and recognition initialized");
    }
    
    private void startPronunciationQuizGame() {
        String difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "EASY";

        // 娑?character_matching 閻╃鎮撻敍灞肩矤 game_words 閼惧嘲褰囨０妯兼窗
        questions = gameWordDao.getRandomGameWordsForGame(QUESTIONS_PER_GAME, difficulty);
        if (questions == null) questions = new ArrayList<>();

        if (questions.size() < QUESTIONS_PER_GAME) {
            if (tvPronunciationWord != null) tvPronunciationWord.setText("Not enough questions.");
            if (tvPronunciationResult != null) {
                tvPronunciationResult.setVisibility(View.VISIBLE);
                tvPronunciationResult.setText("Only " + questions.size() + " question(s) available.\nPlease tap Reload to import game_words data (need at least 10).");
            }
            return;
        }
        if (questions.size() > QUESTIONS_PER_GAME) {
            questions = new ArrayList<>(questions.subList(0, QUESTIONS_PER_GAME));
        }

        currentIndex = 0;
        correctCount = 0;
        pronScores = new int[QUESTIONS_PER_GAME];
        questionResults = new ArrayList<>();
        gameCompleted = false;
        gameStartTime = System.currentTimeMillis();

        if (btnBackToMenu != null) btnBackToMenu.setVisibility(View.GONE);

        GameScore gs = createGameScoreForPronunciationQuiz(difficulty, 0, 0, 0.0, 0L, false);
        currentGameScoreId = gameScoreDao.save(gs);
        if (currentGameScoreId <= 0) currentGameScoreId = 0;

        if (currentGameScoreId > 0) {
            List<GameQuestionDetail> placeholders = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                GameQuestionDetail d = new GameQuestionDetail();
                d.setGameScoreId((int) currentGameScoreId);
                d.setQuestionOrder(i + 1);
                d.setQuestionId(questions.get(i).getWordId());
                d.setUserAnswer("");
                d.setCorrect(false);
                d.setTimeSpent(0L);
                d.setQuestionScore(0);
                d.setMaxScore(MAX_SCORE_PER_QUESTION);
                placeholders.add(d);
            }
            gameQuestionDetailDao.insertBatch(placeholders);
        }

        showCurrentPronunciationQuestion();
    }
    
    private GameScore createGameScoreForPronunciationQuiz(String difficultyStr, int score, int maxPossibleScore, double accuracy, long timeSpent, boolean completed) {
        GameScore.Difficulty diff = GameScore.Difficulty.EASY;
        try { diff = GameScore.Difficulty.valueOf(difficultyStr); } catch (Exception ignored) { }
        GameScore gs = new GameScore();
        gs.setUserId(userId);
        gs.setGameType(GameScore.GameType.PRONUNCIATION_QUIZ);
        gs.setDifficulty(diff);
        gs.setScore(score);
        gs.setMaxPossibleScore(maxPossibleScore);
        gs.setAccuracy(accuracy);
        gs.setTimeSpent(timeSpent);
        gs.setPlayDate(new java.util.Date());
        gs.setCompleted(completed);
        return gs;
    }
    
    private void showCurrentPronunciationQuestion() {
        if (currentIndex >= questions.size()) {
            endPronunciationQuizGame();
            return;
        }

        questionStartTime = System.currentTimeMillis();
        currentQuestionScore = 0;

        CharacterMatching q = questions.get(currentIndex);
        String word = q.getWord();
        String pinyin = q.getPinyin();
        String hint = q.getHint();

        if (tvPronunciationWord != null) tvPronunciationWord.setText(word != null ? word : "");
        if (tvPronunciationPinyin != null) tvPronunciationPinyin.setText(pinyin != null ? pinyin : "");
        if (tvQuestionHint != null) tvQuestionHint.setText(hint != null ? hint : "");
        if (tvPronunciationProgress != null)
            tvPronunciationProgress.setText((currentIndex + 1) + " / " + questions.size());
        if (tvRecognitionStatus != null) tvRecognitionStatus.setText("Hold the mic to speak");
        if (tvCurrentScore != null) tvCurrentScore.setVisibility(View.GONE);
        if (btnNextQuestion != null) btnNextQuestion.setVisibility(View.GONE);
        if (tvFeedbackBanner != null) tvFeedbackBanner.setVisibility(View.GONE);

        setupRecordingButtons();
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private void setupRecordingButtons() {
        if (btnStartRecording != null) {
            btnStartRecording.setEnabled(true);
            btnStartRecording.setVisibility(View.VISIBLE);
            setMicRecordingVisual(false);
            btnStartRecording.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isHoldRecording) startRecording();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isHoldRecording) stopRecording();
                        return true;
                }
                return false;
            });
        }

        if (btnNextQuestion != null) {
            btnNextQuestion.setVisibility(View.GONE);
            btnNextQuestion.setOnClickListener(v -> {
                pronScores[currentIndex] = currentQuestionScore;
                currentIndex++;
                showCurrentPronunciationQuestion();
            });
        }
    }
    
        private void startRecording() {
        if (iseEvaluator == null && speechHelper == null) {
            Toast.makeText(this, "Speech engine is not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (speechHelper != null && !speechHelper.checkAndRequestPermission()) {
            Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
            return;
        }

        isHoldRecording = true;
        if (backgroundMusicManager != null) {
            backgroundMusicManager.setDucked(true);
        }
        if (tvRecognitionStatus != null) tvRecognitionStatus.setText("Listening...");
        setMicRecordingVisual(true);
        if (voiceWaveView != null) voiceWaveView.start();

        String targetWord = (questions != null && currentIndex < questions.size())
                ? questions.get(currentIndex).getWord() : "";

        try {
            iseEvaluator.evaluate(targetWord, new IseEvaluator.EvaluationCallback() {
    @Override
                public void onResult(IseEvaluator.EvalResult result) {
                    runOnUiThread(() -> {
                        isHoldRecording = false;
                        onRecordingStopped();
                        handleIseResult(result);
                    });
                }
    @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        isHoldRecording = false;
                        onRecordingStopped();
                        android.util.Log.w("ISE", "ISE evaluation failed: " + errorMessage);
                        if (tvRecognitionStatus != null) {
                            tvRecognitionStatus.setText("Evaluation failed: " + errorMessage);
                        }
                        Toast.makeText(GameActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        if (btnStartRecording != null) btnStartRecording.setEnabled(true);
                    });
                }
            });
        } catch (Throwable e) {
            android.util.Log.e("SpeechRecognition", "Failed to start speech evaluation", e);
            isHoldRecording = false;
            onRecordingStopped();
            Toast.makeText(this, "Failed to start speech evaluation: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void stopRecording() {
        isHoldRecording = false;
        if (iseEvaluator != null) iseEvaluator.stop();
        if (tvRecognitionStatus != null) tvRecognitionStatus.setText("Processing...");
        onRecordingStopped();
    }

    private void onRecordingStopped() {
        if (backgroundMusicManager != null) {
            backgroundMusicManager.setDucked(false);
        }
        setMicRecordingVisual(false);
        if (voiceWaveView != null) voiceWaveView.stop();
    }
    
    /**
     * 婢跺嫮鎮奍SE鐠囶參鐓剁拠鍕ゴ缂佹挻鐏夐妴?     * 娑撳秷鍤滈崝銊ㄧ儲妫版﹫绱濋崗浣筋啅闁插秴顦查崣鎴︾叾閿涘苯褰囬張鈧崥搴濈濞嗏剝鍨氱紒鈹库偓?     */
    private void handleIseResult(IseEvaluator.EvalResult evalResult) {
        long timeSpentMs = System.currentTimeMillis() - questionStartTime;
        float iseScore = evalResult.totalScore;
        currentQuestionScore = Math.round(iseScore / 100f * MAX_SCORE_PER_QUESTION);

        String userAnswer = evalResult.recognizedText != null ? evalResult.recognizedText : "";
        boolean correct = currentQuestionScore >= 6;
        showAnswerFeedback(correct, correct
                ? "Good pronunciation! Score " + currentQuestionScore + "/" + MAX_SCORE_PER_QUESTION
                : "Keep trying! Score " + currentQuestionScore + "/" + MAX_SCORE_PER_QUESTION);

        if (currentGameScoreId > 0) {
            gameQuestionDetailDao.updateByGameScoreAndOrder(
                    (int) currentGameScoreId, currentIndex + 1,
                    userAnswer, correct, timeSpentMs, currentQuestionScore);
        }

        if (tvCurrentScore != null) {
            tvCurrentScore.setVisibility(View.VISIBLE);
            tvCurrentScore.setText(currentQuestionScore + " / " + MAX_SCORE_PER_QUESTION);
            animateAnswerTarget(tvCurrentScore, correct);
        }

        if (tvRecognitionStatus != null) {
            StringBuilder fb = new StringBuilder();
            fb.append("Score: ").append(String.format("%.0f", iseScore)).append("/100");
            fb.append(" -> ").append(currentQuestionScore).append("/").append(MAX_SCORE_PER_QUESTION);
            if (evalResult.accuracyScore > 0) {
                fb.append("\nAccuracy: ").append(String.format("%.0f", evalResult.accuracyScore));
            }
            if (evalResult.fluencyScore > 0) {
                fb.append("  Fluency: ").append(String.format("%.0f", evalResult.fluencyScore));
            }
            if (evalResult.completenessScore > 0) {
                fb.append("  Completeness: ").append(String.format("%.0f", evalResult.completenessScore));
            }
            fb.append("\n\nHold mic to try again, or tap Next");
            tvRecognitionStatus.setText(fb.toString());
        }

        if (btnNextQuestion != null) btnNextQuestion.setVisibility(View.VISIBLE);
        if (btnStartRecording != null) btnStartRecording.setEnabled(true);
    }

    /**
     * 婢跺嫮鎮夾SR缁绢垵鐦戦崚顐ょ波閺嬫粣绱欐径鍥╂暏鐠侯垰绶為敍宀€鐣濋崡鏇炲爱闁板秵澧﹂崚鍡礆
     */
    private void handleRecognitionResult(SpeechRecognitionResult result) {
        String recognizedText = result.getRecognizedText();
        String targetWord = questions.get(currentIndex).getWord();
        boolean correct = recognizedText != null && recognizedText.contains(targetWord);
        currentQuestionScore = correct ? MAX_SCORE_PER_QUESTION : 0;
        showAnswerFeedback(correct, correct
                ? "Recognized correctly!"
                : "Try again. Target: " + targetWord);

        if (tvCurrentScore != null) {
            tvCurrentScore.setVisibility(View.VISIBLE);
            tvCurrentScore.setText(currentQuestionScore + " / " + MAX_SCORE_PER_QUESTION);
            animateAnswerTarget(tvCurrentScore, correct);
        }
        if (tvRecognitionStatus != null) {
            String fb = correct
                    ? "閴?Correct! You said: " + recognizedText
                    : "閴?You said: " + recognizedText + "\nExpected: " + targetWord;
            fb += "\n\nHold mic to try again, or tap Next";
            tvRecognitionStatus.setText(fb);
        }

        if (btnNextQuestion != null) btnNextQuestion.setVisibility(View.VISIBLE);
        if (btnStartRecording != null) btnStartRecording.setEnabled(true);
    }
    
        private void endPronunciationQuizGame() {
        gameCompleted = true;
        int total = questions.size();
        int maxScoreTotal = total * MAX_SCORE_PER_QUESTION;

        int totalScore = 0;
        for (int i = 0; i < total; i++) {
            totalScore += pronScores[i];
        }
        double accuracy = maxScoreTotal > 0 ? (double) totalScore / maxScoreTotal : 0;
        long timeSpent = System.currentTimeMillis() - gameStartTime;

        String difficultyStr = getIntent().getStringExtra("DIFFICULTY");
        if (difficultyStr == null && !questions.isEmpty()) {
            difficultyStr = questions.get(0).getDifficulty();
        }
        if (difficultyStr == null) difficultyStr = "EASY";

        if (currentGameScoreId > 0) {
            GameScore gs = createGameScoreForPronunciationQuiz(
                    difficultyStr, totalScore, maxScoreTotal, accuracy, timeSpent, true);
            gs.setId((int) currentGameScoreId);
            gameScoreDao.update(gs);
        }
        syncCompletedSessionStats(totalScore);

        if (tvPronunciationInstruction != null) tvPronunciationInstruction.setVisibility(View.GONE);
        if (tvPronunciationWord != null) tvPronunciationWord.setVisibility(View.GONE);
        if (tvPronunciationPinyin != null) tvPronunciationPinyin.setVisibility(View.GONE);
        if (tvQuestionHint != null) tvQuestionHint.setVisibility(View.GONE);
        if (tvPronunciationProgress != null) tvPronunciationProgress.setVisibility(View.GONE);
        if (btnStartRecording != null) btnStartRecording.setVisibility(View.GONE);
        if (btnNextQuestion != null) btnNextQuestion.setVisibility(View.GONE);
        if (tvCurrentScore != null) tvCurrentScore.setVisibility(View.GONE);
        if (voiceWaveView != null) { voiceWaveView.reset(); voiceWaveView.setVisibility(View.GONE); }
        if (tvRecognitionStatus != null) tvRecognitionStatus.setVisibility(View.GONE);

        if (tvPronunciationResult != null) {
            double averagePerWord = total > 0 ? (double) totalScore / total : 0.0;
            renderResultCard(
                    tvPronunciationResult,
                    "Pronunciation Quiz",
                    "Average",
                    String.format(Locale.getDefault(), "%.1f / %d", averagePerWord, MAX_SCORE_PER_QUESTION),
                    totalScore,
                    maxScoreTotal,
                    accuracy,
                    timeSpent
            );
        }
    }

    // ==================== Word Puzzle Drag Mode ====================
    private void startWordPuzzleDragGame() {
        String difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "EASY";

        puzzleQuestions = loadWordPuzzleDragQuestions(difficulty, QUESTIONS_PER_GAME);
        if (puzzleQuestions == null) puzzleQuestions = new ArrayList<>();
        if (puzzleQuestions.size() < QUESTIONS_PER_GAME) {
            if (tvWordPuzzleResult != null) {
                tvWordPuzzleResult.setVisibility(View.VISIBLE);
                tvWordPuzzleResult.setText("Only " + puzzleQuestions.size() + " question(s) available.");
            }
            if (btnWordPuzzleSubmit != null) btnWordPuzzleSubmit.setEnabled(false);
            if (btnWordPuzzleReset != null) btnWordPuzzleReset.setEnabled(false);
            if (btnWordPuzzleBack != null) btnWordPuzzleBack.setOnClickListener(v -> finish());
            return;
        }
        if (puzzleQuestions.size() > QUESTIONS_PER_GAME) {
            puzzleQuestions = new ArrayList<>(puzzleQuestions.subList(0, QUESTIONS_PER_GAME));
        }

        currentIndex = 0;
        correctCount = 0;
        questionResults = new ArrayList<>();
        gameCompleted = false;
        gameStartTime = System.currentTimeMillis();

        GameScore gs = createGameScoreForWordPuzzle(difficulty, 0, 0, 0.0, 0L, false);
        currentGameScoreId = gameScoreDao.save(gs);
        if (currentGameScoreId <= 0) currentGameScoreId = 0;

        if (currentGameScoreId > 0) {
            List<GameQuestionDetail> placeholders = new ArrayList<>();
            for (int i = 0; i < puzzleQuestions.size(); i++) {
                GameQuestionDetail d = new GameQuestionDetail();
                d.setGameScoreId((int) currentGameScoreId);
                d.setQuestionOrder(i + 1);
                d.setQuestionId(puzzleQuestions.get(i).sentenceId);
                d.setUserAnswer("");
                d.setCorrect(false);
                d.setTimeSpent(0L);
                d.setQuestionScore(0);
                d.setMaxScore(MAX_SCORE_PER_QUESTION);
                placeholders.add(d);
            }
            gameQuestionDetailDao.insertBatch(placeholders);
        }

        if (btnWordPuzzleBack != null) btnWordPuzzleBack.setOnClickListener(v -> onBackToMenuClicked());
        if (btnWordPuzzleReset != null) btnWordPuzzleReset.setOnClickListener(v -> resetWordPuzzleDragBoard());
        if (btnWordPuzzleSubmit != null) btnWordPuzzleSubmit.setOnClickListener(v -> submitWordPuzzleDragAnswer());
        if (layoutWordPuzzleSource != null) {
            layoutWordPuzzleSource.setOnDragListener((v, event) -> {
                if (!(event.getLocalState() instanceof View)) return false;
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    moveWordBlock((View) event.getLocalState(), layoutWordPuzzleSource);
                    return true;
                }
                return true;
            });
        }

        showWordPuzzleDragQuestion();
    }

    private List<PuzzleQuestion> loadWordPuzzleDragQuestions(String difficulty, int count) {
        List<PuzzleQuestion> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getPersistentDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id, sentence FROM sentences WHERE difficulty = ? ORDER BY RANDOM() LIMIT ?",
                new String[]{difficulty, String.valueOf(MAX_SENTENCE_POOL)});
        while (cursor.moveToNext() && result.size() < count) {
            PuzzleQuestion q = buildWordPuzzleDragQuestion(cursor.getInt(0), cursor.getString(1));
            if (q != null) result.add(q);
        }
        cursor.close();

        if (result.size() >= count) return result;

        cursor = db.rawQuery(
                "SELECT id, sentence FROM sentences ORDER BY RANDOM() LIMIT ?",
                new String[]{String.valueOf(MAX_SENTENCE_POOL)});
        while (cursor.moveToNext() && result.size() < count) {
            int sid = cursor.getInt(0);
            boolean exists = false;
            for (PuzzleQuestion q : result) {
                if (q.sentenceId == sid) {
                    exists = true;
                    break;
                }
            }
            if (exists) continue;
            PuzzleQuestion q = buildWordPuzzleDragQuestion(sid, cursor.getString(1));
            if (q != null) result.add(q);
        }
        cursor.close();
        return result;
    }

            private PuzzleQuestion buildWordPuzzleDragQuestion(int sentenceId, String sentence) {
        if (EXCLUDED_WORD_PUZZLE_SENTENCE_IDS.contains(sentenceId)) return null;
        if (sentence == null) return null;
        String sentenceText = sentence.trim();
        if (sentenceText.isEmpty()) return null;

        List<SentenceToken> tokens = getSentenceTokensForDrag(sentenceId, sentenceText);
        if (tokens.size() < PUZZLE_BLOCK_COUNT) return null;
        if (!isSuitableWordPuzzleSentence(sentenceText, tokens)) return null;

        int start = tokens.size() == PUZZLE_BLOCK_COUNT ? 0 : (tokens.size() - PUZZLE_BLOCK_COUNT + 1) / 2;
        List<String> target = new ArrayList<>();
        for (int i = start; i < start + PUZZLE_BLOCK_COUNT; i++) {
            target.add(tokens.get(i).word);
        }

        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        for (int i = 0; i < start; i++) left.append(tokens.get(i).word);
        for (int i = start + PUZZLE_BLOCK_COUNT; i < tokens.size(); i++) right.append(tokens.get(i).word);

        PuzzleQuestion q = new PuzzleQuestion();
        q.sentenceId = sentenceId;
        q.sentence = sentenceText;
        q.targetBlocks = target;

        String leftText = left.toString();
        String rightText = right.toString();
        if (leftText.length() > 0 || rightText.length() > 0) {
            q.contextPrompt = buildMaskedContextWithPunctuation(sentenceText, leftText, rightText);
            q.hint = "Drag the 5 blocks to restore the correct order.";
        } else {
            q.contextPrompt = "Restore the sentence order using 5 blocks.";
            q.hint = "Drag the 5 blocks to restore the correct order.";
        }
        return q;
    }

    private boolean isSuitableWordPuzzleSentence(String sentenceText, List<SentenceToken> tokens) {
        if (sentenceText == null || sentenceText.isEmpty()) return false;
        if (tokens == null || tokens.size() < PUZZLE_BLOCK_COUNT) return false;
        for (SentenceToken token : tokens) {
            if (token == null || token.word == null) return false;
            String word = token.word.trim();
            if (word.isEmpty()) return false;
            if (word.length() > MAX_WORD_PUZZLE_TOKEN_LENGTH) return false;
        }
        return true;
    }

        private String buildMaskedContextWithPunctuation(String sentenceText, String left, String right) {
        String blank = " ___ ";
        if (sentenceText == null) return blank;

        int start = 0;
        if (left != null && !left.isEmpty()) {
            int leftIdx = sentenceText.indexOf(left);
            if (leftIdx >= 0) {
                start = leftIdx + left.length();
            }
        }

        int end = sentenceText.length();
        if (right != null && !right.isEmpty()) {
            int rightIdx = sentenceText.lastIndexOf(right);
            if (rightIdx >= 0) {
                end = rightIdx;
            }
        }

        if (start >= 0 && end >= start && end <= sentenceText.length()) {
            String maskedText = sentenceText.substring(0, start) + blank + sentenceText.substring(end);
            return formatClauseBreaks(maskedText);
        }

        String fallback = (left == null ? "" : left) + blank + (right == null ? "" : right);
        return formatClauseBreaks(fallback);
    }

        private String formatClauseBreaks(String text) {
        if (text == null) return "";
        String formatted = text
                .replace("\uFF0C", "\uFF0C\n")
                .replace(",", ",\n")
                .replace("\uFF1B", "\uFF1B\n")
                .replace(";", ";\n");
        return formatted.trim();
    }

        private List<SentenceToken> getSentenceTokensForDrag(int sentenceId, String sentenceText) {
        List<SentenceToken> tokens = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getPersistentDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT word, word_order, word_position FROM sentence_words WHERE sentence_id = ? ORDER BY word_order ASC",
                new String[]{String.valueOf(sentenceId)});
        while (cursor.moveToNext()) {
            String word = cursor.getString(0);
            if (word == null) continue;
            String w = word.trim();
            if (w.isEmpty()) continue;
            if (w.matches("^[\\p{Punct}\\uFF0C\\u3002\\uFF01\\uFF1F\\uFF1B\\uFF1A\\u201C\\u201D\\u2018\\u2019\\u3001\\u2026\\u300A\\u300B\\u3010\\u3011\\uFF08\\uFF09\\u2014]+$")) continue;
            tokens.add(new SentenceToken(w, cursor.getInt(1), cursor.getInt(2)));
        }
        cursor.close();

        if (!tokens.isEmpty()) return tokens;

        int order = 1;
        for (int i = 0; i < sentenceText.length(); i++) {
            char ch = sentenceText.charAt(i);
            if (Character.isWhitespace(ch)) continue;
            String w = String.valueOf(ch);
            if (w.matches("^[\\p{Punct}\\uFF0C\\u3002\\uFF01\\uFF1F\\uFF1B\\uFF1A\\u201C\\u201D\\u2018\\u2019\\u3001\\u2026\\u300A\\u300B\\u3010\\u3011\\uFF08\\uFF09\\u2014]+$")) continue;
            tokens.add(new SentenceToken(w, order++, i + 1));
        }
        return tokens;
    }

    private void showWordPuzzleDragQuestion() {
        if (puzzleQuestions == null || currentIndex >= puzzleQuestions.size()) {
            endWordPuzzleDragGame();
            return;
        }
        questionStartTime = System.currentTimeMillis();
        currentPuzzleQuestion = puzzleQuestions.get(currentIndex);

        if (tvWordPuzzleProgress != null) tvWordPuzzleProgress.setText((currentIndex + 1) + " / " + puzzleQuestions.size());
        if (tvWordPuzzleContext != null) tvWordPuzzleContext.setText(currentPuzzleQuestion.contextPrompt);
        if (tvWordPuzzleHint != null) tvWordPuzzleHint.setText(currentPuzzleQuestion.hint);
        if (tvWordPuzzleResult != null) tvWordPuzzleResult.setVisibility(View.GONE);
        if (btnWordPuzzleSubmit != null) btnWordPuzzleSubmit.setEnabled(true);
        if (btnWordPuzzleReset != null) btnWordPuzzleReset.setEnabled(true);
        if (tvFeedbackBanner != null) tvFeedbackBanner.setVisibility(View.GONE);

        renderWordPuzzleDragBoard();
    }

    private void renderWordPuzzleDragBoard() {
        if (layoutWordPuzzleSlots == null || layoutWordPuzzleSource == null || currentPuzzleQuestion == null) return;
        layoutWordPuzzleSlots.removeAllViews();
        layoutWordPuzzleSource.removeAllViews();
        wordPuzzleSlots.clear();
        int slotWidth = getWordPuzzleSlotWidth(currentPuzzleQuestion.targetBlocks);

        for (int i = 0; i < PUZZLE_BLOCK_COUNT; i++) {
            FrameLayout slot = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(slotWidth, dpToPx(56));
            if (i > 0) lp.setMarginStart(dpToPx(6));
            if (i < PUZZLE_BLOCK_COUNT - 1) lp.setMarginEnd(dpToPx(6));
            slot.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#FFF3E0"));
            bg.setCornerRadius(dpToPx(8));
            bg.setStroke(dpToPx(1), Color.parseColor("#FFB74D"));
            slot.setBackground(bg);

            slot.setOnDragListener((v, event) -> {
                if (!(event.getLocalState() instanceof View)) return false;
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    FrameLayout target = (FrameLayout) v;
                    View dragged = (View) event.getLocalState();
                    if (target.getChildCount() > 0) {
                        View occupied = target.getChildAt(0);
                        if (occupied != dragged) moveWordBlock(occupied, layoutWordPuzzleSource);
                    }
                    moveWordBlock(dragged, target);
                    return true;
                }
                return true;
            });

            wordPuzzleSlots.add(slot);
            layoutWordPuzzleSlots.addView(slot);
        }

        List<String> shuffled = new ArrayList<>(currentPuzzleQuestion.targetBlocks);
        Collections.shuffle(shuffled, RANDOM);
        if (shuffled.equals(currentPuzzleQuestion.targetBlocks)) Collections.rotate(shuffled, 1);

        for (String block : shuffled) {
            TextView blockView = new TextView(this);
            blockView.setText(block);
            blockView.setTextSize(18);
            blockView.setTextColor(Color.BLACK);
            blockView.setGravity(android.view.Gravity.CENTER);
            blockView.setMinWidth(Math.max(dpToPx(72), slotWidth - dpToPx(12)));
            blockView.setMinHeight(dpToPx(52));
            blockView.setMaxLines(1);
            blockView.setIncludeFontPadding(false);
            blockView.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    blockView, 12, 18, 1, TypedValue.COMPLEX_UNIT_SP
            );

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E3F2FD"));
            bg.setCornerRadius(dpToPx(8));
            bg.setStroke(dpToPx(1), Color.parseColor("#64B5F6"));
            blockView.setBackground(bg);
            blockView.setOnLongClickListener(v -> {
                ClipData data = ClipData.newPlainText("word_block", ((TextView) v).getText());
                v.startDragAndDrop(data, new View.DragShadowBuilder(v), v, 0);
                return true;
            });

            moveWordBlock(blockView, layoutWordPuzzleSource);
        }
    }

    private void moveWordBlock(View block, ViewGroup target) {
        if (block == null || target == null) return;
        ViewGroup parent = (ViewGroup) block.getParent();
        if (parent == target) return;
        if (parent != null) parent.removeView(block);

        if (target instanceof FrameLayout) {
            block.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(52));
            lp.setMarginStart(dpToPx(6));
            lp.setMarginEnd(dpToPx(6));
            block.setLayoutParams(lp);
        }
        target.addView(block);
    }

    private int getWordPuzzleSlotWidth(List<String> blocks) {
        int maxLength = 1;
        if (blocks != null) {
            for (String block : blocks) {
                if (block != null) {
                    maxLength = Math.max(maxLength, block.trim().length());
                }
            }
        }
        if (maxLength >= 5) return dpToPx(124);
        if (maxLength == 4) return dpToPx(112);
        if (maxLength == 3) return dpToPx(96);
        return dpToPx(84);
    }

    private void resetWordPuzzleDragBoard() {
        if (layoutWordPuzzleSource == null) return;
        for (FrameLayout slot : wordPuzzleSlots) {
            if (slot.getChildCount() > 0) {
                moveWordBlock(slot.getChildAt(0), layoutWordPuzzleSource);
            }
        }
    }

    private void submitWordPuzzleDragAnswer() {
        if (currentPuzzleQuestion == null) return;
        List<String> userBlocks = new ArrayList<>();
        for (FrameLayout slot : wordPuzzleSlots) {
            if (slot.getChildCount() == 0 || !(slot.getChildAt(0) instanceof TextView)) {
                Toast.makeText(this, "Please fill all 5 slots first.", Toast.LENGTH_SHORT).show();
                return;
            }
            userBlocks.add(((TextView) slot.getChildAt(0)).getText().toString());
        }

        long timeSpentMs = System.currentTimeMillis() - questionStartTime;
        boolean correct = userBlocks.equals(currentPuzzleQuestion.targetBlocks);
        if (correct) correctCount++;

        String userAnswer = android.text.TextUtils.join("|", userBlocks);
        QuestionResult qr = new QuestionResult(currentPuzzleQuestion.sentenceId, userAnswer, correct, timeSpentMs);
        questionResults.add(qr);

        if (currentGameScoreId > 0) {
            boolean updated = gameQuestionDetailDao.updateByGameScoreAndOrder(
                    (int) currentGameScoreId, currentIndex + 1,
                    qr.userAnswer, qr.correct, qr.timeSpentMs,
                    qr.correct ? MAX_SCORE_PER_QUESTION : 0);
            if (!updated) {
                GameQuestionDetail d = new GameQuestionDetail();
                d.setGameScoreId((int) currentGameScoreId);
                d.setQuestionOrder(currentIndex + 1);
                d.setQuestionId(currentPuzzleQuestion.sentenceId);
                d.setUserAnswer(qr.userAnswer);
                d.setCorrect(qr.correct);
                d.setTimeSpent(qr.timeSpentMs);
                d.setQuestionScore(qr.correct ? MAX_SCORE_PER_QUESTION : 0);
                d.setMaxScore(MAX_SCORE_PER_QUESTION);
                gameQuestionDetailDao.insert(d);
            }
        }

        animateAnswerTarget(layoutWordPuzzleSlots, correct);
        showAnswerFeedback(correct, correct
                ? "Awesome! Sentence order is correct."
                : "Not quite. Correct: " + android.text.TextUtils.join("", currentPuzzleQuestion.targetBlocks));

        if (btnWordPuzzleSubmit != null) btnWordPuzzleSubmit.setEnabled(false);
        if (btnWordPuzzleReset != null) btnWordPuzzleReset.setEnabled(false);

        if (layoutWordPuzzleSource != null) {
            layoutWordPuzzleSource.postDelayed(() -> {
                currentIndex++;
                showWordPuzzleDragQuestion();
            }, 700);
        }
    }

    private void saveIncompleteWordPuzzleDragGame() {
        if (currentGameScoreId <= 0 || puzzleQuestions == null) return;
        int answered = currentIndex;
        int scoreSoFar = correctCount * MAX_SCORE_PER_QUESTION;
        double accuracySoFar = answered > 0 ? (double) correctCount / answered : 0.0;
        long timeSpentSoFar = System.currentTimeMillis() - gameStartTime;
        String difficultyStr = getIntent().getStringExtra("DIFFICULTY");
        if (difficultyStr == null) difficultyStr = "EASY";
        GameScore gs = createGameScoreForWordPuzzle(
                difficultyStr,
                scoreSoFar,
                answered * MAX_SCORE_PER_QUESTION,
                accuracySoFar,
                timeSpentSoFar,
                false);
        gs.setId((int) currentGameScoreId);
        gameScoreDao.update(gs);
    }

    private void endWordPuzzleDragGame() {
        gameCompleted = true;
        int total = puzzleQuestions != null ? puzzleQuestions.size() : 0;
        int maxScoreTotal = total * MAX_SCORE_PER_QUESTION;
        int score = correctCount * MAX_SCORE_PER_QUESTION;
        double accuracy = total > 0 ? (double) correctCount / total : 0.0;
        long timeSpent = System.currentTimeMillis() - gameStartTime;

        String difficultyStr = getIntent().getStringExtra("DIFFICULTY");
        if (difficultyStr == null) difficultyStr = "EASY";
        if (currentGameScoreId > 0) {
            GameScore gs = createGameScoreForWordPuzzle(difficultyStr, score, maxScoreTotal, accuracy, timeSpent, true);
            gs.setId((int) currentGameScoreId);
            gameScoreDao.update(gs);
        }
        syncCompletedSessionStats(score);

        if (layoutWordPuzzleSource != null) layoutWordPuzzleSource.removeAllViews();
        if (layoutWordPuzzleSlots != null) layoutWordPuzzleSlots.removeAllViews();
        wordPuzzleSlots.clear();

        if (tvWordPuzzleResult != null) {
            renderResultCard(
                    tvWordPuzzleResult,
                    "Word Puzzle",
                    "Correct",
                    correctCount + " / " + total,
                    score,
                    maxScoreTotal,
                    accuracy,
                    timeSpent
            );
        }
        if (btnWordPuzzleSubmit != null) btnWordPuzzleSubmit.setVisibility(View.GONE);
        if (btnWordPuzzleReset != null) btnWordPuzzleReset.setVisibility(View.GONE);
    }

    private void syncCompletedSessionStats(int score) {
        if (sessionStatsSynced || userDao == null || userId <= 0) {
            return;
        }
        userDao.updateGameStats(userId, score);
        sessionStatsSynced = true;
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
        if (btnMusicSettings == null || backgroundMusicManager == null) {
            return;
        }
        boolean enabled = backgroundMusicManager.isMusicEnabled();
        btnMusicSettings.setText(enabled ? "Music On" : "Music Off");
        btnMusicSettings.setAlpha(enabled ? 1f : 0.7f);
    }

    private void renderResultCard(
            TextView target,
            String gameLabel,
            String primaryLabel,
            String primaryValue,
            int score,
            int maxScore,
            double accuracy,
            long timeSpent
    ) {
        if (target == null) {
            return;
        }

        String headline = accuracy >= 0.9
                ? "Excellent finish"
                : accuracy >= 0.7
                ? "Nice work"
                : "Round complete";
        String footer = accuracy >= 0.9
                ? "Strong result. Ready for another round."
                : accuracy >= 0.6
                ? "Good momentum. A second try could push it higher."
                : "Progress saved in your history. Keep building accuracy.";

        SpannableStringBuilder builder = new SpannableStringBuilder();

        int headlineStart = builder.length();
        builder.append(headline);
        int headlineEnd = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), headlineStart, headlineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new RelativeSizeSpan(1.2f), headlineStart, headlineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(getColor(
                accuracy >= 0.9 ? R.color.duo_green_dark : R.color.duo_blue
        )), headlineStart, headlineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append("\n");
        int gameLabelStart = builder.length();
        builder.append(gameLabel);
        int gameLabelEnd = builder.length();
        builder.setSpan(new RelativeSizeSpan(0.92f), gameLabelStart, gameLabelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(getColor(R.color.duo_text_muted)), gameLabelStart, gameLabelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        appendMetricLine(builder, primaryLabel, primaryValue, true);
        appendMetricLine(builder, "Score", score + " / " + maxScore, false);
        appendMetricLine(builder, "Accuracy", String.format(Locale.getDefault(), "%.0f%%", accuracy * 100), false);
        appendMetricLine(builder, "Time", formatDuration(timeSpent), false);

        builder.append("\n\n");
        int footerStart = builder.length();
        builder.append(footer);
        int footerEnd = builder.length();
        builder.setSpan(new ForegroundColorSpan(getColor(R.color.duo_text_muted)), footerStart, footerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new RelativeSizeSpan(0.92f), footerStart, footerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        target.setVisibility(View.VISIBLE);
        target.setBackgroundResource(R.drawable.bg_result_summary_card);
        target.setText(builder);
        target.setTextColor(getColor(R.color.duo_text_dark));
        target.setGravity(android.view.Gravity.START);
        target.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        target.setLineSpacing(0f, 1.15f);
    }

    private void appendMetricLine(SpannableStringBuilder builder, String label, String value, boolean firstMetric) {
        builder.append(firstMetric ? "\n\n" : "\n");
        int labelStart = builder.length();
        builder.append(label).append("  ");
        int labelEnd = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), labelStart, labelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(getColor(R.color.duo_text_muted)), labelStart, labelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int valueStart = builder.length();
        builder.append(value);
        int valueEnd = builder.length();
        builder.setSpan(new RelativeSizeSpan(1.05f), valueStart, valueEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(getColor(R.color.duo_text_dark)), valueStart, valueEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String formatDuration(long timeSpentMs) {
        long totalSeconds = Math.max(0L, timeSpentMs / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SpeechRecognitionManager.REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone permission denied. Voice quiz may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        refreshMusicButtonState();
    }

    @Override
    protected void onDestroy() {
        if (backgroundMusicManager != null) {
            backgroundMusicManager.setDucked(false);
        }
        if (currentGameScoreId > 0 && !gameCompleted) {
            if ("WORD_PUZZLE".equals(gameType) && puzzleQuestions != null && currentIndex < puzzleQuestions.size()) {
                saveIncompleteWordPuzzleDragGame();
            } else if ("CHARACTER_MATCHING".equals(gameType) && questions != null && currentIndex < questions.size()) {
                saveIncompleteGame();
            }
        }
        if (iseEvaluator != null) {
            iseEvaluator.cancel();
        }
        if (speechHelper != null) {
            speechHelper.destroy();
        }
        super.onDestroy();
    }
}

