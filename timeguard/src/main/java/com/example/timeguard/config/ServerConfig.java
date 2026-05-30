package com.example.timeguard.config;

import com.example.timeguard.BuildConfig;

public final class ServerConfig {
    private ServerConfig() {
    }

    public static String apiBaseUrlNoSlash() {
        return "http://" + BuildConfig.SISTER_SERVER_HOST + ":" + BuildConfig.SISTER_SERVER_PORT;
    }
}
