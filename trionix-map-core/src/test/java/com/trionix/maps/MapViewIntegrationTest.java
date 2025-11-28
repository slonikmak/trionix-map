package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.trionix.maps.internal.MapState;
import com.trionix.maps.internal.tiles.PlaceholderTileFactory;
import com.trionix.maps.internal.tiles.TileCoordinate;
import com.trionix.maps.layer.MapLayer;
import com.trionix.maps.testing.FxTestHarness;
import com.trionix.maps.testing.MapViewTestHarness;
import com.trionix.maps.testing.MapViewTestHarness.MountedMapView;
import com.trionix.maps.testing.RecordingTileRetriever;
import com.trionix.maps.testing.RecordingTileRetriever.LoadRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits;
import javafx.scene.input.ScrollEvent.VerticalTextScrollUnits;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class MapViewIntegrationTest {

    @Test
    void requestsVisibleTilesWhenViewportSized() throws Exception {
        RecordingTileRetriever retriever = new RecordingTileRetriever();
        InMemoryTileCache cache = new InMemoryTileCache(256);

        try (MountedMapView mounted = MapViewTestHarness.mount(
                () -> new MapView(retriever, cache),
                512,
                512,
                view -> {
                    view.setCenterLat(37.7749);
                    view.setCenterLon(-122.4194);
                    view.setZoom(3.0);
                })) {
            mounted.layout();

            MapState expectedState = new MapState();
            expectedState.setCenterLat(37.7749);
            expectedState.setCenterLon(-122.4194);
            expectedState.setZoom(3.0);
            expectedState.setViewportSize(512.0, 512.0);
            List<TileCoordinate> expectedTiles = expectedState.visibleTiles();

                List<LoadRequest> requests = retriever.awaitRequests(expectedTiles.size(), Duration.ofSeconds(1));
                assertThat(requests)
                    .extracting(LoadRequest::coordinate)
                    .containsExactlyInAnyOrderElementsOf(expectedTiles);

            WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
            requests.forEach(request -> request.future().complete(tileImage));
            mounted.flushFx();
        }
    }

    @Test
    void rendersPlaceholderWhenTileLoadFails() throws Exception {
        RecordingTileRetriever retriever = new RecordingTileRetriever();
        InMemoryTileCache cache = new InMemoryTileCache(32);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 256, 256)) {
            mounted.layout();

            MapState state = new MapState();
            state.setCenterLat(0.0);
            state.setCenterLon(0.0);
            state.setZoom(mounted.mapView().getZoom());
            state.setViewportSize(256.0, 256.0);
            List<TileCoordinate> expectedTiles = state.visibleTiles();

            List<LoadRequest> requests = retriever.awaitRequests(expectedTiles.size(), Duration.ofSeconds(1));
            RuntimeException failure = new RuntimeException("intentional failure");
            requests.forEach(request -> request.future().completeExceptionally(failure));
            mounted.layout();

            WritableImage snapshot = mounted.snapshot();
            Image placeholder = PlaceholderTileFactory.placeholder();
            Color expectedColor = FxTestHarness.callOnFxThread(() -> colorAt(placeholder, 10, 10));
            Color actualColor = FxTestHarness.callOnFxThread(() -> colorAt(snapshot, 128, 128));
            assertThat(actualColor.getRed()).isCloseTo(expectedColor.getRed(), within(0.01));
            assertThat(actualColor.getGreen()).isCloseTo(expectedColor.getGreen(), within(0.01));
            assertThat(actualColor.getBlue()).isCloseTo(expectedColor.getBlue(), within(0.01));
        }
    }

    @Test
    void panZoomLoopMaintainsHighFrameRate() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(512);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                view.setCenterLat(0.0);
                view.setCenterLon(0.0);
                view.setZoom(3.0);
            });
            mounted.layout();

            int frames = 90;
            long start = System.nanoTime();
            for (int i = 0; i < frames; i++) {
                double dragOffset = 40.0 * Math.sin(i * 0.25);
                double dragX = 256.0 + dragOffset;
                double dragY = 256.0 - dragOffset;
                double scrollDelta = (i % 2 == 0) ? 120.0 : -120.0;
                FxTestHarness.runOnFxThread(() -> {
                    MapView view = mounted.mapView();
                    view.fireEvent(mousePressed(256.0, 256.0));
                    view.fireEvent(mouseDragged(dragX, dragY));
                    view.fireEvent(mouseReleased(dragX, dragY));
                    view.fireEvent(scrollEvent(scrollDelta, 256.0, 256.0));
                });
                mounted.layout();
            }
            long elapsed = System.nanoTime() - start;
            double seconds = elapsed / 1_000_000_000.0;
            double fps = frames / seconds;
            assertThat(fps).as("pan/zoom fps (%s)", fps).isGreaterThan(45.0);
        }
    }

    @Test
    void layersReceiveLifecycleCallbacks() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(64);
        TrackingLayer layer = new TrackingLayer();

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 400, 300)) {
            FxTestHarness.runOnFxThread(() -> mounted.mapView().getLayers().add(layer));
            mounted.layout();

            FxTestHarness.runOnFxThread(layer::requestLayerLayout);
            mounted.layout();

            FxTestHarness.runOnFxThread(() -> mounted.mapView().getLayers().remove(layer));
            mounted.flushFx();
        }

        assertThat(layer.addedCount).isEqualTo(1);
        assertThat(layer.removedCount).isEqualTo(1);
        assertThat(layer.layoutCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    void mouseDragUpdatesCenterCoordinates() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                view.setCenterLat(0.0);
                view.setCenterLon(0.0);
                view.setZoom(2.0);
            });
            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                view.fireEvent(mousePressed(256.0, 256.0));
                view.fireEvent(mouseDragged(356.0, 256.0));
                view.fireEvent(mouseReleased(356.0, 256.0));
            });

            double lat = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLat());
            double lon = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLon());
            assertThat(lat).isCloseTo(0.0, within(0.0001));
            assertThat(lon).isLessThan(0.0);
        }
    }

    @Test
    void scrollZoomAdjustsZoomLevelAndRequestsNewTiles() throws Exception {
        RecordingTileRetriever retriever = new RecordingTileRetriever();
        InMemoryTileCache cache = new InMemoryTileCache(256);
        double initialZoom = 2.8;
        double centerLat = 12.3;
        double centerLon = -45.6;

        try (MountedMapView mounted = MapViewTestHarness.mount(
                () -> new MapView(retriever, cache),
                512,
                512,
                view -> {
                    view.getAnimationConfig().setAnimationsEnabled(false);
                    view.setCenterLat(centerLat);
                    view.setCenterLon(centerLon);
                    view.setZoom(initialZoom);
                })) {
            mounted.layout();

            MapState baseState = new MapState();
            baseState.setCenterLat(centerLat);
            baseState.setCenterLon(centerLon);
            baseState.setZoom(initialZoom);
            baseState.setViewportSize(512.0, 512.0);
            List<TileCoordinate> baseTiles = baseState.visibleTiles();

            WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
            List<LoadRequest> initialRequests = retriever.awaitRequests(baseTiles.size(), Duration.ofSeconds(1));
            initialRequests.forEach(request -> request.future().complete(tileImage));

            FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(scrollEvent(120.0, 256.0, 256.0)));

            double zoomAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());
            assertThat(zoomAfter).isCloseTo(initialZoom + 0.5, within(0.0001));

            MapState zoomState = new MapState();
            zoomState.setCenterLat(centerLat);
            zoomState.setCenterLon(centerLon);
            zoomState.setZoom(zoomAfter);
            zoomState.setViewportSize(512.0, 512.0);
            List<TileCoordinate> zoomTiles = zoomState.visibleTiles();
                assertThat(zoomState.discreteZoomLevel()).isGreaterThan(baseState.discreteZoomLevel());

                List<LoadRequest> zoomRequests = retriever.awaitRequests(zoomTiles.size(), Duration.ofSeconds(1));
                assertThat(zoomRequests)
                    .extracting(LoadRequest::coordinate)
                    .containsExactlyInAnyOrderElementsOf(zoomTiles);
            zoomRequests.forEach(request -> request.future().complete(tileImage));
            mounted.flushFx();
        }
    }

    private static MouseEvent mousePressed(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_PRESSED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                true,
                null);
    }

    private static MouseEvent mouseDragged(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                null);
    }

    private static MouseEvent mouseReleased(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_RELEASED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1,
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

    private static ScrollEvent scrollEvent(double deltaY, double x, double y) {
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
                HorizontalTextScrollUnits.NONE,
                0.0,
                VerticalTextScrollUnits.NONE,
                0.0,
                0,
                null);
    }

    private static Color colorAt(Image image, int x, int y) {
        PixelReader reader = image.getPixelReader();
        return reader.getColor(x, y);
    }

    private static final class TrackingLayer extends MapLayer {
        private int addedCount;
        private int removedCount;
        private int layoutCount;

        @Override
        public void layerAdded(MapView mapView) {
            addedCount++;
        }

        @Override
        public void layerRemoved(MapView mapView) {
            removedCount++;
        }

        @Override
        public void layoutLayer(MapView mapView) {
            layoutCount++;
        }
    }
}
