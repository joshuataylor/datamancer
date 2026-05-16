package com.github.joshuataylor.datamancer.dbtcore.mise

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project

/**
 * Coordinates mise integration. Safe to call even when the mise plugin is not installed.
 */
object DatamancerMiseIntegration {
    private val log = logger<DatamancerMiseIntegration>()

    private const val MISE_PLUGIN_ID = "com.github.l34130.mise"

    /**
     * Whether the mise plugin is installed and enabled.
     */
    fun isMisePluginAvailable(): Boolean {
        val pluginId = PluginId.getId(MISE_PLUGIN_ID)
        return PluginManagerCore.isLoaded(pluginId)
    }

    /**
     * Whether mise is available and enabled at the project level.
     */
    fun isMiseEnabledForProject(project: Project): Boolean {
        if (!isMisePluginAvailable()) return false
        return try {
            DatamancerMiseHelper.isMiseEnabledForProject(project)
        } catch (e: NoClassDefFoundError) {
            log.debug("Mise plugin classes not available: ${e.message}")
            false
        }
    }

    /**
     * Get mise environment variables, or empty map if mise is unavailable/disabled.
     */
    fun getMiseEnvironmentVariables(
        project: Project,
        workingDirectory: String,
    ): Map<String, String> {
        if (!isMisePluginAvailable()) return emptyMap()
        return try {
            DatamancerMiseHelper.getMiseEnvironmentVariables(project, workingDirectory)
        } catch (e: NoClassDefFoundError) {
            log.warn("Mise plugin classes not available: ${e.message}")
            emptyMap()
        }
    }
}
