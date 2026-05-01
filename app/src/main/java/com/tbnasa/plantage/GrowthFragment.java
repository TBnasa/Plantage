package com.tbnasa.plantage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tbnasa.plantage.model.Leaf;
import com.tbnasa.plantage.model.LeafStatus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * GrowthFragment — Statistics dashboard showing streak,
 * total memories, leaf status donut chart, and words written.
 */
public class GrowthFragment extends Fragment {

    private TextView tvStreakValue, tvTotalMemories, tvWordsWritten;
    private MoodChartView moodChart;
    private DatabaseHelper dbHelper;
    private LanguageManager lang;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_growth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        dbHelper = activity.getDbHelper();
        lang = activity.getLang();

        tvStreakValue = view.findViewById(R.id.tvStreakValue);
        tvTotalMemories = view.findViewById(R.id.tvTotalMemories);
        tvWordsWritten = view.findViewById(R.id.tvWordsWritten);
        moodChart = view.findViewById(R.id.moodChart);

        // i18n labels
        TextView tvGrowthTitle = view.findViewById(R.id.tvGrowthTitle);
        TextView tvGrowthSubtitle = view.findViewById(R.id.tvGrowthSubtitle);
        TextView tvStreakLabel = view.findViewById(R.id.tvStreakLabel);
        TextView tvMemoriesLabel = view.findViewById(R.id.tvMemoriesLabel);
        TextView tvWordsLabel = view.findViewById(R.id.tvWordsLabel);
        TextView tvLegendActive = view.findViewById(R.id.tvLegendActive);
        TextView tvLegendLocked = view.findViewById(R.id.tvLegendLocked);
        TextView tvLegendWithered = view.findViewById(R.id.tvLegendWithered);
        TextView tvLeafStatusLabel = view.findViewById(R.id.tvLeafStatusLabel);

        if (tvGrowthTitle != null)
            tvGrowthTitle.setText(lang.getStreak() + " 📊");
        if (tvStreakLabel != null)
            tvStreakLabel.setText(lang.getStreak() + " 🔥");
        if (tvMemoriesLabel != null)
            tvMemoriesLabel.setText(lang.getTotalMemoriesLabel() + " 🌿");
        if (tvWordsLabel != null)
            tvWordsLabel.setText(lang.getWordsWritten() + " ✍️");
        if (tvLegendActive != null)
            tvLegendActive.setText(lang.getActiveStatus());
        if (tvLegendLocked != null)
            tvLegendLocked.setText(lang.getLockedStatus());
        if (tvLegendWithered != null)
            tvLegendWithered.setText(lang.getWitheredStatus());
        if (tvLeafStatusLabel != null)
            tvLeafStatusLabel.setText(lang.getLeafStatus());

        loadStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        List<Leaf> leaves = dbHelper.getAllLeaves();

        // Total memories (leaves with content)
        int totalMemories = 0;
        int totalWords = 0;
        int activeCount = 0;
        int lockedCount = 0;
        int witheredCount = 0;

        for (Leaf leaf : leaves) {
            if (leaf.hasContent()) {
                totalMemories++;
                if (leaf.content != null) {
                    totalWords += leaf.content.trim().split("\\s+").length;
                }
            }

            switch (leaf.status) {
                case ACTIVE:
                    activeCount++;
                    break;
                case LOCKED:
                    lockedCount++;
                    break;
                case WITHERED:
                    witheredCount++;
                    break;
            }
        }

        tvTotalMemories.setText(String.valueOf(totalMemories));
        tvWordsWritten.setText(String.valueOf(totalWords));

        // Calculate streak
        int streak = calculateStreak(leaves);
        tvStreakValue.setText(String.valueOf(streak));

        // Update donut chart
        if (moodChart != null) {
            moodChart.setData(activeCount, lockedCount, witheredCount);
        }
    }

    /**
     * Calculates the current consecutive-day streak
     * (days with content, counting backwards from today).
     */
    private int calculateStreak(List<Leaf> leaves) {
        int streak = 0;
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 365; i++) { // Max 1 year lookback
            String dateStr = DATE_FORMAT.format(cal.getTime());
            boolean found = false;
            for (Leaf leaf : leaves) {
                if (dateStr.equals(leaf.date) && leaf.hasContent()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                streak++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }
}
