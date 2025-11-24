# Tasks: Split Multi-Module Maven

## 1. Parent POM Setup
- [x] 1.1 Convert root `pom.xml` to parent POM with `pom` packaging
- [x] 1.2 Move all dependencies to `<dependencyManagement>` section
- [x] 1.3 Move plugin configurations to `<pluginManagement>` section
- [x] 1.4 Add `<modules>` section listing core and demo

## 2. Core Module Creation
- [x] 2.1 Create `trionix-map-core/` directory
- [x] 2.2 Create `trionix-map-core/pom.xml` inheriting from parent
- [x] 2.3 Move `src/main/java/com/trionix/maps/` (except samples) to core module
- [x] 2.4 Move `src/test/java/com/trionix/maps/` to core module
- [x] 2.5 Move `src/main/resources/` to core module (if exists)
- [x] 2.6 Declare JavaFX dependencies with `provided` scope in core POM
- [x] 2.7 Declare test dependencies in core POM

## 3. Demo Module Creation
- [x] 3.1 Create `trionix-map-demo/` directory
- [x] 3.2 Create `trionix-map-demo/pom.xml` inheriting from parent
- [x] 3.3 Move `src/main/java/com/trionix/maps/samples/` to demo module
- [x] 3.4 Add dependency on `trionix-map-core` in demo POM
- [x] 3.5 Declare JavaFX dependencies with `compile` scope and platform classifier in demo POM
- [x] 3.6 Copy OS-based profiles for JavaFX platform selection to demo POM

## 4. Cleanup & Verification
- [x] 4.1 Remove old `src/` directory from root (after confirming moves)
- [x] 4.2 Update `run-examples.ps1` with new module paths
- [x] 4.3 Update `run-examples.sh` with new module paths
- [x] 4.4 Run `mvn clean verify` from root to confirm build
- [x] 4.5 Run demo applications to verify they work
- [x] 4.6 Update README.md with new project structure

## Dependencies
- Tasks 2.x and 3.x can be done in parallel after 1.x
- Task 4.x depends on completion of 1.x, 2.x, and 3.x
