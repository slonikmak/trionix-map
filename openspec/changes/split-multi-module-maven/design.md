# Design: Multi-Module Maven Structure

## Context
The project needs to separate library distribution from demo applications. The library depends on JavaFX for its UI components (`MapView extends Region`), but should not force specific JavaFX versions on consumers. Consumers may use different JavaFX versions or platform classifiers.

## Goals
- Clean library artifact without demo code
- JavaFX as `provided` dependency (not bundled transitively)
- Parent POM with zero dependencies (only dependency management and plugin management)
- Demo module as standalone examples with full JavaFX runtime

## Non-Goals
- Creating a fat JAR or modular JAR with JavaFX bundled
- Supporting non-JavaFX rendering backends
- Changing the public API

## Decisions

### Module Structure
```
trionix-map/                    # Parent (pom packaging)
├── pom.xml                     # dependencyManagement, pluginManagement only
├── trionix-map-core/           # Library module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/trionix/maps/  # All library code
│       └── test/java/com/trionix/maps/  # All tests
└── trionix-map-demo/           # Demo module
    ├── pom.xml
    └── src/
        └── main/java/com/trionix/maps/samples/
```

**Rationale**: Standard Maven multi-module layout. Each module has single responsibility.

### Dependency Scopes

| Dependency | Parent | Core | Demo |
|------------|--------|------|------|
| JavaFX (base, graphics, controls) | `<dependencyManagement>` | `provided` | `compile` |
| SLF4J API | `<dependencyManagement>` | `compile` | (inherited) |
| JUnit, AssertJ, MockWebServer | `<dependencyManagement>` | `test` | - |

**Rationale**: 
- `provided` in core means consumers must supply JavaFX at runtime (gives flexibility for platform selection)
- Demo module uses `compile` scope with platform classifier for runnable examples
- Parent only manages versions, never declares dependencies directly

### Parent POM Structure
```xml
<project>
    <groupId>com.trionix</groupId>
    <artifactId>trionix-map</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>trionix-map-core</module>
        <module>trionix-map-demo</module>
    </modules>
    
    <properties><!-- versions --></properties>
    <dependencyManagement><!-- all deps with versions --></dependencyManagement>
    <build>
        <pluginManagement><!-- plugins with versions --></pluginManagement>
    </build>
</project>
```

### Alternatives Considered

1. **Keep single module with classifier-based publishing**: Rejected because it doesn't separate demo from library.

2. **Create separate repository for demos**: Rejected as overkill for MVP; single repo with modules is simpler.

3. **Use `optional` instead of `provided` for JavaFX**: Rejected because `provided` more clearly indicates runtime requirement and doesn't appear in transitive dependencies.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Build complexity increases | Clear module responsibilities, documented in README |
| Demo won't run without JavaFX platform classifier | Profile activation based on OS (existing pattern) |
| IDE may need reimport | Document in tasks.md |

## Migration Plan

1. Create parent POM with `pom` packaging
2. Create `trionix-map-core` directory and move library source/test
3. Create `trionix-map-demo` directory and move samples
4. Update paths in scripts (`run-examples.ps1`, `run-examples.sh`)
5. Verify build: `mvn clean verify`
6. Verify demos run correctly

## Open Questions
None - scope is well-defined.
