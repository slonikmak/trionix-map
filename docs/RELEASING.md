# Release Process

This document describes how to release a new version of `trionix-map`. The release process is partially automated using GitHub Actions, but requires manual version bumping and tag creation.

## Prerequisites

- Write access to the GitHub repository.
- `mvn` installed locally (optional, for verification).

## Step-by-Step Guide

### 1. Bump Version

Before creating a release, you must update the version number in all `pom.xml` files.

1.  **Update the parent `pom.xml`**:
    Edit `pom.xml` in the root directory and update the `<version>`:
    ```xml
    <version>0.1.0-beta.2</version> <!-- Example -->
    ```

2.  **Update modules**:
    Ensure the child modules (`trionix-map-core`, `trionix-map-layers`, `trionix-map-demo`) also have their parent version updated.
    
    *Typically, you can use the `versions-maven-plugin` to handle this locally:*
    ```bash
    mvn versions:set -DnewVersion=0.1.0-beta.2
    mvn versions:commit
    ```
    *Or manually edit the `<parent><version>` block in each child pom.*

### 2. Commit and Push

Commit the version bump changes.

```bash
git add .
git commit -m "Bump version to 0.1.0-beta.2"
git push origin master
```

### 3. Create GitHub Release

1.  Go to the [GitHub Repository Releases page](https://github.com/slonikmak/trionix-map/releases).
2.  Click **"Draft a new release"**.
3.  **Choose a tag**: Create a new tag matching your version, e.g., `v0.1.0-beta.2`.
4.  **Target**: Select `master`.
5.  **Release title**: `v0.1.0-beta.2` (or a descriptive title).
6.  **Description**: Add release notes, changelog, and highlights.
7.  Click **"Publish release"**.

### 4. Automated Publication

Once the release is published on GitHub, the **"Release to GitHub Packages and Releases"** workflow will automatically trigger.

This workflow will:
1.  Build the project.
2.  Run tests.
3.  **Publish Artifacts** to GitHub Packages (Maven Registry).
4.  **Attach JARs** to the GitHub Release page as assets.

### 5. Verify

- Check the **Actions** tab to ensure the workflow succeeded.
- Verify that the new version is available in **GitHub Packages**.
- Verify that `.jar` files are attached to the Release page.

## Using the Released Version

Consumers can now use the new version in their `pom.xml` (ensure they have authenticated with GitHub Packages or the relevant repository configuration).

```xml
<dependency>
    <groupId>com.trionix</groupId>
    <artifactId>trionix-map-core</artifactId>
    <version>0.1.0-beta.2</version>
</dependency>
```

## Publishing notes & PAT fallback ⚠️

If publishing with the automatically-provided `GITHUB_TOKEN` continues to return `401`/`403`:

1. **Check workflow permissions** — ensure the workflow has `packages: write` (and `contents: read`/`write`) in the workflow `permissions` block and the repo-level Actions permissions are set to **Read and write** in Settings → Actions → General.
2. **Verify generated `settings.xml`** — the workflow includes a `Check settings.xml` step that verifies the `<server><id>github</id>` entry. Inspect job logs for the masked `settings.xml` output if the check fails.
3. **Package access controls** — for private packages, go to the package page → **Manage Actions access** and add the repository so workflows can write to the package.
4. **Use a PAT fallback** — create a Personal Access Token (classic or fine-grained) with **write access to Packages** (and `repo` if your package requires it). Add it as a repository secret named `MAVEN_TOKEN` and the release workflow will automatically prefer `MAVEN_TOKEN` over `GITHUB_TOKEN` when present.

Example (create secret):
- Repo → Settings → Secrets and variables → Actions → **New repository secret**
- **Name**: `MAVEN_TOKEN`
- **Value**: the token you generated

After adding the secret, re-run the release workflow. The workflow is already configured to fall back to `MAVEN_TOKEN` when available.