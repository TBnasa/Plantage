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
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.example.orbitfocus.model.Memory;

public class TreeView extends View {

    private List<Memory> memories = new ArrayList<>();
    private Paint textPaint;
    private OnLeafClickListener leafClickListener;
    private List<LeafHitBox> clickZones = new ArrayList<>();

    private class LeafHitBox {
        float x, y, r;
        int index;

        LeafHitBox(float x, float y, float r, int index) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.index = index;
        }
    }

    public interface OnLeafClickListener {
        void onLeafClick(int index, String date);
    }

    public TreeView(Context context) {
        super(context);
        init();
    }

    public TreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint = new Paint();
        textPaint.setColor(Color.rgb(100, 100, 100));
        textPaint.setTextSize(22f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setMemories(List<Memory> memories) {
        this.memories = memories;
        invalidate();
    }

    public void setOnLeafClickListener(OnLeafClickListener listener) {
        this.leafClickListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Grayish background matching layout
        canvas.drawColor(Color.rgb(240, 240, 238));

        int w = getWidth();
        int h = getHeight();

        float centerX = w / 2f;
        float potBottomY = h - 40f;

        // Draw shadow under pot
        drawShadow(canvas, centerX, potBottomY);

        // Draw Pot
        drawPot(canvas, centerX, potBottomY);

        // Draw Plant - bigger and like the reference
        float stemStartY = potBottomY - 120f;
        drawPlant(canvas, centerX, stemStartY);
    }

    private void drawShadow(Canvas canvas, float cx, float bottomY) {
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.rgb(200, 200, 200));
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setAntiAlias(true);

        // Ellipse shadow
        RectF shadowRect = new RectF(cx - 100, bottomY - 15, cx + 100, bottomY + 15);
        canvas.drawOval(shadowRect, shadowPaint);
    }

    private void drawPot(Canvas canvas, float cx, float bottomY) {
        // Terracotta pot like reference
        Paint potPaint = new Paint();
        potPaint.setColor(Color.rgb(180, 100, 60)); // Terracotta brown
        potPaint.setStyle(Paint.Style.FILL);
        potPaint.setAntiAlias(true);

        float potTopW = 180f;
        float potBottomW = 120f;
        float potH = 100f;

        // Trapezoid pot shape
        Path potPath = new Path();
        potPath.moveTo(cx - potTopW / 2, bottomY - potH);
        potPath.lineTo(cx + potTopW / 2, bottomY - potH);
        potPath.lineTo(cx + potBottomW / 2, bottomY);
        potPath.lineTo(cx - potBottomW / 2, bottomY);
        potPath.close();

        canvas.drawPath(potPath, potPaint);

        // Pot rim - lighter terracotta
        Paint rimPaint = new Paint();
        rimPaint.setColor(Color.rgb(200, 120, 80));
        rimPaint.setStyle(Paint.Style.FILL);
        rimPaint.setAntiAlias(true);

        RectF rim = new RectF(cx - potTopW / 2 - 10, bottomY - potH - 20, cx + potTopW / 2 + 10, bottomY - potH + 5);
        canvas.drawRoundRect(rim, 8, 8, rimPaint);
    }

    private void drawPlant(Canvas canvas, float startX, float startY) {
        clickZones.clear();

        int count = memories.size();
        if (count == 0)
            count = 1;

        // Bright green stem like reference
        Paint stemPaint = new Paint();
        stemPaint.setColor(Color.rgb(46, 184, 96)); // Bright green
        stemPaint.setStyle(Paint.Style.STROKE);
        stemPaint.setStrokeWidth(18f);
        stemPaint.setStrokeCap(Paint.Cap.ROUND);
        stemPaint.setAntiAlias(true);

        float currentX = startX;
        float currentY = startY;
        float segmentHeight = 200f; // Taller segments

        Path stemPath = new Path();
        stemPath.moveTo(startX, startY);

        for (int i = 0; i < count; i++) {
            float nextY = currentY - segmentHeight;
            // Gentle S-curve like reference
            float curveOffset = (i % 2 == 0) ? 30 : -30;
            float nextX = startX + curveOffset * 0.3f;

            // Control points for smooth curve
            float ctrlX = currentX + curveOffset;
            float ctrlY = currentY - segmentHeight * 0.5f;

            stemPath.quadTo(ctrlX, ctrlY, nextX, nextY);

            // Draw leaf at each segment
            drawLeaf(canvas, nextX, nextY, i % 2 == 0, i);

            currentX = nextX;
            currentY = nextY;
        }
        canvas.drawPath(stemPath, stemPaint);
    }

    private void drawLeaf(Canvas canvas, float stemX, float stemY, boolean isLeft, int index) {
        // Bright green like reference
        Paint leafPaint = new Paint();
        leafPaint.setColor(Color.rgb(76, 187, 86)); // Bright leaf green
        leafPaint.setStyle(Paint.Style.FILL);
        leafPaint.setAntiAlias(true);

        // Darker green for leaf vein
        Paint veinPaint = new Paint();
        veinPaint.setColor(Color.rgb(50, 150, 70));
        veinPaint.setStyle(Paint.Style.STROKE);
        veinPaint.setStrokeWidth(4f);
        veinPaint.setStrokeCap(Paint.Cap.ROUND);
        veinPaint.setAntiAlias(true);

        // Large leaf like reference
        float leafLen = 180f;
        float leafWid = 90f;

        // Angle pointing upward-outward like reference
        float angle = isLeft ? -50 : 50;

        canvas.save();
        canvas.translate(stemX, stemY);
        canvas.rotate(angle);

        // Smooth pointed oval leaf shape like reference
        Path leafPath = new Path();
        leafPath.moveTo(0, 0);
        // Top curve
        leafPath.cubicTo(
                leafLen * 0.25f, -leafWid * 0.6f,
                leafLen * 0.6f, -leafWid * 0.5f,
                leafLen, 0);
        // Bottom curve
        leafPath.cubicTo(
                leafLen * 0.6f, leafWid * 0.5f,
                leafLen * 0.25f, leafWid * 0.6f,
                0, 0);
        leafPath.close();

        canvas.drawPath(leafPath, leafPaint);

        // Draw center vein line
        canvas.drawLine(10, 0, leafLen - 20, 0, veinPaint);

        // Photo thumbnail on leaf
        if (index < memories.size()) {
            Memory m = memories.get(index);
            String firstImg = m.getFirstImage();

            if (firstImg != null && !firstImg.isEmpty()) {
                File imgFile = new File(firstImg);
                if (imgFile.exists()) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 8;
                        Bitmap bmp = BitmapFactory.decodeFile(firstImg, opts);
                        if (bmp != null) {
                            float imgSize = 55f;
                            float imgX = leafLen * 0.5f - imgSize / 2;
                            float imgY = -imgSize / 2;

                            Paint imgPaint = new Paint();
                            imgPaint.setAntiAlias(true);

                            RectF imgRect = new RectF(imgX, imgY, imgX + imgSize, imgY + imgSize);
                            canvas.drawBitmap(bmp, null, imgRect, imgPaint);
                            bmp.recycle();
                        }
                    } catch (Exception e) {
                        drawPlaceholder(canvas, leafLen);
                    }
                } else {
                    drawPlaceholder(canvas, leafLen);
                }
            } else {
                drawPlaceholder(canvas, leafLen);
            }
        }

        canvas.restore();

        // Date label
        if (index < memories.size() && memories.get(index).date != null) {
            String dateLabel = memories.get(index).date;
            textPaint.setTextSize(20f);
            textPaint.setColor(Color.rgb(120, 120, 120));
            float labelX = isLeft ? stemX - 110 : stemX + 110;
            canvas.drawText(dateLabel, labelX, stemY + 8, textPaint);
        }

        clickZones.add(new LeafHitBox(stemX, stemY, 110f, index));
    }

    private void drawPlaceholder(Canvas canvas, float leafLen) {
        Paint placeholderPaint = new Paint();
        placeholderPaint.setColor(Color.WHITE);
        placeholderPaint.setAlpha(220);
        placeholderPaint.setStyle(Paint.Style.FILL);
        placeholderPaint.setAntiAlias(true);

        float size = 45f;
        float x = leafLen * 0.5f - size / 2;
        float y = -size / 2;

        RectF rect = new RectF(x, y, x + size, y + size);
        canvas.drawRoundRect(rect, 10, 10, placeholderPaint);

        // Plus icon
        Paint plusPaint = new Paint();
        plusPaint.setColor(Color.rgb(160, 160, 160));
        plusPaint.setStrokeWidth(3);
        plusPaint.setStrokeCap(Paint.Cap.ROUND);
        plusPaint.setAntiAlias(true);
        canvas.drawLine(x + size / 2, y + 12, x + size / 2, y + size - 12, plusPaint);
        canvas.drawLine(x + 12, y + size / 2, x + size - 12, y + size / 2, plusPaint);
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();

            for (int i = clickZones.size() - 1; i >= 0; i--) {
                LeafHitBox box = clickZones.get(i);
                float dist = (float) Math.hypot(tx - box.x, ty - box.y);
                if (dist < box.r) {
                    if (leafClickListener != null) {
                        String date = "";
                        if (i < memories.size())
                            date = memories.get(i).date;
                        leafClickListener.onLeafClick(i, date);
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
