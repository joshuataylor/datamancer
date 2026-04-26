plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugins.kotlin.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.intelliJPlatform.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
