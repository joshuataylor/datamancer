package com.github.joshuataylor.datamancer.core.services

import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.github.joshuataylor.datamancer.core.workspace.DatamancerWorkspaceKeys
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that maintains an index of dbt projects for efficient lookups.
 * Listens to workspace model changes and updates the index accordingly.
 *
 * Call [start] to begin monitoring workspace changes.
 */
@Service(Service.Level.PROJECT)
class DatamancerDbtProjectIndexService(
    private val project: Project,
    private val cs: CoroutineScope
) {
    private val log = logger<DatamancerDbtProjectIndexService>()
    private val dbtProjectsByModule = ConcurrentHashMap<ModuleId, DatamancerProjectConfig>()

    @Volatile
    private var started = false

    /**
     * Start monitoring workspace model changes.
     * Should be called after project initialisation is complete.
     * This method is idempotent - calling it multiple times has no effect.
     */
    fun start() {
        if (started) {
            log.debug("Index service already started for project: ${project.name}")
            return
        }
        log.debug("Starting dbt project index service for project: ${project.name}")
        started = true

        cs.launch {
            // Build initial index from current workspace snapshot directly,
            // rather than relying on eventLog replay which may miss earlier updates
            buildInitialIndex()

            // Refresh the project view so directory decorators pick up the newly indexed roots.
            // Without this, subdirectory dbt project icons may not appear because the tree
            // rendered before the index was populated.
            if (dbtProjectsByModule.isNotEmpty()) {
                withContext(Dispatchers.EDT) {
                    ProjectView.getInstance(project).currentProjectViewPane?.updateFromRoot(true)
                }
            }

            // Then subscribe to workspace model changes for ongoing updates
            log.debug("Subscribing to workspace model changes")
            val workspaceModel = WorkspaceModel.getInstance(project)
            workspaceModel.eventLog.collect { event ->
                log.debug("Workspace model changed, updating dbt project index")
                updateIndex(event.storageAfter)
            }
        }
        log.debug("Started dbt project index service for project: ${project.name}")
    }

    private suspend fun buildInitialIndex() {
        log.debug("Building initial dbt project index from current workspace snapshot")
        readAction {
            val snapshot = WorkspaceModel.getInstance(project).currentSnapshot
            val mapping = snapshot.getExternalMapping(DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG)
            val allModules = snapshot.entities(ModuleEntity::class.java).toList()
            log.debug("Found ${allModules.size} modules in workspace")

            dbtProjectsByModule.clear()
            allModules.forEach { module ->
                mapping.getDataByEntity(module)?.let { config ->
                    dbtProjectsByModule[module.symbolicId] = config
                    log.debug("Indexed dbt project for module: ${module.name} (project root: ${config.projectRoot})")
                }
            }

            log.debug("Initial index built: ${dbtProjectsByModule.size} dbt projects")
        }
    }

    private suspend fun updateIndex(storage: ImmutableEntityStorage) {
        log.debug("Updating dbt project index from workspace change")
        readAction {
            val previousSize = dbtProjectsByModule.size
            dbtProjectsByModule.clear()

            val mapping = storage.getExternalMapping(DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG)
            storage.entities(ModuleEntity::class.java).forEach { module ->
                mapping.getDataByEntity(module)?.let { config ->
                    dbtProjectsByModule[module.symbolicId] = config
                    log.debug("Indexed dbt project for module: ${module.name} (project root: ${config.projectRoot})")
                }
            }

            val newSize = dbtProjectsByModule.size
            log.debug("Index updated: $newSize dbt projects (was: ${previousSize})")
        }
    }

    /**
     * Get all dbt projects with their module entities.
     * Uses readAction to safely access workspace model.
     */
    suspend fun getDbtProjects(): List<Pair<ModuleEntity, DatamancerProjectConfig>> {
        log.debug("Getting all dbt projects from index")
        return readAction {
            val snapshot = WorkspaceModel.getInstance(project).currentSnapshot
            val projects = dbtProjectsByModule.mapNotNull { (moduleId, config) ->
                val module = moduleId.resolve(snapshot)
                if (module == null) {
                    log.warn("Could not resolve module for ID: ${moduleId.name}")
                    return@mapNotNull null
                }
                log.trace("Resolved module: ${module.name} with project root: ${config.projectRoot}")
                module to config
            }
            log.debug("Returning ${projects.size} dbt projects")
            projects
        }
    }

    /**
     * Get the dbt configuration for a module by name.
     * This is a simple map lookup and doesn't require read action.
     */
    fun getDbtConfigForModuleName(moduleName: String): DatamancerProjectConfig? {
        log.trace("Getting dbt config for module name: $moduleName")
        val moduleId = ModuleId(moduleName)
        val config = dbtProjectsByModule[moduleId]
        if (config != null) {
            log.trace("Found dbt config for module name: $moduleName, project root: ${config.projectRoot}")
        } else {
            log.trace("No dbt config found for module name: $moduleName")
        }
        return config
    }

    /**
     * Check if the given path is a dbt project root.
     * This is a synchronous operation that accesses the in-memory cache.
     * Safe to call from any thread, including the EDT and background threads.
     *
     * @param path The absolute path to check
     * @return true if the path is a dbt project root, false otherwise
     */
    fun isDbtProjectRoot(path: String): Boolean {
        log.trace("Checking if path is dbt project root: $path")
        val normalised = path.trimEnd('/')
        val isDbt = dbtProjectsByModule.values.any { it.projectRoot.trimEnd('/') == normalised }
        if (isDbt) {
            log.trace("Path is dbt project root: $path")
        }
        return isDbt
    }

    /**
     * Get all dbt configurations from the in-memory cache.
     * This is a synchronous operation that doesn't require read actions.
     * Safe to call from any thread, including the EDT and background threads.
     *
     * Returns a map of module names to their dbt configurations.
     */
    fun getAllDbtConfigsSync(): Map<String, DatamancerProjectConfig> {
        log.trace("Getting all dbt configs from cache synchronously")
        return dbtProjectsByModule.entries.associate { (moduleId, config) ->
            moduleId.name to config
        }
    }

    companion object {
        fun getInstance(project: Project): DatamancerDbtProjectIndexService =
            project.getService(DatamancerDbtProjectIndexService::class.java)
    }
}
