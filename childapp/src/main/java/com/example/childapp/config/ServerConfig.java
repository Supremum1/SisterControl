package com.example.childapp.config;

import com.example.childapp.BuildConfig;

public final class ServerConfig {
    private ServerConfig() {
    }

    public static String apiBaseUrl() {
        return "http://" + BuildConfig.SISTER_SERVER_HOST + ":" + BuildConfig.SISTER_SERVER_PORT + "/";
    }
}
