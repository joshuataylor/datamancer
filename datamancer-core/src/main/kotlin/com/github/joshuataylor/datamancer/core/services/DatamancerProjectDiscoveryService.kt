package com.github.joshuataylor.datamancer.core.services

import com.github.joshuataylor.datamancer.core.workspace.DatamancerExcludedDirectories
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfigStore
import com.github.joshuataylor.datamancer.core.workspace.DatamancerWorkspaceKeys
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Service for discovering dbt projects in the workspace and associating them with modules.
 */
@Service(Service.Level.PROJECT)
class DatamancerProjectDiscoveryService(private val project: Project) {
    private val log = logger<DatamancerProjectDiscoveryService>()

    /**
     * Find all dbt_project.yml files in the project.
     *
     * Filters out files that sit inside another project's excluded directories
     * (e.g. `dbt_packages/some_package/dbt_project.yml`), since those are
     * dependency projects rather than the user's own projects.
     *
     * Uses readAction to ensure proper read lock acquisition.
     */
    suspend fun findAllDbtProjects(): List<VirtualFile> {
        log.debug("Searching for dbt_project.yml files in project: ${project.name}")
        return readAction {
            val allFiles = FilenameIndex.getVirtualFilesByName(
                "dbt_project.yml",
                GlobalSearchScope.projectScope(project)
            ).filter { it.isValid && !it.isDirectory }

            log.debug("Found ${allFiles.size} dbt_project.yml files (before exclusion filtering)")

            // Collect all project roots (parent directory of each dbt_project.yml)
            val projectRoots = allFiles.map { it.parent.path.trimEnd('/') }

            // Filter out files that sit inside another project's excluded directories.
            // Use the default excluded list here since we may not have configs yet for
            // newly discovered projects; saved configs are checked separately via the
            // persistent store.
            val defaultExcluded = DatamancerProjectConfig.DEFAULT_EXCLUDED_DIRECTORIES
            val configStore = DatamancerProjectConfigStore.getInstance(project)

            val files = allFiles.filter { file ->
                val filePath = file.path
                val isExcluded = projectRoots.any { root ->
                    // Don't test a root against itself
                    if (filePath.startsWith("$root/dbt_project.yml")) return@any false
                    val savedConfig = configStore.getAllConfigs().values
                        .find { it.projectRoot.trimEnd('/') == root }
                    val excludedDirs = savedConfig?.excludedDirectories ?: defaultExcluded
                    DatamancerExcludedDirectories.isInExcludedDirectory(filePath, root, excludedDirs)
                }
                if (isExcluded) {
                    log.debug("  Excluding nested dbt project: $filePath")
                }
                !isExcluded
            }

            log.debug("After exclusion filtering: ${files.size} dbt_project.yml files")
            files.forEachIndexed { index, file ->
                log.debug("  ${index + 1}. ${file.path}")
            }
            files
        }
    }

    /**
     * Find the name of the module that contains the given dbt project file.
     * Returns the module name (not the entity) so it can be resolved later in the correct storage context.
     * Uses readAction to ensure proper read lock acquisition.
     */
    suspend fun findContainingModuleName(dbtProjectFile: VirtualFile): String? {
        log.debug("Finding containing module for dbt project: ${dbtProjectFile.path}")
        return readAction {
            val workspaceModel = WorkspaceModel.getInstance(project)
            val snapshot = workspaceModel.currentSnapshot

            val module = snapshot.entities(ModuleEntity::class.java).find { module ->
                module.contentRoots.any { contentRoot ->
                    val rootPath = contentRoot.url.url
                    val isContained = dbtProjectFile.path.startsWith(rootPath.removePrefix("file://"))
                    if (isContained) {
                        log.debug("  dbt project ${dbtProjectFile.path} is contained in module: ${module.name}")
                    }
                    isContained
                }
            }

            if (module == null) {
                log.debug("  No containing module found for: ${dbtProjectFile.path}")
            } else {
                log.debug("  Found containing module: ${module.name}")
            }
            module?.name
        }
    }

