package com.trionix.maps.layer;

import com.trionix.maps.GeoPoint;
import com.trionix.maps.MapView;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

/**
 * A layer that renders polylines on the map. Supports custom styling, vertex markers, and interactive editing.
 */
public final class PolylineLayer extends MapLayer {

    private final ObservableList<com.trionix.maps.layer.Polyline> polylines = FXCollections.observableArrayList();
    private final Map<com.trionix.maps.layer.Polyline, PolylineVisual> visuals = new HashMap<>();
    private final Projection projection = new WebMercatorProjection();

    private Node draggingNode;
    private com.trionix.maps.layer.Polyline draggingPolyline;
    private int draggingVertexIndex = -1;

    public PolylineLayer() {
        polylines.addListener((ListChangeListener<com.trionix.maps.layer.Polyline>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (com.trionix.maps.layer.Polyline p : change.getRemoved()) {
                        removeVisual(p);
                        p.owner = null;
                    }
                }
                if (change.wasAdded()) {
                    for (com.trionix.maps.layer.Polyline p : change.getAddedSubList()) {
                        p.owner = this;
                        createVisual(p);
                    }
                }
            }
            requestLayerLayout();
        });
    }

    public ObservableList<com.trionix.maps.layer.Polyline> getPolylines() {
        return polylines;
    }

    public void addPolyline(com.trionix.maps.layer.Polyline polyline) {
        polylines.add(polyline);
    }

    public void removePolyline(com.trionix.maps.layer.Polyline polyline) {
        polylines.remove(polyline);
    }

    @Override
    public void layoutLayer(MapView mapView) {
        int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        double halfWidth = mapView.getWidth() / 2.0;
        double halfHeight = mapView.getHeight() / 2.0;

        for (com.trionix.maps.layer.Polyline polyline : polylines) {
            PolylineVisual visual = visuals.get(polyline);
            if (visual == null) {
                continue; // Should have been created by listener
            }

            // Update style
            visual.lineNode.setStroke(polyline.getStrokeColor());
            visual.lineNode.setStrokeWidth(polyline.getStrokeWidth());
            visual.lineNode.getStrokeDashArray().setAll(polyline.getStrokeDashArray());

            // Update points
            List<GeoPoint> points = polyline.getPoints();
            visual.lineNode.getPoints().clear();
            
            // Rebuild markers if needed (count mismatch or visibility change)
            boolean markersNeeded = polyline.isMarkersVisible() || polyline.isEditable();
            if (!markersNeeded) {
                for (Node m : visual.markerNodes) {
                    getChildren().remove(m);
                }
                visual.markerNodes.clear();
            } else {
                // If count differs, rebuild all (simplest approach)
                if (visual.markerNodes.size() != points.size()) {
                    for (Node m : visual.markerNodes) {
                        getChildren().remove(m);
                    }
                    visual.markerNodes.clear();
                    for (int i = 0; i < points.size(); i++) {
                        Node markerNode = polyline.getMarkerFactory().apply(points.get(i));
                        markerNode.setManaged(false);
                        installMarkerHandlers(markerNode, polyline, i);
                        visual.markerNodes.add(markerNode);
                        getChildren().add(markerNode);
                    }
                }
            }

            // Position points and markers
            for (int i = 0; i < points.size(); i++) {
                GeoPoint gp = points.get(i);
                Projection.PixelCoordinate pixel = projection.latLonToPixel(
                        gp.latitude(), gp.longitude(), zoomLevel);
                double screenX = pixel.x() - centerPixels.x() + halfWidth;
                double screenY = pixel.y() - centerPixels.y() + halfHeight;

                visual.lineNode.getPoints().addAll(screenX, screenY);

                if (markersNeeded && i < visual.markerNodes.size()) {
                    Node markerNode = visual.markerNodes.get(i);
                    // Update marker visibility based on polyline state
                    // If editable but markers not visible, we might still want to show handles?
                    // Spec says: "WHEN a Polyline is set to non-editable ... markers (if visible) do not respond"
                    // Spec also says: "Enable vertex markers ... visual marker is rendered"
                    // So if markersVisible=false but editable=true, do we show markers?
                    // Usually yes, as handles. But let's stick to markersVisible flag for visibility.
                    // Wait, if editable=true and markersVisible=false, user can't see what to drag.
                    // I'll assume markers must be visible to be dragged, OR editable implies visible handles.
                    // Let's respect markersVisible for now. If user wants editable, they should probably enable markers or I should force them.
                    // But spec separates them.
                    // Let's show markers if markersVisible is true.
                    
                    markerNode.setVisible(polyline.isMarkersVisible());
                    
                    if (markerNode.isVisible()) {
                        double w = markerNode.prefWidth(-1);
                        double h = markerNode.prefHeight(-1);
                        markerNode.resizeRelocate(screenX - w / 2.0, screenY - h / 2.0, w, h);
                    }
                }
            }
            
            // Ensure line is at the bottom of this layer's children so markers are on top
            visual.lineNode.toBack();
        }
    }

    private void createVisual(com.trionix.maps.layer.Polyline polyline) {
        PolylineVisual visual = new PolylineVisual();
        visual.lineNode.setManaged(false);
        visual.lineNode.setMouseTransparent(true); // Line itself not interactive for now
        getChildren().add(visual.lineNode);
        visuals.put(polyline, visual);
    }

    private void removeVisual(com.trionix.maps.layer.Polyline polyline) {
        PolylineVisual visual = visuals.remove(polyline);
        if (visual != null) {
            getChildren().remove(visual.lineNode);
            getChildren().removeAll(visual.markerNodes);
        }
    }

    private void installMarkerHandlers(Node node, com.trionix.maps.layer.Polyline polyline, int index) {
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, ev -> {
            if (!ev.isPrimaryButtonDown() || !polyline.isEditable()) {
                return;
            }
            draggingNode = node;
            draggingPolyline = polyline;
            draggingVertexIndex = index;
            ev.consume();
        });

        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, ev -> {
            if (draggingNode != node || !ev.isPrimaryButtonDown()) {
                return;
            }
            MapView view = getMapView();
            if (view == null) {
                return;
            }
            
            var local = view.sceneToLocal(ev.getSceneX(), ev.getSceneY());
            int zoomLevel = Math.max(0, (int) Math.floor(view.getZoom()));
            Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                    view.getCenterLat(), view.getCenterLon(), zoomLevel);
            double offsetX = local.getX() - view.getWidth() / 2.0;
            double offsetY = local.getY() - view.getHeight() / 2.0;
            double pixelX = centerPixels.x() + offsetX;
            double pixelY = centerPixels.y() + offsetY;
            var latlon = projection.pixelToLatLon(pixelX, pixelY, zoomLevel);
            
            GeoPoint newPoint = GeoPoint.of(latlon.latitude(), latlon.longitude());
            polyline.updatePoint(index, newPoint);
            ev.consume();
        });

        node.addEventHandler(MouseEvent.MOUSE_RELEASED, ev -> {
            if (draggingNode == node) {
                draggingNode = null;
                draggingPolyline = null;
                draggingVertexIndex = -1;
                ev.consume();
            }
        });
    }

    private static class PolylineVisual {
        final javafx.scene.shape.Polyline lineNode = new javafx.scene.shape.Polyline();
        final List<Node> markerNodes = new ArrayList<>();
    }
}
