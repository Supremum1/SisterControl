package com.example.timeguard.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import com.example.timeguard.BlockedActivity;
import com.example.timeguard.config.ServerConfig;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

public class AppTimeGuardService extends AccessibilityService {
    private static final String TAG = "TimeGuard";

    public static final String PREFS = "timeguard_prefs";
    public static final String KEY_POLICY_JSON = "policy_json";
    public static final String KEY_DAY = "usage_day";
    public static final String KEY_TOTAL_SEC = "total_sec";
    public static final String KEY_APP_SEC_PREFIX = "app_sec_";
    public static final String ACTION_POLICY_UPDATED = "com.example.childapp.TIMEGUARD_POLICY_UPDATED";

    private static final String KEY_WARN_PREFIX = "warn_";
    private static final String TOKEN_PREFS = "session";
    private static final String TOKEN_KEY = "token";
    private static final String BASE_URL = ServerConfig.apiBaseUrlNoSlash();
    private static final long POLICY_FETCH_COOLDOWN_MS = 1_000L;
    private static final int WARN_5_MIN = 5 * 60;
    private static final int WARN_3_MIN = 3 * 60;
    private static final int WARN_1_MIN = 1 * 60;
    private static final long USAGE_UPLOAD_COOLDOWN_MS = 1_000L;
    private static final Set<String> ALWAYS_ALLOW = Set.of(
            "com.example.childapp",
            "com.example.timeguard",
            "com.android.settings",
            "com.android.systemui",
            "com.google.android.dialer",
            "com.android.dialer"
    );

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build();
    private final Map<String, Integer> appLimitsMin = new HashMap<>();

    private int totalLimitMin;
    private String lastPkg;
    private long lastTickMs;
    private long lastPolicyFetchMs;
    private long lastUsageUploadMs;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        loadPolicyFromPrefs();
        fetchPolicyAsync();
        Log.d(TAG, "Service connected");
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        String packageName = event.getPackageName().toString();
        if (packageName.isBlank()) {
            return;
        }

        if (ALWAYS_ALLOW.contains(packageName)) {
            tickUsage(packageName);
            return;
        }

        fetchPolicyAsync();
        tickUsage(packageName);
        uploadUsageAsync();
        enforceIfNeeded(packageName);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void loadPolicyFromPrefs() {
        totalLimitMin = 0;
        appLimitsMin.clear();

        String raw = prefs().getString(KEY_POLICY_JSON, null);
        if (raw == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject(raw);
            totalLimitMin = Math.max(json.optInt("totalLimitMin", 0), 0);
            JSONArray rules = json.optJSONArray("rules");
            if (rules == null) {
                rules = new JSONArray();
            }
            for (int i = 0; i < rules.length(); i++) {
                JSONObject rule = rules.getJSONObject(i);
                String packageName = rule.optString("packageName", "").trim();
                int limit = rule.optInt("limitMin", 0);
                if (!packageName.isBlank() && limit > 0) {
                    appLimitsMin.put(packageName, limit);
                }
            }
            Log.d(TAG, "Policy loaded: total=" + totalLimitMin + " rules=" + appLimitsMin.size());
        } catch (Exception ex) {
            Log.e(TAG, "Policy parse error", ex);
        }
    }

    private void savePolicyToPrefs(String json) {
        SharedPreferences preferences = prefs();
        String previous = preferences.getString(KEY_POLICY_JSON, null);
        preferences.edit().putString(KEY_POLICY_JSON, json).apply();
        loadPolicyFromPrefs();

        if (!json.equals(previous)) {
            SharedPreferences.Editor editor = preferences.edit();
            for (String key : new HashSet<>(preferences.getAll().keySet())) {
                if (key.startsWith(KEY_WARN_PREFIX)) {
                    editor.remove(key);
                }
            }
            editor.apply();
            sendBroadcast(new Intent(ACTION_POLICY_UPDATED));
        }
    }

