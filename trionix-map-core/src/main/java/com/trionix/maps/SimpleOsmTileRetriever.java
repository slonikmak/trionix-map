package com.trionix.maps;

import com.trionix.maps.internal.concurrent.TileExecutors;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import javafx.scene.image.Image;

/**
 * Default {@link TileRetriever} that pulls PNG tiles from tile.openstreetmap.org.
 * <p>
 * Uses asynchronous HTTP calls with direct JavaFX Image decoding for optimal performance.
 * Concurrent requests are limited via {@link TileExecutors#concurrencyLimiter()} to avoid
 * overwhelming the tile server.
 */
public final class SimpleOsmTileRetriever implements TileRetriever {

    private static final String DEFAULT_BASE_URL = "https://tile.openstreetmap.org/";
    private static final String DEFAULT_USER_AGENT = "TrionixMapView/0.1 (+https://trionix.example)";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private final URI baseUri;
    private final String userAgent;
    private final Duration readTimeout;
    private final HttpClient httpClient;
    private final Semaphore concurrencyLimiter;

    public SimpleOsmTileRetriever() {
        this(DEFAULT_BASE_URL, DEFAULT_USER_AGENT, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public SimpleOsmTileRetriever(String baseUrl, String userAgent, Duration connectTimeout, Duration readTimeout) {
        this.baseUri = sanitizeBaseUrl(baseUrl);
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        this.concurrencyLimiter = TileExecutors.concurrencyLimiter();
        Duration connect = Objects.requireNonNull(connectTimeout, "connectTimeout");
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connect)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public CompletableFuture<Image> loadTile(int zoom, long x, long y) {
        URI tileUri = baseUri.resolve(pathFor(zoom, x, y));
        HttpRequest request = HttpRequest.newBuilder(tileUri)
                .GET()
                .timeout(readTimeout)
                .header("User-Agent", userAgent)
                .build();

        return CompletableFuture.runAsync(() -> {
            try {
                concurrencyLimiter.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TileRetrievalException("Interrupted while waiting for permit", e);
            }
        }, TileExecutors.tileExecutor())
        .thenCompose(ignored -> httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()))
        .thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new TileRetrievalException(
                        "Unexpected HTTP status " + response.statusCode() + " for tile " + tileUri);
            }
            return new Image(new ByteArrayInputStream(response.body()));
        })
        .whenComplete((result, error) -> concurrencyLimiter.release());
    }

    private static URI sanitizeBaseUrl(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        String normalized = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return URI.create(normalized);
    }

    private static String pathFor(int zoom, long x, long y) {
        return zoom + "/" + x + "/" + y + ".png";
    }

    private static final class TileRetrievalException extends RuntimeException {
        TileRetrievalException(String message) {
            super(message);
        }

        TileRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
