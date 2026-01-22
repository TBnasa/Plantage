package com.example.orbitfocus.model;

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

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public Leaf(long id, String date, String content, String imagePaths, LeafStatus status) {
        this.id = id;
        this.date = date;
        this.content = content;
        this.imagePaths = imagePaths;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Bu yaprak bugüne ait mi kontrol eder.
     */
    public boolean isToday() {
        String today = dateFormat.format(new Date());
        return today.equals(this.date);
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
     * Yaprağın deadline'ı geçip geçmediğini kontrol eder.
     * Deadline: Yaprağın tarihinden sonraki gün saat 09:00
     */
    public boolean isDeadlinePassed() {
        try {
            Date leafDate = dateFormat.parse(this.date);
            if (leafDate == null)
                return false;

            Calendar deadline = Calendar.getInstance();
            deadline.setTime(leafDate);
            deadline.add(Calendar.DAY_OF_YEAR, 1);
            deadline.set(Calendar.HOUR_OF_DAY, 0); // Gece yarısı 00:00
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
     * Bu metod çağrıldığında mevcut zamana göre durum güncellenir.
     * NOT: Eğer yaprak zaten WITHERED veya LOCKED ise, durumu değişmez.
     */
    public LeafStatus calculateCurrentStatus() {
        // Eğer yaprak zaten WITHERED ise (manuel silme dahil), durumu koru
        if (this.status == LeafStatus.WITHERED) {
            return LeafStatus.WITHERED;
        }

        // Eğer yaprak LOCKED ise (ölümsüz), durumu koru
        if (this.status == LeafStatus.LOCKED) {
            return LeafStatus.LOCKED;
        }

        // ACTIVE yapraklar için deadline kontrolü yap
        if (!isDeadlinePassed()) {
            // Deadline henüz geçmedi - hala ACTIVE
            return LeafStatus.ACTIVE;
        } else {
            // Deadline geçti
            if (hasContent()) {
                // İçerik var - LOCKED (kilitli/ölümsüz)
                return LeafStatus.LOCKED;
            } else {
                // İçerik yok - WITHERED (kurumuş)
                return LeafStatus.WITHERED;
            }
        }
    }
}
