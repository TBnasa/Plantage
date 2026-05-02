package com.tbnasa.plantage;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tbnasa.plantage.model.Leaf;
import com.tbnasa.plantage.model.LeafStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TimelineFragment — Main tab showing the tree view, greeting,
 * countdown timer, and recent memory cards.
 */
public class TimelineFragment extends Fragment {

    private PlantageTreeView treeView;
    private TextView tvGreeting, tvDate, tvCountdown, tvStreakCounter;
    private LinearLayout cardPlantMemory, layoutMemoryCards, layoutCountdown;
    private View tvEmptyState;
    // Removed filtering fields

    private DatabaseHelper dbHelper;
    private LanguageManager lang;
    private CountDownTimer countdownTimer;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timeline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get shared services
        MainActivity activity = (MainActivity) requireActivity();
        dbHelper = activity.getDbHelper();
        lang = activity.getLang();

        // Bind views
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvDate = view.findViewById(R.id.tvDate);
        tvCountdown = view.findViewById(R.id.tvCountdown);
        layoutCountdown = view.findViewById(R.id.layoutCountdown);
        cardPlantMemory = view.findViewById(R.id.cardPlantMemory);
        layoutMemoryCards = view.findViewById(R.id.layoutMemoryCards);
        treeView = view.findViewById(R.id.plantageTreeView);
        tvStreakCounter = view.findViewById(R.id.tvStreakCounter);

        setupGreeting();
        setupCountdown();
        setupPlantMemoryCard();
        loadTreeData();
        loadRecentMemories();

