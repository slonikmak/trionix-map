package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trionix.maps.testing.FxTestHarness;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleOsmTileRetrieverTest {

    private static byte[] pngBytes;

    private MockWebServer webServer;

    @BeforeAll
    static void initializeToolkitAndLoadImage() throws IOException {
        FxTestHarness.runOnFxThread(() -> {
        });
        try (InputStream stream = SimpleOsmTileRetrieverTest.class
                .getResourceAsStream("/com/trionix/maps/placeholder-tile.png")) {
            if (stream == null) {
                throw new IOException("Missing placeholder tile resource");
            }
            pngBytes = stream.readAllBytes();
        }
    }

    @AfterAll
    static void cleanup() {
        pngBytes = null;
    }

    @BeforeEach
    void setUp() throws IOException {
        webServer = new MockWebServer();
        webServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        webServer.shutdown();
    }

    @Test
    void loadsTileFromMockServer() throws ExecutionException, InterruptedException {
        @SuppressWarnings("resource")
        Buffer buffer = new Buffer().write(pngBytes);
        webServer.enqueue(new MockResponse().setResponseCode(200).setBody(buffer));
        SimpleOsmTileRetriever retriever = new SimpleOsmTileRetriever(
                webServer.url("/tiles/").toString(),
                "JUnit-Test",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));

        var image = retriever.loadTile(1, 0, 0).get();
        assertThat(image.getWidth()).isEqualTo(256.0);
        assertThat(image.getHeight()).isEqualTo(256.0);

        var recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getRequestUrl().encodedPath()).isEqualTo("/tiles/1/0/0.png");
        assertThat(recordedRequest.getHeader("User-Agent")).isEqualTo("JUnit-Test");
    }

    @Test
    void propagatesHttpErrors() {
        webServer.enqueue(new MockResponse().setResponseCode(404));
        SimpleOsmTileRetriever retriever = new SimpleOsmTileRetriever(
                webServer.url("/").toString(),
                "JUnit-Test",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));

        assertThatThrownBy(() -> retriever.loadTile(0, 0, 0).get())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected HTTP status");
    }
}
