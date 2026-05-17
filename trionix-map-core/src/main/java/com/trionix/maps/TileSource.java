package com.trionix.maps;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration for the built-in HTTP XYZ tile pipeline.
 */
public record TileSource(String baseUrl, String userAgent, Duration connectTimeout, Duration readTimeout) {

    private static final String DEFAULT_BASE_URL = "https://tile.openstreetmap.org/";
    private static final String DEFAULT_USER_AGENT = "TrionixMapView/0.1 (+https://trionix.example)";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final TileSource OPEN_STREET_MAP = new TileSource(
            DEFAULT_BASE_URL,
            DEFAULT_USER_AGENT,
            DEFAULT_CONNECT_TIMEOUT,
            DEFAULT_READ_TIMEOUT);

    public TileSource {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(userAgent, "userAgent");
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(readTimeout, "readTimeout");
        baseUrl = normalizeBaseUrl(baseUrl);
        URI.create(baseUrl);
    }

    public static TileSource openStreetMap() {
        return OPEN_STREET_MAP;
    }

    public static TileSource of(String baseUrl, String userAgent,
            Duration connectTimeout, Duration readTimeout) {
        return new TileSource(baseUrl, userAgent, connectTimeout, readTimeout);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
