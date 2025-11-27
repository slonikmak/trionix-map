package com.trionix.maps.control;

import com.trionix.maps.MapView;
import com.trionix.maps.internal.util.DistanceUtils;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * A control that displays a visual scale ruler indicating the distance represented by a segment
 * on the map at the current zoom level and center latitude.
 */
public final class ScaleRulerControl extends Region {

    private static final double DEFAULT_PREFERRED_WIDTH = 150.0;
    private static final double BAR_HEIGHT = 8.0;
    private static final double TEXT_PADDING = 4.0;

    private final Canvas canvas = new Canvas();
    private final ObjectProperty<MapView> mapView = new SimpleObjectProperty<>(null);
    private final DoubleProperty preferredWidthPixels = new SimpleDoubleProperty(DEFAULT_PREFERRED_WIDTH);

    /**
     * Creates a scale ruler control without a map view. Use {@link #setMapView(MapView)} to bind it.
     */
    public ScaleRulerControl() {
        this(null);
    }

    /**
     * Creates a scale ruler control bound to the specified map view.
     *
     * @param mapView the map view to observe, or null
     */
    public ScaleRulerControl(MapView mapView) {
        getChildren().add(canvas);
        canvas.setManaged(false);

        // Default styling
        setBackground(null);
        setPadding(new Insets(10));

        // Update ruler when map view changes
        this.mapView.addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                oldVal.centerLatProperty().removeListener(this::handleMapChange);
                oldVal.centerLonProperty().removeListener(this::handleMapChange);
                oldVal.zoomProperty().removeListener(this::handleMapChange);
                oldVal.widthProperty().removeListener(this::handleMapChange);
                oldVal.heightProperty().removeListener(this::handleMapChange);
            }
            if (newVal != null) {
                newVal.centerLatProperty().addListener(this::handleMapChange);
                newVal.centerLonProperty().addListener(this::handleMapChange);
                newVal.zoomProperty().addListener(this::handleMapChange);
                newVal.widthProperty().addListener(this::handleMapChange);
                newVal.heightProperty().addListener(this::handleMapChange);
            }
            requestLayout();
        });

        preferredWidthPixels.addListener((obs, oldVal, newVal) -> requestLayout());

        setMapView(mapView);
    }

    /**
     * Returns the map view this control is observing.
     */
    public MapView getMapView() {
        return mapView.get();
    }

    /**
     * Sets the map view this control should observe.
     */
    public void setMapView(MapView mapView) {
        this.mapView.set(mapView);
    }

    /**
     * Returns the map view property.
     */
    public ObjectProperty<MapView> mapViewProperty() {
        return mapView;
    }

    /**
     * Returns the preferred width of the ruler bar in pixels.
     */
    public double getPreferredWidthPixels() {
        return preferredWidthPixels.get();
    }

    /**
     * Sets the preferred width of the ruler bar in pixels.
     */
    public void setPreferredWidthPixels(double preferredWidthPixels) {
        this.preferredWidthPixels.set(preferredWidthPixels);
    }

    /**
     * Returns the preferred width property.
     */
    public DoubleProperty preferredWidthPixelsProperty() {
        return preferredWidthPixels;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets insets = getInsets();
        return getPreferredWidthPixels() + insets.getLeft() + insets.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        // Estimate text height
        Text text = new Text("0 km");
        text.setFont(Font.font(12));
        double textHeight = text.getLayoutBounds().getHeight();

        Insets insets = getInsets();
        return BAR_HEIGHT + TEXT_PADDING + textHeight + insets.getTop() + insets.getBottom();
    }

    @Override
    protected void layoutChildren() {
        MapView mv = getMapView();
        if (mv == null) {
            return;
        }

        Insets insets = getInsets();
        double contentWidth = getWidth() - insets.getLeft() - insets.getRight();
        double contentHeight = getHeight() - insets.getTop() - insets.getBottom();

        if (contentWidth <= 0 || contentHeight <= 0) {
            return;
        }

        canvas.setWidth(contentWidth);
        canvas.setHeight(contentHeight);
        canvas.relocate(insets.getLeft(), insets.getTop());

        renderRuler(mv, contentWidth, contentHeight);
    }

    private void renderRuler(MapView mapView, double width, double height) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double centerLat = mapView.getCenterLat();
        int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));

        // Calculate meters per pixel at center
        double mpp = DistanceUtils.metersPerPixel(centerLat, zoomLevel);

        // Calculate distance for preferred width
        double targetMeters = mpp * getPreferredWidthPixels();

        // Get nice distance value
        double niceMeters = DistanceUtils.getNiceDistance(targetMeters);

        // Calculate actual width in pixels for nice distance
        double actualWidth = niceMeters / mpp;

        // Format distance label
        String label = DistanceUtils.formatDistance(niceMeters);

        // Render checkerboard pattern bar (black and white segments)
        double barY = height - BAR_HEIGHT;
        int segments = 4; // Number of segments in the ruler
        double segmentWidth = actualWidth / segments;
        
        for (int i = 0; i < segments; i++) {
            double x = i * segmentWidth;
            // Alternate between black and white
            gc.setFill(i % 2 == 0 ? Color.BLACK : Color.WHITE);
            gc.fillRect(x, barY, segmentWidth, BAR_HEIGHT);
        }
        
        // Draw border around the ruler
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2.0);
        gc.strokeRect(0, barY, actualWidth, BAR_HEIGHT);

        // Render text with white background for readability
        gc.setFont(Font.font(12));
        Text text = new Text(label);
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        double textX = (actualWidth - textWidth) / 2.0;
        double textY = barY - TEXT_PADDING;
        
        // White background behind text
        gc.setFill(Color.WHITE);
        gc.fillRect(textX - 2, textY - textHeight, textWidth + 4, textHeight + 2);
        
        // Black text
        gc.setFill(Color.BLACK);
        gc.fillText(label, textX, textY);
    }

    private void handleMapChange(javafx.beans.Observable obs) {
        requestLayout();
    }
}
