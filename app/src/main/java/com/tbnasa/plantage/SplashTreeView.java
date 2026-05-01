package com.tbnasa.plantage;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * SplashTreeView — Custom Canvas-drawn animated tree for the splash screen.
 * Draws a stylized potted tree with multiple leaf clusters and smooth entry animation.
 */
public class SplashTreeView extends View {

    // ── Tree color constants ──
    private static final int TRUNK_DARK   = 0xFF5C3D1E;
    private static final int TRUNK_LIGHT  = 0xFF7A5230;
    private static final int LEAF_BACK    = 0xFF1E4A1E;
    private static final int LEAF_MID     = 0xFF2D4A2D;
    private static final int LEAF_MID2    = 0xFF254E25;
    private static final int LEAF_FRONT   = 0xFF3A7A3A;
    private static final int LEAF_HIGH    = 0xFF4A9A4A;
    private static final int LEAF_BRIGHT  = 0xFF5DB05D;
    private static final int POT_BODY     = 0xFF8B5E3C;
    private static final int POT_RIM      = 0xFFA06A42;
    private static final int GLOW_COLOR   = 0x374A7A4A;

    private final Paint paintTrunk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLeaf  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPot   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlow  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Animation progress [0,1]
    private float trunkProgress = 0f;
    private float leafProgress  = 0f;

    public SplashTreeView(Context context) {
        super(context);
        init();
    }

