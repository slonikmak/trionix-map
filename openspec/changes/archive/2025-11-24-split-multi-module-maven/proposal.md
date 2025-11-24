# Change: Split project into multi-module Maven structure

## Why
The current single-module project mixes library code with demo applications and bundles JavaFX dependencies transitively. Splitting into modules enables:
- Clean library distribution without demo code
- Users can provide their own JavaFX version/platform
- Parent POM manages versions without declaring dependencies
- Better separation of concerns between library and examples

## What Changes
- Convert root `pom.xml` to parent POM with `pom` packaging and no dependencies
- Create `trionix-map-core` module containing library code (`com.trionix.maps.*` except samples)
- Create `trionix-map-demo` module containing sample applications
- Move JavaFX dependencies to `provided` scope in core module
- Demo module declares JavaFX with compile scope for runtime

## Impact
- Affected files: `pom.xml` (convert to parent), new `trionix-map-core/pom.xml`, new `trionix-map-demo/pom.xml`
- Affected code: Move `src/main/java/com/trionix/maps/samples/` to demo module
- Affected tests: Tests remain in core module (they test library behavior)
- **Non-breaking**: No public API changes, only build structure reorganization
