package com.example.parentapp;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GpsTrackingActivity extends AppCompatActivity {
    private static final long POLL_MS = 500L;

    private final ApiService api = ApiService.create();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            fetchChildLocation();
            handler.postDelayed(this, POLL_MS);
        }
    };

    private MapView map;
    private Marker marker;
    private SessionStore store;
    private GeoPoint lastPoint;
    private ValueAnimator animation;
    private boolean pickSafeZoneMode;
    private GeoPoint safeZoneCenter;
    private Double safeZoneRadiusM;
    private Polygon safeZonePolygon;
    private SafeZoneLabelOverlay safeZoneLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_gps_tracking);

        store = new SessionStore(this);
        map = findViewById(R.id.map);

        Button btnSafe = findViewById(R.id.btnSafeZone);
        btnSafe.setOnClickListener(view -> {
            pickSafeZoneMode = true;
            toast("Выбери на карте точку (например, дом) --- это будет центр безопасной зоны");
        });

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint point) {
                if (!pickSafeZoneMode) {
                    return false;
                }
                pickSafeZoneMode = false;
                safeZoneCenter = point;
                showRadiusDialogAndApply(point);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint point) {
                return false;
            }
        }));

        map.setMultiTouchControls(true);
        map.getController().setZoom(3.5);
        map.getController().setCenter(new GeoPoint(20.0, 0.0));

        restoreSafeZoneIfAny();

        String token = store.getToken();
        if (token == null || token.isBlank()) {
            toast("Нет токена родителя. Сначала login.");
            finish();
            return;
        }

        api.requestGps("Bearer " + token).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (!response.isSuccessful()) {
                    toast("Не удалось запросить GPS у ребёнка: code=" + response.code());
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable throwable) {
                toast("Сеть: " + throwable.getMessage());
            }
        });

        handler.post(pollRunnable);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        map.onDetach();
        super.onDestroy();
    }

    private void fetchChildLocation() {
        String token = store.getToken();
        if (token == null) {
            return;
        }

        api.getChildLocation("Bearer " + token).enqueue(new Callback<ChildLocationResponse>() {
            @Override
            public void onResponse(Call<ChildLocationResponse> call, Response<ChildLocationResponse> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                ChildLocationResponse body = response.body();
                if (body == null || !body.getHasLocation() || body.getLat() == null || body.getLon() == null) {
                    return;
                }

                GeoPoint point = new GeoPoint(body.getLat(), body.getLon());
                if (marker == null) {
                    marker = new Marker(map);
                    marker.setPosition(point);
                    marker.setTitle("Ребёнок");
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(marker);
                    map.getController().setZoom(15.0);
                    map.getController().setCenter(point);
                    lastPoint = point;
                } else {
                    animateMarkerTo(point, POLL_MS);
                }

                map.invalidate();
            }

            @Override
            public void onFailure(Call<ChildLocationResponse> call, Throwable throwable) {
            }
        });
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private void animateMarkerTo(GeoPoint newPoint, long durationMs) {
        if (marker == null) {
            return;
        }
        GeoPoint start = lastPoint == null ? marker.getPosition() : lastPoint;
        if (start == null) {
            start = newPoint;
        }

        if (animation != null) {
            animation.cancel();
        }

        GeoPoint startPoint = start;
        animation = ValueAnimator.ofFloat(0f, 1f);
        animation.setDuration(durationMs);
        animation.setInterpolator(new LinearInterpolator());
        animation.addUpdateListener(valueAnimator -> {
            float progress = (Float) valueAnimator.getAnimatedValue();
            double lat = startPoint.getLatitude() + (newPoint.getLatitude() - startPoint.getLatitude()) * progress;
            double lon = startPoint.getLongitude() + (newPoint.getLongitude() - startPoint.getLongitude()) * progress;
            marker.setPosition(new GeoPoint(lat, lon));
            map.invalidate();
        });
        animation.start();
        lastPoint = newPoint;
    }

    private void showRadiusDialogAndApply(GeoPoint center) {
        EditText input = new EditText(this);
        input.setHint("Радиус в метрах (например 200)");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText("200");

        new AlertDialog.Builder(this)
                .setTitle("Безопасная зона")
                .setMessage("Введи радиус в метрах")
                .setView(input)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    double radius;
                    try {
                        radius = Double.parseDouble(input.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        toast("Радиус должен быть числом > 0");
                        return;
                    }
                    if (radius <= 0) {
                        toast("Радиус должен быть числом > 0");
                        return;
                    }

                    safeZoneRadiusM = radius;
                    safeZoneCenter = center;
                    store.saveSafeZone(center.getLatitude(), center.getLongitude(), radius);
                    drawSafeZone(center, radius);

                    String token = store.getToken();
                    if (token == null) {
                        return;
                    }
                    api.setSafeZone("Bearer " + token, new SafeZoneRequest(center.getLatitude(), center.getLongitude(), radius))
                            .enqueue(new Callback<BasicResponse>() {
                                @Override
                                public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                                    if (!response.isSuccessful()) {
                                        toast("Не удалось сохранить зону: code=" + response.code());
                                    } else {
                                        toast("Безопасная зона установлена");
                                    }
                                }

                                @Override
                                public void onFailure(Call<BasicResponse> call, Throwable throwable) {
                                    toast("Сеть: " + throwable.getMessage());
                                }
                            });
                })
                .show();
    }

    private void drawSafeZone(GeoPoint center, double radiusM) {
        if (safeZonePolygon != null) {
            map.getOverlays().remove(safeZonePolygon);
        }
        if (safeZoneLabel != null) {
            map.getOverlays().remove(safeZoneLabel);
        }

        ArrayList<GeoPoint> circlePoints = buildCirclePoints(center, radiusM, 72);

        Polygon polygon = new Polygon(map);
        polygon.setPoints(circlePoints);
        polygon.getFillPaint().setColor(Color.argb(70, 0, 255, 0));
        polygon.getOutlinePaint().setColor(Color.argb(200, 0, 200, 0));
        polygon.getOutlinePaint().setStrokeWidth(4f);
        safeZonePolygon = polygon;
        map.getOverlays().add(polygon);

        SafeZoneLabelOverlay label = new SafeZoneLabelOverlay(center, "Безопасная зона");
        safeZoneLabel = label;
        map.getOverlays().add(label);
        map.invalidate();
    }

    private ArrayList<GeoPoint> buildCirclePoints(GeoPoint center, double radiusM, int steps) {
        ArrayList<GeoPoint> points = new ArrayList<>(steps + 1);
        double lat = Math.toRadians(center.getLatitude());
        double lon = Math.toRadians(center.getLongitude());
        double earth = 6371000.0;
        double distance = radiusM / earth;

        for (int i = 0; i <= steps; i++) {
            double bearing = 2.0 * Math.PI * i / steps;
            double lat2 = Math.asin(
                    Math.sin(lat) * Math.cos(distance)
                            + Math.cos(lat) * Math.sin(distance) * Math.cos(bearing)
            );
            double lon2 = lon + Math.atan2(
                    Math.sin(bearing) * Math.sin(distance) * Math.cos(lat),
                    Math.cos(distance) - Math.sin(lat) * Math.sin(lat2)
            );
            points.add(new GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2)));
        }
        return points;
    }

    private void restoreSafeZoneIfAny() {
        SessionStore.SafeZone zone = store.getSafeZone();
        if (zone == null) {
            return;
        }

        GeoPoint center = new GeoPoint(zone.getLat(), zone.getLon());
        safeZoneCenter = center;
        safeZoneRadiusM = zone.getRadiusM();
        drawSafeZone(center, zone.getRadiusM());
    }

    public static class SafeZoneLabelOverlay extends Overlay {
        private final Paint paint = new Paint();
        private GeoPoint center;
        private String text;

        public SafeZoneLabelOverlay(GeoPoint center, String text) {
            this.center = center;
            this.text = text;
            paint.setAntiAlias(true);
            paint.setColor(Color.GREEN);
            paint.setTextSize(42f);
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(4f, 2f, 2f, Color.argb(160, 0, 0, 0));
        }

        public void update(GeoPoint center, String text) {
            this.center = center;
            this.text = text;
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow) {
                return;
            }
            Projection projection = mapView.getProjection();
            android.graphics.Point point = projection.toPixels(center, null);
            canvas.drawText(text, point.x - 140f, point.y - 20f, paint);
        }
    }
}
