# Spring Fluent Query — IntelliJ Plugin

Changelog
=========

## [Unreleased]

### Added

- Scaffold del plugin IntelliJ (Gradle Platform Plugin 2.x)
- Grafo JPA cacheado (`model/`): propiedades, asociaciones, `@Embedded`; omite `@Transient` / `serialVersionUID`
- Resolve de call-site: `FluentQuery<T>` / `FluentQueryRepository` / `RelatedFilter` (leaf)
- Path references, autocomplete e inspección ERROR (incl. aviso si `where` usa dots)
- Soporte `select` shorthand (`status:id,name`)
- Tests unitarios + `LightJavaCodeInsightFixture` (grafo, inspection, references, completion)
- Docker Compose (`gradle` / `buildPlugin` / `verify`) con JDK 21
- Release: tag `v*` → draft GitHub Release (+ ZIP); al publicar → JetBrains Marketplace
- CI: `buildPlugin`, `check`, `verifyPlugin` + artifact ZIP
