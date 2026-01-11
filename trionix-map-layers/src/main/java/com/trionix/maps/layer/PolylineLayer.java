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
 * A layer that renders polylines on the map. Supports custom styling, vertex
 * markers, and interactive editing.
 */
public final class PolylineLayer extends MapLayer {

    private final ObservableList<com.trionix.maps.layer.Polyline> polylines = FXCollections.observableArrayList();
    private final Map<com.trionix.maps.layer.Polyline, PolylineVisual> visuals = new HashMap<>();
    private final Projection projection = WebMercatorProjection.INSTANCE;

    private Node draggingNode;

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
        LayoutContext ctx = createLayoutContext(mapView);
        if (ctx == null) {
            return;
        }

        for (com.trionix.maps.layer.Polyline polyline : polylines) {
            PolylineVisual visual = visuals.get(polyline);
            if (visual == null) {
                continue;
            }

            updateVisualStyle(polyline, visual);
            updateMarkers(polyline, visual);
            updateLinePoints(polyline, visual, ctx);
            visual.lineNode.toBack();
        }
    }

    private record LayoutContext(
            int zoomLevel,
            double centerX,
            double centerY,
            double halfWidth,
            double halfHeight,
            Projection projection) {
    }

    private LayoutContext createLayoutContext(MapView mapView) {
        if (mapView.getWidth() <= 0 || mapView.getHeight() <= 0) {
            return null;
        }
        int zoomLevel = mapView.getDiscreteZoomLevel();
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        return new LayoutContext(
                zoomLevel,
                centerPixels.x(),
                centerPixels.y(),
                mapView.getWidth() / 2.0,
                mapView.getHeight() / 2.0,
                projection);
    }

    private void updateVisualStyle(com.trionix.maps.layer.Polyline polyline, PolylineVisual visual) {
        visual.lineNode.setStroke(polyline.getStrokeColor());
        visual.lineNode.setStrokeWidth(polyline.getStrokeWidth());
        visual.lineNode.getStrokeDashArray().setAll(polyline.getStrokeDashArray());
    }

    private void updateMarkers(com.trionix.maps.layer.Polyline polyline, PolylineVisual visual) {
        boolean markersNeeded = polyline.isMarkersVisible() || polyline.isEditable();
        List<GeoPoint> points = polyline.getPoints();

        if (!markersNeeded) {
            removeAllMarkers(visual);
            return;
        }

        if (visual.markerNodes.size() != points.size()) {
            rebuildMarkers(polyline, visual, points);
        }
    }

    private void removeAllMarkers(PolylineVisual visual) {
        for (Node m : visual.markerNodes) {
            getChildren().remove(m);
        }
        visual.markerNodes.clear();
    }

    private void rebuildMarkers(com.trionix.maps.layer.Polyline polyline, PolylineVisual visual,
            List<GeoPoint> points) {
        removeAllMarkers(visual);
        for (int i = 0; i < points.size(); i++) {
            Node markerNode = polyline.getMarkerFactory().apply(points.get(i));
            markerNode.setManaged(false);
            installMarkerHandlers(markerNode, polyline, i);
            visual.markerNodes.add(markerNode);
            getChildren().add(markerNode);
        }
    }

    private void updateLinePoints(com.trionix.maps.layer.Polyline polyline, PolylineVisual visual,
            LayoutContext ctx) {
        List<GeoPoint> points = polyline.getPoints();
        visual.lineNode.getPoints().clear();

        boolean markersNeeded = polyline.isMarkersVisible() || polyline.isEditable();

        for (int i = 0; i < points.size(); i++) {
            GeoPoint gp = points.get(i);
            double[] screenPos = toScreenPosition(gp, ctx);

            visual.lineNode.getPoints().addAll(screenPos[0], screenPos[1]);

            if (markersNeeded && i < visual.markerNodes.size()) {
                positionMarker(visual.markerNodes.get(i), screenPos, polyline);
            }
        }
    }

    private double[] toScreenPosition(GeoPoint gp, LayoutContext ctx) {
        Projection.PixelCoordinate pixel = ctx.projection().latLonToPixel(
                gp.latitude(), gp.longitude(), ctx.zoomLevel());
        double screenX = pixel.x() - ctx.centerX() + ctx.halfWidth();
        double screenY = pixel.y() - ctx.centerY() + ctx.halfHeight();
        return new double[] { screenX, screenY };
    }

    private void positionMarker(Node markerNode, double[] screenPos,
            com.trionix.maps.layer.Polyline polyline) {
        boolean showHandle = polyline.isMarkersVisible() || polyline.isEditable();
        markerNode.setVisible(showHandle);
        markerNode.setMouseTransparent(!polyline.isEditable());

        if (markerNode.isVisible()) {
            double w = markerNode.prefWidth(-1);
            double h = markerNode.prefHeight(-1);
            markerNode.resizeRelocate(screenPos[0] - w / 2.0, screenPos[1] - h / 2.0, w, h);
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
            GeoPoint geo = view.sceneToGeoPoint(ev.getSceneX(), ev.getSceneY());
            if (geo != null) {
                polyline.updatePoint(index, geo);
            }
            ev.consume();
        });

        node.addEventHandler(MouseEvent.MOUSE_RELEASED, ev -> {
            if (draggingNode == node) {
                draggingNode = null;
                ev.consume();
            }
        });
    }

    private static class PolylineVisual {
        final javafx.scene.shape.Polyline lineNode = new javafx.scene.shape.Polyline();
        final List<Node> markerNodes = new ArrayList<>();
    }
}
