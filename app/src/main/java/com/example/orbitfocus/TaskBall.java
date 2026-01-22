package com.example.orbitfocus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;

import android.text.TextPaint;

public class TaskBall {
    public int id;
    public String text;
    public int color;
    public float x, y; // Position
    public float vx, vy; // Velocity
    public float radius = 100f; // This will start as 0 for animation and grow to targetRadius
    public float targetRadius;
    public boolean isDragging = false;

    private Paint paint;
    private TextPaint textPaint;

    public TaskBall(int id, String text, int color) {
        this.id = id;
        this.text = text;
        this.color = color;
        this.radius = 0f; // Start invisible/small

        // Initialize Paints
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        // Shadow for depth
        paint.setShadowLayer(15, 5, 5, 0xAA000000);

        textPaint = new TextPaint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(5, 0, 0, Color.BLACK);

        // Randomize initial position
        Random rnd = new Random();
        this.x = 200 + rnd.nextInt(500);
        this.y = 200 + rnd.nextInt(500);
        // Initial gentle drift
        this.vx = rnd.nextFloat() * 10 - 5;
        this.vy = rnd.nextFloat() * 10 - 5;
    }

    public void updateScale(float density) {
        this.targetRadius = 60f * density;
        if (this.radius == 0f) {
            // If it's the very first time, we might want to start at 0,
            // but targetRadius is set.
        }
        this.textPaint.setTextSize(14f * density);
    }

    private float wobblePhase = 0f;

    public void animate() {
        // Simple spring/ease-out to target radius
        if (radius < targetRadius) {
            radius += (targetRadius - radius) * 0.1f;
            if (Math.abs(targetRadius - radius) < 1f)
                radius = targetRadius;
        }
        wobblePhase += 0.1f;
    }

    public void draw(Canvas canvas) {
        canvas.save();

        // Jelly / Wobble Effect
        float scaleX = 1.0f + 0.05f * (float) Math.sin(wobblePhase);
        float scaleY = 1.0f + 0.05f * (float) Math.sin(wobblePhase + Math.PI / 2); // Out of phase

        // Pivot at center
        canvas.translate(x, y);
        canvas.scale(scaleX, scaleY);
        canvas.translate(-x, -y);

        // Enhanced 3D Glassy/Glowing Look

        // 1. Outer Glow (Soft Halo)
        if (paint.getShader() != null)
            paint.setShader(null);
        paint.setColor(color);
        paint.setAlpha(100); // Semi-transparent
        paint.setShadowLayer(radius * 0.5f, 0, 0, color); // Glowing edge
        canvas.drawCircle(x, y, radius, paint);
        paint.setShadowLayer(0, 0, 0, 0); // Reset

        // 2. Main Body (Gradient)
        if (paint.getShader() == null || paint.getColor() != color) {
            paint.setShader(new android.graphics.RadialGradient(
                    x - radius * 0.3f, y - radius * 0.3f,
                    radius * 1.5f,
                    new int[] { Color.argb(255, 255, 255, 255), color, darkenColor(color) },
                    new float[] { 0.0f, 0.4f, 1.0f },
                    android.graphics.Shader.TileMode.CLAMP));
            paint.setAlpha(255);
        } else {
            paint.setShader(new android.graphics.RadialGradient(
                    x - radius * 0.3f, y - radius * 0.3f,
                    radius * 1.5f,
                    new int[] { Color.argb(255, 255, 255, 255), color, darkenColor(color) },
                    new float[] { 0.0f, 0.4f, 1.0f },
                    android.graphics.Shader.TileMode.CLAMP));
        }
        canvas.drawCircle(x, y, radius * 0.9f, paint);

        // 3. Specular Highlight (Reflection)
        Paint highlightPaint = new Paint();
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setAntiAlias(true);
        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setAlpha(80);
        canvas.drawOval(x - radius * 0.5f, y - radius * 0.6f, x + radius * 0.2f, y - radius * 0.3f, highlightPaint);

        // Draw text
        drawWrappedText(canvas);

        canvas.restore();
    }

    private int darkenColor(int color) {
        float[] msg = new float[3];
        Color.colorToHSV(color, msg);
        msg[2] *= 0.8f;
        return Color.HSVToColor(msg);
    }

    private void drawWrappedText(Canvas canvas) {
        android.text.StaticLayout.Builder builder = android.text.StaticLayout.Builder.obtain(
                text, 0, text.length(), textPaint, (int) (radius * 1.6f));
        builder.setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL);
        android.text.StaticLayout layout = builder.build();

        canvas.save();
        canvas.translate(x, y - layout.getHeight() / 2f);
        layout.draw(canvas);
        canvas.restore();
    }

    public boolean contains(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    public void updatePhysics(float width, float height) {
        if (isDragging)
            return;

        // Apply Friction (Slow down)
        vx *= 0.98f;
        vy *= 0.98f;

        if (Math.abs(vx) < 0.1f)
            vx = 0;
        if (Math.abs(vy) < 0.1f)
            vy = 0;

        // Update position
        x += vx;
        y += vy;

        // Bounce off World Walls
        if (x - radius < 0) {
            x = radius;
            vx = -vx * 0.8f;
        } else if (x + radius > width) {
            x = width - radius;
            vx = -vx * 0.8f;
        }

        if (y - radius < 0) {
            y = radius;
            vy = -vy * 0.8f;
        } else if (y + radius > height) {
            y = height - radius;
            vy = -vy * 0.8f;
        }
    }

    // Simple collision resolution with another ball
    public void resolveCollision(TaskBall other) {
        float dx = other.x - x;
        float dy = other.y - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float minDistance = radius + other.radius;

        if (distance < minDistance) {
            // Overlap amount
            float overlap = minDistance - distance;

            // Normalize direction
            float nx = dx / distance;
            float ny = dy / distance;

            // Move apart
            float moveX = nx * overlap * 0.5f;
            float moveY = ny * overlap * 0.5f;

            this.x -= moveX;
            this.y -= moveY;
            other.x += moveX;
            other.y += moveY;

            // Elastic collision (swap velocities roughly or bounce)
            // Simplified: average momentum exchange or just bounce
            // Vector reflection is better but simple bounce for UI is often enough
            float k = 0.5f; // reduced bounce for softness
            this.vx -= nx * k;
            this.vy -= ny * k;
            other.vx += nx * k;
            other.vy += ny * k;
        }
    }
}
