package com.tbnasa.plantage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import com.tbnasa.plantage.model.Leaf;
import com.tbnasa.plantage.model.LeafStatus;

/**
 * PlantageTreeView - Modern Fractal Tree implementation.
 */
public class PlantageTreeView extends View {

    private List<Leaf> leaves = new ArrayList<>();
    private List<LeafHitBox> clickZones = new ArrayList<>();
    private OnLeafClickListener leafClickListener;

    private Paint stemPaint;
    private Paint leafLightPaint;
    private Paint leafDarkPaint;
    private Paint potPaint;
    private Paint potRimPaint;
    private Paint soilPaint;
    private Paint shadowPaint;
    private Paint textPaint;

    private int colorBackground;
    private static final int COLOR_STEM = Color.parseColor("#27AE60");
    private static final int COLOR_LEAF_LIGHT = Color.parseColor("#40D47E");
    private static final int COLOR_LEAF_DARK = Color.parseColor("#2ECC71");
    private static final int COLOR_GROWING_LIGHT = Color.parseColor("#A9DFBF");
    private static final int COLOR_GROWING_DARK = Color.parseColor("#7DCEA0");
    private static final int COLOR_WITHERED_LIGHT = Color.parseColor("#D4C5B0");
    private static final int COLOR_WITHERED_DARK = Color.parseColor("#B8A995");
    private static final int COLOR_LOCKED_LIGHT = Color.parseColor("#58D68D");
    private static final int COLOR_LOCKED_DARK = Color.parseColor("#1E8449");
    private static final int COLOR_SHADOW = Color.argb(20, 0, 0, 0);

    private static final float POT_HEIGHT = 100f;
    private static final float POT_TOP_WIDTH = 160f;
    private static final float POT_BOTTOM_WIDTH = 120f;
    private static final float POT_MARGIN_BOTTOM = 60f;

    private int leafIndexCounter = 0;

    private class LeafHitBox {
        float x, y, radius;
        int index;
        LeafHitBox(float x, float y, float radius, int index) {
            this.x = x; this.y = y; this.radius = radius; this.index = index;
        }
    }

    public interface OnLeafClickListener {
        void onLeafClick(int index, Leaf leaf);
    }

    public PlantageTreeView(Context context) { super(context); init(); }
    public PlantageTreeView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        colorBackground = ContextCompat.getColor(getContext(), R.color.colorBackground);
        stemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stemPaint.setColor(COLOR_STEM);
        stemPaint.setStyle(Paint.Style.STROKE);
        stemPaint.setStrokeCap(Paint.Cap.ROUND);

        leafLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leafLightPaint.setStyle(Paint.Style.FILL);

        leafDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leafDarkPaint.setStyle(Paint.Style.FILL);

        potPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        potRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        soilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(COLOR_SHADOW);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
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
        canvas.drawColor(colorBackground);

        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float potBottomY = height - POT_MARGIN_BOTTOM;
        float potTopY = potBottomY - POT_HEIGHT;

        clickZones.clear();

        // 1. Shadow
        canvas.drawOval(new RectF(centerX - 90, potBottomY - 5, centerX + 90, potBottomY + 5), shadowPaint);

        // 2. Pot
        potPaint.setColor(Color.rgb(185, 100, 70));
        Path potPath = new Path();
        potPath.moveTo(centerX - POT_TOP_WIDTH / 2 + 10, potTopY);
        potPath.lineTo(centerX + POT_TOP_WIDTH / 2 - 10, potTopY);
        potPath.lineTo(centerX + POT_BOTTOM_WIDTH / 2, potBottomY);
        potPath.lineTo(centerX - POT_BOTTOM_WIDTH / 2, potBottomY);
        potPath.close();
        canvas.drawPath(potPath, potPaint);

        // 3. Soil
        soilPaint.setColor(Color.parseColor("#5D4037"));
        canvas.drawOval(new RectF(centerX - 60, potTopY - 10, centerX + 60, potTopY + 10), soilPaint);

        // 4. Fractal Tree
        drawFractalTree(canvas, centerX, potTopY);

        // 5. Pot Rim
        potRimPaint.setColor(Color.rgb(210, 120, 90));
        canvas.drawRoundRect(new RectF(centerX - POT_TOP_WIDTH / 2, potTopY - 15, centerX + POT_TOP_WIDTH / 2, potTopY + 15), 10, 10, potRimPaint);

