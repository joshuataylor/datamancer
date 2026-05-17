package com.github.joshuataylor.datamancer.core.schema

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

/**
 * Provides bundled JSON schemas for dbt configuration files.
 *
 * Registers schemas for dbt_project.yml, YAML property files (models/, macros/, etc.),
 * packages.yml, dependencies.yml, selectors.yml, profiles.yml, and dbt_cloud.yml.
 * Schemas are only active within detected dbt project roots.
 */
class DatamancerDbtSchemaProviderFactory : JsonSchemaProviderFactory, DumbAware {

    override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        return listOf(
            DbtProjectSchemaProvider(indexService),
            DbtYmlFilesSchemaProvider(indexService),
            DbtPackagesSchemaProvider(indexService),
            DbtDependenciesSchemaProvider(indexService),
            DbtSelectorsSchemaProvider(indexService),
            DbtProfilesSchemaProvider(indexService),
            DbtCloudSchemaProvider(indexService),
        )
    }
}
