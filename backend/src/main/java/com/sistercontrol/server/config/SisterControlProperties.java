package com.sistercontrol.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sister-control")
public record SisterControlProperties(
        Security security,
        Ollama ollama,
        Detector detector
) {
    public record Security(int sessionTtlDays, int pairCodeTtlMinutes) {
    }

    public record Ollama(String baseUrl, String model) {
    }

    public record Detector(String baseUrl) {
    }
}
