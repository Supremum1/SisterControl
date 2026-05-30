package com.example.overlaydetector.config;

import com.example.overlaydetector.BuildConfig;

public final class ServerConfig {
    private ServerConfig() {
    }

    public static String apiBaseUrlNoSlash() {
        return "http://" + BuildConfig.SISTER_SERVER_HOST + ":" + BuildConfig.SISTER_SERVER_PORT;
    }

    public static String detectorBaseUrlNoSlash() {
        return "http://" + BuildConfig.SISTER_SERVER_HOST + ":" + BuildConfig.SISTER_DETECTOR_PORT;
    }
}
