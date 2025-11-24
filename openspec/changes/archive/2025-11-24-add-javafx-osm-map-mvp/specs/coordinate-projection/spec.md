# Coordinate Projection

## ADDED Requirements

### Requirement: Projection Interface
The system SHALL define an internal `Projection` interface for coordinate transformations.

#### Scenario: Define projection contract
- **WHEN** the system implements a projection
- **THEN** it provides `latLonToPixel(double lat, double lon, int zoom)` returning global pixel coordinates and `pixelToLatLon(double pixelX, double pixelY, int zoom)` returning lat/lon

### Requirement: Web Mercator Projection
The system SHALL implement Web Mercator (EPSG:3857) projection for tile coordinate calculations.

#### Scenario: Convert lat/lon to global pixel coordinates
- **WHEN** `latLonToPixel(0.0, 0.0, 1)` is called
- **THEN** it returns pixel coordinates (256.0, 256.0) representing the equator/prime meridian at zoom level 1

#### Scenario: Convert global pixel coordinates to lat/lon
- **WHEN** `pixelToLatLon(256.0, 256.0, 1)` is called
- **THEN** it returns approximately (0.0, 0.0)

#### Scenario: Handle high zoom level
- **WHEN** `latLonToPixel(48.8566, 2.3522, 18)` is called for Paris
- **THEN** it returns global pixel coordinates within the range [0, 256 * 2^18)

### Requirement: Tile Coordinate Calculation
The system SHALL convert global pixel coordinates to tile coordinates.

#### Scenario: Calculate tile coordinates from global pixels
- **WHEN** the system needs to display a region at zoom 12 centered at (48.8566, 2.3522)
- **THEN** it calculates the corresponding tile x and y indices by dividing global pixel coordinates by 256

#### Scenario: Determine visible tile range
- **WHEN** the viewport is 800×600 pixels, centered at (40.7128, -74.0060) at zoom 10
- **THEN** the system calculates the set of tile coordinates (z=10, x in range, y in range) covering the viewport plus a small margin

### Requirement: Screen Coordinate Conversion
The system SHALL convert lat/lon to screen (viewport) pixel coordinates.

#### Scenario: Project lat/lon to screen position
- **WHEN** a layer needs to position a marker at (51.5074, -0.1278) with the map centered at (51.5074, -0.1278) and zoom 12
- **THEN** the marker's screen position is calculated as the viewport center (width/2, height/2)

#### Scenario: Project off-screen lat/lon
- **WHEN** a layer projects a lat/lon outside the current viewport
- **THEN** the screen coordinates may be negative or exceed viewport dimensions (clipping is the layer's responsibility)

### Requirement: Latitude Clamping
The system SHALL enforce Web Mercator latitude limits.

#### Scenario: Clamp latitude to ~85 degrees
- **WHEN** a latitude of 90.0 is used in projection calculations
- **THEN** it is clamped to approximately 85.05112878 (the Web Mercator singularity)

#### Scenario: Clamp negative latitude
- **WHEN** a latitude of -90.0 is used
- **THEN** it is clamped to approximately -85.05112878

### Requirement: Longitude Normalization
The system SHALL normalize longitude to the [-180, 180] range.

#### Scenario: Wrap positive longitude
- **WHEN** a longitude of 200.0 is normalized
- **THEN** it wraps to -160.0

#### Scenario: Wrap negative longitude
- **WHEN** a longitude of -200.0 is normalized
- **THEN** it wraps to 160.0

#### Scenario: Handle multiple wraps
- **WHEN** a longitude of 540.0 is normalized
- **THEN** it wraps to -180.0

### Requirement: Tile Size Constant
The system SHALL use a fixed 256×256 pixel tile size for all calculations.

#### Scenario: Calculate tiles per axis at zoom 0
- **WHEN** tile calculations are performed at zoom level 0
- **THEN** there is 1 tile horizontally and 1 tile vertically (world in a single 256×256 tile)

#### Scenario: Calculate tiles per axis at zoom 10
- **WHEN** tile calculations are performed at zoom level 10
- **THEN** there are 2^10 = 1024 tiles horizontally and 1024 tiles vertically
