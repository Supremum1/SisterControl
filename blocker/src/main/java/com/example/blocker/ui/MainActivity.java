package com.example.blocker.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blocker.R;
import com.example.blocker.databinding.ActivityBlockerBinding;
import com.example.blocker.service.SiteBlockerService;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "self_site_blocker_prefs";
    private static final String KEY_BLOCKING_ENABLED = "blocking_enabled";
    private static final long OPEN_DELAY_MS = 450L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private ActivityBlockerBinding binding;
    private SharedPreferences prefs;
    private boolean pendingOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        binding = ActivityBlockerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean enabled = prefs.getBoolean(KEY_BLOCKING_ENABLED, false);
        binding.switchEnableBlocking.setChecked(enabled);
        updateStatusText();

        binding.switchEnableBlocking.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(KEY_BLOCKING_ENABLED, checked).apply();
            updateStatusText();
        });

        binding.buttonOpenAccessibility.setOnClickListener(view -> {
            if (pendingOpen) {
                return;
            }
            pendingOpen = true;
            binding.buttonOpenAccessibility.setEnabled(false);
            showSecondMascot();

            binding.getRoot().post(() -> ui.postDelayed(() -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                pendingOpen = false;
                binding.buttonOpenAccessibility.setEnabled(true);
            }, OPEN_DELAY_MS));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusText();
        showMainMascot();
        pendingOpen = false;
        binding.buttonOpenAccessibility.setEnabled(true);
    }

    private void showSecondMascot() {
        binding.ivMascotMain.setVisibility(View.INVISIBLE);
        binding.ivMascotAlt.setVisibility(View.VISIBLE);
        binding.ivMascotAlt.setAlpha(1f);
    }

    private void showMainMascot() {
        binding.ivMascotAlt.setVisibility(View.INVISIBLE);
        binding.ivMascotMain.setVisibility(View.VISIBLE);
        binding.ivMascotMain.setAlpha(1f);
    }

    private boolean isAccessibilityServiceEnabled() {
        String expectedComponentName = getPackageName() + "/" + SiteBlockerService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabledServices == null) {
            return false;
        }

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            String componentName = splitter.next();
            if (componentName.equalsIgnoreCase(expectedComponentName)) {
                return true;
            }
        }
        return false;
    }

    private void updateStatusText() {
        boolean blockingEnabled = prefs.getBoolean(KEY_BLOCKING_ENABLED, false);
        boolean serviceEnabled = isAccessibilityServiceEnabled();
        int textId = blockingEnabled && serviceEnabled
                ? R.string.status_blocking_enabled
                : R.string.status_blocking_disabled;
        binding.textStatus.setText(getString(textId));
    }
}
