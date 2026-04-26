import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("datamancer-conventions")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("datamancerDbtFusionPluginVersion").get()

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    intellijPlatform {
        // Installs core plugin into the sandbox and adds it to the compile classpath
        localPlugin(project(":datamancer-core"))
        testFramework(TestFrameworkType.Platform)
        jetbrainsRuntime()
    }
}

intellijPlatform {
    buildSearchableOptions = providers.gradleProperty("enableBuildSearchableOptions")
        .map { it.toBoolean() }
        .orElse(false)

    pluginConfiguration {
        name = "Datamancer - dbt Fusion Backend"
        version = providers.gradleProperty("datamancerDbtFusionPluginVersion")

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
        channels = providers.gradleProperty("datamancerDbtFusionPluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
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
}
