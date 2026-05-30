package com.example.blocker.config;

import com.example.blocker.BuildConfig;

public final class ServerConfig {
    private ServerConfig() {
    }

    public static String apiBaseUrlNoSlash() {
        return "http://" + BuildConfig.SISTER_SERVER_HOST + ":" + BuildConfig.SISTER_SERVER_PORT;
    }
}
