package com.tbnasa.plantage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity — Hosts BottomNavigationView and swaps between 4 Fragments:
 * Timeline, Gardens, Growth, Settings.
 *
 * Shared services (DatabaseHelper, LanguageManager, MusicService) are
 * exposed via getters so Fragments can access them without re-instantiating.
 */
public class MainActivity extends AppCompatActivity {

    // ─── Shared services ───
    private DatabaseHelper dbHelper;
    private LanguageManager lang;
    private MusicService musicService;
    private boolean musicBound = false;

    // ─── Image picker ───
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> fileExportLauncher;
    private ActivityResultLauncher<String> fileImportLauncher;
    private long pendingImageLeafId = -1;
    private BackupManager backupManager;

    // ─── Fragments (cached) ───
    private final TimelineFragment timelineFragment = new TimelineFragment();
    // GardensFragment removed
    private final GrowthFragment growthFragment = new GrowthFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private Fragment activeFragment;

    // ─── Service connection ───
    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
            musicService = null;
        }
    };

    // ─── Lifecycle ───

    @Override
    protected void attachBaseContext(Context newBase) {
        LanguageManager langManager = new LanguageManager(newBase);
        langManager.applyTheme(); // Apply dark/light theme
        super.attachBaseContext(langManager.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init shared services
        lang = new LanguageManager(this);
        dbHelper = new DatabaseHelper(this);
        lang.scheduleNotifications(lang.isRemindersEnabled());

        // Start music service
        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);
        bindService(musicIntent, musicConnection, Context.BIND_AUTO_CREATE);

        // Image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && pendingImageLeafId != -1 && activeFragment instanceof TimelineFragment) {
                        ((TimelineFragment) activeFragment).onImagePicked(pendingImageLeafId, uri);
                    }
                    pendingImageLeafId = -1;
                });

        backupManager = new BackupManager(this);

        fileExportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                uri -> {
                    if (uri != null && activeFragment instanceof SettingsFragment) {
                        ((SettingsFragment) activeFragment).onExportLocationSelected(uri);
                    }
                });

        fileImportLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && activeFragment instanceof SettingsFragment) {
                        ((SettingsFragment) activeFragment).onImportFileSelected(uri);
                    }
                });

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_timeline) {
                switchFragment(timelineFragment);
            } else if (id == R.id.nav_status) {
                switchFragment(growthFragment);
            } else if (id == R.id.nav_settings) {
                switchFragment(settingsFragment);
            }
            return true;
        });

        // Default tab
        if (savedInstanceState == null) {
            switchFragment(timelineFragment);
            bottomNav.setSelectedItemId(R.id.nav_timeline);
        }
    }

    // ─── Fragment management ───

    private void switchFragment(Fragment target) {
        if (target == activeFragment)
            return;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, target)
                .commit();
        activeFragment = target;
    }

    // ─── Public getters for Fragments ───

    public DatabaseHelper getDbHelper() {
        return dbHelper;
    }

    public LanguageManager getLang() {
        return lang;
    }

    public MusicService getMusicService() {
        return musicService;
    }

    /**
     * Called by TimelineFragment to launch the image picker for a given leaf.
     */
    public void launchImagePicker(long leafId) {
        pendingImageLeafId = leafId;
        imagePickerLauncher.launch("image/*");
    }

    public void launchFileExport(String fileName) {
        fileExportLauncher.launch(fileName);
    }

    public void launchFileImport() {
        fileImportLauncher.launch("*/*");
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    // showTimelineFiltered removed

    @Override
    protected void onStart() {
        super.onStart();
        if (musicBound && musicService != null) {
            musicService.resume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (musicBound && musicService != null) {
            musicService.pause();
        }
    }

    // ─── Lifecycle cleanup ───

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop music playback completely
        Intent musicIntent = new Intent(this, MusicService.class);
        stopService(musicIntent);
        if (musicBound) {
            unbindService(musicConnection);
            musicBound = false;
        }
    }
}
