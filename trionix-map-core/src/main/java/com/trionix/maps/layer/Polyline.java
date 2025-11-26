package com.trionix.maps.layer;

import com.trionix.maps.GeoPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Represents a polyline on the map, consisting of a sequence of geographic coordinates.
 * Supports custom styling, vertex markers, and interactive editing.
 */
public final class Polyline {

    private final List<GeoPoint> points = new ArrayList<>();
    private Color strokeColor = Color.BLUE;
    private double strokeWidth = 2.0;
    private final List<Double> strokeDashArray = new ArrayList<>();
    private boolean markersVisible = false;
    private boolean editable = false;
    private Function<GeoPoint, Node> markerFactory = p -> {
        Circle circle = new Circle(5, Color.RED);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(1);
        return circle;
    };

    PolylineLayer owner;

    /**
     * Creates a new empty polyline.
     */
    public Polyline() {
    }

    /**
     * Creates a new polyline with the given points.
     */
    public Polyline(List<GeoPoint> points) {
        this.points.addAll(points);
    }

    public List<GeoPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public void setPoints(List<GeoPoint> points) {
        this.points.clear();
        this.points.addAll(points);
        notifyUpdate();
    }

    public void addPoint(GeoPoint point) {
        this.points.add(point);
        notifyUpdate();
    }

    public void updatePoint(int index, GeoPoint point) {
        this.points.set(index, point);
        notifyUpdate();
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
        notifyUpdate();
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
        notifyUpdate();
    }

    public List<Double> getStrokeDashArray() {
        return Collections.unmodifiableList(strokeDashArray);
    }

    public void setStrokeDashArray(List<Double> dashArray) {
        this.strokeDashArray.clear();
        if (dashArray != null) {
            this.strokeDashArray.addAll(dashArray);
        }
        notifyUpdate();
    }

    public boolean isMarkersVisible() {
        return markersVisible;
    }

    public void setMarkersVisible(boolean markersVisible) {
        this.markersVisible = markersVisible;
        notifyUpdate();
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        notifyUpdate();
    }

    public Function<GeoPoint, Node> getMarkerFactory() {
        return markerFactory;
    }

    public void setMarkerFactory(Function<GeoPoint, Node> markerFactory) {
        this.markerFactory = Objects.requireNonNull(markerFactory);
        notifyUpdate();
    }

    private void notifyUpdate() {
        if (owner != null) {
            owner.requestLayerLayout();
        }
    }
}
