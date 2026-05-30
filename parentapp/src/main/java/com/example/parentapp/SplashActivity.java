package com.example.parentapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.activity.ComponentActivity;

public class SplashActivity extends ComponentActivity {
    private View softCircle;
    private TextView tvPercent;
    private TextView tvAppName;
    private int previousSize;
    private boolean moving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        softCircle = findViewById(R.id.softCircle);
        tvPercent = findViewById(R.id.tvPercent);
        tvAppName = findViewById(R.id.tvAppName);

        startLoadingAndRedirect();
    }

    private void startLoadingAndRedirect() {
        long totalMs = 2800L;
        long tickMs = 28L;
        int max = 100;

        var metrics = getResources().getDisplayMetrics();
        int target = (int) Math.hypot(metrics.widthPixels, metrics.heightPixels) + dp(120);
        int startSize = Math.max(softCircle.getLayoutParams().width, dp(140));
        previousSize = startSize;

        new CountDownTimer(totalMs, tickMs) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsed = totalMs - millisUntilFinished;
                double fraction = Math.max(0.0, Math.min(1.0, (double) elapsed / totalMs));
                int value = Math.max(0, Math.min(max, (int) (fraction * max)));
                tvPercent.setText(value + "%");

                int size = (int) (startSize + fraction * (target - startSize));
                int delta = size - previousSize;
                previousSize = size;

                var params = softCircle.getLayoutParams();
                params.width = size;
                params.height = size;
                softCircle.setLayoutParams(params);
                softCircle.requestLayout();

                if (!moving && value >= 60) {
                    moving = true;
                }

                if (moving && delta > 0) {
                    tvAppName.setTranslationY(tvAppName.getTranslationY() - delta);
                }
            }

            @Override
            public void onFinish() {
                tvPercent.setText("100%");
                var params = softCircle.getLayoutParams();
                params.width = target;
                params.height = target;
                softCircle.setLayoutParams(params);
                softCircle.requestLayout();

                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }.start();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
