package com.trionix.maps.layer;

import com.trionix.maps.MapView;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

/**
 * Layer for displaying point markers at geographic coordinates. Markers may be draggable and
 * support click callbacks.
 */
public final class PointMarkerLayer extends MapLayer {

    private final List<PointMarker> markers = new ArrayList<>();
    private final Projection projection = new WebMercatorProjection();

    // Track currently dragging marker (only one at a time)
    private PointMarker draggingMarker;

    public PointMarkerLayer() {
        // nothing special
    }

    /**
     * Adds a new marker to this layer and returns the created {@link PointMarker}.
     * The supplied node is adopted into the layer's scene graph and will be positioned
     * during {@link #layoutLayer(MapView)} calls.
     */
    public PointMarker addMarker(double latitude, double longitude, Node node) {
        Objects.requireNonNull(node, "node");
        PointMarker marker = new PointMarker(latitude, longitude, node);
        marker.owner = this;
        installHandlers(marker);
        markers.add(marker);
        // ensure node sits in the layer's scene graph
        if (!getChildren().contains(node)) {
            // marker visuals are positioned explicitly by the layer; avoid parent-managed layout
            node.setManaged(false);
            getChildren().add(node);
        }
        requestLayerLayout();
        return marker;
    }

    /**
     * Removes the marker from the layer if present. Returns {@code true} when the marker was
     * removed, {@code false} when the marker was not known by the layer.
     */
    public boolean removeMarker(PointMarker marker) {
        if (marker == null) {
            return false;
        }
        boolean removed = markers.remove(marker);
        if (removed) {
            getChildren().remove(marker.getNode());
            marker.owner = null;
        }
        return removed;
    }

    /**
     * Removes all markers and their visual nodes from the layer.
     */
    public void clearMarkers() {
        markers.clear();
        getChildren().clear();
        requestLayerLayout();
    }

    /**
     * Returns an unmodifiable view of the markers in this layer.
     */
    public List<PointMarker> getMarkers() {
        return Collections.unmodifiableList(markers);
    }

    @Override
    public void layoutLayer(MapView mapView) {
        if (markers.isEmpty()) {
            return;
        }
        int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        double halfWidth = mapView.getWidth() / 2.0;
        double halfHeight = mapView.getHeight() / 2.0;

        for (PointMarker marker : markers) {
            Node node = marker.getNode();
            if (!marker.isVisible()) {
                node.setVisible(false);
                continue;
            }
            node.setVisible(true);
            Projection.PixelCoordinate pixel = projection.latLonToPixel(
                    marker.getLatitude(), marker.getLongitude(), zoomLevel);

            double screenX = pixel.x() - centerPixels.x() + halfWidth;
            double screenY = pixel.y() - centerPixels.y() + halfHeight;

            double width = node.prefWidth(-1);
            double height = node.prefHeight(-1);
            // center horizontally and align bottom edge with the coordinate
            double layoutX = screenX - width / 2.0;
            double layoutY = screenY - height;

            node.resizeRelocate(layoutX, layoutY, width, height);
        }
    }

    private void installHandlers(PointMarker marker) {
        Node node = marker.getNode();

        node.addEventHandler(MouseEvent.MOUSE_PRESSED, ev -> {
            if (!ev.isPrimaryButtonDown() || !marker.isDraggable() || !marker.isVisible()) {
                return;
            }
            draggingMarker = marker;
            ev.consume();
        });

        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, ev -> {
            if (draggingMarker != marker || !ev.isPrimaryButtonDown()) {
                return;
            }
            MapView view = getMapView();
            if (view == null) {
                return;
            }
            // Convert scene coords to map-local coords then to lat/lon
            var local = view.sceneToLocal(ev.getSceneX(), ev.getSceneY());
            int zoomLevel = Math.max(0, (int) Math.floor(view.getZoom()));
            Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                    view.getCenterLat(), view.getCenterLon(), zoomLevel);
            double offsetX = local.getX() - view.getWidth() / 2.0;
            double offsetY = local.getY() - view.getHeight() / 2.0;
            double pixelX = centerPixels.x() + offsetX;
            double pixelY = centerPixels.y() + offsetY;
            var latlon = projection.pixelToLatLon(pixelX, pixelY, zoomLevel);
            marker.setLocation(latlon.latitude(), latlon.longitude());
            ev.consume();
        });

        node.addEventHandler(MouseEvent.MOUSE_RELEASED, ev -> {
            if (draggingMarker == marker) {
                draggingMarker = null;
                ev.consume();
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> {
            if (!marker.isVisible()) {
                return;
            }
            // If we've been dragging, ignore click
            if (draggingMarker != null) {
                return;
            }
            var handler = marker.getOnClick();
            if (handler != null) {
                handler.accept(marker);
                ev.consume();
            }
        });
    }
}
