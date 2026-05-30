package com.example.childapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ChildPermissionsOnboardingActivity extends AppCompatActivity {
    private static final String PKG_BLUR = "com.example.overlaydetector";
    private static final String PKG_BLOCKER = "com.example.blocker";
    private static final String PKG_TIMEGUARD = "com.example.timeguard";

    private SessionStore store;
    private TextView tvBlurStatus;
    private TextView tvBlockerStatus;
    private TextView tvTimeGuardStatus;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_permissions_onboarding);

        store = new SessionStore(this);
        tvBlurStatus = findViewById(R.id.tvBlurStatus);
        tvBlockerStatus = findViewById(R.id.tvBlockerStatus);
        tvTimeGuardStatus = findViewById(R.id.tvTimeGuardStatus);
        btnContinue = findViewById(R.id.btnContinue);

        Button blurSetupButton = findViewById(R.id.btnBlurSetup);
        Button blockerSetupButton = findViewById(R.id.btnBlockerSetup);
        Button timeGuardSetupButton = findViewById(R.id.btnTimeGuardSetup);

        blurSetupButton.setOnClickListener(view -> {
            openOverlaySettingsFor(PKG_BLUR);
            tryStartActivityByClass("com.example.overlaydetector.MainActivity");
        });
        blockerSetupButton.setOnClickListener(view -> {
            openAccessibilitySettings();
            tryStartActivityByClass("com.example.blocker.ui.MainActivity");
        });
        timeGuardSetupButton.setOnClickListener(view -> {
            pushTokenToTimeGuard();
            openAccessibilitySettings();
        });
        btnContinue.setOnClickListener(view -> {
            store.setPermissionsOnboardingDone(true);
            Intent intent = new Intent(this, ChildHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {
        boolean blurOk = Settings.canDrawOverlays(this);
        boolean blockerOk = isAccessibilityEnabledContains(PKG_BLOCKER);
        boolean timeOk = isAccessibilityEnabledContains(PKG_TIMEGUARD);

        tvBlurStatus.setText("Статус: " + (blurOk ? " выдано" : " не выдано"));
        tvBlockerStatus.setText("Статус: " + (blockerOk ? " включено" : " не включено"));
        tvTimeGuardStatus.setText("Статус: " + (timeOk ? " включено" : " не включено"));
        btnContinue.setEnabled(blurOk && blockerOk && timeOk);
    }

    private boolean isAccessibilityEnabledContains(String marker) {
        String enabled = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabled == null) {
            return false;
        }

        for (String service : enabled.split(":")) {
            if (service.toLowerCase().contains(marker.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void openOverlaySettingsFor(String packageName) {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + packageName)
        );
        startActivity(intent);
    }

    private void pushTokenToTimeGuard() {
        String token = store.getToken();
        if (token == null) {
            return;
        }
        Intent intent = new Intent("com.example.timeguard.SET_TOKEN");
        intent.setPackage(PKG_TIMEGUARD);
        intent.putExtra("token", token);
        try {
            sendBroadcast(intent);
        } catch (Exception ignored) {
        }
    }

    private void tryStartActivityByClass(String className) {
        try {
            Class<?> activityClass = Class.forName(className);
            startActivity(new Intent(this, activityClass));
        } catch (Exception ignored) {
        }
    }
}
