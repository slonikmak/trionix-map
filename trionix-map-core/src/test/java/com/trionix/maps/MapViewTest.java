package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trionix.maps.internal.MapState;
import com.trionix.maps.internal.projection.Projection;
import java.time.Duration;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class MapViewTest {

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit - stage not needed for these tests
    }

    @Test
    void initializesWithDefaultState() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        assertThat(mapView.getCenterLat()).isEqualTo(0.0);
        assertThat(mapView.getCenterLon()).isEqualTo(0.0);
        assertThat(mapView.getZoom()).isEqualTo(1.0);
        assertThat(mapView.getTileSource()).isEqualTo(TileSource.openStreetMap());
    }

    @Test
    void normalizesCoordinatesAndSupportsBinding() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        SimpleDoubleProperty boundLat = new SimpleDoubleProperty();
        boundLat.bindBidirectional(mapView.centerLatProperty());

        boundLat.set(48.8566);
        assertThat(mapView.getCenterLat()).isEqualTo(48.8566);

        mapView.setCenterLat(Projection.MAX_LATITUDE + 10.0);
        assertThat(mapView.getCenterLat()).isEqualTo(Projection.MAX_LATITUDE);

        mapView.setCenterLon(200.0);
        assertThat(mapView.getCenterLon()).isEqualTo(-160.0);

        mapView.setZoom(-5.0);
        assertThat(mapView.getZoom()).isEqualTo(MapState.DEFAULT_MIN_ZOOM);

        mapView.setZoom(25.0);
        assertThat(mapView.getZoom()).isEqualTo(MapState.DEFAULT_MAX_ZOOM);
    }

    @Test
    void updatesTileSourceProperty() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        TileSource source = TileSource.of(
                "https://tiles.example.test/",
                "MapView-Test",
                Duration.ofSeconds(3),
                Duration.ofSeconds(4));

        mapView.setTileSource(source);

        assertThat(mapView.getTileSource()).isEqualTo(source);
        assertThat(mapView.tileSourceProperty().get()).isEqualTo(source);
    }

    @Test
    void directTileSourcePropertyMutationUsesSameSwitchingPath() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        TileSource source = TileSource.of(
                "https://tiles.example.test/property/",
                "MapView-Property-Test",
                Duration.ofSeconds(6),
                Duration.ofSeconds(7));

        mapView.tileSourceProperty().set(source);

        assertThat(mapView.getTileSource()).isEqualTo(source);
    }

    @Test
    void rejectsTileSourceMutationForCustomRetrieverMode() {
        WaitForAsyncUtils.waitForFxEvents();
        TileRetriever retriever = (zoom, x, y) -> java.util.concurrent.CompletableFuture.completedFuture(null);
        MapView mapView = new MapView(retriever, new InMemoryTileCache(8));

        assertThatThrownBy(() -> mapView.setTileSource(TileSource.openStreetMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("built-in tile pipeline");
    }
}
