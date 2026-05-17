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
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.image.Image;

/**
 * Default {@link TileRetriever} that pulls PNG tiles from tile.openstreetmap.org.
 * <p>
 * Uses asynchronous HTTP calls with direct JavaFX Image decoding for optimal performance.
 * Concurrent requests are limited via {@link TileExecutors#concurrencyLimiter()} to avoid
 * overwhelming the tile server.
 */
public final class SimpleOsmTileRetriever implements TileRetriever {

    private final Semaphore concurrencyLimiter;
    private final AtomicReference<RuntimeConfig> runtimeConfig;

    public SimpleOsmTileRetriever() {
        this(TileSource.openStreetMap());
    }

    public SimpleOsmTileRetriever(TileSource tileSource) {
        this.concurrencyLimiter = TileExecutors.concurrencyLimiter();
        this.runtimeConfig = new AtomicReference<>(buildRuntimeConfig(tileSource));
    }

    public SimpleOsmTileRetriever(String baseUrl, String userAgent, Duration connectTimeout, Duration readTimeout) {
        this(TileSource.of(baseUrl, userAgent, connectTimeout, readTimeout));
    }

    public TileSource getTileSource() {
        return runtimeConfig.get().tileSource();
    }

    public void setTileSource(TileSource tileSource) {
        runtimeConfig.set(buildRuntimeConfig(tileSource));
    }

    @Override
    public CompletableFuture<Image> loadTile(int zoom, long x, long y) {
        RuntimeConfig config = runtimeConfig.get();
        URI tileUri = config.baseUri().resolve(pathFor(zoom, x, y));
        HttpRequest request = HttpRequest.newBuilder(tileUri)
                .GET()
                .timeout(config.tileSource().readTimeout())
                .header("User-Agent", config.tileSource().userAgent())
                .build();

        return CompletableFuture.supplyAsync(() -> {
            boolean acquired = false;
            try {
                concurrencyLimiter.acquire();
                acquired = true;
                HttpResponse<byte[]> response = config.httpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    throw new TileRetrievalException(
                            "Unexpected HTTP status " + response.statusCode() + " for tile " + tileUri);
                }

                Image image = new Image(new ByteArrayInputStream(response.body()));
                if (image.isError()) {
                    Throwable exception = image.getException();
                    throw new TileRetrievalException(
                            "Failed to decode tile " + tileUri,
                            exception instanceof Exception ex ? ex : null);
                }
                return image;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TileRetrievalException("Interrupted while waiting for permit", e);
            } catch (TileRetrievalException e) {
                throw e;
            } catch (Exception e) {
                throw new TileRetrievalException("Failed to load tile " + tileUri, e);
            } finally {
                if (acquired) {
                    concurrencyLimiter.release();
                }
            }
        }, TileExecutors.tileExecutor());
    }

    private static RuntimeConfig buildRuntimeConfig(TileSource tileSource) {
        TileSource source = Objects.requireNonNull(tileSource, "tileSource");
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(source.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new RuntimeConfig(source, URI.create(source.baseUrl()), httpClient);
    }

    private static String pathFor(int zoom, long x, long y) {
        return zoom + "/" + x + "/" + y + ".png";
    }

    private record RuntimeConfig(TileSource tileSource, URI baseUri, HttpClient httpClient) {
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
