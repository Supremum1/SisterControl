package com.example.overlaydetector;

import android.content.Context;
import com.example.overlaydetector.config.ServerConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

public class EventReporter {
    private static final String BASE_URL = ServerConfig.apiBaseUrlNoSlash();
    private static final String PREFS = "session";
    private static final String KEY_TOKEN = "token";

    private final Context context;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();

    public EventReporter(Context context) {
        this.context = context;
    }

    public boolean sendEvent(String eventType, String message) {
        String token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null);
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject json = new JSONObject()
                    .put("eventType", eventType)
                    .put("message", message);
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/child/event")
                    .addHeader("Authorization", "Bearer " + token.trim())
                    .post(body)
                    .build();

            try (var response = http.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception ex) {
            return false;
        }
    }
}
