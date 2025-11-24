package com.trionix.maps;

import com.trionix.maps.internal.concurrent.TileExecutors;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/** Default {@link TileRetriever} that pulls PNG tiles from tile.openstreetmap.org. */
public final class SimpleOsmTileRetriever implements TileRetriever {

    private static final String DEFAULT_BASE_URL = "https://tile.openstreetmap.org/";
    private static final String DEFAULT_USER_AGENT = "TrionixMapView/0.1 (+https://trionix.example)";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    private final URI baseUri;
    private final String userAgent;
    private final Duration readTimeout;
    private final HttpClient httpClient;

    public SimpleOsmTileRetriever() {
        this(DEFAULT_BASE_URL, DEFAULT_USER_AGENT, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public SimpleOsmTileRetriever(String baseUrl, String userAgent, Duration connectTimeout, Duration readTimeout) {
        this.baseUri = sanitizeBaseUrl(baseUrl);
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        Duration connect = Objects.requireNonNull(connectTimeout, "connectTimeout");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connect)
                .executor(TileExecutors.tileExecutor())
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
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new TileRetrievalException(
                                "Unexpected HTTP status " + response.statusCode() + " for tile " + tileUri);
                    }
                    return decodeImage(response.body(), tileUri);
                });
    }

    private static Image decodeImage(byte[] bytes, URI tileUri) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            var buffered = ImageIO.read(stream);
            if (buffered == null) {
                throw new TileRetrievalException("Unable to decode tile " + tileUri);
            }
            return SwingFXUtils.toFXImage(buffered, null);
        } catch (IOException e) {
            throw new TileRetrievalException("Failed to decode tile " + tileUri, e);
        }
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