    /**
     * Discover all dbt projects and associate them with modules in the workspace model.
     * Only creates configurations for modules that don't already have one.
     */
    suspend fun discoverAndAssociate() {
        log.debug("Starting dbt project discovery and association for project: ${project.name}")

        // Step 1: Find all dbt projects (requires read action)
        val dbtProjects = findAllDbtProjects()
        log.debug("Found ${dbtProjects.size} dbt projects")

        if (dbtProjects.isEmpty()) {
            log.debug("No dbt projects found, discovery complete")
            return
        }

        // Step 2: Find containing module name for each dbt project (requires read action)
        log.debug("Finding containing modules for ${dbtProjects.size} dbt projects")
        val projectModulePairs = dbtProjects.mapNotNull { dbtProjectFile ->
            val moduleName = findContainingModuleName(dbtProjectFile)
            if (moduleName == null) {
                log.warn("Could not find containing module for dbt project: ${dbtProjectFile.path}")
                null
            } else {
                log.debug("Paired dbt project ${dbtProjectFile.path} with module $moduleName")
                dbtProjectFile to moduleName
            }
        }

        log.debug("Successfully paired ${projectModulePairs.size} dbt projects with modules")

        // Step 3: Update workspace model with all discovered projects (write action)
        // Entities must be resolved within the builder context for external mapping to persist.
        // External mappings are in-memory only, so we also check the persistent store for
        // configs saved from a previous session.
        log.debug("Updating workspace model with dbt project associations")
        val workspaceModel = WorkspaceModel.getInstance(project)
        val configStore = DatamancerProjectConfigStore.getInstance(project)
        var newAssociations = 0
        var restoredAssociations = 0
        var existingAssociations = 0
        val configsToSave = mutableListOf<Pair<String, DatamancerProjectConfig>>()

        // Use edtWriteAction + updateProjectModel because the suspend workspaceModel.update() uses
        // updateWithRetry which silently discards external-mapping-only changes (areEntitiesChanged
        // returns false when only external mappings are modified).
        edtWriteAction {
            workspaceModel.updateProjectModel("Associate dbt projects with modules") { builder ->
                val mapping = builder.getMutableExternalMapping(DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG)

                for ((dbtProjectFile, moduleName) in projectModulePairs) {
                    // Resolve the module entity within the builder so addMapping uses the correct entity ID
                    val module = ModuleId(moduleName).resolve(builder)
                    if (module == null) {
                        log.warn("Could not resolve module $moduleName in builder, skipping")
                        continue
                    }

                    // Skip if external mapping already present (mid-session rediscovery)
                    val existingConfig = mapping.getDataByEntity(module)
                    if (existingConfig != null) {
                        log.debug("Module ${module.name} already has dbt config, skipping (project root: ${existingConfig.projectRoot})")
                        existingAssociations++
                        continue
                    }

                    val projectRoot = dbtProjectFile.parent.path.trimEnd('/')

                    // Try restoring from the persistent store (survives project close/reopen)
                    val savedConfig = configStore.getConfig(moduleName)
                    val config = if (savedConfig != null && savedConfig.projectRoot == projectRoot) {
                        log.debug("Restored saved config for module $moduleName (dataSource=${savedConfig.defaultDataSource})")
                        restoredAssociations++
                        savedConfig
                    } else {
                        log.debug("Creating fresh config for module $moduleName")
                        val fresh = DatamancerProjectConfig(projectRoot = projectRoot)
                        configsToSave.add(moduleName to fresh)
                        newAssociations++
                        fresh
                    }

                    mapping.addMapping(module, config)
                    log.debug("Associated dbt project ${dbtProjectFile.path} with module ${module.name}")
                }
            }
        }

        // Persist any newly created default configs
        for ((moduleName, config) in configsToSave) {
            configStore.saveConfig(moduleName, config)
        }

        // Apply directory exclusions to IntelliJ module content entries.
        // This marks directories like target/, logs/, dbt_packages/ as excluded so
        // the IDE skips them during indexing, search, and file traversal.
        log.debug("Applying directory exclusions for discovered dbt projects")
        for ((_, moduleName) in projectModulePairs) {
            val config = configStore.getConfig(moduleName) ?: continue
            DatamancerExcludedDirectories.applyExclusions(project, moduleName, config)
        }

        log.debug("Discovery complete: $newAssociations new, $restoredAssociations restored, $existingAssociations existing")
    }

    companion object {
        fun getInstance(project: Project): DatamancerProjectDiscoveryService =
            project.getService(DatamancerProjectDiscoveryService::class.java)
    }
}
