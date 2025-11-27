package com.trionix.maps.control;

import com.trionix.maps.MapView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScaleRulerControlTest {

    @Test
    void constructor_withoutMapView_succeeds() {
        var control = new ScaleRulerControl();
        assertNull(control.getMapView());
    }

    @Test
    void constructor_withMapView_bindsToMap() {
        var mapView = new MapView();
        var control = new ScaleRulerControl(mapView);
        assertEquals(mapView, control.getMapView());
    }

    @Test
    void setMapView_updatesProperty() {
        var control = new ScaleRulerControl();
        var mapView = new MapView();

        control.setMapView(mapView);
        assertEquals(mapView, control.getMapView());
    }

    @Test
    void setMapView_null_unbinds() {
        var mapView = new MapView();
        var control = new ScaleRulerControl(mapView);

        control.setMapView(null);
        assertNull(control.getMapView());
    }

    @Test
    void setPreferredWidthPixels_updatesProperty() {
        var control = new ScaleRulerControl();
        control.setPreferredWidthPixels(200.0);
        assertEquals(200.0, control.getPreferredWidthPixels());
    }

    @Test
    void layoutChildren_withoutMapView_doesNotThrow() {
        var control = new ScaleRulerControl();
        control.resize(200, 50);
        assertDoesNotThrow(control::layout);
    }

    @Test
    void layoutChildren_withMapView_doesNotThrow() {
        var mapView = new MapView();
        mapView.resize(800, 600);
        mapView.setCenterLat(50.0);
        mapView.setCenterLon(10.0);
        mapView.setZoom(5);

        var control = new ScaleRulerControl(mapView);
        control.resize(200, 50);

        assertDoesNotThrow(control::layout);
    }

    @Test
    void mapViewChanges_triggerLayout() {
        var mapView = new MapView();
        mapView.resize(800, 600);
        mapView.setZoom(5);

        var control = new ScaleRulerControl(mapView);
        control.resize(200, 50);
        control.layout();

        // Change zoom
        mapView.setZoom(10);
        mapView.layout();

        // Should not throw
        assertDoesNotThrow(control::layout);
    }

    @Test
    void computePrefWidth_includesInsets() {
        var control = new ScaleRulerControl();
        control.setPadding(new javafx.geometry.Insets(10));
        
        double prefWidth = control.prefWidth(-1);
        assertTrue(prefWidth > control.getPreferredWidthPixels());
    }

    @Test
    void computePrefHeight_isReasonable() {
        var control = new ScaleRulerControl();
        double prefHeight = control.prefHeight(-1);
        assertTrue(prefHeight > 0 && prefHeight < 100);
    }
}