    private void fetchPolicyAsync() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastPolicyFetchMs < POLICY_FETCH_COOLDOWN_MS) {
            return;
        }
        lastPolicyFetchMs = now;

        String token = getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null);
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "No token yet -> cannot fetch policy");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/child/screen-time")
                .addHeader("Authorization", "Bearer " + token.trim())
                .get()
                .build();

        new Thread(() -> {
            try (var response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Policy fetch failed code=" + response.code());
                    return;
                }
                String body = response.body() == null ? "" : response.body().string();
                savePolicyToPrefs(body);
                Log.d(TAG, "Policy fetched ok");
            } catch (Exception ex) {
                Log.e(TAG, "Policy fetch error", ex);
            }
        }).start();
    }

    private String todayKey() {
        Calendar calendar = Calendar.getInstance();
        return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    private void resetIfNewDay() {
        String day = todayKey();
        SharedPreferences preferences = prefs();
        String saved = preferences.getString(KEY_DAY, null);
        if (day.equals(saved)) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DAY, day);
        editor.putInt(KEY_TOTAL_SEC, 0);
        for (String key : new HashSet<>(preferences.getAll().keySet())) {
            if (key.startsWith(KEY_APP_SEC_PREFIX) || key.startsWith(KEY_WARN_PREFIX)) {
                editor.remove(key);
            }
        }
        editor.apply();

        lastTickMs = 0L;
        lastPkg = null;
        Log.d(TAG, "Reset usage for new day: " + day);
        sendBroadcast(new Intent(ACTION_POLICY_UPDATED));
    }

    private void tickUsage(String currentPkg) {
        resetIfNewDay();

        long now = SystemClock.elapsedRealtime();
        if (lastTickMs == 0L) {
            lastTickMs = now;
            lastPkg = currentPkg;
            return;
        }

        String prevPkg = lastPkg;
        int deltaSec = (int) ((now - lastTickMs) / 1000L);
        lastTickMs = now;
        lastPkg = currentPkg;

        deltaSec = Math.max(0, Math.min(10, deltaSec));
        if (deltaSec <= 0 || prevPkg == null || prevPkg.isBlank()) {
            return;
        }

        SharedPreferences preferences = prefs();
        preferences.edit()
                .putInt(KEY_TOTAL_SEC, preferences.getInt(KEY_TOTAL_SEC, 0) + deltaSec)
                .apply();

        String appKey = KEY_APP_SEC_PREFIX + prevPkg;
        preferences.edit()
                .putInt(appKey, preferences.getInt(appKey, 0) + deltaSec)
                .apply();
    }

    private void enforceIfNeeded(String packageName) {
        resetIfNewDay();
        SharedPreferences preferences = prefs();

        if (totalLimitMin > 0) {
            int totalSec = preferences.getInt(KEY_TOTAL_SEC, 0);
            int leftSec = totalLimitMin * 60 - totalSec;
            if (leftSec <= 0) {
                blockNow(packageName, "Общий лимит телефона исчерпан до завтра");
                sendChildEvent("SCREEN_TIME_LIMIT_REACHED", "Total limit reached");
                return;
            }
            maybeWarn(packageName, leftSec, true);
        }

        Integer limitMin = appLimitsMin.get(packageName);
        if (limitMin == null) {
            return;
        }
        int usedSec = preferences.getInt(KEY_APP_SEC_PREFIX + packageName, 0);
        int leftSec = limitMin * 60 - usedSec;
        if (leftSec <= 0) {
            blockNow(packageName, "Лимит для приложения исчерпан до завтра");
            sendChildEvent("SCREEN_TIME_LIMIT_REACHED", "App limit reached: " + packageName);
            return;
        }
        maybeWarn(packageName, leftSec, false);
    }

    private void maybeWarn(String packageName, int leftSec, boolean total) {
        String keyBase = total ? "TOTAL" : packageName;
        int threshold;
        if (leftSec <= WARN_1_MIN) {
            threshold = 1;
        } else if (leftSec <= WARN_3_MIN) {
            threshold = 3;
        } else if (leftSec <= WARN_5_MIN) {
            threshold = 5;
        } else {
            threshold = 0;
        }
        if (threshold == 0) {
            return;
        }

        String warnKey = KEY_WARN_PREFIX + keyBase + "_" + threshold;
        if (prefs().getBoolean(warnKey, false)) {
            return;
        }
        prefs().edit().putBoolean(warnKey, true).apply();

        String message = total
                ? "Осталось ~" + threshold + " мин общего времени"
                : "Осталось ~" + threshold + " мин в приложении";
        showToast(message);
        sendChildEvent("SCREEN_TIME_WARNING", message);
    }

    private void blockNow(String packageName, String reason) {
        performGlobalAction(GLOBAL_ACTION_BACK);

        Intent intent = new Intent(this, BlockedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("reason", reason);
        intent.putExtra("pkg", packageName);
        startActivity(intent);

        showToast(reason);
        Log.w(TAG, "BLOCK: pkg=" + packageName + " reason=" + reason);
    }

    private void showToast(String text) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }

    private void sendChildEvent(String eventType, String message) {
        String token = getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null);
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        try {
            JSONObject json = new JSONObject()
                    .put("eventType", eventType)
                    .put("message", message);
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/child/event")
                    .addHeader("Authorization", "Bearer " + token.trim())
                    .post(body)
                    .build();

            new Thread(() -> {
                try (var response = http.newCall(request).execute()) {
                    Log.d(TAG, "Event sent type=" + eventType + " code=" + response.code());
                } catch (Exception ex) {
                    Log.e(TAG, "Event send failed", ex);
                }
            }).start();
        } catch (Exception ex) {
            Log.e(TAG, "Event JSON build failed", ex);
        }
    }

    private void uploadUsageAsync() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastUsageUploadMs < USAGE_UPLOAD_COOLDOWN_MS) {
            return;
        }
        lastUsageUploadMs = now;

        String token = getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null);
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "No token -> cannot upload usage");
            return;
        }
        if (appLimitsMin.isEmpty()) {
            return;
        }

        String day = todayKey();
        JSONArray usage = new JSONArray();
        try {
            for (String packageName : new ArrayList<>(appLimitsMin.keySet())) {
                int usedSec = prefs().getInt(KEY_APP_SEC_PREFIX + packageName, 0);
                usage.put(new JSONObject()
                        .put("packageName", packageName)
                        .put("usedSec", usedSec));
            }

            JSONObject json = new JSONObject()
                    .put("day", day)
                    .put("usage", usage);
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/child/screen-time-usage")
                    .addHeader("Authorization", "Bearer " + token.trim())
                    .post(body)
                    .build();

            new Thread(() -> {
                try (var response = http.newCall(request).execute()) {
                    Log.d(TAG, "Usage upload -> code=" + response.code());
                } catch (Exception ex) {
                    Log.e(TAG, "Usage upload failed", ex);
                }
            }).start();
        } catch (Exception ex) {
            Log.e(TAG, "Usage JSON build failed", ex);
        }
    }
}
