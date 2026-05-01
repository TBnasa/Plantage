package com.tbnasa.plantage;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.Random;
import java.util.concurrent.Executor;

/**
 * SplashActivity - Uygulama açılış ekranı.
 * Her açılışta rastgele bir felsefe sözü gösterir ve Biyometrik Kilit uygular.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000;
    private LanguageManager langManager;
    private boolean isAuthenticating = false;

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

        TextView tvQuote = findViewById(R.id.tvQuote);
        TextView tvAppName = findViewById(R.id.tvAppName);
        ImageView imgLogo = findViewById(R.id.imgLogo);

        String randomQuote = getRandomQuote();

        if (imgLogo != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1200);
            fadeIn.setFillAfter(true);
            imgLogo.startAnimation(fadeIn);
        }

        if (tvQuote != null) {
            tvQuote.setText("\"" + randomQuote + "\"");
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1500);
            fadeIn.setStartOffset(500);
            fadeIn.setFillAfter(true);
            tvQuote.startAnimation(fadeIn);
        }

        if (tvAppName != null) {
            AlphaAnimation fadeInDelayed = new AlphaAnimation(0.0f, 1.0f);
            fadeInDelayed.setDuration(1000);
            fadeInDelayed.setStartOffset(300);
            fadeInDelayed.setFillAfter(true);
            tvAppName.startAnimation(fadeInDelayed);
        }

        // Delay starting authentication to allow splash to show
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (langManager.isBiometricEnabled()) {
                startBiometricAuth();
            } else {
                proceedToMain();
            }
        }, SPLASH_DURATION);
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
                // If user cancels or error occurs, show toast and possibly exit or retry
                Toast.makeText(getApplicationContext(), errString, Toast.LENGTH_SHORT).show();
                // For a privacy app, we might want to finish() here if auth fails
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
                // Failed attempt, usually handled by system UI, but we can log it
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setSubtitle(langManager.getLockedMemoryInfo())
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void proceedToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private String getRandomQuote() {
        String[] quotes = langManager.getQuotes();
        Random random = new Random();
        int index = random.nextInt(quotes.length);
        return quotes[index];
    }
}
