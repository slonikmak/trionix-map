package com.trionix.maps.samples;

import com.trionix.maps.MapView;
import com.trionix.maps.layer.MapLayer;
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

        MarkerLayer markerLayer = new MarkerLayer();
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
        marker.setMouseTransparent(true);
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

    /**
     * Simple layer that positions {@link Region} markers using Web Mercator math.
     */
    private static final class MarkerLayer extends MapLayer {
        private final Projection projection = new WebMercatorProjection();
        private final List<Marker> markers = new ArrayList<>();

        void addMarker(double latitude, double longitude, Region node) {
            Objects.requireNonNull(node, "node");
            node.setManaged(false);
            getChildren().add(node);
            markers.add(new Marker(latitude, longitude, node));
            requestLayerLayout();
        }

        @Override
        public void layoutLayer(MapView mapView) {
            if (markers.isEmpty()) {
                return;
            }
            double width = getWidth();
            double height = getHeight();
            if (width <= 0.0 || height <= 0.0) {
                return;
            }
            int zoomLevel = (int) Math.round(mapView.getZoom());
            Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                    mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
            double halfWidth = width / 2.0;
            double halfHeight = height / 2.0;

            for (Marker marker : markers) {
                Projection.PixelCoordinate markerPixels = projection.latLonToPixel(
                        marker.latitude(), marker.longitude(), zoomLevel);
                double screenX = markerPixels.x() - centerPixels.x() + halfWidth;
                double screenY = markerPixels.y() - centerPixels.y() + halfHeight;
                Region node = marker.node();
                double markerWidth = node.prefWidth(-1);
                double markerHeight = node.prefHeight(-1);
                node.resizeRelocate(screenX - markerWidth / 2.0,
                        screenY - markerHeight,
                        markerWidth,
                        markerHeight);
            }
        }

        private record Marker(double latitude, double longitude, Region node) {
        }
    }
}
