package com.tbnasa.plantage;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.widget.EditText;
import android.net.Uri;
import android.content.Intent;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * SettingsFragment — Profile, volume slider, language selector,
 * dark mode, biometric lock, and about info.
 */
public class SettingsFragment extends Fragment {

    private SeekBar seekVolume, seekFrequency;
    private TextView tvVolumeValue, tvFrequencyValue, tvCurrentLang;
    private SwitchCompat switchDarkMode, switchBiometric;
    private LanguageManager lang;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        lang = activity.getLang();

        seekVolume = view.findViewById(R.id.seekVolume);
        seekFrequency = view.findViewById(R.id.seekFrequency);
        tvVolumeValue = view.findViewById(R.id.tvVolumeValue);
        tvFrequencyValue = view.findViewById(R.id.tvFrequencyValue);
        tvCurrentLang = view.findViewById(R.id.tvCurrentLang);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchBiometric = view.findViewById(R.id.switchBiometric);

        setupVolumeSlider(activity);
        setupFrequencySlider();
        setupLanguageRow(view);
        setupFeatureSwitches();
        updateLanguageDisplay();

        // i18n labels
        TextView tvPrefsLabel = view.findViewById(R.id.tvPrefsLabel);
        TextView tvVolumeLabel = view.findViewById(R.id.tvVolumeLabel);
        TextView tvLanguageLabel = view.findViewById(R.id.tvLanguageLabel);
        TextView tvAboutLabel = view.findViewById(R.id.tvAboutLabel);
        TextView tvSettingsSubtitle = view.findViewById(R.id.tvSettingsSubtitle);
        TextView tvDarkModeLabel = view.findViewById(R.id.tvDarkModeLabel);
        TextView tvBiometricLabel = view.findViewById(R.id.tvBiometricLabel);
        TextView tvFrequencyLabel = view.findViewById(R.id.tvFrequencyLabel);
        TextView tvGithubLabel = view.findViewById(R.id.tvGithubLabel);

        if (tvPrefsLabel != null) tvPrefsLabel.setText(lang.getPreferences());
        if (tvVolumeLabel != null) tvVolumeLabel.setText(lang.getMusicVolume());
        if (tvLanguageLabel != null) tvLanguageLabel.setText(lang.getLanguage_());
        if (tvAboutLabel != null) tvAboutLabel.setText(lang.getAboutApp());
        if (tvSettingsSubtitle != null) tvSettingsSubtitle.setText(lang.getYourGarden());
        if (tvDarkModeLabel != null) tvDarkModeLabel.setText(lang.getDarkModeLabel());
        if (tvBiometricLabel != null) tvBiometricLabel.setText(lang.getBiometricLabel());
        if (tvFrequencyLabel != null) tvFrequencyLabel.setText(lang.getFrequencyLabel());
        if (tvGithubLabel != null) tvGithubLabel.setText(lang.getGithubLabel());

        setupBackupSection(view);

