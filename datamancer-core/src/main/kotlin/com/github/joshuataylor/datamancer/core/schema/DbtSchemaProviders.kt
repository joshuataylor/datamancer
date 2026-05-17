package com.github.joshuataylor.datamancer.core.schema

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion

private const val SCHEMA_BASE_PATH = "/schemas/dbt"

/**
 * Base class for dbt schema providers. Handles common logic for loading
 * bundled schema resources and checking dbt project membership.
 */
internal abstract class DbtSchemaProviderBase(
    private val indexService: DatamancerDbtProjectIndexService,
    private val schemaFileName: String,
    private val displayName: String,
) : JsonSchemaFileProvider {

    override fun getName(): String = displayName

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaVersion(): JsonSchemaVersion = JsonSchemaVersion.SCHEMA_7

    override fun getSchemaFile(): VirtualFile? {
        val url = javaClass.getResource("$SCHEMA_BASE_PATH/$schemaFileName") ?: return null
        return VfsUtil.findFileByURL(url)
    }

    override fun isUserVisible(): Boolean = true

    /**
     * Checks whether the given file resides within any known dbt project root.
     * Uses the in-memory project index (ConcurrentHashMap lookup, thread-safe,
     * no read action required).
     */
    protected fun isInDbtProject(file: VirtualFile): Boolean {
        val filePath = file.path
        return indexService.getAllDbtConfigsSync().values.any { config ->
            filePath.startsWith(config.projectRoot.trimEnd('/') + "/")
        }
    }

    /**
     * Checks whether the file is within a specific subdirectory of a dbt project.
     */
    protected fun isInDbtSubdirectory(file: VirtualFile, vararg subdirs: String): Boolean {
        val filePath = file.path
        return indexService.getAllDbtConfigsSync().values.any { config ->
            val root = config.projectRoot.trimEnd('/')
            subdirs.any { subdir -> filePath.startsWith("$root/$subdir/") }
        }
    }
}

/** Schema for dbt_project.yml / dbt_project.yaml */
internal class DbtProjectSchemaProvider(
    indexService: DatamancerDbtProjectIndexService,
) : DbtSchemaProviderBase(indexService, "dbt-project.json", "dbt Project") {

    override fun isAvailable(file: VirtualFile): Boolean {
        val name = file.name
        return (name == "dbt_project.yml" || name == "dbt_project.yaml")
            && isInDbtProject(file)
    }
}

/** Schema for YAML property files in models/, macros/, seeds/, snapshots/, analyses/, tests/ */
internal class DbtYmlFilesSchemaProvider(
    indexService: DatamancerDbtProjectIndexService,
) : DbtSchemaProviderBase(indexService, "dbt-yml-files.json", "dbt YAML Properties") {

    override fun isAvailable(file: VirtualFile): Boolean {
        val ext = file.extension
        if (ext != "yml" && ext != "yaml") return false
        if (file.name == "dbt_project.yml" || file.name == "dbt_project.yaml") return false
        return isInDbtSubdirectory(file, "models", "macros", "seeds", "snapshots", "analyses", "tests")
    }
}

/** Schema for packages.yml / packages.yaml */
internal class DbtPackagesSchemaProvider(
    indexService: DatamancerDbtProjectIndexService,
) : DbtSchemaProviderBase(indexService, "packages.json", "dbt Packages") {

    override fun isAvailable(file: VirtualFile): Boolean {
        val name = file.name
        return (name == "packages.yml" || name == "packages.yaml")
            && isInDbtProject(file)
    }
}

/** Schema for dependencies.yml / dependencies.yaml */
internal class DbtDependenciesSchemaProvider(
    indexService: DatamancerDbtProjectIndexService,
) : DbtSchemaProviderBase(indexService, "dependencies.json", "dbt Dependencies") {

    override fun isAvailable(file: VirtualFile): Boolean {
        val name = file.name
        return (name == "dependencies.yml" || name == "dependencies.yaml")
            && isInDbtProject(file)
    }
}

/** Schema for selectors.yml / selectors.yaml */
internal class DbtSelectorsSchemaProvider(
    indexService: DatamancerDbtProjectIndexService,
) : DbtSchemaProviderBase(indexService, "selectors.json", "dbt Selectors") {

    override fun isAvailable(file: VirtualFile): Boolean {
        val name = file.name
        return (name == "selectors.yml" || name == "selectors.yaml")
            && isInDbtProject(file)
    }
}

/** Schema for profiles.yml */
internal class DbtProfilesSchemaProvider(
    indexService: DatamancerDbtProjectIndexService,
) : DbtSchemaProviderBase(indexService, "profiles.json", "dbt Profiles") {

    override fun isAvailable(file: VirtualFile): Boolean {
        return file.name == "profiles.yml" && isInDbtProject(file)
    }
}

/** Schema for dbt_cloud.yml */
internal class DbtCloudSchemaProvider(
    indexService: DatamancerDbtProjectIndexService,
) : DbtSchemaProviderBase(indexService, "dbt-cloud.json", "dbt Cloud") {

    override fun isAvailable(file: VirtualFile): Boolean {
        return file.name == "dbt_cloud.yml" && isInDbtProject(file)
    }
}
