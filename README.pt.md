# Spring Fluent Query — IntelliJ Plugin

[🇬🇧 English version](README.md) | [🇪🇸 Versión en español](README.es.md)

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2024.3+-000000?logo=intellijidea&logoColor=white)](https://www.jetbrains.com/idea/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**DX de IDE** para [spring-fluent-query](https://github.com/BenjaminOR-dev/spring-fluent-query): autocomplete e inspeções sobre **paths** de campos / associações de entidade escritos como strings (`where`, `select`, `fetch`, `whereHas`, …).

> Isto **não** é a biblioteca Maven. A biblioteca executa suas queries; este plugin ajuda a escrever corretamente os paths em string dentro do IntelliJ.

Irmão de:

| Projeto | Papel |
|---------|-------|
| [spring-fluent-query](https://github.com/BenjaminOR-dev/spring-fluent-query) | Queries fluidas em runtime (Maven) |
| **spring-fluent-query-intellij** | Suporte IDE para esses paths em string |
| [spring-fluent-map](https://github.com/BenjaminOR-dev/spring-fluent-map) | Mapeamento entidade → DTO |
| [spring-validation-plus](https://github.com/BenjaminOR-dev/spring-validation-plus) | Validação de DTOs |

**Inclui:**

- Referências de path dentro de literais string do FluentQuery (Ctrl/Cmd-click → campo)
- Autocomplete de atributos e associações de entidade (paths aninhados)
- Inspeção: path não resolvido → **ERROR**
- Grafo de entidades JPA do **projeto aberto** (campos + `@ManyToOne` / `@OneToMany` / …; sem JAR do Fluent Query no runtime do plugin)

> Status: **MVP** (`0.1.0-SNAPSHOT`). Instale a partir do fonte com `./gradlew buildPlugin` até publicar no Marketplace.

## Por que usar este plugin?

Fluent Query aceita paths como strings (`"email"`, `"profile.address"`, `"orders"`). O compilador não pode verificá-los — um typo vira falha em runtime.

| Sem o plugin | Com o plugin |
|--------------|--------------|
| Paths são strings opacos | Navegar / resolver paths contra entidades JPA |
| Typos descobertos em runtime | Path não resolvido marcado como ERROR |
| Lembrar à mão os nomes das associações | Autocomplete a partir do grafo de entidades |

**Quando você não precisa:** só usa overloads type-safe do metamodelo (`User_.email`) e nunca paths em string.

**Quando vale a pena:** uso misto ou intensivo de strings do Fluent Query num projeto JPA aberto no IntelliJ.

## Índice

- [Por que usar este plugin?](#por-que-usar-este-plugin)
- [Requisitos](#requisitos)
- [Início rápido](#início-rápido)
- [O que cobre](#o-que-cobre)
- [Arquitetura do módulo](#arquitetura-do-módulo)
- [Desenvolvimento](#desenvolvimento)
- [CI / publicação](#ci--publicação)
- [Roadmap](#roadmap)
- [Licença](#licença)

## Requisitos

- **JDK 21** (toolchain de build do plugin)
- **IntelliJ IDEA 2024.3+** (Community ou Ultimate)
- Um projeto que use **spring-fluent-query** + entidades JPA (o plugin inspeciona fontes no IDE, não um JAR empacotado da biblioteca)

## Início rápido

### 1. Instalar o plugin

**A partir do código-fonte (hoje):**

```bash
./gradlew buildPlugin   # ZIP → build/distributions/
```

Depois no IntelliJ: **Settings → Plugins → ⚙ → Install Plugin from Disk…** e escolha o ZIP.

**Pelo JetBrains Marketplace** — quando houver um release publicado (ver [CI / publicação](#ci--publicação)).

### 2. Abrir um projeto Fluent Query

Estenda `FluentQueryRepository` e escreva paths em string como de costume. O plugin mira chamadas como:

```java
userRepository.query()
        .where("email", email)       // path → User.email
        .fetch("profile")            // associação
        .whereHas("orders")          // coleção / relação
        .select("id", "email")
        .first();
```

Abra um projeto que dependa de spring-fluent-query e tenha classes `@Entity` JPA; os paths em string sob `query().where` / `fetch` / `select` / … são resolvidos contra essas entidades.

## O que cobre

| API Fluent Query (args string) | Intenção do plugin |
|--------------------------------|--------------------|
| `where*` / `orWhere*` / `optionalWhere*` | Resolver / completar paths de atributos raiz ou aninhados |
| `select(...)` | O mesmo para propriedades projetadas |
| `fetch` / `with` / `fetchCollection` | Paths de associação (to-one / to-many) |
| `whereHas` / `whereDoesntHave` / `whereRelation` / `whereRelated*` | Nomes de relação + paths aninhados relacionados |

Fora do escopo: reescrever queries, executar SQL ou substituir a biblioteca Maven.

## Arquitetura do módulo

```text
spring-fluent-query-intellij/
└── src/main/java/.../intellij/
    ├── model/         # Grafo JPA + resolve de call-site / paths
    ├── reference/     # PsiReferenceContributor (paths em literais string)
    ├── completion/    # Autocomplete de paths dentro desses literais
    └── inspection/    # LocalInspectionTool (path não resolvido → ERROR)
```

| Peça | Papel |
|------|-------|
| `JpaEntityGraphResolver` / `PathResolver` | Construir grafo a partir de PSI + resolver paths com pontos |
| `FluentQueryCallAnalyzer` | Detectar call sites FluentQuery / RelatedFilter e a entidade raiz `T` |
| `FluentQueryPathReferenceContributor` | Anexar references aos argumentos string de path |
| `FluentQueryPathCompletionContributor` | Autocomplete de atributos / associações |
| `FluentQueryUnresolvedPathInspection` | Reportar paths que não existem no grafo de entidades |

Versionado **independentemente** de `spring-fluent-query` (Maven). Mesmas convenções públicas de path; sem dependência de runtime do JAR da biblioteca.

## Desenvolvimento

**Local (JDK 21 + Gradle Wrapper):**

```bash
./gradlew check           # unit + LightJavaCodeInsightFixture
./gradlew buildPlugin     # ZIP em build/distributions/
./gradlew runIde          # IDE sandbox com o plugin carregado
./gradlew verifyPlugin    # Plugin Verifier (IDEs recomendados)
```

**Docker** (mesmo fluxo das outras libs Spring Fluent — sem JDK local para runs tipo CI):

```bash
docker compose run --rm gradle          # ./gradlew check
docker compose run --rm buildPlugin     # ZIP em build/distributions/
docker compose run --rm verify          # verifyPlugin (pesado)
```

A primeira execução baixa a IntelliJ Platform em volumes (`gradle-cache`, `intellij-cache`).

## CI / publicação

| Workflow | Quando | O quê |
|----------|--------|-------|
| `build.yml` | PR / `main` | `buildPlugin`, `check`, `verifyPlugin` + artifact ZIP |
| `release.yml` | Tag `v*` | Release do GitHub em **draft** (+ ZIP do plugin) |
| `release.yml` | Release **publicado** | `./gradlew publishPlugin` → JetBrains Marketplace |

Segredo necessário: `PUBLISH_TOKEN`. Checklist: [PUBLISHING.md](PUBLISHING.md).

## Roadmap

- [x] Scaffold do plugin (Gradle IntelliJ Platform 2.x)
- [x] Grafo de entidades JPA (`model/`) a partir das fontes do projeto aberto
- [x] Completion de paths dentro de literais string do FluentQuery
- [x] References de path + inspeção ERROR para paths não resolvidos
- [x] Testes com fixture IDE (`LightJavaCodeInsightFixture`)
- [x] Docker Compose para check / buildPlugin / verify
- [x] Fluxo de release: tag → draft → publish → Marketplace
- [ ] Primeiro release no Marketplace (`0.1.0`)

## Licença

Copyright © 2026 **Benjamín Olvera R.**

Licenciado sob a [Apache License, Version 2.0](LICENSE).
