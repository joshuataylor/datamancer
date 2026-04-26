import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    sandboxContainer = project.layout.buildDirectory.dir("idea-sandbox")
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        ) {
            useInstaller.set(
                providers.gradleProperty("platformUseInstaller")
                    .map { it.toBoolean() }
                    .orElse(true)
            )
        }

        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins").map { it.split(',') }
        )

        bundledModules(
            providers.gradleProperty("platformBundledModules").map { it.split(',') }
        )

        // Marketplace plugins (non-bundled plugin dependencies)
        val platformPluginsList = providers.gradleProperty("platformPlugins")
            .map { it.split(',').filter(String::isNotBlank) }
            .getOrElse(emptyList())
        if (platformPluginsList.isNotEmpty()) {
            plugins(platformPluginsList)
        }

        // Compatible plugins for compile-time (optional dependencies)
        val platformCompatiblePluginsList = providers.gradleProperty("platformCompatiblePlugins")
            .map { it.split(',').filter(String::isNotBlank) }
            .getOrElse(emptyList())
        if (platformCompatiblePluginsList.isNotEmpty()) {
            compatiblePlugins(platformCompatiblePluginsList)
        }
    }
}

intellijPlatformTesting.runIde.configureEach {
    val runIdeCompatiblePluginsList = providers.gradleProperty("runIdeCompatiblePlugins")
        .map { it.split(',').filter(String::isNotBlank) }
        .getOrElse(emptyList())

    plugins {
        if (runIdeCompatiblePluginsList.isNotEmpty()) {
            compatiblePlugins(runIdeCompatiblePluginsList)
        }
        disablePlugin("com.intellij.kubernetes")
        disablePlugin("org.jetbrains.plugins.sass")
    }
}

val runIntelliJUltimate by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.IntellijIdeaUltimate
    version = providers.gradleProperty("intellijUltimatePlatformVersion")
}

val runPyCharm by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.PyCharm
    version = providers.gradleProperty("pycharmPlatformVersion")
}

val runRubyMine by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.RubyMine
    version = providers.gradleProperty("rubyminePlatformVersion")
}
