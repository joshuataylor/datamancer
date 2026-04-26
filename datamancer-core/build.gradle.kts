import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("datamancer-conventions")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("datamancerCorePluginVersion").get()

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
        jetbrainsRuntime()
    }
}

intellijPlatform {
    buildSearchableOptions = providers.gradleProperty("enableBuildSearchableOptions")
        .map { it.toBoolean() }
        .orElse(false)

    pluginConfiguration {
        name = "Datamancer Core"
        version = providers.gradleProperty("datamancerCorePluginVersion")

//        description = providers.fileContents(rootProject.layout.projectDirectory.file("README.md")).asText.map {
//            val start = "<!-- Plugin description -->"
//            val end = "<!-- Plugin description end -->"
//
//            with(it.lines()) {
//                if (!containsAll(listOf(start, end))) {
//                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
//                }
//                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
//            }
//        }

        val changelog = project.changelog
        changeNotes = providers.gradleProperty("datamancerCorePluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("datamancerCorePluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    path.set(rootProject.layout.projectDirectory.file("CHANGELOG.md").asFile.path)
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    test {
        systemProperty("idea.log.trace.categories", "#com.github.joshuataylor.datamancer")
    }
    runIde {
        systemProperties.put("idea.log.debug.categories", "com.github.joshuataylor.datamancer")
    }
    publishPlugin {
        dependsOn(patchChangelog)
    }
}
