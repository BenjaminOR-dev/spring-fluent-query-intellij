# Spring Fluent Query — IntelliJ Plugin

Changelog
=========

## [Unreleased]

## [0.1.3] — 2026-07-30

### Added

- ERROR inspection: `fetch` / `with*` + projection terminals (`first(Class)`, … and deprecated `*As`)
- ERROR inspection: `fetchCollection` / `withCollection` + `page` / `slice` / `paginate` / `chunk` / `limit`
- WARNING inspection: deprecated `*As` with quick fix → Class overloads (Class last for `page`/`paginate`/`slice`)
- WEAK WARNING: `select(...)` + entity terminal without projection Class
- Path validation for `static final String` constants used as path arguments
- Quick fixes: remove `fetch`/`with*` from chain; remove `fetchCollection`/`withCollection`
- Path roles for `latest*` / `oldest*` sort column arguments (including Class overloads)

### Changed

- Aligned with spring-fluent-query **0.2.1**: Class overloads on shared terminal names; fetch+projection inspection uses argument count so entity `first()` is not flagged

## [0.1.2] — 2026-07-27

### Fixed

- Exclude already-used paths from autocomplete in `select` / `fetch` / `orderBy*` varargs
- ERROR inspection for duplicate path arguments in those methods (and duplicate columns in `select` shorthand)

## [0.1.1] — 2026-07-27

### Fixed / aligned with spring-fluent-query

- Nested path completion after trailing `.` (segment prefix matcher; no duplicate variants)
- `where*` suggests and accepts **scalar attributes only** (associations → clear ERROR + what to use instead)
- `orderBy*` / `latest` / `oldest`: nested property paths (`profile.bio`) allowed
- `fetch` / `whereHas` / `FetchRel`: reject `:` with actionable message (`select` for column lists)
- `select`: leaf must be scalar; shorthand validated like the library
- Map keys in `fetch(Map.of(...))` / `Map.ofEntries(Map.entry(...))`, `new FetchRel(...)`, `PropertyFilters.*`
- Repository generic hierarchy (`Repo extends BaseRepo<Entity>`) + JPA mapping annotations on getters
- Ctrl+click on columns after `:` in `select("rel:col")`
- Inspection messages rewritten to explain *why* and *what to use instead*

### Docs / release

- README coverage table aligned with library rules (EN / ES / PT)
- Release workflow runs `check` + `verifyPlugin` before draft ZIP / Marketplace publish
- Plugin Verifier: Compatible on IC 2024.3 / 2025.1 / 2025.2

## [0.1.0]

### Added

- IntelliJ plugin scaffold (Gradle Platform Plugin 2.x)
- Cached JPA entity graph (`model/`): properties, associations, `@Embedded`; skips `@Transient` / `serialVersionUID`
- Call-site resolve: `FluentQuery<T>` / `FluentQueryRepository` / `RelatedFilter` (leaf)
- Path references, autocomplete, and ERROR inspection
- `select` shorthand support (`status:id,name`)
- Unit tests + `LightJavaCodeInsightFixture`
- Docker Compose + CI + Marketplace publish workflow
