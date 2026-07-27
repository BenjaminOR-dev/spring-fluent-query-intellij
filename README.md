# Spring Fluent Query — IntelliJ Plugin

Plugin de IntelliJ IDEA para [spring-fluent-query](https://github.com/BenjaminOR-dev/spring-fluent-query):
autocomplete e inspecciones sobre paths de campos/relaciones en strings
(`where`, `select`, `fetch`, `whereHas`, …).

> Estado: **scaffold** (0.1.0-SNAPSHOT). El MVP de references/completion viene a continuación.

## Requisitos

- JDK **21**
- IntelliJ IDEA **2024.3+** (Community o Ultimate)
- Gradle Wrapper (incluido)

## Desarrollo

```bash
./gradlew buildPlugin      # genera ZIP en build/distributions/
./gradlew runIde           # IDE sandbox con el plugin cargado
./gradlew check            # tests
./gradlew verifyPlugin     # Plugin Verifier
```

## Estructura

```
src/main/java/.../intellij/
  reference/     # PsiReferenceContributor (paths en literales)
  inspection/    # LocalInspectionTool (paths inválidos → ERROR)
  model/         # grafo JPA (campos + asociaciones) — por implementar
```

## CI/CD

| Workflow | Qué hace |
|----------|----------|
| `build.yml` | En PR/`main`: `buildPlugin`, `check`, `verifyPlugin` + artifact ZIP |
| `publish.yml` | Manual / tag: publica a JetBrains Marketplace (`PUBLISH_TOKEN`) |

Secretos GitHub necesarios para publicar:

- `PUBLISH_TOKEN` — token de [JetBrains Marketplace](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)

## Relación con la librería

Repo **aparte** de `spring-fluent-query` (Maven). Versionado independiente;
el plugin entiende la API pública de FluentQuery sin empaquetar el JAR de runtime
(lee entidades JPA del proyecto abierto en el IDE).

## Licencia

Apache License 2.0 (alineada con spring-fluent-query).
