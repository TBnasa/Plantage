package com.example.orbitfocus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.example.orbitfocus.model.Leaf;
import com.example.orbitfocus.model.LeafStatus;

/**
 * PlantageTreeView - Matematiksel Fraktal Ağaç Custom View.
 * Canvas kullanarak dinamik olarak dallanıp budaklanan bir ağaç çizer.
 * Her yaprak, Leaf durumuna göre farklı renkte görünür.
 */
public class PlantageTreeView extends View {

    private List<Leaf> leaves = new ArrayList<>();
    private Paint stemPaint;
    private Paint leafActivePaint;
    private Paint leafLockedPaint;
    private Paint leafWitheredPaint;
    private Paint veinPaint;
    private Paint veinWitheredPaint;
    private Paint textPaint;
    private Paint potPaint;
    private Paint potRimPaint;
    private Paint shadowPaint;

    private OnLeafClickListener leafClickListener;
    private List<LeafHitBox> clickZones = new ArrayList<>();
    private Random fractalRandom;

    // Colors
    private static final int COLOR_STEM = Color.rgb(46, 184, 96);
    private static final int COLOR_LEAF_ACTIVE = Color.rgb(76, 175, 80);
    private static final int COLOR_LEAF_LOCKED = Color.rgb(102, 187, 106);
    private static final int COLOR_LEAF_WITHERED = Color.rgb(139, 105, 20);
    private static final int COLOR_VEIN = Color.rgb(50, 140, 70);
    private static final int COLOR_VEIN_WITHERED = Color.rgb(107, 80, 16);
    private static final int COLOR_TERRACOTTA = Color.rgb(180, 101, 76);
    private static final int COLOR_TERRACOTTA_LIGHT = Color.rgb(200, 128, 106);
    private static final int COLOR_BACKGROUND = Color.rgb(236, 238, 232);

    private class LeafHitBox {
        float x, y, radius;
        int index;

