package com.trionix.maps;

import java.util.Objects;
import javafx.animation.Interpolator;
import javafx.util.Duration;

/**
 * Centralized configuration for MapView animations (fly-to, scroll zoom, double-click zoom,
 * and touch zoom gestures).
 */
public final class MapAnimationConfig {

    private boolean animationsEnabled = true;
    private boolean flyToAnimationEnabled = true;
    private boolean scrollZoomAnimationEnabled = true;
    private boolean doubleClickZoomAnimationEnabled = true;
    private boolean touchZoomAnimationEnabled = true;
    private boolean pinchMomentumEnabled = true;

    private Duration scrollZoomDuration = Duration.millis(180);
    private Duration doubleClickZoomDuration = Duration.millis(240);
    private Duration pinchGestureAnimationDuration = Duration.millis(75);
    private Duration pinchMomentumDuration = Duration.millis(150);

    private Interpolator flyToInterpolator = Interpolator.EASE_BOTH;
    private Interpolator scrollZoomInterpolator = Interpolator.EASE_OUT;
    private Interpolator doubleClickZoomInterpolator = Interpolator.EASE_BOTH;
    private Interpolator pinchZoomInterpolator = Interpolator.EASE_OUT;
    private Interpolator pinchMomentumInterpolator = Interpolator.EASE_OUT;

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }

    public boolean isFlyToAnimationEnabled() {
        return flyToAnimationEnabled;
    }

    public void setFlyToAnimationEnabled(boolean flyToAnimationEnabled) {
        this.flyToAnimationEnabled = flyToAnimationEnabled;
    }

    public boolean isScrollZoomAnimationEnabled() {
        return scrollZoomAnimationEnabled;
    }

    public void setScrollZoomAnimationEnabled(boolean scrollZoomAnimationEnabled) {
        this.scrollZoomAnimationEnabled = scrollZoomAnimationEnabled;
    }

    public boolean isDoubleClickZoomAnimationEnabled() {
        return doubleClickZoomAnimationEnabled;
    }

    public void setDoubleClickZoomAnimationEnabled(boolean doubleClickZoomAnimationEnabled) {
        this.doubleClickZoomAnimationEnabled = doubleClickZoomAnimationEnabled;
    }

    public boolean isTouchZoomAnimationEnabled() {
        return touchZoomAnimationEnabled;
    }

    public void setTouchZoomAnimationEnabled(boolean touchZoomAnimationEnabled) {
        this.touchZoomAnimationEnabled = touchZoomAnimationEnabled;
    }

    public boolean isPinchMomentumEnabled() {
        return pinchMomentumEnabled;
    }

    public void setPinchMomentumEnabled(boolean pinchMomentumEnabled) {
        this.pinchMomentumEnabled = pinchMomentumEnabled;
    }

    public Duration getScrollZoomDuration() {
        return scrollZoomDuration;
    }

    public void setScrollZoomDuration(Duration scrollZoomDuration) {
        this.scrollZoomDuration = validateDuration(scrollZoomDuration, "scrollZoomDuration");
    }

    public Duration getDoubleClickZoomDuration() {
        return doubleClickZoomDuration;
    }

    public void setDoubleClickZoomDuration(Duration doubleClickZoomDuration) {
        this.doubleClickZoomDuration = validateDuration(doubleClickZoomDuration, "doubleClickZoomDuration");
    }

    public Duration getPinchGestureAnimationDuration() {
        return pinchGestureAnimationDuration;
    }

    public void setPinchGestureAnimationDuration(Duration pinchGestureAnimationDuration) {
        this.pinchGestureAnimationDuration = validateDuration(pinchGestureAnimationDuration, "pinchGestureAnimationDuration");
    }

    public Duration getPinchMomentumDuration() {
        return pinchMomentumDuration;
    }

    public void setPinchMomentumDuration(Duration pinchMomentumDuration) {
        this.pinchMomentumDuration = validateDuration(pinchMomentumDuration, "pinchMomentumDuration");
    }

    public Interpolator getFlyToInterpolator() {
        return flyToInterpolator;
    }

    public void setFlyToInterpolator(Interpolator flyToInterpolator) {
        this.flyToInterpolator = validateInterpolator(flyToInterpolator, "flyToInterpolator");
    }

    public Interpolator getScrollZoomInterpolator() {
        return scrollZoomInterpolator;
    }

    public void setScrollZoomInterpolator(Interpolator scrollZoomInterpolator) {
        this.scrollZoomInterpolator = validateInterpolator(scrollZoomInterpolator, "scrollZoomInterpolator");
    }

    public Interpolator getDoubleClickZoomInterpolator() {
        return doubleClickZoomInterpolator;
    }

    public void setDoubleClickZoomInterpolator(Interpolator doubleClickZoomInterpolator) {
        this.doubleClickZoomInterpolator = validateInterpolator(doubleClickZoomInterpolator, "doubleClickZoomInterpolator");
    }

    public Interpolator getPinchZoomInterpolator() {
        return pinchZoomInterpolator;
    }

    public void setPinchZoomInterpolator(Interpolator pinchZoomInterpolator) {
        this.pinchZoomInterpolator = validateInterpolator(pinchZoomInterpolator, "pinchZoomInterpolator");
    }

    public Interpolator getPinchMomentumInterpolator() {
        return pinchMomentumInterpolator;
    }

    public void setPinchMomentumInterpolator(Interpolator pinchMomentumInterpolator) {
        this.pinchMomentumInterpolator = validateInterpolator(pinchMomentumInterpolator, "pinchMomentumInterpolator");
    }

    private static Duration validateDuration(Duration duration, String label) {
        Objects.requireNonNull(duration, label);
        if (duration.lessThan(Duration.ZERO)) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
        if (duration.isUnknown() || duration.isIndefinite()) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        return duration;
    }

    private static Interpolator validateInterpolator(Interpolator interpolator, String label) {
        return Objects.requireNonNull(interpolator, label);
    }
}
