package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import javafx.stage.Stage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@ExtendWith(ApplicationExtension.class)
class SimpleOsmTileRetrieverTest {

    private byte[] pngBytes;
    private MockWebServer webServer;

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit
    }

    @BeforeEach
    void setUp() throws IOException {
        try (InputStream stream = SimpleOsmTileRetrieverTest.class
                .getResourceAsStream("/com/trionix/maps/placeholder-tile.png")) {
            if (stream == null) {
                throw new IOException("Missing placeholder tile resource");
            }
            pngBytes = stream.readAllBytes();
        }
        webServer = new MockWebServer();
        webServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        webServer.shutdown();
        pngBytes = null;
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
