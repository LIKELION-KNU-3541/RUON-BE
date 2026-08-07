package com.springboot.ruon.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Vision API 연동 설정.
 */
@ConfigurationProperties(prefix = "google.vision")
public record VisionProperties(
        String apiKey,
        String endpoint,
        Duration connectTimeout,
        Duration readTimeout) {

    private static final String DEFAULT_ENDPOINT = "https://vision.googleapis.com/v1/images:annotate";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    public VisionProperties {
        if (apiKey == null) {
            apiKey = "";
        }
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = DEFAULT_ENDPOINT;
        }
        if (connectTimeout == null) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (readTimeout == null) {
            readTimeout = DEFAULT_READ_TIMEOUT;
        }
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }
}
