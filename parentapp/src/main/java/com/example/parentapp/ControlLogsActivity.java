package com.example.parentapp;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parentapp.config.ServerConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ControlLogsActivity extends AppCompatActivity {
    private static final String TAG = "ControlLogs";
    private static final String BASE_URL = ServerConfig.apiBaseUrlNoSlash();
    private static final long POLL_MS = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    private final Runnable pollRunnable = this::fetchEvents;

    private TextView tvLogs;
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_logs);

        store = new SessionStore(this);
        tvLogs = findViewById(R.id.tvLogs);
        NotificationUtils.ensureChannel(this);

        tvLogs.setText("Загрузка логов...");
        String token = store.getToken();
        if (token == null || token.isBlank()) {
            tvLogs.setText("Нет токена родителя. Сначала выполните login в ParentApp.");
            return;
        }

        fetchEvents();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void fetchEvents() {
        String token = store.getToken();
        if (token == null) {
            scheduleNext();
            return;
        }

        if (!inFlight.compareAndSet(false, true)) {
            scheduleNext();
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/parent/events?limit=50")
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = http.newCall(request).execute()) {
                String body = readBody(response.body());
                if (!response.isSuccessful()) {
                    Log.w(TAG, "code=" + response.code() + " body=" + abbreviate(body, 500));
                    runOnUiThread(() -> tvLogs.setText("Ошибка: " + response.code()));
                    return;
                }

                JSONObject json = new JSONObject(body);
                JSONArray events = json.optJSONArray("events");
                if (events == null) {
                    events = new JSONArray();
                }

                EventsBuildResult result = buildEventsAndDetectNew(events);
                runOnUiThread(() -> tvLogs.setText(result.text.isBlank() ? "События отсутствуют (или не пришли)." : result.text));

                if (result.newCount > 0 && result.newestTs != null && !result.newestTs.isBlank()) {
                    store.saveLastEventTs(result.newestTs);
                    if (canPostNotifications()) {
                        String title = result.newCount == 1 ? "Новое событие от ребёнка" : "Новых событий: " + result.newCount;
                        String notificationText = result.newestLine == null ? "Откройте логи, чтобы посмотреть детали" : result.newestLine;
                        NotificationUtils.notifyNewEvents(this, title, notificationText, 1001);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "fetch failed", e);
                runOnUiThread(() -> toast("Сеть/сервер недоступны: " + e.getClass().getSimpleName() + " / " + e.getMessage()));
            } finally {
                inFlight.set(false);
                scheduleNext();
            }
        }, "parent-control-logs").start();
    }

    private String readBody(ResponseBody responseBody) throws Exception {
        return responseBody == null ? "" : responseBody.string();
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean canPostNotifications() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private EventsBuildResult buildEventsAndDetectNew(JSONArray events) throws Exception {
        StringBuilder text = new StringBuilder();
        String lastTsSaved = store.getLastEventTs();
        Instant lastInstant = lastTsSaved == null ? null : safeInstant(lastTsSaved);

        int newCount = 0;
        String newestTs = null;
        String newestLine = null;

        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            String type = event.optString("event_type", event.optString("eventType", ""));
            String message = event.optString("message", "");
            String createdAt = event.optString("created_at", event.optString("createdAt", ""));
            String niceTime = createdAt.length() > 19 ? createdAt.substring(0, 19) : createdAt;
            niceTime = niceTime.replace('T', ' ');

            String line = "[" + niceTime + "] " + type + ": " + message;
            text.append(line).append('\n');

            Instant instant = safeInstant(createdAt);
            if (instant != null) {
                Instant newestInstant = newestTs == null ? null : safeInstant(newestTs);
                if (newestInstant == null || instant.isAfter(newestInstant)) {
                    newestTs = createdAt;
                    newestLine = line;
                }
                if (lastInstant == null || instant.isAfter(lastInstant)) {
                    newCount++;
                }
            }
        }

        return new EventsBuildResult(rtrim(text.toString()), newCount, newestTs, newestLine);
    }

    private Instant safeInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String rtrim(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private void scheduleNext() {
        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, POLL_MS);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private static class EventsBuildResult {
        private final String text;
        private final int newCount;
        private final String newestTs;
        private final String newestLine;

        private EventsBuildResult(String text, int newCount, String newestTs, String newestLine) {
            this.text = text;
            this.newCount = newCount;
            this.newestTs = newestTs;
            this.newestLine = newestLine;
        }
    }
}
