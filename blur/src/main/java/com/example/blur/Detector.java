package com.example.overlaydetector;

import android.graphics.Bitmap;
import com.example.overlaydetector.config.ServerConfig;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

public class Detector {
    private static final String BASE_URL = ServerConfig.detectorBaseUrlNoSlash();
    private static final String INFER_PATH = "/v1/detect";
    private static final float MIN_SCORE = 0.25f;
    private static final Set<String> DANGEROUS_CLASSES = Set.of(
            "FEMALE_GENITALIA_EXPOSED",
            "MALE_GENITALIA_EXPOSED",
            "ANUS_EXPOSED",
            "FEMALE_BREAST_EXPOSED",
            "BUTTOCKS_EXPOSED",
            "FEMALE_GENITALIA_COVERED",
            "MALE_GENITALIA_COVERED",
            "ANUS_COVERED",
            "FEMALE_BREAST_COVERED",
            "BUTTOCKS_COVERED"
    );

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(4, 5, TimeUnit.MINUTES))
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private volatile Call lastCall;

    public List<NsfwBox> detect(Bitmap bitmap) {
        Call previous = lastCall;
        if (previous != null) {
            previous.cancel();
        }

        byte[] jpegBytes = bitmapToJpeg(bitmap);
        if (jpegBytes.length == 0) {
            return List.of();
        }

        RequestBody fileBody = RequestBody.create(jpegBytes, MediaType.parse("image/jpeg"));
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "frame.jpg", fileBody)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + INFER_PATH)
                .post(multipartBody)
                .build();

        Call call = client.newCall(request);
        lastCall = call;

        try (var response = call.execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return List.of();
            }
            return parseResponse(response.body().string());
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<NsfwBox> parseResponse(String body) {
        try {
            JSONObject root = new JSONObject(body);
            if (!root.optBoolean("success")) {
                return List.of();
            }
            JSONArray prediction = root.optJSONArray("prediction");
            if (prediction == null || prediction.length() == 0) {
                return List.of();
            }
            JSONArray boxes = prediction.optJSONArray(0);
            if (boxes == null || boxes.length() == 0) {
                return List.of();
            }

            List<NsfwBox> result = new ArrayList<>();
            for (int i = 0; i < boxes.length(); i++) {
                JSONObject item = boxes.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String label = item.optString("class");
                float score = (float) item.optDouble("score", 0);
                JSONArray box = item.optJSONArray("box");
                if (score < MIN_SCORE || box == null || box.length() != 4 || !DANGEROUS_CLASSES.contains(label)) {
                    continue;
                }
                result.add(new NsfwBox(
                        label,
                        score,
                        box.optInt(0),
                        box.optInt(1),
                        box.optInt(2),
                        box.optInt(3)
                ));
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private byte[] bitmapToJpeg(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output);
        return ok ? output.toByteArray() : new byte[0];
    }
}
