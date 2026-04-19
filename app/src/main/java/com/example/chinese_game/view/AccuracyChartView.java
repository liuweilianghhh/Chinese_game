package com.example.chinese_game.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.chinese_game.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class AccuracyChartView extends View {

    public static class ChartPoint {
        public final String label;
        public final float accuracyPercent;

        public ChartPoint(String label, float accuracyPercent) {
            this.label = label;
            this.accuracyPercent = accuracyPercent;
        }
    }

    private final List<ChartPoint> points = new ArrayList<>();
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AccuracyChartView(Context context) {
        super(context);
        init();
    }

    public AccuracyChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccuracyChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(Color.parseColor("#1A213A12"));
        gridPaint.setStrokeWidth(dpToPx(1));

        axisLabelPaint.setColor(ContextCompat.getColor(getContext(), R.color.duo_text_muted));
        axisLabelPaint.setTextSize(spToPx(11));

        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.duo_blue));
        linePaint.setStrokeWidth(dpToPx(3));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setColor(Color.parseColor("#331CB0F6"));
        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint.setColor(ContextCompat.getColor(getContext(), R.color.duo_blue));
        pointPaint.setStyle(Paint.Style.FILL);

        pointStrokePaint.setColor(Color.WHITE);
        pointStrokePaint.setStyle(Paint.Style.STROKE);
        pointStrokePaint.setStrokeWidth(dpToPx(2));

        emptyPaint.setColor(ContextCompat.getColor(getContext(), R.color.duo_text_muted));
        emptyPaint.setTextSize(spToPx(14));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setPoints(List<ChartPoint> chartPoints) {
        points.clear();
        if (chartPoints != null) {
            points.addAll(chartPoints);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = dpToPx(42);
        float top = dpToPx(18);
        float right = getWidth() - dpToPx(18);
        float bottom = getHeight() - dpToPx(34);
        if (right <= left || bottom <= top) {
            return;
        }

        if (points.isEmpty()) {
            canvas.drawText("No completed records in this range", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        float chartHeight = bottom - top;
        float chartWidth = right - left;

        for (int i = 0; i <= 4; i++) {
            float ratio = i / 4f;
            float y = bottom - (ratio * chartHeight);
            canvas.drawLine(left, y, right, y, gridPaint);

            String label = String.format(Locale.getDefault(), "%d%%", Math.round(ratio * 100));
            canvas.drawText(label, dpToPx(4), y + dpToPx(4), axisLabelPaint);
        }

        Path linePath = new Path();
        Path fillPath = new Path();
        float[] xs = new float[points.size()];
        float[] ys = new float[points.size()];

        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1
                    ? left + chartWidth / 2f
                    : left + (chartWidth * i / (points.size() - 1f));
            float percent = Math.max(0f, Math.min(100f, points.get(i).accuracyPercent));
            float y = bottom - (percent / 100f) * chartHeight;
            xs[i] = x;
            ys[i] = y;

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        fillPath.lineTo(xs[xs.length - 1], bottom);
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < xs.length; i++) {
            canvas.drawCircle(xs[i], ys[i], dpToPx(5), pointPaint);
            canvas.drawCircle(xs[i], ys[i], dpToPx(5), pointStrokePaint);
        }

        LinkedHashSet<Integer> labelIndexes = new LinkedHashSet<>();
        labelIndexes.add(0);
        labelIndexes.add(points.size() / 2);
        labelIndexes.add(points.size() - 1);

        Paint.Align previousAlign = axisLabelPaint.getTextAlign();
        for (int index : labelIndexes) {
            float x = xs[index];
            if (index == 0) {
                axisLabelPaint.setTextAlign(Paint.Align.LEFT);
            } else if (index == points.size() - 1) {
                axisLabelPaint.setTextAlign(Paint.Align.RIGHT);
            } else {
                axisLabelPaint.setTextAlign(Paint.Align.CENTER);
            }
            canvas.drawText(points.get(index).label, x, getHeight() - dpToPx(10), axisLabelPaint);
        }
        axisLabelPaint.setTextAlign(previousAlign);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
