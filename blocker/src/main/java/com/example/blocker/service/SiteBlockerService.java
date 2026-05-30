package com.example.blocker.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import com.example.blocker.config.ServerConfig;
import com.example.blocker.ui.BlockedActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

public class SiteBlockerService extends AccessibilityService {
    private static final String TAG = "Blocker";
    private static final String PREFS = "self_site_blocker_prefs";
    private static final String KEY_ENABLED = "blocking_enabled";
    private static final String EVENTS_BASE_URL = ServerConfig.apiBaseUrlNoSlash();
    private static final String TOKEN_PREFS = "session";
    private static final String TOKEN_KEY = "token";
    private static final long SEND_COOLDOWN_MS = 1500L;
    private static final long UI_COOLDOWN_MS = 1500L;

    private final List<String> blocked = new ArrayList<>();
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .build();

    private SharedPreferences prefs;
    private long lastSendAtMs;
    private long lastUiAtMs;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadBlockedList();
        Log.d(TAG, "Service connected. Domains loaded: " + blocked.size());
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || prefs == null || !prefs.getBoolean(KEY_ENABLED, false)) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            scanNode(root);
        }
    }

    private void loadBlockedList() {
        blocked.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("blocked_sites.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String cleaned = line.trim().toLowerCase(Locale.ROOT);
                if (!cleaned.isEmpty() && !cleaned.startsWith("#")) {
                    blocked.add(cleaned);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Cannot load blocked list: " + ex.getMessage(), ex);
        }
    }

    private void scanNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }

        CharSequence nodeText = node.getText();
        String text = nodeText == null ? "" : nodeText.toString().toLowerCase(Locale.ROOT);
        if (!text.isEmpty()) {
            for (String domain : blocked) {
                if (text.contains(domain)) {
                    Log.w(TAG, "MATCH FOUND: " + domain + " in \"" + text + "\"");
                    softBlock(domain);
                    return;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            scanNode(node.getChild(i));
        }
    }

    private void softBlock(String domain) {
        Log.d(TAG, "Soft-block triggered, domain=" + domain);
        performGlobalAction(GLOBAL_ACTION_BACK);
        triggerVibration();
        showBlockedUi(domain);
        showSoftToast("Blocked: unsafe site");
        sendChildEventSiteBlocked(domain);
    }

    private void sendChildEventSiteBlocked(String domain) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastSendAtMs < SEND_COOLDOWN_MS) {
            return;
        }
        lastSendAtMs = now;

        String token = getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null);
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "No token in prefs(" + TOKEN_PREFS + "/" + TOKEN_KEY + "). Event not sent.");
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("eventType", "SITE_BLOCKED");
            json.put("message", "Заблокирован сайт: " + domain);
        } catch (Exception ex) {
            Log.e(TAG, "Cannot build event JSON", ex);
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(EVENTS_BASE_URL + "/api/child/event")
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        new Thread(() -> {
            try (var response = http.newCall(request).execute()) {
                Log.d(TAG, "Event sent /api/child/event -> code=" + response.code());
            } catch (Exception ex) {
                Log.e(TAG, "Event send failed", ex);
            }
        }).start();
    }

    private void triggerVibration() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(80);
        }
    }

    private void showSoftToast(String message) {
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show(),
                180
        );
    }

    private void showBlockedUi(String domain) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastUiAtMs < UI_COOLDOWN_MS) {
            return;
        }
        lastUiAtMs = now;

        Intent intent = new Intent(this, BlockedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("domain", domain);
        try {
            startActivity(intent);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to start BlockedActivity", ex);
        }
    }
}
