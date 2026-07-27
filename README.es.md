# Spring Fluent Query — IntelliJ Plugin

[🇬🇧 English version](README.md) | [🇧🇷 Versão em português](README.pt.md)

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2024.3+-000000?logo=intellijidea&logoColor=white)](https://www.jetbrains.com/idea/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**DX de IDE** para [spring-fluent-query](https://github.com/BenjaminOR-dev/spring-fluent-query): autocomplete e inspecciones sobre **paths** de campos / asociaciones de entidad escritos como strings (`where`, `select`, `fetch`, `whereHas`, …).

> Esto **no** es la librería Maven. La librería ejecuta tus queries; este plugin te ayuda a escribir bien los paths en string dentro de IntelliJ.

Hermano de:

| Proyecto | Rol |
|----------|-----|
| [spring-fluent-query](https://github.com/BenjaminOR-dev/spring-fluent-query) | Queries fluidas en runtime (Maven) |
| **spring-fluent-query-intellij** | Soporte IDE para esos paths en string |
| [spring-fluent-map](https://github.com/BenjaminOR-dev/spring-fluent-map) | Mapeo entidad → DTO |
| [spring-validation-plus](https://github.com/BenjaminOR-dev/spring-validation-plus) | Validación de DTOs |

**Incluye:**

- Referencias de path dentro de literales string de FluentQuery (Ctrl/Cmd-click → campo)
- Autocomplete de atributos y asociaciones de entidad (paths anidados)
- Inspección: path no resuelto → **ERROR**
- Grafo de entidades JPA del **proyecto abierto** (campos + `@ManyToOne` / `@OneToMany` / …; sin JAR de Fluent Query en runtime del plugin)

> Versión actual: **0.1.1** · [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33175-spring-fluent-query) · build desde fuente con `./gradlew buildPlugin`

## ¿Por qué usar este plugin?

Fluent Query acepta paths como strings (`"email"`, `"profile.address"`, `"orders"`). El compilador no puede verificarlos — un typo se convierte en fallo en runtime.

| Sin el plugin | Con el plugin |
|---------------|---------------|
| Los paths son strings opacos | Navegar / resolver paths contra entidades JPA |
| Typos descubiertos en runtime | Path no resuelto marcado como ERROR |
| Recordar a mano los nombres de asociaciones | Autocomplete desde el grafo de entidades |

**Cuándo no lo necesitas:** solo usas sobrecargas type-safe del metamodelo (`User_.email`) y nunca paths en string.

**Cuándo compensa:** uso mixto o intensivo de strings de Fluent Query en un proyecto JPA abierto en IntelliJ.

## Tabla de contenidos

- [¿Por qué usar este plugin?](#por-qué-usar-este-plugin)
- [Requisitos](#requisitos)
- [Inicio rápido](#inicio-rápido)
- [Qué cubre](#qué-cubre)
- [Arquitectura del módulo](#arquitectura-del-módulo)
- [Desarrollo](#desarrollo)
- [CI / publicación](#ci--publicación)
- [Licencia](#licencia)

## Requisitos

- **JDK 21** (toolchain de build del plugin)
- **IntelliJ IDEA 2024.3+** (Community o Ultimate)
- Un proyecto que use **spring-fluent-query** + entidades JPA (el plugin inspecciona fuentes en el IDE, no un JAR empaquetado de la librería)

## Inicio rápido

### 1. Instalar el plugin

**Desde código fuente (hoy):**

```bash
./gradlew buildPlugin   # ZIP → build/distributions/
```

Luego en IntelliJ: **Settings → Plugins → ⚙ → Install Plugin from Disk…** y elige el ZIP.

**Desde JetBrains Marketplace** — cuando haya un release publicado (ver [CI / publicación](#ci--publicación)).

### 2. Abrir un proyecto Fluent Query

Extiende `FluentQueryRepository` y escribe paths en string como de costumbre. El plugin apunta a llamadas como:

```java
userRepository.query()
        .where("email", email)       // path → User.email
        .fetch("profile")            // asociación
        .whereHas("orders")          // colección / relación
        .select("id", "email")
        .first();
```

Abre un proyecto que dependa de spring-fluent-query y tenga clases `@Entity` JPA; los paths en string bajo `query().where` / `fetch` / `select` / … se resuelven contra esas entidades.

## Qué cubre

| API Fluent Query (args string) | Intención del plugin |
|--------------------------------|----------------------|
| `where*` / `orWhere*` / `optionalWhere*` | Resolver / completar paths de atributos raíz o anidados |
| `select(...)` | Igual para propiedades proyectadas |
| `fetch` / `with` / `fetchCollection` | Paths de asociación (to-one / to-many) |
| `whereHas` / `whereDoesntHave` / `whereRelation` / `whereRelated*` | Nombres de relación + paths anidados relacionados |

Fuera de alcance: reescribir queries, ejecutar SQL o reemplazar la librería Maven.

## Arquitectura del módulo

```text
spring-fluent-query-intellij/
└── src/main/java/.../intellij/
    ├── model/         # Grafo JPA + resolve de call-site / paths
    ├── reference/     # PsiReferenceContributor (paths en literales string)
    ├── completion/    # Autocomplete de paths dentro de esos literales
    └── inspection/    # LocalInspectionTool (path no resuelto → ERROR)
```

| Pieza | Rol |
|-------|-----|
| `JpaEntityGraphResolver` / `PathResolver` | Construir grafo desde PSI + resolver paths con puntos |
| `FluentQueryCallAnalyzer` | Detectar call sites FluentQuery / RelatedFilter y la entidad raíz `T` |
| `FluentQueryPathReferenceContributor` | Adjuntar references a argumentos string de path |
| `FluentQueryPathCompletionContributor` | Autocomplete de atributos / asociaciones |
| `FluentQueryUnresolvedPathInspection` | Reportar paths que no existen en el grafo de entidades |

Versionado **independiente** de `spring-fluent-query` (Maven). Mismas convenciones públicas de path; sin dependencia de runtime del JAR de la librería.

## Desarrollo

**Local (JDK 21 + Gradle Wrapper):**

```bash
./gradlew check           # unit + LightJavaCodeInsightFixture
./gradlew buildPlugin     # ZIP en build/distributions/
./gradlew runIde          # IDE sandbox con el plugin cargado
./gradlew verifyPlugin    # Plugin Verifier (IDEs recomendados)
```

**Docker** (mismo flujo que las otras librerías Spring Fluent — sin JDK local para runs tipo CI):

```bash
docker compose run --rm gradle          # ./gradlew check
docker compose run --rm buildPlugin     # ZIP en build/distributions/
docker compose run --rm verify          # verifyPlugin (pesado)
```

La primera ejecución descarga la IntelliJ Platform en volúmenes (`gradle-cache`, `intellij-cache`).

## CI / publicación

| Workflow | Cuándo | Qué |
|----------|--------|-----|
| `build.yml` | PR / `main` | `buildPlugin`, `check`, `verifyPlugin` + artifact ZIP |
| `release.yml` | Tag `v*` | Release de GitHub en **draft** (+ ZIP del plugin) |
| `release.yml` | Release **publicado** | `./gradlew publishPlugin` → JetBrains Marketplace |

Secreto requerido: `PUBLISH_TOKEN`. Checklist: [PUBLISHING.md](PUBLISHING.md).

## Licencia

Copyright © 2026 **Benjamín Olvera R.**

Licenciado bajo la [Apache License, Version 2.0](LICENSE).
