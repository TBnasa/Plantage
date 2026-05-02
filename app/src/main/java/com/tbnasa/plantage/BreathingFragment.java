package com.tbnasa.plantage;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BreathingFragment extends Fragment {

    private View breathingCircle;
    private TextView tvTimerCount, tvZenInstruction;
    private Button btnZenAction;
    private LanguageManager lang;
    private Vibrator vibrator;

    private boolean isRunning = false;
    private CountDownTimer mainTimer;
    private int currentStep = 0; // 0: Inhale, 1: Hold, 2: Exhale
    private int secondsRemaining = 4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_breathing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        lang = activity.getLang();
        vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);

        breathingCircle = view.findViewById(R.id.breathingCircle);
        tvTimerCount = view.findViewById(R.id.tvTimerCount);
        tvZenInstruction = view.findViewById(R.id.tvZenInstruction);
        btnZenAction = view.findViewById(R.id.btnZenAction);

        tvZenInstruction.setText("");
        tvTimerCount.setText("4");

        btnZenAction.setOnClickListener(v -> {
            if (isRunning) {
                stopBreathing();
            } else {
                startBreathing();
            }
        });
    }

    private void startBreathing() {
        isRunning = true;
        btnZenAction.setText(lang.getStopZen());
        btnZenAction.setBackgroundResource(R.drawable.bg_button_danger);
        currentStep = 0;
        secondsRemaining = 4;
        runStep();
    }

    private void stopBreathing() {
        isRunning = false;
        if (mainTimer != null) mainTimer.cancel();
        btnZenAction.setText(lang.getStartZen());
        btnZenAction.setBackgroundResource(R.drawable.bg_button_zen);
        tvZenInstruction.setText("");
        tvTimerCount.setText("4");
        breathingCircle.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
        
        Toast.makeText(getContext(), lang.getZenDone(), Toast.LENGTH_LONG).show();
    }

    private void runStep() {
        if (!isRunning) return;

        // Vibrate on step change
        if (vibrator != null) {
            vibrator.vibrate(100);
        }

        // Update Instruction and Animation
        switch (currentStep) {
            case 0: // Inhale
                tvZenInstruction.setText(lang.getInhale());
                breathingCircle.animate()
                        .scaleX(1.4f)
                        .scaleY(1.4f)
                        .setDuration(4000)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                break;
            case 1: // Hold
                tvZenInstruction.setText(lang.getHold());
                // No scale animation, keep current
                break;
            case 2: // Exhale
                tvZenInstruction.setText(lang.getExhale());
                breathingCircle.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(4000)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                break;
        }

        secondsRemaining = 4;
        tvTimerCount.setText(String.valueOf(secondsRemaining));

        mainTimer = new CountDownTimer(4000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = (int) (millisUntilFinished / 1000) + 1;
                tvTimerCount.setText(String.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                currentStep = (currentStep + 1) % 3;
                runStep();
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mainTimer != null) mainTimer.cancel();
    }
}