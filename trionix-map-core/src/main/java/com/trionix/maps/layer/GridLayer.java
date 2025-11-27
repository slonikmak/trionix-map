package com.trionix.maps.layer;

import com.trionix.maps.MapView;
import com.trionix.maps.internal.util.DistanceUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * A layer that renders a metric grid overlay on the map for scale reference.
 * The grid spacing matches the scale ruler distance and is displayed in screen space,
 * making it easier to perceive distances visually. Grid cells represent equal distances
 * in meters/kilometers (e.g., 10km x 10km cells).
 */
public final class GridLayer extends MapLayer {

    private final Canvas canvas = new Canvas();

    private final ObjectProperty<Color> strokeColor = new SimpleObjectProperty<>(Color.rgb(200, 200, 200, 0.5)) {
        @Override
        protected void invalidated() {
            requestLayerLayout();
        }
    };

    private final DoubleProperty strokeWidth = new SimpleDoubleProperty(1.0) {
        @Override
        protected void invalidated() {
            requestLayerLayout();
        }
    };

    /**
     * Creates a metric grid layer with automatic spacing calculation.
     */
    public GridLayer() {
        getChildren().add(canvas);
        canvas.setManaged(false);
        canvas.setMouseTransparent(true);
    }

    /**
     * Returns the stroke color for grid lines.
     */
    public Color getStrokeColor() {
        return strokeColor.get();
    }

    /**
     * Sets the stroke color for grid lines.
     */
    public void setStrokeColor(Color strokeColor) {
        this.strokeColor.set(strokeColor);
    }

    /**
     * Returns the stroke color property.
     */
    public ObjectProperty<Color> strokeColorProperty() {
        return strokeColor;
    }

    /**
     * Returns the stroke width for grid lines.
     */
    public double getStrokeWidth() {
        return strokeWidth.get();
    }

    /**
     * Sets the stroke width for grid lines.
     */
    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth.set(strokeWidth);
    }

    /**
     * Returns the stroke width property.
     */
    public DoubleProperty strokeWidthProperty() {
        return strokeWidth;
    }

    @Override
    public void layoutLayer(MapView mapView) {
        double width = mapView.getWidth();
        double height = mapView.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.setWidth(width);
        canvas.setHeight(height);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double centerLat = mapView.getCenterLat();
        int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));

        // Set stroke style
        gc.setStroke(getStrokeColor());
        gc.setLineWidth(getStrokeWidth());

        // Calculate grid spacing in pixels based on scale ruler distance
        double mpp = DistanceUtils.metersPerPixel(centerLat, zoomLevel);
        double targetMeters = mpp * 150; // Match scale ruler width
        double niceMeters = DistanceUtils.getNiceDistance(targetMeters);
        
        // Convert nice distance to pixels
        double gridSpacingPixels = niceMeters / mpp;

        // Draw vertical lines (screen-space grid)
        for (double x = 0; x < width; x += gridSpacingPixels) {
            gc.strokeLine(x, 0, x, height);
        }

        // Draw horizontal lines (screen-space grid)
        for (double y = 0; y < height; y += gridSpacingPixels) {
            gc.strokeLine(0, y, width, y);
        }
    }
}
