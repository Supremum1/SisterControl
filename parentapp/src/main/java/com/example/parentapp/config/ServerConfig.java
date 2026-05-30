package com.example.parentapp.config;

import com.example.parentapp.BuildConfig;

public final class ServerConfig {
    private ServerConfig() {
    }

    public static String apiBaseUrl() {
        return "http://" + BuildConfig.SISTER_SERVER_HOST + ":" + BuildConfig.SISTER_SERVER_PORT + "/";
    }

    public static String apiBaseUrlNoSlash() {
        return trimTrailingSlash(apiBaseUrl());
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
