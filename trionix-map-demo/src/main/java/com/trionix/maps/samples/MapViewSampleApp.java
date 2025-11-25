package com.trionix.maps.samples;

import com.trionix.maps.MapView;
import com.trionix.maps.layer.PointMarker;
import com.trionix.maps.layer.PointMarkerLayer;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Minimal JavaFX application that embeds {@link MapView} and overlays a simple marker layer.
 */
public final class MapViewSampleApp extends Application {

    private static final double DEFAULT_LAT = 37.7749; // San Francisco
    private static final double DEFAULT_LON = -122.4194;

    @Override
    public void start(Stage stage) {
        MapView mapView = new MapView();
        mapView.setPrefSize(960.0, 640.0);
        mapView.setCenterLat(DEFAULT_LAT);
        mapView.setCenterLon(DEFAULT_LON);
        mapView.setZoom(12.0);

        PointMarkerLayer markerLayer = new PointMarkerLayer();
        markerLayer.addMarker(DEFAULT_LAT, DEFAULT_LON, createMarker("San Francisco"));
        markerLayer.addMarker(37.8199, -122.4783, createMarker("Golden Gate Bridge"));
        mapView.getLayers().add(markerLayer);

        StackPane root = new StackPane(mapView);
        Scene scene = new Scene(root, 960.0, 640.0);
        stage.setTitle("MapView Sample");
        stage.setScene(scene);
        stage.show();
    }

    private static Region createMarker(String label) {
        Label marker = new Label(label);
        marker.setPadding(new Insets(4, 8, 4, 8));
        marker.setAlignment(Pos.CENTER);
        marker.setStyle("-fx-background-color: rgba(217,30,54,0.95);"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 12;"
                + "-fx-font-weight: bold;");
        return marker;
    }

    /**
     * Launches the sample.
     */
    public static void main(String[] args) {
        launch(args);
    }

    // Using library PointMarkerLayer; demo's custom MarkerLayer removed
}
