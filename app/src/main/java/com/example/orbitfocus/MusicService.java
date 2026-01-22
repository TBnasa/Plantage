package com.example.orbitfocus;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MusicService - Arka plan müzik çalma servisi.
 * 
 * Özellikler:
 * - 4 müzik dosyasını rastgele sırayla çalar
 * - Kesintisiz geçiş (Gapless playback with NextMediaPlayer)
 * - Sonsuz döngü (liste bitince tekrar karıştırıp başlar)
 * - Arka planda çalmaya devam eder
 * - Ses seviyesi %35 (arka plan müziği için uygun)
 */
public class MusicService extends Service {

    private static final String TAG = "MusicService";
    private static final float BACKGROUND_VOLUME = 0.35f; // %35 ses seviyesi

    private MediaPlayer currentPlayer;
    private MediaPlayer nextPlayer;
    private List<Integer> playlist;
    private int currentIndex = 0;
    private boolean isPrepared = false;
    private float currentVolume = BACKGROUND_VOLUME;

    // Müzik dosyaları (res/raw klasöründe)
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

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicService onCreate");
        initializePlaylist();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MusicService onStartCommand");
        if (!isPrepared) {
            startPlayback();
        }
        return START_STICKY; // Servis öldürülürse yeniden başlat
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Playlist'i oluşturur ve karıştırır.
     */
    private void initializePlaylist() {
        playlist = new ArrayList<>();
        for (int resId : musicResources) {
            playlist.add(resId);
        }
        shufflePlaylist();
        currentIndex = 0;
        Log.d(TAG, "Playlist initialized with " + playlist.size() + " tracks (shuffled)");
    }

    /**
     * Playlist'i rastgele karıştırır.
     */
    private void shufflePlaylist() {
        Collections.shuffle(playlist);
        Log.d(TAG, "Playlist shuffled");
    }

    /**
     * Müzik çalmayı başlatır.
     */
    private void startPlayback() {
        if (playlist == null || playlist.isEmpty()) {
            initializePlaylist();
        }

        try {
            releasePlayer(currentPlayer);
            currentPlayer = createPlayer(playlist.get(currentIndex));

            if (currentPlayer != null) {
                currentPlayer.setVolume(currentVolume, currentVolume);
                currentPlayer.setOnCompletionListener(mp -> playNext());
                currentPlayer.start();
                isPrepared = true;

                // Sonraki parçayı hazırla (gapless geçiş için)
                prepareNextPlayer();

                Log.d(TAG, "Started playing track " + (currentIndex + 1));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting playback: " + e.getMessage());
        }
    }

    /**
     * Sonraki parçayı önceden hazırlar (gapless geçiş için).
     */
    private void prepareNextPlayer() {
        try {
            int nextIndex = (currentIndex + 1) % playlist.size();

            releasePlayer(nextPlayer);
            nextPlayer = createPlayer(playlist.get(nextIndex));

            if (nextPlayer != null && currentPlayer != null) {
                nextPlayer.setVolume(currentVolume, currentVolume);
                // Gapless playback için setNextMediaPlayer kullan
                currentPlayer.setNextMediaPlayer(nextPlayer);
                Log.d(TAG, "Next player prepared for track " + (nextIndex + 1));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing next player: " + e.getMessage());
        }
    }

    /**
     * Sonraki parçaya geçer.
     */
    private void playNext() {
        currentIndex++;

        // Liste bitti mi kontrol et
        if (currentIndex >= playlist.size()) {
            // Listeyi yeniden karıştır ve başa dön
            shufflePlaylist();
            currentIndex = 0;
            Log.d(TAG, "Playlist completed, reshuffled and starting over");
        }

        // Önceki player'ı temizle
        releasePlayer(currentPlayer);

        // Hazırlanan next player'ı current yap
        currentPlayer = nextPlayer;
        nextPlayer = null;

        if (currentPlayer != null) {
            currentPlayer.setOnCompletionListener(mp -> playNext());

            // Eğer henüz çalmıyorsa başlat
            if (!currentPlayer.isPlaying()) {
                currentPlayer.start();
            }

            Log.d(TAG, "Playing track " + (currentIndex + 1));

            // Sonraki parçayı hazırla
            prepareNextPlayer();
        } else {
            // Yedek: Manuel başlat
            startPlayback();
        }
    }

    /**
     * MediaPlayer oluşturur.
     */
    private MediaPlayer createPlayer(int resId) {
        try {
            MediaPlayer player = MediaPlayer.create(this, resId);
            if (player != null) {
                player.setVolume(currentVolume, currentVolume);
            }
            return player;
        } catch (Exception e) {
            Log.e(TAG, "Error creating player: " + e.getMessage());
            return null;
        }
    }

    /**
     * MediaPlayer'ı güvenli şekilde serbest bırakır.
     */
    private void releasePlayer(MediaPlayer player) {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.reset();
                player.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing player: " + e.getMessage());
            }
        }
    }

    /**
     * Ses seviyesini ayarlar (0.0 - 1.0).
     */
    public void setVolume(float volume) {
        currentVolume = Math.max(0f, Math.min(1f, volume));
        if (currentPlayer != null) {
            currentPlayer.setVolume(currentVolume, currentVolume);
        }
        if (nextPlayer != null) {
            nextPlayer.setVolume(currentVolume, currentVolume);
        }
        Log.d(TAG, "Volume set to " + (int) (currentVolume * 100) + "%");
    }

    /**
     * Mevcut ses seviyesini döndürür.
     */
    public float getVolume() {
        return currentVolume;
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

    /**
     * Müzik çalıyor mu kontrol eder.
     */
    public boolean isPlaying() {
        return currentPlayer != null && currentPlayer.isPlaying();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MusicService onDestroy");
        releasePlayer(currentPlayer);
        releasePlayer(nextPlayer);
        currentPlayer = null;
        nextPlayer = null;
        isPrepared = false;
        super.onDestroy();
    }
}
