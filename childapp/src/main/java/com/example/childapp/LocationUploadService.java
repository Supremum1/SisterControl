package com.example.childapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationUploadService extends Service implements LocationListener {
    private static final String TAG = "GPS_SVC";
    private static final String CHANNEL_ID = "gps_upload";
    private static final int NOTIFICATION_ID = 44;
    private static final String SAFE_CHANNEL_ID = "safe_zone_alert";
    private static final int SAFE_NOTIFICATION_ID = 77;
    private static final long ALERT_COOLDOWN_MS = 60_000L;

    private final ApiService api = ApiService.create();

    private LocationManager locationManager;
    private SessionStore store;
    private long lastAlertAt;
    private boolean wasOutside;

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void onCreate() {
        super.onCreate();
        store = new SessionStore(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            ensureNotificationChannel();
            ensureSafeChannel();
            startForeground(NOTIFICATION_ID, buildNotification());
        } catch (Exception e) {
            Log.e(TAG, "startForeground failed", e);
            stopSelf();
            return;
        }

        try {
            startUpdates();
            sendLastKnownOnce();
        } catch (Exception e) {
            Log.e(TAG, "startUpdates failed", e);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        onNewLocation(location);
    }

    @Override
    public void onDestroy() {
        try {
            locationManager.removeUpdates(this);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID,
                    "GPS Upload",
                    NotificationManager.IMPORTANCE_LOW
            ));
        }
    }

    private void ensureSafeChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager.getNotificationChannel(SAFE_CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(new NotificationChannel(
                    SAFE_CHANNEL_ID,
                    "Safe Zone Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS-трекинг включён")
                .setContentText("Отправка геоданных родителю")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
    }

    private boolean hasLocationPermission() {
        int fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void startUpdates() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "No location permission, stopping");
            stopSelf();
            return;
        }

        boolean gpsEnabled = isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean netEnabled = isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "providers: gps=" + gpsEnabled + " net=" + netEnabled);

        if (!gpsEnabled && !netEnabled) {
            Log.w(TAG, "Location providers disabled -> no updates");
            return;
        }

        if (gpsEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, this);
        }
        if (netEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, this);
        }

        Log.d(TAG, "Location updates requested");
    }

    private boolean isProviderEnabled(String provider) {
        try {
            return locationManager.isProviderEnabled(provider);
        } catch (Exception e) {
            return false;
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void sendLastKnownOnce() {
        if (!hasLocationPermission()) {
            return;
        }

        Location lastGps = getLastKnown(LocationManager.GPS_PROVIDER);
        Location lastNet = getLastKnown(LocationManager.NETWORK_PROVIDER);
        Location best;
        if (lastGps != null && lastNet != null) {
            best = lastGps.getTime() >= lastNet.getTime() ? lastGps : lastNet;
        } else {
            best = lastGps != null ? lastGps : lastNet;
        }

        if (best != null) {
            Log.d(TAG, "Sending lastKnown location immediately");
            onNewLocation(best);
        } else {
            Log.d(TAG, "No lastKnown location available yet");
        }
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnown(String provider) {
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (Exception e) {
            return null;
        }
    }

    private void onNewLocation(Location location) {
        Log.d(TAG, "loc lat=" + location.getLatitude()
                + " lon=" + location.getLongitude()
                + " acc=" + location.getAccuracy()
                + " provider=" + location.getProvider());

        checkSafeZoneAndNotify(location);
        sendLocation(location);
    }

    private void sendLocation(Location location) {
        String token = store.getToken();
        Log.d(TAG, "TRY_SEND lat=" + location.getLatitude()
                + " lon=" + location.getLongitude()
                + " tokenPresent=" + (token != null && !token.isBlank()));

        if (token == null || token.isBlank()) {
            Log.e(TAG, "No child token -> cannot send location");
            return;
        }

        LocationUpdateRequest body = new LocationUpdateRequest(
                location.getLatitude(),
                location.getLongitude(),
                (double) location.getAccuracy(),
                location.getProvider()
        );

        api.sendLocation("Bearer " + token, body).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                Log.d(TAG, "SEND_RESPONSE code=" + response.code() + " body=" + response.body());
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable throwable) {
                Log.e(TAG, "SEND_FAIL", throwable);
            }
        });
    }

    private void checkSafeZoneAndNotify(Location location) {
        SessionStore.SafeZone zone = store.getSafeZone();
        if (zone == null) {
            return;
        }

        float[] distanceResult = new float[1];
        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                zone.getLat(),
                zone.getLon(),
                distanceResult
        );
        double distance = distanceResult[0];
        boolean outsideNow = distance > zone.getRadiusM();
        long now = System.currentTimeMillis();

        if (outsideNow) {
            if (!wasOutside || now - lastAlertAt >= ALERT_COOLDOWN_MS) {
                wasOutside = true;
                lastAlertAt = now;
                showSafeZoneNotification(distance, zone.getRadiusM());
            }
        } else {
            wasOutside = false;
        }
    }

    private void showSafeZoneNotification(double distance, double radius) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, SAFE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Ты ушёл слишком далеко!")
                .setContentText("Вышел за безопасную зону (" + (int) distance + " м > " + (int) radius + " м)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(SAFE_NOTIFICATION_ID, notification);
    }
}
