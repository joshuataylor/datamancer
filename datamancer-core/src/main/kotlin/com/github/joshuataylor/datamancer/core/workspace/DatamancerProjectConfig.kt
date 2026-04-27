package com.github.joshuataylor.datamancer.core.workspace

import kotlinx.serialization.Serializable

/**
 * Configuration for a single dbt project within the workspace.
 * Stored in workspace model via external mapping.
 */
@Serializable
data class DatamancerProjectConfig(
    val projectRoot: String,
    val profileDirectory: String? = null,
    val defaultTarget: String? = null,
    val defaultDataSource: String? = null,
    val queryLimit: Int = 1000,
    val environmentVariables: Map<String, String> = emptyMap(),
    val preferredBackend: String? = null,
    val useMiseEnvironment: Boolean = true,
    val excludedDirectories: List<String> = DEFAULT_EXCLUDED_DIRECTORIES,
) {
    companion object {
        val DEFAULT_EXCLUDED_DIRECTORIES = listOf("target", "logs", "dbt_packages")
    }
}
