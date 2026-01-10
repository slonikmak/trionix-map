package com.trionix.maps.layer;

/**
 * Listener for marker property changes.
 * <p>
 * This functional interface provides a callback mechanism for notification when
 * a marker's properties (such as location) change. It decouples the marker from
 * direct knowledge of the layer, following the Dependency Inversion Principle.
 */
@FunctionalInterface
public interface MarkerChangeListener {

    /**
     * Called when a marker's properties change and the layer should re-layout.
     *
     * @param marker the marker that changed
     */
    void onMarkerChanged(PointMarker marker);
}
