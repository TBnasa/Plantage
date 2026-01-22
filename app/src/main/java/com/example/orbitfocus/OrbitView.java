package com.example.orbitfocus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class OrbitView extends View {

    private List<TaskBall> taskBalls = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>(); // Particle system
    private List<Star> stars = new ArrayList<>(); // Background stars

    private Paint dropZonePaint;
    private RectF dropZone;
    private TaskBall draggedBall = null;
    private OnTaskActionListener listener;
    private boolean isHovering = false;
    private Random random = new Random();

    public float getCameraX() {
        return cameraX;
    }

    public float getCameraY() {
        return cameraY;
    }

    // World & Camera
    private float worldWidth = 3000f;
    private float worldHeight = 3000f;
    private float cameraX = 0f;
    private float cameraY = 0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isPanning = false;
    private float lastDragX;
    private float lastDragY;
    private long lastDragTime;

    // Particle Class
    private static class Particle {
        float x, y;
        float vx, vy;
        float alpha;
        float size;
        int color;
        float decay;

        Particle(float x, float y, int color, boolean isExplosion) {
            this.x = x;
            this.y = y;
            this.color = color;
            Random r = new Random();
            if (isExplosion) {
                this.vx = (r.nextFloat() - 0.5f) * 15f;
                this.vy = (r.nextFloat() - 0.5f) * 15f;
                this.size = 5f + r.nextFloat() * 10f;
                this.decay = 0.05f;
                this.alpha = 1.0f;
            } else {
                // Ambient
                this.vx = (r.nextFloat() - 0.5f) * 2f;
                this.vy = (r.nextFloat() - 0.5f) * 2f;
                this.size = 2f + r.nextFloat() * 4f;
                this.decay = 0.005f;
                this.alpha = 0.5f + r.nextFloat() * 0.5f;
            }
        }

        boolean update() {
            x += vx;
            y += vy;
            alpha -= decay;
            return alpha <= 0;
        }
    }

    // Star Class for background
    private static class Star {
        float x, y, size;
        int alpha;

        Star(float x, float y, float size, int alpha) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.alpha = alpha;
        }
    }

    public interface OnTaskActionListener {
        void onTaskCompleted(TaskBall task);
    }

    public OrbitView(Context context) {
        super(context);
        init();
    }

    public OrbitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dropZonePaint = new Paint();
        dropZonePaint.setColor(Color.parseColor("#33FF0000")); // Semi-transparent Red
        dropZonePaint.setStyle(Paint.Style.FILL);
        dropZonePaint.setAntiAlias(true);

        // Generate Stars for the Space Background
        for (int i = 0; i < 300; i++) {
            float s = 1f + random.nextFloat() * 3f;
            stars.add(new Star(random.nextFloat() * worldWidth, random.nextFloat() * worldHeight, s,
                    100 + random.nextInt(155)));
        }

        // Center Camera roughly initialy
        cameraX = worldWidth / 2f - 500;
        cameraY = worldHeight / 2f - 1000;
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<TaskBall> tasks) {
        this.taskBalls = tasks;
        float density = getResources().getDisplayMetrics().density;
        // Randomly distribute new tasks in the world if they are at 0,0
        for (TaskBall t : taskBalls) {
            t.updateScale(density);
            if (t.x == 0 && t.y == 0) {
                t.x = random.nextFloat() * worldWidth;
                t.y = random.nextFloat() * worldHeight;
            }
        }
        invalidate();
    }

    public void addTask(TaskBall task) {
        // Position at center of CURRENT VIEW
        task.x = cameraX + getWidth() / 2f;
        task.y = cameraY + getHeight() / 2f;

        task.updateScale(getResources().getDisplayMetrics().density);
        taskBalls.add(task);
        invalidate();
    }

    public void updateSensorData(float x, float y) {
        // No longer used for physics in Space Mode
        // kept to avoid compilation error in MainActivity
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Define drop zone at bottom right of the SCREEN (fixed UI)
        float size = 250f;
        dropZone = new RectF(w - size, h - size, w, h);

        // Center camera precisely now that we know screen size
        if (oldw == 0) {
            cameraX = (worldWidth - w) / 2f;
            cameraY = (worldHeight - h) / 2f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw Space Background (Static)
        canvas.drawColor(Color.BLACK); // Deep Space

        // 2. Transform Camera
        canvas.save();
        canvas.translate(-cameraX, -cameraY);

        // 3. Draw Stars
        Paint starPaint = new Paint();
        starPaint.setColor(Color.WHITE);
        for (Star s : stars) {
            starPaint.setAlpha(s.alpha);
            canvas.drawCircle(s.x, s.y, s.size, starPaint);
        }

        // 4. Draw Particles (World Space)
        Iterator<Particle> pDivider = particles.iterator();
        Paint pPaint = new Paint();
        pPaint.setAntiAlias(true);
        while (pDivider.hasNext()) {
            Particle p = pDivider.next();
            if (p.update()) {
                pDivider.remove();
            } else {
                pPaint.setColor(p.color);
                pPaint.setAlpha((int) (p.alpha * 255));
                canvas.drawCircle(p.x, p.y, p.size, pPaint);
            }
        }

        // Ambient Particles near Camera
        if (random.nextInt(20) == 0 && particles.size() < 100) {
            float px = cameraX + random.nextFloat() * getWidth();
            float py = cameraY + random.nextFloat() * getHeight();
            particles.add(new Particle(px, py, Color.WHITE, false));
        }

        // 5. Draw Tasks
        if (taskBalls.isEmpty()) {
            Paint emptyPaint = new Paint();
            emptyPaint.setColor(Color.WHITE);
            emptyPaint.setAlpha(150);
            emptyPaint.setTextSize(60f);
            emptyPaint.setTextAlign(Paint.Align.CENTER);
            emptyPaint.setAntiAlias(true);
            emptyPaint.setShadowLayer(10, 0, 0, Color.CYAN);
            // Draw text relative to camera center
            canvas.drawText("Yeni bir görev ekleyin...", cameraX + getWidth() / 2f, cameraY + getHeight() / 2f,
                    emptyPaint);
        } else {
            for (int i = 0; i < taskBalls.size(); i++) {
                TaskBall b1 = taskBalls.get(i);

                b1.animate();
                // Physics update with World Dimensions
                b1.updatePhysics(worldWidth, worldHeight);

                for (int j = i + 1; j < taskBalls.size(); j++) {
                    b1.resolveCollision(taskBalls.get(j));
                }

                b1.draw(canvas);
            }
        }

        canvas.restore(); // END WORLD SPACE

        // 6. Draw Fixed UI (Drop Zone)
        if (dropZone != null) {
            if (isHovering) {
                dropZonePaint.setColor(Color.parseColor("#66FF0000"));
                canvas.drawRoundRect(dropZone.left - 10, dropZone.top - 10, dropZone.right, dropZone.bottom, 40, 40,
                        dropZonePaint);
            } else {
                dropZonePaint.setColor(Color.parseColor("#33FF0000"));
                canvas.drawRoundRect(dropZone, 30, 30, dropZonePaint);
            }

            android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(getContext(),
                    R.drawable.ic_check);
            if (d != null) {
                int iconSize = isHovering ? 120 : 100;
                int left = (int) dropZone.centerX() - iconSize / 2;
                int top = (int) dropZone.centerY() - iconSize / 2;
                d.setBounds(left, top, left + iconSize, top + iconSize);
                d.draw(canvas);
            }
            Paint p = new Paint();
            p.setColor(Color.WHITE);
            p.setTextSize(30f);
            p.setTextAlign(Paint.Align.CENTER);
            p.setAntiAlias(true);
            canvas.drawText("DONE", dropZone.centerX(), dropZone.bottom - 40, p);
        }

        invalidate();
    }

    public void explode(float x, float y, int color) {
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle(x, y, color, true));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float screenX = event.getX();
        float screenY = event.getY();

        // Convert screen coordinates to world coordinates
        float worldX = screenX + cameraX;
        float worldY = screenY + cameraY;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = screenX;
                lastTouchY = screenY;
                isPanning = true;

                // Check for ball click (Reverse order for Z-index)
                for (int i = taskBalls.size() - 1; i >= 0; i--) {
                    TaskBall b = taskBalls.get(i);
                    if (b.contains(worldX, worldY)) {
                        draggedBall = b;
                        b.isDragging = true;
                        isPanning = false;

                        // Valid start for velocity tracking
                        lastDragX = worldX;
                        lastDragY = worldY;
                        lastDragTime = System.currentTimeMillis();

                        // Check Drop Zone
                        if (dropZone != null)
                            isHovering = dropZone.contains(screenX, screenY);
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (draggedBall != null) {
                    // Track velocity
                    long now = System.currentTimeMillis();
                    long dt = now - lastDragTime;
                    if (dt > 0) {
                        // Calculate velocity
                        float vx = (worldX - lastDragX) / dt * 50f; // Scale up
                        float vy = (worldY - lastDragY) / dt * 50f;

                        // Clamp
                        float maxSpeed = 40f;
                        if (vx > maxSpeed)
                            vx = maxSpeed;
                        if (vx < -maxSpeed)
                            vx = -maxSpeed;
                        if (vy > maxSpeed)
                            vy = maxSpeed;
                        if (vy < -maxSpeed)
                            vy = -maxSpeed;

                        draggedBall.vx = vx;
                        draggedBall.vy = vy;
                    }

                    lastDragX = worldX;
                    lastDragY = worldY;
                    lastDragTime = now;

                    // Move ball
                    draggedBall.x = worldX;
                    draggedBall.y = worldY;

                    if (dropZone != null) {
                        isHovering = dropZone.contains(screenX, screenY);
                    }
                } else if (isPanning) {
                    // Pan Camera
                    float dx = lastTouchX - screenX;
                    float dy = lastTouchY - screenY;
                    cameraX += dx;
                    cameraY += dy;

                    // Clamp Camera to World
                    if (cameraX < 0)
                        cameraX = 0;
                    if (cameraY < 0)
                        cameraY = 0;
                    if (cameraX > worldWidth - getWidth())
                        cameraX = worldWidth - getWidth();
                    if (cameraY > worldHeight - getHeight())
                        cameraY = worldHeight - getHeight();

                    lastTouchX = screenX;
                    lastTouchY = screenY;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (draggedBall != null) {
                    if (dropZone != null && dropZone.contains(screenX, screenY)) {
                        if (listener != null) {
                            listener.onTaskCompleted(draggedBall);
                            explode(draggedBall.x, draggedBall.y, draggedBall.color);
                        }
                        taskBalls.remove(draggedBall);
                    }
                    draggedBall.isDragging = false;
                    draggedBall = null;
                }
                isHovering = false;
                isPanning = false;
                break;
        }
        return true;
    }
}