        // Slow animation for sway
        postInvalidateDelayed(50);
    }

    private void drawFractalTree(Canvas canvas, float cx, float baseY) {
        if (leaves == null || leaves.isEmpty()) return;
        leafIndexCounter = 0;
        
        int totalLeaves = leaves.size();
        int maxDepth = (int) Math.ceil(Math.log(totalLeaves) / Math.log(2));
        if (maxDepth < 3) maxDepth = 3;
        if (maxDepth > 7) maxDepth = 7;

        float initialLength = 160f;
        float initialThickness = 12f;

        drawBranch(canvas, cx, baseY, -90f, initialLength, initialThickness, 0, maxDepth);
    }

    private void drawBranch(Canvas canvas, float x1, float y1, float angle, float length, float thickness, int depth, int maxDepth) {
        float sway = (float)(Math.sin(System.currentTimeMillis() / 1500.0 + depth) * 1.5);
        float currentAngle = angle + sway;

        float x2 = x1 + (float) (Math.cos(Math.toRadians(currentAngle)) * length);
        float y2 = y1 + (float) (Math.sin(Math.toRadians(currentAngle)) * length);

        stemPaint.setStrokeWidth(thickness);
        canvas.drawLine(x1, y1, x2, y2, stemPaint);

        if (depth < maxDepth) {
            float subAngle = 28f;
            float subLength = length * 0.72f;
            float subThickness = thickness * 0.65f;

            drawBranch(canvas, x2, y2, currentAngle - subAngle, subLength, subThickness, depth + 1, maxDepth);
            drawBranch(canvas, x2, y2, currentAngle + subAngle, subLength, subThickness, depth + 1, maxDepth);
        } else {
            if (leafIndexCounter < leaves.size()) {
                drawFractalLeaf(canvas, x2, y2, currentAngle, leafIndexCounter);
                leafIndexCounter++;
            }
        }
    }

    private void drawFractalLeaf(Canvas canvas, float x, float y, float angle, int index) {
        Leaf leaf = leaves.get(index);
        float scale = 0.85f;
        if (leaf.status == LeafStatus.GROWING) {
            scale *= (0.2f + (leaf.getGrowthProgress() * 0.8f));
        }

        int lightColor = COLOR_LEAF_LIGHT;
        int darkColor = COLOR_LEAF_DARK;
        if (leaf.status == LeafStatus.GROWING) { lightColor = COLOR_GROWING_LIGHT; darkColor = COLOR_GROWING_DARK; }
        else if (leaf.status == LeafStatus.WITHERED) { lightColor = COLOR_WITHERED_LIGHT; darkColor = COLOR_WITHERED_DARK; }
        else if (leaf.status == LeafStatus.LOCKED) { lightColor = COLOR_LOCKED_LIGHT; darkColor = COLOR_LOCKED_DARK; }

        leafLightPaint.setColor(lightColor);
        leafDarkPaint.setColor(darkColor);

        float radius = 35f * scale;

        canvas.save();
        canvas.translate(x, y);
        
        // Circular glowing leaf
        canvas.drawCircle(0, 0, radius, leafDarkPaint);
        canvas.drawCircle(0, 0, radius * 0.7f, leafLightPaint);

        if (leaf.status == LeafStatus.ACTIVE && !leaf.hasContent()) {
            textPaint.setTextSize(22f * scale);
            canvas.drawText("+", 0, 8f * scale, textPaint);
        }
        canvas.restore();

        if (leaf.status != LeafStatus.GROWING) {
            clickZones.add(new LeafHitBox(x, y, 50f * scale, index));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();
            for (int i = clickZones.size() - 1; i >= 0; i--) {
                LeafHitBox box = clickZones.get(i);
                if (Math.hypot(tx - box.x, ty - box.y) < box.radius) {
                    if (leafClickListener != null && box.index < leaves.size()) {
                        leafClickListener.onLeafClick(box.index, leaves.get(box.index));
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Renders the tree to a Bitmap for the home screen widget.
     */
    public static Bitmap renderToBitmap(Context context, int width, int height, List<Leaf> leaves) {
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        PlantageTreeView tempView = new PlantageTreeView(context);
        tempView.setLeaves(leaves);
        tempView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        );
        tempView.layout(0, 0, width, height);
        tempView.draw(canvas);

        return bitmap;
    }
}
