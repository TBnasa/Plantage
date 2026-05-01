package com.tbnasa.plantage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

/**
 * MoodChartView — Custom donut chart displaying leaf status distribution.
 * Uses the Zen sage-green palette.
 *
 * Performance: No allocations in onDraw; Paint objects and RectF are
 * pre-allocated and reused.
 */
public class MoodChartView extends View {

    // Zen palette colours
    private static final int COLOR_ACTIVE = Color.parseColor("#8DA399");
    private static final int COLOR_LOCKED = Color.parseColor("#7B9A8B");
    private static final int COLOR_WITHERED = Color.parseColor("#F0EFEA");
    private static final int COLOR_EMPTY = Color.parseColor("#E5E7EB");

    // Pre-allocated paints (no alloc in onDraw)
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lockedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint witheredPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF arcRect = new RectF();

    private int activeCount = 0;
    private int lockedCount = 0;
    private int witheredCount = 0;

    public MoodChartView(Context context) {
        super(context);
        init();
    }

    public MoodChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoodChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int colorSurface = ContextCompat.getColor(getContext(), R.color.colorSurface);
        int colorTextPrimary = ContextCompat.getColor(getContext(), R.color.colorTextPrimary);
        int colorEmpty = ContextCompat.getColor(getContext(), R.color.colorDivider);

        activePaint.setStyle(Paint.Style.FILL);
        activePaint.setColor(COLOR_ACTIVE);

        lockedPaint.setStyle(Paint.Style.FILL);
        lockedPaint.setColor(COLOR_LOCKED);

        witheredPaint.setStyle(Paint.Style.FILL);
        witheredPaint.setColor(COLOR_WITHERED);

        emptyPaint.setStyle(Paint.Style.FILL);
        emptyPaint.setColor(colorEmpty);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(colorSurface);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(colorTextPrimary);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int active, int locked, int withered) {
        this.activeCount = active;
        this.lockedCount = locked;
        this.witheredCount = withered;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float pad = size * 0.05f;

        arcRect.set(pad, pad, size - pad, size - pad);

        int total = activeCount + lockedCount + witheredCount;
        int capacity = Math.max(total, 30); // Show progress relative to at least 30 days

        // Draw background empty ring
        canvas.drawArc(arcRect, 0, 360, true, emptyPaint);

        if (total > 0) {
            float startAngle = -90f;

            // Active arc
            if (activeCount > 0) {
                float sweep = (360f * activeCount) / capacity;
                canvas.drawArc(arcRect, startAngle, sweep, true, activePaint);
                startAngle += sweep;
            }

            // Locked arc
            if (lockedCount > 0) {
                float sweep = (360f * lockedCount) / capacity;
                canvas.drawArc(arcRect, startAngle, sweep, true, lockedPaint);
                startAngle += sweep;
            }

            // Withered arc
            if (witheredCount > 0) {
                float sweep = (360f * witheredCount) / capacity;
                canvas.drawArc(arcRect, startAngle, sweep, true, witheredPaint);
            }
        }

        // Center hole (donut effect)
        float cx = size / 2f;
        float cy = size / 2f;
        float innerRadius = (size - 2 * pad) * 0.35f;
        canvas.drawCircle(cx, cy, innerRadius, centerPaint);

        // Center text — total count
        textPaint.setTextSize(size * 0.14f);
        canvas.drawText(String.valueOf(total), cx, cy + textPaint.getTextSize() * 0.35f, textPaint);
    }
}
