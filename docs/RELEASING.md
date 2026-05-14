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

## Release Notes Template

Use this structure for every GitHub Release:

```md
## Summary
- One-line description of this release.

## Changes
- Added: ...
- Changed: ...
- Fixed: ...

## Breaking changes
- None
  or
- <describe migration impact>

## Upgrade
- Maven coordinates:
  - com.github.slonikmak.trionix-map:trionix-map-core:<TAG>
  - com.github.slonikmak.trionix-map:trionix-map-layers:<TAG>
```

## Versioning Policy

- `vX.Y.Z-beta.N`:
  - pre-release builds for active iteration,
  - API can change between beta versions.
- `vX.Y.Z-rc.N`:
  - release candidate for validation,
  - no planned API changes except critical fixes.
- `vX.Y.Z`:
  - stable release,
  - backward compatibility expected within the same major version.

Rules:
- Never reuse an existing tag name.
- Always increment to a new tag for any fix (`...beta.6`, `...rc.2`, etc.).
- If clients already consume a tag, prefer forward-fix with a newer tag instead of rewriting history.

## Rollback and Retag

If a release is bad:

1. Do not force-push or mutate published tag history.
2. Create a fixing commit on `main`.
3. Create a new tag and push it (for example `v0.1.0-beta.7`).
4. Let workflow publish and verify the new tag.
5. Mark the bad release as deprecated:
   - edit release notes and add `DO NOT USE`,
   - link to the replacement tag.

If a tag was pushed by mistake and must be removed before adoption:

```bash
git tag -d v0.1.0-beta.7
git push origin :refs/tags/v0.1.0-beta.7
```

Then create and push the correct new tag.
