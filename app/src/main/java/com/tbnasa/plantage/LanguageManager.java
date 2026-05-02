package com.tbnasa.plantage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.util.Locale;

/**
 * Dil ve Ayar yöneticisi.
 * Güvenli SharedPreferences kullanarak ayarları saklar.
 */
public class LanguageManager {

    private static final String PREFS_NAME = "PlantageSecurePrefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BIOMETRIC = "biometric_enabled";
    private static final String KEY_REMINDERS = "reminders_enabled";
    private static final String KEY_REMINDER_FREQUENCY = "reminder_frequency"; // in minutes

    public static final String LANG_TR = "tr";
    public static final String LANG_EN = "en";
    public static final String LANG_RU = "ru";
    public static final String LANG_ZH = "zh";
    public static final String LANG_DE = "de";
    public static final String LANG_FR = "fr";

    private Context context;
    private SharedPreferences prefs;

    public LanguageManager(Context context) {
        this.context = context;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            this.prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            this.prefs = context.getSharedPreferences("PlantagePrefs", Context.MODE_PRIVATE);
        }
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, LANG_EN);
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        applyTheme();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply();
    }

    public boolean isRemindersEnabled() {
        return prefs.getBoolean(KEY_REMINDERS, true);
    }

    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDERS, enabled).apply();
        scheduleNotifications(enabled);
    }

    public int getReminderFrequency() {
        return prefs.getInt(KEY_REMINDER_FREQUENCY, 300); // Default 5 hours (300 mins)
    }

    public void setReminderFrequency(int minutes) {
        prefs.edit().putInt(KEY_REMINDER_FREQUENCY, minutes).apply();
        scheduleNotifications(true);
    }

    public void scheduleNotifications(boolean enabled) {
        androidx.work.WorkManager wm = androidx.work.WorkManager.getInstance(context);
        if (enabled) {
            int mins = getReminderFrequency();
            if (mins < 15) mins = 15; // WorkManager minimum
            
            androidx.work.PeriodicWorkRequest request = new androidx.work.PeriodicWorkRequest.Builder(
                    NotificationWorker.class, mins, java.util.concurrent.TimeUnit.MINUTES)
                    .build();
            wm.enqueueUniquePeriodicWork("garden_reminders", 
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE, request);
        } else {
            wm.cancelUniqueWork("garden_reminders");
        }
    }

    public void applyTheme() {
        if (isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Applies the current language to the given context and returns the updated context.
     */
    public Context applyLocale(Context context) {
        String lang = getLanguage();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public String[] getQuotes() {
        return context.getResources().getStringArray(R.array.quotes);
    }

    // String resource getters
    public String getSettings() { return context.getString(R.string.settings); }
    public String getMusicVolume() { return context.getString(R.string.music_volume); }
    public String getLanguage_() { return context.getString(R.string.language); }
    public String getOk() { return context.getString(R.string.ok); }
    public String getCancel() { return context.getString(R.string.cancel); }
    public String getSave() { return context.getString(R.string.save); }
    public String getClose() { return context.getString(R.string.close); }
    public String getDelete() { return context.getString(R.string.delete); }
    public String getNextLeaf() { return context.getString(R.string.next_leaf); }
    public String getPhotos() { return context.getString(R.string.photos); }
    public String getAddPhoto() { return context.getString(R.string.add_photo); }
    public String getWriteMemory() { return context.getString(R.string.write_memory); }
    public String getWhatHappenedToday() { return context.getString(R.string.what_happened_today); }
    public String getMemorySaved() { return context.getString(R.string.memory_saved); }
    public String getPhotoAdded() { return context.getString(R.string.photo_added); }
    public String getPhotoDeleted() { return context.getString(R.string.photo_deleted); }
    public String getLeafWithered() { return context.getString(R.string.leaf_withered); }
    public String getActiveLeafTitle() { return context.getString(R.string.active_leaf_title); }
    public String getLockedLeafTitle() { return context.getString(R.string.locked_leaf_title); }
    public String getWitheredLeafTitle() { return context.getString(R.string.withered_leaf_title); }
    public String getLockedMemoryInfo() { return context.getString(R.string.locked_memory_info); }
    public String getNote() { return context.getString(R.string.note); }
    public String getMissedThisDay() { return context.getString(R.string.missed_this_day); }
    public String getNoMemoryAdded() { return context.getString(R.string.no_memory_added); }
    public String getUnderstood() { return context.getString(R.string.understood); }
    public String getDeleteLeafTitle() { return context.getString(R.string.delete_leaf_title); }
    public String getDeleteConfirmation() { return context.getString(R.string.delete_confirmation); }
    public String getYesDelete() { return context.getString(R.string.yes_delete); }
    public String getAppName() { return context.getString(R.string.app_name); }
    public String getLanguageChanged() { return context.getString(R.string.language_changed); }
    public String getGoodMorning() { return context.getString(R.string.good_morning); }
    public String getGoodAfternoon() { return context.getString(R.string.good_afternoon); }
    public String getGoodEvening() { return context.getString(R.string.good_evening); }
    public String getPlantAMemory() { return context.getString(R.string.plant_a_memory); }
    public String getRecentMemories() { return context.getString(R.string.recent_memories); }
    public String getActiveStatus() { return context.getString(R.string.active_status); }
    public String getGrowingStatus() { return context.getString(R.string.growing_status); }
    public String getLockedStatus() { return context.getString(R.string.locked_status); }
    public String getWitheredStatus() { return context.getString(R.string.withered_status); }

    public String getGrowingLeafTitle() { return context.getString(R.string.growing_leaf_title); }
    public String getGrowingLeafInfo() { return context.getString(R.string.growing_leaf_info); }
    public String getWaitForMaturity() { return context.getString(R.string.wait_for_maturity); }

    public String getLeafStatus() { return context.getString(R.string.leaf_status_label); }
    public String getStreak() { return context.getString(R.string.streak); }
    public String getTotalMemoriesLabel() { return context.getString(R.string.total_memories_label); }
    public String getWordsWritten() { return context.getString(R.string.words_written); }
    public String getPreferences() { return context.getString(R.string.preferences); }
    public String getDarkModeLabel() { return context.getString(R.string.dark_mode); }
    public String getBiometricLabel() { return context.getString(R.string.biometric_lock); }
    public String getRemindersLabel() { return context.getString(R.string.reminders_label); }
    public String getFrequencyLabel() { return context.getString(R.string.frequency_label); }
    public String getGithubLabel() { return context.getString(R.string.github_repo); }
    public String getSupportDeveloper() { return context.getString(R.string.support_developer); }
    public String getAboutApp() { return context.getString(R.string.about_app); }
    public String getYourGarden() { return context.getString(R.string.your_garden); }
    public String getBackupRestore() { return context.getString(R.string.backup_restore); }
    public String getExportData() { return context.getString(R.string.export_data); }
    public String getImportData() { return context.getString(R.string.import_data); }
    public String getEnterPassword() { return context.getString(R.string.enter_password); }
    public String getBackupCreated() { return context.getString(R.string.backup_created); }
    public String getRestoreSuccess() { return context.getString(R.string.restore_success); }
    public String getInvalidPassword() { return context.getString(R.string.invalid_password); }
}
