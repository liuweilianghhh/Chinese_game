package com.example.chinese_game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

public final class BackgroundMusicManager {
    private static final String PREFS_NAME = "background_music_settings";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";

    private static final float BASE_VOLUME = 0.24f;
    private static final float DUCKED_VOLUME_MULTIPLIER = 0.06f;
    private static final long CROSSFADE_DURATION_MS = 2800L;
    private static final long FADE_STEP_MS = 80L;
    private static final long BACKGROUND_PAUSE_DELAY_MS = 900L;

    private static BackgroundMusicManager instance;

    private final Context appContext;
    private final SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] playlist = new int[]{
            R.raw.dream_build_repeat,
            R.raw.breakfast_of_champignons,
            R.raw.the_light_in_us_all
    };

    private MediaPlayer currentPlayer;
    private MediaPlayer nextPlayer;
    private int currentTrackIndex = -1;
    private int nextTrackIndex = -1;
    private float currentGain = 1f;
    private float nextGain = 0f;
    private float masterVolumeMultiplier = 1f;
    private boolean appInForeground;
    private boolean ducked;
    private boolean crossfading;
    private boolean musicEnabled;

    private Runnable scheduledTransitionRunnable;
    private Runnable crossfadeRunnable;
    private Runnable masterVolumeRunnable;
    private final Runnable backgroundPauseRunnable = this::pauseForBackground;

    private BackgroundMusicManager(Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        musicEnabled = preferences.getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public static synchronized BackgroundMusicManager getInstance(Context context) {
        if (instance == null) {
            instance = new BackgroundMusicManager(context);
        }
        return instance;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void onAppForeground() {
        handler.post(() -> {
            appInForeground = true;
            handler.removeCallbacks(backgroundPauseRunnable);

            if (!musicEnabled) {
                return;
            }

            ensurePlaybackReady();
            restartIfPaused(currentPlayer);
            restartIfPaused(nextPlayer);
            scheduleTrackTransition();
            animateMasterVolumeTo(getTargetMasterVolume(), 520L);
        });
    }

    public void onAppBackground() {
        handler.post(() -> {
            appInForeground = false;
            handler.removeCallbacks(backgroundPauseRunnable);
            handler.postDelayed(backgroundPauseRunnable, BACKGROUND_PAUSE_DELAY_MS);
        });
    }

    public void setDucked(boolean enabled) {
        handler.post(() -> {
            ducked = enabled;
            if (currentPlayer == null && nextPlayer == null) {
                return;
            }
            animateMasterVolumeTo(getTargetMasterVolume(), enabled ? 180L : 320L);
        });
    }

    public void setMusicEnabled(boolean enabled) {
        handler.post(() -> {
            musicEnabled = enabled;
            preferences.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply();

            if (!enabled) {
                collapseToSinglePlayer();
                clearTransitionCallbacks();
                animateMasterVolumeTo(0f, 220L, () -> {
                    pauseIfPlaying(currentPlayer);
                    pauseIfPlaying(nextPlayer);
                });
                return;
            }

            if (!appInForeground) {
                return;
            }

            ensurePlaybackReady();
            restartIfPaused(currentPlayer);
            restartIfPaused(nextPlayer);
            scheduleTrackTransition();
            animateMasterVolumeTo(getTargetMasterVolume(), 260L);
        });
    }

    public void release() {
        handler.post(() -> {
            handler.removeCallbacksAndMessages(null);
            releasePlayer(currentPlayer);
            releasePlayer(nextPlayer);
            currentPlayer = null;
            nextPlayer = null;
            currentTrackIndex = -1;
            nextTrackIndex = -1;
            crossfading = false;
        });
    }

    private void ensurePlaybackReady() {
        if (currentPlayer != null) {
            return;
        }

        currentTrackIndex = currentTrackIndex >= 0 ? currentTrackIndex : 0;
        currentPlayer = createPlayerForIndex(currentTrackIndex);
        if (currentPlayer == null) {
            return;
        }

        currentGain = 1f;
        nextGain = 0f;
        masterVolumeMultiplier = 0f;
        applyVolumes();
        currentPlayer.start();
    }

    private void pauseForBackground() {
        if (appInForeground) {
            return;
        }
        collapseToSinglePlayer();
        clearTransitionCallbacks();
        animateMasterVolumeTo(0f, 260L, () -> {
            pauseIfPlaying(currentPlayer);
            pauseIfPlaying(nextPlayer);
        });
    }

    private void collapseToSinglePlayer() {
        if (currentPlayer == null || nextPlayer == null) {
            return;
        }

        MediaPlayer keepPlayer = currentGain >= nextGain ? currentPlayer : nextPlayer;
        MediaPlayer discardPlayer = keepPlayer == currentPlayer ? nextPlayer : currentPlayer;

        if (keepPlayer == nextPlayer) {
            currentPlayer = nextPlayer;
            currentTrackIndex = nextTrackIndex;
        }

        releasePlayer(discardPlayer);
        nextPlayer = null;
        nextTrackIndex = -1;
        currentGain = 1f;
        nextGain = 0f;
        crossfading = false;
        applyVolumes();
    }

    private void scheduleTrackTransition() {
        if (!appInForeground || !musicEnabled || currentPlayer == null || crossfading) {
            return;
        }

        if (scheduledTransitionRunnable != null) {
            handler.removeCallbacks(scheduledTransitionRunnable);
        }

        int duration = currentPlayer.getDuration();
        int position = currentPlayer.getCurrentPosition();
        long delayMs = Math.max(600L, duration - position - CROSSFADE_DURATION_MS);

        scheduledTransitionRunnable = this::startCrossfadeToNextTrack;
        handler.postDelayed(scheduledTransitionRunnable, delayMs);
    }

    private void startCrossfadeToNextTrack() {
        if (!appInForeground || !musicEnabled || currentPlayer == null || crossfading) {
            return;
        }

        nextTrackIndex = getNextTrackIndex();
        nextPlayer = createPlayerForIndex(nextTrackIndex);
        if (nextPlayer == null) {
            return;
        }

        clearTransitionCallbacks();
        crossfading = true;
        nextGain = 0f;
        applyVolumes();
        nextPlayer.start();

        final long startTime = System.currentTimeMillis();
        crossfadeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!crossfading) {
                    return;
                }

                float progress = Math.min(1f,
                        (System.currentTimeMillis() - startTime) / (float) CROSSFADE_DURATION_MS);
                currentGain = 1f - progress;
                nextGain = progress;
                applyVolumes();

                if (progress >= 1f) {
                    completeCrossfade();
                } else {
                    handler.postDelayed(this, FADE_STEP_MS);
                }
            }
        };
        handler.post(crossfadeRunnable);
    }

    private void completeCrossfade() {
        MediaPlayer oldPlayer = currentPlayer;
        currentPlayer = nextPlayer;
        currentTrackIndex = nextTrackIndex;
        nextPlayer = null;
        nextTrackIndex = -1;
        releasePlayer(oldPlayer);
        currentGain = 1f;
        nextGain = 0f;
        crossfading = false;
        applyVolumes();
        scheduleTrackTransition();
    }

    private MediaPlayer createPlayerForIndex(int trackIndex) {
        if (trackIndex < 0 || trackIndex >= playlist.length) {
            return null;
        }

        MediaPlayer player = MediaPlayer.create(appContext, playlist[trackIndex]);
        if (player == null) {
            return null;
        }

        player.setLooping(false);
        player.setOnCompletionListener(mp -> handler.post(() -> handleTrackCompletion(mp)));
        return player;
    }

    private void handleTrackCompletion(MediaPlayer completedPlayer) {
        if (completedPlayer != currentPlayer) {
            return;
        }

        if (crossfading && nextPlayer != null) {
            completeCrossfade();
            return;
        }

        releasePlayer(currentPlayer);
        currentTrackIndex = getNextTrackIndex();
        currentPlayer = createPlayerForIndex(currentTrackIndex);
        if (currentPlayer == null) {
            return;
        }

        currentGain = 1f;
        nextGain = 0f;
        applyVolumes();

        if (appInForeground && musicEnabled) {
            currentPlayer.start();
            scheduleTrackTransition();
        }
    }

    private int getNextTrackIndex() {
        if (playlist.length == 0) {
            return -1;
        }
        if (currentTrackIndex < 0) {
            return 0;
        }
        return (currentTrackIndex + 1) % playlist.length;
    }

    private void animateMasterVolumeTo(float targetMultiplier, long durationMs) {
        animateMasterVolumeTo(targetMultiplier, durationMs, null);
    }

    private void animateMasterVolumeTo(float targetMultiplier, long durationMs, Runnable endAction) {
        if (masterVolumeRunnable != null) {
            handler.removeCallbacks(masterVolumeRunnable);
        }

        final float startMultiplier = masterVolumeMultiplier;
        final long startTime = System.currentTimeMillis();

        masterVolumeRunnable = new Runnable() {
            @Override
            public void run() {
                float progress = durationMs <= 0
                        ? 1f
                        : Math.min(1f, (System.currentTimeMillis() - startTime) / (float) durationMs);
                masterVolumeMultiplier = startMultiplier + ((targetMultiplier - startMultiplier) * progress);
                applyVolumes();

                if (progress >= 1f) {
                    masterVolumeMultiplier = targetMultiplier;
                    applyVolumes();
                    if (endAction != null) {
                        endAction.run();
                    }
                } else {
                    handler.postDelayed(this, FADE_STEP_MS);
                }
            }
        };
        handler.post(masterVolumeRunnable);
    }

    private float getTargetMasterVolume() {
        if (!musicEnabled) {
            return 0f;
        }
        return ducked ? DUCKED_VOLUME_MULTIPLIER : 1f;
    }

    private void applyVolumes() {
        applyVolume(currentPlayer, currentGain);
        applyVolume(nextPlayer, nextGain);
    }

    private void applyVolume(MediaPlayer player, float gain) {
        if (player == null) {
            return;
        }
        float volume = BASE_VOLUME * masterVolumeMultiplier * Math.max(0f, Math.min(1f, gain));
        player.setVolume(volume, volume);
    }

    private void clearTransitionCallbacks() {
        if (scheduledTransitionRunnable != null) {
            handler.removeCallbacks(scheduledTransitionRunnable);
            scheduledTransitionRunnable = null;
        }
        if (crossfadeRunnable != null) {
            handler.removeCallbacks(crossfadeRunnable);
            crossfadeRunnable = null;
        }
        crossfading = false;
    }

    private void restartIfPaused(MediaPlayer player) {
        if (player == null) {
            return;
        }
        try {
            if (!player.isPlaying()) {
                player.start();
            }
        } catch (IllegalStateException ignored) {
        }
    }

    private void pauseIfPlaying(MediaPlayer player) {
        if (player == null) {
            return;
        }
        try {
            if (player.isPlaying()) {
                player.pause();
            }
        } catch (IllegalStateException ignored) {
        }
    }

    private void releasePlayer(MediaPlayer player) {
        if (player == null) {
            return;
        }
        try {
            player.stop();
        } catch (IllegalStateException ignored) {
        }
        player.release();
    }
}
