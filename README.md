# Spring Fluent Query — IntelliJ Plugin

[🇪🇸 Versión en español](README.es.md) | [🇧🇷 Versão em português](README.pt.md)

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2024.3+-000000?logo=intellijidea&logoColor=white)](https://www.jetbrains.com/idea/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**IDE DX** for [spring-fluent-query](https://github.com/BenjaminOR-dev/spring-fluent-query): autocomplete and inspections on entity field / association **paths** written as strings (`where`, `select`, `fetch`, `whereHas`, …).

> This is **not** the Maven library. The library runs your queries; this plugin helps you write the string paths correctly inside IntelliJ.

Companion to:

| Project | Role |
|---------|------|
| [spring-fluent-query](https://github.com/BenjaminOR-dev/spring-fluent-query) | Runtime fluent queries (Maven) |
| **spring-fluent-query-intellij** | IDE support for those string paths |
| [spring-fluent-map](https://github.com/BenjaminOR-dev/spring-fluent-map) | Entity → DTO mapping |
| [spring-validation-plus](https://github.com/BenjaminOR-dev/spring-validation-plus) | DTO validation |

**Includes:**

- Path references inside FluentQuery string literals (Ctrl/Cmd-click → field)
- Autocomplete for entity attributes and associations (nested paths)
- Inspection: unresolved path → **ERROR**
- JPA entity graph from the **open project** (fields + `@ManyToOne` / `@OneToMany` / …; no Fluent Query JAR at plugin runtime)

> Current version: **0.1.1** · [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33175-spring-fluent-query) · build from source with `./gradlew buildPlugin`

## Why use this plugin?

Fluent Query accepts paths as strings (`"email"`, `"profile.address"`, `"orders"`). The compiler cannot check them — a typo becomes a runtime failure.

| Without the plugin | With the plugin |
|--------------------|-----------------|
| Paths are opaque strings | Navigate / resolve paths against JPA entities |
| Typos discovered at runtime | Unresolved path highlighted as ERROR |
| Manual recall of association names | Autocomplete from the entity graph |

**When you do not need it:** you only use type-safe metamodel overloads (`User_.email`) and never string paths.

**When it pays off:** mixed or string-heavy Fluent Query usage in a JPA project open in IntelliJ.

## Table of contents

- [Why use this plugin?](#why-use-this-plugin)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [What it covers](#what-it-covers)
- [Module architecture](#module-architecture)
- [Development](#development)
- [CI / publishing](#ci--publishing)
- [License](#license)

## Requirements

- **JDK 21** (plugin build toolchain)
- **IntelliJ IDEA 2024.3+** (Community or Ultimate)
- A project that uses **spring-fluent-query** + JPA entities (the plugin inspects sources in the IDE, not a bundled library JAR)

## Quick start

### 1. Install the plugin

**From source (today):**

```bash
./gradlew buildPlugin   # ZIP → build/distributions/
```

Then in IntelliJ: **Settings → Plugins → ⚙ → Install Plugin from Disk…** and pick the ZIP.

**From JetBrains Marketplace** — when a release is published (see [CI / publishing](#ci--publishing)).

### 2. Open a Fluent Query project

Extend `FluentQueryRepository` and write string paths as usual. The plugin targets calls such as:

```java
userRepository.query()
        .where("email", email)       // path → User.email
        .fetch("profile")            // association
        .whereHas("orders")          // collection / relation
        .select("id", "email")
        .first();
```

Open a project that depends on spring-fluent-query and has JPA `@Entity` classes; path strings under `query().where` / `fetch` / `select` / … are then resolved against those entities.

## What it covers

| Fluent Query API (string args) | What the plugin does |
|--------------------------------|----------------------|
| `where*` / `orWhere*` / `optionalWhere*` / RelatedFilter | **Scalar attributes only** (no dots, no associations) |
| `orderBy*` / `latest` / `oldest` | Property paths (dots OK; scalar leaf, e.g. `profile.bio`) |
| `select(...)` | Paths + `assoc:col1,col2` shorthand (scalar leaf) |
| `fetch` / `with` / `fetchCollection` / `withCollection` / `FetchRel` / `Map.of` keys | **Associations only** (dots OK; `:` forbidden) |
| `whereHas*` / `whereRelated*` / `whereRelation*` | Relation (+ leaf column for related/relation) |
| `PropertyFilters.hasProperty*` / `hasRelated*` / `hasRelation` | Same roles on the repository |

Key rules: `where("a.b")` → error (use `whereRelated*` / `whereHas`); `fetch("rel:cols")` → error (use `select`); association name in `where` → error.

Out of scope: rewriting queries, running SQL, non-literal strings, Kotlin.

## Module architecture

```text
spring-fluent-query-intellij/
└── src/main/java/.../intellij/
    ├── model/         # JPA entity graph + call-site / path resolve
    ├── reference/     # PsiReferenceContributor (paths in string literals)
    ├── completion/    # Path autocomplete inside those literals
    └── inspection/    # LocalInspectionTool (unresolved path → ERROR)
```

| Piece | Role |
|-------|------|
| `JpaEntityGraphResolver` / `PathResolver` | Build graph from PSI + resolve dotted paths |
| `FluentQueryCallAnalyzer` | Detect FluentQuery / RelatedFilter call sites and root entity `T` |
| `FluentQueryPathReferenceContributor` | Attach references to path string arguments |
| `FluentQueryPathCompletionContributor` | Autocomplete attributes / associations |
| `FluentQueryUnresolvedPathInspection` | Report paths that do not exist on the entity graph |

Versioned **independently** from `spring-fluent-query` (Maven). Same public path conventions; no runtime dependency on the library JAR.

## Development

**Local (JDK 21 + Gradle Wrapper):**

```bash
./gradlew check           # unit + LightJavaCodeInsightFixture tests
./gradlew buildPlugin     # ZIP in build/distributions/
./gradlew runIde          # sandbox IDE with the plugin loaded
./gradlew verifyPlugin    # Plugin Verifier (recommended IDEs)
```

**Docker** (same flow as the other Spring Fluent libraries — no local JDK required for CI-like runs):

```bash
docker compose run --rm gradle          # ./gradlew check
docker compose run --rm buildPlugin     # ZIP under build/distributions/
docker compose run --rm verify          # verifyPlugin (heavy)
```

First run downloads the IntelliJ Platform into named volumes (`gradle-cache`, `intellij-cache`).

## CI / publishing

| Workflow | When | What |
|----------|------|------|
| `build.yml` | PR / `main` | `buildPlugin`, `check`, `verifyPlugin` + ZIP artifact |
| `release.yml` | Tag `v*` | **Draft** GitHub Release (+ plugin ZIP) |
| `release.yml` | Release **published** | `./gradlew publishPlugin` → JetBrains Marketplace |

Required secret: `PUBLISH_TOKEN`. Full checklist: [PUBLISHING.md](PUBLISHING.md).

## License

Copyright © 2026 **Benjamín Olvera R.**

Licensed under the [Apache License, Version 2.0](LICENSE).
