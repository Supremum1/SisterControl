package com.example.childassistant.config;

import com.example.childassistant.BuildConfig;

public final class ServerConfig {
    private ServerConfig() {
    }

    public static String apiBaseUrl() {
        return "http://" + BuildConfig.SISTER_SERVER_HOST + ":" + BuildConfig.SISTER_SERVER_PORT + "/";
    }
}
