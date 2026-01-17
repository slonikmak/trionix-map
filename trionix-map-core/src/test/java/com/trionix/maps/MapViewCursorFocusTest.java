package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class MapViewCursorFocusTest {

    private static final double PIXEL_TOLERANCE = 2.0;
    private final Projection projection = WebMercatorProjection.INSTANCE;
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
    void scrollZoomPreservesCursorFocusWithinTwoPixels() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            mapView.getAnimationConfig().setAnimationsEnabled(false);
            mapView.setCenterLat(37.7749);
            mapView.setCenterLon(-122.4194);
            mapView.setZoom(10.0);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Scroll at an off-center position
        double scrollX = 384.0;
        double scrollY = 192.0;

        Projection.LatLon geoPointBefore = latLonAt(mapView, scrollX, scrollY);

        Platform.runLater(() -> mapView.fireEvent(createScrollEvent(120.0, scrollX, scrollY)));
        WaitForAsyncUtils.waitForFxEvents();

        double pixelShift = computePixelShift(mapView, geoPointBefore, scrollX, scrollY);

        assertThat(pixelShift)
                .describedAs("Geographic point should remain within ±2px of cursor during scroll zoom")
                .isLessThanOrEqualTo(PIXEL_TOLERANCE);
    }

    @Test
    void doubleClickZoomPreservesCursorFocusWithinTwoPixels() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            mapView.getAnimationConfig().setAnimationsEnabled(true);
            mapView.getAnimationConfig().setDoubleClickZoomAnimationEnabled(true);
            mapView.getAnimationConfig().setDoubleClickZoomDuration(Duration.millis(250));
            mapView.setCenterLat(37.7749);
            mapView.setCenterLon(-122.4194);
            mapView.setZoom(10.0);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double clickX = 320.0;
        double clickY = 256.0;

        Projection.LatLon geoPointBefore = latLonAt(mapView, clickX, clickY);

        Platform.runLater(() -> mapView.fireEvent(mouseDoubleClick(clickX, clickY)));
        WaitForAsyncUtils.waitForFxEvents();

        double pixelShift = computePixelShift(mapView, geoPointBefore, clickX, clickY);

        assertThat(pixelShift)
                .describedAs("Geographic point should remain within ±2px of cursor during double-click zoom")
                .isLessThanOrEqualTo(PIXEL_TOLERANCE);
    }

    @Test
    void scrollZoomAtMapCenterPreservesCenter() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            mapView.getAnimationConfig().setAnimationsEnabled(false);
            mapView.setCenterLat(40.7128);
            mapView.setCenterLon(-74.0060);
            mapView.setZoom(8.0);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double initialLat = mapView.getCenterLat();
        double initialLon = mapView.getCenterLon();

        Platform.runLater(() -> mapView.fireEvent(createScrollEvent(120.0, 256.0, 256.0)));
        WaitForAsyncUtils.waitForFxEvents();

        double finalLat = mapView.getCenterLat();
        double finalLon = mapView.getCenterLon();
        double finalZoom = mapView.getZoom();

        assertThat(finalLat).isCloseTo(initialLat, within(0.01));
        assertThat(finalLon).isCloseTo(initialLon, within(0.01));
        assertThat(finalZoom).isCloseTo(8.5, within(0.05));
    }

    @Test
    void multipleScrollEventsPreserveCursorFocus() {
        WritableImage tileImage = new WritableImage(256, 256);
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        mount(() -> new MapView(retriever, cache), 512, 512);

        Platform.runLater(() -> {
            mapView.getAnimationConfig().setAnimationsEnabled(false);
            mapView.setCenterLat(51.5074);
            mapView.setCenterLon(-0.1278);
            mapView.setZoom(9.0);
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double scrollX = 400.0;
        double scrollY = 300.0;

        Projection.LatLon geoPoint = latLonAt(mapView, scrollX, scrollY);

        Platform.runLater(() -> {
            mapView.fireEvent(createScrollEvent(120.0, scrollX, scrollY));
            mapView.fireEvent(createScrollEvent(120.0, scrollX, scrollY));
            mapView.fireEvent(createScrollEvent(120.0, scrollX, scrollY));
        });
        WaitForAsyncUtils.waitForFxEvents();

        double pixelShift = computePixelShift(mapView, geoPoint, scrollX, scrollY);

        assertThat(pixelShift)
                .describedAs("Geographic point should remain within ±2px after multiple scroll zooms")
                .isLessThanOrEqualTo(PIXEL_TOLERANCE);
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

    private Projection.LatLon latLonAt(MapView mapView, double sceneX, double sceneY) {
        // Need to access properties on FX thread? MapView properties are usually accessible on any thread
        // if not bound, but for safety lets assume we are calling this in a context where it's safe or we don't care about concurrency for snapshot
        // Actually this method reads width/height/center which are FX properties.
        // But in these tests we often call it after waitForFxEvents, so main thread access is sort of okay if no other changes happen.
        // However, standard JavaFX properties are not thread safe.
        // I'll wrap in runLater and use FutureTask/completable future if I needed result, but here I can't easily.
        // But since we are using WaitForAsyncUtils, the FX thread is idle.
        
        double width = mapView.getWidth();
        double height = mapView.getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return null;
        }
        int zoomLevel = mapView.getZoom() >= 1.0 ? (int) mapView.getZoom() : 1;
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        double offsetX = sceneX - width / 2.0;
        double offsetY = sceneY - height / 2.0;
        double pixelX = centerPixels.x() + offsetX;
        double pixelY = centerPixels.y() + offsetY;
        return projection.pixelToLatLon(pixelX, pixelY, zoomLevel);
    }

    private double computePixelShift(MapView mapView, Projection.LatLon geoPoint, double expectedX, double expectedY) {
        if (geoPoint == null) {
            return Double.MAX_VALUE;
        }
        double width = mapView.getWidth();
        double height = mapView.getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return Double.MAX_VALUE;
        }
        int zoomLevel = mapView.getZoom() >= 1.0 ? (int) mapView.getZoom() : 1;
        Projection.PixelCoordinate geoPixels = projection.latLonToPixel(
                geoPoint.latitude(), geoPoint.longitude(), zoomLevel);
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        double actualX = (geoPixels.x() - centerPixels.x()) + width / 2.0;
        double actualY = (geoPixels.y() - centerPixels.y()) + height / 2.0;
        double dx = actualX - expectedX;
        double dy = actualY - expectedY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static ScrollEvent createScrollEvent(double deltaY, double x, double y) {
        return new ScrollEvent(
                ScrollEvent.SCROLL,
                x,
                y,
                x,
                y,
                false,
                false,
                false,
                false,
                false,
                false,
                0.0,
                deltaY,
                0.0,
                deltaY,
                ScrollEvent.HorizontalTextScrollUnits.NONE,
                0.0,
                ScrollEvent.VerticalTextScrollUnits.NONE,
                0.0,
                0,
                null);
    }

    private static MouseEvent mouseDoubleClick(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                2,
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
