package com.trionix.maps.layer;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.Node;

/**
 * Represents a single map marker with geographic coordinates and a visual node.
 *
 * <p>
 * Markers are lightweight value objects which hold their geographic location
 * and a visual
 * {@link javafx.scene.Node} that will be positioned by {@link PointMarkerLayer}
 * during layout.
 */
public final class PointMarker {

    private double latitude;
    private double longitude;
    private final Node node;
    private boolean draggable = false;
    private boolean visible = true;
    private Consumer<PointMarker> onClick;
    private Consumer<PointMarker> onLocationChanged;
    private MarkerChangeListener changeListener;

    // owner set by PointMarkerLayer when marker is added (kept for backward
    // compatibility)
    PointMarkerLayer owner;

    PointMarker(double latitude, double longitude, Node node) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.node = Objects.requireNonNull(node, "node");
    }

    /**
     * Returns the current latitude (degrees) for this marker.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the current longitude (degrees) for this marker.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the visual node used to render this marker. The node is owned by the
     * layer's
     * scene graph and must not be shared across different layers.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Returns whether the marker will respond to mouse drag gestures to update its
     * location.
     */
    public boolean isDraggable() {
        return draggable;
    }

    /**
     * Enables or disables interactive dragging for this marker.
     */
    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    /**
     * Returns whether the marker is currently visible and participates in layout
     * and events.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Shows or hides the marker. When hidden the node is set non-interactive and
     * not shown.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        node.setVisible(visible);
        node.setMouseTransparent(!visible);
    }

    /**
     * Registers a click callback that will receive this marker instance when the
     * visual node is
     * clicked. A {@code null} handler removes any existing callback.
     */
    public void setOnClick(Consumer<PointMarker> handler) {
        this.onClick = handler;
    }

    /**
     * Registers a callback invoked when this marker's geographic location changes.
     * The handler receives this marker instance and may be {@code null} to remove
     * the callback.
     */
    public void setOnLocationChanged(Consumer<PointMarker> handler) {
        this.onLocationChanged = handler;
    }

    /**
     * Returns the current click handler for this marker, or {@code null} if none is
     * registered.
     */
    public Consumer<PointMarker> getOnClick() {
        return onClick;
    }

    /**
     * Sets the change listener for this marker. Package-private for use by the
     * layer.
     */
    void setChangeListener(MarkerChangeListener listener) {
        this.changeListener = listener;
    }

    /**
     * Updates the geographic coordinates for this marker. The owning
     * {@link PointMarkerLayer} (if present) will be requested to re-layout so the
     * visual node
     * is moved on the next pulse.
     */
    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (changeListener != null) {
            changeListener.onMarkerChanged(this);
        }
        if (owner != null) {
            owner.requestLayerLayout();
        }
        if (onLocationChanged != null) {
            onLocationChanged.accept(this);
        }
    }
}
