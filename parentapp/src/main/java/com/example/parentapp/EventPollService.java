package com.example.parentapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.parentapp.config.ServerConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class EventPollService extends Service {
    private static final String TAG = "EventPollService";
    private static final String BASE_URL = ServerConfig.apiBaseUrlNoSlash();
    private static final long POLL_MS = 2500L;
    private static final String CHANNEL_FOREGROUND = "parent_foreground";
    private static final String CHANNEL_ALERTS = "parent_alerts";

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    private SessionStore store;

    public static void start(Context context) {
        Intent intent = new Intent(context, EventPollService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, EventPollService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        store = new SessionStore(this);
        ensureChannels();
        startForeground(1, buildForegroundNotification());
        running.set(true);
        new Thread(this::loop, "parent-event-poll").start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running.set(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void loop() {
        while (running.get()) {
            try {
                pollOnce();
            } catch (Exception e) {
                Log.e(TAG, "poll error", e);
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running.set(false);
            }
        }
    }

    private void pollOnce() throws Exception {
        String token = store.getToken();
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/parent/events?limit=1")
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            String body = readBody(response.body());
            if (!response.isSuccessful()) {
                Log.w(TAG, "code=" + response.code() + " body=" + abbreviate(body, 200));
                return;
            }

            JSONObject json = new JSONObject(body);
            JSONArray events = json.optJSONArray("events");
            if (events == null || events.length() == 0) {
                return;
            }

            JSONObject event = events.getJSONObject(0);
            String type = event.optString("event_type", event.optString("eventType", ""));
            String message = event.optString("message", "");
            String createdAt = event.optString("created_at", event.optString("createdAt", ""));

            String lastSeen = store.getLastSeenEventTime();
            if (!createdAt.isBlank() && !createdAt.equals(lastSeen)) {
                store.setLastSeenEventTime(createdAt);
                showAlert(type, message, createdAt);
            }
        }
    }

    private String readBody(ResponseBody responseBody) throws IOException {
        return responseBody == null ? "" : responseBody.string();
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void showAlert(String type, String message, String createdAt) {
        String niceTime = createdAt.length() > 19 ? createdAt.substring(0, 19) : createdAt;
        niceTime = niceTime.replace('T', ' ');

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Событие от ребёнка: " + type)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("[" + niceTime + "]\n" + type + ": " + message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) (System.currentTimeMillis() % 100000), notification);
    }

    private Notification buildForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_FOREGROUND)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("ParentApp: мониторинг включён")
                .setContentText("Получаю события ребёнка в фоне")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    private void ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(new NotificationChannel(
                CHANNEL_FOREGROUND,
                "Фоновая работа ParentApp",
                NotificationManager.IMPORTANCE_LOW
        ));
        notificationManager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ALERTS,
                "События ребёнка",
                NotificationManager.IMPORTANCE_HIGH
        ));
    }
}
