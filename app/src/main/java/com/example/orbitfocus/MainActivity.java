package com.example.orbitfocus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.graphics.Color;
import java.util.List;
import java.util.ArrayList;
import android.os.Handler;
import android.os.Looper;
import com.example.orbitfocus.model.Leaf;
import com.example.orbitfocus.model.LeafStatus;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * MainActivity - Plantage ana ekranı.
 * Fraktal ağaç görünümü ve yaprak yönetimi.
 */
public class MainActivity extends AppCompatActivity {

    private PlantageTreeView treeView;
    private DatabaseHelper dbHelper;
    private LanguageManager lang;
    private TextView tvCountdown;
    private TextView tvTimer;
    private Handler handler = new Handler(Looper.getMainLooper());

    // MusicService bağlantısı
    private MusicService musicService;
    private boolean musicBound = false;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Leaf currentEditingLeaf;
    private LinearLayout currentPhotoContainer;

    // Service bağlantı callback'i
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        lang = new LanguageManager(this);

        treeView = findViewById(R.id.plantageTreeView);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvTimer = findViewById(R.id.tvTimer);

        // Update countdown label based on language
        if (tvCountdown != null) {
            tvCountdown.setText(lang.getNextLeaf());
        }

        // Image picker setup
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && currentEditingLeaf != null) {
                            long timestamp = System.currentTimeMillis();
                            String savedPath = saveImageToInternal(uri, currentEditingLeaf.id, timestamp);
                            if (savedPath != null) {
                                String newPaths = currentEditingLeaf.addImagePath(savedPath);
                                dbHelper.updateLeafImages(currentEditingLeaf.id, newPaths);
                                currentEditingLeaf.imagePaths = newPaths;

                                if (currentPhotoContainer != null) {
                                    addPhotoToContainer(savedPath);
                                }

                                loadLeaves();
                                Toast.makeText(this, lang.getPhotoAdded(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // Leaf click listener
        treeView.setOnLeafClickListener((index, leaf) -> {
            showLeafDialog(leaf);
        });

        // Settings button
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsDialog());
        }

        // Support button - Buy Me a Coffee
        setupSupportButton();

        ensureTodayLeafExists();
        startCountdownTimer();
        startMusicService();
    }

    /**
     * Support butonunu ayarlar - Buy Me a Coffee linki.
     */
    private void setupSupportButton() {
        View supportButton = findViewById(R.id.supportButton);
        TextView tvSupport = findViewById(R.id.tvSupport);

        // Update text based on language
        if (tvSupport != null) {
            tvSupport.setText(lang.getSupport());
        }

        // Click to open Buy Me a Coffee
        if (supportButton != null) {
            supportButton.setOnClickListener(v -> {
                String url = "https://buymeacoffee.com/plantage_";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            });
        }
    }

    /**
     * MusicService'i başlatır.
     */
    private void startMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, musicConnection, Context.BIND_AUTO_CREATE);
    }

    private String saveImageToInternal(Uri uri, long leafId, long timestamp) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File dir = new File(getFilesDir(), "leaves");
            if (!dir.exists())
                dir.mkdirs();

            File file = new File(dir, "leaf_" + leafId + "_" + timestamp + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteImage(String path) {
        try {
            File file = new File(path);
            if (file.exists())
                file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Bugün için yaprak yoksa oluşturur.
     */
    private void ensureTodayLeafExists() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        List<Leaf> leaves = dbHelper.getAllLeaves();
        if (leaves.isEmpty()) {
            dbHelper.createLeaf(today);
        } else {
            // Son yaprağın tarihini al
            Leaf lastLeaf = leaves.get(leaves.size() - 1);
            try {
                Date lastDate = sdf.parse(lastLeaf.date);
                Date todayDate = sdf.parse(today);

                Calendar cal = Calendar.getInstance();
                cal.setTime(lastDate);

                // Son yaprağın ertesi gününden başlayarak bugüne kadar (bugün dahil) eksikleri
                // tamamla
                cal.add(Calendar.DAY_OF_YEAR, 1);

                while (!cal.getTime().after(todayDate)) {
                    String dateStr = sdf.format(cal.getTime());
                    // Zaten varsa (CONFLICT_IGNORE sayesinde) sorun olmaz, yoksa oluşturulur
                    dbHelper.createLeaf(dateStr);
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Hata durumunda en azından bugünü oluştur
                dbHelper.createLeaf(today);
            }
        }

        loadLeaves();
    }

    private void loadLeaves() {
        List<Leaf> leaves = dbHelper.getAllLeaves();
        treeView.setLeaves(leaves);
    }

    /**
     * Yaprak detay/düzenleme dialogu gösterir.
     * Duruma göre farklı davranışlar sergiler.
     */
    private void showLeafDialog(Leaf leaf) {
        currentEditingLeaf = leaf;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this,
                android.R.style.Theme_Material_Light_Dialog);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 50, 50, 50);
        mainLayout.setBackgroundColor(0xFFFCFCFA);

        // Title with status
        TextView tvTitle = new TextView(this);
        String statusEmoji = getStatusEmoji(leaf.status);
        tvTitle.setText(statusEmoji + " " + leaf.date);
        tvTitle.setTextSize(22f);
        tvTitle.setTextColor(getStatusColor(leaf.status));
        tvTitle.setPadding(0, 0, 0, 20);
        mainLayout.addView(tvTitle);

        // Handle based on status
        switch (leaf.status) {
            case WITHERED:
                showWitheredLeafContent(mainLayout, leaf, builder);
                break;
            case LOCKED:
                showLockedLeafContent(mainLayout, leaf, builder);
                break;
            case ACTIVE:
            default:
                showActiveLeafContent(mainLayout, leaf, builder);
                break;
        }
    }

    /**
     * WITHERED (Kurumuş) yaprak görünümü - Sadece uyarı mesajı.
     */
    private void showWitheredLeafContent(LinearLayout layout, Leaf leaf,
            android.app.AlertDialog.Builder builder) {
        TextView tvWarning = new TextView(this);
        String witheredMsg = lang.isTurkish()
                ? "😔 Bu günü kaçırdın.\n\nNe yazık ki bu güne artık anı ekleyemezsin. Ama endişelenme, yarın yeni bir yaprak seni bekliyor!"
                : "😔 You missed this day.\n\nUnfortunately, you can no longer add a memory for this day. But don't worry, a new leaf awaits you tomorrow!";
        tvWarning.setText(witheredMsg);
        tvWarning.setTextSize(16f);
        tvWarning.setTextColor(0xFF666666);
        tvWarning.setLineSpacing(0, 1.4f);
        tvWarning.setPadding(0, 30, 0, 30);
        layout.addView(tvWarning);

        // Custom Button - Dark Gray
        Button btnOk = new Button(this);
        btnOk.setText(lang.getUnderstood());
        btnOk.setBackgroundColor(0xFF444444);
        btnOk.setTextColor(0xFFFFFFFF);
        btnOk.setTextSize(14f);
        btnOk.setAllCaps(false);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 10, 0, 0);
        btnOk.setLayoutParams(btnParams);
        layout.addView(btnOk);

        builder.setView(layout);
        android.app.AlertDialog dialog = builder.create();

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * LOCKED (Kilitli) yaprak görünümü - Salt okunur.
     */
    private void showLockedLeafContent(LinearLayout layout, Leaf leaf,
            android.app.AlertDialog.Builder builder) {
        // Info text
        TextView tvInfo = new TextView(this);
        tvInfo.setText(lang.getLockedMemoryInfo());
        tvInfo.setTextSize(14f);
        tvInfo.setTextColor(0xFF888888);
        tvInfo.setPadding(0, 0, 0, 20);
        layout.addView(tvInfo);

        // Photos (read-only)
        if (leaf.imagePaths != null && !leaf.imagePaths.isEmpty()) {
            TextView tvPhotos = new TextView(this);
            tvPhotos.setText(lang.getPhotos());
            tvPhotos.setTextSize(14f);
            tvPhotos.setTextColor(0xFF666666);
            layout.addView(tvPhotos);

            HorizontalScrollView photoScroll = new HorizontalScrollView(this);
            photoScroll.setHorizontalScrollBarEnabled(false);

            LinearLayout photoContainer = new LinearLayout(this);
            photoContainer.setOrientation(LinearLayout.HORIZONTAL);
            photoContainer.setPadding(0, 15, 0, 20);

            String[] photos = leaf.getAllImages();
            for (String path : photos) {
                if (path != null && !path.isEmpty()) {
                    ImageView img = createPhotoView(path, false);
                    photoContainer.addView(img);
                }
            }

            photoScroll.addView(photoContainer);
            layout.addView(photoScroll);
        }

        // Content (read-only)
        if (leaf.content != null && !leaf.content.isEmpty()) {
            TextView tvNoteLabel = new TextView(this);
            tvNoteLabel.setText(lang.getNote());
            tvNoteLabel.setTextSize(14f);
            tvNoteLabel.setTextColor(0xFF666666);
            tvNoteLabel.setPadding(0, 10, 0, 5);
            layout.addView(tvNoteLabel);

            TextView tvContent = new TextView(this);
            tvContent.setText(leaf.content);
            tvContent.setTextSize(15f);
            tvContent.setTextColor(0xFF444444);
            tvContent.setBackgroundColor(0xFFF5F5F5);
            tvContent.setPadding(25, 25, 25, 25);
            tvContent.setLineSpacing(0, 1.3f);
            layout.addView(tvContent);
        }
        // Custom Close Button - Black
        Button btnClose = new Button(this);
        btnClose.setText(lang.getClose());
        btnClose.setBackgroundColor(0xFF333333);
        btnClose.setTextColor(0xFFFFFFFF);
        btnClose.setTextSize(14f);
        btnClose.setAllCaps(false);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.setMargins(0, 25, 0, 0);
        btnClose.setLayoutParams(closeParams);
        layout.addView(btnClose);

        builder.setView(layout);
        android.app.AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * ACTIVE (Canlı) yaprak görünümü - Düzenlenebilir.
     */
    private void showActiveLeafContent(LinearLayout layout, Leaf leaf,
            android.app.AlertDialog.Builder builder) {
        // Photos section
        TextView tvPhotos = new TextView(this);
        tvPhotos.setText(lang.getPhotos());
        tvPhotos.setTextSize(14f);
        tvPhotos.setTextColor(0xFF666666);
        layout.addView(tvPhotos);

        HorizontalScrollView photoScroll = new HorizontalScrollView(this);
        photoScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout photoContainer = new LinearLayout(this);
        photoContainer.setOrientation(LinearLayout.HORIZONTAL);
        photoContainer.setPadding(0, 15, 0, 15);
        currentPhotoContainer = photoContainer;

        String[] existingPhotos = leaf.getAllImages();
        for (String path : existingPhotos) {
            if (path != null && !path.isEmpty()) {
                addPhotoToContainer(path);
            }
        }

        photoScroll.addView(photoContainer);
        layout.addView(photoScroll);

        // Add photo button
        Button btnPhoto = new Button(this);
        btnPhoto.setText(lang.getAddPhoto());
        btnPhoto.setBackgroundColor(0xFF4CAF50);
        btnPhoto.setTextColor(0xFFFFFFFF);
        btnPhoto.setTextSize(14f);
        btnPhoto.setAllCaps(false);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 5, 0, 20);
        btnPhoto.setLayoutParams(btnParams);
        btnPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
        layout.addView(btnPhoto);

        // Note section
        TextView tvNote = new TextView(this);
        tvNote.setText(lang.getWriteMemory());
        tvNote.setTextSize(14f);
        tvNote.setTextColor(0xFF666666);
        layout.addView(tvNote);

        EditText etContent = new EditText(this);
        etContent.setHint(lang.getWhatHappenedToday());
        etContent.setHintTextColor(0xFFCCCCCC);
        etContent.setText(leaf.content != null ? leaf.content : "");
        etContent.setMinLines(4);
        etContent.setTextSize(15f);
        etContent.setTextColor(0xFF333333);
        etContent.setBackgroundColor(0xFFFFFFFF);
        etContent.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        etParams.setMargins(0, 10, 0, 20);
        etContent.setLayoutParams(etParams);
        layout.addView(etContent);

        // Custom Buttons Container
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams buttonContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonContainerParams.setMargins(0, 20, 0, 0);
        buttonContainer.setLayoutParams(buttonContainerParams);

        // Cancel Button - Dark Gray
        Button btnCancel = new Button(this);
        btnCancel.setText(lang.getCancel());
        btnCancel.setBackgroundColor(0xFF555555);
        btnCancel.setTextColor(0xFFFFFFFF);
        btnCancel.setTextSize(14f);
        btnCancel.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cancelParams.setMargins(0, 0, 5, 0);
        btnCancel.setLayoutParams(cancelParams);
        buttonContainer.addView(btnCancel);

        // Delete Button - Red
        Button btnDelete = new Button(this);
        btnDelete.setText(lang.getDelete());
        btnDelete.setBackgroundColor(0xFFB71C1C);
        btnDelete.setTextColor(0xFFFFFFFF);
        btnDelete.setTextSize(14f);
        btnDelete.setAllCaps(false);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        deleteParams.setMargins(5, 0, 5, 0);
        btnDelete.setLayoutParams(deleteParams);
        buttonContainer.addView(btnDelete);

        // Save Button - Black
        Button btnSave = new Button(this);
        btnSave.setText(lang.getSave());
        btnSave.setBackgroundColor(0xFF222222);
        btnSave.setTextColor(0xFFFFFFFF);
        btnSave.setTextSize(14f);
        btnSave.setAllCaps(false);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        saveParams.setMargins(5, 0, 0, 0);
        btnSave.setLayoutParams(saveParams);
        buttonContainer.addView(btnSave);

        layout.addView(buttonContainer);

        builder.setView(layout);

        android.app.AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            // Silme onayı iste
            android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(this,
                    android.R.style.Theme_Material_Light_Dialog)
                    .setTitle(lang.getDeleteLeafTitle())
                    .setMessage(lang.getDeleteConfirmation())
                    .setPositiveButton(lang.getYesDelete(), (d, w) -> {
                        // Yaprağı WITHERED olarak işaretle
                        dbHelper.updateLeafStatus(leaf.id, LeafStatus.WITHERED);
                        loadLeaves();
                        Toast.makeText(this, lang.getLeafWithered(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton(lang.getCancel(), null)
                    .create();

            // Butonları siyah arka planlı yap
            confirmDialog.setOnShowListener(d -> {
                confirmDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLACK);
                confirmDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                confirmDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.BLACK);
                confirmDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
            });

            confirmDialog.show();
        });

        btnSave.setOnClickListener(v -> {
            String newContent = etContent.getText().toString();
            dbHelper.updateLeafContent(leaf.id, newContent);
            loadLeaves();
            Toast.makeText(this, lang.getMemorySaved(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addPhotoToContainer(String path) {
        if (currentPhotoContainer == null)
            return;

        LinearLayout imgContainer = new LinearLayout(this);
        imgContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        containerParams.setMargins(0, 0, 12, 0);
        imgContainer.setLayoutParams(containerParams);

        ImageView img = createPhotoView(path, true);
        imgContainer.addView(img);

        // Delete button
        TextView btnDelete = new TextView(this);
        btnDelete.setText("Sil");
        btnDelete.setTextColor(0xFFCC6666);
        btnDelete.setTextSize(12f);
        btnDelete.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        btnDelete.setPadding(0, 8, 0, 0);
        btnDelete.setOnClickListener(v -> {
            String currentPaths = currentEditingLeaf.imagePaths;
            if (currentPaths != null) {
                String[] paths = currentPaths.split(",");
                List<String> newPaths = new ArrayList<>();
                for (String p : paths) {
                    if (!p.equals(path))
                        newPaths.add(p);
                }
                String updatedPaths = String.join(",", newPaths);
                dbHelper.updateLeafImages(currentEditingLeaf.id, updatedPaths);
                currentEditingLeaf.imagePaths = updatedPaths;

                deleteImage(path);
                currentPhotoContainer.removeView(imgContainer);
                loadLeaves();
                Toast.makeText(this, "Fotoğraf silindi", Toast.LENGTH_SHORT).show();
            }
        });
        imgContainer.addView(btnDelete);

        currentPhotoContainer.addView(imgContainer);
    }

    private ImageView createPhotoView(String path, boolean clickable) {
        ImageView img = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(150, 150);
        img.setLayoutParams(imgParams);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(0xFFF0F0F0);

        File file = new File(path);
        if (file.exists()) {
            img.setImageURI(Uri.fromFile(file));
        }

        if (clickable) {
            img.setOnClickListener(v -> showFullImage(path));
        }

        return img;
    }

    private void showFullImage(String path) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        ImageView fullImg = new ImageView(this);
        fullImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullImg.setAdjustViewBounds(true);
        fullImg.setBackgroundColor(Color.BLACK);

        File file = new File(path);
        if (file.exists()) {
            fullImg.setImageURI(Uri.fromFile(file));
        }

        builder.setView(fullImg);
        builder.setPositiveButton("Kapat", null);
        builder.show();
    }

    private String getStatusEmoji(LeafStatus status) {
        switch (status) {
            case LOCKED:
                return "🔒";
            case WITHERED:
                return "🍂";
            case ACTIVE:
            default:
                return "🌿";
        }
    }

    private int getStatusColor(LeafStatus status) {
        switch (status) {
            case LOCKED:
                return 0xFF66BB6A;
            case WITHERED:
                return 0xFF8B6914;
            case ACTIVE:
            default:
                return 0xFF4CAF50;
        }
    }

    // Timer Logic
    private void startCountdownTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateCountdown();
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void updateCountdown() {
        Calendar now = Calendar.getInstance();
        Calendar nextMidnight = Calendar.getInstance();
        // Bir sonraki gün 00:00
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 0);
        nextMidnight.set(Calendar.SECOND, 0);

        // Eğer şu an (örn 00:30) midnight'ı geçtiyse, hedef yarın 00:00 olmalı
        // Ama "nextMidnight" şu anı (bugün 00:00) gösteriyorsa ve now ondan büyükse, +1
        // gün ekle
        if (now.after(nextMidnight)) {
            nextMidnight.add(Calendar.DAY_OF_YEAR, 1);
        }

        long diff = nextMidnight.getTimeInMillis() - now.getTimeInMillis();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                hours % 24, minutes % 60, seconds % 60);
        if (tvTimer != null) {
            tvTimer.setText(time);
        }
    }

    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this,
                android.R.style.Theme_Material_Light_Dialog);
        builder.setTitle(lang.getSettings());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 50, 60, 50);
        layout.setBackgroundColor(0xFFFAFAFA);

        // ===================== MUSIC VOLUME =====================
        TextView tvVolume = new TextView(this);
        tvVolume.setText(lang.getMusicVolume());
        tvVolume.setTextSize(15f);
        tvVolume.setTextColor(0xFF333333);
        tvVolume.setPadding(0, 0, 0, 8);
        layout.addView(tvVolume);

        android.widget.SeekBar volumeSeek = new android.widget.SeekBar(this);
        volumeSeek.setMax(100);
        int currentProgress = musicBound && musicService != null
                ? (int) (musicService.getVolume() * 100)
                : 35;
        volumeSeek.setProgress(currentProgress);
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        seekParams.setMargins(0, 8, 0, 30);
        volumeSeek.setLayoutParams(seekParams);
        layout.addView(volumeSeek);

        volumeSeek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (musicBound && musicService != null) {
                    float vol = progress / 100f;
                    musicService.setVolume(vol);
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
            }
        });

        // ===================== LANGUAGE =====================
        TextView tvLang = new TextView(this);
        tvLang.setText(lang.getLanguage_());
        tvLang.setTextSize(15f);
        tvLang.setTextColor(0xFF333333);
        tvLang.setPadding(0, 0, 0, 12);
        layout.addView(tvLang);

        // Language buttons - Row 1 (TR, EN, RU)
        LinearLayout langRow1 = new LinearLayout(this);
        langRow1.setOrientation(LinearLayout.HORIZONTAL);
        langRow1.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams row1Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        row1Params.setMargins(0, 0, 0, 8);
        langRow1.setLayoutParams(row1Params);

        String currentLang = lang.getLanguage();

        Button btnTR = createLangButton("🇹🇷 TR", LanguageManager.LANG_TR.equals(currentLang));
        Button btnEN = createLangButton("🇬🇧 EN", LanguageManager.LANG_EN.equals(currentLang));
        Button btnRU = createLangButton("🇷🇺 RU", LanguageManager.LANG_RU.equals(currentLang));

        langRow1.addView(btnTR);
        langRow1.addView(btnEN);
        langRow1.addView(btnRU);
        layout.addView(langRow1);

        // Language buttons - Row 2 (ZH, DE, FR)
        LinearLayout langRow2 = new LinearLayout(this);
        langRow2.setOrientation(LinearLayout.HORIZONTAL);
        langRow2.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams row2Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        row2Params.setMargins(0, 0, 0, 20);
        langRow2.setLayoutParams(row2Params);

        Button btnZH = createLangButton("🇨🇳 中文", LanguageManager.LANG_ZH.equals(currentLang));
        Button btnDE = createLangButton("🇩🇪 DE", LanguageManager.LANG_DE.equals(currentLang));
        Button btnFR = createLangButton("🇫🇷 FR", LanguageManager.LANG_FR.equals(currentLang));

        langRow2.addView(btnZH);
        langRow2.addView(btnDE);
        langRow2.addView(btnFR);
        layout.addView(langRow2);

        builder.setView(layout);

        android.app.AlertDialog dialog = builder.create();

        // Language button click handlers
        btnTR.setOnClickListener(v -> {
            setLanguageAndRestart(LanguageManager.LANG_TR, dialog);
        });
        btnEN.setOnClickListener(v -> {
            setLanguageAndRestart(LanguageManager.LANG_EN, dialog);
        });
        btnRU.setOnClickListener(v -> {
            setLanguageAndRestart(LanguageManager.LANG_RU, dialog);
        });
        btnZH.setOnClickListener(v -> {
            setLanguageAndRestart(LanguageManager.LANG_ZH, dialog);
        });
        btnDE.setOnClickListener(v -> {
            setLanguageAndRestart(LanguageManager.LANG_DE, dialog);
        });
        btnFR.setOnClickListener(v -> {
            setLanguageAndRestart(LanguageManager.LANG_FR, dialog);
        });

        dialog.show();
    }

    private Button createLangButton(String text, boolean isSelected) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11f);
        btn.setAllCaps(false);
        btn.setBackgroundColor(isSelected ? 0xFF4CAF50 : 0xFFE0E0E0);
        btn.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF333333);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(3, 0, 3, 0);
        btn.setLayoutParams(params);
        return btn;
    }

    private void setLanguageAndRestart(String langCode, android.app.AlertDialog dialog) {
        if (!langCode.equals(lang.getLanguage())) {
            lang.setLanguage(langCode);
            Toast.makeText(this, lang.getLanguageChanged(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            recreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // MusicService devam ettiğinde müzik de devam eder
        if (musicBound && musicService != null) {
            musicService.resume();
        }
        ensureTodayLeafExists();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Uygulama arka plana atılınca müziği duraklat
        if (musicBound && musicService != null) {
            musicService.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Service bağlantısını kes
        if (musicBound) {
            unbindService(musicConnection);
            musicBound = false;
        }
        // Uygulama kapanınca servisi de durdur
        Intent intent = new Intent(this, MusicService.class);
        stopService(intent);
    }
}
