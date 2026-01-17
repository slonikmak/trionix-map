package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.internal.MapState;
import com.trionix.maps.internal.projection.Projection;
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
}
