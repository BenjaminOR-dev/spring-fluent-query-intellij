plugins {
    java
    id("org.jetbrains.intellij.platform")
}

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    intellijPlatform {
        val platformType = providers.gradleProperty("platformType")
        val platformVersion = providers.gradleProperty("platformVersion")
        create(platformType, platformVersion)

        bundledPlugin("com.intellij.java")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }

    // Required explicitly: Platform test frameworks do not put JUnit on the compile classpath.
    // LightJavaCodeInsightFixtureTestCase needs junit.framework.TestCase; unit tests use org.junit.*.
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = providers.gradleProperty("pluginId")
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }

        vendor {
            name = "Benjamín Olvera R."
            url = "https://github.com/BenjaminOR-dev"
        }

        description = """
            <p>
              IDE support for
              <a href="https://github.com/BenjaminOR-dev/spring-fluent-query">spring-fluent-query</a>:
              autocomplete, references, and inspections for entity fields, associations, and nested paths
              in fluent query strings (<code>where</code>, <code>select</code>, <code>fetch</code>,
              <code>whereHas</code>, …).
            </p>
            <p>
              Reads JPA entities from the open project. Does not replace the Maven library.
            </p>
        """.trimIndent()

        changeNotes = """
            <h3>0.1.2</h3>
            <ul>
              <li>Autocomplete hides paths already used in the same select/fetch/orderBy call</li>
              <li>ERROR on duplicate path args (including select shorthand <code>rel:col1,col2</code>)</li>
            </ul>
            <h3>0.1.1</h3>
            <ul>
              <li>Aligned path rules with spring-fluent-query (where = scalars only; fetch rejects ':')</li>
              <li>Nested completion after trailing dots; orderBy/latest property paths</li>
              <li>Map.of / FetchRel / PropertyFilters support; clearer inspection messages</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    withType<Test> {
        useJUnit()
        maxHeapSize = "2g"
    }
}
