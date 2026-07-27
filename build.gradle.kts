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
            <ul>
              <li>JPA entity graph from open project sources</li>
              <li>Path references, autocomplete, and ERROR inspection</li>
              <li>select shorthand (<code>assoc:col1,col2</code>)</li>
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
