package com.example.childapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionStore {
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PERMISSIONS_ONBOARDING_DONE = "permissions_onboarding_done";

    private final SharedPreferences prefs;

    public SessionStore(Context context) {
        prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE);
    }

    public void saveDisplayName(String name) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim()).apply();
    }

    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, null);
    }

    public void saveToken(String token) {
        prefs.edit().putString("token", token).apply();
    }

    public String getToken() {
        return prefs.getString("token", null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public void saveUser(String email) {
        prefs.edit().putString("user_email", email).apply();
    }

    public String getUserEmail() {
        return prefs.getString("user_email", null);
    }

    public void saveSafeZone(double lat, double lon, double radiusM) {
        prefs.edit()
                .putFloat("safe_lat", (float) lat)
                .putFloat("safe_lon", (float) lon)
                .putFloat("safe_radius", (float) radiusM)
                .putBoolean("safe_enabled", true)
                .apply();
    }

    public void clearSafeZone() {
        prefs.edit()
                .putBoolean("safe_enabled", false)
                .apply();
    }

    public SafeZone getSafeZone() {
        boolean enabled = prefs.getBoolean("safe_enabled", false);
        if (!enabled) {
            return null;
        }
        double lat = prefs.getFloat("safe_lat", 0f);
        double lon = prefs.getFloat("safe_lon", 0f);
        double radius = prefs.getFloat("safe_radius", 0f);
        if (radius <= 0) {
            return null;
        }
        return new SafeZone(lat, lon, radius);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public void setPermissionsOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_PERMISSIONS_ONBOARDING_DONE, done).apply();
    }

    public boolean isPermissionsOnboardingDone() {
        return prefs.getBoolean(KEY_PERMISSIONS_ONBOARDING_DONE, false);
    }

    public static class SafeZone {
        private final double lat;
        private final double lon;
        private final double radiusM;

        public SafeZone(double lat, double lon, double radiusM) {
            this.lat = lat;
            this.lon = lon;
            this.radiusM = radiusM;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public double getRadiusM() {
            return radiusM;
        }
    }
}