        LinearLayout rowGithub = view.findViewById(R.id.rowGithub);
        if (rowGithub != null) {
            rowGithub.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/TBnasa/Plantage"));
                startActivity(intent);
            });
        }
    }

    private void setupVolumeSlider(MainActivity activity) {
        MusicService musicService = activity.getMusicService();
        if (musicService != null) {
            int vol = (int) (musicService.getVolume() * 100);
            seekVolume.setProgress(vol);
            tvVolumeValue.setText(vol + "%");
        }

        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVolumeValue.setText(progress + "%");
                if (fromUser) {
                    MusicService svc = activity.getMusicService();
                    if (svc != null) {
                        svc.setVolume(progress / 100f);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupLanguageRow(View root) {
        LinearLayout rowLanguage = root.findViewById(R.id.rowLanguage);
        rowLanguage.setOnClickListener(v -> showLanguageDialog());
    }

    private void setupFrequencySlider() {
        int freq = lang.getReminderFrequency();
        seekFrequency.setProgress(freq);
        updateFrequencyText(freq);

        seekFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) progress = 1;
                updateFrequencyText(progress);
                if (fromUser) {
                    lang.setReminderFrequency(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateFrequencyText(int minutes) {
        if (minutes < 60) {
            tvFrequencyValue.setText(minutes + lang.getFreqMinutes());
        } else {
            int h = minutes / 60;
            int m = minutes % 60;
            if (m == 0) tvFrequencyValue.setText(h + lang.getFreqHours());
            else tvFrequencyValue.setText(h + lang.getFreqHours() + " " + m + lang.getFreqMinutes());
        }
    }

    private void setupFeatureSwitches() {
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(lang.isDarkMode());
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                lang.setDarkMode(isChecked);
            });
        }

        if (switchBiometric != null) {
            switchBiometric.setChecked(lang.isBiometricEnabled());
            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                lang.setBiometricEnabled(isChecked);
            });
        }
    }

    private void showLanguageDialog() {
        String[] languages = { "Türkçe", "English", "Русский", "中文", "Deutsch", "Français" };
        String[] codes = {
                LanguageManager.LANG_TR,
                LanguageManager.LANG_EN,
                LanguageManager.LANG_RU,
                LanguageManager.LANG_ZH,
                LanguageManager.LANG_DE,
                LanguageManager.LANG_FR
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(lang.getLanguage_())
                .setItems(languages, (dialog, which) -> {
                    lang.setLanguage(codes[which]);
                    updateLanguageDisplay();
                    Toast.makeText(requireContext(),
                            lang.getLanguageChanged(), Toast.LENGTH_SHORT).show();
                    requireActivity().recreate();
                })
                .setNegativeButton(lang.getCancel(), null)
                .show();
    }

    private void updateLanguageDisplay() {
        String currentLang = lang.getLanguage();
        switch (currentLang) {
            case LanguageManager.LANG_TR: tvCurrentLang.setText("Türkçe"); break;
            case LanguageManager.LANG_RU: tvCurrentLang.setText("Русский"); break;
            case LanguageManager.LANG_ZH: tvCurrentLang.setText("中文"); break;
            case LanguageManager.LANG_DE: tvCurrentLang.setText("Deutsch"); break;
            case LanguageManager.LANG_FR: tvCurrentLang.setText("Français"); break;
            default: tvCurrentLang.setText("English"); break;
        }
    }

    private void setupBackupSection(View view) {
        TextView tvBackupLabel = view.findViewById(R.id.tvBackupLabel);
        TextView tvExportLabel = view.findViewById(R.id.tvExportLabel);
        TextView tvImportLabel = view.findViewById(R.id.tvImportLabel);

        if (tvBackupLabel != null) tvBackupLabel.setText(lang.getBackupRestore());
        if (tvExportLabel != null) tvExportLabel.setText(lang.getExportData());
        if (tvImportLabel != null) tvImportLabel.setText(lang.getImportData());

        LinearLayout rowExport = view.findViewById(R.id.rowExport);
        LinearLayout rowImport = view.findViewById(R.id.rowImport);

        rowExport.setOnClickListener(v -> showPasswordDialog(true, null));
        rowImport.setOnClickListener(v -> ((MainActivity)requireActivity()).launchFileImport());
    }

    private void showPasswordDialog(boolean isExport, Uri importUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.ZenDialogTheme);
        builder.setTitle(lang.getEnterPassword());

        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("****");
        input.setTextColor(getResources().getColor(R.color.colorTextPrimary, null));
        input.setHintTextColor(getResources().getColor(R.color.colorTextHint, null));
        input.setBackgroundResource(R.drawable.bg_input_zen);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        lp.setMargins(margin, margin/2, margin, margin);
        input.setLayoutParams(lp);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(lang.getOk(), (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) return;

            if (isExport) {
                startExportProcess(password);
            } else {
                startImportProcess(importUri, password);
            }
        });
        builder.setNegativeButton(lang.getCancel(), null);
        builder.show();
    }

    private void startExportProcess(String password) {
        MainActivity activity = (MainActivity) requireActivity();
        activity.getBackupManager().exportData(password, new BackupManager.BackupListener() {
            @Override
            public void onProgress(String message) {
                activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onSuccess(File file) {
                activity.runOnUiThread(() -> activity.launchFileExport("Plantage_Backup.plntg"));
                // We'll handle the actual copy in onExportLocationSelected
                tempBackupFile = file;
            }

            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Error: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private File tempBackupFile;

    public void onExportLocationSelected(Uri uri) {
        if (tempBackupFile == null) return;
        new Thread(() -> {
            try {
                InputStream is = new java.io.FileInputStream(tempBackupFile);
                OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
                os.close();
                is.close();
                tempBackupFile.delete();
                tempBackupFile = null;
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), lang.getBackupCreated(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void onImportFileSelected(Uri uri) {
        showPasswordDialog(false, uri);
    }

    private void startImportProcess(Uri uri, String password) {
        MainActivity activity = (MainActivity) requireActivity();
        activity.getBackupManager().importData(uri, password, new BackupManager.RestoreListener() {
            @Override
            public void onProgress(String message) {
                activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onSuccess() {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, lang.getRestoreSuccess(), Toast.LENGTH_SHORT).show();
                    activity.recreate(); // Reload everything
                });
            }

            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> Toast.makeText(activity, lang.getInvalidPassword(), Toast.LENGTH_LONG).show());
            }
        });
    }
}

