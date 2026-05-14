# Releasing `trionix-map`

This project publishes consumer artifacts via **JitPack**.  
The canonical public version for clients is the **Git tag** (for example `v0.1.0-beta.5`).

## Versioning model

- Public dependency versions are tag-based (`v*`).
- GitHub Release must exist for every public tag.
- Workflow now enforces this: on tag push, it creates a Release if missing, builds, uploads assets, and verifies JitPack artifacts.

## What clients should use

Maven:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.slonikmak.trionix-map</groupId>
    <artifactId>trionix-map-core</artifactId>
    <version>v0.1.0-beta.5</version>
  </dependency>
  <dependency>
    <groupId>com.github.slonikmak.trionix-map</groupId>
    <artifactId>trionix-map-layers</artifactId>
    <version>v0.1.0-beta.5</version>
  </dependency>
</dependencies>
```

## Release flow (maintainer)

1. Ensure `main` is green.
2. Create and push a version tag:

```bash
git checkout main
git pull
git tag v0.1.0-beta.6
git push origin v0.1.0-beta.6
```

3. Wait for workflow `Release to GitHub Packages and Releases`.

The workflow does:
- ensures a GitHub Release exists for the tag,
- builds and deploys Maven artifacts,
- uploads release JAR assets,
- warms JitPack and waits until `trionix-map-core` and `trionix-map-layers` POMs return HTTP 200.

## Verification checklist

- Actions run is green.
- Release page exists for the tag.
- JitPack artifacts are resolvable:
  - `https://jitpack.io/com/github/slonikmak/trionix-map/trionix-map-core/<TAG>/trionix-map-core-<TAG>.pom`
  - `https://jitpack.io/com/github/slonikmak/trionix-map/trionix-map-layers/<TAG>/trionix-map-layers-<TAG>.pom`
- Consumer project can run `mvn -U ...` with the new tag.

## Notes

- GitHub Packages may still require auth depending on package visibility and owner policy.
- For public consumption without tokens, JitPack tag versions are the source of truth.
