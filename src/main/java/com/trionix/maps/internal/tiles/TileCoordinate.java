package com.trionix.maps.internal.tiles;

/**
 * Immutable representation of a single tile in slippy-map coordinates.
 */
public record TileCoordinate(int zoom, long x, long y) {
}
