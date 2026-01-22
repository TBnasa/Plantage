package com.example.orbitfocus;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MusicService - Arka plan müzik çalma servisi.
 * Özellikler:
 * - Fade-Out / Fade-In geçişleri
 * - Sonsuz döngü (Shuffle)
 * - Arka planda çalma
 */
public class MusicService extends Service {

    private static final String TAG = "MusicService";
    private float targetVolume = 0.35f; // Hedef ses seviyesi (değiştirilebilir)
    private static final int FADE_DURATION = 3000; // 3 saniye fade süresi
    private static final int FADE_INTERVAL = 100; // 100ms güncelleme sıklığı
    private float fadeStep = 0.01f; // Dinamik hesaplanacak

    private MediaPlayer currentPlayer;
    private List<Integer> playlist;
    private int currentIndex = 0;
    private float currentVolume = 0f;
    private boolean isFadingOut = false;

    // İzleme ve Fade işlemleri için Handler
    private Handler handler = new Handler(Looper.getMainLooper());

    // Müzik dosyaları
    private final int[] musicResources = {
            R.raw.muzik_1,
            R.raw.muzik_2,
            R.raw.muzik_3,
            R.raw.muzik_4
    };

    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    // Periyodik kontrol: Şarkı bitimine az kaldı mı?
    private Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentPlayer != null && currentPlayer.isPlaying() && !isFadingOut) {
                int remaining = currentPlayer.getDuration() - currentPlayer.getCurrentPosition();
                // Bitişe FADE_DURATION kadar kaldıysa FadeOut başlat
                if (remaining > 0 && remaining <= FADE_DURATION) {
                    startFadeOut();
                }
            }
            handler.postDelayed(this, 1000); // Her saniye kontrol et
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initializePlaylist();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (currentPlayer == null) {
            startPlayback();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void initializePlaylist() {
        playlist = new ArrayList<>();
        for (int resId : musicResources) {
            playlist.add(resId);
        }
        shufflePlaylist();
    }

    private void shufflePlaylist() {
        Collections.shuffle(playlist);
    }

    private void startPlayback() {
        if (playlist.isEmpty())
            initializePlaylist();

        playTrack(playlist.get(currentIndex));
        handler.post(monitorRunnable); // İzlemeyi başlat
    }

    private void playTrack(int resId) {
        try {
            // Varsa eski player'ı temizle
            if (currentPlayer != null) {
                currentPlayer.release();
            }

            currentPlayer = MediaPlayer.create(this, resId);
            if (currentPlayer != null) {
                // Başlangıç sesi 0 (Fade-In için)
                currentVolume = 0f;
                currentPlayer.setVolume(currentVolume, currentVolume);

                currentPlayer.setOnCompletionListener(mp -> {
                    // Normalde monitorRunnable bitişten önce yakalar ama
                    // çok kısa şarkılarda burası devreye girebilir
                    playNext();
                });

                currentPlayer.start();
                isFadingOut = false;
                startFadeIn(); // Fade-In ile başla

                Log.d(TAG, "Playing track index: " + currentIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing track", e);
        }
    }

    private void playNext() {
        currentIndex++;
        if (currentIndex >= playlist.size()) {
            currentIndex = 0;
            shufflePlaylist();
        }
        playTrack(playlist.get(currentIndex));
    }

    private void startFadeIn() {
        // Step hesapla: Hedef sese 3 saniyede ulaşmalı
        float steps = (float) FADE_DURATION / FADE_INTERVAL;
        fadeStep = targetVolume / steps;

        handler.removeCallbacks(fadeOutRunnable); // FadeOut varsa iptal
        handler.post(fadeInRunnable);
    }

    private void startFadeOut() {
        isFadingOut = true;
        // Step hesapla: Mevcut sesten 0'a 3 saniyede inmeli
        float steps = (float) FADE_DURATION / FADE_INTERVAL;
        fadeStep = currentVolume / steps;

        Log.d(TAG, "Starting Fade Out");
        handler.removeCallbacks(fadeInRunnable); // FadeIn varsa iptal
        handler.post(fadeOutRunnable);
    }

    private Runnable fadeInRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentPlayer == null)
                return;

            currentVolume += fadeStep;
            if (currentVolume >= targetVolume) {
                currentVolume = targetVolume;
                currentPlayer.setVolume(currentVolume, currentVolume);
                // Fade bitti
            } else {
                currentPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(this, FADE_INTERVAL);
            }
        }
    };

    private Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentPlayer == null)
                return;

            currentVolume -= fadeStep;
            if (currentVolume <= 0f) {
                currentVolume = 0f;
                currentPlayer.setVolume(currentVolume, currentVolume);
                // Ses tamamen kısıldı, bir sonrakine geç
                playNext();
            } else {
                currentPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(this, FADE_INTERVAL);
            }
        }
    };

    // ==================== PUBLIC METHODS (MainActivity için) ====================

    /**
     * Ses seviyesini ayarlar (0.0 - 1.0).
     */
    public void setVolume(float volume) {
        targetVolume = Math.max(0f, Math.min(1f, volume));

        // Eğer şu an fade yapmıyorsak, sesi direkt güncelle veya fade et
        // Basitlik için eğer fade yoksa direkt uygula
        boolean isFading = (handler.hasCallbacks(fadeInRunnable) || handler.hasCallbacks(fadeOutRunnable));
        if (!isFading) {
            currentVolume = targetVolume;
            if (currentPlayer != null) {
                currentPlayer.setVolume(currentVolume, currentVolume);
            }
        }
        Log.d(TAG, "Volume set to " + targetVolume);
    }

    /**
     * Mevcut (hedef) ses seviyesini döndürür.
     */
    public float getVolume() {
        return targetVolume;
    }

    /**
     * Müziği duraklatır.
     */
    public void pause() {
        if (currentPlayer != null && currentPlayer.isPlaying()) {
            currentPlayer.pause();
            Log.d(TAG, "Music paused");
        }
    }

    /**
     * Müziği devam ettirir.
     */
    public void resume() {
        if (currentPlayer != null && !currentPlayer.isPlaying()) {
            currentPlayer.start();
            Log.d(TAG, "Music resumed");
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null); // Tüm runnable'ları durdur
        if (currentPlayer != null) {
            currentPlayer.release();
            currentPlayer = null;
        }
        super.onDestroy();
    }
}