    public SplashTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SplashTreeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintTrunk.setStyle(Paint.Style.STROKE);
        paintTrunk.setStrokeCap(Paint.Cap.ROUND);
        paintLeaf.setStyle(Paint.Style.FILL);
        paintPot.setStyle(Paint.Style.FILL);
        paintGlow.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    private void startAnimation() {
        // Trunk grows first
        ValueAnimator trunkAnim = ValueAnimator.ofFloat(0f, 1f);
        trunkAnim.setDuration(1400);
        trunkAnim.setInterpolator(new DecelerateInterpolator());
        trunkAnim.addUpdateListener(a -> {
            trunkProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        trunkAnim.start();

        // Leaves fade in slightly later
        ValueAnimator leafAnim = ValueAnimator.ofFloat(0f, 1f);
        leafAnim.setDuration(900);
        leafAnim.setStartDelay(200);
        leafAnim.setInterpolator(new DecelerateInterpolator());
        leafAnim.addUpdateListener(a -> {
            leafProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        leafAnim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float density = getContext().getResources().getDisplayMetrics().density;

        // Coordinate system helpers
        float cx = w * 0.5f;           // Horizontal center
        float potTop = h * 0.72f;      // Top of the pot
        float trunkBase = potTop;       // Trunk starts at pot top
        float trunkTop = h * 0.35f;    // Trunk ends here
        float trunkMid = (trunkBase + trunkTop) / 2f;

        // ── 1. GLOW (under tree) ──────────────────────────────────────────
        RadialGradient glow = new RadialGradient(cx, potTop,
                80 * density,
                new int[]{GLOW_COLOR, 0x00000000},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP);
        paintGlow.setShader(glow);
        canvas.drawOval(new RectF(cx - 80 * density, potTop - 20 * density,
                cx + 80 * density, potTop + 20 * density), paintGlow);
        paintGlow.setShader(null);

        // ── 2. ROOT HINTS ─────────────────────────────────────────────────
        paintTrunk.setColor(TRUNK_DARK);
        paintTrunk.setStrokeWidth(2 * density);
        canvas.drawLine(cx - 4 * density, trunkBase, cx - 14 * density, trunkBase + 5 * density, paintTrunk);
        canvas.drawLine(cx + 4 * density, trunkBase, cx + 14 * density, trunkBase + 5 * density, paintTrunk);

        // ── 3. MAIN TRUNK ─────────────────────────────────────────────────
        float animatedTrunkTop = trunkBase - (trunkBase - trunkTop) * trunkProgress;

        // Left edge (darker)
        paintTrunk.setColor(TRUNK_DARK);
        paintTrunk.setStrokeWidth(9 * density);
        Path leftTrunk = new Path();
        leftTrunk.moveTo(cx - 4 * density, trunkBase);
        leftTrunk.quadTo(cx - 6 * density, trunkMid, cx - 3 * density, animatedTrunkTop);
        canvas.drawPath(leftTrunk, paintTrunk);

        // Right edge (lighter)
        paintTrunk.setColor(TRUNK_LIGHT);
        paintTrunk.setStrokeWidth(6 * density);
        Path rightTrunk = new Path();
        rightTrunk.moveTo(cx + 4 * density, trunkBase);
        rightTrunk.quadTo(cx + 7 * density, trunkMid, cx + 3 * density, animatedTrunkTop);
        canvas.drawPath(rightTrunk, paintTrunk);

        // ── 4. MAIN BRANCHES ─────────────────────────────────────────────
        if (trunkProgress > 0.4f) {
            float branchAlpha = Math.min(1f, (trunkProgress - 0.4f) / 0.6f);
            paintTrunk.setAlpha((int)(255 * branchAlpha));

            float branchOriginY = trunkTop + 30 * density;

            // Left large branch
            paintTrunk.setColor(TRUNK_DARK);
            paintTrunk.setStrokeWidth(5 * density);
            Path leftBig = new Path();
            leftBig.moveTo(cx, branchOriginY);
            leftBig.quadTo(cx - 30 * density, branchOriginY - 20 * density,
                           cx - 55 * density, branchOriginY - 45 * density);
            canvas.drawPath(leftBig, paintTrunk);

            // Right large branch
            paintTrunk.setColor(TRUNK_LIGHT);
            Path rightBig = new Path();
            rightBig.moveTo(cx, branchOriginY);
            rightBig.quadTo(cx + 30 * density, branchOriginY - 20 * density,
                            cx + 55 * density, branchOriginY - 45 * density);
            canvas.drawPath(rightBig, paintTrunk);

            // Center top branch
            paintTrunk.setColor(TRUNK_DARK);
            paintTrunk.setStrokeWidth(4 * density);
            Path centerBranch = new Path();
            centerBranch.moveTo(cx, trunkTop);
            centerBranch.quadTo(cx + 5 * density, trunkTop - 20 * density,
                                cx, trunkTop - 40 * density);
            canvas.drawPath(centerBranch, paintTrunk);

            // Left lower branch
            paintTrunk.setColor(TRUNK_LIGHT);
            paintTrunk.setStrokeWidth(3 * density);
            Path leftLow = new Path();
            leftLow.moveTo(cx, trunkMid);
            leftLow.quadTo(cx - 20 * density, trunkMid - 10 * density,
                           cx - 40 * density, trunkMid - 25 * density);
            canvas.drawPath(leftLow, paintTrunk);

            // Right lower branch
            Path rightLow = new Path();
            rightLow.moveTo(cx, trunkMid);
            rightLow.quadTo(cx + 20 * density, trunkMid - 10 * density,
                            cx + 40 * density, trunkMid - 25 * density);
            canvas.drawPath(rightLow, paintTrunk);

            // Sub-branches (smaller)
            paintTrunk.setStrokeWidth(2 * density);
            paintTrunk.setColor(TRUNK_LIGHT);
            canvas.drawLine(cx - 55 * density, branchOriginY - 45 * density,
                            cx - 70 * density, branchOriginY - 60 * density, paintTrunk);
            canvas.drawLine(cx - 55 * density, branchOriginY - 45 * density,
                            cx - 45 * density, branchOriginY - 65 * density, paintTrunk);
            canvas.drawLine(cx + 55 * density, branchOriginY - 45 * density,
                            cx + 70 * density, branchOriginY - 60 * density, paintTrunk);
            canvas.drawLine(cx + 55 * density, branchOriginY - 45 * density,
                            cx + 45 * density, branchOriginY - 65 * density, paintTrunk);

            paintTrunk.setAlpha(255);
        }

        // ── 5. LEAF CLUSTERS ─────────────────────────────────────────────
        float leafAlpha = leafProgress;
        float leafTranslate = (1f - leafProgress) * 8 * density; // slide up
        float centerY = h * 0.30f;

        canvas.save();
        canvas.translate(0, leafTranslate);

        // Back layer (10 ovals, dark)
        paintLeaf.setAlpha((int)(200 * leafAlpha));
        paintLeaf.setColor(LEAF_BACK);
        drawLeafCluster(canvas, cx, centerY, density, 10, 26, 20, 0);

        // Mid layer (12 ovals)
        paintLeaf.setColor(LEAF_MID);
        drawLeafCluster(canvas, cx, centerY, density, 12, 30, 22, 15);

        // Mid2 layer
        paintLeaf.setColor(LEAF_MID2);
        drawLeafCluster(canvas, cx, centerY - 5 * density, density, 10, 28, 20, 30);

        // Front layer (larger)
        paintLeaf.setColor(LEAF_FRONT);
        drawLeafCluster(canvas, cx, centerY - 8 * density, density, 12, 32, 24, 50);

        // Highlight layer (small, top)
        paintLeaf.setColor(LEAF_HIGH);
        drawLeafCluster(canvas, cx, centerY - 16 * density, density, 5, 20, 14, 70);

        // Bright individual highlights
        paintLeaf.setColor(LEAF_BRIGHT);
        paintLeaf.setAlpha((int)(180 * leafAlpha));
        drawLeafCluster(canvas, cx, centerY - 18 * density, density, 3, 14, 10, 80);

        canvas.restore();

        // ── 6. POT ───────────────────────────────────────────────────────
        paintPot.setColor(POT_BODY);
        float potH = 22 * density;
        float potW = 34 * density;
        Path potPath = new Path();
        potPath.moveTo(cx - potW * 0.8f, potTop);
        potPath.lineTo(cx + potW * 0.8f, potTop);
        potPath.lineTo(cx + potW, potTop + potH);
        potPath.lineTo(cx - potW, potTop + potH);
        potPath.close();
        canvas.drawPath(potPath, paintPot);

        // Pot rim
        paintPot.setColor(POT_RIM);
        canvas.drawRoundRect(new RectF(cx - potW * 0.85f, potTop - 4 * density,
                cx + potW * 0.85f, potTop + 5 * density),
                3 * density, 3 * density, paintPot);
    }

    /**
     * Draws a cluster of leaf ovals distributed in a rough circle.
     * @param count     number of ovals
     * @param radW      half-width of each oval in dp
     * @param radH      half-height of each oval in dp
     * @param spreadDeg spread angle in degrees (rotation)
     */
    private void drawLeafCluster(Canvas canvas, float cx, float cy, float density,
                                  int count, float radW, float radH, float spreadDeg) {
        float spread = 48 * density;
        float rW = radW * density;
        float rH = radH * density;
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians((360.0 / count) * i + spreadDeg);
            float ex = (float) Math.cos(angle) * spread * 0.7f;
            float ey = (float) Math.sin(angle) * spread * 0.45f;
            canvas.save();
            canvas.rotate((float)(Math.toDegrees(angle)), cx + ex, cy + ey);
            canvas.drawOval(new RectF(cx + ex - rW, cy + ey - rH,
                                     cx + ex + rW, cy + ey + rH), paintLeaf);
            canvas.restore();
        }
        // Center fill
        canvas.drawOval(new RectF(cx - rW * 0.8f, cy - rH * 0.8f,
                                   cx + rW * 0.8f, cy + rH * 0.8f), paintLeaf);
    }
}
