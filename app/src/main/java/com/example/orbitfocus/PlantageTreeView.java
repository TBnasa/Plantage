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
import com.example.orbitfocus.model.Leaf;
import com.example.orbitfocus.model.LeafStatus;

/**
 * PlantageTreeView - Fraktal Ağaç Custom View
 * 
 * Özellikler:
 * - Dinamik büyüme: Her Leaf için bir dal
 * - Fraktal/recursive dallanma yapısı
 * - Açık yeşil renk paleti
 * - Görsel hata koruması (canvas.save/restore)
 * - Klasik terracotta saksı + gölge
 */
public class PlantageTreeView extends View {

    // ==================== DATA ====================
    private List<Leaf> leaves = new ArrayList<>();
    private List<LeafHitBox> clickZones = new ArrayList<>();
    private OnLeafClickListener leafClickListener;

    // ==================== PAINTS ====================
    private Paint stemPaint;
    private Paint leafLightPaint;
    private Paint leafDarkPaint;
    private Paint potPaint;
    private Paint potRimPaint;
    private Paint soilPaint;
    private Paint shadowPaint;
    private Paint textPaint;

    // ==================== COLOR PALETTE - AÇIK YEŞİL ====================
    private static final int COLOR_BACKGROUND = Color.WHITE;

    // Gövde - Açık Yeşil
    private static final int COLOR_STEM = Color.rgb(102, 187, 106);

    // Yapraklar - Mint & Pastel Yeşil
    private static final int COLOR_LEAF_LIGHT = Color.rgb(165, 214, 167);
    private static final int COLOR_LEAF_DARK = Color.rgb(129, 199, 132);

    // Solmuş Yapraklar
    private static final int COLOR_WITHERED_LIGHT = Color.rgb(188, 170, 164);
    private static final int COLOR_WITHERED_DARK = Color.rgb(161, 136, 127);

    // Kilitli Yapraklar
    private static final int COLOR_LOCKED_LIGHT = Color.rgb(200, 200, 200);
    private static final int COLOR_LOCKED_DARK = Color.rgb(170, 170, 170);

    // Saksı - Klasik Terracotta
    private static final int COLOR_POT_MAIN = Color.rgb(194, 125, 95);
    private static final int COLOR_POT_RIM = Color.rgb(220, 160, 130);
    private static final int COLOR_SOIL = Color.rgb(100, 72, 60);

    // Gölge
    private static final int COLOR_SHADOW = Color.argb(40, 0, 0, 0);

    // ==================== DIMENSIONS ====================
    private static final float POT_HEIGHT = 100f;
    private static final float POT_TOP_WIDTH = 160f;
    private static final float POT_BOTTOM_WIDTH = 120f;
    private static final float POT_MARGIN_BOTTOM = 50f;

    // ==================== HITBOX ====================
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

    // ==================== CONSTRUCTORS ====================
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

        // Leaf Paints
        leafLightPaint = new Paint();
        leafLightPaint.setStyle(Paint.Style.FILL);
        leafLightPaint.setAntiAlias(true);

        leafDarkPaint = new Paint();
        leafDarkPaint.setStyle(Paint.Style.FILL);
        leafDarkPaint.setAntiAlias(true);

        // Pot Paint
        potPaint = new Paint();
        potPaint.setColor(COLOR_POT_MAIN);
        potPaint.setStyle(Paint.Style.FILL);
        potPaint.setAntiAlias(true);

        // Pot Rim Paint
        potRimPaint = new Paint();
        potRimPaint.setColor(COLOR_POT_RIM);
        potRimPaint.setStyle(Paint.Style.FILL);
        potRimPaint.setAntiAlias(true);

        // Soil Paint
        soilPaint = new Paint();
        soilPaint.setColor(COLOR_SOIL);
        soilPaint.setStyle(Paint.Style.FILL);
        soilPaint.setAntiAlias(true);

        // Shadow Paint
        shadowPaint = new Paint();
        shadowPaint.setColor(COLOR_SHADOW);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setAntiAlias(true);