        // Set i18n text
        TextView tvPlantHint = view.findViewById(R.id.tvPlantHint);
        if (tvPlantHint != null)
            tvPlantHint.setText(lang.getPlantAMemory());
        TextView tvRecentTitle = view.findViewById(R.id.tvRecentTitle);
        if (tvRecentTitle != null)
            tvRecentTitle.setText(lang.getRecentMemories());
    }

    private void setupGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = lang.getGoodMorning();
        } else if (hour < 17) {
            greeting = lang.getGoodAfternoon();
        } else {
            greeting = lang.getGoodEvening();
        }
        tvGreeting.setText(greeting);
        tvDate.setText(DISPLAY_DATE.format(new Date()));

        // Streak calculation
        List<Leaf> leaves = dbHelper.getAllLeaves();
        int streak = 0;
        Calendar streakCal = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            String dateStr = DATE_FORMAT.format(streakCal.getTime());
            boolean found = false;
            for (Leaf leaf : leaves) {
                if (dateStr.equals(leaf.date) && leaf.hasContent()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                streak++;
                streakCal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        if (tvStreakCounter != null) {
            tvStreakCounter.setText(String.valueOf(streak));
        }
    }

    private void setupCountdown() {
        // Next day midnight countdown
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        long remaining = next.getTimeInMillis() - System.currentTimeMillis();
        if (remaining <= 0)
            remaining = 1000;

        countdownTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = millisUntilFinished / 3_600_000;
                long mins = (millisUntilFinished % 3_600_000) / 60_000;
                long secs = (millisUntilFinished % 60_000) / 1000;
                tvCountdown.setText(String.format(Locale.getDefault(),
                        "%02d:%02d:%02d", hours, mins, secs));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("00:00:00");
                // Refresh the UI for the new day
                if (isAdded()) {
                    setupGreeting();
                    loadTreeData();
                    loadRecentMemories();
                    setupCountdown();
                }
            }
        }.start();
    }

    private void setupPlantMemoryCard() {
        cardPlantMemory.setOnClickListener(v -> {
            String today = DATE_FORMAT.format(new Date());
            Leaf leaf = dbHelper.getLeafByDate(today);
            if (leaf == null) {
                // Default creation (no category)
                long id = dbHelper.createLeaf(today);
                leaf = dbHelper.getLeafByDate(today);
            }
            if (leaf != null) {
                showLeafDialog(leaf);
            }
        });
    }

    private void loadTreeData() {
        if (treeView == null)
            return;
        List<Leaf> leaves = dbHelper.getAllLeaves();
        treeView.setLeaves(leaves);

        // Check / create today's leaf
        String today = DATE_FORMAT.format(new Date());
        
        // Check / create tomorrow's leaf
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = DATE_FORMAT.format(cal.getTime());

        boolean hasTodayLeaf = false;
        boolean hasTomorrowLeaf = false;

        for (Leaf l : leaves) {
            if (today.equals(l.date)) hasTodayLeaf = true;
            if (tomorrow.equals(l.date)) hasTomorrowLeaf = true;
        }

        boolean changed = false;
        if (!hasTodayLeaf) {
            dbHelper.createLeaf(today);
            changed = true;
        }
        if (!hasTomorrowLeaf) {
            dbHelper.createLeaf(tomorrow);
            changed = true;
        }

        if (changed) {
            treeView.setLeaves(dbHelper.getAllLeaves());
        }

        treeView.setOnLeafClickListener((index, leaf) -> showLeafDialog(leaf));
    }

    private void loadRecentMemories() {
        if (layoutMemoryCards == null)
            return;
        layoutMemoryCards.removeAllViews();

        List<Leaf> leaves = dbHelper.getAllLeaves();
        int count = 0;
        // Show all leaves with content, newest first
        for (int i = leaves.size() - 1; i >= 0; i--) {
            Leaf leaf = leaves.get(i);
            if (leaf.hasContent()) {
                View card = createMemoryCard(leaf);
                layoutMemoryCards.addView(card);
                count++;
            }
        }

        // Show empty state if no memories
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private View createMemoryCard(Leaf leaf) {
        Context ctx = requireContext();
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_zen);
        card.setElevation(4f);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        int marginBottom = (int) (8 * getResources().getDisplayMetrics().density);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = marginBottom;
        card.setLayoutParams(lp);

        // Date
        TextView tvDate = new TextView(ctx);
        tvDate.setText(leaf.date);
        tvDate.setTextColor(getResources().getColor(R.color.zen_accent, null));
        tvDate.setTextSize(13f);
        card.addView(tvDate);

        // Content preview
        if (leaf.content != null && !leaf.content.trim().isEmpty()) {
            TextView tvContent = new TextView(ctx);
            String preview = leaf.content.length() > 100
                    ? leaf.content.substring(0, 100) + "..."
                    : leaf.content;
            tvContent.setText(preview);
            tvContent.setTextColor(getResources().getColor(R.color.zen_text_secondary, null));
            tvContent.setTextSize(14f);
            tvContent.setMaxLines(3);
            tvContent.setEllipsize(TextUtils.TruncateAt.END);

            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tvLp.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
            tvContent.setLayoutParams(tvLp);
            card.addView(tvContent);
        }

        // Status badge
        TextView tvStatus = new TextView(ctx);
        String statusText;
        int statusColor;
        switch (leaf.status) {
            case GROWING:
                statusText = lang.getGrowingStatus();
                statusColor = R.color.zen_accent_light;
                break;
            case LOCKED:
                statusText = lang.getLockedStatus();
                statusColor = R.color.zen_accent_dark;
                break;
            case WITHERED:
                statusText = lang.getWitheredStatus();
                statusColor = R.color.leaf_withered;
                break;
            default:
                statusText = lang.getActiveStatus();
                statusColor = R.color.zen_accent;
                break;
        }
        tvStatus.setText(statusText);
        tvStatus.setTextColor(getResources().getColor(statusColor, null));
        tvStatus.setTextSize(12f);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
        tvStatus.setLayoutParams(sLp);
        card.addView(tvStatus);

        card.setOnClickListener(v -> showLeafDialog(leaf));
        return card;
    }

    /**
     * Shows the leaf detail / edit dialog with Zen styling.
     */
    private void showLeafDialog(Leaf leaf) {
        Context ctx = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.ZenDialogTheme);

        float density = getResources().getDisplayMetrics().density;

        // Title based on status
        String title;
        switch (leaf.status) {
            case GROWING:
                title = lang.getGrowingLeafTitle();
                break;
            case LOCKED:
                title = lang.getLockedLeafTitle();
                break;
            case WITHERED:
                title = lang.getWitheredLeafTitle();
                break;
            default:
                title = lang.getActiveLeafTitle();
                break;
        }
        builder.setTitle(title);

        // Custom view
        LinearLayout dialogLayout = new LinearLayout(ctx);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        dialogLayout.setPadding(pad, pad, pad, pad);

        // Date label
        TextView tvDialogDate = new TextView(ctx);
        tvDialogDate.setText("📅 " + leaf.date);
        tvDialogDate.setTextColor(getResources().getColor(R.color.zen_accent, null));
        tvDialogDate.setTextSize(14f);
        dialogLayout.addView(tvDialogDate);

        if (leaf.status == LeafStatus.GROWING) {
            // Growing - show info only
            TextView tvInfo = new TextView(ctx);
            tvInfo.setText("\n" + lang.getGrowingLeafInfo() + "\n\n" + lang.getWaitForMaturity());
            tvInfo.setTextColor(getResources().getColor(R.color.zen_text_secondary, null));
            tvInfo.setTextSize(14f);
            dialogLayout.addView(tvInfo);

            builder.setView(dialogLayout);
            builder.setPositiveButton(lang.getOk(), null);
        } else if (leaf.status == LeafStatus.WITHERED) {
            // Withered - show info only
            TextView tvInfo = new TextView(ctx);
            tvInfo.setText("\n" + lang.getMissedThisDay() + "\n" + lang.getNoMemoryAdded());
            tvInfo.setTextColor(getResources().getColor(R.color.zen_text_secondary, null));
            tvInfo.setTextSize(14f);
            dialogLayout.addView(tvInfo);

            builder.setView(dialogLayout);
            builder.setPositiveButton(lang.getUnderstood(), null);
        } else if (leaf.status == LeafStatus.LOCKED) {
            // Locked - show content read-only
            TextView tvLockedInfo = new TextView(ctx);
            tvLockedInfo.setText("\n" + lang.getLockedMemoryInfo());
            tvLockedInfo.setTextColor(getResources().getColor(R.color.zen_text_secondary, null));
            tvLockedInfo.setTextSize(14f);
            dialogLayout.addView(tvLockedInfo);

            if (leaf.content != null && !leaf.content.trim().isEmpty()) {
                TextView tvNote = new TextView(ctx);
                tvNote.setText("\n" + lang.getNote() + ":\n" + leaf.content);
                tvNote.setTextColor(getResources().getColor(R.color.zen_text_primary, null));
                tvNote.setTextSize(14f);
                dialogLayout.addView(tvNote);
            }

            // Show photos if any
            addPhotoViews(dialogLayout, leaf);

            builder.setView(dialogLayout);
            builder.setPositiveButton(lang.getClose(), null);
        } else {
            // ACTIVE leaf - editable
            TextView tvWriteLabel = new TextView(ctx);
            tvWriteLabel.setText("\n" + lang.getWriteMemory());
            tvWriteLabel.setTextColor(getResources().getColor(R.color.zen_text_primary, null));
            tvWriteLabel.setTextSize(14f);
            dialogLayout.addView(tvWriteLabel);

            EditText etContent = new EditText(ctx);
            etContent.setHint(lang.getWhatHappenedToday());
            etContent.setHintTextColor(getResources().getColor(R.color.zen_icon_inactive, null));
            etContent.setTextColor(getResources().getColor(R.color.zen_text_primary, null));
            etContent.setTextSize(14f);
            etContent.setMinLines(4);
            etContent.setBackgroundResource(R.drawable.bg_input_zen);
            int inputPad = (int) (12 * getResources().getDisplayMetrics().density);
            etContent.setPadding(inputPad, inputPad, inputPad, inputPad);
            if (leaf.content != null) {
                etContent.setText(leaf.content);
            }

            LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            etLp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            etContent.setLayoutParams(etLp);
            dialogLayout.addView(etContent);

            // Photo section
            addPhotoViews(dialogLayout, leaf);

            // Add photo button
            TextView btnAddPhoto = new TextView(ctx);
            btnAddPhoto.setText(lang.getAddPhoto());
            btnAddPhoto.setTextColor(getResources().getColor(R.color.zen_accent, null));
            btnAddPhoto.setTextSize(14f);
            LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            photoLp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
            btnAddPhoto.setPadding(0, (int)(8*density), 0, (int)(8*density));
            btnAddPhoto.setLayoutParams(photoLp);
            btnAddPhoto.setOnClickListener(v -> {
                MainActivity activity = (MainActivity) requireActivity();
                activity.launchImagePicker(leaf.id);
            });
            dialogLayout.addView(btnAddPhoto);

            // Wrap in ScrollView
            android.widget.ScrollView scrollView = new android.widget.ScrollView(ctx);
            scrollView.addView(dialogLayout);
            builder.setView(scrollView);

            builder.setPositiveButton(lang.getSave(), (dialog, which) -> {
                String newContent = etContent.getText().toString().trim();
                dbHelper.updateLeafContent(leaf.id, newContent);
                leaf.content = newContent;
                Toast.makeText(ctx, lang.getMemorySaved(), Toast.LENGTH_SHORT).show();
                PlantageWidgetProvider.refreshAllWidgets(ctx);
                loadTreeData();
                loadRecentMemories();
            });

            builder.setNegativeButton(lang.getCancel(), null);
        }

        builder.show();
    }

    private void addPhotoViews(LinearLayout parent, Leaf leaf) {
        String[] images = leaf.getAllImages();
        if (images.length == 0)
            return;

        Context ctx = requireContext();
        float density = getResources().getDisplayMetrics().density;

        TextView tvPhotos = new TextView(ctx);
        tvPhotos.setText("\n" + lang.getPhotos());
        tvPhotos.setTextColor(getResources().getColor(R.color.zen_text_primary, null));
        tvPhotos.setTextSize(14f);
        parent.addView(tvPhotos);

        for (String path : images) {
            if (path == null || path.trim().isEmpty())
                continue;
            File file = new File(path.trim());
            if (!file.exists())
                continue;

            ImageView iv = new ImageView(ctx);
            try {
                Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bm != null) {
                    iv.setImageBitmap(bm);
                    LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) (180 * density));
                    ivLp.topMargin = (int) (8 * density);
                    iv.setLayoutParams(ivLp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setClipToOutline(true);
                    parent.addView(iv);
                }
            } catch (Exception e) {
                // Skip broken images
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
    }

    /**
     * Called from MainActivity after image is picked.
     */
    public void onImagePicked(long leafId, Uri imageUri) {
        try {
            Context ctx = requireContext();
            InputStream is = ctx.getContentResolver().openInputStream(imageUri);
            if (is == null)
                return;

            File dir = new File(ctx.getFilesDir(), "leaf_images");
            if (!dir.exists())
                dir.mkdirs();

            String fileName = "leaf_" + leafId + "_" + System.currentTimeMillis() + ".jpg";
            File outFile = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(outFile);

            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.close();
            is.close();

            Leaf leaf = dbHelper.getLeafByDate(DATE_FORMAT.format(new Date()));
            if (leaf != null && leaf.id == leafId) {
                String newPaths = leaf.addImagePath(outFile.getAbsolutePath());
                dbHelper.updateLeafImages(leafId, newPaths);
                Toast.makeText(ctx, lang.getPhotoAdded(), Toast.LENGTH_SHORT).show();
                PlantageWidgetProvider.refreshAllWidgets(ctx);
                loadTreeData();
                loadRecentMemories();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
