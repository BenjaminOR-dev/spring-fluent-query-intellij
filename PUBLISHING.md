# Publishing — JetBrains Marketplace

How to ship **Spring Fluent Query** (IntelliJ plugin) to
[JetBrains Marketplace](https://plugins.jetbrains.com/).

Flow (same idea as spring-fluent-query / fluent-map / validation-plus):

1. Push a `v*` tag → GitHub Actions creates a **draft** Release (with plugin ZIP).
2. You click **Publish release** → Actions deploys with `./gradlew publishPlugin`.

See [`.github/workflows/release.yml`](.github/workflows/release.yml).

## One-time setup

| Item | Where |
|------|--------|
| Marketplace account + plugin listing | [JetBrains Marketplace](https://plugins.jetbrains.com/) |
| `PUBLISH_TOKEN` | GitHub repo → **Settings → Secrets and variables → Actions** |

Token docs: [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html).

## Checklist per release

### 1. Non-SNAPSHOT version (before the tag)

In [`gradle.properties`](gradle.properties):

```properties
pluginVersion = 0.1.1
```

Commit that change on `main` (or the release branch). The tag must be `v` + that version
(`v0.1.1` ↔ `pluginVersion = 0.1.1`). The publish job refuses SNAPSHOT and tag mismatches.

### 2. Tag and push

```bash
git tag -a v0.1.1 -m "Release 0.1.1"
git push origin v0.1.1
```

### 3. Review the draft Release

GitHub → **Releases** → open the draft for `v0.1.1`:

- Check release notes and the attached ZIP.
- Click **Publish release** when ready (only after JetBrains has approved the listing if this is the first upload).

### 4. Marketplace

After a green `publish-marketplace` job, the upload appears in your
[Marketplace vendor profile](https://plugins.jetbrains.com/vendor/edit) for review / listing updates.

## Local dry-run (optional)

```bash
./gradlew check verifyPlugin buildPlugin
# ZIP → build/distributions/

# Only with a real token in the environment:
export PUBLISH_TOKEN=...
./gradlew publishPlugin
```

Or via Docker:

```bash
docker compose run --rm gradle ./gradlew check verifyPlugin buildPlugin
```