        // Text Paint
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
    }

    // ==================== PUBLIC METHODS ====================
    public void setLeaves(List<Leaf> leaves) {
        this.leaves = leaves;
        invalidate();
    }

    public void setOnLeafClickListener(OnLeafClickListener listener) {
        this.leafClickListener = listener;
    }

    // ==================== ONDRAW ====================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Background
        canvas.drawColor(COLOR_BACKGROUND);

        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float potBottomY = height - POT_MARGIN_BOTTOM;
        float potTopY = potBottomY - POT_HEIGHT;

        // Clear click zones
        clickZones.clear();

        // === 1. SHADOW (Strictly Under Pot) ===
        canvas.save();
        drawShadow(canvas, centerX, potBottomY + 5f); // 5px below pot base - moved up
        canvas.restore();

        // === 2. POT BODY ===
        canvas.save();
        // Slightly darker terracotta for better look
        potPaint.setColor(Color.rgb(185, 100, 70));
        drawPot(canvas, centerX, potBottomY);
        canvas.restore();

        // === 3. SOIL (Inside Rim Area) ===
        // Soil center should be aligned with Rim center
        canvas.save();
        drawSoil(canvas, centerX, potTopY);
        canvas.restore();

        // === 4. TREE (Logo Style - Straight Stem) ===
        // Tree comes out from the center of the Soil
        canvas.save();
        int leafCount = leaves.size();
        drawLogoTree(canvas, centerX, potTopY, leafCount);
        canvas.restore();

        // === 5. POT RIM (Covers the connection point) ===
        canvas.save();
        // Rim slightly lighter
        potRimPaint.setColor(Color.rgb(210, 120, 90));
        drawPotRim(canvas, centerX, potTopY);
        canvas.restore();
    }

    // ==================== SHADOW ====================
    private void drawShadow(Canvas canvas, float cx, float bottomY) {
        // Flat oval shadow - slightly tighter
        RectF shadowRect = new RectF(cx - 95, bottomY - 6, cx + 95, bottomY + 6);
        canvas.drawOval(shadowRect, shadowPaint);
    }

    // ==================== POT ====================
    private void drawPot(Canvas canvas, float cx, float bottomY) {
        Path potPath = new Path();
        potPath.moveTo(cx - POT_TOP_WIDTH / 2 + 10, bottomY - POT_HEIGHT); // Slightly inset top
        potPath.lineTo(cx + POT_TOP_WIDTH / 2 - 10, bottomY - POT_HEIGHT);
        potPath.lineTo(cx + POT_BOTTOM_WIDTH / 2, bottomY);
        potPath.lineTo(cx - POT_BOTTOM_WIDTH / 2, bottomY);
        potPath.close();
        canvas.drawPath(potPath, potPaint);
    }

    private void drawPotRim(Canvas canvas, float cx, float topY) {
        // Rim covers the top edge
        RectF rimRect = new RectF(
                cx - POT_TOP_WIDTH / 2,
                topY - 15,
                cx + POT_TOP_WIDTH / 2,
                topY + 15);
        canvas.drawRoundRect(rimRect, 8, 8, potRimPaint);
    }

    // ==================== SOIL ====================
    private void drawSoil(Canvas canvas, float cx, float topY) {
        // Soil is slightly smaller than rim, creating depth
        RectF soilRect = new RectF(
                cx - POT_TOP_WIDTH / 2 + 15,
                topY - 8,
                cx + POT_TOP_WIDTH / 2 - 15,
                topY + 8);
        canvas.drawOval(soilRect, soilPaint);
    }

    // ==================== LOGO STYLE TREE ====================
    /**
     * Logo Style Tree
     * Straight stem, symmetric leaves, clean look.
     */

    // ==================== LOGO STYLE TREE ====================
    /**
     * Logo Style Tree
     * Straight stem, symmetric leaves, clean look.
     */
    private void drawLogoTree(Canvas canvas, float cx, float baseY, int leafCount) {
        if (leafCount <= 0)
            return;

        float treeHeight = 350f;
        float stemTopY = baseY - treeHeight;

        // === MAIN STEM ===
        stemPaint.setStrokeWidth(18f);
        stemPaint.setColor(COLOR_STEM);
        // Draw stem slightly higher than top leaf base
        canvas.drawLine(cx, baseY, cx, stemTopY + 50f, stemPaint);

        // Get Top Leaf (Last one - "Anı Yaprağı")
        int topLeafIndex = leaves.size() - 1;
        Leaf topLeaf = leaves.get(topLeafIndex);

        // Side Leaves (All except last one)
        int sideLeafCount = Math.max(0, leafCount - 1);

        // === SIDE LEAVES ===
        if (sideLeafCount > 0) {
            float startY = baseY - 60f;
            // Distribute side leaves
            float spacePerLeaf = (startY - (stemTopY + 80f)) / Math.max(1, sideLeafCount);

            for (int i = 0; i < sideLeafCount; i++) {
                float y = startY - (i * spacePerLeaf);
                boolean isLeft = (i % 2 == 0);

                float scale = 1.0f - (i * 0.05f);
                scale = Math.max(0.6f, scale);

                drawLogoLeaf(canvas, cx, y, isLeft, i, leaves.get(i), scale);
            }
        }

        // === TOP LEAF (Angled 25 degrees right) ===
        // The last leaf is placed at the top
        drawLogoLeaf(canvas, cx, stemTopY + 60f, topLeafIndex, topLeaf, 1.0f, 25f);
    }

    /**
     * Standard Leaf Call (Calculates angle based on side)
     */
    private void drawLogoLeaf(Canvas canvas, float stemX, float stemY, boolean isLeft, int index, Leaf leaf,
            float scale) {
        float angle = isLeft ? -45f : 45f;
        drawLogoLeaf(canvas, stemX, stemY, index, leaf, scale, angle);
    }

    /**
     * Actual Leaf Drawing (With specific angle)
     */
    private void drawLogoLeaf(Canvas canvas, float stemX, float stemY, int index, Leaf leaf, float scale, float angle) {
        // Status Colors
        int lightColor = COLOR_LEAF_LIGHT;
        int darkColor = COLOR_LEAF_DARK;
        if (leaf.status == LeafStatus.WITHERED) {
            lightColor = COLOR_WITHERED_LIGHT;
            darkColor = COLOR_WITHERED_DARK;
        } else if (leaf.status == LeafStatus.LOCKED) {
            lightColor = COLOR_LOCKED_LIGHT;
            darkColor = COLOR_LOCKED_DARK;
        }

        leafLightPaint.setColor(lightColor);
        leafDarkPaint.setColor(darkColor);

        float h = 110f * scale;
        float w = 55f * scale;

        canvas.save();
        canvas.translate(stemX, stemY);
        canvas.rotate(angle);

        // Use a stem offset so leaf doesn't overlap main stem weirdly
        // Move out slightly
        float offset = 8f;
        canvas.translate(0, -offset);

        // Shape: Broad oval
        Path lightHalf = new Path();
        lightHalf.moveTo(0, 0);
        lightHalf.cubicTo(w, -h * 0.3f, w * 0.5f, -h * 0.8f, 0, -h);
        lightHalf.close();
        canvas.drawPath(lightHalf, leafLightPaint);

        Path darkHalf = new Path();
        darkHalf.moveTo(0, 0);
        darkHalf.cubicTo(-w, -h * 0.3f, -w * 0.5f, -h * 0.8f, 0, -h);
        darkHalf.close();
        canvas.drawPath(darkHalf, leafDarkPaint);

        // Content
        if (leaf.status == LeafStatus.ACTIVE) {
            String img = leaf.getFirstImage();
            if (img != null && !img.isEmpty()) {
                drawThumbnail(canvas, -h * 0.45f, img, scale);
            } else {
                textPaint.setAlpha(220);
                textPaint.setTextSize(24f * scale);
                canvas.drawText("+", 0, -h * 0.45f, textPaint);
            }
        }

        canvas.restore();

        // Hitbox (only if real leaf)
        if (index >= 0) {
            clickZones.add(new LeafHitBox(stemX, stemY, 70f * scale, index));
        }
    }

    private void drawThumbnail(Canvas canvas, float centerY, String imagePath, float scale) {
        try {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 8;
                Bitmap bmp = BitmapFactory.decodeFile(imagePath, opts);
                if (bmp != null) {
                    float size = 30f * scale;
                    RectF rect = new RectF(-size / 2, centerY - size / 2, size / 2, centerY + size / 2);

                    canvas.save();
                    Path clip = new Path();
                    clip.addOval(rect, Path.Direction.CW);
                    canvas.clipPath(clip);
                    canvas.drawBitmap(bmp, null, rect, null);
                    canvas.restore();

                    bmp.recycle();
                }
            }
        } catch (Exception e) {
            // Hata durumunda + işareti göster
            textPaint.setAlpha(200);
            canvas.drawText("+", 0, centerY, textPaint);
        }
    }

    // ==================== TOUCH ====================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();

            for (int i = clickZones.size() - 1; i >= 0; i--) {
                LeafHitBox box = clickZones.get(i);
                float dist = (float) Math.hypot(tx - box.x, ty - box.y);
                if (dist < box.radius) {
                    if (leafClickListener != null && box.index < leaves.size()) {
                        leafClickListener.onLeafClick(box.index, leaves.get(box.index));
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
