# Change Proposal: 2025-11-27-add-double-click-zoom

## Summary
Add double-click (primary mouse button) zoom-in interaction to `MapView`, centered on the cursor location, as a fast alternative to scroll and pinch gestures. This improves discoverability and aligns with common map UX conventions (OSM, Google Maps, etc.).

## Motivation
Current `MapView` supports scroll-wheel and pinch zoom. Users expect double-click to quickly zoom in without precise wheel control—especially on devices with coarse wheels or touchpads. Adding this lightweight interaction increases usability with minimal complexity.

## Scope
Focused solely on zoom-in via double primary-click within the map viewport. No multi-button variants, no double-right-click, no modifier-based zoom-out (these can be future enhancements). Animation behavior reuses existing zoom/animation system (no new animation spec in this change).

## Out of Scope / Deferred
- Double-click zoom-out (e.g., with modifier key) – future change.
- Customizable double-click zoom delta beyond a simple property.
- Gesture conflict resolution with specialized layers consuming double-click.

## Impacted Specs
- `map-view-control` (ADDED requirement for double-click zoom interaction)

## Why
Users expect quick, discoverable zoom controls on maps (double-click is a long-standing convention). This change closes a usability gap by offering a fast, low-complexity zoom-in option that leverages existing animation and zoom logic.

## What Changes
- Add a new double-click zoom behavior requirement to the `map-view-control` spec (delta under `openspec/changes/add-double-click-zoom/specs/map-view-control/spec.md`).
- Implement an event handler on `MapView` to detect primary-button double-clicks and invoke existing zoom semantics; add an optional configuration flag to enable/disable the behavior.
- Add unit tests to verify zooming behavior, max-zoom clamping, center preservation, and event consumption.

## Architectural Notes
Simple event handler added to `MapView` to detect double-click and invoke existing zoom logic. No threading changes. Cursor geographic point preserved as focal point (matching scroll zoom semantics). Optional enable flag keeps feature configurable.

## Risks / Mitigations
- Risk: Interference with layer-specific double-click actions. Mitigation: allow layers to consume the event first; only trigger zoom if not consumed.
- Risk: Over-zoom at max level. Mitigation: existing zoom clamping applies.

## Validation Strategy
Unit tests covering:
1. Zoom level increments on double-click.
2. No change when already at max zoom.
3. Center adjustment keeps clicked geo point under cursor (within tolerance).
4. Event ignored when feature disabled.

## Alternatives Considered
- Implement configurable double-click zoom delta now (rejected: adds complexity). Use fixed +1 level.
- Use animated smooth zoom vs. discrete step (deferred: rely on existing animation if already present, otherwise instantaneous).

## Decision
Proceed with minimal ADD requirement and implementation tasks as outlined; extensibility reserved for future changes.
