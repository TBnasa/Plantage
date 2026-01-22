package com.example.orbitfocus;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.animation.AlphaAnimation;
import java.util.Random;

/**
 * SplashActivity - Uygulama açılış ekranı.
 * Her açılışta rastgele bir felsefe sözü gösterir.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 saniye
    private LanguageManager langManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        langManager = new LanguageManager(this);

        TextView tvQuote = findViewById(R.id.tvQuote);
        TextView tvAppName = findViewById(R.id.tvAppName);
        ImageView imgLogo = findViewById(R.id.imgLogo);

        // Get random quote based on language
        String randomQuote = getRandomQuote();

        // Fade in animation for logo
        if (imgLogo != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1200);
            fadeIn.setFillAfter(true);
            imgLogo.startAnimation(fadeIn);
        }

        // Quote animation - Fade in
        if (tvQuote != null) {
            tvQuote.setText("\"" + randomQuote + "\"");
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1500);
            fadeIn.setStartOffset(500);
            fadeIn.setFillAfter(true);
            tvQuote.startAnimation(fadeIn);
        }

        // App name animation
        if (tvAppName != null) {
            AlphaAnimation fadeInDelayed = new AlphaAnimation(0.0f, 1.0f);
            fadeInDelayed.setDuration(1000);
            fadeInDelayed.setStartOffset(300);
            fadeInDelayed.setFillAfter(true);
            tvAppName.startAnimation(fadeInDelayed);
        }

        // Navigate to MainActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION);
    }

    /**
     * Rastgele bir felsefe sözü döndürür (dile göre).
     */
    private String getRandomQuote() {
        String[] quotes = langManager.getQuotes();
        Random random = new Random();
        int index = random.nextInt(quotes.length);
        return quotes[index];
    }
}
