package com.tbnasa.plantage.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Yaprak (Leaf) veri sınıfı.
 * Her yaprak bir günü ve o güne ait anıları temsil eder.
 */
public class Leaf {
    public long id;
    public String date; // YYYY-MM-DD formatında
    public String content; // Yazılı anı metni
    public String imagePaths; // Virgülle ayrılmış fotoğraf/video yolları
    public LeafStatus status; // Yaprak durumu
    public long createdAt; // Oluşturulma zamanı (millis)
    // Category removed

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public Leaf(long id, String date, String content, String imagePaths, LeafStatus status, long createdAt) {
        this.id = id;
        this.date = date;
        this.content = content;
        this.imagePaths = imagePaths;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Yaprağın büyüme ilerlemesini döndürür (0.0 - 1.0).
     */
    public float getGrowthProgress() {
        try {
            Date leafDate = dateFormat.parse(this.date);
            if (leafDate == null) return 1.0f;

            long targetStartMillis = leafDate.getTime();
            long now = System.currentTimeMillis();

            // Eğer bu yarının yaprağıysa, bugünün başlangıcından yarına kadar büyür
            Calendar cal = Calendar.getInstance();
            cal.setTime(leafDate);
            cal.add(Calendar.DAY_OF_YEAR, -1);
            long growthStartMillis = cal.getTimeInMillis();

            if (now < growthStartMillis) return 0.05f; // Çok küçük
            if (now >= targetStartMillis) return 1.0f; // Tamamlandı

            float progress = (float) (now - growthStartMillis) / (24 * 60 * 60 * 1000f);
            return Math.min(1.0f, Math.max(0.05f, progress));
        } catch (Exception e) {
            return 1.0f;
        }
    }

    /**
     * Bu yaprak bugüne ait mi kontrol eder.
     */
    public boolean isToday() {
        String today = dateFormat.format(new Date());
        return today.equals(this.date);
    }

    /**
     * Bu yaprak yarına mı ait?
     */
    public boolean isTomorrow() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = dateFormat.format(cal.getTime());
        return tomorrow.equals(this.date);
    }

    /**
     * İçerik var mı kontrol eder (yazı veya fotoğraf).
     */
    public boolean hasContent() {
        boolean hasText = content != null && !content.trim().isEmpty();
        boolean hasImages = imagePaths != null && !imagePaths.trim().isEmpty();
        return hasText || hasImages;
    }

    /**
     * İlk fotoğrafı döndürür (thumbnail için).
     */
    public String getFirstImage() {
        if (imagePaths == null || imagePaths.isEmpty())
            return null;
        String[] paths = imagePaths.split(",");
        return paths.length > 0 ? paths[0] : null;
    }

    /**
     * Tüm fotoğrafları array olarak döndürür.
     */
    public String[] getAllImages() {
        if (imagePaths == null || imagePaths.isEmpty())
            return new String[0];
        return imagePaths.split(",");
    }

    /**
     * Yeni fotoğraf yolu ekler.
     */
    public String addImagePath(String newPath) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return newPath;
        }
        return imagePaths + "," + newPath;
    }

    /**
     * Yaprağın büyüme süresinin dolup dolmadığını kontrol eder.
     */
    public boolean isMature() {
        try {
            Date leafDate = dateFormat.parse(this.date);
            if (leafDate == null) return true;
            return System.currentTimeMillis() >= leafDate.getTime();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Yaprağın deadline'ı geçip geçmediğini kontrol eder.
     * Deadline: Yaprak gününden sonraki gece yarısı
     */
    public boolean isDeadlinePassed() {
        try {
            Date leafDate = dateFormat.parse(this.date);
            if (leafDate == null)
                return false;

            Calendar deadline = Calendar.getInstance();
            deadline.setTime(leafDate);
            deadline.add(Calendar.DAY_OF_YEAR, 1);
            deadline.set(Calendar.HOUR_OF_DAY, 0);
            deadline.set(Calendar.MINUTE, 0);
            deadline.set(Calendar.SECOND, 0);

            Calendar now = Calendar.getInstance();
            return now.after(deadline);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Yaprağın güncel durumunu hesaplar ve döndürür.
     */
    public LeafStatus calculateCurrentStatus() {
        if (this.status == LeafStatus.WITHERED || this.status == LeafStatus.LOCKED) {
            return this.status;
        }

        // 1. Yarının yaprağı mı? -> Büyüyor
        if (isTomorrow()) {
            return LeafStatus.GROWING;
        }

        // 2. Bugünün yaprağı mı? -> Aktif
        if (isToday()) {
            return LeafStatus.ACTIVE;
        }

        // 3. Geçmiş yaprak mı?
        if (isDeadlinePassed()) {
            if (hasContent()) {
                return LeafStatus.LOCKED;
            } else {
                return LeafStatus.WITHERED;
            }
        }

        return LeafStatus.ACTIVE;
    }
}
