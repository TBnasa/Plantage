package com.tbnasa.plantage;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tbnasa.plantage.model.Leaf;

/**
 * NotificationWorker - Handles periodic reminders for journaling and breathing.
 */
public class NotificationWorker extends Worker {

    private static final String CHANNEL_ID = "plantage_reminders";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        LanguageManager lang = new LanguageManager(context);
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        
        // Check if today's memory is already planted
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        Leaf todayLeaf = dbHelper.getLeafByDate(today);

        if (todayLeaf == null || !todayLeaf.hasContent()) {
            sendNotification(
                "How was your day? 🌱",
                "Your garden misses you. Spend a moment to plant a memory."
            );
        } else {
            sendNotification(
                "Time for a Zen moment? 🌿",
                "Take a deep breath and relax with your garden."
            );
        }

        // Handle recursive scheduling for frequencies < 15 minutes
        int freq = lang.getReminderFrequency();
        if (freq < 15) {
            scheduleNextOneShot(context, freq);
        }

        return Result.success();
    }

    private void scheduleNextOneShot(Context context, int minutes) {
        androidx.work.OneTimeWorkRequest request = new androidx.work.OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(minutes, java.util.concurrent.TimeUnit.MINUTES)
                .addTag("garden_reminders_oneshot")
                .build();
        
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "garden_reminders_oneshot",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request);
    }

    private void sendNotification(String title, String message) {
        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Garden Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
