package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.internal.MapState;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.testing.FxTestHarness;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.Test;

class MapViewTest {

    @Test
    void initializesWithDefaultState() {
        FxTestHarness.runOnFxThread(() -> {
            MapView mapView = new MapView();
            assertThat(mapView.getCenterLat()).isEqualTo(0.0);
            assertThat(mapView.getCenterLon()).isEqualTo(0.0);
            assertThat(mapView.getZoom()).isEqualTo(1.0);
        });
    }

    @Test
    void normalizesCoordinatesAndSupportsBinding() {
        FxTestHarness.runOnFxThread(() -> {
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
        });
    }
}
