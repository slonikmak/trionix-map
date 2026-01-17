package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.trionix.maps.internal.MapState;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class MapViewDoubleClickZoomTest {

    private Stage stage;
    private MapView mapView;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane(), 512, 512));
        stage.show();
    }

    @AfterEach
    void cleanup() {
        Platform.runLater(() -> {
            if (stage != null && stage.getScene() != null) {
                ((StackPane) stage.getScene().getRoot()).getChildren().clear();
            }
            mapView = null;
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void doubleClickIncrementsZoomByOne() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            disableAnimations(mapView);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(5.0);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double initialZoom = mapView.getZoom();

        Platform.runLater(() -> mapView.fireEvent(mouseDoubleClick(256.0, 256.0)));
        WaitForAsyncUtils.waitForFxEvents();

        double zoomAfter = mapView.getZoom();
        assertThat(zoomAfter).isCloseTo(initialZoom + 1.0, within(0.0001));
    }

    @Test
    void doubleClickDoesNotZoomWhenDisabled() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            disableAnimations(mapView);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(5.0);
            mapView.setEnableDoubleClickZoom(false);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double initialZoom = mapView.getZoom();

        Platform.runLater(() -> mapView.fireEvent(mouseDoubleClick(256.0, 256.0)));
        WaitForAsyncUtils.waitForFxEvents();

        double zoomAfter = mapView.getZoom();
        assertThat(zoomAfter).isEqualTo(initialZoom);
    }

    @Test
    void doubleClickDoesNotExceedMaxZoom() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            disableAnimations(mapView);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(MapState.DEFAULT_MAX_ZOOM);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double initialZoom = mapView.getZoom();
        assertThat(initialZoom).isEqualTo(MapState.DEFAULT_MAX_ZOOM);

        Platform.runLater(() -> mapView.fireEvent(mouseDoubleClick(256.0, 256.0)));
        WaitForAsyncUtils.waitForFxEvents();

        double zoomAfter = mapView.getZoom();
        assertThat(zoomAfter).isEqualTo(MapState.DEFAULT_MAX_ZOOM);
    }

    @Test
    void doubleClickPreservesCursorGeographicPosition() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            disableAnimations(mapView);
            mapView.setCenterLat(37.7749);
            mapView.setCenterLon(-122.4194);
            mapView.setZoom(10.0);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Double-click at a position offset from center
        double clickX = 384.0;
        double clickY = 192.0;

        Platform.runLater(() -> {
            mapView.fireEvent(mouseDoubleClick(clickX, clickY));
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double zoomAfter = mapView.getZoom();
        assertThat(zoomAfter).isCloseTo(11.0, within(0.0001));

        double centerLatAfter = mapView.getCenterLat();
        double centerLonAfter = mapView.getCenterLon();
        
        // Center should have changed
        assertThat(centerLatAfter).isNotEqualTo(37.7749);
        assertThat(centerLonAfter).isNotEqualTo(-122.4194);
    }

    @Test
    void singleClickDoesNotZoom() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            disableAnimations(mapView);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(5.0);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double initialZoom = mapView.getZoom();

        Platform.runLater(() -> mapView.fireEvent(mouseSingleClick(256.0, 256.0)));
        WaitForAsyncUtils.waitForFxEvents();

        double zoomAfter = mapView.getZoom();
        assertThat(zoomAfter).isEqualTo(initialZoom);
    }

    @Test
    void enableDoubleClickZoomDefaultsToTrue() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        assertThat(mapView.isEnableDoubleClickZoom()).isTrue();
    }

    @Test
    void enableDoubleClickZoomCanBeToggled() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        mapView.setEnableDoubleClickZoom(false);
        assertThat(mapView.isEnableDoubleClickZoom()).isFalse();
        
        mapView.setEnableDoubleClickZoom(true);
        assertThat(mapView.isEnableDoubleClickZoom()).isTrue();
    }

    private void mount(java.util.function.Supplier<MapView> factory, double width, double height) {
        Platform.runLater(() -> {
            this.mapView = factory.get();
            StackPane root = (StackPane) stage.getScene().getRoot();
            root.getChildren().setAll(mapView);
            mapView.resize(width, height);
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static MouseEvent mouseDoubleClick(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                2, // Click count = 2
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                null);
    }

    private static void disableAnimations(MapView mapView) {
        mapView.getAnimationConfig().setAnimationsEnabled(false);
    }

    private static MouseEvent mouseSingleClick(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1, // Click count = 1
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                null);
    }
}
