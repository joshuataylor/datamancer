package com.github.joshuataylor.datamancer.core.workspace

import com.intellij.platform.workspace.storage.ExternalMappingKey

/**
 * External mapping keys for storing dbt-specific data in the workspace model.
 */
object DatamancerWorkspaceKeys {
    /**
     * Key for storing DatamancerProjectConfig associated with ModuleEntity instances.
     */
    val DBT_PROJECT_CONFIG: ExternalMappingKey<DatamancerProjectConfig> =
        ExternalMappingKey.create("dbt.project.config")
}
