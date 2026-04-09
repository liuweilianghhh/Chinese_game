package com.example.chinese_game.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * 语音录制时的声波动态可视化控件。
 * 接收 RMS 音量值后实时绘制跳动的竖条。
 */
public class VoiceWaveView extends View {

    private static final int BAR_COUNT = 7;
    private static final float BAR_MIN_FRACTION = 0.08f;
    private static final float BAR_CORNER_RADIUS = 6f;
    private static final long IDLE_ANIM_DURATION = 600;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final float[] barHeights = new float[BAR_COUNT];  // 0‥1
    private final float[] targetHeights = new float[BAR_COUNT];

    private boolean isActive = false;
    private ValueAnimator idleAnimator;

    private int colorStart = 0xFF4CAF50;
    private int colorEnd   = 0xFF81C784;
    private int colorIdle  = 0xFFBDBDBD;

    public VoiceWaveView(Context context) { this(context, null); }
    public VoiceWaveView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public VoiceWaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        barPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < BAR_COUNT; i++) barHeights[i] = BAR_MIN_FRACTION;
    }

    /** 开始录音时调用 */
    public void start() {
        isActive = true;
        stopIdleAnimation();
        invalidate();
    }

    /** 停止录音时调用 */
    public void stop() {
        isActive = false;
        for (int i = 0; i < BAR_COUNT; i++) {
            targetHeights[i] = BAR_MIN_FRACTION;
        }
        startIdleAnimation();
    }

    /** 重置到默认状态 */
    public void reset() {
        isActive = false;
        stopIdleAnimation();
        for (int i = 0; i < BAR_COUNT; i++) {
            barHeights[i] = BAR_MIN_FRACTION;
            targetHeights[i] = BAR_MIN_FRACTION;
        }
        invalidate();
    }

    /**
     * 传入 RMS 音量 (大约 0‥80 范围, 由 20*log10 计算得出)
     */
    public void updateVolume(float rmsDb) {
        if (!isActive) return;

        float normalized = Math.max(0f, Math.min(rmsDb / 70f, 1f));

        int mid = BAR_COUNT / 2;
        for (int i = 0; i < BAR_COUNT; i++) {
            float dist = Math.abs(i - mid) / (float) mid;
            float variation = (float) (Math.random() * 0.25);
            float h = normalized * (1f - dist * 0.5f) + variation * normalized;
            targetHeights[i] = Math.max(BAR_MIN_FRACTION, Math.min(h, 1f));
        }

        // 平滑过渡
        for (int i = 0; i < BAR_COUNT; i++) {
            barHeights[i] += (targetHeights[i] - barHeights[i]) * 0.45f;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float totalGap = w * 0.25f;
        float gap = totalGap / (BAR_COUNT + 1);
        float barWidth = (w - totalGap) / BAR_COUNT;

        for (int i = 0; i < BAR_COUNT; i++) {
            float fraction = barHeights[i];
            float barH = h * fraction;
            float left = gap + i * (barWidth + gap);
            float top = (h - barH) / 2f;
            barRect.set(left, top, left + barWidth, top + barH);

            if (isActive) {
                barPaint.setShader(new LinearGradient(
                        left, top, left, top + barH,
                        colorStart, colorEnd, Shader.TileMode.CLAMP));
            } else {
                barPaint.setShader(null);
                barPaint.setColor(colorIdle);
            }

            canvas.drawRoundRect(barRect, BAR_CORNER_RADIUS, BAR_CORNER_RADIUS, barPaint);
        }
        barPaint.setShader(null);
    }

    private void startIdleAnimation() {
        stopIdleAnimation();
        idleAnimator = ValueAnimator.ofFloat(0f, 1f);
        idleAnimator.setDuration(IDLE_ANIM_DURATION);
        idleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        idleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        idleAnimator.setInterpolator(new DecelerateInterpolator());
        idleAnimator.addUpdateListener(anim -> {
            float v = (float) anim.getAnimatedValue();
            for (int i = 0; i < BAR_COUNT; i++) {
                barHeights[i] += (BAR_MIN_FRACTION + v * 0.06f - barHeights[i]) * 0.3f;
            }
            invalidate();
        });
        idleAnimator.start();
    }

    private void stopIdleAnimation() {
        if (idleAnimator != null) {
            idleAnimator.cancel();
            idleAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopIdleAnimation();
    }
}
