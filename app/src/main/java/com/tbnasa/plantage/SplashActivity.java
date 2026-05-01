package com.tbnasa.plantage;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.Random;
import java.util.concurrent.Executor;

/**
 * SplashActivity — Premium animated splash screen.
 * Shows animated tree canvas, quote, and transitions to MainActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2200;
    private LanguageManager langManager;
    private boolean isAuthenticating = false;
    private boolean hasNavigated = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        LanguageManager lm = new LanguageManager(newBase);
        lm.applyTheme();
        super.attachBaseContext(lm.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        langManager = new LanguageManager(this);

        TextView tvQuote   = findViewById(R.id.tvQuote);
        TextView tvAppName = findViewById(R.id.tvAppName);

        View logoContainer = findViewById(R.id.logoContainer);
        View dotIndicator = findViewById(R.id.dotIndicator);

        // Animate app name fade in
        if (tvAppName != null) {
            tvAppName.animate().alpha(1f).setDuration(1000).setStartDelay(200).setInterpolator(new DecelerateInterpolator()).start();
        }

        // Animate quote fade in
        if (tvQuote != null) {
            String randomQuote = getRandomQuote();
            tvQuote.setText("\u201c" + randomQuote + "\u201d");
            tvQuote.animate().alpha(1f).setDuration(1200).setStartDelay(600).setInterpolator(new DecelerateInterpolator()).start();
        }

        // Animate logo and dot fade in
        if (logoContainer != null) {
            logoContainer.animate().alpha(1f).setDuration(1000).setInterpolator(new DecelerateInterpolator()).start();
        }

        if (dotIndicator != null) {
            dotIndicator.animate().alpha(1f).setDuration(1200).setStartDelay(800).setInterpolator(new DecelerateInterpolator()).start();
        }

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (langManager.isBiometricEnabled()) {
                startBiometricAuth();
            } else {
                proceedToMain();
            }
        }, SPLASH_DURATION);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Touch to skip
        if (event.getAction() == MotionEvent.ACTION_UP && !hasNavigated && !isAuthenticating) {
            if (langManager.isBiometricEnabled()) {
                startBiometricAuth();
            } else {
                proceedToMain();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void startBiometricAuth() {
        if (isAuthenticating) return;
        isAuthenticating = true;

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(SplashActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                isAuthenticating = false;
                Toast.makeText(getApplicationContext(), errString, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isAuthenticating = false;
                proceedToMain();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setSubtitle(langManager.getLockedMemoryInfo())
                .setAllowedAuthenticators(
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void proceedToMain() {
        if (hasNavigated) return;
        hasNavigated = true;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private String getRandomQuote() {
        String[] quotes = langManager.getQuotes();
        Random random = new Random();
        return quotes[random.nextInt(quotes.length)];
    }
}