        LeafHitBox(float x, float y, float radius, int index) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.index = index;
        }
    }

    public interface OnLeafClickListener {
        void onLeafClick(int index, Leaf leaf);
    }

    public PlantageTreeView(Context context) {
        super(context);
        init();
    }

    public PlantageTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Stem Paint
        stemPaint = new Paint();
        stemPaint.setColor(COLOR_STEM);
        stemPaint.setStyle(Paint.Style.STROKE);
        stemPaint.setStrokeCap(Paint.Cap.ROUND);
        stemPaint.setAntiAlias(true);

        // Active Leaf Paint
        leafActivePaint = new Paint();
        leafActivePaint.setColor(COLOR_LEAF_ACTIVE);
        leafActivePaint.setStyle(Paint.Style.FILL);
        leafActivePaint.setAntiAlias(true);

        // Locked Leaf Paint
        leafLockedPaint = new Paint();
        leafLockedPaint.setColor(COLOR_LEAF_LOCKED);
        leafLockedPaint.setStyle(Paint.Style.FILL);
        leafLockedPaint.setAntiAlias(true);

        // Withered Leaf Paint
        leafWitheredPaint = new Paint();
        leafWitheredPaint.setColor(COLOR_LEAF_WITHERED);
        leafWitheredPaint.setStyle(Paint.Style.FILL);
        leafWitheredPaint.setAntiAlias(true);

        // Vein Paints
        veinPaint = new Paint();
        veinPaint.setColor(COLOR_VEIN);
        veinPaint.setStyle(Paint.Style.STROKE);
        veinPaint.setStrokeWidth(4f);
        veinPaint.setStrokeCap(Paint.Cap.ROUND);
        veinPaint.setAntiAlias(true);

        veinWitheredPaint = new Paint();
        veinWitheredPaint.setColor(COLOR_VEIN_WITHERED);
        veinWitheredPaint.setStyle(Paint.Style.STROKE);
        veinWitheredPaint.setStrokeWidth(4f);
        veinWitheredPaint.setAntiAlias(true);

        // Text Paint
        textPaint = new Paint();
        textPaint.setColor(Color.rgb(100, 100, 100));
        textPaint.setTextSize(18f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Pot Paints
        potPaint = new Paint();
        potPaint.setColor(COLOR_TERRACOTTA);
        potPaint.setStyle(Paint.Style.FILL);
        potPaint.setAntiAlias(true);

        potRimPaint = new Paint();
        potRimPaint.setColor(COLOR_TERRACOTTA_LIGHT);
        potRimPaint.setStyle(Paint.Style.FILL);
        potRimPaint.setAntiAlias(true);

        // Shadow Paint
        shadowPaint = new Paint();
        shadowPaint.setColor(Color.rgb(200, 200, 195));
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setAntiAlias(true);
    }

    public void setLeaves(List<Leaf> leaves) {
        this.leaves = leaves;
        invalidate();
    }

    public void setOnLeafClickListener(OnLeafClickListener listener) {
        this.leafClickListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Background
        canvas.drawColor(COLOR_BACKGROUND);

        int w = getWidth();
        int h = getHeight();
        float centerX = w / 2f;
        float potBottomY = h - 40f;

        // Reset click zones and random seed for consistent fractal
        clickZones.clear();
        fractalRandom = new Random(42); // Fixed seed for consistent tree shape

        // Draw shadow
        drawShadow(canvas, centerX, potBottomY);

        // Draw pot
        drawPot(canvas, centerX, potBottomY);

        // Calculate tree parameters based on leaf count
        int leafCount = Math.max(1, leaves.size());
        float stemStartY = potBottomY - 120f;

        // Draw fractal tree
        drawFractalTree(canvas, centerX, stemStartY, leafCount);
    }

    private void drawShadow(Canvas canvas, float cx, float bottomY) {
        RectF shadowRect = new RectF(cx - 90, bottomY - 12, cx + 90, bottomY + 12);
        canvas.drawOval(shadowRect, shadowPaint);
    }

    private void drawPot(Canvas canvas, float cx, float bottomY) {
        float potTopW = 160f;
        float potBottomW = 110f;
        float potH = 95f;

        Path potPath = new Path();
        potPath.moveTo(cx - potTopW / 2, bottomY - potH);
        potPath.lineTo(cx + potTopW / 2, bottomY - potH);
        potPath.lineTo(cx + potBottomW / 2, bottomY);
        potPath.lineTo(cx - potBottomW / 2, bottomY);
        potPath.close();

        canvas.drawPath(potPath, potPaint);

        RectF rim = new RectF(cx - potTopW / 2 - 8, bottomY - potH - 16,
                cx + potTopW / 2 + 8, bottomY - potH + 8);
        canvas.drawRoundRect(rim, 8, 8, potRimPaint);
    }

    /**
     * Fraktal ağaç çizimi - dallanıp budaklanan yapı.
     */
    private void drawFractalTree(Canvas canvas, float startX, float startY, int totalLeaves) {
        if (totalLeaves <= 0)
            return;

        // Single stem growing upward with branches
        float currentX = startX;
        float currentY = startY;

        // Calculate segment height based on leaf count
        float baseSegmentHeight = 180f;
        float segmentHeight = Math.max(120f, baseSegmentHeight - (totalLeaves * 5));

        Path mainStem = new Path();
        mainStem.moveTo(startX, startY);

        // Main trunk thickness decreases as we go up
        float baseThickness = 16f;

        for (int i = 0; i < totalLeaves; i++) {
            boolean isLeft = i % 2 == 0;

            // Calculate next point with slight curve
            float branchAngle = isLeft ? -25f : 25f;
            float nextY = currentY - segmentHeight;
            float curveOffset = branchAngle * 0.8f;
            float nextX = startX + (isLeft ? -15 : 15) + fractalRandom.nextFloat() * 10 - 5;

            // Stem thickness decreases
            float thickness = baseThickness - (i * 1.5f);
            thickness = Math.max(6f, thickness);
            stemPaint.setStrokeWidth(thickness);

            // Draw stem segment
            Path segment = new Path();
            segment.moveTo(currentX, currentY);
            segment.quadTo(currentX + curveOffset, currentY - segmentHeight / 2, nextX, nextY);
            canvas.drawPath(segment, stemPaint);

            // Draw leaf at this branch point
            if (i < leaves.size()) {
                drawLeafAtPosition(canvas, nextX, nextY, isLeft, i, leaves.get(i));
            }

            currentX = nextX;
            currentY = nextY;
        }
    }

    private void drawLeafAtPosition(Canvas canvas, float stemX, float stemY,
            boolean isLeft, int index, Leaf leaf) {
        // Determine paint based on leaf status
        Paint currentLeafPaint;
        Paint currentVeinPaint;

        switch (leaf.status) {
            case LOCKED:
                currentLeafPaint = leafLockedPaint;
                currentVeinPaint = veinPaint;
                break;
            case WITHERED:
                currentLeafPaint = leafWitheredPaint;
                currentVeinPaint = veinWitheredPaint;
                break;
            case ACTIVE:
            default:
                currentLeafPaint = leafActivePaint;
                currentVeinPaint = veinPaint;
                break;
        }

        float leafLen = 160f;
        float leafWid = 80f;
        float angle = isLeft ? -50 : 50;

        canvas.save();
        canvas.translate(stemX, stemY);
        canvas.rotate(angle);

        // Draw leaf shape
        Path leafPath = new Path();
        leafPath.moveTo(0, 0);
        leafPath.cubicTo(leafLen * 0.25f, -leafWid * 0.55f,
                leafLen * 0.6f, -leafWid * 0.45f,
                leafLen, 0);
        leafPath.cubicTo(leafLen * 0.6f, leafWid * 0.45f,
                leafLen * 0.25f, leafWid * 0.55f,
                0, 0);
        leafPath.close();

        canvas.drawPath(leafPath, currentLeafPaint);

        // Draw center vein
        canvas.drawLine(12, 0, leafLen - 25, 0, currentVeinPaint);

        // Draw thumbnail or status indicator
        if (leaf.status == LeafStatus.WITHERED) {
            // Draw X mark for withered
            drawWitheredMark(canvas, leafLen);
        } else {
            // Draw photo thumbnail or placeholder
            String firstImg = leaf.getFirstImage();
            if (firstImg != null && !firstImg.isEmpty()) {
                drawThumbnail(canvas, leafLen, firstImg);
            } else {
                drawPlaceholder(canvas, leafLen, leaf.status == LeafStatus.ACTIVE);
            }
        }

        canvas.restore();

        // Date label
        if (leaf.date != null) {
            textPaint.setTextSize(16f);
            float labelX = isLeft ? stemX - 100 : stemX + 100;
            canvas.drawText(leaf.date, labelX, stemY + 6, textPaint);
        }

        // Add to click zones
        clickZones.add(new LeafHitBox(stemX, stemY, 100f, index));
    }

    private void drawThumbnail(Canvas canvas, float leafLen, String imagePath) {
        try {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 8;
                Bitmap bmp = BitmapFactory.decodeFile(imagePath, opts);
                if (bmp != null) {
                    float imgSize = 50f;
                    float imgX = leafLen * 0.5f - imgSize / 2;
                    float imgY = -imgSize / 2;

                    Paint imgPaint = new Paint();
                    imgPaint.setAntiAlias(true);

                    RectF imgRect = new RectF(imgX, imgY, imgX + imgSize, imgY + imgSize);
                    canvas.drawBitmap(bmp, null, imgRect, imgPaint);
                    bmp.recycle();
                    return;
                }
            }
        } catch (Exception e) {
            // Fall through to placeholder
        }
        drawPlaceholder(canvas, leafLen, true);
    }

    private void drawPlaceholder(Canvas canvas, float leafLen, boolean isActive) {
        Paint placeholderPaint = new Paint();
        placeholderPaint.setColor(Color.WHITE);
        placeholderPaint.setAlpha(isActive ? 230 : 150);
        placeholderPaint.setStyle(Paint.Style.FILL);
        placeholderPaint.setAntiAlias(true);

        float size = 42f;
        float x = leafLen * 0.5f - size / 2;
        float y = -size / 2;

        RectF rect = new RectF(x, y, x + size, y + size);
        canvas.drawRoundRect(rect, 10, 10, placeholderPaint);

        if (isActive) {
            // Plus icon
            Paint plusPaint = new Paint();
            plusPaint.setColor(Color.rgb(150, 150, 150));
            plusPaint.setStrokeWidth(3);
            plusPaint.setStrokeCap(Paint.Cap.ROUND);
            plusPaint.setAntiAlias(true);
            canvas.drawLine(x + size / 2, y + 12, x + size / 2, y + size - 12, plusPaint);
            canvas.drawLine(x + 12, y + size / 2, x + size - 12, y + size / 2, plusPaint);
        }
    }

    private void drawWitheredMark(Canvas canvas, float leafLen) {
        Paint markPaint = new Paint();
        markPaint.setColor(Color.rgb(100, 70, 20));
        markPaint.setAlpha(180);
        markPaint.setStyle(Paint.Style.FILL);
        markPaint.setAntiAlias(true);

        float size = 40f;
        float x = leafLen * 0.5f - size / 2;
        float y = -size / 2;

        RectF rect = new RectF(x, y, x + size, y + size);
        canvas.drawRoundRect(rect, 10, 10, markPaint);

        // X mark
        Paint xPaint = new Paint();
        xPaint.setColor(Color.rgb(180, 140, 60));
        xPaint.setStrokeWidth(4);
        xPaint.setStrokeCap(Paint.Cap.ROUND);
        xPaint.setAntiAlias(true);
        canvas.drawLine(x + 12, y + 12, x + size - 12, y + size - 12, xPaint);
        canvas.drawLine(x + size - 12, y + 12, x + 12, y + size - 12, xPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();

            // Check from top to bottom (latest leaves first)
            for (int i = clickZones.size() - 1; i >= 0; i--) {
                LeafHitBox box = clickZones.get(i);
                float dist = (float) Math.hypot(tx - box.x, ty - box.y);
                if (dist < box.radius) {
                    if (leafClickListener != null && i < leaves.size()) {
                        leafClickListener.onLeafClick(i, leaves.get(i));
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
